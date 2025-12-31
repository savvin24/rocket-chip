#ifndef HPC_HH
#define HPC_HH
#include <stdint.h>
#include <stdio.h>
#include <string>
/** Helper class used to measure performance
 */

#define INST_COMMIT_EVENTS 0x100
#define EXCEPTION_TAKEN INST_COMMIT_EVENTS
#define INTEGER_LOAD_INSTRUCTION_RETIRED INST_COMMIT_EVENTS<<1
#define INTEGER_STORE_INSTRUCTION_RETIRED INST_COMMIT_EVENTS<<2
#define ATOMIC_MEMORY_OPERATION_RETIRED INST_COMMIT_EVENTS<<3
#define SYSTEM_INSTRUCTION_RETIRED INST_COMMIT_EVENTS<<4
#define INTEGER_ARITHMETIC_INSTRUCTION_RETIRED INST_COMMIT_EVENTS<<5
#define CONDITIONAL_BRANCH_RETIRED INST_COMMIT_EVENTS<<6
#define JAL_INSTRUCTION_RETIRED INST_COMMIT_EVENTS<<7
#define JALR_INSTRUCTION_RETIRED INST_COMMIT_EVENTS<<8
#define INTEGER_MULTIPLICATION_INSTRUCTION_RETIRED INST_COMMIT_EVENTS<<9
#define INTEGER_DIVISION_INSTRUCTION_RETIRED INST_COMMIT_EVENTS<<10
#define FLOATING_POINT_LOAD_INSTRUCTION_RETIRED INST_COMMIT_EVENTS<<11
#define FLOATING_POINT_STORE_INSTRUCTION_RETIRED INST_COMMIT_EVENTS<<12
#define FLOATING_POINT_ADDITION_INSTRUCTION_RETIRED INST_COMMIT_EVENTS<<13
#define FLOATING_POINT_MULTIPLICATION_INSTRUCTION_RETIRED INST_COMMIT_EVENTS<<14
#define FLOATING_POINT_FUSED_MULTIPLY_ADD_RETIRED INST_COMMIT_EVENTS<<15
#define FLAOTING_POINT_DIVISION_SQRT_RETIRED INST_COMMIT_EVENTS<<16
#define OTHER_FLOATING_POINT_INSTRUCTION_RETIRED INST_COMMIT_EVENTS<<17

#define MICROARCHTECTURAL_EVENTS 0x1
#define LOAD_USE_INTERLOCK 0x1 | (0x100<<0)
#define LONG_LATENCY_INTERLOCK 0x1 | (0x100<<1)
#define CSR_READ_INTERLOCK 0x1 | (0x100<<2)
#define INSTRUCTION_CACHE_ITIM_BUSY 0x1 | (0x100<<3) // SAVVINA COMMENT "I$ blocked"
#define DATA_CACHE_DTIM_BUSY 0x1 | (0x100<<4)        // SAVVINA COMMENT "D$ blocked"
#define BRANCH_DIRECTION_MISPREDICTION 0x1 | (0x100<<5)
#define BRANCH_JUMP_TARGET_MISPREDICTION 0x1 | (0x100<<6) // SAVVINA COMMENT "control-flow target misprediction"
#define PIPELINE_FLUSH_FROM_CSR_WRITE 0x1 | (0x100<<7)
#define PIPELINE_FLUSH_FROM_OTHER_EVENT 0x1 | (0x100<<8)
#define INTEGER_MULTIPLICATION_INTERLOCK 0x1 | (0x100<<9) // SAVVINA COMMENT "replay"
#define FLOATING_POINT_INTERLOCK 0x1 | (0x100<<10)

#define MEMORY_SYSTEM_EVENTS 0x2
#define INSTRUCTION_CACHE_MISS 0x2 | (0x100<<0)
#define DATA_CACHE_MISS_OR_MEMORY_MAPPED_IO_ACCESS 0x2 | (0x100<<1)
#define DATA_CACHE_WRITEBACK 0x2 | (0x100<<2)  // SAVVINA comment: D$ release
#define INSTRUCTION_TLB_MISS 0x2 | (0x100<<3)
#define DATA_TLB_MISS 0x2 | (0x100<<4)
#define L2_TLB_MISS 0x2 | (0x100<<5)

