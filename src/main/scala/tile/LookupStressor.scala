package tile

import chisel3._
import chisel3.util._
import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.diplomacy.LazyModule
import freechips.rocketchip.rocket._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.tile._
import freechips.rocketchip.diplomacy.{LazyModule, LazyModuleImp, IdRange}

/** This is the top module we use to stress the memory bandwidth.
  * We try to simulate a case where atom lookups are really frequent
  * almost 1-1 in proportion to core memory requests.
  */
class ALUStressor(val dontblock: Boolean = false)(implicit p: Parameters) extends Module{
  val io = IO(new Bundle{
    val lookup      = Flipped(new AMULookUpIO)
    val core_snoop  = Flipped(new CoreASPIO)
    val debug1      = Output(Bool())
    val debug2      = Output(Bool())
  })

  // Queue core requests
  val prq            = Module(new RequestQueue(32))
  // io.core           <> DontCare // SAVVINA commented out (uncommented in original metasys)
  if(dontblock){
    println("I wont block requestors")
    io.core_snoop.bp_q_full := false.B
  }else{
    io.core_snoop.bp_q_full := prq.io.almost_full
  }

  val req_addr_s0  = io.core_snoop.addr
  val req_size_s0   = io.core_snoop.size
  val req_cmd_s0   = io.core_snoop.cmd
  // Snoop both stores and loads
  val req_valid_s0 = io.core_snoop.valid && (req_cmd_s0 === 0.U || req_cmd_s0 === 1.U)
  val req_phys_s0  = io.core_snoop.physical // SAVVINA added, not existing in original metasys

  // ------------------------------ stage 1 ------------------------------------
  // cache accesses dtlb at stage 1, so we know the translation
  // but we don't know whether or not the request is valid yet
  // val req_paddr_s1   = Mux(RegNext(io.core.physical), RegNext(io.core.addr), io.core.s1_paddr)  // Returned by dcache
  val req_vaddr_s1   = RegNext(req_addr_s0)
  val paddr_s1_valid = RegNext(req_valid_s0)
  val req_size_s1    = RegNext(req_size_s0) // Convert to prefetch requests
  val req_cmd_s1     = RegNext(req_cmd_s0 + 2.U) // Not important in this case
  val req_phys_s1    = RegNext(req_phys_s0) // SAVVINA added, not existing in original metasys
  // ------------------------------ stage 2 ------------------------------------
  // prq enqueue logic
  // we enqueue 2 cycles later w.r.t core request since d$ could
  // potentially nack the request or generate an exception at cycle 2

  val req_paddr_s2 = Mux(RegNext(req_phys_s1), RegNext(req_vaddr_s1), io.core_snoop.s2_paddr)  // Returned by dcache // SAVVINA changed from RegNext(req_paddr_s1)

  val core_s2_nack   = io.core_snoop.s2_nack
  val core_s2_xcpt   = io.core_snoop.s2_xcpt.asUInt.orR
  val s2_dc_miss     = io.core_snoop.miss

  // Three reasons for the request to be shotdown
  // 1 - Core kills the originating memory access
  // 2 - Access resulted in exception in D$
  // 3 - D$ was busy with handling coherence or did not have
  //      any free MSHRs left so it nacked the request. 
  val prq_enq_valid  = RegNext(paddr_s1_valid) && RegNext(~io.core_snoop.s1_kill) &&
                        ~core_s2_nack && ~core_s2_xcpt //&& s2_dc_miss
/*
  printf("\n")
  printf("S0 V:%d VA:0x%x\n",req_valid_s0,req_addr_s0)
  printf("S1 V:%d PA:0x%x K:%d\n",paddr_s1_valid,req_paddr_s1,io.core.s1_kill)
  printf("S2 V:%d PA:0x%x NACK:%d MISS:%d XCPT:%d ENQ:%d\n",RegNext(paddr_s1_valid),req_paddr_s2,core_s2_nack,s2_dc_miss,core_s2_xcpt,prq_enq_valid)
  printf("\n")
*/
  prq.io.enq.paddr := req_paddr_s2 // SAVVINA changed it from RegNext(req_paddr_s1) to req_paddr_s2
  prq.io.enq.vaddr := RegNext(req_vaddr_s1)
  prq.io.enq.size  := RegNext(req_size_s1)
  prq.io.enq.cmd   := RegNext(req_cmd_s1)

  prq.io.enq.valid := prq_enq_valid && prq.io.enq.ready

