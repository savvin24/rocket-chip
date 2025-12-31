#!/bin/bash

# Define the subfolders of interest
SUBFOLDERS=("benchmarks" "microbenchmarks" "olden")  # Replace with actual names

# Define the log file
LOG_FILE="make_benches.log"

# Clear the log file at the start
> "$LOG_FILE"

# Iterate through the specified subfolders
for folder in "${SUBFOLDERS[@]}"; do
    # Ensure the subfolder exists
    if [[ -d "$folder" ]]; then
        # Find all subdirectories containing a Makefile
        find "$folder" -type f -name "Makefile" | while read -r makefile; do
            make_dir=$(dirname "$makefile")
            echo "Running make in $make_dir" | tee -a "$LOG_FILE"
            (cd "$make_dir" && make >> "../../$LOG_FILE" 2>&1)
            echo "--------------------------------------" | tee -a "$LOG_FILE"
        done
    else
        echo "Skipping missing folder: $folder" | tee -a "$LOG_FILE"
    fi
done

echo "All make processes completed. Check $LOG_FILE for details."
