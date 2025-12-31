// See LICENSE.SiFive for license details.
// See LICENSE.Berkeley for license details.

package freechips.rocketchip.tile

import chisel3._
import org.chipsalliance.cde.config._
import freechips.rocketchip.devices.tilelink._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.interrupts._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.rocket._
import freechips.rocketchip.subsystem.HierarchicalElementCrossingParamsLike
import freechips.rocketchip.util._
import freechips.rocketchip.prci.{ClockSinkParameters}

case class RocketTileBoundaryBufferParams(force: Boolean = false)

case class RocketTileParams(
    core: RocketCoreParams = RocketCoreParams(),
    icache: Option[ICacheParams] = Some(ICacheParams()),
    dcache: Option[DCacheParams] = Some(DCacheParams()),
    //rocc: Seq[RoCCParams] = Nil, //SAVVINA: Added for MetaSys, should be maybe removed in the future
    btb: Option[BTBParams] = Some(BTBParams()),
    dataScratchpadBytes: Int = 0,
    tileId: Int = 0,
    beuAddr: Option[BigInt] = None,
    //hartId: Int = 0, //SAVVINA: Added for MetaSys, should be maybe removed in the future
    blockerCtrlAddr: Option[BigInt] = None,
    clockSinkParams: ClockSinkParameters = ClockSinkParameters(),
    boundaryBuffers: Option[RocketTileBoundaryBufferParams] = None
    ) extends InstantiableTileParams[RocketTile] {
  require(icache.isDefined)
  require(dcache.isDefined)
  val baseName = "rockettile"
  val uniqueName = s"${baseName}_$tileId"
  def instantiate(crossing: HierarchicalElementCrossingParamsLike, lookup: LookupByHartIdImpl)(implicit p: Parameters): RocketTile = {
    new RocketTile(this, crossing, lookup)
  }
}

class RocketTile private(
      val rocketParams: RocketTileParams,
      crossing: ClockCrossingType,
      lookup: LookupByHartIdImpl,
      q: Parameters)
    extends BaseTile(rocketParams, crossing, lookup, q)
    with SinksExternalInterrupts
    with SourcesExternalNotifications
    with HasLazyRoCC  // implies CanHaveSharedFPU with CanHavePTW with HasHellaCache
    with HasHellaCache
    with HasICacheFrontend
{
  // Private constructor ensures altered LazyModule.p is used implicitly
  def this(params: RocketTileParams, crossing: HierarchicalElementCrossingParamsLike, lookup: LookupByHartIdImpl)(implicit p: Parameters) =
    this(params, crossing.crossingType, lookup, p)

// /*SAVVINA START*/
//   val firstRoCCOption = roccs.headOption

//   firstRoCCOption match {
//     case Some(firstRoCC) => 
//       val firstRoCC = roccs.head
//       firstRoCC.module.io.mem_pref <> DontCare
//       firstRoCC.module.io.mem_amulookup <> DontCare
//       firstRoCC.module.io.core_snoop <> DontCare
//       firstRoCC.module.io.ptw_snoop <> DontCare
//     case None =>
//       // Handle the case where there are no RoCCs in the sequence

//       printf("Savvina: Error: No RoCCs in the sequence\n")
//   }
// /*SAVVINA END*/

  val intOutwardNode = rocketParams.beuAddr map { _ => IntIdentityNode() }
  val slaveNode = TLIdentityNode()
  val masterNode = visibilityNode

  val dtim_adapter = tileParams.dcache.flatMap { d => d.scratch.map { s =>
    LazyModule(new ScratchpadSlavePort(AddressSet.misaligned(s, d.dataScratchpadBytes), lazyCoreParamsView.coreDataBytes, tileParams.core.useAtomics && !tileParams.core.useAtomicsOnlyForIO))
  }}
  dtim_adapter.foreach(lm => connectTLSlave(lm.node, lm.node.portParams.head.beatBytes))

  val bus_error_unit = rocketParams.beuAddr map { a =>
    val beu = LazyModule(new BusErrorUnit(new L1BusErrors, BusErrorUnitParams(a)))
    intOutwardNode.get := beu.intNode
    connectTLSlave(beu.node, xBytes)
    beu
  }

  val tile_master_blocker =
    tileParams.blockerCtrlAddr
      .map(BasicBusBlockerParams(_, xBytes, masterPortBeatBytes, deadlock = true))
      .map(bp => LazyModule(new BasicBusBlocker(bp)))

  tile_master_blocker.foreach(lm => connectTLSlave(lm.controlNode, xBytes))

  // TODO: this doesn't block other masters, e.g. RoCCs
  tlOtherMastersNode := tile_master_blocker.map { _.node := tlMasterXbar.node } getOrElse { tlMasterXbar.node }
  masterNode :=* tlOtherMastersNode
  DisableMonitors { implicit p => tlSlaveXbar.node :*= slaveNode }

  nDCachePorts += 1 /*core */ + (dtim_adapter.isDefined).toInt

  val dtimProperty = dtim_adapter.map(d => Map(
    "sifive,dtim" -> d.device.asProperty)).getOrElse(Nil)

  val itimProperty = frontend.icache.itimProperty.toSeq.flatMap(p => Map("sifive,itim" -> p))

  val beuProperty = bus_error_unit.map(d => Map(
          "sifive,buserror" -> d.device.asProperty)).getOrElse(Nil)

  val cpuDevice: SimpleDevice = new SimpleDevice("cpu", Seq("sifive,rocket0", "riscv")) {
    override def parent = Some(ResourceAnchors.cpus)
    override def describe(resources: ResourceBindings): Description = {
      val Description(name, mapping) = super.describe(resources)
      Description(name, mapping ++ cpuProperties ++ nextLevelCacheProperty
                  ++ tileProperties ++ dtimProperty ++ itimProperty ++ beuProperty)
    }
  }

  ResourceBinding {
    Resource(cpuDevice, "reg").bind(ResourceAddress(tileId))
  }

  override lazy val module = new RocketTileModuleImp(this)

  override def makeMasterBoundaryBuffers(crossing: ClockCrossingType)(implicit p: Parameters) = (rocketParams.boundaryBuffers, crossing) match {
    case (Some(RocketTileBoundaryBufferParams(true )), _)                   => TLBuffer()
    case (Some(RocketTileBoundaryBufferParams(false)), _: RationalCrossing) => TLBuffer(BufferParams.none, BufferParams.flow, BufferParams.none, BufferParams.flow, BufferParams(1))
    case _ => TLBuffer(BufferParams.none)
  }

  override def makeSlaveBoundaryBuffers(crossing: ClockCrossingType)(implicit p: Parameters) = (rocketParams.boundaryBuffers, crossing) match {
    case (Some(RocketTileBoundaryBufferParams(true )), _)                   => TLBuffer()
    case (Some(RocketTileBoundaryBufferParams(false)), _: RationalCrossing) => TLBuffer(BufferParams.flow, BufferParams.none, BufferParams.none, BufferParams.none, BufferParams.none)
    case _ => TLBuffer(BufferParams.none)
  }
}

