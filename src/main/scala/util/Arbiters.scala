// See LICENSE.Berkeley for license details.

package freechips.rocketchip.util

import chisel3._
import chisel3.util._
import org.chipsalliance.cde.config.Parameters

/** A generalized locking RR arbiter that addresses the limitations of the
 *  version in the Chisel standard library */
abstract class HellaLockingArbiter[T <: Data](typ: T, arbN: Int, rr: Boolean = false)
    extends Module {

  val io = IO(new Bundle {
    val in = Flipped(Vec(arbN, Decoupled(typ.cloneType)))
    val out = Decoupled(typ.cloneType)
  })

  def rotateLeft[T <: Data](norm: Vec[T], rot: UInt): Vec[T] = {
    val n = norm.size
    VecInit.tabulate(n) { i =>
      Mux(rot < (n - i).U, norm(i.U + rot), norm(rot - (n - i).U))
    }
  }

  val lockIdx = RegInit(0.U(log2Up(arbN).W))
  val locked = RegInit(false.B)

  val choice = if (rr) {
    PriorityMux(
      rotateLeft(VecInit(io.in.map(_.valid)), lockIdx + 1.U),
      rotateLeft(VecInit((0 until arbN).map(_.U)), lockIdx + 1.U))
  } else {
    PriorityEncoder(io.in.map(_.valid))
  }

  val chosen = Mux(locked, lockIdx, choice)

  for (i <- 0 until arbN) {
    io.in(i).ready := io.out.ready && chosen === i.U
  }

  io.out.valid := io.in(chosen).valid
  io.out.bits := io.in(chosen).bits
}

/** This locking arbiter determines when it is safe to unlock
 *  by peeking at the data */
class HellaPeekingArbiter[T <: Data](
      typ: T, arbN: Int,
      canUnlock: T => Bool,
      needsLock: Option[T => Bool] = None,
      rr: Boolean = false)
    extends HellaLockingArbiter(typ, arbN, rr) {

  def realNeedsLock(data: T): Bool =
    needsLock.map(_(data)).getOrElse(true.B)

  when (io.out.fire) {
    when (!locked && realNeedsLock(io.out.bits)) {
      lockIdx := choice
      locked := true.B
    }
    // the unlock statement takes precedent
    when (canUnlock(io.out.bits)) {
      locked := false.B
    }
  }
}

/** This arbiter determines when it is safe to unlock by counting transactions */
class HellaCountingArbiter[T <: Data](
      typ: T, arbN: Int, count: Int,
      val needsLock: Option[T => Bool] = None,
      rr: Boolean = false)
    extends HellaLockingArbiter(typ, arbN, rr) {

  def realNeedsLock(data: T): Bool =
    needsLock.map(_(data)).getOrElse(true.B)

  // if count is 1, you should use a non-locking arbiter
  require(count > 1, "CountingArbiter cannot have count <= 1")

  val lock_ctr = Counter(count)

  when (io.out.fire) {
    when (!locked && realNeedsLock(io.out.bits)) {
      lockIdx := choice
      locked := true.B
      lock_ctr.inc()
    }

    when (locked) {
      when (lock_ctr.inc()) { locked := false.B }
    }
  }
}

