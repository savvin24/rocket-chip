
package tile

import chisel3._
import freechips.rocketchip.util.ParameterizedBundle
import chisel3.util._
import freechips.rocketchip.tilelink.TLEdgeOut
import freechips.rocketchip.util._
import freechips.rocketchip.rocket._
import freechips.rocketchip.tile.{HasCoreParameters, CoreBundle, CoreModule}
import org.chipsalliance.cde.config.Parameters
/*
import AmuRequest._
import ClientAmuResponse._



trait HasClientAmuParameters extends HasCoreParameters with HasAmuParameters {
  val amuAddrBits = coreMaxAddrBits
  val amuSegmentSizeBits = coreMaxAddrBits
  val amuSegmentBits = 24
  val amuClientIdBits = 2
}

abstract class ClientAmuBundle(implicit val p: Parameters)
  extends ParameterizedBundle()(p) with HasClientAmuParameters

abstract class ClientAmuModule(implicit val p: Parameters)
  extends Module with HasClientAmuParameters



class ClientAmuRequest(implicit p: Parameters)
  extends ClientAmuBundle()(p) {
  val client_id = UInt(amuClientIdBits.W)
  val cmd = UInt(AMU_CMD_SZ.W)
  val dst_start = UInt(amuAddrBits.W)
  val dst_stride = UInt(amuSegmentSizeBits.W)
  val segment_size = UInt(amuSegmentSizeBits.W)
  val nsegments = UInt(amuSegmentBits.W)
  val alloc = UInt(2.W)

  def isPrefetch(dummy: Int = 0): Bool =
    cmd === AmuRequest.AMU_CMD_PFR || cmd === AmuRequest.AMU_CMD_PFW
}


object ClientAmuRequest {
  val DMA_CMD_RESUME = "b01".U

  def apply(client_id: UInt,
            cmd: UInt,
            dst_start: UInt,
            segment_size: UInt,
            nsegments: UInt = 1.U,
            dst_stride: UInt = 0.U,
            alloc: UInt = "b10".U)
           (implicit p: Parameters) = {
    val req = Wire(new ClientAmuRequest)
    req.client_id := client_id
    req.cmd := cmd
    req.dst_start := dst_start
    req.dst_stride := dst_stride
    req.segment_size := segment_size
    req.nsegments := nsegments
    req.alloc := alloc
    req
  }

}

object ClientAmuResponse {
  val NO_ERROR = "b000".U
  val PAUSED = "b001".U
  val DST_PAGE_FAULT = "b011".U
  val DST_INVALID_REGION = "b101".U
}

class ClientAmuResponse(implicit p:Parameters)
  extends ClientAmuBundle()(p) with HasCoreParameters
{
  val client_id = UInt(amuClientIdBits.W)
  val status = UInt(amuStatusBits.W)
  val fault_vpn = UInt(vpnBits.W)
}


class ClientAmuIO(implicit p: Parameters) extends ParameterizedBundle()(p) {
    val req = Decoupled(new ClientAmuRequest)
    val resp = Flipped(Valid(new ClientAmuResponse))
}


class ClientAmuArbiter(n: Int)(implicit p: Parameters) extends Module {
  val io = IO(new Bundle {
    val in = Flipped(Vec(n, new ClientAmuIO))
    val out = new ClientAmuIO
  })

  val req_arb = Module(new RRArbiter(new ClientAmuRequest, n))
  req_arb.io.in <> io.in.map(_.req)
  io.out.req <> req_arb.io.out

  io.in.zipWithIndex.foreach { case (in, i) =>
    val me = io.out.resp.bits.client_id === i.U
    in.resp.valid := me && io.out.resp.valid
    in.resp.bits := io.out.resp.bits
  }
}
*/
class DecoupledTLB(TLBsets: Int, TLBways: Int)(implicit p: Parameters, edge : TLEdgeOut)
  extends CoreModule
{
  val lgMaxSize = log2Ceil(coreDataBytes)
   val io = IO(new Bundle {
     val req = Flipped(Decoupled(new TLBReq(lgMaxSize)))
     val resp = Decoupled(new TLBResp)
     val ptw = new TLBPTWIO
     val sfence = Flipped(Valid(new SFenceReq)) // SAVVINA added, did't exist in original metasys
     val kill = Input(Bool())  // SAVVINA added, did't exist in original metasys
   })

  val req = Reg(new TLBReq(lgMaxSize))
  val resp = Reg(new TLBResp)
  val tlb = Module(new TLB(false, lgMaxSize, new TLBConfig(nSets = TLBsets, nWays = TLBways)))

  tlb.io.sfence <> io.sfence // SAVVINA
  tlb.io.kill <> io.kill // SAVVINA 

  val s_idle :: s_tlb_req :: s_tlb_resp :: s_done :: Nil = Enum(4)
  val state = RegInit(init = s_idle)

  //io.req.bits.isLookup := false.B

  when (io.req.fire()) {
    req := io.req.bits
    state := s_tlb_req
  }

  when (tlb.io.req.fire()) {
    state := s_tlb_resp
  }

  when (state === s_tlb_resp) {
    when (tlb.io.resp.miss) {
      state := s_tlb_req
    } .otherwise {
      resp := tlb.io.resp
      state := s_done
    }
  }

  when (io.resp.fire()) { state := s_idle }

  io.req.ready := state === s_idle

  tlb.io.req.valid := state === s_tlb_req
  tlb.io.req.bits := req
  //tlb.io.req.bits.isLookup := false.B

  io.resp.valid := state === s_done
  io.resp.bits := resp

  io.ptw <> tlb.io.ptw
}