class RocketTileModuleImp(outer: RocketTile) extends BaseTileModuleImp(outer)
    with HasFpuOpt
    with HasLazyRoCCModule
    with HasICacheFrontendModule {
  Annotated.params(this, outer.rocketParams)

  val core = Module(new Rocket(outer)(outer.p))

  // reset vector is connected in the Frontend to s2_pc
  core.io.reset_vector := DontCare

  // Report unrecoverable error conditions; for now the only cause is cache ECC errors
  outer.reportHalt(List(outer.dcache.module.io.errors))

  // Report when the tile has ceased to retire instructions; for now the only cause is clock gating
  outer.reportCease(outer.rocketParams.core.clockGate.option(
    !outer.dcache.module.io.cpu.clock_enabled &&
    !outer.frontend.module.io.cpu.clock_enabled &&
    !ptw.io.dpath.clock_enabled &&
    core.io.cease))

  outer.reportWFI(Some(core.io.wfi))

  outer.decodeCoreInterrupts(core.io.interrupts) // Decode the interrupt vector

  outer.bus_error_unit.foreach { beu =>
    core.io.interrupts.buserror.get := beu.module.io.interrupt
    beu.module.io.errors.dcache := outer.dcache.module.io.errors
    beu.module.io.errors.icache := outer.frontend.module.io.errors
  }

  core.io.interrupts.nmi.foreach { nmi => nmi := outer.nmiSinkNode.get.bundle }

  // Pass through various external constants and reports that were bundle-bridged into the tile
  outer.traceSourceNode.bundle <> core.io.trace
  core.io.traceStall := outer.traceAuxSinkNode.bundle.stall
  outer.bpwatchSourceNode.bundle <> core.io.bpwatch
  core.io.hartid := outer.hartIdSinkNode.bundle
  require(core.io.hartid.getWidth >= outer.hartIdSinkNode.bundle.getWidth,
    s"core hartid wire (${core.io.hartid.getWidth}b) truncates external hartid wire (${outer.hartIdSinkNode.bundle.getWidth}b)")

  // Connect the core pipeline to other intra-tile modules
  outer.frontend.module.io.cpu <> core.io.imem

  println("Before adding core<->dmem" + dcachePorts.size) // SAVVINA added from MetaSys

  dcachePorts += core.io.dmem // TODO outer.dcachePorts += () => module.core.io.dmem ??

 /* SAVVINA added from MetaSys */

  // Toying with ports to prioritize core over prefetcher
  // require(dcachePorts.size == 5)
  // val prefetcherPort = dcachePorts(2)
  // dcachePorts(2) = dcachePorts(4)
  // dcachePorts(4) = prefetcherPort

/*
  // SWAP LOOKUP AND CORE PORT ORDERING
  val lookupPort = dcachePorts(3)
  dcachePorts(3) = dcachePorts(2)
  dcachePorts(2) = lookupPort
*/

  fpuOpt foreach { fpu =>
    core.io.fpu :<>= fpu.io.waiveAs[FPUCoreIO](_.cp_req, _.cp_resp)
    fpu.io.cp_req := DontCare
    fpu.io.cp_resp := DontCare
  }
  if (fpuOpt.isEmpty) {
    core.io.fpu := DontCare
  }
  core.io.ptw <> ptw.io.dpath

  // Connect the coprocessor interfaces
  if (outer.roccs.size > 0) {
    cmdRouter.get.io.in <> core.io.rocc.cmd 
    // core.io.rocc.cmd carries each RoCC command packet that the core destines for the RoCCs
    // cmdRouter fans out each of these RoCC command packets to the appropriate RoCC, so that 1 accelerator “wins” the command channel each cycle (must verify this by checking the code)

    outer.roccs.foreach{ lm =>
      lm.module.io.exception := core.io.rocc.exception
      lm.module.io.fpu_req.ready := DontCare
      lm.module.io.fpu_resp.valid := DontCare
      lm.module.io.fpu_resp.bits.data := DontCare
      lm.module.io.fpu_resp.bits.exc := DontCare

      /* SAVVINA added because "not fully initialised error" (didn't exist in MetaSys) */

      lm.module.io.core_snoop.valid      := core.io.rocc.core_snoop.valid
      lm.module.io.core_snoop.addr       := core.io.rocc.core_snoop.addr
      lm.module.io.core_snoop.cmd        := core.io.rocc.core_snoop.cmd
      lm.module.io.core_snoop.size       := core.io.rocc.core_snoop.size
      lm.module.io.core_snoop.s1_kill    := core.io.rocc.core_snoop.s1_kill
      lm.module.io.core_snoop.s2_paddr   := core.io.rocc.core_snoop.s2_paddr
      lm.module.io.core_snoop.s2_xcpt    := core.io.rocc.core_snoop.s2_xcpt
      lm.module.io.core_snoop.s2_nack    := core.io.rocc.core_snoop.s2_nack
      lm.module.io.core_snoop.miss       := core.io.rocc.core_snoop.miss
      lm.module.io.core_snoop.physical   := core.io.rocc.core_snoop.physical

      lm.module.io.core_snoop.bp_q_full := DontCare
      lm.module.io.core_snoop.bc_resolved := DontCare
      lm.module.io.core_snoop.dbg1 := DontCare
      lm.module.io.core_snoop.dbg2 := DontCare
      lm.module.io.core_snoop.dbg3 := DontCare
      lm.module.io.core_snoop.dbg4 := DontCare

      /*SAVVINA START*/

      // lm.module.io.mem_amulookup <> core.io.rocc.mem_amulookup
      // lm.module.io.core_snoop <> core.io.rocc.core_snoop
      //lm.module.io.ptw_snoop <> core.io.rocc.ptw_snoop
      //ptw.io.snoop //In old metasys: LazyRoCC.scala:169
      /*SAVVINA END*/
    }
    core.io.rocc.resp <> respArb.get.io.out  // respArb.get.io.out: Accelerators generate RoCC responses (e.g. result tokens) via respArb, which merges the outputs of all accelerators onto a single response channel
    // core.io.rocc.resp is the port that the core uses to receive RoCC responses
    core.io.rocc.busy <> (cmdRouter.get.io.busy || outer.roccs.map(_.module.io.busy).reduce(_ || _)) // either the cmdRouter is busy or at least one of the RoCCs is busy
    core.io.rocc.interrupt := outer.roccs.map(_.module.io.interrupt).reduce(_ || _)
    (core.io.rocc.csrs zip roccCSRIOs.flatten).foreach { t => t._2 <> t._1 }

    // core.io.rocc.core_snoop.bp_q_full := DontCare // SAVVINA added because "not fully initialised error" (didn't exist in MetaSys)
    // core.io.rocc.core_snoop.bc_resolved := DontCare // SAVVINA added because "not fully initialised error" (didn't exist in MetaSys)
    // core.io.rocc.core_snoop.dbg1 := DontCare // SAVVINA added because "not fully initialised error" (didn't exist in MetaSys)
    // core.io.rocc.core_snoop.dbg2 := DontCare // SAVVINA added because "not fully initialised error" (didn't exist in MetaSys)
    // core.io.rocc.core_snoop.dbg3 := DontCare // SAVVINA added because "not fully initialised error" (didn't exist in MetaSys)
    // core.io.rocc.core_snoop.dbg4 := DontCare // SAVVINA added because "not fully initialised error" (didn't exist in MetaSys)
    
    // core.io.rocc.ptw_snoop.bp_q_full := DontCare // SAVVINA added because "not fully initialised error" (didn't exist in MetaSys)
    // core.io.rocc.ptw_snoop.bc_resolved := DontCare // SAVVINA added because "not fully initialised error" (didn't exist in MetaSys)
    // core.io.rocc.ptw_snoop.dbg1 := DontCare // SAVVINA added because "not fully initialised error" (didn't exist in MetaSys)
    // core.io.rocc.ptw_snoop.dbg2 := DontCare // SAVVINA added because "not fully initialised error" (didn't exist in MetaSys) 
    // core.io.rocc.ptw_snoop.dbg3 := DontCare // SAVVINA added because "not fully initialised error" (didn't exist in MetaSys)
    // core.io.rocc.ptw_snoop.dbg4 := DontCare // SAVVINA added because "not fully initialised error" (didn't exist in MetaSys)

    

  } else {
    // tie off
    core.io.rocc.cmd.ready := false.B
    core.io.rocc.resp.valid := false.B
    core.io.rocc.resp.bits := DontCare
    core.io.rocc.busy := DontCare
    core.io.rocc.interrupt := DontCare
  }
  // Dont care mem since not all RoCC need accessing memory
  core.io.rocc.mem := DontCare
  core.io.rocc.mem_pref := DontCare // SAVVINA added because "not fully initialised error" (didn't exist in MetaSys) 
  core.io.rocc.mem_amulookup := DontCare // SAVVINA added because "not fully initialised error" (didn't exist in MetaSys) 
  // core.io.rocc.mem_pref := DontCare
  // core.io.rocc.mem_amulookup := DontCare
  // core.io.rocc.core_snoop := DontCare
  //core.io.rocc.ptw_snoop := ptw.io.snoop

    core.io.rocc.core_snoop.bp_q_full := DontCare // SAVVINA added because "not fully initialised error" (didn't exist in MetaSys)
    core.io.rocc.core_snoop.bc_resolved := DontCare // SAVVINA added because "not fully initialised error" (didn't exist in MetaSys)
    core.io.rocc.core_snoop.dbg1 := DontCare // SAVVINA added because "not fully initialised error" (didn't exist in MetaSys)
    core.io.rocc.core_snoop.dbg2 := DontCare // SAVVINA added because "not fully initialised error" (didn't exist in MetaSys)
    core.io.rocc.core_snoop.dbg3 := DontCare // SAVVINA added because "not fully initialised error" (didn't exist in MetaSys)
    core.io.rocc.core_snoop.dbg4 := DontCare // SAVVINA added because "not fully initialised error" (didn't exist in MetaSys)
    
    core.io.rocc.ptw_snoop.bp_q_full := DontCare // SAVVINA added because "not fully initialised error" (didn't exist in MetaSys)
    core.io.rocc.ptw_snoop.bc_resolved := DontCare // SAVVINA added because "not fully initialised error" (didn't exist in MetaSys)
    core.io.rocc.ptw_snoop.dbg1 := DontCare // SAVVINA added because "not fully initialised error" (didn't exist in MetaSys)
    core.io.rocc.ptw_snoop.dbg2 := DontCare // SAVVINA added because "not fully initialised error" (didn't exist in MetaSys) 
    core.io.rocc.ptw_snoop.dbg3 := DontCare // SAVVINA added because "not fully initialised error" (didn't exist in MetaSys)
    core.io.rocc.ptw_snoop.dbg4 := DontCare // SAVVINA added because "not fully initialised error" (didn't exist in MetaSys)

  // Rocket has higher priority to DTIM than other TileLink clients
  outer.dtim_adapter.foreach { lm => dcachePorts += lm.module.io.dmem }

  // TODO eliminate this redundancy
  val h = dcachePorts.size
  val c = core.dcacheArbPorts
  val o = outer.nDCachePorts
  require(h == c, s"port list size was $h, core expected $c")
  require(h == o, s"port list size was $h, outer counted $o")
  // TODO figure out how to move the below into their respective mix-ins
  dcacheArb.io.requestor <> dcachePorts.toSeq
  ptw.io.requestor <> ptwPorts.toSeq
}

trait HasFpuOpt { this: RocketTileModuleImp =>
  val fpuOpt = outer.tileParams.core.fpu.map(params => Module(new FPU(params)(outer.p)))
}
