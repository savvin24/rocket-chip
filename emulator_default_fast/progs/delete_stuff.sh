#!/bin/bash

# Define the 3 subfolders of interest
SUBFOLDERS=("benchmarks" "microbenchmarks" "olden")

# Iterate over each specified subfolder
for SUB in "${SUBFOLDERS[@]}"; do
    # Construct the full path to the subfolder
    BASE_PATH="./$SUB"

    # Ensure the base subfolder exists
    if [ -d "$BASE_PATH" ]; then
        # Find all subdirectories
        for DIR in $(find "$BASE_PATH" -type d); do
            # Check if make_output.log exists and delete it
            if [ -f "$DIR/verilator_indpreffpu_noatom.log" ]; then
                echo "Deleting $DIR/verilator_indpreffpu_noatom.log"
                rm "$DIR/verilator_indpreffpu_noatom.log"
            fi
        done
    else
        echo "Warning: $BASE_PATH does not exist."
    fi
done
