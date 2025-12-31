package tile

import Chisel.{RegInit, UInt, Vec, log2Up, printf}
import chisel3._
import chisel3.util._
import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.diplomacy.LazyModule
import freechips.rocketchip.rocket._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.tile._
// import ClientAmuRequest._
import freechips.rocketchip.diplomacy.{LazyModule, LazyModuleImp, IdRange}

// We may need to share the readport
// or this might get huge
class PrivateAttributeTable (implicit p: Parameters) extends Module {
  val io = IO(new Bundle {
    // val wdata = Input(UInt(32.W)) // SAVVINA: commented out
    val wdata = Input(UInt(64.W)) 
    // val attindex = Input(UInt(4.W)) // SAVVINA: commented out
    val attindex = Input(UInt(3.W)) // SAVVINA: commented out
    // val attindex = Input(UInt(3.W)) // SAVVINA: added
    val wen   =  Input(Bool())
    val ren   =  Input(Bool())
    val addr = Input(UInt(8.W))
    // val atom_prim = Output(UInt(32.W)) // SAVVINA: commented out
    val atom_prim = Output(UInt(64.W))
    val addr_pref = Input(UInt(8.W))
    val atom_pref = Output(UInt(512.W))
  })
  val tableSize = 256
  val atomBytes = 64
  //val atomBeats = 4 // SAVVINA: commented out
  val atomBeats = 8 // SAVVINA: added
  //val atomBits  = 32 // SAVVINA: commented out
  val atomBits  = 64 // SAVVINA: added

  val wr_mask = Vec((1.U << io.attindex).asBools) // SAVVINA comment: If attindex = 2, then wr_mask = [false, false, true, false, ...]
  val wr_data = Wire(Vec(atomBytes/atomBeats, UInt(atomBits.W)))
  for (i <- 0 until atomBytes/atomBeats){
    wr_data(i) := io.wdata
  }

  // val pat = Mem(tableSize, Vec(atomBytes/atomBeats, UInt(atomBits.W))) // SAVVINA: commented out
  val pat = SyncReadMem(tableSize, Vec(atomBytes/atomBeats, UInt(atomBits.W))) // SAVVINA: modified compared to MetaSys (where it was Mem)
  // Sequential read, sequential write
  // val pat = SyncReadMem(tableSize * (atomBytes/atomBeats), UInt(atomBits.W)) // SAVVINA: added
  //val pat = RegInit(Vec.fill(tableSize) {Vec(Seq.fill(atomBytes/atomBeats)(0.U(atomBits.W)))})

  //val pat = RegInit(Vec(Seq.fill(tableSize)(0.U(512.W))))
  //def access(addr: UInt, offset: UInt) = pat(~addr)(31.U+offset<<5.U,offset<<5.U)

  def access(addr: UInt) = pat(addr).asUInt // SAVVINA: commented out
  // def access(addr: UInt) = pat(addr * (atomBytes/atomBeats).U).asUInt 

  val out = Reg(UInt(512.W))

  val debugSig1 = RegNext(io.wen)


  when(io.wen) { // If write enable and activate -> write activation
    // TODO make it clear that "fatom" does not work anymore
    pat.write(io.addr, wr_data, wr_mask) // SAVVINA: commented out
    // pat.write(io.addr * (atomBytes / atomBeats).U, VecInit(wr_data.zip(wr_mask).map { case (d, m) => Mux(m, d, 0.U) }))

    // /* SAVVINA: added: start */
    // for (i <- 0 until (atomBytes / atomBeats)) {
    //   when(wr_mask(i)) {
    //     pat.write(io.addr * (atomBytes / atomBeats).U + i.U, wr_data(i))
    //   }
    // }
    // /* SAVVINA: added: end */

    //pat(io.addr)(io.attindex) := io.wdata 
    printf("Writing to PAT --- aid:0x%x off:0x%x attr:0x%x\n", io.addr, io.attindex, io.wdata)
  }
  when (debugSig1) {
    for(i <- 0 until tableSize){
      printf("Entry%d: data:%x\n", i.U, pat(i.U).asUInt)      // SAVVINA: commented out
      // printf("Entry%d: data:%x\n", i.U, pat(i.U*(atomBytes/atomBeats)).asUInt)
    }
  }

  //when(io.ren)
  //{
    //out := access(io.addr)
    // out := pat.read(io.addr * (atomBytes / atomBeats).U).asUInt // SAVVINA: commented out
    out := pat.read(io.addr).asUInt // SAVVINA: commented out

    // /* SAVVINA: added: start */  
    // val readData = Wire(Vec(atomBytes / atomBeats, UInt(atomBits.W)))
  
    // for (i <- 0 until (atomBytes / atomBeats)) {
    //   readData(i) := pat.read(io.addr * (atomBytes / atomBeats).U + i.U)
    // }
    // out := readData.asUInt
    // /* SAVVINA: added: end */

  //}

  // io.atom_pref := pat.read(io.addr_pref * (atomBytes / atomBeats).U).asUInt // SAVVINA: commented out
  io.atom_pref := pat.read(io.addr_pref).asUInt // SAVVINA: commented out

  // /* SAVVINA: added: start */ 
  // val prefData = Wire(Vec(atomBytes / atomBeats, UInt(atomBits.W)))
  // for (i <- 0 until (atomBytes / atomBeats)) {
  //   prefData(i) := pat.read(io.addr_pref * (atomBytes / atomBeats).U + i.U)
  // }
  // io.atom_pref := prefData.asUInt
  // /* SAVVINA: added: end */

  io.atom_prim := out
}

class PrivateAttributeTable2048X11CycleRDLatencyNoren (implicit p: Parameters) extends Module {
  val io = IO(new Bundle {
    val wen   =  Input(Bool())
    val waddr = Input(UInt(11.W))
    val wdata = Input(UInt(64.W)) 
    val addr_pref = Input(UInt(11.W))
    val atom_pref = Output(UInt(64.W))
  })
  
  val tableSize = 2048

  val pat = SyncReadMem(tableSize, UInt(64.W)) 

  when(io.wen) {
    pat.write(io.waddr, io.wdata)
  }


  io.atom_pref := pat.read(io.addr_pref).asUInt // Read with 1 cycle latency

}

class PrivateAttributeTable512X11CycleRDLatencyNoren (implicit p: Parameters) extends Module {
  val io = IO(new Bundle {
    val wen   =  Input(Bool())
    val waddr = Input(UInt(9.W))
    val wdata = Input(UInt(64.W)) 
    val addr_pref = Input(UInt(9.W))
    val atom_pref = Output(UInt(64.W))
  })
  
  val tableSize = 512

  val pat = SyncReadMem(tableSize, UInt(64.W)) 

  when(io.wen) {
    pat.write(io.waddr, io.wdata)
  }


  io.atom_pref := pat.read(io.addr_pref).asUInt // Read with 1 cycle latency

}

class PrivateAttributeTable1024X11CycleRDLatencyNoren (implicit p: Parameters) extends Module {
  val io = IO(new Bundle {
    val wen   =  Input(Bool())
    val waddr = Input(UInt(10.W))
    val wdata = Input(UInt(64.W)) 
    val addr_pref = Input(UInt(10.W))
    val atom_pref = Output(UInt(64.W))
  })
  
  val tableSize = 1024

  val pat = SyncReadMem(tableSize, UInt(64.W)) 

  when(io.wen) {
    pat.write(io.waddr, io.wdata)
  }


  io.atom_pref := pat.read(io.addr_pref).asUInt // Read with 1 cycle latency

}

/* This module selects its corresponding register, based on the value of*/
class AtomAddressMapRegACR(implicit p: Parameters) extends Module {
  val io = IO(new Bundle {
    val wen    = Input(Bool())
    val wdata  = Input(UInt(39.W))
    val waddr  = Input(UInt(4.W))
    val clear  = Input(Bool())
    val len    = Output(UInt(39.W))
    val stride = Output(UInt(39.W))
    val rowCnt = Output(UInt(39.W))
    val acr    = Output(UInt(39.W))
    val pat_va    = Output(UInt(39.W))
  })

  val RegACR    = RegInit(0.U(39.W))
  val RegLen    = RegInit(0.U(39.W))
  val RegStride = RegInit(0.U(39.W))
  val RegRowCnt = RegInit(0.U(39.W))
  val RegPat    = RegInit(0.U(39.W)) // Reg for PAT Address

  when(io.clear)
  {
    RegACR := 0.U
  }

  when(io.wen && io.waddr === 0.U)
  {
    RegACR := io.wdata
    printf("Writing to ACR, %d\n", io.wdata)
  }

  when(io.wen && io.waddr === 1.U)
  {
    RegLen := io.wdata
  }

  when(io.wen && io.waddr === 2.U)
  {
    RegStride := io.wdata
  }

  when(io.wen && io.waddr === 3.U)
  {
    RegRowCnt := io.wdata
  }

/*
  when(io.wen && io.waddr === 4.U)
  {
    RegPat := io.wdata

  }
*/


  io.acr    := RegACR
  io.len    := RegLen
  io.stride := RegStride
  io.rowCnt := RegRowCnt
  io.pat_va := RegPat    // Register for the PAT address

}

class AtomAddressMapController (val atomGranularity: Int = 9, isPhysical: Boolean = false, isDummy: Boolean = false) 
(implicit p: Parameters, edge: TLEdgeOut) extends CoreModule {
  val io = IO(new Bundle {
    val cmd = Flipped(Decoupled(new RoCCCommand)) //Atom cmd

    val resp = Decoupled(new RoCCResponse)
    val mem = new HellaCacheIO
    val busy = Output(Bool())
    val interrupt = Output(Bool())
    val tlb = new FrontendTLBIO
    val mstatus = Output(new MStatus)

    val map_update_addr   = Output(UInt(40.W))
    val map_update_valid  = Output(Bool())
    val map_update_id     = Output(UInt(8.W)) 

    val acr = Output(UInt(39.W))
    val phybase = Output(UInt(39.W))
    val pref_pat_addr = Input(UInt(8.W))
    val pref_pat_atom = Output(UInt(512.W))

    // val pref_ast_addr   = Input(UInt(8.W)) // SAVVINA: comment out AST related stuff
    // val pref_ast_status = Output(Bool())   // SAVVINA: comment out AST related stuff
  })

  val n = 4
  val cmd = Queue(io.cmd)
  val atomID = RegInit(34.U(log2Up(n)-1))
  val str_vaddr = RegInit(33.U(32.W))
  val mem_wdata = RegInit(33.U(64.W))
  val str_paddr = RegInit(34.U(32.W))
  val bytes_left = RegInit(0.U(39.W))
  val rows_left = RegInit(0.U(39.W))
  val amu_vaddr = RegInit(0.U(39.W))
  val str_paddr_offset = RegInit(0.U(32.W))
  val fill_pat_address = RegInit(0.U(32.W))
  val pat_counter = RegInit(0.U(8.W))
  val n_atoms = RegInit(0.U(8.W))
  val is_map = RegInit(false.B)
  val is_unmap = RegInit(false.B)
  val is_map_2d = RegInit(false.B)
  val is_unmap_2d = RegInit(false.B)
  val verbose = RegInit(false.B)
  val mem_wen = RegInit(false.B)
  val atom_id = RegInit(0.U(8.W))
  val attribute = RegInit(0.U(64.W))
  val atomoffset = RegInit(0.U(4.W))

  if(isDummy)
    println("I won't spend time on mapping stuff")

  val status = RegEnable(cmd.bits.status, cmd.fire()) // && (is_amu_map))

  val inst = cmd.bits.inst
  val is_acr_read       = (inst.funct === 1.U)
  val is_acr_write      = (inst.funct === 2.U)
  val is_acr_clear      = (inst.funct === 3.U)
  val is_len_write      = (inst.funct === 4.U)
  val is_len_read       = (inst.funct === 5.U)
  val is_amu_map        = (inst.funct === 6.U)
  val is_amu_unmap      = (inst.funct === 7.U)
  val is_ast_activate   = (inst.funct === 8.U)
  val is_ast_deactivate = (inst.funct === 9.U)
  val is_ast_status     = (inst.funct === 10.U)
  val is_stride_write   = (inst.funct === 11.U)
  val is_stride_read    = (inst.funct === 12.U)
  val is_rowcnt_write   = (inst.funct === 13.U)
  val is_rowcnt_read    = (inst.funct === 14.U)
  val is_amu_map_2d     = (inst.funct === 15.U)
  val is_amu_unmap_2d   = (inst.funct === 16.U)
  val is_fatom          = (inst.funct === 17.U)
  val is_fatom_select   = (inst.funct === 18.U)
  val is_fatom_load     = (inst.funct === 19.U)
  
  val is_bc_load        = (inst.funct === 20.U)
  val is_bc_store       = (inst.funct === 21.U)
  val is_bc_atom_select = (inst.funct === 22.U)

  /* SAVVINA comment: According to the instruction called, contents of rs2 are stored in the corresponding register */

  val acr_reg = Module(new AtomAddressMapRegACR())
  acr_reg.io.wen := cmd.fire() && (is_acr_write || is_len_write || is_stride_write || is_rowcnt_write )
  acr_reg.io.waddr := Mux(is_acr_write, 0.U,
                      Mux(is_len_write, 1.U,
                      Mux(is_stride_write, 2.U,3.U)))
  acr_reg.io.wdata := cmd.bits.rs2
  acr_reg.io.clear := cmd.fire() && is_acr_clear

  io.acr := acr_reg.io.acr.asUInt
  io.phybase := acr_reg.io.rowCnt.asUInt

  /* SAVVINA comment: read the status of pref_addr and write 0 or 1 at address pointed by rs2, according to instruction called (activate/deactivate)*/

  /* SAVVINA comment out any AST related stuff */
/*
  val atomStatusTable = Module(new AtomStatusTable())

  atomStatusTable.io.pref_addr  := io.pref_ast_addr
  io.pref_ast_status            := atomStatusTable.io.pref_status

  atomStatusTable.io.wen := cmd.fire() && (is_ast_activate || is_ast_deactivate)
  atomStatusTable.io.AD := is_ast_activate
  atomStatusTable.io.waddr := cmd.bits.rs2
  atomStatusTable.io.raddr := cmd.bits.rs2
  val mem_atom_status = atomStatusTable.io.stat
*/

  val (s_idle :: s_translate :: s_fillpat :: s_fillpat_single :: s_readpat :: s_load_pat ::  s_send_pat_cache :: s_wait :: s_write :: s_finish :: Nil) = Enum(10)
  val state = RegInit(s_idle)
 

  val pat = Module (new PrivateAttributeTable())

  /* SAVVINA comment: Read attribute stored at addr_pref */
  pat.io.addr_pref := io.pref_pat_addr
  io.pref_pat_atom := pat.io.atom_pref

  when(cmd.fire() && (is_fatom_select) && ((state === s_idle) && !isDummy.B)){
    printf("fatom_select: ")
    printf("rs1:%d ", cmd.bits.rs1)
    printf("rs2:%d\n", cmd.bits.rs2)
    atom_id    := cmd.bits.rs1
    state := s_finish
  }

  when(cmd.fire() && (is_fatom_load) && ((state === s_idle) && !isDummy.B))
  {
    printf("fatom_load: ")
    printf("atomoffset:%d ", cmd.bits.rs1)
    printf("attribute:%d\n", cmd.bits.rs2)
    attribute := cmd.bits.rs2
    atomoffset := cmd.bits.rs1
    state := s_fillpat_single
  }

  when((state === s_fillpat_single)){
    state := s_finish
  }


  when(cmd.fire() && (is_fatom) && ((state === s_idle) && !isDummy.B))
  {
    printf("State change from IDLE to FILL_CACHE \n")
    printf("-- CMD (%d)  ", state)
    printf("v:%d  ", cmd.valid)
    printf("r:%d  ", cmd.ready)
    printf("b:%d  ", io.busy)
    printf("l:%d  ", acr_reg.io.len)
    printf("a:%d  ", acr_reg.io.acr)
    printf("rs1:%d  ", cmd.bits.rs1)
    printf("rs2:%d  ", cmd.bits.rs2)
    //printf("rd:%d  ", cmd.bits.rd)
    printf("inst:%d \n ", cmd.bits.inst.funct)
    fill_pat_address:= cmd.bits.rs1
    n_atoms:= cmd.bits.rs2
    state := s_fillpat
    pat_counter := 0.U
//    verbose := true.B

  }
   when((state === s_fillpat))
  {

    printf("Number of Atoms: %d\n", pat_counter)
    printf("Fill pat address: %d\n", fill_pat_address)
    state := s_readpat
    printf("============ MEM REQUEST ========== \n")
    printf("io.mem.req.valid := %d \n",io.mem.req.valid)
    printf("io.mem.req.bits.addr := %d\n",io.mem.req.bits.addr)
    printf("io.mem.req.bits.tag :=  %d\n",io.mem.req.bits.tag)
    printf(" io.mem.req.bits.cmd :=  %d\n",io.mem.req.bits.cmd)
    //printf(" io.mem.req.bits.typ :=  %d\n",io.mem.req.bits.typ)
    printf(" io.mem.req.bits.data :=  %d\n",io.mem.req.bits.data)
    printf(" io.mem.req.bits.phys := %d\n",io.mem.req.bits.phys)
    printf("===============================================\n")
    mem_wen := true.B
  }

  pat.io.wen := ((state === s_readpat) && io.mem.resp.valid ) || (state === s_fillpat_single)
  pat.io.addr := Mux((state === s_fillpat_single), atom_id, io.mem.resp.bits.data(7,0)) //not sure
  pat.io.wdata := Mux((state === s_fillpat_single), attribute, io.mem.resp.bits.data(31,8)) //not sure
  pat.io.ren := (state === s_send_pat_cache) && io.mem.req.fire() || state === s_load_pat
  pat.io.attindex := atomoffset

  when(state === s_readpat && io.mem.resp.valid )
  {

        printf("============== READ_PAT STATE ==============\n")
        printf("Data from memory: %d\n",io.mem.resp.bits.data)
        printf( "Pat_counter: %d\n", pat_counter )
        printf("===========================================\n")

        when ( pat_counter === (n_atoms ))
        {
              state:= s_finish
              pat_counter := 1.U

        }
        .otherwise {
              fill_pat_address := fill_pat_address + 4.U
              mem_wen := true.B
        }


  }


  when(state === s_load_pat){
      state := s_send_pat_cache
      printf( "Pat_counter: %d\n", pat_counter )
      printf("Primitive: %d\n", pat.io.atom_prim)
      mem_wen := true.B

  }

  when(state === s_send_pat_cache && io.mem.resp.valid )
  {

        when ( pat_counter === (n_atoms+1.U) )
        {
              state:= s_finish
        }
        .otherwise {

            printf("============= SEND_PAT_CACHE  STATE ==========\n")
            printf( "Pat_counter: %d\n", pat_counter )
            printf("Primitive: %d\n", pat.io.atom_prim)

            printf("io.mem.req.valid := %d \n",io.mem.req.valid)
            printf("io.mem.req.bits.addr := %d\n",io.mem.req.bits.addr)
            printf("io.mem.req.bits.tag :=  %d\n",io.mem.req.bits.tag)
            printf(" io.mem.req.bits.cmd :=  %d\n",io.mem.req.bits.cmd)
          //  printf(" io.mem.req.bits.typ :=  %d\n",io.mem.req.bits.typ)
            printf(" io.mem.req.bits.data :=  %d\n",io.mem.s1_data.data)
            printf(" io.mem.req.bits.phys := %d\n",io.mem.req.bits.phys)

            printf(" =============================================\n")

            pat_counter := pat_counter + 1.U
            mem_wen := true.B
        }
  }

  //----------------------------------------------------------------------------------------------//
  // TLB implementation
  // val tlb = Module(new FrontendTLB(1))//
  // io.ptw <> tlb.io.ptw
  // tlb.io.ptw.status := status

  io.mstatus := status
  val to_translate = RegInit(false.B)
  val tlb_sent = RegInit(true.B)
  val tlb_to_send = to_translate & !tlb_sent
  val ptw_error = false.B
  io.tlb.req.valid := tlb_to_send
  io.tlb.req.bits.vaddr := str_vaddr
  io.tlb.req.bits.passthrough := false.B
  io.tlb.req.bits.size := log2Ceil(4).U
  io.tlb.req.bits.cmd := M_XWR
  io.tlb.req.bits.prv := cmd.bits.status.prv // SAVVINA added, not existing in the original metasys
  //io.tlb.req.bits.prv := PRV.S.U // SAVVINA added, not existing in the original metasys
  io.tlb.req.bits.v := cmd.bits.status.v // SAVVINA added, not existing in the original metasys
  // io.tlb.req.bits.isLookup := false.B 

  io.tlb.resp.ready := true.B

  io.tlb.sfence.valid     := ptw_error
  io.tlb.sfence.bits.rs1  := true.B
  io.tlb.sfence.bits.rs2  := false.B
  io.tlb.sfence.bits.addr :=  str_vaddr
  io.tlb.sfence.bits.asid := 0.U
  io.tlb.sfence.bits.hv   := false.B // SAVVINA added, not existing in the original metasys
  io.tlb.sfence.bits.hg   := false.B // SAVVINA added, not existing in the original metasys

  io.tlb.kill := false.B // SAVVINA added, not existing in the original metasys
  
  when(io.tlb.req.fire())
  {
    tlb_sent := true.B
  }

  val timeout = RegInit(UInt(0,10.W))

  when(verbose)
  {
    timeout := timeout + UInt(1)
    //when(timeout > UInt(30)){
      //verbose := false.B
    //}
    printf("-- CMD (%d)  ", state)
    printf("v:%d  ", cmd.valid)
    printf("r:%d  ", cmd.ready)
    printf("b:%d  ", io.busy)
    printf("l:%d  ", acr_reg.io.len)
    printf("a:%d  ", acr_reg.io.acr)
    printf("rs1:%d  ", cmd.bits.inst.rs1)
    printf("rs2:%d  ", cmd.bits.inst.rs2)
    printf("rd:%d  ", cmd.bits.inst.rd)
    printf("inst:%d \n ", cmd.bits.inst.funct)

    printf("-- R (%d)  ", state)
    printf("v:%d  ", io.tlb.req.valid)
    printf("r:%d  ", io.tlb.req.ready)
    printf("va:%x  ", io.tlb.req.bits.vaddr)
    printf("ps:%d  \n", io.tlb.req.bits.passthrough)


    printf("-- S (%d)  ", state)
    printf("v:%d  ", io.tlb.resp.valid)
    printf("r:%d  ", io.tlb.resp.ready)
    printf("pa:%x  ", io.tlb.resp.bits.paddr)
    printf("ms:%d  ", io.tlb.resp.bits.miss)
    printf("ld:%d  ", io.tlb.resp.bits.pf.ld)
    printf("st:%d  ", io.tlb.resp.bits.pf.st)
    printf("cc:%d\n", io.tlb.resp.bits.cacheable)


    printf("-- MR (%d)  ", state)
    printf("v:%d  ", io.mem.req.valid)
    printf("r:%d  ", io.mem.req.ready)
    printf("ad:%x  ", io.mem.req.bits.addr)
    printf("tg:%d  ", io.mem.req.bits.tag)
    printf("cmd:%d  ", io.mem.req.bits.cmd)
  //  printf("typ:%d  ", io.mem.req.bits.typ)
    printf("data:%x  \n", io.mem.req.bits.data)


    printf("-- MS (%d)  ", state)
    printf("v: %d  ", io.mem.resp.valid)
    printf("r: %d \n", io.mem.resp.bits.data)

    printf("s2_xcpt : %d  ", io.mem.s2_xcpt.asUInt)
    printf("ma.st : %d  ", io.mem.s2_xcpt.ma.st)
    printf("ma.ld : %d  ", io.mem.s2_xcpt.ma.ld)
    printf("pf.st : %d  ", io.mem.s2_xcpt.pf.st)
    printf("pf.ld : %d  ", io.mem.s2_xcpt.pf.ld)
    printf("ae.st : %d  ", io.mem.s2_xcpt.ae.st)
    printf("ae.ld : %d\n", io.mem.s2_xcpt.ae.ld)



  }

    when(cmd.fire())
    {
      printf("-- CMDFIRE (%d)  ", state)
      printf("v:%d  ", cmd.valid)
      printf("r:%d  ", cmd.ready)
      printf("rs1:%d  ", cmd.bits.inst.rs1)
      printf("rs2:%d  ", cmd.bits.inst.rs2)
      printf("rd:%d  ", cmd.bits.inst.rd)
      printf("inst:%d\n", cmd.bits.inst.funct)
    }


  when(io.tlb.resp.fire())
  {
    to_translate := false.B
    str_paddr := io.tlb.resp.bits.paddr
  }

  //----------------------------------------------------------------------------------------------//
  when(cmd.fire() && (is_amu_map || is_amu_unmap || is_amu_map_2d || is_amu_unmap_2d) && (state === s_idle))
  {
    if(isDummy)
    {
      state := s_idle
    }else{
      state := s_translate
    }

    printf("State change from IDLE to TRANSLATE\n")
    printf("-- CMD (%d)  ", state)
    printf("vs_fillpat:%d  ", cmd.valid)
    printf("r:%d  ", cmd.ready)
    printf("b:%d  ", io.busy)
    printf("l:%d  ", acr_reg.io.len)
    printf("a:%d  ", acr_reg.io.acr)
    printf("rs1:%d  ", cmd.bits.inst.rs1)
    printf("rs2:%d  ", cmd.bits.inst.rs2)
    printf("rd:%d  ", cmd.bits.inst.rd)
    printf("inst:%d \n ", cmd.bits.inst.funct)
    str_vaddr := cmd.bits.rs1
    atomID    := cmd.bits.rs2(log2Up(n)-1,0)
    mem_wdata       := (cmd.bits.rs2 & "h00000000000000ff".U) |
      ((cmd.bits.rs2 << 8 ).asUInt & "h000000000000ff00".U) |
      ((cmd.bits.rs2 << 16).asUInt & "h0000000000ff0000".U) |
      ((cmd.bits.rs2 << 24).asUInt & "h00000000ff000000".U) |
      ((cmd.bits.rs2 << 32).asUInt & "h000000ff00000000".U) |
      ((cmd.bits.rs2 << 40).asUInt & "h0000ff0000000000".U) |
      ((cmd.bits.rs2 << 48).asUInt & "h00ff000000000000".U) |
      ((cmd.bits.rs2 << 56).asUInt & "hff00000000000000".U)

    is_map := is_amu_map
    is_unmap := is_amu_unmap
    is_map_2d := is_amu_map_2d
    is_unmap_2d := is_amu_unmap_2d
    to_translate := true.B
    tlb_sent := false.B
    verbose := true.B
  }

  println("\nAtom Granularity " + atomGranularity + "\n")
  when((state === s_translate) && !to_translate)
  {
    printf("State change from TRANSLATE to WRITE\n")
    state := s_write
    str_paddr_offset := (str_paddr >> atomGranularity).asUInt
    if(isPhysical){
      amu_vaddr := ((str_paddr >> atomGranularity).asUInt + (acr_reg.io.acr).asUInt) + acr_reg.io.rowCnt//((str_paddr >> 9).asUInt() + (acr_reg.io.acr).asUInt())
    }
    else{
      amu_vaddr := ((str_paddr >> atomGranularity).asUInt + (acr_reg.io.acr).asUInt) //((str_paddr >> 9).asUInt() + (acr_reg.io.acr).asUInt())
    }
    bytes_left := acr_reg.io.len
    rows_left := Mux((is_amu_map || is_amu_unmap), 0.U, acr_reg.io.rowCnt - 1.U)
    mem_wen := true.B
  }


// TODO this never becomes valid when using physical stuff
  when((state === s_write) && io.mem.resp.valid)
  {
    when (bytes_left === 0.U && ((rows_left === 0.U && (is_map_2d || is_unmap_2d)) || is_map || is_unmap)) {

      printf("State change from WRITE to FINISH\n")
      state := s_finish
    }
    .elsewhen (bytes_left === 0.U && rows_left =/= 0.U && (is_map_2d || is_unmap_2d)) {
      bytes_left := acr_reg.io.len
      amu_vaddr := amu_vaddr + acr_reg.io.stride + 1.U
      rows_left := rows_left - 1.U
      mem_wen := true.B
    }
    .otherwise {
      str_paddr := str_paddr + atomGranularity.U
      amu_vaddr := amu_vaddr + 1.U
      mem_wen := true.B
    }
  }

  io.map_update_valid := state === s_write
  io.map_update_addr  := str_paddr
  io.map_update_id    := mem_wdata(7,0)

  when(state === s_finish)
  {
    printf("State change from FINISH to IDLE\n")
    state := s_idle
    verbose := false.B
    mem_wen := false.B
  }


  //----------------------------------------------------------------------------------------------//
  io.interrupt := false.B
  if(isDummy)
  {
    io.busy   := false.B
    cmd.ready := true.B
  }else
  {
    io.busy :=  cmd.valid  || (state =/= s_idle)
    cmd.ready :=
      is_acr_write || is_len_write || // Write can always go through immediately
      is_ast_activate || is_ast_deactivate ||
      is_stride_write || is_rowcnt_write ||
      (is_ast_status && io.resp.ready) ||
      (is_acr_read && io.resp.ready) ||
      (is_stride_read && io.resp.ready) ||
      (is_rowcnt_read && io.resp.ready) ||
      is_acr_clear ||
      (is_len_read && io.resp.ready) ||
      ( (is_amu_map || is_amu_unmap || is_amu_map_2d || is_amu_unmap_2d || is_fatom || is_fatom_select || is_fatom_load) && (state === s_idle ) )
  }

  // Memory Request Interface
  io.mem.req.valid := ((state === s_write) && mem_wen) || ((state === s_readpat) && mem_wen) || ((state === s_send_pat_cache) && mem_wen)
  io.mem.req.bits.addr := Mux((state === s_readpat),fill_pat_address,Mux(state === s_send_pat_cache || state === s_wait,fill_pat_address,amu_vaddr))
  io.mem.req.bits.tag := Mux((state === s_readpat),pat_counter,Mux(state === s_send_pat_cache || state === s_wait, pat_counter,0.U))
  io.mem.req.bits.cmd := Mux((state === s_readpat),M_XRD,Mux((state === s_send_pat_cache || state === s_wait), M_XWR, M_XWR))
  io.mem.req.bits.size := Mux((state === s_readpat) || (state === s_send_pat_cache || state === s_wait), log2Ceil(4).U, 0.U)
  //io.mem.req.bits.size := Mux((state === s_readpat) || (state === s_send_pat_cache || state === s_wait), log2Ceil(4).U, log2Ceil(8).U) // SAVVINA alternative 
  io.mem.req.bits.signed := false.B // SAVVINA added extra 
  io.mem.req.bits.dprv := cmd.bits.status.dprv
  //io.mem.req.bits.dprv := PRV.S.U // SAVVINA alternative
  io.mem.req.bits.dv := cmd.bits.status.dv
  // io.mem.req.bits.isLookup := false.B

  io.mem.req.bits.data := Mux((is_map || is_map_2d), mem_wdata.asUInt, Mux(state === s_send_pat_cache, pat.io.atom_prim, 0.U))
  //io.mem.req.bits.mask := ~(0.U(coreDataBytes.W))
  io.mem.req.bits.mask := ~(0.U(8.W)) // SAVVINA alternative
  // io.mem.req.bits.paddr := 0.U

  io.mem.req.bits.phys := isPhysical.B
  io.mem.req.bits.no_alloc := false.B // SAVVINA
  io.mem.req.bits.no_xcpt := isPhysical.B

  io.mem.s1_kill := false.B
  io.mem.s1_data.data := Mux(state === s_send_pat_cache, pat.io.atom_prim, 0.U)
  io.mem.s1_data.mask := 0.U
  // io.mem.s1_data.paddr := 0.U//SAVVINA
  io.mem.s2_kill := false.B // SAVVINA
  io.mem.keep_clock_enabled := false.B // SAVVINA


  when( io.mem.req.fire() && !(state === s_readpat) )
  {
    bytes_left := bytes_left - 1.U
    mem_wen := false.B
    val mem_s2_nack = RegNext(RegNext(io.mem.s2_nack))
    val mem_s2_xcpt = RegNext(RegNext(io.mem.s2_xcpt))
  }
  .elsewhen( io.mem.req.fire() && (state === s_readpat) )
  {
    pat_counter := pat_counter + 1.U
    mem_wen := false.B
  }


  when( io.mem.req.fire() && (state === s_send_pat_cache) )
  {
    printf("SEND_PAT_CACHE - Memory request fire\n")
    printf("Pat.io.atom_prim = %d \n", pat.io.atom_prim)
    mem_wen := false.B
  }

  io.resp.valid := (cmd.valid && (is_acr_read || is_len_read || is_stride_read || is_rowcnt_read || is_ast_status))
  io.resp.bits.rd := inst.rd
  /* SAVVINA commented out
  io.resp.bits.data := Mux(is_ast_status, mem_atom_status,
            Mux(is_stride_read, acr_reg.io.stride,
            Mux(is_rowcnt_read, acr_reg.io.rowCnt,
            Mux(is_acr_read, acr_reg.io.acr, acr_reg.io.len))))
            */

  io.resp.bits.data :=  Mux(is_stride_read, acr_reg.io.stride,
          Mux(is_rowcnt_read, acr_reg.io.rowCnt,
          Mux(is_acr_read, acr_reg.io.acr, acr_reg.io.len)))
}

