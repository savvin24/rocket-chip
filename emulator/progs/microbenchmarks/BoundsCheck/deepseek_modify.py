import re

def modify_makefile(input_file, output_file):
    with open(input_file, 'r') as file:
        content = file.read()

    # Extract CFLAGS from the Makefile
    cflags_match = re.search(r'CFLAGS\s*=\s*(.*)', content)
    if not cflags_match:
        raise ValueError("CFLAGS not found in the Makefile")
    cflags = cflags_match.group(1).strip()

    # Extract SRCS from the Makefile
    srcs_match = re.search(r'SRCS\s*=\s*(.*)', content)
    if not srcs_match:
        raise ValueError("SRCS not found in the Makefile")
    srcs = srcs_match.group(1).strip()

    # Filter out utility files (e.g., ../../util/HPC.cpp)
    srcs_list = srcs.split()
    program_srcs = [src for src in srcs_list if 'util' not in src]
    program_srcs = ' '.join(program_srcs)

    # Extract INCLUDES from the Makefile
    includes_match = re.search(r'INCLUDES\s*=\s*(.*)', content)
    includes = includes_match.group(1).strip() if includes_match else ""

    # Extract LFLAGS from the Makefile
    lflags_match = re.search(r'LFLAGS\s*=\s*(.*)', content)
    lflags = lflags_match.group(1).strip() if lflags_match else ""

    # Extract LIBS from the Makefile
    libs_match = re.search(r'LIBS\s*=\s*(.*)', content)
    libs = libs_match.group(1).strip() if libs_match else ""

    # Extract MAIN from the Makefile (if it exists)
    main_match = re.search(r'MAIN\s*=\s*(.*)', content)
    main = main_match.group(1).strip() if main_match else ""

    # Check if -DNOATOM is present in CFLAGS
    has_noatom = '-DNOATOM' in cflags

    # Define the new content dynamically
    new_content = f"""
CXX := /home/riscv/Documents/Chipyard/SavvinaThesis/riscv-gnu-toolchain/installed-tools/bin/riscv64-unknown-elf-g++
CLINUXXX := /home/riscv/Documents/Chipyard/SavvinaThesis/riscv-gnu-toolchain/installed-tools/bin/riscv64-unknown-linux-gnu-g++

CFLAGSNOATOM = {cflags}
"""

    if has_noatom:
        new_content += f"""
CFLAGS = {cflags.replace('-DNOATOM', '')}
"""

    new_content += f"""
LDFLAGS = -static
INCLUDES = {includes}
LFLAGS = {lflags}
LIBS = {libs}

SRCS = {program_srcs}
HPC_SRC = ../../util/HPC.cpp

TARGET_NOATOM_RISCV = $(SRCS:.cpp=_noatom.riscv)
TARGET_ZCU_NOATOM_RISCV = $(SRCS:.cpp=_zcu_noatom.riscv)
"""

    if has_noatom:
        new_content += f"""
TARGET_RISCV = $(SRCS:.cpp=.riscv)
TARGET_ZCU_RISCV = $(SRCS:.cpp=_zcu.riscv)
"""

    # Define the `all` rule in a single line
    all_targets = ["$(TARGET_NOATOM_RISCV)", "$(TARGET_ZCU_NOATOM_RISCV)"]
    if has_noatom:
        all_targets.extend(["$(TARGET_RISCV)", "$(TARGET_ZCU_RISCV)"])
    all_rule = " ".join(all_targets)

    new_content += f"""
.PHONY: all clean depend

all: {all_rule}
"""

    new_content += f"""
$(TARGET_NOATOM_RISCV): $(SRCS:.cpp=_noatom.o) hpc_noatom.o
	$(CXX) $(CFLAGSNOATOM) $(INCLUDES) $(SRCS:.cpp=_noatom.o) hpc_noatom.o $(LFLAGS) $(LIBS) -o $@

$(TARGET_ZCU_NOATOM_RISCV): $(SRCS:.cpp=_zcu_noatom.o) hpc_zcu_noatom.o
	$(CLINUXXX) $(LDFLAGS) $(INCLUDES) $(SRCS:.cpp=_zcu_noatom.o) hpc_zcu_noatom.o $(LFLAGS) $(LIBS) -o $@
"""

    if has_noatom:
        new_content += f"""
$(TARGET_RISCV): $(SRCS:.cpp=.o) hpc.o
	$(CXX) $(CFLAGS) $(INCLUDES) $(SRCS:.cpp=.o) hpc.o $(LFLAGS) $(LIBS) -o $@

$(TARGET_ZCU_RISCV): $(SRCS:.cpp=_zcu.o) hpc_zcu.o
	$(CLINUXXX) $(LDFLAGS) $(INCLUDES) $(SRCS:.cpp=_zcu.o) hpc_zcu.o $(LFLAGS) $(LIBS) -o $@
"""

    new_content += f"""
$(SRCS:.cpp=_noatom.o): $(SRCS)
	$(CXX) $(CFLAGSNOATOM) $(INCLUDES) -c $< -o $@

$(SRCS:.cpp=_zcu_noatom.o): $(SRCS)
	$(CLINUXXX) $(CFLAGSNOATOM) $(INCLUDES) -c $< -o $@
"""

    if has_noatom:
        new_content += f"""
$(SRCS:.cpp=.o): $(SRCS)
	$(CXX) $(CFLAGS) $(INCLUDES) -c $< -o $@

$(SRCS:.cpp=_zcu.o): $(SRCS)
	$(CLINUXXX) $(CFLAGS) $(INCLUDES) -c $< -o $@
"""

    new_content += f"""
hpc_noatom.o: $(HPC_SRC)
	$(CXX) $(CFLAGSNOATOM) $(INCLUDES) -c $< -o $@

hpc_zcu_noatom.o: $(HPC_SRC)
	$(CLINUXXX) $(CFLAGSNOATOM) $(INCLUDES) -c $< -o $@
"""

    if has_noatom:
        new_content += f"""
hpc.o: $(HPC_SRC)
	$(CXX) $(CFLAGS) $(INCLUDES) -c $< -o $@

hpc_zcu.o: $(HPC_SRC)
	$(CLINUXXX) $(CFLAGS) $(INCLUDES) -c $< -o $@
"""

    # Define the `clean` rule in a single line
    clean_targets = ["*.o", "$(TARGET_NOATOM_RISCV)", "$(TARGET_ZCU_NOATOM_RISCV)"]
    if has_noatom:
        clean_targets.extend(["$(TARGET_RISCV)", "$(TARGET_ZCU_RISCV)"])
    clean_rule = " ".join(clean_targets)

    new_content += f"""
clean:
	$(RM) {clean_rule} *~

depend: $(SRCS) $(HPC_SRC)
	makedepend $(INCLUDES) $^
"""

    # Write the modified content to the output file
    with open(output_file, 'w') as file:
        file.write(new_content)

# Example usage
input_makefile = 'Makefile'
output_makefile = 'ModifiedMakefile'
modify_makefile(input_makefile, output_makefile)