CXX := /home/riscv/Documents/Chipyard/SavvinaThesis/riscv-gnu-toolchain/installed-tools/bin/riscv64-unknown-elf-g++
CLINUXXX := /home/riscv/Documents/Chipyard/SavvinaThesis/riscv-gnu-toolchain/installed-tools/bin/riscv64-unknown-linux-gnu-g++

CFLAGS = -std=c++11 -O3 -DGRANULARITY=9 -DDEBUG -DVALIDATE
CFLAGSLOOKUPONLY = -std=c++11 -O3 -DGRANULARITY=9 -DDEBUG -DVALIDATE -DLOOKUPONLY
CFLAGSNOATOM = -std=c++11 -O3 -DGRANULARITY=9 -DDEBUG -DVALIDATE -DNOATOM
CFLAGSMEAS = -std=c++11 -O3 -DGRANULARITY=9
CFLAGSLOOKUPONLYMEAS = -std=c++11 -O3 -DGRANULARITY=9 -DLOOKUPONLY
CFLAGSNOATOMMEAS = -std=c++11 -O3 -DGRANULARITY=9 -DNOATOM

LDFLAGS = -static
INCLUDES = -I../../util  -I../../XMemLib
LFLAGS = -L../../XMemLib
LIBS = -lXMemLib

SRCS = LinkedListBench.cpp 
HPC_SRC = ../../util/HPC.cpp

TARGET_RISCV = $(SRCS:.cpp=.riscv)
TARGET_LOOKUP_RISCV = $(SRCS:.cpp=_lookup.riscv)
TARGET_NOATOM_RISCV = $(SRCS:.cpp=_noatom.riscv)
TARGET_MEAS_RISCV = $(SRCS:.cpp=_meas.riscv)
TARGET_NOATOM_MEAS_RISCV = $(SRCS:.cpp=_meas_noatom.riscv)
TARGET_LOOKUP_MEAS_RISCV = $(SRCS:.cpp=_lookup_meas.riscv)

TARGET_LINUX = $(SRCS:.cpp=_linux.riscv)
TARGET_LOOKUP_LINUX = $(SRCS:.cpp=_lookup_linux.riscv)
TARGET_NOATOM_LINUX = $(SRCS:.cpp=_noatom_linux.riscv)
TARGET_MEAS_LINUX = $(SRCS:.cpp=_meas_linux.riscv)
TARGET_NOATOM_MEAS_LINUX = $(SRCS:.cpp=_meas_noatom_linux.riscv)
TARGET_LOOKUP_MEAS_LINUX = $(SRCS:.cpp=_lookup_meas_linux.riscv)

.PHONY: all clean

all: $(TARGET_RISCV) $(TARGET_LOOKUP_RISCV) $(TARGET_NOATOM_RISCV) $(TARGET_MEAS_RISCV) $(TARGET_NOATOM_MEAS_RISCV) $(TARGET_LOOKUP_MEAS_RISCV) $(TARGET_LINUX) $(TARGET_LOOKUP_LINUX) $(TARGET_NOATOM_LINUX) $(TARGET_MEAS_LINUX) $(TARGET_NOATOM_MEAS_LINUX) $(TARGET_LOOKUP_MEAS_LINUX)

#all: $(TARGET_RISCV) $(TARGET_LOOKUP_RISCV) $(TARGET_NOATOM_RISCV) $(TARGET_MEAS_RISCV) $(TARGET_NOATOM_MEAS_RISCV) $(TARGET_LOOKUP_MEAS_RISCV) 

# Compile standard
$(TARGET_RISCV): $(SRCS:.cpp=.o) hpc.o
	$(CXX) $(CFLAGS) $(INCLUDES) $(SRCS:.cpp=.o) hpc.o $(LFLAGS) $(LIBS) ../../XMemLib/libXMemLibdeb.a -o $@
   
# Compile lookup
$(TARGET_LOOKUP_RISCV): $(SRCS:.cpp=_lookup.o) hpc_lookup.o
	$(CXX) $(CFLAGSLOOKUPONLY) $(INCLUDES) $(SRCS:.cpp=_lookup.o) hpc_lookup.o $(LFLAGS) $(LIBS) ../../XMemLib/libXMemLibdeb.a -o $@