class AtomAddressMapController39 (val atomGranularity: Int = 9, isPhysical: Boolean = false, isDummy: Boolean = false) 
(implicit p: Parameters, edge: TLEdgeOut) extends CoreModule {
  val io = IO(new Bundle {
    val cmd = Flipped(Decoupled(new RoCCCommand)) //Atom cmd

    val resp = Decoupled(new RoCCResponse)
    val mem = new HellaCacheIO
    val busy = Output(Bool())
    val interrupt = Output(Bool())
    val tlb = new FrontendTLBIO
    val mstatus = Output(new MStatus)

    val map_update_addr   = Output(UInt(40.W))
    val map_update_valid  = Output(Bool())
    val map_update_id     = Output(UInt(8.W)) 

    val acr = Output(UInt(39.W))
    val phybase = Output(UInt(39.W))
    val pref_pat_addr = Input(UInt(8.W))
    val pref_pat_atom = Output(UInt(512.W))

    // val pref_ast_addr   = Input(UInt(8.W)) // SAVVINA: comment out AST related stuff
    // val pref_ast_status = Output(Bool())   // SAVVINA: comment out AST related stuff
  })

  val n = 4
  val cmd = Queue(io.cmd)
  val atomID = RegInit(34.U(log2Up(n)-1))
  val str_vaddr = RegInit(33.U(39.W))
  val mem_wdata = RegInit(33.U(64.W))
  val str_paddr = RegInit(34.U(39.W))
  val bytes_left = RegInit(0.U(39.W))
  val rows_left = RegInit(0.U(39.W))
  val amu_vaddr = RegInit(0.U(39.W))
  val str_paddr_offset = RegInit(0.U(39.W))
  val fill_pat_address = RegInit(0.U(39.W))
  val pat_counter = RegInit(0.U(8.W))
  val n_atoms = RegInit(0.U(8.W))
  val is_map = RegInit(false.B)
  val is_unmap = RegInit(false.B)
  val is_map_2d = RegInit(false.B)
  val is_unmap_2d = RegInit(false.B)
  val verbose = RegInit(false.B)
  val mem_wen = RegInit(false.B)
  val atom_id = RegInit(0.U(8.W))
  val attribute = RegInit(0.U(64.W))
  val atomoffset = RegInit(0.U(4.W))

  if(isDummy)
    println("I won't spend time on mapping stuff")

  val status = RegEnable(cmd.bits.status, cmd.fire()) // && (is_amu_map))

  val inst = cmd.bits.inst
  val is_acr_read       = (inst.funct === 1.U)
  val is_acr_write      = (inst.funct === 2.U)
  val is_acr_clear      = (inst.funct === 3.U)
  val is_len_write      = (inst.funct === 4.U)
  val is_len_read       = (inst.funct === 5.U)
  val is_amu_map        = (inst.funct === 6.U)
  val is_amu_unmap      = (inst.funct === 7.U)
  val is_ast_activate   = (inst.funct === 8.U)
  val is_ast_deactivate = (inst.funct === 9.U)
  val is_ast_status     = (inst.funct === 10.U)
  val is_stride_write   = (inst.funct === 11.U)
  val is_stride_read    = (inst.funct === 12.U)
  val is_rowcnt_write   = (inst.funct === 13.U)
  val is_rowcnt_read    = (inst.funct === 14.U)
  val is_amu_map_2d     = (inst.funct === 15.U)
  val is_amu_unmap_2d   = (inst.funct === 16.U)
  val is_fatom          = (inst.funct === 17.U)
  val is_fatom_select   = (inst.funct === 18.U)
  val is_fatom_load     = (inst.funct === 19.U)
  
  val is_bc_load        = (inst.funct === 20.U)
  val is_bc_store       = (inst.funct === 21.U)
  val is_bc_atom_select = (inst.funct === 22.U)

  /* SAVVINA comment: According to the instruction called, contents of rs2 are stored in the corresponding register */

  val acr_reg = Module(new AtomAddressMapRegACR())
  acr_reg.io.wen := cmd.fire() && (is_acr_write || is_len_write || is_stride_write || is_rowcnt_write )
  acr_reg.io.waddr := Mux(is_acr_write, 0.U,
                      Mux(is_len_write, 1.U,
                      Mux(is_stride_write, 2.U,3.U)))
  acr_reg.io.wdata := cmd.bits.rs2
  acr_reg.io.clear := cmd.fire() && is_acr_clear

  io.acr := acr_reg.io.acr.asUInt
  io.phybase := acr_reg.io.rowCnt.asUInt

  /* SAVVINA comment: read the status of pref_addr and write 0 or 1 at address pointed by rs2, according to instruction called (activate/deactivate)*/

  /* SAVVINA comment out any AST related stuff */
/*
  val atomStatusTable = Module(new AtomStatusTable())

  atomStatusTable.io.pref_addr  := io.pref_ast_addr
  io.pref_ast_status            := atomStatusTable.io.pref_status

  atomStatusTable.io.wen := cmd.fire() && (is_ast_activate || is_ast_deactivate)
  atomStatusTable.io.AD := is_ast_activate
  atomStatusTable.io.waddr := cmd.bits.rs2
  atomStatusTable.io.raddr := cmd.bits.rs2
  val mem_atom_status = atomStatusTable.io.stat
*/

  val (s_idle :: s_translate :: s_fillpat :: s_fillpat_single :: s_readpat :: s_load_pat ::  s_send_pat_cache :: s_wait :: s_write :: s_finish :: Nil) = Enum(10)
  val state = RegInit(s_idle)
 

  val pat = Module (new PrivateAttributeTable())

  /* SAVVINA comment: Read attribute stored at addr_pref */
  pat.io.addr_pref := io.pref_pat_addr
  io.pref_pat_atom := pat.io.atom_pref

  when(cmd.fire() && (is_fatom_select) && ((state === s_idle) && !isDummy.B)){
    printf("fatom_select: ")
    printf("rs1:%d ", cmd.bits.rs1)
    printf("rs2:%d\n", cmd.bits.rs2)
    atom_id    := cmd.bits.rs1
    state := s_finish
  }

  when(cmd.fire() && (is_fatom_load) && ((state === s_idle) && !isDummy.B))
  {
    printf("fatom_load: ")
    printf("atomoffset:%d ", cmd.bits.rs1)
    printf("attribute:%d\n", cmd.bits.rs2)
    attribute := cmd.bits.rs2
    atomoffset := cmd.bits.rs1
    state := s_fillpat_single
  }

  when((state === s_fillpat_single)){
    state := s_finish
  }


  when(cmd.fire() && (is_fatom) && ((state === s_idle) && !isDummy.B))
  {
    printf("State change from IDLE to FILL_CACHE \n")
    printf("-- CMD (%d)  ", state)
    printf("v:%d  ", cmd.valid)
    printf("r:%d  ", cmd.ready)
    printf("b:%d  ", io.busy)
    printf("l:%d  ", acr_reg.io.len)
    printf("a:%d  ", acr_reg.io.acr)
    printf("rs1:%d  ", cmd.bits.rs1)
    printf("rs2:%d  ", cmd.bits.rs2)
    //printf("rd:%d  ", cmd.bits.rd)
    printf("inst:%d \n ", cmd.bits.inst.funct)
    fill_pat_address:= cmd.bits.rs1
    n_atoms:= cmd.bits.rs2
    state := s_fillpat
    pat_counter := 0.U
//    verbose := true.B

  }
   when((state === s_fillpat))
  {

    printf("Number of Atoms: %d\n", pat_counter)
    printf("Fill pat address: %d\n", fill_pat_address)
    state := s_readpat
    printf("============ MEM REQUEST ========== \n")
    printf("io.mem.req.valid := %d \n",io.mem.req.valid)
    printf("io.mem.req.bits.addr := %d\n",io.mem.req.bits.addr)
    printf("io.mem.req.bits.tag :=  %d\n",io.mem.req.bits.tag)
    printf(" io.mem.req.bits.cmd :=  %d\n",io.mem.req.bits.cmd)
    //printf(" io.mem.req.bits.typ :=  %d\n",io.mem.req.bits.typ)
    printf(" io.mem.req.bits.data :=  %d\n",io.mem.req.bits.data)
    printf(" io.mem.req.bits.phys := %d\n",io.mem.req.bits.phys)
    printf("===============================================\n")
    mem_wen := true.B
  }

  pat.io.wen := ((state === s_readpat) && io.mem.resp.valid ) || (state === s_fillpat_single)
  pat.io.addr := Mux((state === s_fillpat_single), atom_id, io.mem.resp.bits.data(7,0)) //not sure
  pat.io.wdata := Mux((state === s_fillpat_single), attribute, io.mem.resp.bits.data(31,8)) //not sure
  pat.io.ren := (state === s_send_pat_cache) && io.mem.req.fire() || state === s_load_pat
  pat.io.attindex := atomoffset

  when(state === s_readpat && io.mem.resp.valid )
  {

        printf("============== READ_PAT STATE ==============\n")
        printf("Data from memory: %d\n",io.mem.resp.bits.data)
        printf( "Pat_counter: %d\n", pat_counter )
        printf("===========================================\n")

        when ( pat_counter === (n_atoms ))
        {
              state:= s_finish
              pat_counter := 1.U

        }
        .otherwise {
              fill_pat_address := fill_pat_address + 4.U
              mem_wen := true.B
        }


  }


  when(state === s_load_pat){
      state := s_send_pat_cache
      printf( "Pat_counter: %d\n", pat_counter )
      printf("Primitive: %d\n", pat.io.atom_prim)
      mem_wen := true.B

  }

  when(state === s_send_pat_cache && io.mem.resp.valid )
  {

        when ( pat_counter === (n_atoms+1.U) )
        {
              state:= s_finish
        }
        .otherwise {

            printf("============= SEND_PAT_CACHE  STATE ==========\n")
            printf( "Pat_counter: %d\n", pat_counter )
            printf("Primitive: %d\n", pat.io.atom_prim)

            printf("io.mem.req.valid := %d \n",io.mem.req.valid)
            printf("io.mem.req.bits.addr := %d\n",io.mem.req.bits.addr)
            printf("io.mem.req.bits.tag :=  %d\n",io.mem.req.bits.tag)
            printf(" io.mem.req.bits.cmd :=  %d\n",io.mem.req.bits.cmd)
          //  printf(" io.mem.req.bits.typ :=  %d\n",io.mem.req.bits.typ)
            printf(" io.mem.req.bits.data :=  %d\n",io.mem.s1_data.data)
            printf(" io.mem.req.bits.phys := %d\n",io.mem.req.bits.phys)

            printf(" =============================================\n")

            pat_counter := pat_counter + 1.U
            mem_wen := true.B
        }
  }

  //----------------------------------------------------------------------------------------------//
  // TLB implementation
  // val tlb = Module(new FrontendTLB(1))//
  // io.ptw <> tlb.io.ptw
  // tlb.io.ptw.status := status

  io.mstatus := status
  val to_translate = RegInit(false.B)
  val tlb_sent = RegInit(true.B)
  val tlb_to_send = to_translate & !tlb_sent
  val ptw_error = false.B
  io.tlb.req.valid := tlb_to_send
  io.tlb.req.bits.vaddr := str_vaddr
  io.tlb.req.bits.passthrough := false.B
  io.tlb.req.bits.size := log2Ceil(4).U
  io.tlb.req.bits.cmd := M_XWR
  io.tlb.req.bits.prv := cmd.bits.status.prv // SAVVINA added, not existing in the original metasys
  //io.tlb.req.bits.prv := PRV.S.U // SAVVINA added, not existing in the original metasys
  io.tlb.req.bits.v := cmd.bits.status.v // SAVVINA added, not existing in the original metasys
  // io.tlb.req.bits.isLookup := false.B 

  io.tlb.resp.ready := true.B

  io.tlb.sfence.valid     := ptw_error
  io.tlb.sfence.bits.rs1  := true.B
  io.tlb.sfence.bits.rs2  := false.B
  io.tlb.sfence.bits.addr :=  str_vaddr
  io.tlb.sfence.bits.asid := 0.U
  io.tlb.sfence.bits.hv   := false.B // SAVVINA added, not existing in the original metasys
  io.tlb.sfence.bits.hg   := false.B // SAVVINA added, not existing in the original metasys

  io.tlb.kill := false.B // SAVVINA added, not existing in the original metasys
  
  when(io.tlb.req.fire())
  {
    tlb_sent := true.B
  }

  val timeout = RegInit(UInt(0,10.W))

  when(verbose)
  {
    timeout := timeout + UInt(1)
    //when(timeout > UInt(30)){
      //verbose := false.B
    //}
    printf("-- CMD (%d)  ", state)
    printf("v:%d  ", cmd.valid)
    printf("r:%d  ", cmd.ready)
    printf("b:%d  ", io.busy)
    printf("l:%d  ", acr_reg.io.len)
    printf("a:%d  ", acr_reg.io.acr)
    printf("rs1:%d  ", cmd.bits.inst.rs1)
    printf("rs2:%d  ", cmd.bits.inst.rs2)
    printf("rd:%d  ", cmd.bits.inst.rd)
    printf("inst:%d \n ", cmd.bits.inst.funct)

    printf("-- R (%d)  ", state)
    printf("v:%d  ", io.tlb.req.valid)
    printf("r:%d  ", io.tlb.req.ready)
    printf("va:%x  ", io.tlb.req.bits.vaddr)
    printf("ps:%d  \n", io.tlb.req.bits.passthrough)


    printf("-- S (%d)  ", state)
    printf("v:%d  ", io.tlb.resp.valid)
    printf("r:%d  ", io.tlb.resp.ready)
    printf("pa:%x  ", io.tlb.resp.bits.paddr)
    printf("ms:%d  ", io.tlb.resp.bits.miss)
    printf("ld:%d  ", io.tlb.resp.bits.pf.ld)
    printf("st:%d  ", io.tlb.resp.bits.pf.st)
    printf("cc:%d\n", io.tlb.resp.bits.cacheable)


    printf("-- MR (%d)  ", state)
    printf("v:%d  ", io.mem.req.valid)
    printf("r:%d  ", io.mem.req.ready)
    printf("ad:%x  ", io.mem.req.bits.addr)
    printf("tg:%d  ", io.mem.req.bits.tag)
    printf("cmd:%d  ", io.mem.req.bits.cmd)
  //  printf("typ:%d  ", io.mem.req.bits.typ)
    printf("data:%x  \n", io.mem.req.bits.data)


    printf("-- MS (%d)  ", state)
    printf("v: %d  ", io.mem.resp.valid)
    printf("r: %d \n", io.mem.resp.bits.data)

    printf("s2_xcpt : %d  ", io.mem.s2_xcpt.asUInt)
    printf("ma.st : %d  ", io.mem.s2_xcpt.ma.st)
    printf("ma.ld : %d  ", io.mem.s2_xcpt.ma.ld)
    printf("pf.st : %d  ", io.mem.s2_xcpt.pf.st)
    printf("pf.ld : %d  ", io.mem.s2_xcpt.pf.ld)
    printf("ae.st : %d  ", io.mem.s2_xcpt.ae.st)
    printf("ae.ld : %d\n", io.mem.s2_xcpt.ae.ld)



  }

    when(cmd.fire())
    {
      printf("-- CMDFIRE (%d)  ", state)
      printf("v:%d  ", cmd.valid)
      printf("r:%d  ", cmd.ready)
      printf("rs1:%d  ", cmd.bits.inst.rs1)
      printf("rs2:%d  ", cmd.bits.inst.rs2)
      printf("rd:%d  ", cmd.bits.inst.rd)
      printf("inst:%d\n", cmd.bits.inst.funct)
    }


  when(io.tlb.resp.fire())
  {
    to_translate := false.B
    str_paddr := io.tlb.resp.bits.paddr
  }

  //----------------------------------------------------------------------------------------------//
  when(cmd.fire() && (is_amu_map || is_amu_unmap || is_amu_map_2d || is_amu_unmap_2d) && (state === s_idle))
  {
    if(isDummy)
    {
      state := s_idle
    }else{
      state := s_translate
    }

    printf("State change from IDLE to TRANSLATE\n")
    printf("-- CMD (%d)  ", state)
    printf("vs_fillpat:%d  ", cmd.valid)
    printf("r:%d  ", cmd.ready)
    printf("b:%d  ", io.busy)
    printf("l:%d  ", acr_reg.io.len)
    printf("a:%d  ", acr_reg.io.acr)
    printf("rs1:%d  ", cmd.bits.inst.rs1)
    printf("rs2:%d  ", cmd.bits.inst.rs2)
    printf("rd:%d  ", cmd.bits.inst.rd)
    printf("inst:%d \n ", cmd.bits.inst.funct)
    str_vaddr := cmd.bits.rs1
    atomID    := cmd.bits.rs2(log2Up(n)-1,0)
    mem_wdata       := (cmd.bits.rs2 & "h00000000000000ff".U) |
      ((cmd.bits.rs2 << 8 ).asUInt & "h000000000000ff00".U) |
      ((cmd.bits.rs2 << 16).asUInt & "h0000000000ff0000".U) |
      ((cmd.bits.rs2 << 24).asUInt & "h00000000ff000000".U) |
      ((cmd.bits.rs2 << 32).asUInt & "h000000ff00000000".U) |
      ((cmd.bits.rs2 << 40).asUInt & "h0000ff0000000000".U) |
      ((cmd.bits.rs2 << 48).asUInt & "h00ff000000000000".U) |
      ((cmd.bits.rs2 << 56).asUInt & "hff00000000000000".U)

    is_map := is_amu_map
    is_unmap := is_amu_unmap
    is_map_2d := is_amu_map_2d
    is_unmap_2d := is_amu_unmap_2d
    to_translate := true.B
    tlb_sent := false.B
    verbose := true.B
  }

  println("\nAtom Granularity " + atomGranularity + "\n")
  when((state === s_translate) && !to_translate)
  {
    printf("State change from TRANSLATE to WRITE\n")
    state := s_write
    str_paddr_offset := (str_paddr >> atomGranularity).asUInt
    if(isPhysical){
      amu_vaddr := ((str_paddr >> atomGranularity).asUInt + (acr_reg.io.acr).asUInt) + acr_reg.io.rowCnt//((str_paddr >> 9).asUInt() + (acr_reg.io.acr).asUInt())
    }
    else{
      amu_vaddr := ((str_paddr >> atomGranularity).asUInt + (acr_reg.io.acr).asUInt) //((str_paddr >> 9).asUInt() + (acr_reg.io.acr).asUInt())
    }
    bytes_left := acr_reg.io.len
    rows_left := Mux((is_amu_map || is_amu_unmap), 0.U, acr_reg.io.rowCnt - 1.U)
    mem_wen := true.B
  }


// TODO this never becomes valid when using physical stuff
  when((state === s_write) && io.mem.resp.valid)
  {
    when (bytes_left === 0.U && ((rows_left === 0.U && (is_map_2d || is_unmap_2d)) || is_map || is_unmap)) {

      printf("State change from WRITE to FINISH\n")
      state := s_finish
    }
    .elsewhen (bytes_left === 0.U && rows_left =/= 0.U && (is_map_2d || is_unmap_2d)) {
      bytes_left := acr_reg.io.len
      amu_vaddr := amu_vaddr + acr_reg.io.stride + 1.U
      rows_left := rows_left - 1.U
      mem_wen := true.B
    }
    .otherwise {
      str_paddr := str_paddr + atomGranularity.U
      amu_vaddr := amu_vaddr + 1.U
      mem_wen := true.B
    }
  }

  io.map_update_valid := state === s_write
  io.map_update_addr  := str_paddr
  io.map_update_id    := mem_wdata(7,0)

  when(state === s_finish)
  {
    printf("State change from FINISH to IDLE\n")
    state := s_idle
    verbose := false.B
    mem_wen := false.B
  }


  //----------------------------------------------------------------------------------------------//
  io.interrupt := false.B
  if(isDummy)
  {
    io.busy   := false.B
    cmd.ready := true.B
  }else
  {
    io.busy :=  cmd.valid  || (state =/= s_idle)
    cmd.ready :=
      is_acr_write || is_len_write || // Write can always go through immediately
      is_ast_activate || is_ast_deactivate ||
      is_stride_write || is_rowcnt_write ||
      (is_ast_status && io.resp.ready) ||
      (is_acr_read && io.resp.ready) ||
      (is_stride_read && io.resp.ready) ||
      (is_rowcnt_read && io.resp.ready) ||
      is_acr_clear ||
      (is_len_read && io.resp.ready) ||
      ( (is_amu_map || is_amu_unmap || is_amu_map_2d || is_amu_unmap_2d || is_fatom || is_fatom_select || is_fatom_load) && (state === s_idle ) )
  }

  // Memory Request Interface
  io.mem.req.valid := ((state === s_write) && mem_wen) || ((state === s_readpat) && mem_wen) || ((state === s_send_pat_cache) && mem_wen)
  io.mem.req.bits.addr := Mux((state === s_readpat),fill_pat_address,Mux(state === s_send_pat_cache || state === s_wait,fill_pat_address,amu_vaddr))
  io.mem.req.bits.tag := Mux((state === s_readpat),pat_counter,Mux(state === s_send_pat_cache || state === s_wait, pat_counter,0.U))
  io.mem.req.bits.cmd := Mux((state === s_readpat),M_XRD,Mux((state === s_send_pat_cache || state === s_wait), M_XWR, M_XWR))
  io.mem.req.bits.size := Mux((state === s_readpat) || (state === s_send_pat_cache || state === s_wait), log2Ceil(4).U, 0.U)
  //io.mem.req.bits.size := Mux((state === s_readpat) || (state === s_send_pat_cache || state === s_wait), log2Ceil(4).U, log2Ceil(8).U) // SAVVINA alternative 
  io.mem.req.bits.signed := false.B // SAVVINA added extra 
  io.mem.req.bits.dprv := cmd.bits.status.dprv
  //io.mem.req.bits.dprv := PRV.S.U // SAVVINA alternative
  io.mem.req.bits.dv := cmd.bits.status.dv
  // io.mem.req.bits.isLookup := false.B

  io.mem.req.bits.data := Mux((is_map || is_map_2d), mem_wdata.asUInt, Mux(state === s_send_pat_cache, pat.io.atom_prim, 0.U))
  //io.mem.req.bits.mask := ~(0.U(coreDataBytes.W))
  io.mem.req.bits.mask := ~(0.U(8.W)) // SAVVINA alternative
  // io.mem.req.bits.paddr := 0.U

  io.mem.req.bits.phys := isPhysical.B
  io.mem.req.bits.no_alloc := false.B // SAVVINA
  io.mem.req.bits.no_xcpt := isPhysical.B

  io.mem.s1_kill := false.B
  io.mem.s1_data.data := Mux(state === s_send_pat_cache, pat.io.atom_prim, 0.U)
  io.mem.s1_data.mask := 0.U
  // io.mem.s1_data.paddr := 0.U//SAVVINA
  io.mem.s2_kill := false.B // SAVVINA
  io.mem.keep_clock_enabled := false.B // SAVVINA


  when( io.mem.req.fire() && !(state === s_readpat) )
  {
    bytes_left := bytes_left - 1.U
    mem_wen := false.B
    val mem_s2_nack = RegNext(RegNext(io.mem.s2_nack))
    val mem_s2_xcpt = RegNext(RegNext(io.mem.s2_xcpt))
  }
  .elsewhen( io.mem.req.fire() && (state === s_readpat) )
  {
    pat_counter := pat_counter + 1.U
    mem_wen := false.B
  }


  when( io.mem.req.fire() && (state === s_send_pat_cache) )
  {
    printf("SEND_PAT_CACHE - Memory request fire\n")
    printf("Pat.io.atom_prim = %d \n", pat.io.atom_prim)
    mem_wen := false.B
  }

  io.resp.valid := (cmd.valid && (is_acr_read || is_len_read || is_stride_read || is_rowcnt_read || is_ast_status))
  io.resp.bits.rd := inst.rd
  /* SAVVINA commented out
  io.resp.bits.data := Mux(is_ast_status, mem_atom_status,
            Mux(is_stride_read, acr_reg.io.stride,
            Mux(is_rowcnt_read, acr_reg.io.rowCnt,
            Mux(is_acr_read, acr_reg.io.acr, acr_reg.io.len))))
            */

  io.resp.bits.data :=  Mux(is_stride_read, acr_reg.io.stride,
          Mux(is_rowcnt_read, acr_reg.io.rowCnt,
          Mux(is_acr_read, acr_reg.io.acr, acr_reg.io.len)))
}