/** This arbiter preserves the order of responses */
class InOrderArbiter[T <: Data, U <: Data](reqTyp: T, respTyp: U, n: Int)
    (implicit p: Parameters) extends Module {
  val io = IO(new Bundle {
    val in_req = Flipped(Vec(n, Decoupled(reqTyp)))
    val in_resp = Vec(n, Decoupled(respTyp))
    val out_req = Decoupled(reqTyp)
    val out_resp = Flipped(Decoupled(respTyp))
  })

  if (n > 1) {

    /* SAVVINA COMMENT: 
    route_q: FIFO queue that saves the index of the (in this case two) recent requestors. 
    Used to retrieve the requestor index when a response arrives, in order to forward it to the correct requestor*/

    val route_q = Module(new Queue(UInt(log2Up(n).W), 2))

    /* SAVVINA COMMENT:
      req_arb: Round-robin arbiter that selects one of the requestors to forward its request to the out_req to be served.
    */

    val req_arb = Module(new RRArbiter(reqTyp, n))
    req_arb.io.in <> io.in_req

  /* SAVVINA COMMENT:
    req_helper: A chisel utility that ensures that a transaction is fired only when all its inputs are ready/valid.
  */

    val req_helper = DecoupledHelper(
      req_arb.io.out.valid,
      route_q.io.enq.ready,
      io.out_req.ready)

    io.out_req.bits := req_arb.io.out.bits
    //io.out_req.valid := req_helper.fire(io.out_req.ready)
    io.out_req.valid := req_helper.fire()

    route_q.io.enq.bits := req_arb.io.chosen
    //route_q.io.enq.valid := req_helper.fire(route_q.io.enq.ready)
    route_q.io.enq.valid := req_helper.fire()

    //req_arb.io.out.ready := req_helper.fire(req_arb.io.out.valid)
    req_arb.io.out.ready := req_helper.fire()

    val resp_sel = route_q.io.deq.bits
    val resp_ready = io.in_resp(resp_sel).ready
    val resp_helper = DecoupledHelper(
      resp_ready,
      route_q.io.deq.valid,
      io.out_resp.valid)

    /* SAVVINA COMMENT:
      send the response to all requestors, but only the one that matches the selected index will receive it.
    */

    //val resp_valid = resp_helper.fire(resp_ready)
    val resp_valid = resp_helper.fire()
    for (i <- 0 until n) {
      io.in_resp(i).bits := io.out_resp.bits
      io.in_resp(i).valid := resp_valid && resp_sel === i.U
    }

    //route_q.io.deq.ready := resp_helper.fire(route_q.io.deq.valid)
    //io.out_resp.ready := resp_helper.fire(io.out_resp.valid)
    route_q.io.deq.ready := resp_helper.fire()
    io.out_resp.ready := resp_helper.fire()
  } else {
    io.out_req <> io.in_req.head
    io.in_resp.head <> io.out_resp
  }
}


// // SAVVINA ARBITER FOR METASYS (based on InOrderArbiter))

// /**
//  * An arbiter for AMU lookup requests, which also routes responses back to the original requestor.
//  * This is based on the principles of the provided InOrderArbiter.
//  */
// class AMULookUpArbiter(n: Int)(implicit p: Parameters) extends Module {
//   val io = IO(new Bundle {
//     val in = Vec(n, new AMULookUpIO)
//     val out = Flipped(new AMULookUpIO)
//   })

// if (n > 1) {
//     // The queue to track which client's request is outstanding
//     val route_q = Module(new Queue(UInt(log2Up(n).W), 2))

//     // The request arbitration
//     val req_arb = Module(new RRArbiter(new AMULookUpReq, n))
//     for (i <- 0 until n) {
//       req_arb.io.in(i).valid := io.in(i).req.valid
//       req_arb.io.in(i).bits := io.in(i).req.paddr
//       io.in(i).req.ready := req_arb.io.in(i).ready
//     }

//     // Ready signal for the output response is controlled by the selected input's response ready
//     // This is a bit tricky with `AMULookUpIO` because the `ready` is an output.
//     // We can't directly connect it. Instead, the `valid` on the `in` interfaces are set by us.
//     // The `ready` on the `out` side drives the `valid` on the in side. Let's simplify this.
//     // The original `InOrderArbiter` has the same issue. Let's use `DecoupledHelper` to tie it together.

//     val req_helper = DecoupledHelper(
//       req_arb.io.out.valid,
//       route_q.io.enq.ready,
//       io.out.req.ready)

//     io.out.req.paddr := req_arb.io.out.bits.paddr
//     io.out.req.valid := req_helper.fire(io.out.req.ready)

//     route_q.io.enq.bits := req_arb.io.chosen
//     route_q.io.enq.valid := req_helper.fire(route_q.io.enq.ready)

//     req_arb.io.out.ready := req_helper.fire(req_arb.io.out.valid)
    
//     // The response routing
//     val resp_sel = route_q.io.deq.bits

//     // Now for the response
//     val resp_helper = DecoupledHelper(
//       route_q.io.deq.valid,
//       io.out.resp.valid)

//     // Route the response from the output back to the correct input client
//     for (i <- 0 until n) {
//       io.in(i).resp.valid := Mux(resp_sel === i.U, io.out.resp.valid, false.B)
//       io.in(i).resp.miss := io.out.resp.miss
//       io.in(i).resp.atom_id := io.out.resp.atom_id
//       io.in(i).resp.xcpt := io.out.resp.xcpt
//     }

//     route_q.io.deq.ready := resp_helper.fire(route_q.io.deq.valid)

//   } else {
//     // If there's only one client, we can directly connect the request and response
//     io.out.req <> io.in(0).req
//     io.in(0).resp <> io.out.resp
//   }
// }
