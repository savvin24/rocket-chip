#include <stdint.h>
#include <stdio.h>

#include "hpc.h"
#include <inttypes.h>

uint64_t counters_start[10] = {0};
uint64_t counters_end[10] = {0};
const char* counters_names[10] = {  "No Cycles", 
                                    "Time Elapsed", 
                                    "Instructions Retired", 
                                    "Integer loads retired",
                                    "Integer stores retired",
                                    "Data cache busy",
                                    "Data cache miss or memory mapped I/O access",
                                    "Data cache writeback",
                                    "Data TLB miss",
                                    "L2 TLB miss" };
static uint8_t cacheThrasher[1024*16]; // SAVVINA COMMENT: Added from MetaSys, size 1024*16 equal to L1 cache size

void hpcInit()
{
  //pseudo_write_csr(CSR_MCOUNTEREN, -1); // All bits of mcounteren csr set to 1: All counters can be read in S-mode
  //pseudo_write_csr(CSR_SCOUNTEREN, -1); // All bits of scounteren csr set to 1: All counters can be read in U-mode

  // Associate counters with events
  pseudo_write_csr_immediate(CSR_MHPMEVENT3, INTEGER_LOAD_INSTRUCTION_RETIRED);
  pseudo_write_csr_immediate(CSR_MHPMEVENT4, INTEGER_STORE_INSTRUCTION_RETIRED);
  pseudo_write_csr_immediate(CSR_MHPMEVENT5, DATA_CACHE_DTIM_BUSY);
  pseudo_write_csr_immediate(CSR_MHPMEVENT6, DATA_CACHE_MISS_OR_MEMORY_MAPPED_IO_ACCESS);
  pseudo_write_csr_immediate(CSR_MHPMEVENT7, DATA_CACHE_WRITEBACK);
  pseudo_write_csr_immediate(CSR_MHPMEVENT8, DATA_TLB_MISS);
  pseudo_write_csr_immediate(CSR_MHPMEVENT9, L2_TLB_MISS);

  pseudo_write_csr_immediate(CSR_MCOUNTINHIBIT, -1); // All bits of mcountinhibit csr set to 1: Counters do not increment 
  // Added so that reads from some counters do not lead in incrementing others
}

void hpcStartMeasurement()
{

  for (int i = 0 ; i < 1024*16 ; i++) 
    cacheThrasher[i] = (uint8_t) i+1;

  counters_start[3] = pseudo_read_csr_immediate(CSR_HPMCOUNTER3);
  counters_start[4] = pseudo_read_csr_immediate(CSR_HPMCOUNTER4);
  counters_start[5] = pseudo_read_csr_immediate(CSR_HPMCOUNTER5);
  counters_start[6] = pseudo_read_csr_immediate(CSR_HPMCOUNTER6);
  counters_start[7] = pseudo_read_csr_immediate(CSR_HPMCOUNTER7);
  counters_start[8] = pseudo_read_csr_immediate(CSR_HPMCOUNTER8);
  counters_start[9] = pseudo_read_csr_immediate(CSR_HPMCOUNTER9);
  // counters_start[2] = pseudo_read_csr(CSR_INSTRET);
  // counters_start[1] = pseudo_read_csr(CSR_TIME);
  // counters_start[0] = pseudo_read_csr(CSR_CYCLE);
  counters_start[2] = read_minstret();
  counters_start[1] = read_mtime();
  counters_start[0] = read_mcycle();

  pseudo_write_csr_immediate(CSR_MCOUNTINHIBIT, 0xfffffc00); // Bits corresponding to mcycle, mintret, mhpmcounter3-9 are set to 0: Counters increment
}

void hpcEndMeasurement()
{
  pseudo_write_csr_immediate(CSR_MCOUNTINHIBIT, -1); // All bits of mcountinhibit csr set to 1: Counters do not increment

  counters_end[3] = pseudo_read_csr_immediate(CSR_HPMCOUNTER3);
  counters_end[4] = pseudo_read_csr_immediate(CSR_HPMCOUNTER4);
  counters_end[5] = pseudo_read_csr_immediate(CSR_HPMCOUNTER5);
  counters_end[6] = pseudo_read_csr_immediate(CSR_HPMCOUNTER6);
  counters_end[7] = pseudo_read_csr_immediate(CSR_HPMCOUNTER7);
  counters_end[8] = pseudo_read_csr_immediate(CSR_HPMCOUNTER8);
  counters_end[9] = pseudo_read_csr_immediate(CSR_HPMCOUNTER9);
  // counters_end[2] = pseudo_read_csr(CSR_INSTRET);
  // counters_end[1] = pseudo_read_csr(CSR_TIME);
  // counters_end[0] = pseudo_read_csr(CSR_CYCLE);
  counters_end[2] = read_minstret();
  counters_end[1] = read_mtime();
  counters_end[0] = read_mcycle();
}

void hpcPrint()
{
  printf("HPC Counters:\n");
  for (int i = 0; i < 10; i++) {
    printf("%s: %lu\n", counters_names[i], counters_end[i] - counters_start[i]);
  }
}