$(TARGET_NOATOM_RISCV): $(SRCS:.cpp=_noatom.o) hpc_noatom.o
	$(CXX) $(CFLAGSNOATOM) $(INCLUDES) $(SRCS:.cpp=_noatom.o) hpc_noatom.o $(LFLAGS) $(LIBS) ../../XMemLib/libXMemLibdeb.a -o $@

$(TARGET_MEAS_RISCV): $(SRCS:.cpp=_meas.o) hpc_meas.o
	$(CXX) $(CFLAGSMEAS) $(INCLUDES) $(SRCS:.cpp=_meas.o) hpc_meas.o $(LFLAGS) $(LIBS) ../../XMemLib/libXMemLib.a -o $@

$(TARGET_NOATOM_MEAS_RISCV): $(SRCS:.cpp=_meas_noatom.o) hpc_meas_noatom.o
	$(CXX) $(CFLAGSNOATOMMEAS) $(INCLUDES) $(SRCS:.cpp=_meas_noatom.o) hpc_meas_noatom.o $(LFLAGS) $(LIBS) ../../XMemLib/libXMemLib.a -o $@

$(TARGET_LOOKUP_MEAS_RISCV): $(SRCS:.cpp=_lookup_meas.o) hpc_lookup_meas.o
	$(CXX) $(CFLAGSLOOKUPONLYMEAS) $(INCLUDES) $(SRCS:.cpp=_lookup_meas.o) hpc_lookup_meas.o $(LFLAGS) $(LIBS) ../../XMemLib/libXMemLib.a -o $@

# Compile standard
$(TARGET_LINUX): $(SRCS:.cpp=_linux.o) hpc_linux.o
	$(CLINUXXX) $(CFLAGS) $(LDFLAGS) $(INCLUDES) $(SRCS:.cpp=_linux.o) hpc_linux.o $(LFLAGS) $(LIBS) ../../XMemLib/libXMemLibdeb.a -o $@
   
# Compile lookup
$(TARGET_LOOKUP_LINUX): $(SRCS:.cpp=_lookup_linux.o) hpc_lookup_linux.o
	$(CLINUXXX) $(CFLAGSLOOKUPONLY) $(LDFLAGS) $(INCLUDES) $(SRCS:.cpp=_lookup_linux.o) hpc_lookup_linux.o $(LFLAGS) $(LIBS) ../../XMemLib/libXMemLibdeb.a -o $@

$(TARGET_NOATOM_LINUX): $(SRCS:.cpp=_noatom_linux.o) hpc_noatom_linux.o
	$(CLINUXXX) $(CFLAGSNOATOM) $(LDFLAGS) $(INCLUDES) $(SRCS:.cpp=_noatom_linux.o) hpc_noatom_linux.o $(LFLAGS) $(LIBS) ../../XMemLib/libXMemLibdeb.a -o $@

$(TARGET_MEAS_LINUX): $(SRCS:.cpp=_meas_linux.o) hpc_meas_linux.o
	$(CLINUXXX) $(CFLAGSMEAS) $(LDFLAGS) $(INCLUDES) $(SRCS:.cpp=_meas_linux.o) hpc_meas_linux.o $(LFLAGS) $(LIBS) ../../XMemLib/libXMemLib.a -o $@

$(TARGET_NOATOM_MEAS_LINUX): $(SRCS:.cpp=_meas_noatom_linux.o) hpc_meas_noatom_linux.o
	$(CLINUXXX) $(CFLAGSNOATOMMEAS) $(LDFLAGS) $(INCLUDES) $(SRCS:.cpp=_meas_noatom_linux.o) hpc_meas_noatom_linux.o $(LFLAGS) $(LIBS) ../../XMemLib/libXMemLib.a -o $@

$(TARGET_LOOKUP_MEAS_LINUX): $(SRCS:.cpp=_lookup_meas_linux.o) hpc_lookup_meas_linux.o
	$(CLINUXXX) $(CFLAGSLOOKUPONLYMEAS) $(LDFLAGS) $(INCLUDES) $(SRCS:.cpp=_lookup_meas_linux.o) hpc_lookup_meas_linux.o $(LFLAGS) $(LIBS) ../../XMemLib/libXMemLib.a -o $@

##################

$(SRCS:.cpp=.o): $(SRCS)
	$(CXX) $(CFLAGS) $(INCLUDES) -c $< -o $@

$(SRCS:.cpp=_lookup.o): $(SRCS)
	$(CXX) $(CFLAGSLOOKUPONLY) $(INCLUDES) -c $< -o $@

