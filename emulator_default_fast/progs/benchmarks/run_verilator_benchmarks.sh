#!/bin/bash

# Get the current directory (script location)
SUBFOLDER=$(pwd)

# Base path for microbenchmarks
BASE_MICROBENCH_PATH="/home/riscv/Documents/Chipyard/SavvinaThesis/chipyard/generators/rocket-chip/emulator_default/progs/benchmarks"

INPUT_FILE="/home/riscv/Documents/Chipyard/SavvinaThesis/chipyard/generators/rocket-chip/emulator_default/progs/benchmarks/input_graph.txt"

# Define benchmarks that do NOT need "input.txt"
BENCHMARKS_WITHOUT_INPUT=("corr" "dct" "dsyr2k" "dynprog" "fdtd-1d" "floyd" "gesum" "gramschmidt" "jacobi1d" "jacobi2d" "lu" "mvt")

# Define benchmarks that require IndirectPrefetcherBFSRocketConfigPerfCount simulator
BENCHMARKS_BFS=("bfs" "bfs_soft_pref" "char_bfs")

# Define the path to the simulators and pk
SIMULATOR_INDIRECT_BFS="/home/riscv/Documents/Chipyard/SavvinaThesis/chipyard/sims/verilator/simulator-chipyard.harness-IndirectPrefetcherBFSRocketConfigPerfCount"
SIMULATOR_PREFETCHER="/home/riscv/Documents/Chipyard/SavvinaThesis/chipyard/sims/verilator/simulator-chipyard.harness-PrefetcherRocketConfigWithFPUPerfCount"
PK="/home/riscv/Documents/Chipyard/SavvinaThesis/chipyard/.conda-env/riscv-tools/pk_wout_metasys/riscv64-unknown-elf/bin/pk"

# Recursively find and process files
find "$SUBFOLDER" -mindepth 2 -type f \( -name "*_noatom.riscv" -o -name "*.riscv" \) | while read -r FILE; do

    DIR_PATH=$(dirname "$FILE")
    BENCHMARK_NAME=$(basename "$DIR_PATH")  # Extract subfolder name
    FILENAME=$(basename "$FILE")            # Extract only filename

    # Skip specific files
    if [[ ( "$BENCHMARK_NAME" == "floyd" && "$FILENAME" == "floyd_noatom.riscv" ) || \
          ( "$BENCHMARK_NAME" == "sssp_soft_pref" && "$FILENAME" == "sssp_noatom.riscv" ) || \
          ( "$BENCHMARK_NAME" == "sssp_soft_pref" && "$FILENAME" == "sssp.riscv" ) || \
          ( "$BENCHMARK_NAME" == "radii" && "$FILENAME" == "radii_noatom.riscv" ) || \
          ( "$BENCHMARK_NAME" == "sssp" && "$FILENAME" == "sssp.riscv" ) || \
          ( "$BENCHMARK_NAME" == "char_sssp" && "$FILENAME" == "sssp.riscv" ) || \
          ( "$BENCHMARK_NAME" == "char_radii" && "$FILENAME" == "radii_noatom.riscv" ) || \
          ( "$BENCHMARK_NAME" == "char_radii" && "$FILENAME" == "radii.riscv" ) || \
          ( "$BENCHMARK_NAME" == "char_connected_components" && "$FILENAME" == "cc.riscv" ) || \
          ( "$BENCHMARK_NAME" == "bfs" && "$FILENAME" == "bfs_noatom.riscv" ) || \
          ( "$BENCHMARK_NAME" == "char_triangle_counting" && "$FILENAME" == "tc_noatom.riscv" ) || \
          ( "$BENCHMARK_NAME" == "jacobi1d" && "$FILENAME" == "jacobi1d.riscv" ) || \
          ( "$BENCHMARK_NAME" == "jacobi1d" && "$FILENAME" == "jacobi1d_noatom.riscv" ) || \
          ( "$BENCHMARK_NAME" == "mvt" && "$FILENAME" == "mvt_noatom.riscv" ) || \
          ( "$BENCHMARK_NAME" == "mvt" && "$FILENAME" == "mvt.riscv" )]]; then
        echo "Skipping excluded file: $BENCHMARK_NAME/$FILENAME"
        continue
    fi

    # Construct the correct absolute file path
    ABS_FILE="$BASE_MICROBENCH_PATH/$BENCHMARK_NAME/$FILENAME"
    
    # Ensure path resolution is successful
    if [[ ! -f "$ABS_FILE" ]]; then
        echo "Error: Could not resolve full path for $FILE" >&2
    fi

    # Determine log file name
    if [[ "$ABS_FILE" == *_noatom.riscv ]]; then
        LOG_FILE="$BASE_MICROBENCH_PATH/$BENCHMARK_NAME/verilator_preffpu_noatom.log"
    else
        LOG_FILE="$BASE_MICROBENCH_PATH/$BENCHMARK_NAME/verilator_preffpu.log"
    fi

    # Determine the correct simulator and input file usage
    if [[ " ${BENCHMARKS_BFS[@]} " =~ " ${BENCHMARK_NAME} " ]]; then
        SIMULATOR="$SIMULATOR_INDIRECT_BFS"
        CMD="$SIMULATOR $PK \"$ABS_FILE\" $INPUT_FILE"
        INPUT_MSG="Input file: $INPUT_FILE"
    elif [[ " ${BENCHMARKS_WITHOUT_INPUT[@]} " =~ " ${BENCHMARK_NAME} " ]]; then
        SIMULATOR="$SIMULATOR_PREFETCHER"
        CMD="$SIMULATOR $PK \"$ABS_FILE\""
        INPUT_MSG="No input file needed"
    else
        SIMULATOR="$SIMULATOR_PREFETCHER"
        CMD="$SIMULATOR $PK \"$ABS_FILE\" $INPUT_FILE"
        INPUT_MSG="Input file: $INPUT_FILE"
    fi

    # Debug: Print full path info
    echo "-------------------------------------------"
    echo "Running simulation on: $ABS_FILE"
    echo "Using simulator: $SIMULATOR"
    echo "Log file: $LOG_FILE"
    echo "$INPUT_MSG"
    echo "-------------------------------------------"

    # Execute the command, print output, and store in log file
    eval "$CMD" | tee "$LOG_FILE"
    echo "Output stored in: $LOG_FILE"
done
