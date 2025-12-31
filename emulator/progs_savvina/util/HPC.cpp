#include <stdint.h>
#include <stdio.h>
#include <string>

#include "HPC.h"

HPC::HPC()
{
  for (int i = 0 ; i < 10 ; i++)
  {
    //countersSnapshot[i] = 0;
    countersStart[i] = 0;
    countersEnd[i] = 0;
  }

  //write_csr_safe(mcounteren, -1); // All bits of mcounteren csr set to 1: All counters can be read in S-mode
  //write_csr_safe(scounteren, -1); // All bits of scounteren csr set to 1: All counters can be read in U-mode

  #ifdef DEBUG
    printf("In HPC(): Before write_csr_safe(mhpmevent3, INTEGER_LOAD_INSTRUCTION_RETIRED)\n");
  #endif

  // Associate counters with events
  write_csr_safe(mhpmevent3, INTEGER_LOAD_INSTRUCTION_RETIRED);
  //write_csr_safe(mhpmevent4, DATA_CACHE_MISS_OR_MEMORY_MAPPED_IO_ACCESS);
  write_csr_safe(mhpmevent4, INTEGER_STORE_INSTRUCTION_RETIRED);
  write_csr_safe(mhpmevent5, DATA_CACHE_DTIM_BUSY);
  write_csr_safe(mhpmevent6, DATA_CACHE_MISS_OR_MEMORY_MAPPED_IO_ACCESS);
  write_csr_safe(mhpmevent7, DATA_CACHE_WRITEBACK);
  write_csr_safe(mhpmevent8, DATA_TLB_MISS);
  write_csr_safe(mhpmevent9, L2_TLB_MISS);  

  #ifdef DEBUG
    printf("In HPC(): Before write_csr_safe(mcountinhibit, -1)\n");
  #endif 

  write_csr_safe(mcountinhibit, -1); // All bits of mcountinhibit csr set to 1: Counters do not increment
  //Added so that reads from some counters do not lead in incrementing others
}

HPC::~HPC(){}

uint8_t HPC::cacheThrasher[1024*16];

void HPC::startMeasurement()
{
  for (int i = 0 ; i < 1024*16 ; i++) 
    cacheThrasher[i] = (uint8_t) i+1;

  #ifdef DEBUG
    printf("In HPC::startMeasurement(): Before countersStart[i] = read_csr_safe(counterAddresses[i])\n");
  #endif
  
  // for (int i = 3 ; i < 10 ; i++)
  //   countersStart[i] = read_csr_safe(counterAddresses[i]);

  countersStart[3] = pseudo_read_csr_immediate(hpmcounter3);
  countersStart[4] = pseudo_read_csr_immediate(hpmcounter4);
  countersStart[5] = pseudo_read_csr_immediate(hpmcounter5);
  countersStart[6] = pseudo_read_csr_immediate(hpmcounter6);
  countersStart[7] = pseudo_read_csr_immediate(hpmcounter7);
  countersStart[8] = pseudo_read_csr_immediate(hpmcounter8);
  countersStart[9] = pseudo_read_csr_immediate(hpmcounter9);

  // countersStart[2] = pseudo_read_csr(CSR_INSTRET);
  // countersStart[1] = pseudo_read_csr(CSR_TIME);
  // countersStart[0] = pseudo_read_csr(CSR_CYCLE);

  countersStart[2] = read_minstret();
  countersStart[1] = read_mtime();
  countersStart[0] = read_mcycle();

  #ifdef DEBUG
     printf("In HPC::startMeasurement(): Before write_csr_safe(mcountinhibit, 0xfffffc00)\n");
  #endif

  write_csr_safe(mcountinhibit, 0xfffffc00); // Bits corresponding to mcycle, mintret, mhpmcounter3-9 are set to 0: Counters increment
}

void HPC::endMeasurement()
{
  #ifdef DEBUG
    printf("In HPC::endMeasurement(): Before write_csr_safe(mcountinhibit, -1)\n");
  #endif

  write_csr_safe(mcountinhibit, -1); // All bits of mcountinhibit csr set to 1: Counters do not increment

  #ifdef DEBUG
    printf("In HPC::endMeasurement(): Before countersEnd[i] = read_csr_safe(counterAddresses[i])\n");
  #endif

  // for (int i = 3 ; i < 10 ; i++)
  //   countersEnd[i] = read_csr_safe(counterAddresses[i]);
  
  countersEnd[3] = pseudo_read_csr_immediate(hpmcounter3);
  countersEnd[4] = pseudo_read_csr_immediate(hpmcounter4);
  countersEnd[5] = pseudo_read_csr_immediate(hpmcounter5);
  countersEnd[6] = pseudo_read_csr_immediate(hpmcounter6);
  countersEnd[7] = pseudo_read_csr_immediate(hpmcounter7);
  countersEnd[8] = pseudo_read_csr_immediate(hpmcounter8);
  countersEnd[9] = pseudo_read_csr_immediate(hpmcounter9);

  // countersEnd[2] = pseudo_read_csr(CSR_INSTRET);
  // countersEnd[1] = pseudo_read_csr(CSR_TIME);
  // countersEnd[0] = pseudo_read_csr(CSR_CYCLE);

  countersEnd[2] = read_minstret();
  countersEnd[1] = read_mtime();
  countersEnd[0] = read_mcycle();
}

void HPC::printCSV()
{
  for (int i = 0 ; i < 4 ; i++)
  {
    printf("%ld", countersEnd[i] - countersStart[i]);
    printf(", ");
  }

  printf("%ld\n", countersEnd[4] - countersStart[4]);
}

void HPC::printStats()
{
  for (int i = 0 ; i < 5 ; i++)
    printf("%s: %ld\n", counterNames[i].c_str(), countersEnd[i] - countersStart[i]);
}
