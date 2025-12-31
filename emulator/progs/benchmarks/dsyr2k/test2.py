import os
import re

# Define the source directory
source_dir = "."

# Collect all .cpp files
cpp_files = [f for f in os.listdir(source_dir) if f.endswith(".cpp")]

# Separate HPC sources from regular sources
hpc_srcs = [f for f in cpp_files if "hpc" in f.lower()]
normal_srcs = [f for f in cpp_files if f not in hpc_srcs]

# Convert lists to Makefile-compatible format
hpc_srcs_str = " \\\n    ".join(hpc_srcs)
normal_srcs_str = " \\\n    ".join(normal_srcs)

# Generate object files
obj_files = [f.replace(".cpp", ".o") for f in cpp_files]
obj_files_str = " \\\n    ".join(obj_files)

# Create Makefile content
makefile_content = f"""
SRCS = {normal_srcs_str}
HPC_SRC = {hpc_srcs_str}

CFLAGSATOM = -O2 -march=rv64imafdc -mabi=lp64d -Wall -Wextra
CFLAGSZCU = -O2 -march=armv8-a -Wall -Wextra
CFLAGSZCUATOM = -O2 -march=armv8-a+crypto -Wall -Wextra

OBJ = {obj_files_str}

JB1D_RISCV = jb1d_riscv.elf
JB1D_ZCU_RISCV = jb1d_zcu_riscv.elf

all: $(JB1D_RISCV) $(JB1D_ZCU_RISCV)

$(JB1D_RISCV): $(OBJ)
\t$(CXX) $(CFLAGSATOM) -o $@ $^

$(JB1D_ZCU_RISCV): $(OBJ)
\t$(CXX) $(CFLAGSZCUATOM) -o $@ $^

%.o: %.cpp
\t$(CXX) $(CFLAGSATOM) -c $< -o $@

clean:
\trm -f $(OBJ) $(JB1D_RISCV) $(JB1D_ZCU_RISCV)
"""

# Write to Makefile
with open("Makefile", "w") as f:
    f.write(makefile_content)

print("Makefile generated successfully!")
