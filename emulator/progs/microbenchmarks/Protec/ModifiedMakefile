
CXX := /home/riscv/Documents/Chipyard/SavvinaThesis/riscv-gnu-toolchain/installed-tools/bin/riscv64-unknown-elf-g++
CLINUXXX := /home/riscv/Documents/Chipyard/SavvinaThesis/riscv-gnu-toolchain/installed-tools/bin/riscv64-unknown-linux-gnu-g++

CFLAGSNOATOM = -std=c++11 -O2 -finline-small-functions

LDFLAGS = -static
INCLUDES = -I../../util  -I../../XMemLib
LFLAGS = -L../../XMemLib
LIBS = -lXMemLib

SRCS = demo.cpp
HPC_SRC = ../../util/HPC.cpp

TARGET_NOATOM_RISCV = $(SRCS:.cpp=_noatom.riscv)
TARGET_ZCU_NOATOM_RISCV = $(SRCS:.cpp=_zcu_noatom.riscv)

.PHONY: all clean depend

all: $(TARGET_NOATOM_RISCV) $(TARGET_ZCU_NOATOM_RISCV)

$(TARGET_NOATOM_RISCV): $(SRCS:.cpp=_noatom.o) hpc_noatom.o
	$(CXX) $(CFLAGSNOATOM) $(INCLUDES) $(SRCS:.cpp=_noatom.o) hpc_noatom.o $(LFLAGS) $(LIBS) -o $@

$(TARGET_ZCU_NOATOM_RISCV): $(SRCS:.cpp=_zcu_noatom.o) 
	$(CLINUXXX) $(LDFLAGS) $(INCLUDES) $(SRCS:.cpp=_zcu_noatom.o) $(LFLAGS) $(LIBS) -o $@

$(SRCS:.cpp=_noatom.o): $(SRCS)
	$(CXX) $(CFLAGSNOATOM) $(INCLUDES) -c $< -o $@

$(SRCS:.cpp=_zcu_noatom.o): $(SRCS)
	$(CLINUXXX) $(CFLAGSNOATOM) $(INCLUDES) -c $< -o $@

hpc_noatom.o: $(HPC_SRC)
	$(CXX) $(CFLAGSNOATOM) $(INCLUDES) -c $< -o $@

hpc_zcu_noatom.o: $(HPC_SRC)
	$(CLINUXXX) $(CFLAGSNOATOM) $(INCLUDES) -c $< -o $@

clean:
	$(RM) *.o $(TARGET_NOATOM_RISCV) $(TARGET_ZCU_NOATOM_RISCV) *~

depend: $(SRCS) $(HPC_SRC)
	makedepend $(INCLUDES) $^