class AtomAddressMapController39MAPUPDATE (val atomGranularity: Int = 9, isPhysical: Boolean = false, isDummy: Boolean = false) 
(implicit p: Parameters, edge: TLEdgeOut) extends CoreModule {
  val io = IO(new Bundle {
    val cmd = Flipped(Decoupled(new RoCCCommand)) //Atom cmd

    val resp = Decoupled(new RoCCResponse)
    val mem = new HellaCacheIO
    val busy = Output(Bool())
    val interrupt = Output(Bool())
    val tlb = new FrontendTLBIO
    val mstatus = Output(new MStatus)

    val map_update_addr   = Output(UInt(40.W))
    val map_update_valid  = Output(Bool())
    val map_update_id     = Output(UInt(8.W)) 

    val acr = Output(UInt(39.W))
    val phybase = Output(UInt(39.W))
    val pref_pat_addr = Input(UInt(8.W))
    val pref_pat_atom = Output(UInt(512.W))

    // val pref_ast_addr   = Input(UInt(8.W)) // SAVVINA: comment out AST related stuff
    // val pref_ast_status = Output(Bool())   // SAVVINA: comment out AST related stuff
  })

  val n = 4
  val cmd = Queue(io.cmd)
  val atomID = RegInit(34.U(log2Up(n)-1))
  val str_vaddr = RegInit(33.U(39.W))
  val mem_wdata = RegInit(33.U(64.W))
  val str_paddr = RegInit(34.U(39.W))
  val bytes_left = RegInit(0.U(39.W))
  val rows_left = RegInit(0.U(39.W))
  val amu_vaddr = RegInit(0.U(39.W))
  val str_paddr_offset = RegInit(0.U(39.W))
  val fill_pat_address = RegInit(0.U(39.W))
  val pat_counter = RegInit(0.U(8.W))
  val n_atoms = RegInit(0.U(8.W))
  val is_map = RegInit(false.B)
  val is_unmap = RegInit(false.B)
  val is_map_2d = RegInit(false.B)
  val is_unmap_2d = RegInit(false.B)
  val verbose = RegInit(false.B)
  val mem_wen = RegInit(false.B)
  val atom_id = RegInit(0.U(8.W))
  val attribute = RegInit(0.U(64.W))
  val atomoffset = RegInit(0.U(4.W))

  if(isDummy)
    println("I won't spend time on mapping stuff")

  val status = RegEnable(cmd.bits.status, cmd.fire()) // && (is_amu_map))

  val inst = cmd.bits.inst
  val is_acr_read       = (inst.funct === 1.U)
  val is_acr_write      = (inst.funct === 2.U)
  val is_acr_clear      = (inst.funct === 3.U)
  val is_len_write      = (inst.funct === 4.U)
  val is_len_read       = (inst.funct === 5.U)
  val is_amu_map        = (inst.funct === 6.U)
  val is_amu_unmap      = (inst.funct === 7.U)
  val is_ast_activate   = (inst.funct === 8.U)
  val is_ast_deactivate = (inst.funct === 9.U)
  val is_ast_status     = (inst.funct === 10.U)
  val is_stride_write   = (inst.funct === 11.U)
  val is_stride_read    = (inst.funct === 12.U)
  val is_rowcnt_write   = (inst.funct === 13.U)
  val is_rowcnt_read    = (inst.funct === 14.U)
  val is_amu_map_2d     = (inst.funct === 15.U)
  val is_amu_unmap_2d   = (inst.funct === 16.U)
  val is_fatom          = (inst.funct === 17.U)
  val is_fatom_select   = (inst.funct === 18.U)
  val is_fatom_load     = (inst.funct === 19.U)
  
  val is_bc_load        = (inst.funct === 20.U)
  val is_bc_store       = (inst.funct === 21.U)
  val is_bc_atom_select = (inst.funct === 22.U)

  /* SAVVINA comment: According to the instruction called, contents of rs2 are stored in the corresponding register */

  val acr_reg = Module(new AtomAddressMapRegACR())
  acr_reg.io.wen := cmd.fire() && (is_acr_write || is_len_write || is_stride_write || is_rowcnt_write )
  acr_reg.io.waddr := Mux(is_acr_write, 0.U,
                      Mux(is_len_write, 1.U,
                      Mux(is_stride_write, 2.U,3.U)))
  acr_reg.io.wdata := cmd.bits.rs2
  acr_reg.io.clear := cmd.fire() && is_acr_clear

  io.acr := acr_reg.io.acr.asUInt
  io.phybase := acr_reg.io.rowCnt.asUInt

  /* SAVVINA comment: read the status of pref_addr and write 0 or 1 at address pointed by rs2, according to instruction called (activate/deactivate)*/

  /* SAVVINA comment out any AST related stuff */
/*
  val atomStatusTable = Module(new AtomStatusTable())

  atomStatusTable.io.pref_addr  := io.pref_ast_addr
  io.pref_ast_status            := atomStatusTable.io.pref_status

  atomStatusTable.io.wen := cmd.fire() && (is_ast_activate || is_ast_deactivate)
  atomStatusTable.io.AD := is_ast_activate
  atomStatusTable.io.waddr := cmd.bits.rs2
  atomStatusTable.io.raddr := cmd.bits.rs2
  val mem_atom_status = atomStatusTable.io.stat
*/

  val (s_idle :: s_translate :: s_fillpat :: s_fillpat_single :: s_readpat :: s_load_pat ::  s_send_pat_cache :: s_wait :: s_write :: s_finish :: Nil) = Enum(10)
  val state = RegInit(s_idle)
 

  val pat = Module (new PrivateAttributeTable())

  /* SAVVINA comment: Read attribute stored at addr_pref */
  pat.io.addr_pref := io.pref_pat_addr
  io.pref_pat_atom := pat.io.atom_pref

  when(cmd.fire() && (is_fatom_select) && ((state === s_idle) && !isDummy.B)){
    printf("fatom_select: ")
    printf("rs1:%d ", cmd.bits.rs1)
    printf("rs2:%d\n", cmd.bits.rs2)
    atom_id    := cmd.bits.rs1
    state := s_finish
  }

  when(cmd.fire() && (is_fatom_load) && ((state === s_idle) && !isDummy.B))
  {
    printf("fatom_load: ")
    printf("atomoffset:%d ", cmd.bits.rs1)
    printf("attribute:%d\n", cmd.bits.rs2)
    attribute := cmd.bits.rs2
    atomoffset := cmd.bits.rs1
    state := s_fillpat_single
  }

  when((state === s_fillpat_single)){
    state := s_finish
  }


  when(cmd.fire() && (is_fatom) && ((state === s_idle) && !isDummy.B))
  {
    printf("State change from IDLE to FILL_CACHE \n")
    printf("-- CMD (%d)  ", state)
    printf("v:%d  ", cmd.valid)
    printf("r:%d  ", cmd.ready)
    printf("b:%d  ", io.busy)
    printf("l:%d  ", acr_reg.io.len)
    printf("a:%d  ", acr_reg.io.acr)
    printf("rs1:%d  ", cmd.bits.rs1)
    printf("rs2:%d  ", cmd.bits.rs2)
    //printf("rd:%d  ", cmd.bits.rd)
    printf("inst:%d \n ", cmd.bits.inst.funct)
    fill_pat_address:= cmd.bits.rs1
    n_atoms:= cmd.bits.rs2
    state := s_fillpat
    pat_counter := 0.U
//    verbose := true.B

  }
   when((state === s_fillpat))
  {

    printf("Number of Atoms: %d\n", pat_counter)
    printf("Fill pat address: %d\n", fill_pat_address)
    state := s_readpat
    printf("============ MEM REQUEST ========== \n")
    printf("io.mem.req.valid := %d \n",io.mem.req.valid)
    printf("io.mem.req.bits.addr := %d\n",io.mem.req.bits.addr)
    printf("io.mem.req.bits.tag :=  %d\n",io.mem.req.bits.tag)
    printf(" io.mem.req.bits.cmd :=  %d\n",io.mem.req.bits.cmd)
    //printf(" io.mem.req.bits.typ :=  %d\n",io.mem.req.bits.typ)
    printf(" io.mem.req.bits.data :=  %d\n",io.mem.req.bits.data)
    printf(" io.mem.req.bits.phys := %d\n",io.mem.req.bits.phys)
    printf("===============================================\n")
    mem_wen := true.B
  }

  pat.io.wen := ((state === s_readpat) && io.mem.resp.valid ) || (state === s_fillpat_single)
  pat.io.addr := Mux((state === s_fillpat_single), atom_id, io.mem.resp.bits.data(7,0)) //not sure
  pat.io.wdata := Mux((state === s_fillpat_single), attribute, io.mem.resp.bits.data(31,8)) //not sure
  pat.io.ren := (state === s_send_pat_cache) && io.mem.req.fire() || state === s_load_pat
  pat.io.attindex := atomoffset

  when(state === s_readpat && io.mem.resp.valid )
  {

        printf("============== READ_PAT STATE ==============\n")
        printf("Data from memory: %d\n",io.mem.resp.bits.data)
        printf( "Pat_counter: %d\n", pat_counter )
        printf("===========================================\n")

        when ( pat_counter === (n_atoms ))
        {
              state:= s_finish
              pat_counter := 1.U

        }
        .otherwise {
              fill_pat_address := fill_pat_address + 4.U
              mem_wen := true.B
        }


  }


  when(state === s_load_pat){
      state := s_send_pat_cache
      printf( "Pat_counter: %d\n", pat_counter )
      printf("Primitive: %d\n", pat.io.atom_prim)
      mem_wen := true.B

  }

  when(state === s_send_pat_cache && io.mem.resp.valid )
  {

        when ( pat_counter === (n_atoms+1.U) )
        {
              state:= s_finish
        }
        .otherwise {

            printf("============= SEND_PAT_CACHE  STATE ==========\n")
            printf( "Pat_counter: %d\n", pat_counter )
            printf("Primitive: %d\n", pat.io.atom_prim)

            printf("io.mem.req.valid := %d \n",io.mem.req.valid)
            printf("io.mem.req.bits.addr := %d\n",io.mem.req.bits.addr)
            printf("io.mem.req.bits.tag :=  %d\n",io.mem.req.bits.tag)
            printf(" io.mem.req.bits.cmd :=  %d\n",io.mem.req.bits.cmd)
          //  printf(" io.mem.req.bits.typ :=  %d\n",io.mem.req.bits.typ)
            printf(" io.mem.req.bits.data :=  %d\n",io.mem.s1_data.data)
            printf(" io.mem.req.bits.phys := %d\n",io.mem.req.bits.phys)

            printf(" =============================================\n")

            pat_counter := pat_counter + 1.U
            mem_wen := true.B
        }
  }

  //----------------------------------------------------------------------------------------------//
  // TLB implementation
  // val tlb = Module(new FrontendTLB(1))//
  // io.ptw <> tlb.io.ptw
  // tlb.io.ptw.status := status

  io.mstatus := status
  val to_translate = RegInit(false.B)
  val tlb_sent = RegInit(true.B)
  val tlb_to_send = to_translate & !tlb_sent
  val ptw_error = false.B
  io.tlb.req.valid := tlb_to_send
  io.tlb.req.bits.vaddr := str_vaddr
  io.tlb.req.bits.passthrough := false.B
  io.tlb.req.bits.size := log2Ceil(4).U
  io.tlb.req.bits.cmd := M_XWR
  io.tlb.req.bits.prv := cmd.bits.status.prv // SAVVINA added, not existing in the original metasys
  //io.tlb.req.bits.prv := PRV.S.U // SAVVINA added, not existing in the original metasys
  io.tlb.req.bits.v := cmd.bits.status.v // SAVVINA added, not existing in the original metasys
  // io.tlb.req.bits.isLookup := false.B 

  io.tlb.resp.ready := true.B

  io.tlb.sfence.valid     := ptw_error
  io.tlb.sfence.bits.rs1  := true.B
  io.tlb.sfence.bits.rs2  := false.B
  io.tlb.sfence.bits.addr :=  str_vaddr
  io.tlb.sfence.bits.asid := 0.U
  io.tlb.sfence.bits.hv   := false.B // SAVVINA added, not existing in the original metasys
  io.tlb.sfence.bits.hg   := false.B // SAVVINA added, not existing in the original metasys

  io.tlb.kill := false.B // SAVVINA added, not existing in the original metasys
  
  when(io.tlb.req.fire())
  {
    tlb_sent := true.B
  }

  val timeout = RegInit(UInt(0,10.W))

  when(verbose)
  {
    timeout := timeout + UInt(1)
    //when(timeout > UInt(30)){
      //verbose := false.B
    //}
    printf("-- CMD (%d)  ", state)
    printf("v:%d  ", cmd.valid)
    printf("r:%d  ", cmd.ready)
    printf("b:%d  ", io.busy)
    printf("l:%d  ", acr_reg.io.len)
    printf("a:%d  ", acr_reg.io.acr)
    printf("rs1:%d  ", cmd.bits.inst.rs1)
    printf("rs2:%d  ", cmd.bits.inst.rs2)
    printf("rd:%d  ", cmd.bits.inst.rd)
    printf("inst:%d \n ", cmd.bits.inst.funct)

    printf("-- R (%d)  ", state)
    printf("v:%d  ", io.tlb.req.valid)
    printf("r:%d  ", io.tlb.req.ready)
    printf("va:%x  ", io.tlb.req.bits.vaddr)
    printf("ps:%d  \n", io.tlb.req.bits.passthrough)


    printf("-- S (%d)  ", state)
    printf("v:%d  ", io.tlb.resp.valid)
    printf("r:%d  ", io.tlb.resp.ready)
    printf("pa:%x  ", io.tlb.resp.bits.paddr)
    printf("ms:%d  ", io.tlb.resp.bits.miss)
    printf("ld:%d  ", io.tlb.resp.bits.pf.ld)
    printf("st:%d  ", io.tlb.resp.bits.pf.st)
    printf("cc:%d\n", io.tlb.resp.bits.cacheable)


    printf("-- MR (%d)  ", state)
    printf("v:%d  ", io.mem.req.valid)
    printf("r:%d  ", io.mem.req.ready)
    printf("ad:%x  ", io.mem.req.bits.addr)
    printf("tg:%d  ", io.mem.req.bits.tag)
    printf("cmd:%d  ", io.mem.req.bits.cmd)
  //  printf("typ:%d  ", io.mem.req.bits.typ)
    printf("data:%x  \n", io.mem.req.bits.data)


    printf("-- MS (%d)  ", state)
    printf("v: %d  ", io.mem.resp.valid)
    printf("r: %d \n", io.mem.resp.bits.data)

    printf("s2_xcpt : %d  ", io.mem.s2_xcpt.asUInt)
    printf("ma.st : %d  ", io.mem.s2_xcpt.ma.st)
    printf("ma.ld : %d  ", io.mem.s2_xcpt.ma.ld)
    printf("pf.st : %d  ", io.mem.s2_xcpt.pf.st)
    printf("pf.ld : %d  ", io.mem.s2_xcpt.pf.ld)
    printf("ae.st : %d  ", io.mem.s2_xcpt.ae.st)
    printf("ae.ld : %d\n", io.mem.s2_xcpt.ae.ld)



  }

    when(cmd.fire())
    {
      printf("-- CMDFIRE (%d)  ", state)
      printf("v:%d  ", cmd.valid)
      printf("r:%d  ", cmd.ready)
      printf("rs1:%d  ", cmd.bits.inst.rs1)
      printf("rs2:%d  ", cmd.bits.inst.rs2)
      printf("rd:%d  ", cmd.bits.inst.rd)
      printf("inst:%d\n", cmd.bits.inst.funct)
    }


  when(io.tlb.resp.fire())
  {
    to_translate := false.B
    str_paddr := io.tlb.resp.bits.paddr
  }

  //----------------------------------------------------------------------------------------------//
  when(cmd.fire() && (is_amu_map || is_amu_unmap || is_amu_map_2d || is_amu_unmap_2d) && (state === s_idle))
  {
    if(isDummy)
    {
      state := s_idle
    }else{
      state := s_translate
    }

    printf("State change from IDLE to TRANSLATE\n")
    printf("-- CMD (%d)  ", state)
    printf("vs_fillpat:%d  ", cmd.valid)
    printf("r:%d  ", cmd.ready)
    printf("b:%d  ", io.busy)
    printf("l:%d  ", acr_reg.io.len)
    printf("a:%d  ", acr_reg.io.acr)
    printf("rs1:%d  ", cmd.bits.inst.rs1)
    printf("rs2:%d  ", cmd.bits.inst.rs2)
    printf("rd:%d  ", cmd.bits.inst.rd)
    printf("inst:%d \n ", cmd.bits.inst.funct)
    str_vaddr := cmd.bits.rs1
    atomID    := cmd.bits.rs2(log2Up(n)-1,0)
    mem_wdata       := (cmd.bits.rs2 & "h00000000000000ff".U) |
      ((cmd.bits.rs2 << 8 ).asUInt & "h000000000000ff00".U) |
      ((cmd.bits.rs2 << 16).asUInt & "h0000000000ff0000".U) |
      ((cmd.bits.rs2 << 24).asUInt & "h00000000ff000000".U) |
      ((cmd.bits.rs2 << 32).asUInt & "h000000ff00000000".U) |
      ((cmd.bits.rs2 << 40).asUInt & "h0000ff0000000000".U) |
      ((cmd.bits.rs2 << 48).asUInt & "h00ff000000000000".U) |
      ((cmd.bits.rs2 << 56).asUInt & "hff00000000000000".U)

    is_map := is_amu_map
    is_unmap := is_amu_unmap
    is_map_2d := is_amu_map_2d
    is_unmap_2d := is_amu_unmap_2d
    to_translate := true.B
    tlb_sent := false.B
    verbose := true.B
  }

  println("\nAtom Granularity " + atomGranularity + "\n")
  when((state === s_translate) && !to_translate)
  {
    printf("State change from TRANSLATE to WRITE\n")
    state := s_write
    str_paddr_offset := (str_paddr >> atomGranularity).asUInt
    if(isPhysical){
      amu_vaddr := ((str_paddr >> atomGranularity).asUInt + (acr_reg.io.acr).asUInt) + acr_reg.io.rowCnt//((str_paddr >> 9).asUInt() + (acr_reg.io.acr).asUInt())
    }
    else{
      amu_vaddr := ((str_paddr >> atomGranularity).asUInt + (acr_reg.io.acr).asUInt) //((str_paddr >> 9).asUInt() + (acr_reg.io.acr).asUInt())
    }
    bytes_left := acr_reg.io.len
    rows_left := Mux((is_amu_map || is_amu_unmap), 0.U, acr_reg.io.rowCnt - 1.U)
    mem_wen := true.B
  }


// TODO this never becomes valid when using physical stuff
  when((state === s_write) && io.mem.resp.valid)
  {
    when (bytes_left === 0.U && ((rows_left === 0.U && (is_map_2d || is_unmap_2d)) || is_map || is_unmap)) {

      printf("State change from WRITE to FINISH\n")
      state := s_finish
    }
    .elsewhen (bytes_left === 0.U && rows_left =/= 0.U && (is_map_2d || is_unmap_2d)) {
      bytes_left := acr_reg.io.len
      amu_vaddr := amu_vaddr + acr_reg.io.stride + 1.U
      rows_left := rows_left - 1.U
      mem_wen := true.B
    }
    .otherwise {
      str_paddr := str_paddr + (1.U << atomGranularity).asUInt
      amu_vaddr := amu_vaddr + 1.U
      mem_wen := true.B
    }
  }

  io.map_update_valid := state === s_write
  io.map_update_addr  := str_paddr
  io.map_update_id    := mem_wdata(7,0)

  when(state === s_finish)
  {
    printf("State change from FINISH to IDLE\n")
    state := s_idle
    verbose := false.B
    mem_wen := false.B
  }


  //----------------------------------------------------------------------------------------------//
  io.interrupt := false.B
  if(isDummy)
  {
    io.busy   := false.B
    cmd.ready := true.B
  }else
  {
    io.busy :=  cmd.valid  || (state =/= s_idle)
    cmd.ready :=
      is_acr_write || is_len_write || // Write can always go through immediately
      is_ast_activate || is_ast_deactivate ||
      is_stride_write || is_rowcnt_write ||
      (is_ast_status && io.resp.ready) ||
      (is_acr_read && io.resp.ready) ||
      (is_stride_read && io.resp.ready) ||
      (is_rowcnt_read && io.resp.ready) ||
      is_acr_clear ||
      (is_len_read && io.resp.ready) ||
      ( (is_amu_map || is_amu_unmap || is_amu_map_2d || is_amu_unmap_2d || is_fatom || is_fatom_select || is_fatom_load) && (state === s_idle ) )
  }

  // Memory Request Interface
  io.mem.req.valid := ((state === s_write) && mem_wen) || ((state === s_readpat) && mem_wen) || ((state === s_send_pat_cache) && mem_wen)
  io.mem.req.bits.addr := Mux((state === s_readpat),fill_pat_address,Mux(state === s_send_pat_cache || state === s_wait,fill_pat_address,amu_vaddr))
  io.mem.req.bits.tag := Mux((state === s_readpat),pat_counter,Mux(state === s_send_pat_cache || state === s_wait, pat_counter,0.U))
  io.mem.req.bits.cmd := Mux((state === s_readpat),M_XRD,Mux((state === s_send_pat_cache || state === s_wait), M_XWR, M_XWR))
  io.mem.req.bits.size := Mux((state === s_readpat) || (state === s_send_pat_cache || state === s_wait), log2Ceil(4).U, 0.U)
  //io.mem.req.bits.size := Mux((state === s_readpat) || (state === s_send_pat_cache || state === s_wait), log2Ceil(4).U, log2Ceil(8).U) // SAVVINA alternative 
  io.mem.req.bits.signed := false.B // SAVVINA added extra 
  io.mem.req.bits.dprv := cmd.bits.status.dprv
  //io.mem.req.bits.dprv := PRV.S.U // SAVVINA alternative
  io.mem.req.bits.dv := cmd.bits.status.dv
  // io.mem.req.bits.isLookup := false.B

  io.mem.req.bits.data := Mux((is_map || is_map_2d), mem_wdata.asUInt, Mux(state === s_send_pat_cache, pat.io.atom_prim, 0.U))
  //io.mem.req.bits.mask := ~(0.U(coreDataBytes.W))
  io.mem.req.bits.mask := ~(0.U(8.W)) // SAVVINA alternative
  // io.mem.req.bits.paddr := 0.U

  io.mem.req.bits.phys := isPhysical.B
  io.mem.req.bits.no_alloc := false.B // SAVVINA
  io.mem.req.bits.no_xcpt := isPhysical.B

  io.mem.s1_kill := false.B
  io.mem.s1_data.data := Mux(state === s_send_pat_cache, pat.io.atom_prim, 0.U)
  io.mem.s1_data.mask := 0.U
  // io.mem.s1_data.paddr := 0.U//SAVVINA
  io.mem.s2_kill := false.B // SAVVINA
  io.mem.keep_clock_enabled := false.B // SAVVINA


  when( io.mem.req.fire() && !(state === s_readpat) )
  {
    bytes_left := bytes_left - 1.U
    mem_wen := false.B
    val mem_s2_nack = RegNext(RegNext(io.mem.s2_nack))
    val mem_s2_xcpt = RegNext(RegNext(io.mem.s2_xcpt))
  }
  .elsewhen( io.mem.req.fire() && (state === s_readpat) )
  {
    pat_counter := pat_counter + 1.U
    mem_wen := false.B
  }


  when( io.mem.req.fire() && (state === s_send_pat_cache) )
  {
    printf("SEND_PAT_CACHE - Memory request fire\n")
    printf("Pat.io.atom_prim = %d \n", pat.io.atom_prim)
    mem_wen := false.B
  }

  io.resp.valid := (cmd.valid && (is_acr_read || is_len_read || is_stride_read || is_rowcnt_read || is_ast_status))
  io.resp.bits.rd := inst.rd
  /* SAVVINA commented out
  io.resp.bits.data := Mux(is_ast_status, mem_atom_status,
            Mux(is_stride_read, acr_reg.io.stride,
            Mux(is_rowcnt_read, acr_reg.io.rowCnt,
            Mux(is_acr_read, acr_reg.io.acr, acr_reg.io.len))))
            */

  io.resp.bits.data :=  Mux(is_stride_read, acr_reg.io.stride,
          Mux(is_rowcnt_read, acr_reg.io.rowCnt,
          Mux(is_acr_read, acr_reg.io.acr, acr_reg.io.len)))
}

