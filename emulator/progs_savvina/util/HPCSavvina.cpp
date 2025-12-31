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

  #ifdef DEBUG
    printf("In HPC(): Before pseudo_write_csr_immediate(mhpmevent3, INTEGER_LOAD_INSTRUCTION_RETIRED)\n");
  #endif
  //pseudo_write_csr_immediate(mcounteren, -1); // All bits of mcounteren csr set to 1: All counters can be read in S-mode
  //pseudo_write_csr_immediate(scounteren, -1); // All bits of scounteren csr set to 1: All counters can be read in U-mode    

  // Associate counters with events
  pseudo_write_csr_immediate(mhpmevent3, INTEGER_LOAD_INSTRUCTION_RETIRED);
  pseudo_write_csr_immediate(mhpmevent4, INTEGER_STORE_INSTRUCTION_RETIRED);
  pseudo_write_csr_immediate(mhpmevent5, DATA_CACHE_DTIM_BUSY);
  pseudo_write_csr_immediate(mhpmevent6, DATA_CACHE_MISS_OR_MEMORY_MAPPED_IO_ACCESS);
  pseudo_write_csr_immediate(mhpmevent7, DATA_CACHE_WRITEBACK);
  pseudo_write_csr_immediate(mhpmevent8, DATA_TLB_MISS);
  pseudo_write_csr_immediate(mhpmevent9, L2_TLB_MISS);  

  #ifdef DEBUG
    printf("In HPC(): Before pseudo_write_csr_immediate(mcountinhibit, -1)\n");
  #endif 

  pseudo_write_csr_immediate(mcountinhibit, -1); // All bits of mcountinhibit csr set to 1: Counters do not increment
  // Added so that reads from some counters do not lead in incrementing others
}

HPC::~HPC(){}

uint8_t HPC::cacheThrasher[1024*16];

void HPC::startMeasurement()
{
  for (int i = 0 ; i < 1024*16 ; i++) 
    cacheThrasher[i] = (uint8_t) i+1;

  #ifdef DEBUG
    printf("In HPC::startMeasurement(): Before countersStart[3] = pseudo_read_csr_immediate(hpmcounter3)\n");
  #endif
  
  // for (int i = 3 ; i < 10 ; i++)
  //   countersStart[i] = pseudo_read_csr_immediate(counterAddresses[i]);
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
    printf("In HPC::startMeasurement(): Before pseudo_write_csr_immediate(mcountinhibit, 0xfffffc00)\n");
  #endif

  pseudo_write_csr_immediate(mcountinhibit, 0xfffffc00); // Bits corresponding to mcycle, mintret, mhpmcounter3-9 are set to 0: Counters increment
}

void HPC::endMeasurement()
{
  #ifdef DEBUG
    printf("In HPC::endMeasurement(): Before pseudo_write_csr_immediate(mcountinhibit, -1)\n");
  #endif

  pseudo_write_csr_immediate(mcountinhibit, -1); // All bits of mcountinhibit csr set to 1: Counters do not increment

  #ifdef DEBUG
    printf("In HPC::endMeasurement(): Before countersEnd[3] = pseudo_read_csr_immediate(hpmcounter3)\n");
  #endif
  // for (int i = 3 ; i < 10 ; i++)
  //   countersEnd[i] = pseudo_read_csr_immediate(counterAddresses[i]);
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
  for (int i = 0 ; i < 10 ; i++)
    printf("%ld,", countersEnd[i] - countersStart[i]);
}

void HPC::printStats()
{
  for (int i = 0 ; i < 10 ; i++)
    printf("%s: %ld\n", counterNames[i].c_str(), countersEnd[i] - countersStart[i]);
}
