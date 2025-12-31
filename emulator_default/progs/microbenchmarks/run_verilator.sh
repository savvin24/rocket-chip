#!/bin/bash

# Get the current directory (script location)
SUBFOLDER=$(pwd)

# Base path for microbenchmarks
BASE_MICROBENCH_PATH="/home/riscv/Documents/Chipyard/SavvinaThesis/chipyard/generators/rocket-chip/emulator_default/progs/microbenchmarks"

# Define microbenchmarks that need BoundsCheckerRocketConfigPerfCount simulator
MICROBENCHMARKS_BOUNDSCHECK=("ArrayBC" "ArraySim" "BoundsCheck" "Protec" "RandomAccessBC" "RandomAccessBCSim")

# Define the path to the simulators and pk
SIMULATOR_INDIRECT_PREFETCHER="/home/riscv/Documents/Chipyard/SavvinaThesis/chipyard/sims/verilator/simulator-chipyard.harness-IndirectPrefetcherRocketConfigPerfCount"
SIMULATOR_BOUNDS_CHECKER="/home/riscv/Documents/Chipyard/SavvinaThesis/chipyard/sims/verilator/simulator-chipyard.harness-BoundsCheckerRocketConfigPerfCount"
PK="/home/riscv/Documents/Chipyard/SavvinaThesis/chipyard/.conda-env/riscv-tools/pk_wout_metasys/riscv64-unknown-elf/bin/pk"

# Recursively find and process files
find "$SUBFOLDER" -mindepth 2 -type f \( -name "*_noatom.riscv" -o -name "*.riscv" \) | while read -r FILE; do

    DIR_PATH=$(dirname "$FILE")
    BENCHMARK_NAME=$(basename "$DIR_PATH")  # Extract subfolder name
    FILENAME=$(basename "$FILE")            # Extract only filename

    # Skip specific files
    if [[ ( "$BENCHMARK_NAME" == "Array" && "$FILENAME" == "stream-pk.riscv" ) || \
          ( "$BENCHMARK_NAME" == "Protec" && "$FILENAME" == "demo_noatom.riscv" ) || \
          ( "$BENCHMARK_NAME" == "BufferOverflowAttack" && "$FILENAME" == "attack.riscv" ) || \
          ( "$BENCHMARK_NAME" == "RandomAccessBCSim" && "$FILENAME" == "random_noatom.riscv" ) || \
          ( "$BENCHMARK_NAME" == "ArraySim" && "$FILENAME" == "stream-pk.riscv" ) || \
          ( "$BENCHMARK_NAME" == "ArraySim" && "$FILENAME" == "stream-pk_noatom.riscv" ) || \
          ( "$BENCHMARK_NAME" == "BoundsCheck" && "$FILENAME" == "demo.riscv" ) || \
          ( "$BENCHMARK_NAME" == "RandomAccess" && "$FILENAME" == "random_noatom.riscv" ) || \
          ( "$BENCHMARK_NAME" == "RandomAccessBC" && "$FILENAME" == "random_noatom.riscv" ) || \
          ( "$BENCHMARK_NAME" == "RandomAccess" && "$FILENAME" == "random.riscv" ) ]]; then
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

    # Choose the correct simulator
    if [[ " ${MICROBENCHMARKS_BOUNDSCHECK[@]} " =~ " ${BENCHMARK_NAME} " ]]; then
        SIMULATOR="$SIMULATOR_BOUNDS_CHECKER"
    else
        SIMULATOR="$SIMULATOR_INDIRECT_PREFETCHER"
    fi

    # Debug: Print full path info
    echo "-------------------------------------------"
    echo "Running simulation on: $ABS_FILE"
    echo "Using simulator: $SIMULATOR"
    echo "Log file: $LOG_FILE"
    echo "-------------------------------------------"

    # Execute the command, print output, and store in log file
    eval "$SIMULATOR $PK \"$ABS_FILE\"" | tee "$LOG_FILE"
    echo "Output stored in: $LOG_FILE"
done