class AtomAddressMapController1024X11CycleRDLatencyNoren (val atomGranularity: Int = 9, isPhysical: Boolean = false, isDummy: Boolean = false) 
(implicit p: Parameters, edge: TLEdgeOut) extends CoreModule {
  val io = IO(new Bundle {
    val cmd = Flipped(Decoupled(new RoCCCommand)) //Atom cmd

    val resp = Decoupled(new RoCCResponse)
    val mem = new HellaCacheIO
    val busy = Output(Bool())
    val interrupt = Output(Bool())
    val tlb = new FrontendTLBIO
    val mstatus = Output(new MStatus)

    val map_update_addr   = Output(UInt(40.W))
    val map_update_valid  = Output(Bool())
    val map_update_id     = Output(UInt(10.W)) // SAVVINA 14/7 CHANGED PAT

    val acr = Output(UInt(39.W))
    val phybase = Output(UInt(39.W))
    val pref_pat_addr = Input(UInt(10.W)) // SAVVINA 14/7 CHANGED PAT
    val pref_pat_atom = Output(UInt(64.W)) // SAVVINA 14/7 CHANGED PAT
  })

  val n = 4
  val cmd = Queue(io.cmd)
  val atomID = RegInit(34.U(log2Up(n)-1))
  val str_vaddr = RegInit(33.U(39.W))
  val mem_wdata = RegInit(33.U(64.W))
  val str_paddr = RegInit(34.U(39.W))
  val bytes_left = RegInit(0.U(39.W))
  val rows_left = RegInit(0.U(39.W))
  val amu_vaddr = RegInit(0.U(39.W))
  val is_map = RegInit(false.B)
  val is_unmap = RegInit(false.B)
  val is_map_2d = RegInit(false.B)
  val is_unmap_2d = RegInit(false.B)
  val verbose = RegInit(false.B)
  val mem_wen = RegInit(false.B)
  val atom_id = RegInit(0.U(10.W)) // SAVVINA 14/7 CHANGED PAT
  val attribute = RegInit(0.U(64.W))
  val atomoffset = RegInit(0.U(3.W))
  val baseBool = RegInit(false.B)

  if(isDummy)
    println("I won't spend time on mapping stuff")

  val status = RegEnable(cmd.bits.status, cmd.fire()) // && (is_amu_map))

  val inst = cmd.bits.inst
  val is_acr_read       = (inst.funct === 1.U)
  val is_acr_write      = (inst.funct === 2.U)
  val is_acr_clear      = (inst.funct === 3.U)
  val is_len_write      = (inst.funct === 4.U)
  val is_len_read       = (inst.funct === 5.U)
  val is_amu_map        = (inst.funct === 6.U)
  val is_amu_unmap      = (inst.funct === 7.U)
  val is_ast_activate   = (inst.funct === 8.U)
  val is_ast_deactivate = (inst.funct === 9.U)
  val is_ast_status     = (inst.funct === 10.U)
  val is_stride_write   = (inst.funct === 11.U)
  val is_stride_read    = (inst.funct === 12.U)
  val is_rowcnt_write   = (inst.funct === 13.U)
  val is_rowcnt_read    = (inst.funct === 14.U)
  val is_amu_map_2d     = (inst.funct === 15.U)
  val is_amu_unmap_2d   = (inst.funct === 16.U)
  val is_fatom_select   = (inst.funct === 18.U)
  val is_fatom_load     = (inst.funct === 19.U)
  
  val is_bc_load        = (inst.funct === 20.U)
  val is_bc_store       = (inst.funct === 21.U)
  val is_bc_atom_select = (inst.funct === 22.U)

  /* SAVVINA comment: According to the instruction called, contents of rs2 are stored in the corresponding register */

  val acr_reg = Module(new AtomAddressMapRegACR())
  acr_reg.io.wen := cmd.fire() && (is_acr_write || is_len_write || is_stride_write || is_rowcnt_write )
  acr_reg.io.waddr := Mux(is_acr_write, 0.U,
                      Mux(is_len_write, 1.U,
                      Mux(is_stride_write, 2.U,3.U)))
  acr_reg.io.wdata := cmd.bits.rs2
  acr_reg.io.clear := cmd.fire() && is_acr_clear

  io.acr := acr_reg.io.acr.asUInt
  io.phybase := acr_reg.io.rowCnt.asUInt

  /* SAVVINA comment: read the status of pref_addr and write 0 or 1 at address pointed by rs2, according to instruction called (activate/deactivate)*/

  /* SAVVINA comment out any AST related stuff */
/*
  val atomStatusTable = Module(new AtomStatusTable())

  atomStatusTable.io.pref_addr  := io.pref_ast_addr
  io.pref_ast_status            := atomStatusTable.io.pref_status

  atomStatusTable.io.wen := cmd.fire() && (is_ast_activate || is_ast_deactivate)
  atomStatusTable.io.AD := is_ast_activate
  atomStatusTable.io.waddr := cmd.bits.rs2
  atomStatusTable.io.raddr := cmd.bits.rs2
  val mem_atom_status = atomStatusTable.io.stat
*/

  val (s_idle :: s_translate :: s_fillpat_single :: s_write :: s_finish :: Nil) = Enum(5)
  val state = RegInit(s_idle)
 

  val pat = Module (new PrivateAttributeTable1024X11CycleRDLatencyNoren())

  /* SAVVINA comment: Read attribute stored at addr_pref */
  pat.io.addr_pref := io.pref_pat_addr
  io.pref_pat_atom := pat.io.atom_pref



  when(cmd.fire() && (is_fatom_select) && ((state === s_idle) && !isDummy.B)){
    printf("fatom_select: ")
    printf("rs1:%d ", cmd.bits.rs1)
    printf("rs2:%d\n", cmd.bits.rs2)
    atom_id    := cmd.bits.rs1
    state := s_finish
  }

  when(cmd.fire() && (is_fatom_load) && ((state === s_idle) && !isDummy.B))
  {
    printf("fatom_load: ")
    printf("atomoffset:%d ", cmd.bits.rs1)
    printf("attribute:%d\n", cmd.bits.rs2)
    attribute := cmd.bits.rs2
    atomoffset := cmd.bits.rs1
    state := s_fillpat_single
  }

  when((state === s_fillpat_single)){
    state := s_finish
  }

  pat.io.wen := state === s_fillpat_single
  pat.io.waddr := atom_id 
  pat.io.wdata := attribute 

  //----------------------------------------------------------------------------------------------//
  // TLB implementation
  // val tlb = Module(new FrontendTLB(1))//
  // io.ptw <> tlb.io.ptw
  // tlb.io.ptw.status := status

  io.mstatus := status
  val to_translate = RegInit(false.B)
  val tlb_sent = RegInit(true.B)
  val tlb_to_send = to_translate & !tlb_sent
  val ptw_error = false.B
  io.tlb.req.valid := tlb_to_send
  io.tlb.req.bits.vaddr := str_vaddr
  io.tlb.req.bits.passthrough := false.B
  io.tlb.req.bits.size := log2Ceil(4).U
  io.tlb.req.bits.cmd := M_XWR
  io.tlb.req.bits.prv := cmd.bits.status.prv // SAVVINA added, not existing in the original metasys
  //io.tlb.req.bits.prv := PRV.S.U // SAVVINA added, not existing in the original metasys
  io.tlb.req.bits.v := cmd.bits.status.v // SAVVINA added, not existing in the original metasys
  // io.tlb.req.bits.isLookup := false.B 

  io.tlb.resp.ready := true.B

  io.tlb.sfence.valid     := ptw_error
  io.tlb.sfence.bits.rs1  := true.B
  io.tlb.sfence.bits.rs2  := false.B
  io.tlb.sfence.bits.addr :=  str_vaddr
  io.tlb.sfence.bits.asid := 0.U
  io.tlb.sfence.bits.hv   := false.B // SAVVINA added, not existing in the original metasys
  io.tlb.sfence.bits.hg   := false.B // SAVVINA added, not existing in the original metasys

  io.tlb.kill := false.B // SAVVINA added, not existing in the original metasys
  
  when(io.tlb.req.fire())
  {
    tlb_sent := true.B
  }

  val timeout = RegInit(UInt(0,10.W))

  when(verbose)
  {
    timeout := timeout + UInt(1)
    //when(timeout > UInt(30)){
      //verbose := false.B
    //}
    printf("-- CMD (%d)  ", state)
    printf("v:%d  ", cmd.valid)
    printf("r:%d  ", cmd.ready)
    printf("b:%d  ", io.busy)
    printf("l:%d  ", acr_reg.io.len)
    printf("a:%d  ", acr_reg.io.acr)
    printf("rs1:%d  ", cmd.bits.inst.rs1)
    printf("rs2:%d  ", cmd.bits.inst.rs2)
    printf("rd:%d  ", cmd.bits.inst.rd)
    printf("inst:%d \n ", cmd.bits.inst.funct)

    printf("-- R (%d)  ", state)
    printf("v:%d  ", io.tlb.req.valid)
    printf("r:%d  ", io.tlb.req.ready)
    printf("va:%x  ", io.tlb.req.bits.vaddr)
    printf("ps:%d  \n", io.tlb.req.bits.passthrough)


    printf("-- S (%d)  ", state)
    printf("v:%d  ", io.tlb.resp.valid)
    printf("r:%d  ", io.tlb.resp.ready)
    printf("pa:%x  ", io.tlb.resp.bits.paddr)
    printf("ms:%d  ", io.tlb.resp.bits.miss)
    printf("ld:%d  ", io.tlb.resp.bits.pf.ld)
    printf("st:%d  ", io.tlb.resp.bits.pf.st)
    printf("cc:%d\n", io.tlb.resp.bits.cacheable)


    printf("-- MR (%d)  ", state)
    printf("v:%d  ", io.mem.req.valid)
    printf("r:%d  ", io.mem.req.ready)
    printf("ad:%x  ", io.mem.req.bits.addr)
    printf("tg:%d  ", io.mem.req.bits.tag)
    printf("cmd:%d  ", io.mem.req.bits.cmd)
  //  printf("typ:%d  ", io.mem.req.bits.typ)
    printf("data:%x  \n", io.mem.req.bits.data)


    printf("-- MS (%d)  ", state)
    printf("v: %d  ", io.mem.resp.valid)
    printf("r: %d \n", io.mem.resp.bits.data)

    printf("s2_xcpt : %d  ", io.mem.s2_xcpt.asUInt)
    printf("ma.st : %d  ", io.mem.s2_xcpt.ma.st)
    printf("ma.ld : %d  ", io.mem.s2_xcpt.ma.ld)
    printf("pf.st : %d  ", io.mem.s2_xcpt.pf.st)
    printf("pf.ld : %d  ", io.mem.s2_xcpt.pf.ld)
    printf("ae.st : %d  ", io.mem.s2_xcpt.ae.st)
    printf("ae.ld : %d\n", io.mem.s2_xcpt.ae.ld)



  }

    when(cmd.fire())
    {
      printf("-- CMDFIRE (%d)  ", state)
      printf("v:%d  ", cmd.valid)
      printf("r:%d  ", cmd.ready)
      printf("rs1:%d  ", cmd.bits.inst.rs1)
      printf("rs2:%d  ", cmd.bits.inst.rs2)
      printf("rd:%d  ", cmd.bits.inst.rd)
      printf("inst:%d\n", cmd.bits.inst.funct)
    }


  when(io.tlb.resp.fire())
  {
    to_translate := false.B
    str_paddr := io.tlb.resp.bits.paddr
  }

  //----------------------------------------------------------------------------------------------//


  when(cmd.fire() && (is_amu_map || is_amu_unmap || is_amu_map_2d || is_amu_unmap_2d) && (state === s_idle))
  {
    if(isDummy)
    {
      state := s_idle
    }else{
      state := s_translate
    }

    printf("State change from IDLE to TRANSLATE\n")
    printf("-- CMD (%d)  ", state)
    printf("vs_fillpat:%d  ", cmd.valid)
    printf("r:%d  ", cmd.ready)
    printf("b:%d  ", io.busy)
    printf("l:%d  ", acr_reg.io.len)
    printf("a:%d  ", acr_reg.io.acr)
    printf("rs1:%d  ", cmd.bits.inst.rs1)
    printf("rs2:%d  ", cmd.bits.inst.rs2)
    printf("rd:%d  ", cmd.bits.inst.rd)
    printf("inst:%d \n ", cmd.bits.inst.funct)
    str_vaddr := cmd.bits.rs1
    atomID    := cmd.bits.rs2(log2Up(n)-1,0)
      mem_wdata       := (cmd.bits.rs2 & "h000000000000ffff".U) |
    ((cmd.bits.rs2 << 16 ).asUInt & "h00000000ffff0000".U) |
    ((cmd.bits.rs2 << 32).asUInt & "h0000ffff00000000".U) |
    ((cmd.bits.rs2 << 48).asUInt & "hffff000000000000".U) 

    is_map := is_amu_map
    is_unmap := is_amu_unmap
    is_map_2d := is_amu_map_2d
    is_unmap_2d := is_amu_unmap_2d
    to_translate := true.B
    tlb_sent := false.B
    verbose := true.B
  }

  println("\nAtom Granularity " + atomGranularity + "\n")

  when((state === s_translate) && !to_translate) {
    printf("State change from TRANSLATE to WRITE\n")
    state := s_write

    val base = (str_paddr >> atomGranularity).asUInt

    val half = (1.U << (32 - atomGranularity)).asUInt

    val offset = Mux(base(0) === 1.U, base + 1.U, base + half)

    baseBool := base(0).asBool

    if (isPhysical) {
      amu_vaddr := (offset + acr_reg.io.acr.asUInt) + acr_reg.io.rowCnt
    } else {
      amu_vaddr := (offset + acr_reg.io.acr.asUInt)
    }

    bytes_left := acr_reg.io.len
    rows_left := Mux((is_amu_map || is_amu_unmap), 0.U, acr_reg.io.rowCnt - 1.U)
    mem_wen := true.B
  }

// TODO this never becomes valid when using physical stuff
  when((state === s_write) && io.mem.resp.valid)
  {
    when (bytes_left === 0.U && ((rows_left === 0.U && (is_map_2d || is_unmap_2d)) || is_map || is_unmap)) {

      printf("State change from WRITE to FINISH\n")
      state := s_finish
    }
    .elsewhen (bytes_left === 0.U && rows_left =/= 0.U && (is_map_2d || is_unmap_2d)) {
      bytes_left := acr_reg.io.len
      amu_vaddr := amu_vaddr + acr_reg.io.stride + 1.U
      rows_left := rows_left - 1.U
      mem_wen := true.B
    }
    .otherwise {
      str_paddr := str_paddr + atomGranularity.U

      val half = (1.U << (32 - atomGranularity)).asUInt
      
      amu_vaddr := Mux(baseBool, amu_vaddr - half + 2.U, amu_vaddr + half)
      mem_wen := true.B
    }
  }

  io.map_update_valid := state === s_write
  io.map_update_addr  := str_paddr
  io.map_update_id    := mem_wdata(10,0) // SAVVINA 14/7 CHANGED PAT

  when(state === s_finish)
  {
    printf("State change from FINISH to IDLE\n")
    state := s_idle
    verbose := false.B
    mem_wen := false.B
  }


  //----------------------------------------------------------------------------------------------//
  io.interrupt := false.B
  if(isDummy)
  {
    io.busy   := false.B
    cmd.ready := true.B
  }else
  {
    io.busy :=  cmd.valid  || (state =/= s_idle)
    cmd.ready :=
      is_acr_write || is_len_write || // Write can always go through immediately
      is_ast_activate || is_ast_deactivate ||
      is_stride_write || is_rowcnt_write ||
      (is_ast_status && io.resp.ready) ||
      (is_acr_read && io.resp.ready) ||
      (is_stride_read && io.resp.ready) ||
      (is_rowcnt_read && io.resp.ready) ||
      is_acr_clear ||
      (is_len_read && io.resp.ready) ||
      ( (is_amu_map || is_amu_unmap || is_amu_map_2d || is_amu_unmap_2d || is_fatom_select || is_fatom_load) && (state === s_idle ) )
  }

  // Memory Request Interface
  io.mem.req.valid := ((state === s_write) && mem_wen) 
  io.mem.req.bits.addr := amu_vaddr
  io.mem.req.bits.tag := 0.U
  io.mem.req.bits.cmd := M_XWR
  io.mem.req.bits.size := 1.U
  io.mem.req.bits.signed := false.B // SAVVINA added extra 
  io.mem.req.bits.dprv := cmd.bits.status.dprv
  //io.mem.req.bits.dprv := PRV.S.U // SAVVINA alternative
  io.mem.req.bits.dv := cmd.bits.status.dv
  // io.mem.req.bits.isLookup := false.B

  io.mem.req.bits.data := Mux((is_map || is_map_2d), mem_wdata.asUInt, 0.U)
  //io.mem.req.bits.mask := ~(0.U(coreDataBytes.W))
  io.mem.req.bits.mask := ~(0.U(8.W)) // SAVVINA alternative // SAVVINA 14/7 CHANGED PAT
  // io.mem.req.bits.paddr := 0.U

  io.mem.req.bits.phys := isPhysical.B
  io.mem.req.bits.no_alloc := false.B // SAVVINA
  io.mem.req.bits.no_xcpt := isPhysical.B

  io.mem.s1_kill := false.B
  io.mem.s1_data.data := 0.U
  io.mem.s1_data.mask := 0.U
  // io.mem.s1_data.paddr := 0.U//SAVVINA
  io.mem.s2_kill := false.B // SAVVINA
  io.mem.keep_clock_enabled := false.B // SAVVINA


  when( io.mem.req.fire() )
  {
    bytes_left := bytes_left - 1.U
    baseBool := ~baseBool
    mem_wen := false.B
    val mem_s2_nack = RegNext(RegNext(io.mem.s2_nack))
    val mem_s2_xcpt = RegNext(RegNext(io.mem.s2_xcpt))
  }

  io.resp.valid := (cmd.valid && (is_acr_read || is_len_read || is_stride_read || is_rowcnt_read || is_ast_status))
  io.resp.bits.rd := inst.rd
  /* SAVVINA commented out
  io.resp.bits.data := Mux(is_ast_status, mem_atom_status,
            Mux(is_stride_read, acr_reg.io.stride,
            Mux(is_rowcnt_read, acr_reg.io.rowCnt,
            Mux(is_acr_read, acr_reg.io.acr, acr_reg.io.len))))
            */

  io.resp.bits.data :=  Mux(is_stride_read, acr_reg.io.stride,
          Mux(is_rowcnt_read, acr_reg.io.rowCnt,
          Mux(is_acr_read, acr_reg.io.acr, acr_reg.io.len)))
}