  // Number of lost requests
  io.debug1           := prq_enq_valid && !prq.io.enq.ready
  io.debug2           := prq_enq_valid

  io.lookup.req.valid := prq.io.deq.valid
  prq.io.deq.ready    := io.lookup.req.ready
  io.lookup.req.paddr := prq.io.deq.paddr

}

class ALUStressorTop(opcodes: OpcodeSet, albSize: Int = 32, atomGranularity: Int = 20, isPhysical: Boolean = false)(implicit p: Parameters) extends LazyRoCC(opcodes) {
  override lazy val module = new ALUStressorTopModule(this, albSize, atomGranularity, isPhysical)
  override val atlNode = TLClientNode(Seq(TLClientPortParameters(Seq(TLClientParameters("ALUStressorModule")))))
}

class ALUStressorTopModule (outer: ALUStressorTop,
  albSize: Int = 32, atomGranularity: Int = 9, isPhysical: Boolean = false) (implicit p: Parameters)
  extends LazyRoCCModuleImp(outer){

  val (tl_out, edgesOut) = outer.atlNode.out(0)
  implicit val edge = edgesOut
  //val tlb_wrapper = Module(new AtomTLBWrapper) // SAVVINA commented out and replaced with next line
  val tlb = Module(new DecoupledTLB(1,4)) // SAVVINA no need for 2 clients, prefetcher doesn't use TLB
  val ctrl    = Module(new AtomAddressMapController(atomGranularity, isPhysical))
  val stress  = Module(new ALUStressor)

  // RoCC related connections
  // ctrl.io                     <>  DontCare // SAVVINA commented out (uncommented in original metasys)
  ctrl.io.cmd      <>  io.cmd
  io.resp          <>  ctrl.io.resp
  io.interrupt     := ctrl.io.interrupt
  io.busy          := ctrl.io.busy

/* SAVVINA comment lines 883-887 and replace with lines 889-890 */
  // connect modueles to the TLB they are sharing
  /*tlb_wrapper.io.amu          <>  ctrl.io.tlb
  tlb_wrapper.io.amu_status   :=  ctrl.io.mstatus
  tlb_wrapper.io.pref         <>  DontCare
  // tlb_wrapper.io.pref_status  :=  pref.io.status
  io.ptw.head                 <>  tlb_wrapper.io.ptw*/

  tlb.io <> ctrl.io.tlb
  io.ptw.head <> tlb.io.ptw

  // bank memory ifaces
  io.mem                      <>  ctrl.io.mem
  io.mem_pref                 <>  DontCare

  // snoop core's memory requests
  stress.io.core_snoop        <>  io.core_snoop

  val lookup = Module(new BuffedALU(atomGranularity,albSize,isPhysical))
  io.mem_amulookup            <>  lookup.io.mem
  // Connect AMU_LOOKUP module to the prefetcher
  // and supply it with acr
  lookup.io.acrbase           := ctrl.io.acr
  lookup.io.phybase           := ctrl.io.phybase
  lookup.io.lookup            <> stress.io.lookup
  lookup.io.map_update_id     := ctrl.io.map_update_id
  lookup.io.map_update_addr   := ctrl.io.map_update_addr
  lookup.io.map_update_valid  := ctrl.io.map_update_valid
  
  io.core_snoop.dbg2          := lookup.io.dbg2    // ALB Hits
  io.core_snoop.dbg1          := lookup.io.dbg1    // ALB Misses
  io.core_snoop.dbg3          := stress.io.debug1  // number of missed l1 requests
  io.core_snoop.dbg4          := stress.io.debug2  // number of interceptable l1 requests

}

class StressArbitrator()(implicit p: Parameters) extends Module{
  val io = IO(new Bundle{
    val req1 = new AMULookUpIO
    val req2 = new AMULookUpIO
    val sel  = Flipped(new AMULookUpIO)
  })

  // Make sure no unset signals remain!!

  val next_pref = Reg(Bool())
  val r1        = io.req1.req
  val r2        = io.req2.req
  val s         = io.sel.req
  
  s.valid  := false.B
  r1.ready := false.B
  r2.ready := false.B

  when(s.valid && s.ready)
  {
    next_pref := !next_pref
  }

  // Statically prioritize PTW Lookups
  when(r1.valid && r2.valid)
  {
    s.valid   := r2.valid
    s.paddr   := r2.paddr
    r2.ready  := s.ready
  }.elsewhen(r1.valid)
  {
    s.valid   := r1.valid
    s.paddr   := r1.paddr
    r1.ready  := s.ready  
    printf("Select core\n") 
  }.elsewhen(r2.valid)
  {
    s.valid   := r2.valid
    s.paddr   := r2.paddr
    r2.ready  := s.ready
    printf("Select ptw\n")
  }.otherwise
  {
    s.valid   := false.B
    s.paddr   := 0.U
  }
  
