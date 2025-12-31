import os
import re

# Define the base folder as the script's directory
BASE_FOLDER = os.path.dirname(os.path.abspath(__file__))
TARGET_SUBFOLDERS = {"subfolder1", "subfolder2", "subfolder3"}  # Replace with actual names

# Function to process CFLAGS and generate additional flags
def process_cflags(cflags_line):
    cflags = cflags_line.split("=", 1)[1].strip()
    cflags_atom = cflags.replace("-DNOATOM", "").strip()
    cflags_zcu = f"{cflags} -static"
    cflags_zcu_atom = f"{cflags_atom} -static" if "-DNOATOM" in cflags else None
    
    flags = [
        f"CFLAGS = {cflags}",
        f"CFLAGSATOM = {cflags_atom}" if "-DNOATOM" in cflags else None,
        f"CFLAGSZCU = {cflags_zcu}",
        f"CFLAGSZCUATOM = {cflags_zcu_atom}" if cflags_zcu_atom else None
    ]
    return "\n".join(filter(None, flags))

# Function to generate executable and object file rules
def generate_rules(source_files):
    rules = []
    for src in source_files:
        base = os.path.splitext(os.path.basename(src))[0]
        rules.append(f"{base}.riscv: {base}.o hpc.o\n\t$(CXX) $(CFLAGS) $(INCLUDES) $^ $(LFLAGS) $(LIBS) -o $@")
        rules.append(f"{base}_atom.riscv: {base}_atom.o hpc_atom.o\n\t$(CXX) $(CFLAGSATOM) $(INCLUDES) $^ $(LFLAGS) $(LIBS) -o $@")
        rules.append(f"{base}_zcu.riscv: {base}_zcu.o hpc_zcu.o\n\t$(CLINUXXX) $(CFLAGSZCU) $(INCLUDES) $^ $(LFLAGS) $(LIBS) -o $@")
        rules.append(f"{base}_zcu_atom.riscv: {base}_zcu_atom.o hpc_zcu_atom.o\n\t$(CLINUXXX) $(CFLAGSZCUATOM) $(INCLUDES) $^ $(LFLAGS) $(LIBS) -o $@")
    return "\n".join(rules) + "\n"

# Function to modify a Makefile
def modify_makefile(file_path):
    with open(file_path, 'r') as f:
        content = f.readlines()
    
    modified_content = []
    cflags_line = None
    source_files = []
    for line in content:
        if line.startswith("CXX :="):
            modified_content.append(f"# {line.strip()}\n")
            modified_content.append("CXX := /home/riscv/Documents/Chipyard/SavvinaThesis/riscv-gnu-toolchain/installed-tools/bin/riscv64-unknown-elf-g++\n")
        elif line.startswith("CFLAGS ="):
            cflags_line = line.strip()
            modified_content.append(line)
        elif line.startswith("SRCS ="):
            source_files = re.findall(r"\S+\.cpp", line)
            modified_content.append(line)
        else:
            modified_content.append(line)
    
    if cflags_line:
        modified_content.append(process_cflags(cflags_line) + "\n")
        modified_content.append("CLINUXXX := /home/riscv/Documents/Chipyard/SavvinaThesis/riscv-gnu-toolchain/installed-tools/bin/riscv64-unknown-linux-gnu-g++\n")
    
    if source_files:
        modified_content.append(generate_rules(source_files))
    
    with open(file_path, 'w') as f:
        f.writelines(modified_content)
    print(f"Modified: {file_path}")

# Process the Makefile in the script's directory
makefile_path = os.path.join(BASE_FOLDER, "Makefile")
if os.path.exists(makefile_path):
    modify_makefile(makefile_path)
