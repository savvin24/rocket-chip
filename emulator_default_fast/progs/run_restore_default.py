import os
import subprocess

# Define the base folder and the specific subfolders of interest
BASE_FOLDER = os.path.dirname(os.path.abspath(__file__))  # Current script location
TARGET_SUBFOLDERS = {"benchmarks", "microbenchmarks", "olden"}  # Replace with actual names
PYTHON_SCRIPT = "restore_default.py"  # Replace with your actual script name

# Traverse the base folder
for subfolder in os.listdir(BASE_FOLDER):
    subfolder_path = os.path.join(BASE_FOLDER, subfolder)
    
    if os.path.isdir(subfolder_path) and subfolder in TARGET_SUBFOLDERS:
        # Iterate through subdirectories of the selected subfolder
        for root, dirs, files in os.walk(subfolder_path):
            if "Makefile" in files:
                print(f"Running script in: {root}")
                try:
                    subprocess.run(["python", os.path.join(BASE_FOLDER, PYTHON_SCRIPT)], cwd=root, check=True)
                except subprocess.CalledProcessError as e:
                    print(f"Error running script in {root}: {e}")