  io.req1.resp <> DontCare
  io.req2.resp <> DontCare
  io.sel.resp <> DontCare
}

class ALUDualStressorTop(opcodes: OpcodeSet, albSize: Int = 32, atomGranularity: Int = 20, isPhysical: Boolean = false, dontblock: Boolean = false)(implicit p: Parameters) extends LazyRoCC(opcodes) {
  override lazy val module = new ALUDualStressorTopModule(this, albSize, atomGranularity, isPhysical, dontblock)
  override val atlNode = TLClientNode(Seq(TLClientPortParameters(Seq(TLClientParameters("ALUDualStressorModule")))))
}

class ALUDualStressorTopModule (outer: ALUDualStressorTop,
  albSize: Int = 32, atomGranularity: Int = 9, isPhysical: Boolean = false, dontblock: Boolean = false) (implicit p: Parameters)
  extends LazyRoCCModuleImp(outer){

  val (tl_out, edgesOut) = outer.atlNode.out(0)
  implicit val edge = edgesOut
  //val tlb_wrapper = Module(new AtomTLBWrapper) // SAVVINA commented out and replaced with next line
  val tlb = Module(new DecoupledTLB(1,4)) // SAVVINA no need for 2 clients, prefetcher doesn't use TLB
  val ctrl    = Module(new AtomAddressMapController(atomGranularity,isPhysical))
  val stress_core  = Module(new ALUStressor(dontblock))
  val stress_ptw   = Module(new ALUStressor(dontblock))

  // RoCC related connections
  // ctrl.io                     <>  DontCare // SAVVINA commented out (uncommented in original metasys)
  ctrl.io.cmd    <>  io.cmd
  io.resp        <>  ctrl.io.resp
  io.interrupt   :=  ctrl.io.interrupt
  io.busy        :=  ctrl.io.busy

/* SAVVINA comment lines 883-887 and replace with lines 889-890 */
  // connect modueles to the TLB they are sharing
  /*tlb_wrapper.io.amu          <>  ctrl.io.tlb
  tlb_wrapper.io.amu_status   :=  ctrl.io.mstatus
  tlb_wrapper.io.pref         <>  DontCare
  // tlb_wrapper.io.pref_status  :=  pref.io.status
  io.ptw.head                 <>  tlb_wrapper.io.ptw*/

  tlb.io <> ctrl.io.tlb
  io.ptw.head <> tlb.io.ptw

  // bank memory ifaces
  io.mem                      <>  ctrl.io.mem
  io.mem_pref                 <>  DontCare

  // snoop core's memory requests
  stress_core.io.core_snoop   <>  io.core_snoop
  stress_ptw.io.core_snoop    <>  io.ptw_snoop

  // Arbitrate between two stressor modules
  val lookup_arb = Module(new StressArbitrator())
  lookup_arb.io.req1 <> stress_core.io.lookup
  lookup_arb.io.req2 <> stress_ptw.io.lookup
  val lookup = Module(new BuffedALU(atomGranularity,albSize,isPhysical))
  lookup.io.lookup            <> lookup_arb.io.sel
  io.mem_amulookup            <> lookup.io.mem
  // Connect AMU_LOOKUP module to the prefetcher
  // and supply it with acr
  lookup.io.acrbase           := ctrl.io.acr
  lookup.io.phybase           := ctrl.io.phybase
  lookup.io.map_update_id     := ctrl.io.map_update_id
  lookup.io.map_update_addr   := ctrl.io.map_update_addr
  lookup.io.map_update_valid  := ctrl.io.map_update_valid
  
  io.core_snoop.dbg2          := lookup.io.dbg2    // ALB Hits
  io.core_snoop.dbg1          := lookup.io.dbg1    // ALB Misses
  // TODO modify below
  io.core_snoop.dbg3          := stress_ptw.io.debug1
  io.core_snoop.dbg4          := stress_ptw.io.debug2

}

class ALUDualStressor2ALBTop(opcodes: OpcodeSet, albSize1: Int = 32, albSize2: Int = 32, atomGranularity: Int = 20, isPhysical: Boolean = false)(implicit p: Parameters) extends LazyRoCC(opcodes) {
  override lazy val module = new ALUDualStressor2ALBTopModule(this, albSize1, albSize2, atomGranularity, isPhysical)
  override val atlNode = TLClientNode(Seq(TLClientPortParameters(Seq(TLClientParameters("ALUDualStressor2ALBModule")))))
}

