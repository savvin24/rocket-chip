CXX := /home/riscv/Documents/Chipyard/SavvinaThesis/riscv-gnu-toolchain/installed-tools/bin/riscv64-unknown-elf-gcc
CLINUXXX := /home/riscv/Documents/Chipyard/SavvinaThesis/riscv-gnu-toolchain/installed-tools/bin/riscv64-unknown-linux-gnu-gcc

CFLAGS = -O3 -DGRANULARITY=6 -DDEBUG -DVALIDATE
CFLAGSLOOKUPONLY = -O3 -DGRANULARITY=6 -DDEBUG -DVALIDATE -DLOOKUPONLY
CFLAGSNOATOM = -O3 -DGRANULARITY=6 -DDEBUG -DVALIDATE -DNOATOM
CFLAGSMEAS = -O3 -DGRANULARITY=6
CFLAGSLOOKUPONLYMEAS = -O3 -DGRANULARITY=6 -DLOOKUPONLY
CFLAGSNOATOMMEAS = -O3 -DGRANULARITY=6 -DNOATOM

LDFLAGS = -static
INCLUDES = -I../../../XMemLib
LFLAGS = -L../../../XMemLib
LIBS = -lXMemLib -lm

# --- FIX 1: Use 'wildcard' to actually list the files ---
SRCS := $(wildcard *.c)

# --- NOTE: Your old Makefile included this file. Un-comment if needed: ---
# SRCS += ../../util/HPC_C.c

# Define Object lists for each target type
OBJS_RISCV           = $(SRCS:.c=.o)
OBJS_LOOKUP_RISCV    = $(SRCS:.c=_lookup.o)
OBJS_NOATOM_RISCV    = $(SRCS:.c=_noatom.o)
OBJS_MEAS_RISCV      = $(SRCS:.c=_meas.o)
OBJS_NOATOM_MEAS_RISCV = $(SRCS:.c=_meas_noatom.o)
OBJS_LOOKUP_MEAS_RISCV = $(SRCS:.c=_lookup_meas.o)

OBJS_LINUX           = $(SRCS:.c=_linux.o)
OBJS_LOOKUP_LINUX    = $(SRCS:.c=_lookup_linux.o)
OBJS_NOATOM_LINUX    = $(SRCS:.c=_noatom_linux.o)
OBJS_MEAS_LINUX      = $(SRCS:.c=_meas_linux.o)
OBJS_NOATOM_MEAS_LINUX = $(SRCS:.c=_meas_noatom_linux.o)
OBJS_LOOKUP_MEAS_LINUX = $(SRCS:.c=_lookup_meas_linux.o)

# Define Target Names
TARGET_RISCV = em3d.riscv
TARGET_LOOKUP_RISCV = em3d_lookup.riscv
TARGET_NOATOM_RISCV = em3d_noatom.riscv
TARGET_MEAS_RISCV = em3d_meas.riscv
TARGET_NOATOM_MEAS_RISCV = em3d_meas_noatom.riscv
TARGET_LOOKUP_MEAS_RISCV = em3d_lookup_meas.riscv

TARGET_LINUX = em3d_linux.riscv
TARGET_LOOKUP_LINUX = em3d_lookup_linux.riscv
TARGET_NOATOM_LINUX = em3d_noatom_linux.riscv
TARGET_MEAS_LINUX = em3d_meas_linux.riscv
TARGET_NOATOM_MEAS_LINUX = em3d_meas_noatom_linux.riscv
TARGET_LOOKUP_MEAS_LINUX = em3d_lookup_meas_linux.riscv

.PHONY: all clean

all: $(TARGET_LINUX) $(TARGET_NOATOM_LINUX) $(TARGET_MEAS_LINUX) $(TARGET_NOATOM_MEAS_LINUX)

# ================= LINKS =================

# RISCV Linking
$(TARGET_RISCV): $(OBJS_RISCV)
	$(CXX) $(CFLAGS) $(INCLUDES) $^ $(LFLAGS) $(LIBS) ../../../XMemLib/libXMemLibdeb.a -o $@

$(TARGET_LOOKUP_RISCV): $(OBJS_LOOKUP_RISCV)
	$(CXX) $(CFLAGSLOOKUPONLY) $(INCLUDES) $^ $(LFLAGS) $(LIBS) ../../../XMemLib/libXMemLibdeb.a -o $@

$(TARGET_NOATOM_RISCV): $(OBJS_NOATOM_RISCV)
	$(CXX) $(CFLAGSNOATOM) $(INCLUDES) $^ $(LFLAGS) $(LIBS) ../../../XMemLib/libXMemLibdeb.a -o $@

$(TARGET_MEAS_RISCV): $(OBJS_MEAS_RISCV)
	$(CXX) $(CFLAGSMEAS) $(INCLUDES) $^ $(LFLAGS) $(LIBS) ../../../XMemLib/libXMemLib.a -o $@

