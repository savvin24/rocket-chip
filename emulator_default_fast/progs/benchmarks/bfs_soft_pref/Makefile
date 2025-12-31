
CXX := /home/riscv/Documents/Chipyard/SavvinaThesis/riscv-gnu-toolchain/installed-tools/bin/riscv64-unknown-elf-g++
CLINUXXX := /home/riscv/Documents/Chipyard/SavvinaThesis/riscv-gnu-toolchain/installed-tools/bin/riscv64-unknown-linux-gnu-g++

CFLAGSNOATOM = -std=c++11 -O3 -DNOATOM

CFLAGS = -std=c++11 -O3 

LDFLAGS = -static
INCLUDES = -I../../util  -I../../XMemLib
LFLAGS = -L../../XMemLib
LIBS = -lXMemLib

SRCS = bfs.cpp
HPC_SRC = ../../util/HPC.cpp

TARGET_NOATOM_RISCV = $(SRCS:.cpp=_noatom.riscv)
TARGET_ZCU_NOATOM_RISCV = $(SRCS:.cpp=_zcu_noatom.riscv)

TARGET_RISCV = $(SRCS:.cpp=.riscv)
TARGET_ZCU_RISCV = $(SRCS:.cpp=_zcu.riscv)

.PHONY: all clean depend

all: $(TARGET_NOATOM_RISCV) $(TARGET_RISCV)

$(TARGET_NOATOM_RISCV): $(SRCS:.cpp=_noatom.o) hpc_noatom.o
	$(CXX) $(CFLAGSNOATOM) $(INCLUDES) $(SRCS:.cpp=_noatom.o) hpc_noatom.o $(LFLAGS) $(LIBS) -o $@

$(TARGET_ZCU_NOATOM_RISCV): $(SRCS:.cpp=_zcu_noatom.o) 
	$(CLINUXXX) $(LDFLAGS) $(INCLUDES) $(SRCS:.cpp=_zcu_noatom.o) $(LFLAGS) $(LIBS) -o $@

$(TARGET_RISCV): $(SRCS:.cpp=.o) hpc.o
	$(CXX) $(CFLAGS) $(INCLUDES) $(SRCS:.cpp=.o) hpc.o $(LFLAGS) $(LIBS) -o $@

$(TARGET_ZCU_RISCV): $(SRCS:.cpp=_zcu.o) 
	$(CLINUXXX) $(LDFLAGS) $(INCLUDES) $(SRCS:.cpp=_zcu.o) $(LFLAGS) $(LIBS) -o $@

$(SRCS:.cpp=_noatom.o): $(SRCS)
	$(CXX) $(CFLAGSNOATOM) $(INCLUDES) -c $< -o $@

$(SRCS:.cpp=_zcu_noatom.o): $(SRCS)
	$(CLINUXXX) $(CFLAGSNOATOM) $(INCLUDES) -c $< -o $@

$(SRCS:.cpp=.o): $(SRCS)
	$(CXX) $(CFLAGS) $(INCLUDES) -c $< -o $@

$(SRCS:.cpp=_zcu.o): $(SRCS)
	$(CLINUXXX) $(CFLAGS) $(INCLUDES) -c $< -o $@

hpc_noatom.o: $(HPC_SRC)
	$(CXX) $(CFLAGSNOATOM) $(INCLUDES) -c $< -o $@

hpc_zcu_noatom.o: $(HPC_SRC)
	$(CLINUXXX) $(CFLAGSNOATOM) $(INCLUDES) -c $< -o $@

hpc.o: $(HPC_SRC)
	$(CXX) $(CFLAGS) $(INCLUDES) -c $< -o $@

hpc_zcu.o: $(HPC_SRC)
	$(CLINUXXX) $(CFLAGS) $(INCLUDES) -c $< -o $@

clean:
	$(RM) *.o $(TARGET_NOATOM_RISCV) $(TARGET_RISCV) *~

depend: $(SRCS) $(HPC_SRC)
	makedepend $(INCLUDES) $^