class StressArbitratorDual()(implicit p: Parameters) extends Module{
  val io = IO(new Bundle{
    val req1 = new AMULookUpIO
    val req2 = new AMULookUpIO
    val sel  = Flipped(new AMUDualALBLookUpIO)
  })

  // Make sure no unset signals remain!!

  val next_pref = Reg(Bool())
  val r1        = io.req1.req
  val r2        = io.req2.req
  val s         = io.sel.req
  
  s.valid  := false.B
  r1.ready := false.B
  r2.ready := false.B

  when(s.valid && s.ready)
  {
    next_pref := !next_pref
  }

  s.moduleid := true.B
  // Statically prioritize PTW Lookups
  when(r1.valid && r2.valid)
  {
    s.valid   := r2.valid
    s.paddr   := r2.paddr
    s.moduleid := true.B
    r2.ready  := s.ready
  }.elsewhen(r1.valid)
  {
    s.valid   := r1.valid
    s.paddr   := r1.paddr
    s.moduleid := false.B
    r1.ready  := s.ready  
    printf("Select core\n") 
  }.elsewhen(r2.valid)
  {
    s.valid   := r2.valid
    s.paddr   := r2.paddr
    s.moduleid := true.B
    r2.ready  := s.ready
    printf("Select ptw\n")
  }.otherwise
  {
    s.valid   := false.B
    s.paddr   := 0.U
  }
  
  io.req1.resp <> DontCare
  io.req2.resp <> DontCare
  io.sel.resp <> DontCare
}

// class DualArbitrator()(implicit p: Parameters) extends Module{
//   val io = IO(new Bundle{
//     val requestor1 = new AMULookUpIO
//     val requestor2 = new AMULookUpIO
//     val sel  = Flipped(new AMUDualALBLookUpReturnModuleIdIO)
//   })

//   // Make sure no unset signals remain!!

//   val req1        = io.requestor1.req
//   val resp1       = io.requestor1.resp

//   val req2        = io.requestor2.req
//   val resp2       = io.requestor2.resp

//   val sreq         = io.sel.req
//   val sresp        = io.sel.resp
  
//   //s.valid  := false.B
//   //req1.ready := false.B
//   //req2.ready := false.B

//   resp1.valid := false.B
//   resp2.valid := false.B

//   resp1.miss := false.B
//   resp2.miss := false.B

//   resp1.atom_id := 0.U
//   resp2.atom_id := 0.U

//   resp1.xcpt := false.B
//   resp2.xcpt := false.B

//   //s.moduleid := true.B
//   // Prioritize requestor 2
//   when(req1.valid && req2.valid)
//   {
//     s.valid   := req2.valid
//     s.paddr   := req2.paddr
//     s.moduleid := false.B
//     req2.ready  := s.ready
//   }.elsewhen(req1.valid)
//   {
//     s.valid   := req1.valid
//     s.paddr   := req1.paddr
//     s.moduleid := true.B
//     req1.ready  := s.ready   
//   }.elsewhen(req2.valid)
//   {
//     s.valid   := req2.valid
//     s.paddr   := req2.paddr
//     s.moduleid := false.B
//     req2.ready  := s.ready
//   }.otherwise
//   {
//     s.valid   := false.B
//     s.paddr   := 0.U
//     s.moduleid := false.B
//     req1.ready := false.B
//     req2.ready := false.B
//   }
  
//   // SAVVINA: there is an assumption here that we can't get xcpt for 
//   //one requestor and resp.valid for the other requestor at the same time.
  
//   when(sresp.valid || sresp.xcpt)
//   {
//     when(sresp.moduleid){
//       resp1.valid := sresp.valid
//       resp1.miss  := sresp.miss
//       resp1.atom_id := sresp.atom_id
//       resp1.xcpt := sresp.xcpt
//     }.otherwise{
//       resp2.valid := sresp.valid
//       resp2.miss  := sresp.miss
//       resp2.atom_id := sresp.atom_id
//       resp2.xcpt := sresp.xcpt
//     }
//   }
// }

class DualArbitrator()(implicit p: Parameters) extends Module {
  val io = IO(new Bundle {
    val requestor1 = new AMULookUpIO
    val requestor2 = new AMULookUpIO
    val sel        = Flipped(new AMUDualALBLookUpReturnModuleIdIO)
  })