$(TARGET_NOATOM_MEAS_RISCV): $(OBJS_NOATOM_MEAS_RISCV)
	$(CXX) $(CFLAGSNOATOMMEAS) $(INCLUDES) $^ $(LFLAGS) $(LIBS) ../../../XMemLib/libXMemLib.a -o $@

$(TARGET_LOOKUP_MEAS_RISCV): $(OBJS_LOOKUP_MEAS_RISCV)
	$(CXX) $(CFLAGSLOOKUPONLYMEAS) $(INCLUDES) $^ $(LFLAGS) $(LIBS) ../../../XMemLib/libXMemLib.a -o $@

# LINUX Linking
$(TARGET_LINUX): $(OBJS_LINUX)
	$(CLINUXXX) $(CFLAGS) $(LDFLAGS) $(INCLUDES) $^ $(LFLAGS) $(LIBS) ../../../XMemLib/libXMemLibdeb.a -o $@

$(TARGET_LOOKUP_LINUX): $(OBJS_LOOKUP_LINUX)
	$(CLINUXXX) $(CFLAGSLOOKUPONLY) $(LDFLAGS) $(INCLUDES) $^ $(LFLAGS) $(LIBS) ../../../XMemLib/libXMemLibdeb.a -o $@

$(TARGET_NOATOM_LINUX): $(OBJS_NOATOM_LINUX)
	$(CLINUXXX) $(CFLAGSNOATOM) $(LDFLAGS) $(INCLUDES) $^ $(LFLAGS) $(LIBS) ../../../XMemLib/libXMemLibdeb.a -o $@

$(TARGET_MEAS_LINUX): $(OBJS_MEAS_LINUX)
	$(CLINUXXX) $(CFLAGSMEAS) $(LDFLAGS) $(INCLUDES) $^ $(LFLAGS) $(LIBS) ../../../XMemLib/libXMemLib.a -o $@

$(TARGET_NOATOM_MEAS_LINUX): $(OBJS_NOATOM_MEAS_LINUX)
	$(CLINUXXX) $(CFLAGSNOATOMMEAS) $(LDFLAGS) $(INCLUDES) $^ $(LFLAGS) $(LIBS) ../../../XMemLib/libXMemLib.a -o $@

$(TARGET_LOOKUP_MEAS_LINUX): $(OBJS_LOOKUP_MEAS_LINUX)
	$(CLINUXXX) $(CFLAGSLOOKUPONLYMEAS) $(LDFLAGS) $(INCLUDES) $^ $(LFLAGS) $(LIBS) ../../../XMemLib/libXMemLib.a -o $@

# ================= COMPILATION (Static Pattern Rules) =================
# This syntax tells Make: "For targets in list X that match pattern A, build them from pattern B"

# Standard RISCV
$(OBJS_RISCV): %.o: %.c
	$(CXX) $(CFLAGS) $(INCLUDES) -c $< -o $@

$(OBJS_LOOKUP_RISCV): %_lookup.o: %.c
	$(CXX) $(CFLAGSLOOKUPONLY) $(INCLUDES) -c $< -o $@

$(OBJS_NOATOM_RISCV): %_noatom.o: %.c
	$(CXX) $(CFLAGSNOATOM) $(INCLUDES) -c $< -o $@

$(OBJS_MEAS_RISCV): %_meas.o: %.c
	$(CXX) $(CFLAGSMEAS) $(INCLUDES) -c $< -o $@

$(OBJS_NOATOM_MEAS_RISCV): %_meas_noatom.o: %.c
	$(CXX) $(CFLAGSNOATOMMEAS) $(INCLUDES) -c $< -o $@

$(OBJS_LOOKUP_MEAS_RISCV): %_lookup_meas.o: %.c
	$(CXX) $(CFLAGSLOOKUPONLYMEAS) $(INCLUDES) -c $< -o $@

# Linux
$(OBJS_LINUX): %_linux.o: %.c
	$(CLINUXXX) $(CFLAGS) $(INCLUDES) -c $< -o $@

$(OBJS_LOOKUP_LINUX): %_lookup_linux.o: %.c
	$(CLINUXXX) $(CFLAGSLOOKUPONLY) $(INCLUDES) -c $< -o $@

$(OBJS_NOATOM_LINUX): %_noatom_linux.o: %.c
	$(CLINUXXX) $(CFLAGSNOATOM) $(INCLUDES) -c $< -o $@

$(OBJS_MEAS_LINUX): %_meas_linux.o: %.c
	$(CLINUXXX) $(CFLAGSMEAS) $(INCLUDES) -c $< -o $@

$(OBJS_NOATOM_MEAS_LINUX): %_meas_noatom_linux.o: %.c
	$(CLINUXXX) $(CFLAGSNOATOMMEAS) $(INCLUDES) -c $< -o $@

$(OBJS_LOOKUP_MEAS_LINUX): %_lookup_meas_linux.o: %.c
	$(CLINUXXX) $(CFLAGSLOOKUPONLYMEAS) $(INCLUDES) -c $< -o $@

clean:
	rm -f *.o *_linux.o *_lookup.o *_noatom.o *_meas.o $(TARGET_RISCV) $(TARGET_LINUX) *.riscv