class AtomAddressMapController2048X11CycleRDLatencyNoren (val atomGranularity: Int = 9, isPhysical: Boolean = false, isDummy: Boolean = false) 
(implicit p: Parameters, edge: TLEdgeOut) extends CoreModule {
  val io = IO(new Bundle {
    val cmd = Flipped(Decoupled(new RoCCCommand)) //Atom cmd

    val resp = Decoupled(new RoCCResponse)
    val mem = new HellaCacheIO
    val busy = Output(Bool())
    val interrupt = Output(Bool())
    val tlb = new FrontendTLBIO
    val mstatus = Output(new MStatus)

    val map_update_addr   = Output(UInt(40.W))
    val map_update_valid  = Output(Bool())
    val map_update_id     = Output(UInt(11.W)) // SAVVINA 14/7 CHANGED PAT

    val acr = Output(UInt(39.W))
    val phybase = Output(UInt(39.W))
    val pref_pat_addr = Input(UInt(11.W)) // SAVVINA 14/7 CHANGED PAT
    val pref_pat_atom = Output(UInt(64.W)) // SAVVINA 14/7 CHANGED PAT
  })

  val n = 4
  val cmd = Queue(io.cmd)
  val atomID = RegInit(34.U(log2Up(n)-1))
  val str_vaddr = RegInit(33.U(39.W))
  val mem_wdata = RegInit(33.U(64.W))
  val str_paddr = RegInit(34.U(39.W))
  val bytes_left = RegInit(0.U(39.W))
  val rows_left = RegInit(0.U(39.W))
  val amu_vaddr = RegInit(0.U(39.W))
  val is_map = RegInit(false.B)
  val is_unmap = RegInit(false.B)
  val is_map_2d = RegInit(false.B)
  val is_unmap_2d = RegInit(false.B)
  val verbose = RegInit(false.B)
  val mem_wen = RegInit(false.B)
  val atom_id = RegInit(0.U(11.W)) // SAVVINA 14/7 CHANGED PAT
  val attribute = RegInit(0.U(64.W))
  val atomoffset = RegInit(0.U(3.W))
  val baseBool = RegInit(false.B)

  if(isDummy)
    println("I won't spend time on mapping stuff")

  val status = RegEnable(cmd.bits.status, cmd.fire()) // && (is_amu_map))

  val inst = cmd.bits.inst
  val is_acr_read       = (inst.funct === 1.U)
  val is_acr_write      = (inst.funct === 2.U)
  val is_acr_clear      = (inst.funct === 3.U)
  val is_len_write      = (inst.funct === 4.U)
  val is_len_read       = (inst.funct === 5.U)
  val is_amu_map        = (inst.funct === 6.U)
  val is_amu_unmap      = (inst.funct === 7.U)
  val is_ast_activate   = (inst.funct === 8.U)
  val is_ast_deactivate = (inst.funct === 9.U)
  val is_ast_status     = (inst.funct === 10.U)
  val is_stride_write   = (inst.funct === 11.U)
  val is_stride_read    = (inst.funct === 12.U)
  val is_rowcnt_write   = (inst.funct === 13.U)
  val is_rowcnt_read    = (inst.funct === 14.U)
  val is_amu_map_2d     = (inst.funct === 15.U)
  val is_amu_unmap_2d   = (inst.funct === 16.U)
  val is_fatom_select   = (inst.funct === 18.U)
  val is_fatom_load     = (inst.funct === 19.U)
  
  val is_bc_load        = (inst.funct === 20.U)
  val is_bc_store       = (inst.funct === 21.U)
  val is_bc_atom_select = (inst.funct === 22.U)

  /* SAVVINA comment: According to the instruction called, contents of rs2 are stored in the corresponding register */

  val acr_reg = Module(new AtomAddressMapRegACR())
  acr_reg.io.wen := cmd.fire() && (is_acr_write || is_len_write || is_stride_write || is_rowcnt_write )
  acr_reg.io.waddr := Mux(is_acr_write, 0.U,
                      Mux(is_len_write, 1.U,
                      Mux(is_stride_write, 2.U,3.U)))
  acr_reg.io.wdata := cmd.bits.rs2
  acr_reg.io.clear := cmd.fire() && is_acr_clear

  io.acr := acr_reg.io.acr.asUInt
  io.phybase := acr_reg.io.rowCnt.asUInt

  /* SAVVINA comment: read the status of pref_addr and write 0 or 1 at address pointed by rs2, according to instruction called (activate/deactivate)*/

  /* SAVVINA comment out any AST related stuff */
/*
  val atomStatusTable = Module(new AtomStatusTable())

  atomStatusTable.io.pref_addr  := io.pref_ast_addr
  io.pref_ast_status            := atomStatusTable.io.pref_status

  atomStatusTable.io.wen := cmd.fire() && (is_ast_activate || is_ast_deactivate)
  atomStatusTable.io.AD := is_ast_activate
  atomStatusTable.io.waddr := cmd.bits.rs2
  atomStatusTable.io.raddr := cmd.bits.rs2
  val mem_atom_status = atomStatusTable.io.stat
*/

  val (s_idle :: s_translate :: s_fillpat_single :: s_write :: s_finish :: Nil) = Enum(5)
  val state = RegInit(s_idle)
 

  val pat = Module (new PrivateAttributeTable2048X11CycleRDLatencyNoren())

  /* SAVVINA comment: Read attribute stored at addr_pref */
  pat.io.addr_pref := io.pref_pat_addr
  io.pref_pat_atom := pat.io.atom_pref



  when(cmd.fire() && (is_fatom_select) && ((state === s_idle) && !isDummy.B)){
    printf("fatom_select: ")
    printf("rs1:%d ", cmd.bits.rs1)
    printf("rs2:%d\n", cmd.bits.rs2)
    atom_id    := cmd.bits.rs1
    state := s_finish
  }

  when(cmd.fire() && (is_fatom_load) && ((state === s_idle) && !isDummy.B))
  {
    printf("fatom_load: ")
    printf("atomoffset:%d ", cmd.bits.rs1)
    printf("attribute:%d\n", cmd.bits.rs2)
    attribute := cmd.bits.rs2
    atomoffset := cmd.bits.rs1
    state := s_fillpat_single
  }

  when((state === s_fillpat_single)){
    state := s_finish
  }

  pat.io.wen := state === s_fillpat_single
  pat.io.waddr := atom_id 
  pat.io.wdata := attribute 

  //----------------------------------------------------------------------------------------------//
  // TLB implementation
  // val tlb = Module(new FrontendTLB(1))//
  // io.ptw <> tlb.io.ptw
  // tlb.io.ptw.status := status

  io.mstatus := status
  val to_translate = RegInit(false.B)
  val tlb_sent = RegInit(true.B)
  val tlb_to_send = to_translate & !tlb_sent
  val ptw_error = false.B
  io.tlb.req.valid := tlb_to_send
  io.tlb.req.bits.vaddr := str_vaddr
  io.tlb.req.bits.passthrough := false.B
  io.tlb.req.bits.size := log2Ceil(4).U
  io.tlb.req.bits.cmd := M_XWR
  io.tlb.req.bits.prv := cmd.bits.status.prv // SAVVINA added, not existing in the original metasys
  //io.tlb.req.bits.prv := PRV.S.U // SAVVINA added, not existing in the original metasys
  io.tlb.req.bits.v := cmd.bits.status.v // SAVVINA added, not existing in the original metasys
  // io.tlb.req.bits.isLookup := false.B 

  io.tlb.resp.ready := true.B

  io.tlb.sfence.valid     := ptw_error
  io.tlb.sfence.bits.rs1  := true.B
  io.tlb.sfence.bits.rs2  := false.B
  io.tlb.sfence.bits.addr :=  str_vaddr
  io.tlb.sfence.bits.asid := 0.U
  io.tlb.sfence.bits.hv   := false.B // SAVVINA added, not existing in the original metasys
  io.tlb.sfence.bits.hg   := false.B // SAVVINA added, not existing in the original metasys

  io.tlb.kill := false.B // SAVVINA added, not existing in the original metasys
  
  when(io.tlb.req.fire())
  {
    tlb_sent := true.B
  }

  val timeout = RegInit(UInt(0,10.W))

  when(verbose)
  {
    timeout := timeout + UInt(1)
    //when(timeout > UInt(30)){
      //verbose := false.B
    //}
    printf("-- CMD (%d)  ", state)
    printf("v:%d  ", cmd.valid)
    printf("r:%d  ", cmd.ready)
    printf("b:%d  ", io.busy)
    printf("l:%d  ", acr_reg.io.len)
    printf("a:%d  ", acr_reg.io.acr)
    printf("rs1:%d  ", cmd.bits.inst.rs1)
    printf("rs2:%d  ", cmd.bits.inst.rs2)
    printf("rd:%d  ", cmd.bits.inst.rd)
    printf("inst:%d \n ", cmd.bits.inst.funct)

    printf("-- R (%d)  ", state)
    printf("v:%d  ", io.tlb.req.valid)
    printf("r:%d  ", io.tlb.req.ready)
    printf("va:%x  ", io.tlb.req.bits.vaddr)
    printf("ps:%d  \n", io.tlb.req.bits.passthrough)


    printf("-- S (%d)  ", state)
    printf("v:%d  ", io.tlb.resp.valid)
    printf("r:%d  ", io.tlb.resp.ready)
    printf("pa:%x  ", io.tlb.resp.bits.paddr)
    printf("ms:%d  ", io.tlb.resp.bits.miss)
    printf("ld:%d  ", io.tlb.resp.bits.pf.ld)
    printf("st:%d  ", io.tlb.resp.bits.pf.st)
    printf("cc:%d\n", io.tlb.resp.bits.cacheable)


    printf("-- MR (%d)  ", state)
    printf("v:%d  ", io.mem.req.valid)
    printf("r:%d  ", io.mem.req.ready)
    printf("ad:%x  ", io.mem.req.bits.addr)
    printf("tg:%d  ", io.mem.req.bits.tag)
    printf("cmd:%d  ", io.mem.req.bits.cmd)
  //  printf("typ:%d  ", io.mem.req.bits.typ)
    printf("data:%x  \n", io.mem.req.bits.data)


    printf("-- MS (%d)  ", state)
    printf("v: %d  ", io.mem.resp.valid)
    printf("r: %d \n", io.mem.resp.bits.data)

    printf("s2_xcpt : %d  ", io.mem.s2_xcpt.asUInt)
    printf("ma.st : %d  ", io.mem.s2_xcpt.ma.st)
    printf("ma.ld : %d  ", io.mem.s2_xcpt.ma.ld)
    printf("pf.st : %d  ", io.mem.s2_xcpt.pf.st)
    printf("pf.ld : %d  ", io.mem.s2_xcpt.pf.ld)
    printf("ae.st : %d  ", io.mem.s2_xcpt.ae.st)
    printf("ae.ld : %d\n", io.mem.s2_xcpt.ae.ld)



  }

    when(cmd.fire())
    {
      printf("-- CMDFIRE (%d)  ", state)
      printf("v:%d  ", cmd.valid)
      printf("r:%d  ", cmd.ready)
      printf("rs1:%d  ", cmd.bits.inst.rs1)
      printf("rs2:%d  ", cmd.bits.inst.rs2)
      printf("rd:%d  ", cmd.bits.inst.rd)
      printf("inst:%d\n", cmd.bits.inst.funct)
    }


  when(io.tlb.resp.fire())
  {
    to_translate := false.B
    str_paddr := io.tlb.resp.bits.paddr
  }

  //----------------------------------------------------------------------------------------------//


  when(cmd.fire() && (is_amu_map || is_amu_unmap || is_amu_map_2d || is_amu_unmap_2d) && (state === s_idle))
  {
    if(isDummy)
    {
      state := s_idle
    }else{
      state := s_translate
    }

    printf("State change from IDLE to TRANSLATE\n")
    printf("-- CMD (%d)  ", state)
    printf("vs_fillpat:%d  ", cmd.valid)
    printf("r:%d  ", cmd.ready)
    printf("b:%d  ", io.busy)
    printf("l:%d  ", acr_reg.io.len)
    printf("a:%d  ", acr_reg.io.acr)
    printf("rs1:%d  ", cmd.bits.inst.rs1)
    printf("rs2:%d  ", cmd.bits.inst.rs2)
    printf("rd:%d  ", cmd.bits.inst.rd)
    printf("inst:%d \n ", cmd.bits.inst.funct)
    str_vaddr := cmd.bits.rs1
    atomID    := cmd.bits.rs2(log2Up(n)-1,0)
      mem_wdata       := (cmd.bits.rs2 & "h000000000000ffff".U) |
    ((cmd.bits.rs2 << 16 ).asUInt & "h00000000ffff0000".U) |
    ((cmd.bits.rs2 << 32).asUInt & "h0000ffff00000000".U) |
    ((cmd.bits.rs2 << 48).asUInt & "hffff000000000000".U) 

    is_map := is_amu_map
    is_unmap := is_amu_unmap
    is_map_2d := is_amu_map_2d
    is_unmap_2d := is_amu_unmap_2d
    to_translate := true.B
    tlb_sent := false.B
    verbose := true.B
  }

  println("\nAtom Granularity " + atomGranularity + "\n")

  when((state === s_translate) && !to_translate) {
    printf("State change from TRANSLATE to WRITE\n")
    state := s_write

    val base = (str_paddr >> atomGranularity).asUInt

    val half = (1.U << (32 - atomGranularity)).asUInt

    val offset = Mux(base(0) === 1.U, base + 1.U, base + half)

    baseBool := base(0).asBool

    if (isPhysical) {
      amu_vaddr := (offset + acr_reg.io.acr.asUInt) + acr_reg.io.rowCnt
    } else {
      amu_vaddr := (offset + acr_reg.io.acr.asUInt)
    }

    bytes_left := acr_reg.io.len
    rows_left := Mux((is_amu_map || is_amu_unmap), 0.U, acr_reg.io.rowCnt - 1.U)
    mem_wen := true.B
  }

// TODO this never becomes valid when using physical stuff
  when((state === s_write) && io.mem.resp.valid)
  {
    when (bytes_left === 0.U && ((rows_left === 0.U && (is_map_2d || is_unmap_2d)) || is_map || is_unmap)) {

      printf("State change from WRITE to FINISH\n")
      state := s_finish
    }
    .elsewhen (bytes_left === 0.U && rows_left =/= 0.U && (is_map_2d || is_unmap_2d)) {
      bytes_left := acr_reg.io.len
      amu_vaddr := amu_vaddr + acr_reg.io.stride + 1.U
      rows_left := rows_left - 1.U
      mem_wen := true.B
    }
    .otherwise {
      str_paddr := str_paddr + atomGranularity.U

      val half = (1.U << (32 - atomGranularity)).asUInt
      
      amu_vaddr := Mux(baseBool, amu_vaddr - half + 2.U, amu_vaddr + half)
      mem_wen := true.B
    }
  }

  io.map_update_valid := state === s_write
  io.map_update_addr  := str_paddr
  io.map_update_id    := mem_wdata(10,0) // SAVVINA 14/7 CHANGED PAT

  when(state === s_finish)
  {
    printf("State change from FINISH to IDLE\n")
    state := s_idle
    verbose := false.B
    mem_wen := false.B
  }


  //----------------------------------------------------------------------------------------------//
  io.interrupt := false.B
  if(isDummy)
  {
    io.busy   := false.B
    cmd.ready := true.B
  }else
  {
    io.busy :=  cmd.valid  || (state =/= s_idle)
    cmd.ready :=
      is_acr_write || is_len_write || // Write can always go through immediately
      is_ast_activate || is_ast_deactivate ||
      is_stride_write || is_rowcnt_write ||
      (is_ast_status && io.resp.ready) ||
      (is_acr_read && io.resp.ready) ||
      (is_stride_read && io.resp.ready) ||
      (is_rowcnt_read && io.resp.ready) ||
      is_acr_clear ||
      (is_len_read && io.resp.ready) ||
      ( (is_amu_map || is_amu_unmap || is_amu_map_2d || is_amu_unmap_2d || is_fatom_select || is_fatom_load) && (state === s_idle ) )
  }

  // Memory Request Interface
  io.mem.req.valid := ((state === s_write) && mem_wen) 
  io.mem.req.bits.addr := amu_vaddr
  io.mem.req.bits.tag := 0.U
  io.mem.req.bits.cmd := M_XWR
  io.mem.req.bits.size := 1.U
  io.mem.req.bits.signed := false.B // SAVVINA added extra 
  io.mem.req.bits.dprv := cmd.bits.status.dprv
  //io.mem.req.bits.dprv := PRV.S.U // SAVVINA alternative
  io.mem.req.bits.dv := cmd.bits.status.dv
  // io.mem.req.bits.isLookup := false.B

  io.mem.req.bits.data := Mux((is_map || is_map_2d), mem_wdata.asUInt, 0.U)
  //io.mem.req.bits.mask := ~(0.U(coreDataBytes.W))
  io.mem.req.bits.mask := ~(0.U(8.W)) // SAVVINA alternative // SAVVINA 14/7 CHANGED PAT
  // io.mem.req.bits.paddr := 0.U

  io.mem.req.bits.phys := isPhysical.B
  io.mem.req.bits.no_alloc := false.B // SAVVINA
  io.mem.req.bits.no_xcpt := isPhysical.B

  io.mem.s1_kill := false.B
  io.mem.s1_data.data := 0.U
  io.mem.s1_data.mask := 0.U
  // io.mem.s1_data.paddr := 0.U//SAVVINA
  io.mem.s2_kill := false.B // SAVVINA
  io.mem.keep_clock_enabled := false.B // SAVVINA


  when( io.mem.req.fire() )
  {
    bytes_left := bytes_left - 1.U
    baseBool := ~baseBool
    mem_wen := false.B
    val mem_s2_nack = RegNext(RegNext(io.mem.s2_nack))
    val mem_s2_xcpt = RegNext(RegNext(io.mem.s2_xcpt))
  }

  io.resp.valid := (cmd.valid && (is_acr_read || is_len_read || is_stride_read || is_rowcnt_read || is_ast_status))
  io.resp.bits.rd := inst.rd
  /* SAVVINA commented out
  io.resp.bits.data := Mux(is_ast_status, mem_atom_status,
            Mux(is_stride_read, acr_reg.io.stride,
            Mux(is_rowcnt_read, acr_reg.io.rowCnt,
            Mux(is_acr_read, acr_reg.io.acr, acr_reg.io.len))))
            */

  io.resp.bits.data :=  Mux(is_stride_read, acr_reg.io.stride,
          Mux(is_rowcnt_read, acr_reg.io.rowCnt,
          Mux(is_acr_read, acr_reg.io.acr, acr_reg.io.len)))
}

class AtomAddressMapController512X11CycleRDLatencyNoren (val atomGranularity: Int = 9, isPhysical: Boolean = false, isDummy: Boolean = false) 
(implicit p: Parameters, edge: TLEdgeOut) extends CoreModule {
  val io = IO(new Bundle {
    val cmd = Flipped(Decoupled(new RoCCCommand)) //Atom cmd

    val resp = Decoupled(new RoCCResponse)
    val mem = new HellaCacheIO
    val busy = Output(Bool())
    val interrupt = Output(Bool())
    val tlb = new FrontendTLBIO
    val mstatus = Output(new MStatus)

    val map_update_addr   = Output(UInt(40.W))
    val map_update_valid  = Output(Bool())
    val map_update_id     = Output(UInt(9.W)) // SAVVINA 14/7 CHANGED PAT

    val acr = Output(UInt(39.W))
    val phybase = Output(UInt(39.W))
    val pref_pat_addr = Input(UInt(9.W)) // SAVVINA 14/7 CHANGED PAT
    val pref_pat_atom = Output(UInt(64.W)) // SAVVINA 14/7 CHANGED PAT
  })

  val n = 4
  val cmd = Queue(io.cmd)
  val atomID = RegInit(34.U(log2Up(n)-1))
  val str_vaddr = RegInit(33.U(39.W))
  val mem_wdata = RegInit(33.U(64.W))
  val str_paddr = RegInit(34.U(39.W))
  val bytes_left = RegInit(0.U(39.W))
  val rows_left = RegInit(0.U(39.W))
  val amu_vaddr = RegInit(0.U(39.W))
  val str_paddr_offset = RegInit(0.U(39.W))
  val is_map = RegInit(false.B)
  val is_unmap = RegInit(false.B)
  val is_map_2d = RegInit(false.B)
  val is_unmap_2d = RegInit(false.B)
  val verbose = RegInit(false.B)
  val mem_wen = RegInit(false.B)
  val atom_id = RegInit(0.U(9.W)) // SAVVINA 14/7 CHANGED PAT
  val attribute = RegInit(0.U(64.W))
  val atomoffset = RegInit(0.U(3.W))

  if(isDummy)
    println("I won't spend time on mapping stuff")

  val status = RegEnable(cmd.bits.status, cmd.fire()) // && (is_amu_map))

  val inst = cmd.bits.inst
  val is_acr_read       = (inst.funct === 1.U)
  val is_acr_write      = (inst.funct === 2.U)
  val is_acr_clear      = (inst.funct === 3.U)
  val is_len_write      = (inst.funct === 4.U)
  val is_len_read       = (inst.funct === 5.U)
  val is_amu_map        = (inst.funct === 6.U)
  val is_amu_unmap      = (inst.funct === 7.U)
  val is_ast_activate   = (inst.funct === 8.U)
  val is_ast_deactivate = (inst.funct === 9.U)
  val is_ast_status     = (inst.funct === 10.U)
  val is_stride_write   = (inst.funct === 11.U)
  val is_stride_read    = (inst.funct === 12.U)
  val is_rowcnt_write   = (inst.funct === 13.U)
  val is_rowcnt_read    = (inst.funct === 14.U)
  val is_amu_map_2d     = (inst.funct === 15.U)
  val is_amu_unmap_2d   = (inst.funct === 16.U)
  val is_fatom_select   = (inst.funct === 18.U)
  val is_fatom_load     = (inst.funct === 19.U)
  
  val is_bc_load        = (inst.funct === 20.U)
  val is_bc_store       = (inst.funct === 21.U)
  val is_bc_atom_select = (inst.funct === 22.U)

  /* SAVVINA comment: According to the instruction called, contents of rs2 are stored in the corresponding register */

  val acr_reg = Module(new AtomAddressMapRegACR())
  acr_reg.io.wen := cmd.fire() && (is_acr_write || is_len_write || is_stride_write || is_rowcnt_write )
  acr_reg.io.waddr := Mux(is_acr_write, 0.U,
                      Mux(is_len_write, 1.U,
                      Mux(is_stride_write, 2.U,3.U)))
  acr_reg.io.wdata := cmd.bits.rs2
  acr_reg.io.clear := cmd.fire() && is_acr_clear

  io.acr := acr_reg.io.acr.asUInt
  io.phybase := acr_reg.io.rowCnt.asUInt

  /* SAVVINA comment: read the status of pref_addr and write 0 or 1 at address pointed by rs2, according to instruction called (activate/deactivate)*/

  /* SAVVINA comment out any AST related stuff */
/*
  val atomStatusTable = Module(new AtomStatusTable())

  atomStatusTable.io.pref_addr  := io.pref_ast_addr
  io.pref_ast_status            := atomStatusTable.io.pref_status

  atomStatusTable.io.wen := cmd.fire() && (is_ast_activate || is_ast_deactivate)
  atomStatusTable.io.AD := is_ast_activate
  atomStatusTable.io.waddr := cmd.bits.rs2
  atomStatusTable.io.raddr := cmd.bits.rs2
  val mem_atom_status = atomStatusTable.io.stat
*/

  val (s_idle :: s_translate :: s_fillpat_single :: s_write :: s_finish :: Nil) = Enum(5)
  val state = RegInit(s_idle)
 

  val pat = Module (new PrivateAttributeTable512X11CycleRDLatencyNoren())

  /* SAVVINA comment: Read attribute stored at addr_pref */
  pat.io.addr_pref := io.pref_pat_addr
  io.pref_pat_atom := pat.io.atom_pref



  when(cmd.fire() && (is_fatom_select) && ((state === s_idle) && !isDummy.B)){
    printf("fatom_select: ")
    printf("rs1:%d ", cmd.bits.rs1)
    printf("rs2:%d\n", cmd.bits.rs2)
    atom_id    := cmd.bits.rs1
    state := s_finish
  }

  when(cmd.fire() && (is_fatom_load) && ((state === s_idle) && !isDummy.B))
  {
    printf("fatom_load: ")
    printf("atomoffset:%d ", cmd.bits.rs1)
    printf("attribute:%d\n", cmd.bits.rs2)
    attribute := cmd.bits.rs2
    atomoffset := cmd.bits.rs1
    state := s_fillpat_single
  }

  when((state === s_fillpat_single)){
    state := s_finish
  }

  pat.io.wen := state === s_fillpat_single
  pat.io.waddr := atom_id 
  pat.io.wdata := attribute 

  //----------------------------------------------------------------------------------------------//
  // TLB implementation
  // val tlb = Module(new FrontendTLB(1))//
  // io.ptw <> tlb.io.ptw
  // tlb.io.ptw.status := status

  io.mstatus := status
  val to_translate = RegInit(false.B)
  val tlb_sent = RegInit(true.B)
  val tlb_to_send = to_translate & !tlb_sent
  val ptw_error = false.B
  io.tlb.req.valid := tlb_to_send
  io.tlb.req.bits.vaddr := str_vaddr
  io.tlb.req.bits.passthrough := false.B
  io.tlb.req.bits.size := log2Ceil(4).U
  io.tlb.req.bits.cmd := M_XWR
  io.tlb.req.bits.prv := cmd.bits.status.prv // SAVVINA added, not existing in the original metasys
  //io.tlb.req.bits.prv := PRV.S.U // SAVVINA added, not existing in the original metasys
  io.tlb.req.bits.v := cmd.bits.status.v // SAVVINA added, not existing in the original metasys
  // io.tlb.req.bits.isLookup := false.B 

  io.tlb.resp.ready := true.B

  io.tlb.sfence.valid     := ptw_error
  io.tlb.sfence.bits.rs1  := true.B
  io.tlb.sfence.bits.rs2  := false.B
  io.tlb.sfence.bits.addr :=  str_vaddr
  io.tlb.sfence.bits.asid := 0.U
  io.tlb.sfence.bits.hv   := false.B // SAVVINA added, not existing in the original metasys
  io.tlb.sfence.bits.hg   := false.B // SAVVINA added, not existing in the original metasys

  io.tlb.kill := false.B // SAVVINA added, not existing in the original metasys
  
  when(io.tlb.req.fire())
  {
    tlb_sent := true.B
  }

  val timeout = RegInit(UInt(0,10.W))

  when(verbose)
  {
    timeout := timeout + UInt(1)
    //when(timeout > UInt(30)){
      //verbose := false.B
    //}
    printf("-- CMD (%d)  ", state)
    printf("v:%d  ", cmd.valid)
    printf("r:%d  ", cmd.ready)
    printf("b:%d  ", io.busy)
    printf("l:%d  ", acr_reg.io.len)
    printf("a:%d  ", acr_reg.io.acr)
    printf("rs1:%d  ", cmd.bits.inst.rs1)
    printf("rs2:%d  ", cmd.bits.inst.rs2)
    printf("rd:%d  ", cmd.bits.inst.rd)
    printf("inst:%d \n ", cmd.bits.inst.funct)

    printf("-- R (%d)  ", state)
    printf("v:%d  ", io.tlb.req.valid)
    printf("r:%d  ", io.tlb.req.ready)
    printf("va:%x  ", io.tlb.req.bits.vaddr)
    printf("ps:%d  \n", io.tlb.req.bits.passthrough)


    printf("-- S (%d)  ", state)
    printf("v:%d  ", io.tlb.resp.valid)
    printf("r:%d  ", io.tlb.resp.ready)
    printf("pa:%x  ", io.tlb.resp.bits.paddr)
    printf("ms:%d  ", io.tlb.resp.bits.miss)
    printf("ld:%d  ", io.tlb.resp.bits.pf.ld)
    printf("st:%d  ", io.tlb.resp.bits.pf.st)
    printf("cc:%d\n", io.tlb.resp.bits.cacheable)


    printf("-- MR (%d)  ", state)
    printf("v:%d  ", io.mem.req.valid)
    printf("r:%d  ", io.mem.req.ready)
    printf("ad:%x  ", io.mem.req.bits.addr)
    printf("tg:%d  ", io.mem.req.bits.tag)
    printf("cmd:%d  ", io.mem.req.bits.cmd)
  //  printf("typ:%d  ", io.mem.req.bits.typ)
    printf("data:%x  \n", io.mem.req.bits.data)


    printf("-- MS (%d)  ", state)
    printf("v: %d  ", io.mem.resp.valid)
    printf("r: %d \n", io.mem.resp.bits.data)

    printf("s2_xcpt : %d  ", io.mem.s2_xcpt.asUInt)
    printf("ma.st : %d  ", io.mem.s2_xcpt.ma.st)
    printf("ma.ld : %d  ", io.mem.s2_xcpt.ma.ld)
    printf("pf.st : %d  ", io.mem.s2_xcpt.pf.st)
    printf("pf.ld : %d  ", io.mem.s2_xcpt.pf.ld)
    printf("ae.st : %d  ", io.mem.s2_xcpt.ae.st)
    printf("ae.ld : %d\n", io.mem.s2_xcpt.ae.ld)



  }

    when(cmd.fire())
    {
      printf("-- CMDFIRE (%d)  ", state)
      printf("v:%d  ", cmd.valid)
      printf("r:%d  ", cmd.ready)
      printf("rs1:%d  ", cmd.bits.inst.rs1)
      printf("rs2:%d  ", cmd.bits.inst.rs2)
      printf("rd:%d  ", cmd.bits.inst.rd)
      printf("inst:%d\n", cmd.bits.inst.funct)
    }


  when(io.tlb.resp.fire())
  {
    to_translate := false.B
    str_paddr := io.tlb.resp.bits.paddr
  }

  //----------------------------------------------------------------------------------------------//


  when(cmd.fire() && (is_amu_map || is_amu_unmap || is_amu_map_2d || is_amu_unmap_2d) && (state === s_idle))
  {
    if(isDummy)
    {
      state := s_idle
    }else{
      state := s_translate
    }

    printf("State change from IDLE to TRANSLATE\n")
    printf("-- CMD (%d)  ", state)
    printf("vs_fillpat:%d  ", cmd.valid)
    printf("r:%d  ", cmd.ready)
    printf("b:%d  ", io.busy)
    printf("l:%d  ", acr_reg.io.len)
    printf("a:%d  ", acr_reg.io.acr)
    printf("rs1:%d  ", cmd.bits.inst.rs1)
    printf("rs2:%d  ", cmd.bits.inst.rs2)
    printf("rd:%d  ", cmd.bits.inst.rd)
    printf("inst:%d \n ", cmd.bits.inst.funct)
    str_vaddr := cmd.bits.rs1
    atomID    := cmd.bits.rs2(log2Up(n)-1,0)
      mem_wdata       := (cmd.bits.rs2 & "h000000000000ffff".U) |
    ((cmd.bits.rs2 << 16 ).asUInt & "h00000000ffff0000".U) |
    ((cmd.bits.rs2 << 32).asUInt & "h0000ffff00000000".U) |
    ((cmd.bits.rs2 << 48).asUInt & "hffff000000000000".U) 

    is_map := is_amu_map
    is_unmap := is_amu_unmap
    is_map_2d := is_amu_map_2d
    is_unmap_2d := is_amu_unmap_2d
    to_translate := true.B
    tlb_sent := false.B
    verbose := true.B
  }

  println("\nAtom Granularity " + atomGranularity + "\n")
  when((state === s_translate) && !to_translate) // SAVVINA added fatom_select_lookup functionality
  {
      printf("State change from TRANSLATE to WRITE\n")
      state := s_write
      str_paddr_offset := (str_paddr >> atomGranularity).asUInt
      if(isPhysical){
        amu_vaddr := ((str_paddr >> atomGranularity).asUInt + (acr_reg.io.acr).asUInt) + acr_reg.io.rowCnt//((str_paddr >> 9).asUInt() + (acr_reg.io.acr).asUInt())
      }
      else{
        amu_vaddr := ((str_paddr >> atomGranularity).asUInt + (acr_reg.io.acr).asUInt) //((str_paddr >> 9).asUInt() + (acr_reg.io.acr).asUInt())
      }
      bytes_left := acr_reg.io.len
      rows_left := Mux((is_amu_map || is_amu_unmap), 0.U, acr_reg.io.rowCnt - 1.U)
      mem_wen := true.B
  }


// TODO this never becomes valid when using physical stuff
  when((state === s_write) && io.mem.resp.valid)
  {
    when (bytes_left === 0.U && ((rows_left === 0.U && (is_map_2d || is_unmap_2d)) || is_map || is_unmap)) {

      printf("State change from WRITE to FINISH\n")
      state := s_finish
    }
    .elsewhen (bytes_left === 0.U && rows_left =/= 0.U && (is_map_2d || is_unmap_2d)) {
      bytes_left := acr_reg.io.len
      amu_vaddr := amu_vaddr + acr_reg.io.stride + 1.U
      rows_left := rows_left - 1.U
      mem_wen := true.B
    }
    .otherwise {
      str_paddr := str_paddr + atomGranularity.U
      amu_vaddr := amu_vaddr + 1.U
      mem_wen := true.B
    }
  }

  io.map_update_valid := state === s_write
  io.map_update_addr  := str_paddr
  io.map_update_id    := mem_wdata(8,0) // SAVVINA 14/7 CHANGED PAT

  when(state === s_finish)
  {
    printf("State change from FINISH to IDLE\n")
    state := s_idle
    verbose := false.B
    mem_wen := false.B
  }


  //----------------------------------------------------------------------------------------------//
  io.interrupt := false.B
  if(isDummy)
  {
    io.busy   := false.B
    cmd.ready := true.B
  }else
  {
    io.busy :=  cmd.valid  || (state =/= s_idle)
    cmd.ready :=
      is_acr_write || is_len_write || // Write can always go through immediately
      is_ast_activate || is_ast_deactivate ||
      is_stride_write || is_rowcnt_write ||
      (is_ast_status && io.resp.ready) ||
      (is_acr_read && io.resp.ready) ||
      (is_stride_read && io.resp.ready) ||
      (is_rowcnt_read && io.resp.ready) ||
      is_acr_clear ||
      (is_len_read && io.resp.ready) ||
      ( (is_amu_map || is_amu_unmap || is_amu_map_2d || is_amu_unmap_2d || is_fatom_select || is_fatom_load) && (state === s_idle ) )
  }

  // Memory Request Interface
  io.mem.req.valid := ((state === s_write) && mem_wen) 
  io.mem.req.bits.addr := amu_vaddr
  io.mem.req.bits.tag := 0.U
  io.mem.req.bits.cmd := M_XWR
  io.mem.req.bits.size := 0.U // SAVVINA 14/7 CHANGED PAT

  io.mem.req.bits.signed := false.B // SAVVINA added extra 
  io.mem.req.bits.dprv := cmd.bits.status.dprv
  //io.mem.req.bits.dprv := PRV.S.U // SAVVINA alternative
  io.mem.req.bits.dv := cmd.bits.status.dv
  // io.mem.req.bits.isLookup := false.B

  io.mem.req.bits.data := Mux((is_map || is_map_2d), mem_wdata.asUInt, 0.U)
  //io.mem.req.bits.mask := ~(0.U(coreDataBytes.W))
  io.mem.req.bits.mask := ~(0.U(8.W)) // SAVVINA alternative // SAVVINA 14/7 CHANGED PAT
  // io.mem.req.bits.paddr := 0.U

  io.mem.req.bits.phys := isPhysical.B
  io.mem.req.bits.no_alloc := false.B // SAVVINA
  io.mem.req.bits.no_xcpt := isPhysical.B

  io.mem.s1_kill := false.B
  io.mem.s1_data.data := 0.U
  io.mem.s1_data.mask := 0.U
  // io.mem.s1_data.paddr := 0.U//SAVVINA
  io.mem.s2_kill := false.B // SAVVINA
  io.mem.keep_clock_enabled := false.B // SAVVINA


  when( io.mem.req.fire() )
  {
    bytes_left := bytes_left - 1.U
    mem_wen := false.B
    val mem_s2_nack = RegNext(RegNext(io.mem.s2_nack))
    val mem_s2_xcpt = RegNext(RegNext(io.mem.s2_xcpt))
  }

  io.resp.valid := (cmd.valid && (is_acr_read || is_len_read || is_stride_read || is_rowcnt_read || is_ast_status))
  io.resp.bits.rd := inst.rd
  /* SAVVINA commented out
  io.resp.bits.data := Mux(is_ast_status, mem_atom_status,
            Mux(is_stride_read, acr_reg.io.stride,
            Mux(is_rowcnt_read, acr_reg.io.rowCnt,
            Mux(is_acr_read, acr_reg.io.acr, acr_reg.io.len))))
            */

  io.resp.bits.data :=  Mux(is_stride_read, acr_reg.io.stride,
          Mux(is_rowcnt_read, acr_reg.io.rowCnt,
          Mux(is_acr_read, acr_reg.io.acr, acr_reg.io.len)))
}


