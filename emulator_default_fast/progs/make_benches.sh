#!/bin/bash

# Define the 3 subfolders of interest
SUBFOLDERS=("benchmarks" "microbenchmarks" "olden")

# Iterate over each specified subfolder
for SUB in "${SUBFOLDERS[@]}"; do
    # Construct the full path to the subfolder
    BASE_PATH="./$SUB"

    # Ensure the base subfolder exists
    if [ -d "$BASE_PATH" ]; then
        # Find subdirectories that contain Makefile
        for DIR in $(find "$BASE_PATH" -type d); do
            if [ -f "$DIR/Makefile" ]; then
                # Check for .o or .riscv files
                if find "$DIR" -maxdepth 1 -type f \( -name "*.o" -o -name "*.riscv" \) | grep -q .; then
                    echo "Cleaning and making in $DIR"

                    # Run `make clean` and save output
                    make -C "$DIR" clean > "$DIR/make_clean.log" 2>&1

                # Run `make` and save output
                make -C "$DIR" > "$DIR/make.log" 2>&1
                fi
            fi
        done
    else
        echo "Warning: $BASE_PATH does not exist."
    fi
done