  val req1  = io.requestor1.req
  val resp1 = io.requestor1.resp
  val req2  = io.requestor2.req
  val resp2 = io.requestor2.resp
  val sreq  = io.sel.req
  val sresp = io.sel.resp

  // -------------------------
  // Request arbitration
  // -------------------------
  // Priority: requestor2 > requestor1
  sreq.valid    := req2.valid || req1.valid
  sreq.paddr    := Mux(req2.valid, req2.paddr, req1.paddr)
  sreq.moduleid := !req2.valid 

  // Ready signals (only the chosen one sees ready)
  req1.ready := sreq.ready && !req2.valid
  req2.ready := sreq.ready && req2.valid

  resp1.valid   := sresp.valid && sresp.moduleid
  resp1.miss    := sresp.miss && sresp.moduleid
  resp1.atom_id := Mux(sresp.moduleid, sresp.atom_id, 0.U)
  resp1.xcpt    := sresp.xcpt && sresp.moduleid

  resp2.valid   := sresp.valid && !sresp.moduleid
  resp2.miss    := sresp.miss && !sresp.moduleid
  resp2.atom_id := Mux(sresp.moduleid, 0.U, sresp.atom_id)
  resp2.xcpt    := sresp.xcpt && !sresp.moduleid
}


class ALUDualStressor2ALBTopModule (outer: ALUDualStressor2ALBTop,
  albSize1: Int = 32, albSize2: Int = 32, atomGranularity: Int = 9, isPhysical: Boolean = false) (implicit p: Parameters)
  extends LazyRoCCModuleImp(outer){

  val (tl_out, edgesOut) = outer.atlNode.out(0)
  implicit val edge = edgesOut
  //val tlb_wrapper = Module(new AtomTLBWrapper) // SAVVINA commented out and replaced with next line
  val tlb = Module(new DecoupledTLB(1,4)) // SAVVINA no need for 2 clients, prefetcher doesn't use TLB
  val ctrl    = Module(new AtomAddressMapController(atomGranularity,isPhysical))
  val stress_core  = Module(new ALUStressor)
  val stress_ptw   = Module(new ALUStressor)

  // RoCC related connections
  //ctrl.io                     <>  DontCare // SAVVINA commented out (uncommented in original metasys)
  ctrl.io.cmd                 <>  io.cmd
  io.resp                     <>  ctrl.io.resp
  io.interrupt := ctrl.io.interrupt
  io.busy := ctrl.io.busy

  /* SAVVINA comment lines 883-887 and replace with lines 889-890 */

  // connect modueles to the TLB they are sharing
  /*tlb_wrapper.io.amu          <>  ctrl.io.tlb
  tlb_wrapper.io.amu_status   :=  ctrl.io.mstatus
  tlb_wrapper.io.pref         <>  DontCare
  io.ptw.head                 <>  tlb_wrapper.io.ptw */

  tlb.io <> ctrl.io.tlb
  io.ptw.head <> tlb.io.ptw

  // bank memory ifaces
  io.mem                      <>  ctrl.io.mem
  io.mem_pref                 <>  DontCare

  // snoop core's memory requests
  stress_core.io.core_snoop   <>  io.core_snoop
  stress_ptw.io.core_snoop    <>  io.ptw_snoop

  // Arbitrate between two stressor modules
  val lookup_arb = Module(new StressArbitratorDual())
  lookup_arb.io.req1 <> stress_core.io.lookup
  lookup_arb.io.req2 <> stress_ptw.io.lookup
  val lookup = Module(new BuffedALUDualALB(atomGranularity,albSize1,albSize2,isPhysical))
  lookup.io.lookup            <> lookup_arb.io.sel
  io.mem_amulookup            <> lookup.io.mem
  // Connect AMU_LOOKUP module to the prefetcher
  // and supply it with acr
  lookup.io.acrbase           := ctrl.io.acr
  lookup.io.phybase           := ctrl.io.phybase
  lookup.io.map_update_id     := ctrl.io.map_update_id
  lookup.io.map_update_addr   := ctrl.io.map_update_addr
  lookup.io.map_update_valid  := ctrl.io.map_update_valid
  
  io.core_snoop.dbg2          := lookup.io.dbg2    // ALB Hits
  io.core_snoop.dbg1          := lookup.io.dbg1    // ALB Misses
  // TODO modify below
  io.core_snoop.dbg3          := stress_ptw.io.debug1
  io.core_snoop.dbg4          := stress_ptw.io.debug2

}