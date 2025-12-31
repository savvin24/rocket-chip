// heap_fragmenter.cpp (bin-aligned version with reusepct & spaciness — corrected)
// Usage: ./heap_fragmenter -n <node_count> -s <node_size> [options]
// Example: ./heap_fragmenter -n 100 -s 64 -a 5000 --allocnodes --reusepct 30 --spaciness 5
//
// Compile: riscv64-unknown-linux-gnu-g++ -O2 -std=c++17 heap_fragmenter.cpp -o heap_fragmenter

#include <bits/stdc++.h>
#ifdef __GLIBC__
#include <malloc.h> // for malloc_usable_size
#endif
using namespace std;

struct AllocRec {
    void* ptr = nullptr;
    size_t req_size = 0;
    size_t usable_size = 0;
    bool freed = false;
    bool was_big_enough = false;
    bool is_initial = false; // NEW: mark requests generated in Step 1
};

struct Req {
    size_t size;
    bool is_initial;
};

static unsigned int rng_state = 0xC0FFEE;
static unsigned int next_rand() {
    rng_state = rng_state * 1664525u + 1013904223u;
    return rng_state;
}
static size_t rand_range(size_t lo, size_t hi) {
    if (hi <= lo) return lo;
    return lo + (next_rand() % (hi - lo + 1));
}