class FrontendTLBIO (implicit p:Parameters)
  extends CoreBundle
{
  val lgMaxSize = log2Ceil(coreDataBytes)
  val req = Decoupled(new TLBReq(lgMaxSize))
  val resp = Flipped(Decoupled(new TLBResp))
  val sfence = Valid(new SFenceReq) // SAVVINA added, did't exist in original metasys
  val kill = Output(Bool()) // SAVVINA added, did't exist in original metasys

  //req.bits.isLookup := false.B //SAVVINA addition, did't exist in original metasys

  // req.bits.isLookup := false.B
  // req.bits.sfence.valid := false.B
  // req.bits.sfence.bits.rs1 := true.B
  // req.bits.sfence.bits.rs2 := false.B
  // req.bits.sfence.bits.addr := RegInit(33.U(32.W))
  // req.bits.sfence.bits.asid := 0.U
}


class FrontendTLB(nClients: Int) (implicit p:Parameters, edge: TLEdgeOut)
  extends CoreModule
{
  val io = IO(new Bundle {
    val clients = Flipped(Vec(nClients, new FrontendTLBIO))
    val ptw = new TLBPTWIO
  })

  val lgMaxSize = log2Ceil(coreDataBytes)
  val tlbArb = Module(new InOrderArbiter (
    new TLBReq(lgMaxSize), new TLBResp, nClients
  ))

  val tlb = Module(new DecoupledTLB(1, 4))
  tlb.io.req <> tlbArb.io.out_req
  // tlb.io.req.bits.isLookup := false.B
  // tlb.io.req.bits.sfence.valid := false.B
  // tlb.io.req.bits.sfence.bits.rs1 := true.B
  // tlb.io.req.bits.sfence.bits.rs2 := false.B
  // tlb.io.req.bits.sfence.bits.addr := RegInit(33.U(32.W))
  // tlb.io.req.bits.sfence.bits.asid := 0.U
  //tlb.io.req.bits.isLookup <> DontCare //SAVVINA addition, did't exist in original metasys
  tlbArb.io.out_resp <> tlb.io.resp
  io.ptw <> tlb.io.ptw

  tlbArb.io.in_req <> io.clients.map(_.req)

  // tlbArb.io.in_req <> io.clients.map { client =>
  // val req = Wire(chiselTypeOf(client.req)) // Create a new Wire to hold modified request
  // req <> client.req // Connect all fields
  // req.bits.isLookup := false.B // Ensure isLookup is initialized
  // req
  // }

  io.clients.zip(tlbArb.io.in_resp).foreach {
    case (client, arb_resp) => client.resp <> arb_resp
  }

    // SAVVINA added (didn't exist on original MetaSys)----------------------------------------------------------------
  // SFENCE aggregation (OR + priority‐mux)
  // collect all the client sfence valids and bits
  val sfenceValids = io.clients.map(_.sfence.valid)
  val sfenceBits   = io.clients.map(_.sfence.bits)
  // any client can issue an sfence
  tlb.io.sfence.valid := sfenceValids.reduce(_ || _)
  // pick one of the pending sfence.bits (priority to lower‐index client)
  tlb.io.sfence.bits  := PriorityMux(sfenceValids, sfenceBits)

  // ----------------------------------------------------------------
  // kill aggregation (just OR them together)
  // if any client wants to squash the refill one cycle after a miss
  val killSignals = io.clients.map(_.kill)
  tlb.io.kill := killSignals.reduce(_ || _)
}