/* --- SAVVINA COMMENT --- */
/* ----- Differences compared to AtomAddressMapController ----- */
/* 1. Added functionality for new fatom_select_lookup instruction */

class AtomAddressMapControllerLookup (val atomGranularity: Int = 9, isPhysical: Boolean = false, isDummy: Boolean = false) 
(implicit p: Parameters, edge: TLEdgeOut) extends CoreModule {
  val io = IO(new Bundle {
    val cmd = Flipped(Decoupled(new RoCCCommand)) //Atom cmd

    val resp = Decoupled(new RoCCResponse)
    val mem = new HellaCacheIO
    val busy = Output(Bool())
    val interrupt = Output(Bool())
    val tlb = new FrontendTLBIO
    val mstatus = Output(new MStatus)

    val map_update_addr   = Output(UInt(40.W))
    val map_update_valid  = Output(Bool())
    val map_update_id     = Output(UInt(8.W)) 

    val acr = Output(UInt(39.W))
    val phybase = Output(UInt(39.W))
    val pref_pat_addr = Input(UInt(8.W)) 
    val pref_pat_atom = Output(UInt(512.W))
    
    val amu_lookup  = Flipped(new AMUDualALBLookUpIO) // SAVVINA added lookup interface for fatom_select_lookup functionality

    // val pref_ast_addr   = Input(UInt(8.W)) // SAVVINA: comment out AST related stuff
    // val pref_ast_status = Output(Bool())   // SAVVINA: comment out AST related stuff
  })

  val n = 4
  val cmd = Queue(io.cmd)
  val atomID = RegInit(34.U(log2Up(n)-1))
  val str_vaddr = RegInit(33.U(32.W))
  val mem_wdata = RegInit(33.U(64.W))
  val str_paddr = RegInit(34.U(32.W))
  val bytes_left = RegInit(0.U(39.W))
  val rows_left = RegInit(0.U(39.W))
  val amu_vaddr = RegInit(0.U(39.W))
  val str_paddr_offset = RegInit(0.U(32.W))
  val fill_pat_address = RegInit(0.U(32.W))
  val pat_counter = RegInit(0.U(8.W)) 
  val n_atoms = RegInit(0.U(8.W)) 
  val is_map = RegInit(false.B)
  val is_unmap = RegInit(false.B)
  val is_map_2d = RegInit(false.B)
  val is_unmap_2d = RegInit(false.B)
  val is_fatom_select_lookup_reg = RegInit(false.B) // SAVVINA added for new instruction functionality
  val verbose = RegInit(false.B)
  val mem_wen = RegInit(false.B)
  val atom_id = RegInit(0.U(8.W)) 
  val attribute = RegInit(0.U(64.W))
  val atomoffset = RegInit(0.U(4.W))

  if(isDummy)
    println("I won't spend time on mapping stuff")

  val status = RegEnable(cmd.bits.status, cmd.fire()) // && (is_amu_map))

  val inst = cmd.bits.inst
  val is_acr_read       = (inst.funct === 1.U)
  val is_acr_write      = (inst.funct === 2.U)
  val is_acr_clear      = (inst.funct === 3.U)
  val is_len_write      = (inst.funct === 4.U)
  val is_len_read       = (inst.funct === 5.U)
  val is_amu_map        = (inst.funct === 6.U)
  val is_amu_unmap      = (inst.funct === 7.U)
  val is_ast_activate   = (inst.funct === 8.U)
  val is_ast_deactivate = (inst.funct === 9.U)
  val is_ast_status     = (inst.funct === 10.U)
  val is_stride_write   = (inst.funct === 11.U)
  val is_stride_read    = (inst.funct === 12.U)
  val is_rowcnt_write   = (inst.funct === 13.U)
  val is_rowcnt_read    = (inst.funct === 14.U)
  val is_amu_map_2d     = (inst.funct === 15.U)
  val is_amu_unmap_2d   = (inst.funct === 16.U)
  val is_fatom          = (inst.funct === 17.U)
  val is_fatom_select   = (inst.funct === 18.U)
  val is_fatom_load     = (inst.funct === 19.U)
  
  val is_bc_atom_select = (inst.funct === 20.U)
  val is_fatom_select_lookup = (inst.funct === 21.U) // SAVVINA added new instruction


  /* SAVVINA comment: According to the instruction called, contents of rs2 are stored in the corresponding register */

  val acr_reg = Module(new AtomAddressMapRegACR())
  acr_reg.io.wen := cmd.fire() && (is_acr_write || is_len_write || is_stride_write || is_rowcnt_write )
  acr_reg.io.waddr := Mux(is_acr_write, 0.U,
                      Mux(is_len_write, 1.U,
                      Mux(is_stride_write, 2.U,3.U)))
  acr_reg.io.wdata := cmd.bits.rs2
  acr_reg.io.clear := cmd.fire() && is_acr_clear

  io.acr := acr_reg.io.acr.asUInt
  io.phybase := acr_reg.io.rowCnt.asUInt

  /* SAVVINA comment: read the status of pref_addr and write 0 or 1 at address pointed by rs2, according to instruction called (activate/deactivate)*/

  /* SAVVINA comment out any AST related stuff */
/*
  val atomStatusTable = Module(new AtomStatusTable())

  atomStatusTable.io.pref_addr  := io.pref_ast_addr
  io.pref_ast_status            := atomStatusTable.io.pref_status

  atomStatusTable.io.wen := cmd.fire() && (is_ast_activate || is_ast_deactivate)
  atomStatusTable.io.AD := is_ast_activate
  atomStatusTable.io.waddr := cmd.bits.rs2
  atomStatusTable.io.raddr := cmd.bits.rs2
  val mem_atom_status = atomStatusTable.io.stat
*/

  val (s_idle :: s_translate :: s_atomlookup :: s_wait_atomlookup :: s_fillpat :: s_fillpat_single :: s_readpat :: s_load_pat ::  s_send_pat_cache :: s_wait :: s_write :: s_finish :: Nil) = Enum(12)
  val state = RegInit(s_idle)
 

  val pat = Module (new PrivateAttributeTable())

  /* SAVVINA comment: Read attribute stored at addr_pref */
  pat.io.addr_pref := io.pref_pat_addr
  io.pref_pat_atom := pat.io.atom_pref



  when(cmd.fire() && (is_fatom_select) && ((state === s_idle) && !isDummy.B)){
    printf("fatom_select: ")
    printf("rs1:%d ", cmd.bits.rs1)
    printf("rs2:%d\n", cmd.bits.rs2)
    atom_id    := cmd.bits.rs1
    state := s_finish
  }

  when(cmd.fire() && (is_fatom_load) && ((state === s_idle) && !isDummy.B))
  {
    printf("fatom_load: ")
    printf("atomoffset:%d ", cmd.bits.rs1)
    printf("attribute:%d\n", cmd.bits.rs2)
    attribute := cmd.bits.rs2
    atomoffset := cmd.bits.rs1
    state := s_fillpat_single
  }

  when((state === s_fillpat_single)){
    state := s_finish
  }


  when(cmd.fire() && (is_fatom) && ((state === s_idle) && !isDummy.B))
  {
    printf("State change from IDLE to FILL_CACHE \n")
    printf("-- CMD (%d)  ", state)
    printf("v:%d  ", cmd.valid)
    printf("r:%d  ", cmd.ready)
    printf("b:%d  ", io.busy)
    printf("l:%d  ", acr_reg.io.len)
    printf("a:%d  ", acr_reg.io.acr)
    printf("rs1:%d  ", cmd.bits.rs1)
    printf("rs2:%d  ", cmd.bits.rs2)
    //printf("rd:%d  ", cmd.bits.rd)
    printf("inst:%d \n ", cmd.bits.inst.funct)
    fill_pat_address:= cmd.bits.rs1
    n_atoms:= cmd.bits.rs2
    state := s_fillpat
    pat_counter := 0.U
//    verbose := true.B

  }
   when((state === s_fillpat))
  {

    printf("Number of Atoms: %d\n", pat_counter)
    printf("Fill pat address: %d\n", fill_pat_address)
    state := s_readpat
    printf("============ MEM REQUEST ========== \n")
    printf("io.mem.req.valid := %d \n",io.mem.req.valid)
    printf("io.mem.req.bits.addr := %d\n",io.mem.req.bits.addr)
    printf("io.mem.req.bits.tag :=  %d\n",io.mem.req.bits.tag)
    printf(" io.mem.req.bits.cmd :=  %d\n",io.mem.req.bits.cmd)
    //printf(" io.mem.req.bits.typ :=  %d\n",io.mem.req.bits.typ)
    printf(" io.mem.req.bits.data :=  %d\n",io.mem.req.bits.data)
    printf(" io.mem.req.bits.phys := %d\n",io.mem.req.bits.phys)
    printf("===============================================\n")
    mem_wen := true.B
  }

  pat.io.wen := ((state === s_readpat) && io.mem.resp.valid ) || (state === s_fillpat_single)
  pat.io.addr := Mux((state === s_fillpat_single), atom_id, io.mem.resp.bits.data(7,0)) //not sure 
  pat.io.wdata := Mux((state === s_fillpat_single), attribute, io.mem.resp.bits.data(31,8)) //not sure 
  pat.io.ren := (state === s_send_pat_cache) && io.mem.req.fire() || state === s_load_pat
  pat.io.attindex := atomoffset 

  when(state === s_readpat && io.mem.resp.valid )
  {

        printf("============== READ_PAT STATE ==============\n")
        printf("Data from memory: %d\n",io.mem.resp.bits.data)
        printf( "Pat_counter: %d\n", pat_counter )
        printf("===========================================\n")

        when ( pat_counter === (n_atoms ))
        {
              state:= s_finish
              pat_counter := 1.U

        }
        .otherwise {
              fill_pat_address := fill_pat_address + 4.U
              mem_wen := true.B
        }


  }


  when(state === s_load_pat){
      state := s_send_pat_cache
      printf( "Pat_counter: %d\n", pat_counter )
      printf("Primitive: %d\n", pat.io.atom_prim)
      mem_wen := true.B

  }

  when(state === s_send_pat_cache && io.mem.resp.valid )
  {

        when ( pat_counter === (n_atoms+1.U) )
        {
              state:= s_finish
        }
        .otherwise {

            printf("============= SEND_PAT_CACHE  STATE ==========\n")
            printf( "Pat_counter: %d\n", pat_counter )
            printf("Primitive: %d\n", pat.io.atom_prim)

            printf("io.mem.req.valid := %d \n",io.mem.req.valid)
            printf("io.mem.req.bits.addr := %d\n",io.mem.req.bits.addr)
            printf("io.mem.req.bits.tag :=  %d\n",io.mem.req.bits.tag)
            printf(" io.mem.req.bits.cmd :=  %d\n",io.mem.req.bits.cmd)
          //  printf(" io.mem.req.bits.typ :=  %d\n",io.mem.req.bits.typ)
            printf(" io.mem.req.bits.data :=  %d\n",io.mem.s1_data.data)
            printf(" io.mem.req.bits.phys := %d\n",io.mem.req.bits.phys)

            printf(" =============================================\n")

            pat_counter := pat_counter + 1.U
            mem_wen := true.B
        }
  }

  //----------------------------------------------------------------------------------------------//
  // TLB implementation
  // val tlb = Module(new FrontendTLB(1))//
  // io.ptw <> tlb.io.ptw
  // tlb.io.ptw.status := status

  io.mstatus := status
  val to_translate = RegInit(false.B)
  val tlb_sent = RegInit(true.B)
  val tlb_to_send = to_translate & !tlb_sent
  val ptw_error = false.B
  io.tlb.req.valid := tlb_to_send
  io.tlb.req.bits.vaddr := str_vaddr
  io.tlb.req.bits.passthrough := false.B
  io.tlb.req.bits.size := log2Ceil(4).U
  io.tlb.req.bits.cmd := Mux(is_fatom_select_lookup_reg, M_XRD, M_XWR)
  io.tlb.req.bits.prv := cmd.bits.status.prv // SAVVINA added, not existing in the original metasys
  //io.tlb.req.bits.prv := PRV.S.U // SAVVINA added, not existing in the original metasys
  io.tlb.req.bits.v := cmd.bits.status.v // SAVVINA added, not existing in the original metasys
  // io.tlb.req.bits.isLookup := false.B 

  io.tlb.resp.ready := true.B

  io.tlb.sfence.valid     := ptw_error
  io.tlb.sfence.bits.rs1  := true.B
  io.tlb.sfence.bits.rs2  := false.B
  io.tlb.sfence.bits.addr :=  str_vaddr
  io.tlb.sfence.bits.asid := 0.U
  io.tlb.sfence.bits.hv   := false.B // SAVVINA added, not existing in the original metasys
  io.tlb.sfence.bits.hg   := false.B // SAVVINA added, not existing in the original metasys

  io.tlb.kill := false.B // SAVVINA added, not existing in the original metasys
  
  when(io.tlb.req.fire())
  {
    tlb_sent := true.B
  }

  val timeout = RegInit(UInt(0,10.W))

  when(verbose)
  {
    timeout := timeout + UInt(1)
    //when(timeout > UInt(30)){
      //verbose := false.B
    //}
    printf("-- CMD (%d)  ", state)
    printf("v:%d  ", cmd.valid)
    printf("r:%d  ", cmd.ready)
    printf("b:%d  ", io.busy)
    printf("l:%d  ", acr_reg.io.len)
    printf("a:%d  ", acr_reg.io.acr)
    printf("rs1:%d  ", cmd.bits.inst.rs1)
    printf("rs2:%d  ", cmd.bits.inst.rs2)
    printf("rd:%d  ", cmd.bits.inst.rd)
    printf("inst:%d \n ", cmd.bits.inst.funct)

    printf("-- R (%d)  ", state)
    printf("v:%d  ", io.tlb.req.valid)
    printf("r:%d  ", io.tlb.req.ready)
    printf("va:%x  ", io.tlb.req.bits.vaddr)
    printf("ps:%d  \n", io.tlb.req.bits.passthrough)


    printf("-- S (%d)  ", state)
    printf("v:%d  ", io.tlb.resp.valid)
    printf("r:%d  ", io.tlb.resp.ready)
    printf("pa:%x  ", io.tlb.resp.bits.paddr)
    printf("ms:%d  ", io.tlb.resp.bits.miss)
    printf("ld:%d  ", io.tlb.resp.bits.pf.ld)
    printf("st:%d  ", io.tlb.resp.bits.pf.st)
    printf("cc:%d\n", io.tlb.resp.bits.cacheable)


    printf("-- MR (%d)  ", state)
    printf("v:%d  ", io.mem.req.valid)
    printf("r:%d  ", io.mem.req.ready)
    printf("ad:%x  ", io.mem.req.bits.addr)
    printf("tg:%d  ", io.mem.req.bits.tag)
    printf("cmd:%d  ", io.mem.req.bits.cmd)
  //  printf("typ:%d  ", io.mem.req.bits.typ)
    printf("data:%x  \n", io.mem.req.bits.data)


    printf("-- MS (%d)  ", state)
    printf("v: %d  ", io.mem.resp.valid)
    printf("r: %d \n", io.mem.resp.bits.data)

    printf("s2_xcpt : %d  ", io.mem.s2_xcpt.asUInt)
    printf("ma.st : %d  ", io.mem.s2_xcpt.ma.st)
    printf("ma.ld : %d  ", io.mem.s2_xcpt.ma.ld)
    printf("pf.st : %d  ", io.mem.s2_xcpt.pf.st)
    printf("pf.ld : %d  ", io.mem.s2_xcpt.pf.ld)
    printf("ae.st : %d  ", io.mem.s2_xcpt.ae.st)
    printf("ae.ld : %d\n", io.mem.s2_xcpt.ae.ld)



  }

    when(cmd.fire())
    {
      printf("-- CMDFIRE (%d)  ", state)
      printf("v:%d  ", cmd.valid)
      printf("r:%d  ", cmd.ready)
      printf("rs1:%d  ", cmd.bits.inst.rs1)
      printf("rs2:%d  ", cmd.bits.inst.rs2)
      printf("rd:%d  ", cmd.bits.inst.rd)
      printf("inst:%d\n", cmd.bits.inst.funct)
    }


  when(io.tlb.resp.fire())
  {
    to_translate := false.B
    str_paddr := io.tlb.resp.bits.paddr
  }

  io.amu_lookup.req.valid := state === s_atomlookup
  io.amu_lookup.req.paddr := str_paddr
  io.amu_lookup.req.moduleid := true.B

  when(io.amu_lookup.req.valid && io.amu_lookup.req.ready){
    state := s_wait_atomlookup
    is_fatom_select_lookup_reg := false.B // SAVVINA reset the flag for new instruction functionality
  }

  when(state === s_wait_atomlookup && io.amu_lookup.resp.valid)
  {
    atom_id := io.amu_lookup.resp.atom_id(10,0) // SAVVINA 14/7 CHANGED PAT - HAVEN'T CHANGED THAT YET CAUSE NOT USED
    state := s_finish
  }
  .elsewhen(state === s_wait_atomlookup && io.amu_lookup.resp.xcpt)
  {
    state := s_idle
  }

  //----------------------------------------------------------------------------------------------//

  /* SAVVINA new "fatom_select_lookup" instruction functionality START*/
  when(cmd.fire() && (is_fatom_select_lookup) && ((state === s_idle) && !isDummy.B)){
    printf("fatom_select_lookup: ")
    printf("rs2:%d\n", cmd.bits.rs2)
    state := s_translate
    str_vaddr  := cmd.bits.rs2
    to_translate := true.B
    tlb_sent := false.B
    is_fatom_select_lookup_reg := true.B
  }
  /* SAVVINA new "fatom_select_lookup" instruction functionality END*/

  when(cmd.fire() && (is_amu_map || is_amu_unmap || is_amu_map_2d || is_amu_unmap_2d) && (state === s_idle))
  {
    if(isDummy)
    {
      state := s_idle
    }else{
      state := s_translate
    }

    printf("State change from IDLE to TRANSLATE\n")
    printf("-- CMD (%d)  ", state)
    printf("vs_fillpat:%d  ", cmd.valid)
    printf("r:%d  ", cmd.ready)
    printf("b:%d  ", io.busy)
    printf("l:%d  ", acr_reg.io.len)
    printf("a:%d  ", acr_reg.io.acr)
    printf("rs1:%d  ", cmd.bits.inst.rs1)
    printf("rs2:%d  ", cmd.bits.inst.rs2)
    printf("rd:%d  ", cmd.bits.inst.rd)
    printf("inst:%d \n ", cmd.bits.inst.funct)
    str_vaddr := cmd.bits.rs1
    atomID    := cmd.bits.rs2(log2Up(n)-1,0)
    mem_wdata       := (cmd.bits.rs2 & "h00000000000000ff".U) |
      ((cmd.bits.rs2 << 8 ).asUInt & "h000000000000ff00".U) |
      ((cmd.bits.rs2 << 16).asUInt & "h0000000000ff0000".U) |
      ((cmd.bits.rs2 << 24).asUInt & "h00000000ff000000".U) |
      ((cmd.bits.rs2 << 32).asUInt & "h000000ff00000000".U) |
      ((cmd.bits.rs2 << 40).asUInt & "h0000ff0000000000".U) |
      ((cmd.bits.rs2 << 48).asUInt & "h00ff000000000000".U) |
      ((cmd.bits.rs2 << 56).asUInt & "hff00000000000000".U)


    is_map := is_amu_map
    is_unmap := is_amu_unmap
    is_map_2d := is_amu_map_2d
    is_unmap_2d := is_amu_unmap_2d
    to_translate := true.B
    tlb_sent := false.B
    verbose := true.B
  }

  println("\nAtom Granularity " + atomGranularity + "\n")
  when((state === s_translate) && !to_translate) // SAVVINA added fatom_select_lookup functionality
  {
    when(!is_fatom_select_lookup_reg)
    {
      printf("State change from TRANSLATE to WRITE\n")
      state := s_write
      str_paddr_offset := (str_paddr >> atomGranularity).asUInt
      if(isPhysical){
        amu_vaddr := ((str_paddr >> atomGranularity).asUInt + (acr_reg.io.acr).asUInt) + acr_reg.io.rowCnt//((str_paddr >> 9).asUInt() + (acr_reg.io.acr).asUInt())
      }
      else{
        amu_vaddr := ((str_paddr >> atomGranularity).asUInt + (acr_reg.io.acr).asUInt) //((str_paddr >> 9).asUInt() + (acr_reg.io.acr).asUInt())
      }
      bytes_left := acr_reg.io.len
      rows_left := Mux((is_amu_map || is_amu_unmap), 0.U, acr_reg.io.rowCnt - 1.U)
      mem_wen := true.B
    }
    .otherwise
    {
      state := s_atomlookup
    }
  }


// TODO this never becomes valid when using physical stuff
  when((state === s_write) && io.mem.resp.valid)
  {
    when (bytes_left === 0.U && ((rows_left === 0.U && (is_map_2d || is_unmap_2d)) || is_map || is_unmap)) {

      printf("State change from WRITE to FINISH\n")
      state := s_finish
    }
    .elsewhen (bytes_left === 0.U && rows_left =/= 0.U && (is_map_2d || is_unmap_2d)) {
      bytes_left := acr_reg.io.len
      amu_vaddr := amu_vaddr + acr_reg.io.stride + 1.U
      rows_left := rows_left - 1.U
      mem_wen := true.B
    }
    .otherwise {
      str_paddr := str_paddr + atomGranularity.U
      amu_vaddr := amu_vaddr + 1.U
      mem_wen := true.B
    }
  }

  io.map_update_valid := state === s_write
  io.map_update_addr  := str_paddr
  io.map_update_id    := mem_wdata(7,0) 

  when(state === s_finish)
  {
    printf("State change from FINISH to IDLE\n")
    state := s_idle
    verbose := false.B
    mem_wen := false.B
  }


  //----------------------------------------------------------------------------------------------//
  io.interrupt := false.B
  if(isDummy)
  {
    io.busy   := false.B
    cmd.ready := true.B
  }else
  {
    io.busy :=  cmd.valid  || (state =/= s_idle)
    cmd.ready :=
      is_acr_write || is_len_write || // Write can always go through immediately
      is_ast_activate || is_ast_deactivate ||
      is_stride_write || is_rowcnt_write ||
      (is_ast_status && io.resp.ready) ||
      (is_acr_read && io.resp.ready) ||
      (is_stride_read && io.resp.ready) ||
      (is_rowcnt_read && io.resp.ready) ||
      is_acr_clear ||
      (is_len_read && io.resp.ready) ||
      ( (is_amu_map || is_amu_unmap || is_amu_map_2d || is_amu_unmap_2d || is_fatom || is_fatom_select || is_fatom_select_lookup  || is_fatom_load) && (state === s_idle ) )
  }

  // Memory Request Interface
  io.mem.req.valid := ((state === s_write) && mem_wen) || ((state === s_readpat) && mem_wen) || ((state === s_send_pat_cache) && mem_wen)
  io.mem.req.bits.addr := Mux((state === s_readpat),fill_pat_address,Mux(state === s_send_pat_cache || state === s_wait,fill_pat_address,amu_vaddr))
  io.mem.req.bits.tag := Mux((state === s_readpat),pat_counter,Mux(state === s_send_pat_cache || state === s_wait, pat_counter,0.U))
  io.mem.req.bits.cmd := Mux((state === s_readpat),M_XRD,Mux((state === s_send_pat_cache || state === s_wait), M_XWR, M_XWR))
  io.mem.req.bits.size := Mux((state === s_readpat) || (state === s_send_pat_cache || state === s_wait), log2Ceil(4).U, 0.U)
  //io.mem.req.bits.size := Mux((state === s_readpat) || (state === s_send_pat_cache || state === s_wait), log2Ceil(4).U, log2Ceil(8).U) // SAVVINA alternative 
  io.mem.req.bits.signed := false.B // SAVVINA added extra 
  io.mem.req.bits.dprv := cmd.bits.status.dprv
  //io.mem.req.bits.dprv := PRV.S.U // SAVVINA alternative
  io.mem.req.bits.dv := cmd.bits.status.dv
  // io.mem.req.bits.isLookup := false.B

  io.mem.req.bits.data := Mux((is_map || is_map_2d), mem_wdata.asUInt, Mux(state === s_send_pat_cache, pat.io.atom_prim, 0.U))
  //io.mem.req.bits.mask := ~(0.U(coreDataBytes.W))
  io.mem.req.bits.mask := ~(0.U(8.W)) // SAVVINA alternative 
  // io.mem.req.bits.paddr := 0.U

  io.mem.req.bits.phys := isPhysical.B
  io.mem.req.bits.no_alloc := false.B // SAVVINA
  io.mem.req.bits.no_xcpt := isPhysical.B

  io.mem.s1_kill := false.B
  io.mem.s1_data.data := Mux(state === s_send_pat_cache, pat.io.atom_prim, 0.U)
  io.mem.s1_data.mask := 0.U
  // io.mem.s1_data.paddr := 0.U//SAVVINA
  io.mem.s2_kill := false.B // SAVVINA
  io.mem.keep_clock_enabled := false.B // SAVVINA


  when( io.mem.req.fire() && !(state === s_readpat) )
  {
    bytes_left := bytes_left - 1.U
    mem_wen := false.B
    val mem_s2_nack = RegNext(RegNext(io.mem.s2_nack))
    val mem_s2_xcpt = RegNext(RegNext(io.mem.s2_xcpt))
  }
  .elsewhen( io.mem.req.fire() && (state === s_readpat) )
  {
    pat_counter := pat_counter + 1.U
    mem_wen := false.B
  }


  when( io.mem.req.fire() && (state === s_send_pat_cache) )
  {
    printf("SEND_PAT_CACHE - Memory request fire\n")
    printf("Pat.io.atom_prim = %d \n", pat.io.atom_prim)
    mem_wen := false.B
  }

  io.resp.valid := (cmd.valid && (is_acr_read || is_len_read || is_stride_read || is_rowcnt_read || is_ast_status))
  io.resp.bits.rd := inst.rd
  /* SAVVINA commented out
  io.resp.bits.data := Mux(is_ast_status, mem_atom_status,
            Mux(is_stride_read, acr_reg.io.stride,
            Mux(is_rowcnt_read, acr_reg.io.rowCnt,
            Mux(is_acr_read, acr_reg.io.acr, acr_reg.io.len))))
            */

  io.resp.bits.data :=  Mux(is_stride_read, acr_reg.io.stride,
          Mux(is_rowcnt_read, acr_reg.io.rowCnt,
          Mux(is_acr_read, acr_reg.io.acr, acr_reg.io.len)))
}