$(SRCS:.cpp=_noatom.o): $(SRCS)
	$(CXX) $(CFLAGSNOATOM) $(INCLUDES) -c $< -o $@

$(SRCS:.cpp=_meas.o): $(SRCS)
	$(CXX) $(CFLAGSMEAS) $(INCLUDES) -c $< -o $@

$(SRCS:.cpp=_meas_noatom.o): $(SRCS)
	$(CXX) $(CFLAGSNOATOMMEAS) $(INCLUDES) -c $< -o $@

$(SRCS:.cpp=_lookup_meas.o): $(SRCS)
	$(CXX) $(CFLAGSLOOKUPONLYMEAS) $(INCLUDES) -c $< -o $@

$(SRCS:.cpp=_linux.o): $(SRCS)
	$(CLINUXXX) $(CFLAGS) $(INCLUDES) -c $< -o $@

$(SRCS:.cpp=_lookup_linux.o): $(SRCS)
	$(CLINUXXX) $(CFLAGSLOOKUPONLY) $(INCLUDES) -c $< -o $@

$(SRCS:.cpp=_noatom_linux.o): $(SRCS)
	$(CLINUXXX) $(CFLAGSNOATOM) $(INCLUDES) -c $< -o $@

$(SRCS:.cpp=_meas_linux.o): $(SRCS)
	$(CLINUXXX) $(CFLAGSMEAS) $(INCLUDES) -c $< -o $@

$(SRCS:.cpp=_meas_noatom_linux.o): $(SRCS)
	$(CLINUXXX) $(CFLAGSNOATOMMEAS) $(INCLUDES) -c $< -o $@

$(SRCS:.cpp=_lookup_meas_linux.o): $(SRCS)
	$(CLINUXXX) $(CFLAGSLOOKUPONLYMEAS) $(INCLUDES) -c $< -o $@

hpc.o: $(HPC_SRC)
	$(CXX) $(CFLAGS) $(INCLUDES) -c $< -o $@

hpc_lookup.o: $(HPC_SRC)
	$(CXX) $(CFLAGSLOOKUPONLY) $(INCLUDES) -c $< -o $@

hpc_noatom.o: $(HPC_SRC)
	$(CXX) $(CFLAGSNOATOM) $(INCLUDES) -c $< -o $@

hpc_meas.o: $(HPC_SRC)
	$(CXX) $(CFLAGSMEAS) $(INCLUDES) -c $< -o $@

hpc_meas_noatom.o: $(HPC_SRC)
	$(CXX) $(CFLAGSNOATOMMEAS) $(INCLUDES) -c $< -o $@

hpc_lookup_meas.o: $(HPC_SRC)
	$(CXX) $(CFLAGSLOOKUPONLYMEAS) $(INCLUDES) -c $< -o $@

hpc_linux.o: $(HPC_SRC)
	$(CLINUXXX) $(CFLAGS) $(INCLUDES) -c $< -o $@

hpc_lookup_linux.o: $(HPC_SRC)
	$(CLINUXXX) $(CFLAGSLOOKUPONLY) $(INCLUDES) -c $< -o $@

hpc_noatom_linux.o: $(HPC_SRC)
	$(CLINUXXX) $(CFLAGSNOATOM) $(INCLUDES) -c $< -o $@

hpc_meas_linux.o: $(HPC_SRC)
	$(CLINUXXX) $(CFLAGSMEAS) $(INCLUDES) -c $< -o $@

hpc_meas_noatom_linux.o: $(HPC_SRC)
	$(CLINUXXX) $(CFLAGSNOATOMMEAS) $(INCLUDES) -c $< -o $@

hpc_lookup_meas_linux.o: $(HPC_SRC)
	$(CLINUXXX) $(CFLAGSLOOKUPONLYMEAS) $(INCLUDES) -c $< -o $@

clean:
	rm -f *.o $(TARGET_RISCV) $(TARGET_LOOKUP_RISCV) $(TARGET_NOATOM_RISCV) $(TARGET_MEAS_RISCV) $(TARGET_NOATOM_MEAS_RISCV) $(TARGET_LOOKUP_MEAS_RISCV) $(TARGET_LINUX) $(TARGET_LOOKUP_LINUX) $(TARGET_NOATOM_LINUX) $(TARGET_MEAS_LINUX) $(TARGET_NOATOM_MEAS_LINUX) $(TARGET_LOOKUP_MEAS_LINUX)