// class FrontendTLB(nClients: Int)(implicit p: Parameters, edge: TLEdgeOut)
//   extends CoreModule
// {
//   val io = IO(new Bundle {
//     val clients = Flipped(Vec(nClients, new FrontendTLBIO))
//     val ptw = new TLBPTWIO
//   })

//   val lgMaxSize = log2Ceil(coreDataBytes)
//   val tlbArb = Module(new InOrderArbiter(
//     new TLBReq(lgMaxSize), new TLBResp, nClients
//   ))

//   val tlb = Module(new DecoupledTLB(1, 4))

//   // Connect tlbArb to tlb
//   tlb.io.req <> tlbArb.io.out_req
//   tlbArb.io.out_resp <> tlb.io.resp
//   io.ptw <> tlb.io.ptw

//   // Connect clients to tlbArb
//   for (i <- 0 until nClients) {
//     val clientReq = Wire(new TLBReq(lgMaxSize))
//     clientReq := io.clients(i).req.bits
//     clientReq.isLookup := false.B // Explicitly initialize isLookup
//     tlbArb.io.in_req(i).bits := clientReq
//     tlbArb.io.in_req(i).valid := io.clients(i).req.valid
//     io.clients(i).req.ready := tlbArb.io.in_req(i).ready
//   }

//   // Connect tlbArb responses to clients
//   for (i <- 0 until nClients) {
//     io.clients(i).resp <> tlbArb.io.in_resp(i)
//   }
// }





// class AmuFrontend(implicit p: Parameters) extends CoreModule()(p)
//  // with HasClientAmuParameters
// {
//   val io = IO(new Bundle {
// //    val cpu = Flipped(new ClientAmuIO)
// //    val amu = new AmuIO
//     val tlb = new FrontendTLBIO
//     val busy = Output(Bool())
// //    val pause = Input(Bool())
//   })



//   val (s_idle :: s_translate :: s_amu_req :: s_amu_update ::
//         s_prepare :: s_finish :: Nil) = Enum(6)
//   val state = RegInit(init = s_idle)

//   //----------------------------------------------------------------------------------------------//
//   val to_translate = RegInit(false.B)
//   val tlb_sent = RegInit(true.B)
//   val tlb_to_send = to_translate & !tlb_sent
//   val ptw_error = false.B
//   val str_vaddr = RegInit(33.U(32.W))
//   io.tlb.req.valid := tlb_to_send
//   io.tlb.req.bits.vaddr := str_vaddr
//   io.tlb.req.bits.passthrough := false.B
//   io.tlb.req.bits.sfence.valid := ptw_error
//   io.tlb.req.bits.sfence.bits.rs1 := true.B
//   io.tlb.req.bits.sfence.bits.rs2 := false.B
//   io.tlb.req.bits.sfence.bits.addr := str_vaddr
//   io.tlb.req.bits.sfence.bits.asid := 0.U
//   io.tlb.resp.ready := tlb_sent
//   io.tlb.req.bits.cmd := M_XRD
//   io.tlb.req.bits.size := MT_W
//   //----------------------------------------------------------------------------------------------//

// } 