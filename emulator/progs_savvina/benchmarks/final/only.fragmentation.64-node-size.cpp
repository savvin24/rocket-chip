// heap_fragmenter.cpp (bin-aligned version with reusepct & spaciness — corrected)
// Usage: ./heap_fragmenter -n <node_count> -s <NODESIZE> [options]
// Example: ./heap_fragmenter -n 100 -s 64 -a 5000 --allocnodes --reusepct 30 --spaciness 5
//
// Compile: riscv64-unknown-linux-gnu-g++ -O2 -std=c++17 heap_fragmenter.cpp -o heap_fragmenter

#include <bits/stdc++.h>
#ifdef __GLIBC__
#include <malloc.h> // for malloc_usable_size
#endif
using namespace std;

static size_t NODESIZE = 64;
unsigned long long memfootprint = 0;

struct Req {
    size_t size;
    void* ptr;
};

vector<Req> mallocs_info;

int main(int argc, char** argv) {
    size_t node_count = 100;
    size_t total_allocs = 100000;

    const char* hole_index_filename = nullptr;

    size_t reuse_pct = 70;     // % of initial nodes expected to be allocated in holes

    // Parse args
    for (int i = 1; i < argc; ++i) {
        string a = argv[i];
        if (a == "-s" || a == "--nodesize") { NODESIZE = strtoull(argv[++i], nullptr, 0); }
        else if (a == "-n" || a == "--nodes") { node_count = strtoull(argv[++i], nullptr, 0); }
        else if (a == "-hl" || a == "--holes") { hole_index_filename = argv[++i]; } 
        else if (a == "-a" || a == "--allocs") { total_allocs = strtoull(argv[++i], nullptr, 0); }
        else if (a == "--reusepct") { reuse_pct = strtoull(argv[++i], nullptr, 0); }
        else if (a == "-h" || a == "--help") {
            cout << "Usage: " << argv[0] << " -n <node_count> -s <NODESIZE> [options]\n"
                 << "Options:\n"
                 << "  -hl, --holes <file> File containing traversal indices\n"
                 << "  -a, --allocs <N>     Total mallocs to perform (default 5000)\n"
                 << "  --allocnodes         After fragmenting, allocate node_count nodes\n"
                 << "  --reusepct <N>       Target %% of initial nodes reused from freed chunks (default 70)\n";
            return 0;
        } else {
            cerr << "Unknown arg: " << a << "\n";
            return 1;
        }
    }

    if (hole_index_filename == nullptr && reuse_pct > 0) {
        cerr << "Error: Hole file not specified. Use -hl or --holes.\n";
        return 1;
    }

    vector<int> holeIndex_vec; 
    int holeIndex_vec_size = 0;

    if (reuse_pct > 0)
    {
        ifstream hole_index_file(hole_index_filename);
        if (!hole_index_file.is_open()) {
            cerr << "Error: Could not open random index file " << hole_index_filename << endl;
            return 1;
        }
        int r_val;
        while (hole_index_file >> r_val) {
            holeIndex_vec.push_back(r_val);
        }
        hole_index_file.close();

        holeIndex_vec_size = holeIndex_vec.size();
        if(holeIndex_vec_size < (int)(node_count * reuse_pct / 100)) {
            cerr << "Error: Size of holeIndex_vec is less than required number of holes to meet reuse_pct." << endl;
            return 1; 
        }
    }

    size_t s;

    if(NODESIZE > 64) s = 64;
    else s = NODESIZE;

    for (size_t i = 0; i < total_allocs; i++)
    {
        void* p = malloc(s);
        if (!p) {
            cerr << "malloc failed at i=" << i << " size=" << s << "\n";
        }
        mallocs_info.push_back({s, p}); // Store size and pointer
        memfootprint += s;
    }

    if (reuse_pct > 0)
    {
        for (size_t i = 0; i < holeIndex_vec.size(); i++)
        {
            size_t idx = holeIndex_vec[i];
            if (idx < mallocs_info.size()) {
                printf("Freeing malloc index %zu, ptr %p, size %zu\n", idx, mallocs_info[idx].ptr, mallocs_info[idx].size);
                free((void*)mallocs_info[idx].ptr);
                // Mark as freed in malloc_sizes by setting to 0
                mallocs_info[idx].size = 0;
            } else {
                cerr << "Warning: Hole index " << idx << " out of bounds\n";
            }
        }
    }

    return 0;
}