/* --- SAVVINA COMMENT --- */
/* ----- Differences compared to AtomAddressMapControllerLookup ----- */
/* 1. tagID equal to 0 is considered invalid in this module because it could match:
      a. either to a zero address
      b. or to an invalid address (not associated with a tagID)
      c. or to a previously valid address, which initially has a tagID, but later unmap was called for it
   2. when is_fatom_select_lookup instruction is called, the module checks whether the address given
      as argument is zero and, if it is, it assigns tagID 0 and returns to idle state 
   3. in unmap, the data written to memory is zero (changed XMemLib implementation for that)
   4. If fatom load is called, there is a check whether atom_id is 0, in which case state becomes idle without
      any action
*/

class AtomAddressMapControllerLookupMoreChecks (val atomGranularity: Int = 9, isPhysical: Boolean = false, isDummy: Boolean = false) 
(implicit p: Parameters, edge: TLEdgeOut) extends CoreModule {
  val io = IO(new Bundle {
    val cmd = Flipped(Decoupled(new RoCCCommand)) //Atom cmd

    val resp = Decoupled(new RoCCResponse)
    val mem = new HellaCacheIO
    val busy = Output(Bool())
    val interrupt = Output(Bool())
    val tlb = new FrontendTLBIO
    val mstatus = Output(new MStatus)

    val map_update_addr   = Output(UInt(40.W))
    val map_update_valid  = Output(Bool())
    val map_update_id     = Output(UInt(8.W)) 

    val acr = Output(UInt(39.W))
    val phybase = Output(UInt(39.W))
    val pref_pat_addr = Input(UInt(8.W)) 
    val pref_pat_atom = Output(UInt(512.W))
    
    //val amu_lookup  = Flipped(new AMUDualALBLookUpIO) // SAVVINA added lookup interface for fatom_select_lookup functionality
    //val amu_lookup = Flipped(new AMUDualALBLookUpReturnModuleIdIO)
    val amu_lookup  = Flipped(new AMULookUpIO)

    // val pref_ast_addr   = Input(UInt(8.W)) // SAVVINA: comment out AST related stuff
    // val pref_ast_status = Output(Bool())   // SAVVINA: comment out AST related stuff
  })

  val n = 4
  val cmd = Queue(io.cmd)
  val atomID = RegInit(34.U(log2Up(n)-1))
  val str_vaddr = RegInit(33.U(32.W))
  val mem_wdata = RegInit(33.U(64.W))
  val str_paddr = RegInit(34.U(32.W))
  val bytes_left = RegInit(0.U(39.W))
  val rows_left = RegInit(0.U(39.W))
  val amu_vaddr = RegInit(0.U(39.W))
  val str_paddr_offset = RegInit(0.U(32.W))
  val fill_pat_address = RegInit(0.U(32.W))
  val pat_counter = RegInit(0.U(8.W)) 
  val n_atoms = RegInit(0.U(8.W)) 
  val is_map = RegInit(false.B)
  val is_unmap = RegInit(false.B)
  val is_map_2d = RegInit(false.B)
  val is_unmap_2d = RegInit(false.B)
  val is_fatom_select_lookup_reg = RegInit(false.B) // SAVVINA added for new instruction functionality
  val verbose = RegInit(false.B)
  val mem_wen = RegInit(false.B)
  val atom_id = RegInit(0.U(8.W)) 
  val attribute = RegInit(0.U(64.W))
  val atomoffset = RegInit(0.U(4.W))

  if(isDummy)
    println("I won't spend time on mapping stuff")

  val status = RegEnable(cmd.bits.status, cmd.fire()) // && (is_amu_map))

  val inst = cmd.bits.inst
  val is_acr_read       = (inst.funct === 1.U)
  val is_acr_write      = (inst.funct === 2.U)
  val is_acr_clear      = (inst.funct === 3.U)
  val is_len_write      = (inst.funct === 4.U)
  val is_len_read       = (inst.funct === 5.U)
  val is_amu_map        = (inst.funct === 6.U)
  val is_amu_unmap      = (inst.funct === 7.U)
  val is_ast_activate   = (inst.funct === 8.U)
  val is_ast_deactivate = (inst.funct === 9.U)
  val is_ast_status     = (inst.funct === 10.U)
  val is_stride_write   = (inst.funct === 11.U)
  val is_stride_read    = (inst.funct === 12.U)
  val is_rowcnt_write   = (inst.funct === 13.U)
  val is_rowcnt_read    = (inst.funct === 14.U)
  val is_amu_map_2d     = (inst.funct === 15.U)
  val is_amu_unmap_2d   = (inst.funct === 16.U)
  val is_fatom          = (inst.funct === 17.U)
  val is_fatom_select   = (inst.funct === 18.U)
  val is_fatom_load     = (inst.funct === 19.U)
  
  val is_bc_atom_select = (inst.funct === 20.U)
  val is_fatom_select_lookup = (inst.funct === 21.U) // SAVVINA added new instruction


  /* SAVVINA comment: According to the instruction called, contents of rs2 are stored in the corresponding register */

  val acr_reg = Module(new AtomAddressMapRegACR())
  acr_reg.io.wen := cmd.fire() && (is_acr_write || is_len_write || is_stride_write || is_rowcnt_write )
  acr_reg.io.waddr := Mux(is_acr_write, 0.U,
                      Mux(is_len_write, 1.U,
                      Mux(is_stride_write, 2.U,3.U)))
  acr_reg.io.wdata := cmd.bits.rs2
  acr_reg.io.clear := cmd.fire() && is_acr_clear

  io.acr := acr_reg.io.acr.asUInt
  io.phybase := acr_reg.io.rowCnt.asUInt

  /* SAVVINA comment: read the status of pref_addr and write 0 or 1 at address pointed by rs2, according to instruction called (activate/deactivate)*/

  /* SAVVINA comment out any AST related stuff */
/*
  val atomStatusTable = Module(new AtomStatusTable())

  atomStatusTable.io.pref_addr  := io.pref_ast_addr
  io.pref_ast_status            := atomStatusTable.io.pref_status

  atomStatusTable.io.wen := cmd.fire() && (is_ast_activate || is_ast_deactivate)
  atomStatusTable.io.AD := is_ast_activate
  atomStatusTable.io.waddr := cmd.bits.rs2
  atomStatusTable.io.raddr := cmd.bits.rs2
  val mem_atom_status = atomStatusTable.io.stat
*/

  val (s_idle :: s_translate :: s_atomlookup :: s_wait_atomlookup :: s_fillpat :: s_fillpat_single :: s_readpat :: s_load_pat ::  s_send_pat_cache :: s_wait :: s_write :: s_finish :: Nil) = Enum(12)
  val state = RegInit(s_idle)
 

  val pat = Module (new PrivateAttributeTable())

  /* SAVVINA comment: Read attribute stored at addr_pref */
  pat.io.addr_pref := io.pref_pat_addr
  io.pref_pat_atom := pat.io.atom_pref



  when(cmd.fire() && (is_fatom_select) && ((state === s_idle) && !isDummy.B)){
    printf("fatom_select: ")
    printf("rs1:%d ", cmd.bits.rs1)
    printf("rs2:%d\n", cmd.bits.rs2)
    atom_id    := cmd.bits.rs1
    state := s_finish
  }

  when(cmd.fire() && (is_fatom_load) && ((state === s_idle) && !isDummy.B))
  {
    printf("fatom_load: ")
    printf("atomoffset:%d ", cmd.bits.rs1)
    printf("attribute:%d\n", cmd.bits.rs2)
    when (atom_id === 0.U)
    {
      state := s_finish
    }.otherwise
    {
      attribute := cmd.bits.rs2
      atomoffset := cmd.bits.rs1
      state := s_fillpat_single
    }
  }

  when((state === s_fillpat_single)){
    state := s_finish
  }


  when(cmd.fire() && (is_fatom) && ((state === s_idle) && !isDummy.B))
  {
    printf("State change from IDLE to FILL_CACHE \n")
    printf("-- CMD (%d)  ", state)
    printf("v:%d  ", cmd.valid)
    printf("r:%d  ", cmd.ready)
    printf("b:%d  ", io.busy)
    printf("l:%d  ", acr_reg.io.len)
    printf("a:%d  ", acr_reg.io.acr)
    printf("rs1:%d  ", cmd.bits.rs1)
    printf("rs2:%d  ", cmd.bits.rs2)
    //printf("rd:%d  ", cmd.bits.rd)
    printf("inst:%d \n ", cmd.bits.inst.funct)
    fill_pat_address:= cmd.bits.rs1
    n_atoms:= cmd.bits.rs2
    state := s_fillpat
    pat_counter := 0.U
//    verbose := true.B

  }
   when((state === s_fillpat))
  {

    printf("Number of Atoms: %d\n", pat_counter)
    printf("Fill pat address: %d\n", fill_pat_address)
    state := s_readpat
    printf("============ MEM REQUEST ========== \n")
    printf("io.mem.req.valid := %d \n",io.mem.req.valid)
    printf("io.mem.req.bits.addr := %d\n",io.mem.req.bits.addr)
    printf("io.mem.req.bits.tag :=  %d\n",io.mem.req.bits.tag)
    printf(" io.mem.req.bits.cmd :=  %d\n",io.mem.req.bits.cmd)
    //printf(" io.mem.req.bits.typ :=  %d\n",io.mem.req.bits.typ)
    printf(" io.mem.req.bits.data :=  %d\n",io.mem.req.bits.data)
    printf(" io.mem.req.bits.phys := %d\n",io.mem.req.bits.phys)
    printf("===============================================\n")
    mem_wen := true.B
  }

  pat.io.wen := ((state === s_readpat) && io.mem.resp.valid ) || (state === s_fillpat_single)
  pat.io.addr := Mux((state === s_fillpat_single), atom_id, io.mem.resp.bits.data(7,0)) //not sure 
  pat.io.wdata := Mux((state === s_fillpat_single), attribute, io.mem.resp.bits.data(31,8)) //not sure 
  pat.io.ren := (state === s_send_pat_cache) && io.mem.req.fire() || state === s_load_pat
  pat.io.attindex := atomoffset 

  when(state === s_readpat && io.mem.resp.valid )
  {

        printf("============== READ_PAT STATE ==============\n")
        printf("Data from memory: %d\n",io.mem.resp.bits.data)
        printf( "Pat_counter: %d\n", pat_counter )
        printf("===========================================\n")

        when ( pat_counter === (n_atoms ))
        {
              state:= s_finish
              pat_counter := 1.U

        }
        .otherwise {
              fill_pat_address := fill_pat_address + 4.U
              mem_wen := true.B
        }


  }


  when(state === s_load_pat){
      state := s_send_pat_cache
      printf( "Pat_counter: %d\n", pat_counter )
      printf("Primitive: %d\n", pat.io.atom_prim)
      mem_wen := true.B

  }

  when(state === s_send_pat_cache && io.mem.resp.valid )
  {

        when ( pat_counter === (n_atoms+1.U) )
        {
              state:= s_finish
        }
        .otherwise {

            printf("============= SEND_PAT_CACHE  STATE ==========\n")
            printf( "Pat_counter: %d\n", pat_counter )
            printf("Primitive: %d\n", pat.io.atom_prim)

            printf("io.mem.req.valid := %d \n",io.mem.req.valid)
            printf("io.mem.req.bits.addr := %d\n",io.mem.req.bits.addr)
            printf("io.mem.req.bits.tag :=  %d\n",io.mem.req.bits.tag)
            printf(" io.mem.req.bits.cmd :=  %d\n",io.mem.req.bits.cmd)
          //  printf(" io.mem.req.bits.typ :=  %d\n",io.mem.req.bits.typ)
            printf(" io.mem.req.bits.data :=  %d\n",io.mem.s1_data.data)
            printf(" io.mem.req.bits.phys := %d\n",io.mem.req.bits.phys)

            printf(" =============================================\n")

            pat_counter := pat_counter + 1.U
            mem_wen := true.B
        }
  }

  //----------------------------------------------------------------------------------------------//
  // TLB implementation
  // val tlb = Module(new FrontendTLB(1))//
  // io.ptw <> tlb.io.ptw
  // tlb.io.ptw.status := status

  io.mstatus := status
  val to_translate = RegInit(false.B)
  val tlb_sent = RegInit(true.B)
  val tlb_to_send = to_translate & !tlb_sent
  val ptw_error = false.B
  io.tlb.req.valid := tlb_to_send
  io.tlb.req.bits.vaddr := str_vaddr
  io.tlb.req.bits.passthrough := false.B
  io.tlb.req.bits.size := log2Ceil(4).U
  io.tlb.req.bits.cmd := Mux(is_fatom_select_lookup_reg, M_XRD, M_XWR)
  io.tlb.req.bits.prv := cmd.bits.status.prv // SAVVINA added, not existing in the original metasys
  //io.tlb.req.bits.prv := PRV.S.U // SAVVINA added, not existing in the original metasys
  io.tlb.req.bits.v := cmd.bits.status.v // SAVVINA added, not existing in the original metasys
  // io.tlb.req.bits.isLookup := false.B 

  io.tlb.resp.ready := true.B

  io.tlb.sfence.valid     := ptw_error
  io.tlb.sfence.bits.rs1  := true.B
  io.tlb.sfence.bits.rs2  := false.B
  io.tlb.sfence.bits.addr :=  str_vaddr
  io.tlb.sfence.bits.asid := 0.U
  io.tlb.sfence.bits.hv   := false.B // SAVVINA added, not existing in the original metasys
  io.tlb.sfence.bits.hg   := false.B // SAVVINA added, not existing in the original metasys

  io.tlb.kill := false.B // SAVVINA added, not existing in the original metasys
  
  when(io.tlb.req.fire())
  {
    tlb_sent := true.B
  }

  val timeout = RegInit(UInt(0,10.W))

  when(verbose)
  {
    timeout := timeout + UInt(1)
    //when(timeout > UInt(30)){
      //verbose := false.B
    //}
    printf("-- CMD (%d)  ", state)
    printf("v:%d  ", cmd.valid)
    printf("r:%d  ", cmd.ready)
    printf("b:%d  ", io.busy)
    printf("l:%d  ", acr_reg.io.len)
    printf("a:%d  ", acr_reg.io.acr)
    printf("rs1:%d  ", cmd.bits.inst.rs1)
    printf("rs2:%d  ", cmd.bits.inst.rs2)
    printf("rd:%d  ", cmd.bits.inst.rd)
    printf("inst:%d \n ", cmd.bits.inst.funct)

    printf("-- R (%d)  ", state)
    printf("v:%d  ", io.tlb.req.valid)
    printf("r:%d  ", io.tlb.req.ready)
    printf("va:%x  ", io.tlb.req.bits.vaddr)
    printf("ps:%d  \n", io.tlb.req.bits.passthrough)


    printf("-- S (%d)  ", state)
    printf("v:%d  ", io.tlb.resp.valid)
    printf("r:%d  ", io.tlb.resp.ready)
    printf("pa:%x  ", io.tlb.resp.bits.paddr)
    printf("ms:%d  ", io.tlb.resp.bits.miss)
    printf("ld:%d  ", io.tlb.resp.bits.pf.ld)
    printf("st:%d  ", io.tlb.resp.bits.pf.st)
    printf("cc:%d\n", io.tlb.resp.bits.cacheable)


    printf("-- MR (%d)  ", state)
    printf("v:%d  ", io.mem.req.valid)
    printf("r:%d  ", io.mem.req.ready)
    printf("ad:%x  ", io.mem.req.bits.addr)
    printf("tg:%d  ", io.mem.req.bits.tag)
    printf("cmd:%d  ", io.mem.req.bits.cmd)
  //  printf("typ:%d  ", io.mem.req.bits.typ)
    printf("data:%x  \n", io.mem.req.bits.data)


    printf("-- MS (%d)  ", state)
    printf("v: %d  ", io.mem.resp.valid)
    printf("r: %d \n", io.mem.resp.bits.data)

    printf("s2_xcpt : %d  ", io.mem.s2_xcpt.asUInt)
    printf("ma.st : %d  ", io.mem.s2_xcpt.ma.st)
    printf("ma.ld : %d  ", io.mem.s2_xcpt.ma.ld)
    printf("pf.st : %d  ", io.mem.s2_xcpt.pf.st)
    printf("pf.ld : %d  ", io.mem.s2_xcpt.pf.ld)
    printf("ae.st : %d  ", io.mem.s2_xcpt.ae.st)
    printf("ae.ld : %d\n", io.mem.s2_xcpt.ae.ld)



  }

    when(cmd.fire())
    {
      printf("-- CMDFIRE (%d)  ", state)
      printf("v:%d  ", cmd.valid)
      printf("r:%d  ", cmd.ready)
      printf("rs1:%d  ", cmd.bits.inst.rs1)
      printf("rs2:%d  ", cmd.bits.inst.rs2)
      printf("rd:%d  ", cmd.bits.inst.rd)
      printf("inst:%d\n", cmd.bits.inst.funct)
    }


  when(io.tlb.resp.fire())
  {
    to_translate := false.B
    str_paddr := io.tlb.resp.bits.paddr
  }

  io.amu_lookup.req.valid := state === s_atomlookup
  io.amu_lookup.req.paddr := str_paddr
  //io.amu_lookup.req.moduleid := true.B

  when(io.amu_lookup.req.valid && io.amu_lookup.req.ready){
    state := s_wait_atomlookup
    is_fatom_select_lookup_reg := false.B // SAVVINA reset the flag for new instruction functionality
  }

  when(state === s_wait_atomlookup && io.amu_lookup.resp.valid)
  {
    atom_id := io.amu_lookup.resp.atom_id(7,0) // SAVVINA 14/7 CHANGED PAT - HAVEN'T CHANGED THAT YET CAUSE NOT USED
    state := s_finish
  }
  .elsewhen(state === s_wait_atomlookup && io.amu_lookup.resp.xcpt)
  {
    atom_id := 0.U 
    state := s_finish
  }

  //----------------------------------------------------------------------------------------------//

  /* SAVVINA new "fatom_select_lookup" instruction functionality START*/
  when(cmd.fire() && (is_fatom_select_lookup) && ((state === s_idle) && !isDummy.B)){
    printf("fatom_select_lookup: ")
    printf("rs2:%d\n", cmd.bits.rs2)
    when (cmd.bits.rs2 === 0.U)
    {
      atom_id := 0.U
      state := s_idle
    }.otherwise
    {
      state := s_translate
      str_vaddr  := cmd.bits.rs2
      to_translate := true.B
      tlb_sent := false.B
      is_fatom_select_lookup_reg := true.B
    }
  }
  /* SAVVINA new "fatom_select_lookup" instruction functionality END*/

  when(cmd.fire() && (is_amu_map || is_amu_unmap || is_amu_map_2d || is_amu_unmap_2d) && (state === s_idle))
  {
    if(isDummy)
    {
      state := s_idle
    }else{
      state := s_translate
    }

    printf("State change from IDLE to TRANSLATE\n")
    printf("-- CMD (%d)  ", state)
    printf("vs_fillpat:%d  ", cmd.valid)
    printf("r:%d  ", cmd.ready)
    printf("b:%d  ", io.busy)
    printf("l:%d  ", acr_reg.io.len)
    printf("a:%d  ", acr_reg.io.acr)
    printf("rs1:%d  ", cmd.bits.inst.rs1)
    printf("rs2:%d  ", cmd.bits.inst.rs2)
    printf("rd:%d  ", cmd.bits.inst.rd)
    printf("inst:%d \n ", cmd.bits.inst.funct)
    str_vaddr := cmd.bits.rs1
    atomID    := cmd.bits.rs2(log2Up(n)-1,0)
    mem_wdata       := (cmd.bits.rs2 & "h00000000000000ff".U) |
      ((cmd.bits.rs2 << 8 ).asUInt & "h000000000000ff00".U) |
      ((cmd.bits.rs2 << 16).asUInt & "h0000000000ff0000".U) |
      ((cmd.bits.rs2 << 24).asUInt & "h00000000ff000000".U) |
      ((cmd.bits.rs2 << 32).asUInt & "h000000ff00000000".U) |
      ((cmd.bits.rs2 << 40).asUInt & "h0000ff0000000000".U) |
      ((cmd.bits.rs2 << 48).asUInt & "h00ff000000000000".U) |
      ((cmd.bits.rs2 << 56).asUInt & "hff00000000000000".U)


    is_map := is_amu_map
    is_unmap := is_amu_unmap
    is_map_2d := is_amu_map_2d
    is_unmap_2d := is_amu_unmap_2d
    to_translate := true.B
    tlb_sent := false.B
    verbose := true.B
  }

  println("\nAtom Granularity " + atomGranularity + "\n")
  when((state === s_translate) && !to_translate) // SAVVINA added fatom_select_lookup functionality
  {
    when(!is_fatom_select_lookup_reg)
    {
      printf("State change from TRANSLATE to WRITE\n")
      state := s_write
      str_paddr_offset := (str_paddr >> atomGranularity).asUInt
      if(isPhysical){
        amu_vaddr := ((str_paddr >> atomGranularity).asUInt + (acr_reg.io.acr).asUInt) + acr_reg.io.rowCnt//((str_paddr >> 9).asUInt() + (acr_reg.io.acr).asUInt())
      }
      else{
        amu_vaddr := ((str_paddr >> atomGranularity).asUInt + (acr_reg.io.acr).asUInt) //((str_paddr >> 9).asUInt() + (acr_reg.io.acr).asUInt())
      }
      bytes_left := acr_reg.io.len
      rows_left := Mux((is_amu_map || is_amu_unmap), 0.U, acr_reg.io.rowCnt - 1.U)
      mem_wen := true.B
    }
    .otherwise
    {
      state := s_atomlookup
    }
  }


// TODO this never becomes valid when using physical stuff
  when((state === s_write) && io.mem.resp.valid)
  {
    when (bytes_left === 0.U && ((rows_left === 0.U && (is_map_2d || is_unmap_2d)) || is_map || is_unmap)) {

      printf("State change from WRITE to FINISH\n")
      state := s_finish
    }
    .elsewhen (bytes_left === 0.U && rows_left =/= 0.U && (is_map_2d || is_unmap_2d)) {
      bytes_left := acr_reg.io.len
      amu_vaddr := amu_vaddr + acr_reg.io.stride + 1.U
      rows_left := rows_left - 1.U
      mem_wen := true.B
    }
    .otherwise {
      str_paddr := str_paddr + atomGranularity.U
      amu_vaddr := amu_vaddr + 1.U
      mem_wen := true.B
    }
  }

  io.map_update_valid := state === s_write
  io.map_update_addr  := str_paddr
  io.map_update_id    := mem_wdata(7,0) 

  when(state === s_finish)
  {
    printf("State change from FINISH to IDLE\n")
    state := s_idle
    verbose := false.B
    mem_wen := false.B
  }


  //----------------------------------------------------------------------------------------------//
  io.interrupt := false.B
  if(isDummy)
  {
    io.busy   := false.B
    cmd.ready := true.B
  }else
  {
    io.busy :=  cmd.valid  || (state =/= s_idle)
    cmd.ready :=
      is_acr_write || is_len_write || // Write can always go through immediately
      is_ast_activate || is_ast_deactivate ||
      is_stride_write || is_rowcnt_write ||
      (is_ast_status && io.resp.ready) ||
      (is_acr_read && io.resp.ready) ||
      (is_stride_read && io.resp.ready) ||
      (is_rowcnt_read && io.resp.ready) ||
      is_acr_clear ||
      (is_len_read && io.resp.ready) ||
      ( (is_amu_map || is_amu_unmap || is_amu_map_2d || is_amu_unmap_2d || is_fatom || is_fatom_select || is_fatom_select_lookup  || is_fatom_load) && (state === s_idle ) )
  }

  // Memory Request Interface
  io.mem.req.valid := ((state === s_write) && mem_wen) || ((state === s_readpat) && mem_wen) || ((state === s_send_pat_cache) && mem_wen)
  io.mem.req.bits.addr := Mux((state === s_readpat),fill_pat_address,Mux(state === s_send_pat_cache || state === s_wait,fill_pat_address,amu_vaddr))
  io.mem.req.bits.tag := Mux((state === s_readpat),pat_counter,Mux(state === s_send_pat_cache || state === s_wait, pat_counter,0.U))
  io.mem.req.bits.cmd := Mux((state === s_readpat),M_XRD,Mux((state === s_send_pat_cache || state === s_wait), M_XWR, M_XWR))
  io.mem.req.bits.size := Mux((state === s_readpat) || (state === s_send_pat_cache || state === s_wait), log2Ceil(4).U, 0.U)
  //io.mem.req.bits.size := Mux((state === s_readpat) || (state === s_send_pat_cache || state === s_wait), log2Ceil(4).U, log2Ceil(8).U) // SAVVINA alternative 
  io.mem.req.bits.signed := false.B // SAVVINA added extra 
  io.mem.req.bits.dprv := cmd.bits.status.dprv
  //io.mem.req.bits.dprv := PRV.S.U // SAVVINA alternative
  io.mem.req.bits.dv := cmd.bits.status.dv
  // io.mem.req.bits.isLookup := false.B

  io.mem.req.bits.data := Mux((is_map || is_map_2d), mem_wdata.asUInt, Mux(state === s_send_pat_cache, pat.io.atom_prim, 0.U))
  //io.mem.req.bits.mask := ~(0.U(coreDataBytes.W))
  io.mem.req.bits.mask := ~(0.U(8.W)) // SAVVINA alternative 
  // io.mem.req.bits.paddr := 0.U

  io.mem.req.bits.phys := isPhysical.B
  io.mem.req.bits.no_alloc := false.B // SAVVINA
  io.mem.req.bits.no_xcpt := isPhysical.B

  io.mem.s1_kill := false.B
  io.mem.s1_data.data := Mux(state === s_send_pat_cache, pat.io.atom_prim, 0.U)
  io.mem.s1_data.mask := 0.U
  // io.mem.s1_data.paddr := 0.U//SAVVINA
  io.mem.s2_kill := false.B // SAVVINA
  io.mem.keep_clock_enabled := false.B // SAVVINA


  when( io.mem.req.fire() && !(state === s_readpat) )
  {
    bytes_left := bytes_left - 1.U
    mem_wen := false.B
    val mem_s2_nack = RegNext(RegNext(io.mem.s2_nack))
    val mem_s2_xcpt = RegNext(RegNext(io.mem.s2_xcpt))
  }
  .elsewhen( io.mem.req.fire() && (state === s_readpat) )
  {
    pat_counter := pat_counter + 1.U
    mem_wen := false.B
  }


  when( io.mem.req.fire() && (state === s_send_pat_cache) )
  {
    printf("SEND_PAT_CACHE - Memory request fire\n")
    printf("Pat.io.atom_prim = %d \n", pat.io.atom_prim)
    mem_wen := false.B
  }

  io.resp.valid := (cmd.valid && (is_acr_read || is_len_read || is_stride_read || is_rowcnt_read || is_ast_status))
  io.resp.bits.rd := inst.rd
  /* SAVVINA commented out
  io.resp.bits.data := Mux(is_ast_status, mem_atom_status,
            Mux(is_stride_read, acr_reg.io.stride,
            Mux(is_rowcnt_read, acr_reg.io.rowCnt,
            Mux(is_acr_read, acr_reg.io.acr, acr_reg.io.len))))
            */

  io.resp.bits.data :=  Mux(is_stride_read, acr_reg.io.stride,
          Mux(is_rowcnt_read, acr_reg.io.rowCnt,
          Mux(is_acr_read, acr_reg.io.acr, acr_reg.io.len)))
}

