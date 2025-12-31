import os
import shutil

def restore_makefiles(root_dir):
    """Restores Makefile from OldMakefile in current folder"""
    
    makefile_path = os.path.join(root_dir, "Makefile")
    old_makefile_path = os.path.join(root_dir, "OldMakefile")
        
    if os.path.exists(makefile_path) and os.path.exists(old_makefile_path):
        # Copy contents of OldMakefile to Makefile
        shutil.copy(old_makefile_path, makefile_path)
        # Delete OldMakefile
        os.remove(old_makefile_path)
        print(f"Restored {makefile_path} and deleted {old_makefile_path}")

# Example usage
restore_makefiles('.')