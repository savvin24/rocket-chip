#ifndef HPC_HH
#define HPC_HH
#include <stdint.h>
#include <stdio.h>
#include <string>
/** Helper class used to measure performance
 */
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
  static const int cycle         = 0xc00;
  static const int instret       = 0xc02;

  const int counterAddresses[2] = 
  {
    cycle, instret
  };

  std::string counterNames[2] =
  {
    "cycles", "instructions"
  };

  uint64_t countersSnapshot[2];

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
      default:
      printf("This should not happen!\n");
      exit(1);
    }
    return __tmp;
  }

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
      default:
      printf("This should not happen!\n");
      exit(1);
    }
    return __tmp;
  }

};
#endif