class AtomTLBWrapper(implicit p: Parameters, edge: TLEdgeOut) extends Module{
  val io = IO(new Bundle {
    val ptw         = new TLBPTWIO
    val amu_status  = Input(new MStatus)
    val amu         = Flipped(new FrontendTLBIO)
    // TODO Need to know if we need status here
    // val pref_status = Input(new MStatus)
    val pref        = Flipped(new FrontendTLBIO)
  })

  // One of the requestors is AMU, other one is the prefetcher
  val tlb = Module(new FrontendTLB(2)) //Module(new FrontendTLB(2))
  io.ptw <> tlb.io.ptw

  tlb.io.ptw.status := io.amu_status //Mux(io.amu.req.valid, io.amu_status, io.pref_status)

  tlb.io.clients(0) <> io.amu
  tlb.io.clients(1) <> io.pref
}

class AtomController(opcodes: OpcodeSet, atomGranularity: Int = 20, isPhysical: Boolean = false, isDummy: Boolean = false)(implicit p: Parameters) extends LazyRoCC(opcodes) {
  override lazy val module = new AtomControllerModule(this, atomGranularity, isPhysical, isDummy)
  override val atlNode = TLClientNode(Seq(TLClientPortParameters(Seq(TLClientParameters("AtomCtrlr")))))
}

class AtomControllerModule (outer: AtomController,
  atomGranularity: Int = 9, isPhysical: Boolean = false, isDummy: Boolean = false) (implicit p: Parameters)
  extends LazyRoCCModuleImp(outer){

  // io.mem.req.bits.no_alloc <> DontCare // SAVVINA
  // io.mem.req.bits.no_xcpt <> DontCare // SAVVINA
  // io.mem.req.bits.mask <> DontCare // SAVVINA
  // io.mem.s1_kill <> DontCare // SAVVINA  
  // io.mem.s1_data <> DontCare // SAVVINA 
  // io.mem.s2_kill <> DontCare // SAVVINA
  // io.mem.keep_clock_enabled <> DontCare // SAVVINA
  // io.mem_amulookup.req <> DontCare // SAVVINA
  // io.mem_amulookup.s1_kill <> DontCare // SAVVINA
  // io.mem_amulookup.s1_data <> DontCare // SAVVINA
  // io.mem_amulookup.s2_kill <> DontCare // SAVVINA
  // io.mem_amulookup.keep_clock_enabled <> DontCare // SAVVINA
  // io.core_snoop.bp_q_full <> DontCare // SAVVINA
  // io.core_snoop.bc_resolved <> DontCare // SAVVINA
  // io.core_snoop.dbg1 <> DontCare // SAVVINA
  // io.core_snoop.dbg2 <> DontCare // SAVVINA
  // io.core_snoop.dbg3 <> DontCare // SAVVINA
  // io.core_snoop.dbg4 <> DontCare // SAVVINA
  // io.ptw_snoop.bp_q_full <> DontCare // SAVVINA
  // io.ptw_snoop.bc_resolved <> DontCare // SAVVINA
  // io.ptw_snoop.dbg1 <> DontCare // SAVVINA
  // io.ptw_snoop.dbg2 <> DontCare // SAVVINA
  // io.ptw_snoop.dbg3 <> DontCare // SAVVINA
  // io.ptw_snoop.dbg4 <> DontCare // SAVVINA
  // io.fpu_req.bits <> DontCare // SAVVINA
  // io.fpu_req.valid <> DontCare // SAVVINA
  // io.fpu_resp.ready <> DontCare // SAVVINA

  val (tl_out, edgesOut) = outer.atlNode.out(0)
  implicit val edge = edgesOut
  //val tlb_wrapper = Module(new AtomTLBWrapper) // SAVVINA commented out and replaced with next line
  val tlb = Module(new DecoupledTLB(1,4)) // SAVVINA no need for 2 clients, prefetcher doesn't use TLB
  val ctrl = Module(new AtomAddressMapController(atomGranularity,isPhysical,isDummy))

  // RoCC related connections
  ctrl.io.cmd                 <>  io.cmd
  io.resp                     <>  ctrl.io.resp
  io.interrupt                := ctrl.io.interrupt
  io.busy                     := ctrl.io.busy

/* SAVVINA comment lines 883-887 and replace with lines 889-890 */

  // connect modueles to the TLB they are sharing
/*  tlb_wrapper.io.amu          <>  ctrl.io.tlb
  tlb_wrapper.io.amu_status   :=  ctrl.io.mstatus
  tlb_wrapper.io.pref         <>  DontCare
  io.ptw.head                 <>  tlb_wrapper.io.ptw */

  tlb.io.req <> ctrl.io.tlb.req
  tlb.io.resp <> ctrl.io.tlb.resp
  tlb.io.sfence <> ctrl.io.tlb.sfence
  tlb.io.kill <> ctrl.io.tlb.kill
  io.ptw.head <> tlb.io.ptw

  // bank memory ifaces
  io.mem                      <>  ctrl.io.mem
  io.mem_pref                 <>  DontCare

  io.mem_amulookup            <>  DontCare
  io.core_snoop               <>  DontCare
  io.ptw_snoop                <>  DontCare

  io.fpu_req                 <>  DontCare // SAVVINA added because fiirtl considers it not initialised (not existing in the original metasys) 
  io.fpu_resp                <>  DontCare // SAVVINA added because fiirtl considers it not initialised (not existing in the original metasys)

  ctrl.io.pref_pat_addr       := DontCare
  //ctrl.io.pref_ast_addr       := DontCare

}

class AtomControllerLookupMoreChecks(opcodes: OpcodeSet, albSize: Int = 32, atomGranularity: Int = 20, isPhysical: Boolean = false, isDummy: Boolean = false)(implicit p: Parameters) extends LazyRoCC(opcodes) {
  override lazy val module = new AtomControllerLookupMoreChecksModule(this, albSize, atomGranularity, isPhysical, isDummy)
  override val atlNode = TLClientNode(Seq(TLClientPortParameters(Seq(TLClientParameters("AtomCtrlLookupMoreChecks")))))
}

class AtomControllerLookupMoreChecksModule (outer: AtomControllerLookupMoreChecks, albSize: Int = 32,
  atomGranularity: Int = 9, isPhysical: Boolean = false, isDummy: Boolean = false) (implicit p: Parameters)
  extends LazyRoCCModuleImp(outer){

  // io.mem.req.bits.no_alloc <> DontCare // SAVVINA
  // io.mem.req.bits.no_xcpt <> DontCare // SAVVINA
  // io.mem.req.bits.mask <> DontCare // SAVVINA
  // io.mem.s1_kill <> DontCare // SAVVINA  
  // io.mem.s1_data <> DontCare // SAVVINA 
  // io.mem.s2_kill <> DontCare // SAVVINA
  // io.mem.keep_clock_enabled <> DontCare // SAVVINA
  // io.mem_amulookup.req <> DontCare // SAVVINA
  // io.mem_amulookup.s1_kill <> DontCare // SAVVINA
  // io.mem_amulookup.s1_data <> DontCare // SAVVINA
  // io.mem_amulookup.s2_kill <> DontCare // SAVVINA
  // io.mem_amulookup.keep_clock_enabled <> DontCare // SAVVINA
  // io.core_snoop.bp_q_full <> DontCare // SAVVINA
  // io.core_snoop.bc_resolved <> DontCare // SAVVINA
  // io.core_snoop.dbg1 <> DontCare // SAVVINA
  // io.core_snoop.dbg2 <> DontCare // SAVVINA
  // io.core_snoop.dbg3 <> DontCare // SAVVINA
  // io.core_snoop.dbg4 <> DontCare // SAVVINA
  // io.ptw_snoop.bp_q_full <> DontCare // SAVVINA
  // io.ptw_snoop.bc_resolved <> DontCare // SAVVINA
  // io.ptw_snoop.dbg1 <> DontCare // SAVVINA
  // io.ptw_snoop.dbg2 <> DontCare // SAVVINA
  // io.ptw_snoop.dbg3 <> DontCare // SAVVINA
  // io.ptw_snoop.dbg4 <> DontCare // SAVVINA
  // io.fpu_req.bits <> DontCare // SAVVINA
  // io.fpu_req.valid <> DontCare // SAVVINA
  // io.fpu_resp.ready <> DontCare // SAVVINA

  val (tl_out, edgesOut) = outer.atlNode.out(0)
  implicit val edge = edgesOut
  //val tlb_wrapper = Module(new AtomTLBWrapper) // SAVVINA commented out and replaced with next line
  val tlb = Module(new DecoupledTLB(1,4)) // SAVVINA no need for 2 clients, prefetcher doesn't use TLB
  val ctrl = Module(new AtomAddressMapControllerLookupMoreChecks(atomGranularity,isPhysical,isDummy))

  // RoCC related connections
  ctrl.io.cmd                 <>  io.cmd
  io.resp                     <>  ctrl.io.resp
  io.interrupt                := ctrl.io.interrupt
  io.busy                     := ctrl.io.busy

/* SAVVINA comment lines 883-887 and replace with lines 889-890 */

  // connect modueles to the TLB they are sharing
/*  tlb_wrapper.io.amu          <>  ctrl.io.tlb
  tlb_wrapper.io.amu_status   :=  ctrl.io.mstatus
  tlb_wrapper.io.pref         <>  DontCare
  io.ptw.head                 <>  tlb_wrapper.io.ptw */

  tlb.io.req <> ctrl.io.tlb.req
  tlb.io.resp <> ctrl.io.tlb.resp
  tlb.io.sfence <> ctrl.io.tlb.sfence
  tlb.io.kill <> ctrl.io.tlb.kill
  io.ptw.head <> tlb.io.ptw

  val lookup = Module(new BuffedALU(atomGranularity,albSize,false)) // SAVVINA added for new instruction "fatom_select_lookup"

  // Connect AMU_LOOKUP module to the AtomAddressMapController
  // and supply it with acr
  lookup.io.acrbase           := ctrl.io.acr
  lookup.io.phybase           := ctrl.io.phybase
  lookup.io.lookup            <> ctrl.io.amu_lookup
  lookup.io.map_update_id     := ctrl.io.map_update_id
  lookup.io.map_update_addr   := ctrl.io.map_update_addr
  lookup.io.map_update_valid  := ctrl.io.map_update_valid

  // bank memory ifaces
  io.mem                      <>  ctrl.io.mem
  io.mem_pref                 <>  DontCare
  io.mem_amulookup            <>  lookup.io.mem // SAVVINA added for new instruction "fatom_select_lookup"

  io.core_snoop               <>  DontCare
  io.ptw_snoop                <>  DontCare

  io.fpu_req                 <>  DontCare // SAVVINA added because fiirtl considers it not initialised (not existing in the original metasys) 
  io.fpu_resp                <>  DontCare // SAVVINA added because fiirtl considers it not initialised (not existing in the original metasys)

  ctrl.io.pref_pat_addr       := DontCare
  //ctrl.io.pref_ast_addr       := DontCare

}

class AtomControllerLookupMoreChecksDualALUSavvina(opcodes: OpcodeSet, albSize1: Int = 32, albSize2: Int = 32, atomGranularity: Int = 20, isPhysical: Boolean = false, isDummy: Boolean = false)(implicit p: Parameters) extends LazyRoCC(opcodes) {
  override lazy val module = new AtomControllerLookupMoreChecksDualALUSavvinaModule(this, albSize1, albSize2, atomGranularity, isPhysical, isDummy)
  override val atlNode = TLClientNode(Seq(TLClientPortParameters(Seq(TLClientParameters("AtomCtrlLookupMoreChecksDualALUSavvina")))))
}

class AtomControllerLookupMoreChecksDualALUSavvinaModule (outer: AtomControllerLookupMoreChecksDualALUSavvina, albSize1: Int = 32, albSize2: Int = 32,
  atomGranularity: Int = 9, isPhysical: Boolean = false, isDummy: Boolean = false) (implicit p: Parameters)
  extends LazyRoCCModuleImp(outer){

  // io.mem.req.bits.no_alloc <> DontCare // SAVVINA
  // io.mem.req.bits.no_xcpt <> DontCare // SAVVINA
  // io.mem.req.bits.mask <> DontCare // SAVVINA
  // io.mem.s1_kill <> DontCare // SAVVINA  
  // io.mem.s1_data <> DontCare // SAVVINA 
  // io.mem.s2_kill <> DontCare // SAVVINA
  // io.mem.keep_clock_enabled <> DontCare // SAVVINA
  // io.mem_amulookup.req <> DontCare // SAVVINA
  // io.mem_amulookup.s1_kill <> DontCare // SAVVINA
  // io.mem_amulookup.s1_data <> DontCare // SAVVINA
  // io.mem_amulookup.s2_kill <> DontCare // SAVVINA
  // io.mem_amulookup.keep_clock_enabled <> DontCare // SAVVINA
  // io.core_snoop.bp_q_full <> DontCare // SAVVINA
  // io.core_snoop.bc_resolved <> DontCare // SAVVINA
  // io.core_snoop.dbg1 <> DontCare // SAVVINA
  // io.core_snoop.dbg2 <> DontCare // SAVVINA
  // io.core_snoop.dbg3 <> DontCare // SAVVINA
  // io.core_snoop.dbg4 <> DontCare // SAVVINA
  // io.ptw_snoop.bp_q_full <> DontCare // SAVVINA
  // io.ptw_snoop.bc_resolved <> DontCare // SAVVINA
  // io.ptw_snoop.dbg1 <> DontCare // SAVVINA
  // io.ptw_snoop.dbg2 <> DontCare // SAVVINA
  // io.ptw_snoop.dbg3 <> DontCare // SAVVINA
  // io.ptw_snoop.dbg4 <> DontCare // SAVVINA
  // io.fpu_req.bits <> DontCare // SAVVINA
  // io.fpu_req.valid <> DontCare // SAVVINA
  // io.fpu_resp.ready <> DontCare // SAVVINA

  val (tl_out, edgesOut) = outer.atlNode.out(0)
  implicit val edge = edgesOut
  //val tlb_wrapper = Module(new AtomTLBWrapper) // SAVVINA commented out and replaced with next line
  val tlb = Module(new DecoupledTLB(1,4)) // SAVVINA no need for 2 clients, prefetcher doesn't use TLB
  val ctrl = Module(new AtomAddressMapControllerLookupMoreChecks(atomGranularity,isPhysical,isDummy))

  // RoCC related connections
  ctrl.io.cmd                 <>  io.cmd
  io.resp                     <>  ctrl.io.resp
  io.interrupt                := ctrl.io.interrupt
  io.busy                     := ctrl.io.busy

/* SAVVINA comment lines 883-887 and replace with lines 889-890 */

  // connect modueles to the TLB they are sharing
/*  tlb_wrapper.io.amu          <>  ctrl.io.tlb
  tlb_wrapper.io.amu_status   :=  ctrl.io.mstatus
  tlb_wrapper.io.pref         <>  DontCare
  io.ptw.head                 <>  tlb_wrapper.io.ptw */

  tlb.io.req <> ctrl.io.tlb.req
  tlb.io.resp <> ctrl.io.tlb.resp
  tlb.io.sfence <> ctrl.io.tlb.sfence
  tlb.io.kill <> ctrl.io.tlb.kill
  io.ptw.head <> tlb.io.ptw

  val lookup = Module(new BuffedALUDualALBSavvinaChanges(atomGranularity, albSize1, albSize2, false))

  // Connect AMU_LOOKUP module to the AtomAddressMapController
  // and supply it with acr
  lookup.io.acrbase           := ctrl.io.acr
  lookup.io.phybase           := ctrl.io.phybase
  lookup.io.lookup            <> ctrl.io.amu_lookup
  lookup.io.map_update_id     := ctrl.io.map_update_id
  lookup.io.map_update_addr   := ctrl.io.map_update_addr
  lookup.io.map_update_valid  := ctrl.io.map_update_valid

  // bank memory ifaces
  io.mem                      <>  ctrl.io.mem
  io.mem_pref                 <>  DontCare
  io.mem_amulookup            <>  lookup.io.mem // SAVVINA added for new instruction "fatom_select_lookup"

  io.core_snoop               <>  DontCare
  io.ptw_snoop                <>  DontCare

  io.fpu_req                 <>  DontCare // SAVVINA added because fiirtl considers it not initialised (not existing in the original metasys) 
  io.fpu_resp                <>  DontCare // SAVVINA added because fiirtl considers it not initialised (not existing in the original metasys)

  ctrl.io.pref_pat_addr       := DontCare
  //ctrl.io.pref_ast_addr       := DontCare

}

class AtomAddressMapController2048X11CycleRDLatencyNorenConfig(opcodes: OpcodeSet, atomGranularity: Int = 9, isPhysical: Boolean = false, isDummy: Boolean = false)(implicit p: Parameters) extends LazyRoCC(opcodes) {
  override lazy val module = new AtomAddressMapController2048X11CycleRDLatencyNorenConfigModule(this, atomGranularity, isPhysical, isDummy)
  override val atlNode = TLClientNode(Seq(TLClientPortParameters(Seq(TLClientParameters("AtomAddressMapController2048X11CycleRDLatencyNorenConfig")))))
}

class AtomAddressMapController2048X11CycleRDLatencyNorenConfigModule (outer: AtomAddressMapController2048X11CycleRDLatencyNorenConfig, atomGranularity: Int = 9, isPhysical: Boolean = false, isDummy: Boolean = false) (implicit p: Parameters)
  extends LazyRoCCModuleImp(outer){

  val (tl_out, edgesOut) = outer.atlNode.out(0)
  implicit val edge = edgesOut
  //val tlb_wrapper = Module(new AtomTLBWrapper) // SAVVINA commented out and replaced with next line
  val tlb = Module(new DecoupledTLB(1,4)) // SAVVINA no need for 2 clients, prefetcher doesn't use TLB
  val ctrl = Module(new AtomAddressMapController2048X11CycleRDLatencyNoren(atomGranularity, isPhysical,isDummy))

  // RoCC related connections
  ctrl.io.cmd                 <>  io.cmd
  io.resp                     <>  ctrl.io.resp
  io.interrupt                := ctrl.io.interrupt
  io.busy                     := ctrl.io.busy

  /* SAVVINA comment lines 478-481 and replace with lines 889-890 */
  // connect modueles to the TLB they are sharing
  /*tlb_wrapper.io.amu          <>  ctrl.io.tlb
  tlb_wrapper.io.amu_status   :=  ctrl.io.mstatus
  tlb_wrapper.io.pref         <>  pref.io.tlb
  // tlb_wrapper.io.pref_status  :=  pref.io.status
  io.ptw.head                 <>  tlb_wrapper.io.ptw*/
  
  tlb.io.req    <> ctrl.io.tlb.req
  tlb.io.resp   <> ctrl.io.tlb.resp
  tlb.io.sfence <> ctrl.io.tlb.sfence
  tlb.io.kill   <> ctrl.io.tlb.kill
  io.ptw.head   <> tlb.io.ptw

  // bank memory ifaces
  io.mem                      <>  ctrl.io.mem
  io.mem_pref                 <>  DontCare

  io.mem_amulookup            <>  DontCare
  io.core_snoop               <>  DontCare
  io.ptw_snoop                <>  DontCare

  io.fpu_req                 <>  DontCare // SAVVINA added because fiirtl considers it not initialised (not existing in the original metasys) 
  io.fpu_resp                <>  DontCare // SAVVINA added because fiirtl considers it not initialised (not existing in the original metasys)

  ctrl.io.pref_pat_addr       := DontCare
}

