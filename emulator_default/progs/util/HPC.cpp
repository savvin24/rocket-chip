#include <stdint.h>
#include <stdio.h>
#include <string>

#include "HPC.h"

HPC::HPC()
{
  for (int i = 0 ; i < 2 ; i++)
    countersSnapshot[i] = 0;
}

HPC::~HPC(){}

uint8_t HPC::cacheThrasher[1024*16];

void HPC::startMeasurement()
{
  for (int i = 0 ; i < 1024*16 ; i++) 
    cacheThrasher[i] = (uint8_t) i+1;
  
  for (int i = 0 ; i < 2 ; i++)
    countersSnapshot[i] = read_csr_safe(counterAddresses[i]);
  printf("IN HPC:startMeasurement: Returning from HPC:startMeasurement\n");
}

void HPC::endMeasurement()
{
  for (int i = 0 ; i < 2 ; i++)
    countersSnapshot[i] = read_csr_safe(counterAddresses[i]) - countersSnapshot[i];
  printf("IN HPC:endMeasurement: Returning from HPC:endMeasurement\n");
}

void HPC::printCSV()
{
  for (int i = 0 ; i < 1 ; i++)
    printf("%ld,", countersSnapshot[i]);
  printf("%ld\n", countersSnapshot[1]);
  printf("IN HPC:printCSV: Returning from HPC:printCSV\n");
}

void HPC::printStats()
{
  for (int i = 0 ; i < 2 ; i++)
    //printf("%s: %ld\n", counterNames[i].c_str(), countersSnapshot[i]);
    printf("%ld\n", countersSnapshot[i]);
  printf("IN HPC:printStats: Returning from HPC:printStats\n"); 
}