class HPC
{
  public:
  HPC();
  ~HPC();

  void startMeasurement();
  void endMeasurement();
  void printStats();
  void printCSV();

  private:
  static uint8_t cacheThrasher[16*1024];

  static const int mcounteren    = 0x306;
  static const int scounteren    = 0x106;
  static const int mcountinhibit = 0x320;

  static const int mhpmevent3    = 0x323;
  static const int mhpmevent4    = 0x324;
  static const int mhpmevent5    = 0x325;
  static const int mhpmevent6    = 0x326;
  static const int mhpmevent7    = 0x327;
  static const int mhpmevent8    = 0x328;
  static const int mhpmevent9    = 0x329;

  static const int cycle         = 0xc00;
  static const int time          = 0xc01;
  static const int instret       = 0xc02;
  static const int hpmcounter3   = 0xc03;
  static const int hpmcounter4   = 0xc04;
  static const int hpmcounter5   = 0xc05;
  static const int hpmcounter6   = 0xc06;
  static const int hpmcounter7   = 0xc07;
  static const int hpmcounter8   = 0xc08;
  static const int hpmcounter9   = 0xc09;

  const int counterAddresses[10] = 
  {
    cycle, time , instret, hpmcounter3, hpmcounter4,
    hpmcounter5, hpmcounter6, hpmcounter7, hpmcounter8,
    hpmcounter9
  };

  std::string counterNames[10] =
  { "N of Cycles", 
    "Time Elapsed", 
    "Instructions Retired", 
    "Integer loads retired",
    "Data cache miss or memory mapped I/O access",
    //"Integer stores retired",
    "Data cache busy",
    "Data cache miss or memory mapped I/O access",
    "Data cache writeback",
    "Data TLB miss",
    "L2 TLB miss"
  };

  //uint64_t countersSnapshot[10];
  uint64_t countersStart[10];
  uint64_t countersEnd[10];

  // SAVVINA COMMENT: In principle, this is the immediate version of the csrr instruction
  static uint64_t read_csr(const int reg)
  { 
    uint64_t __tmp;
    switch(reg)
    {
      case cycle:
      asm volatile ("csrr %0, %1" : "=r"(__tmp) : "n"(cycle));
      break;
      case instret:
      asm volatile ("csrr %0, %1" : "=r"(__tmp) : "n"(instret));
      break;
      case hpmcounter3:
      asm volatile ("csrr %0, %1" : "=r"(__tmp) : "n"(hpmcounter3));
      break;
      case hpmcounter4:
      asm volatile ("csrr %0, %1" : "=r"(__tmp) : "n"(hpmcounter4));
      break;
      case hpmcounter5:
      asm volatile ("csrr %0, %1" : "=r"(__tmp) : "n"(hpmcounter5));
      break;
      case hpmcounter6:
      asm volatile ("csrr %0, %1" : "=r"(__tmp) : "n"(hpmcounter6));
      break;
      case hpmcounter7:
      asm volatile ("csrr %0, %1" : "=r"(__tmp) : "n"(hpmcounter7));
      break;
      case hpmcounter8:
      asm volatile ("csrr %0, %1" : "=r"(__tmp) : "n"(hpmcounter8));
      break;
      case hpmcounter9:
      asm volatile ("csrr %0, %1" : "=r"(__tmp) : "n"(hpmcounter9));
      break;
      // case hpmcounter10:
      // asm volatile ("csrr %0, %1" : "=r"(__tmp) : "n"(hpmcounter10));
      // break;
      default:
      printf("This should not happen!\n");
      exit(1);
    }
    return __tmp;
  }

