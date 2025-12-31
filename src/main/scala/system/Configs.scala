// See LICENSE.SiFive for license details.
// See LICENSE.Berkeley for license details.

package freechips.rocketchip.system

import org.chipsalliance.cde.config.Config
import freechips.rocketchip.subsystem._

class WithJtagDTMSystem extends freechips.rocketchip.subsystem.WithJtagDTM
class WithDebugSBASystem extends freechips.rocketchip.subsystem.WithDebugSBA
class WithDebugAPB extends freechips.rocketchip.subsystem.WithDebugAPB

class BaseConfig extends Config(
  new WithDefaultMemPort ++
  new WithDefaultMMIOPort ++
  new WithDefaultSlavePort ++
  new WithTimebase(BigInt(1000000)) ++ // 1 MHz
  new WithDTS("freechips,rocketchip-unknown", Nil) ++
  new WithNExtTopInterrupts(2) ++
  new BaseSubsystemConfig
)

class DefaultConfig extends Config(new WithNBigCores(1) ++ new WithCoherentBusTopology ++ new BaseConfig)

class DefaultBufferlessConfig extends Config(new WithBufferlessBroadcastHub ++ new DefaultConfig)
class DefaultSmallConfig extends Config(new WithNSmallCores(1) ++ new WithCoherentBusTopology ++ new BaseConfig)
class DefaultRV32Config extends Config(new WithRV32 ++ new DefaultConfig)
class DefaultFP16Config extends Config(new WithFP16 ++ new DefaultConfig)

class HypervisorConfig extends Config(new WithHypervisor ++ new DefaultConfig)

class DualBankConfig extends Config(new WithNBanks(2) ++ new DefaultConfig)
class DualCoreConfig extends Config(new WithNBigCores(2) ++ new WithCoherentBusTopology ++ new BaseConfig)
class DualChannelConfig extends Config(new WithNMemoryChannels(2) ++ new DefaultConfig)
class EightChannelConfig extends Config(new WithNMemoryChannels(8) ++ new DefaultConfig)

class ClusterConfig extends Config(
  new WithNBigCores(2, InCluster(3)) ++
  new WithNBigCores(2, InCluster(1)) ++
  new WithNBigCores(2, InCluster(0)) ++
  new WithCluster(3, location=InCluster(2)) ++
  new WithCluster(2) ++
  new WithCluster(1) ++
  new WithCluster(0) ++
  new DefaultConfig
)

class DualChannelDualBankConfig extends Config(
  new WithNMemoryChannels(2) ++
  new WithNBanks(4) ++ new DefaultConfig
)

// class RoccExampleConfig extends Config(new WithRoccExample ++ new DefaultConfig)

class HeterogeneousTileExampleConfig extends Config(
  new WithNBigCores(n = 1) ++
  new WithNMedCores(n = 1) ++
  new WithNSmallCores(n = 1) ++
  new WithCoherentBusTopology ++
  new BaseConfig
)

/* SAVVINA added from MetaSys*/
class AreaBaseline extends Config(new WithSimNMediumCores(1) ++ new BaseConfig)
// class AreaMetaSys extends Config(new BuffedALUConfig(128,9,false) ++ new AreaBaseline)

class DefaultMediumConfig extends Config(new WithSimNMediumCores(1) ++ new BaseConfig)
class ModifiedMediumConfig extends Config(new WithSimNMediumCores(4) ++ new BaseConfig)
// class RoccMediumExampleConfig extends Config(new ALUConfig(32,true,false,20) ++ new WithoutFPU ++ new DefaultMediumConfig)
// class RoccAtomPrefetcherConfig extends Config(new PrefetcherConfig(32,20) ++ new WithoutFPU ++ new DefaultMediumConfig)
// class RoccStridePrefetcherConfig extends Config(new StridePrefetcherConfig(32,20) ++ new WithoutFPU ++ new DefaultMediumConfig)
// class RoccIndirectPrefetcherConfig extends Config(new IndirectPrefetcherConfig(32,20)++ new WithoutFPU ++ new DefaultMediumConfig)
// class RoccIndirectPrefetcherBFSConfig extends Config(new IndirectPrefetcherBFSConfig(32,20)++ new WithoutFPU ++ new DefaultMediumConfig)
// class RoccIndirectPrefetcherStreamOnlyConfig extends Config(new IndirectPrefetcherStreamOnlyConfig(32,20)++ new WithoutFPU ++ new DefaultMediumConfig)
// class RoccAtomControllerConfig extends Config(new AtomControllerConfig(32, 20,false) ++ new WithoutFPU ++ new DefaultMediumConfig)
// class RoccMediumBuffedExampleConfig extends Config(new BuffedALUConfig(32,20,false) ++ new WithoutFPU ++ new DefaultMediumConfig)
// class TestRoccMediumBuffedPhysicalExampleConfig extends Config(new BuffedALUConfig(32,9,true) ++ new WithoutFPU ++ new DefaultMediumConfig)
// class RoccDualConfig extends Config(new BuffedPartitionedALUDualConfig(16,16,20,false) ++ new WithoutFPU ++ new DefaultMediumConfig)
// class RoccMediumBuffedPhysicalExampleConfig extends Config(new BuffedALUConfig(32,20,true) ++ new WithoutFPU ++ new DefaultMediumConfig)
// class RoccMediumBuffedDualExampleConfig extends Config(new BuffedALUDualConfig(32,20,false) ++ new WithoutFPU ++ new DefaultMediumConfig)
// class RoccMoreCoresConfig extends Config(new ALUConfig(32,true,false,9) ++ new WithoutFPU ++ new ModifiedMediumConfig)

// class RoccSmallExampleConfig extends Config(new ALUConfig(32,true,false,9) ++ new WithoutFPU ++ new DefaultSmallConfig)

class Edge128BitConfig extends Config(
  new WithEdgeDataBits(128) ++ new DefaultConfig
)
class Edge32BitConfig extends Config(
  new WithEdgeDataBits(32) ++ new DefaultConfig
)

class SingleChannelBenchmarkConfig extends Config(new DefaultConfig)
class DualChannelBenchmarkConfig extends Config(new WithNMemoryChannels(2) ++ new SingleChannelBenchmarkConfig)
class QuadChannelBenchmarkConfig extends Config(new WithNMemoryChannels(4) ++ new SingleChannelBenchmarkConfig)
class OctoChannelBenchmarkConfig extends Config(new WithNMemoryChannels(8) ++ new SingleChannelBenchmarkConfig)

class TinyConfig extends Config(
  new WithNoMemPort ++
  new WithNMemoryChannels(0) ++
  new WithNBanks(0) ++
  new With1TinyCore ++
  new WithIncoherentBusTopology ++
  new BaseConfig
)

class MemPortOnlyConfig extends Config(
  new WithNoMMIOPort ++
  new WithNoSlavePort ++
  new DefaultConfig
)

class MMIOPortOnlyConfig extends Config(
  new WithNoSlavePort ++
  new WithNoMemPort ++
  new WithNMemoryChannels(0) ++
  new WithNBanks(0) ++
  new WithIncoherentTiles ++
  new WithScratchpadsOnly ++
  new WithIncoherentBusTopology ++
  new DefaultConfig
)

class BaseFPGAConfig extends Config(new BaseConfig ++ new WithCoherentBusTopology)
class DefaultFPGAConfig extends Config(new WithNSmallCores(1) ++ new BaseFPGAConfig)

class CloneTileConfig extends Config(new WithCloneRocketTiles(7) ++ new WithNBigCores(1) ++ new WithCoherentBusTopology ++ new BaseConfig)