int main(int argc, char** argv) {
    size_t node_size = 64;
    size_t node_count = 100;
    size_t total_allocs = 5000;
    bool do_alloc_nodes = false;
    unsigned int seed = (unsigned int)time(nullptr);

    // New parameters
    size_t reuse_pct = 70;     // % of initial nodes expected to be reused
    size_t spaciness = 0;      // noise allocations between nodes

    // Parse args
    for (int i = 1; i < argc; ++i) {
        string a = argv[i];
        if (a == "-s" || a == "--nodesize") { node_size = strtoull(argv[++i], nullptr, 0); }
        else if (a == "-n" || a == "--nodes") { node_count = strtoull(argv[++i], nullptr, 0); }
        else if (a == "-a" || a == "--allocs") { total_allocs = strtoull(argv[++i], nullptr, 0); }
        else if (a == "--allocnodes") { do_alloc_nodes = true; }
        else if (a == "--seed") { seed = (unsigned int)strtoul(argv[++i], nullptr, 0); }
        else if (a == "--reusepct") { reuse_pct = strtoull(argv[++i], nullptr, 0); }
        else if (a == "--spaciness") { spaciness = strtoull(argv[++i], nullptr, 0); }
        else if (a == "-h" || a == "--help") {
            cout << "Usage: " << argv[0] << " -n <node_count> -s <node_size> [options]\n"
                 << "Options:\n"
                 << "  -a, --allocs <N>     Total mallocs to perform (default 5000)\n"
                 << "  --allocnodes         After fragmenting, allocate node_count nodes\n"
                 << "  --seed <num>         RNG seed\n"
                 << "  --reusepct <N>       Target %% of initial nodes reused from freed chunks (default 70)\n"
                 << "  --spaciness <N>      Noise allocs between freed nodes (default 0)\n";
            return 0;
        } else {
            cerr << "Unknown arg: " << a << "\n";
            return 1;
        }
    }

    rng_state = seed;
    if (total_allocs < node_count) total_allocs = node_count * 3;

    cout << "Node size: " << node_size << ", Node count: " << node_count << "\n";
    cout << "Total mallocs: " << total_allocs << ", Seed: " << seed << "\n";
    cout << "Target reuse: " << reuse_pct << "%, Spaciness: " << spaciness << "\n";

    vector<Req> reqs;
    reqs.reserve(total_allocs);

    // Step 1: create controlled bin-aligned blocks (as Req entries)
    size_t reuse_target = node_count * reuse_pct / 100;
    for (size_t i = 0; i < node_count; ++i) {
        size_t s;
        if (i < reuse_target) {
            // Eligible for reuse: same bin as node_size
            s = rand_range(node_size, node_size + 16);
        } else {
            // Not reusable: deliberately mismatched (keep it outside target small bin)
            s = rand_range(node_size * 2, node_size * 4);
        }
        reqs.push_back({s, true}); // mark as initial

        // Insert noise allocations to control spacing (noise are non-initial)
        for (size_t j = 0; j < spaciness; ++j) {
            size_t noise_hi = (node_size > 2 ? node_size / 2 : 1);
            if (noise_hi < 1) noise_hi = 1;
            size_t noise = rand_range(1, noise_hi);
            reqs.push_back({noise, false});
        }
    }

    // Step 2: fill rest with random sizes, but avoid accidentally adding
    // sizes in the small target bin [node_size .. node_size+16] to keep control over reuse.
    for (size_t i = reqs.size(); i < total_allocs; ++i) {
        unsigned int r = next_rand() % 100;
        size_t s;
        if (r < 60) {
            // small
            s = rand_range(1, (node_size > 1) ? (node_size - 1) : 1);
        } else if (r < 90) {
            // medium: choose a medium that avoids the target small bin
            size_t low = node_size + 17;
            size_t high = node_size * 2;
            if (high < low) high = low; // guard for small node_size values
            s = rand_range(low, high);
        } else {
            // large
            s = rand_range(node_size * 2, node_size * 8);
        }
        reqs.push_back({s, false});
    }

    // Shuffle all requests (preserves is_initial flags)
    shuffle(reqs.begin(), reqs.end(), default_random_engine(rng_state));

    // Step 3: malloc
    size_t total = reqs.size();
    vector<AllocRec> recs(total);
    for (size_t i = 0; i < total; ++i) {
        void* p = malloc(reqs[i].size);
        if (!p) {
            cerr << "malloc failed at i=" << i << " size=" << reqs[i].size << "\n";
        }
        recs[i].ptr = p;
        recs[i].req_size = reqs[i].size;
#ifdef __GLIBC__
        recs[i].usable_size = (p ? malloc_usable_size(p) : 0);
#else
        recs[i].usable_size = recs[i].req_size;
#endif
        recs[i].was_big_enough = (reqs[i].size >= node_size) && (reqs[i].size <= (node_size + 16));
        recs[i].is_initial = reqs[i].is_initial;
    }

    // Step 4: free at least node_count of the initial bin-aligned blocks
    vector<size_t> bin_indices;
    for (size_t i = 0; i < recs.size(); ++i) {
        // Only consider entries that are both initial and match the bin
        if (recs[i].ptr && recs[i].is_initial && recs[i].was_big_enough) {
            bin_indices.push_back(i);
        }
    }
    if (bin_indices.size() < node_count) {
        cerr << "Warning: not enough initial bin-aligned blocks. Found "
             << bin_indices.size() << " initial bin-aligned, need " << node_count << "\n";
    }

    shuffle(bin_indices.begin(), bin_indices.end(),
            default_random_engine(rng_state ^ 0x12345678));
    size_t to_free = min(node_count, bin_indices.size());
    for (size_t i = 0; i < to_free; ++i) {
        free(recs[bin_indices[i]].ptr);
        recs[bin_indices[i]].freed = true;
    }

    cout << "Freed " << to_free << " bin-aligned blocks to create holes.\n";

    // Show a sample
    size_t shown = 0;
    cout << "Sample freed blocks (addr : req usable):\n";
    for (size_t i = 0; i < recs.size() && shown < 20; ++i) {
        if (recs[i].freed) {
            cout << recs[i].ptr << " : " << recs[i].req_size
                 << " (" << recs[i].usable_size << ")\n";
            ++shown;
        }
    }

    // Step 5: optionally allocate list nodes
    if (do_alloc_nodes) {
        cout << "\nNow allocating " << node_count << " nodes of size "
             << node_size << "\n";
        unordered_map<uintptr_t, size_t> freed_map;
        for (size_t i = 0; i < recs.size(); ++i) {
            if (recs[i].freed && recs[i].ptr)
                freed_map[(uintptr_t)recs[i].ptr] = i;
        }

        size_t reused = 0;
        for (size_t i = 0; i < node_count; ++i) {
            void* p = malloc(node_size);
            uintptr_t up = (uintptr_t)p;
            auto it = freed_map.find(up);
            if (it != freed_map.end()) {
                ++reused;
                cout << "Node " << i << " at " << p
                     << " (REUSED freed idx=" << it->second
                     << " req=" << recs[it->second].req_size
                     << " usable=" << recs[it->second].usable_size
                     << ")\n";
            } else {
                cout << "Node " << i << " at " << p << "\n";
            }
        }
        cout << "\nReuse ratio: " << reused << " / " << node_count << "\n";
    }

    // Cleanup
    for (auto &r : recs) {
        if (r.ptr && !r.freed) free(r.ptr);
    }
    return 0;
}