  // SAVVINA COMMENT: __tmp value returned is a register. Termed "safe" and is the one used
  static uint64_t read_csr_safe(const int reg)
  {  
    register uint64_t __tmp asm("a0");
    switch(reg)
    {
      case cycle:
      asm volatile ("csrr %0, %1" : "=r"(__tmp) : "n"(cycle));
      break;
      case instret:
      asm volatile ("csrr %0, %1" : "=r"(__tmp) : "n"(instret));
      break;
      case hpmcounter3:
      asm volatile ("csrr %0, %1" : "=r"(__tmp) : "n"(hpmcounter3));
      break;
      case hpmcounter4:
      asm volatile ("csrr %0, %1" : "=r"(__tmp) : "n"(hpmcounter4));
      break;
      case hpmcounter5:
      asm volatile ("csrr %0, %1" : "=r"(__tmp) : "n"(hpmcounter5));
      break;
      case hpmcounter6:
      asm volatile ("csrr %0, %1" : "=r"(__tmp) : "n"(hpmcounter6));
      break;
      case hpmcounter7:
      asm volatile ("csrr %0, %1" : "=r"(__tmp) : "n"(hpmcounter7));
      break;
      case hpmcounter8:
      asm volatile ("csrr %0, %1" : "=r"(__tmp) : "n"(hpmcounter8));
      break;
      case hpmcounter9:
      asm volatile ("csrr %0, %1" : "=r"(__tmp) : "n"(hpmcounter9));
      break;
      // case hpmcounter10:
      // asm volatile ("csrr %0, %1" : "=r"(__tmp) : "n"(hpmcounter10));
      // break;
      default:
      printf("This should not happen!\n");
      exit(1);
    }
    return __tmp;
  }

  static void write_csr_safe(const int reg, const uint64_t val)
  {
    switch(reg)
    {
      case mhpmevent3:
      asm volatile ("csrw %0, %1" :: "n"(mhpmevent3), "r"(val));
      break;
      case mhpmevent4:
      asm volatile ("csrw %0, %1" :: "n"(mhpmevent4), "r"(val));
      break;
      case mhpmevent5:
      asm volatile ("csrw %0, %1" :: "n"(mhpmevent5), "r"(val));
      break;
      case mhpmevent6:
      asm volatile ("csrw %0, %1" :: "n"(mhpmevent6), "r"(val));
      break;
      case mhpmevent7:
      asm volatile ("csrw %0, %1" :: "n"(mhpmevent7), "r"(val));
      break;
      case mhpmevent8:
      asm volatile ("csrw %0, %1" :: "n"(mhpmevent8), "r"(val));
      break;
      case mhpmevent9:
      asm volatile ("csrw %0, %1" :: "n"(mhpmevent9), "r"(val));
      break;
      case mcountinhibit:
      asm volatile ("csrw %0, %1" :: "n"(mcountinhibit), "r"(val));
      break;
      case mcounteren:
      asm volatile ("csrw %0, %1" :: "n"(mcounteren), "r"(val));
      break;
      case scounteren:
      asm volatile ("csrw %0, %1" :: "n"(scounteren), "r"(val));
      break;
      default:
      printf("This should not happen!\n");
      exit(1);
    }
  }


  #define pseudo_write_csr_immediate(reg, val) ({ \
    asm volatile ("csrw " #reg ", %0" :: "rK"(val)); }) // Equivalent to "csrrw x0, csr, rs1"

  #define pseudo_read_csr_immediate(reg) ({ unsigned long __tmp; \
    asm volatile ("csrr %0, " #reg : "=r"(__tmp)); \
    __tmp; }) // Equivalent to "csrrs rd, csr, x0"

  /*
  #define read_set_csr(reg, bit) ({ unsigned long __tmp; \
    asm volatile ("csrrs %0, " #reg ", %1" : "=r"(__tmp) : "rK"(bit)); \
    __tmp; })
  */
  #define read_clear_csr(reg, bit) ({ unsigned long __tmp; \
    asm volatile ("csrrc %0, " #reg ", %1" : "=r"(__tmp) : "rK"(bit)); \
    __tmp; })

  /*
  #define read_csr(reg) ({ unsigned long __tmp; \
    asm volatile ("csrrs %0, " #reg ", x0" : "=r"(__tmp)); \
    __tmp; })
  */

  #define read_mcycle() ({ unsigned long __tmp; \
    asm volatile ("rdcycle %0" : "=r"(__tmp)); \
    __tmp; })

  #define read_mtime() ({ unsigned long __tmp; \
    asm volatile ("rdtime %0" : "=r"(__tmp)); \
    __tmp; })

  #define read_minstret() ({ unsigned long __tmp; \
    asm volatile ("rdinstret %0" : "=r"(__tmp)); \
    __tmp; })

};
#endif