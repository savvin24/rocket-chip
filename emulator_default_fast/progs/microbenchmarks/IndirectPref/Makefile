
CXX := /home/riscv/Documents/Chipyard/SavvinaThesis/riscv-gnu-toolchain/installed-tools/bin/riscv64-unknown-elf-g++
CLINUXXX := /home/riscv/Documents/Chipyard/SavvinaThesis/riscv-gnu-toolchain/installed-tools/bin/riscv64-unknown-linux-gnu-g++

CFLAGS = -std=c++11 -O3

LDFLAGS = -static
INCLUDES = -I../../util  -I../../XMemLib
LFLAGS = -L../../XMemLib
LIBS = -lXMemLib

SRCS = ind_pref_microbenchmark.cpp
HPC_SRC = ../../util/HPC.cpp

TARGET_RISCV = $(SRCS:.cpp=.riscv)
TARGET_ZCU_RISCV = $(SRCS:.cpp=_zcu.riscv)

.PHONY: all clean depend

all: $(TARGET_RISCV)

$(TARGET_RISCV): $(SRCS:.cpp=.o) hpc.o
	$(CXX) $(CFLAGS) $(INCLUDES) $(SRCS:.cpp=.o) hpc.o $(LFLAGS) $(LIBS) -o $@

$(TARGET_ZCU_RISCV): $(SRCS:.cpp=_zcu.o) 
	$(CLINUXXX) $(LDFLAGS) $(INCLUDES) $(SRCS:.cpp=_zcu.o) $(LFLAGS) $(LIBS) -o $@

$(SRCS:.cpp=.o): $(SRCS)
	$(CXX) $(CFLAGS) $(INCLUDES) -c $< -o $@

$(SRCS:.cpp=_zcu.o): $(SRCS)
	$(CLINUXXX) $(CFLAGS) $(INCLUDES) -c $< -o $@

hpc.o: $(HPC_SRC)
	$(CXX) $(CFLAGS) $(INCLUDES) -c $< -o $@

hpc_zcu.o: $(HPC_SRC)
	$(CLINUXXX) $(CFLAGS) $(INCLUDES) -c $< -o $@

clean:
	$(RM) *.o $(TARGET_RISCV) *~

depend: $(SRCS) $(HPC_SRC)
	makedepend $(INCLUDES) $^
