// heap_fragmenter.cpp
// Usage: ./heap_fragmenter -n <node_count> -s <node_size> [options]
// Example: ./heap_fragmenter -n 100 -s 64 -a 5000 -seed 12345 -allocnodes
//
// Compiles: riscv64-unknown-linux-gnu-g++ -O2 -std=c++17 heap_fragmenter.cpp -o heap_fragmenter
// (or use any other g++)

#include <bits/stdc++.h>
#include <unistd.h>
#include <sys/types.h>
#ifdef __GLIBC__
#include <malloc.h> // for malloc_usable_size (glibc)
#endif

using namespace std;

struct AllocRec {
    void* ptr = nullptr;
    size_t req_size = 0;      // requested size
    size_t usable_size = 0;   // malloc usable size if available
    bool freed = false;
    bool was_big_enough = false; // req_size >= node_size
};

static unsigned int rng_state = 0xC0FFEE;

// small thread-safe-ish rand
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

    // parse args (simple)
    for (int i = 1; i < argc; ++i) {
        string a = argv[i];
        if (a == "-s" || a == "--nodesize") { node_size = strtoull(argv[++i], nullptr, 0); }
        else if (a == "-n" || a == "--nodes") { node_count = strtoull(argv[++i], nullptr, 0); }
        else if (a == "-a" || a == "--allocs") { total_allocs = strtoull(argv[++i], nullptr, 0); }
        else if (a == "--allocnodes") { do_alloc_nodes = true; }
        else if (a == "--seed") { seed = (unsigned int)strtoul(argv[++i], nullptr, 0); }
        else if (a == "-h" || a == "--help") {
            cout << "Usage: " << argv[0] << " -n <node_count> -s <node_size> [options]\n"
                 << "Options:\n"
                 << "  -a, --allocs <N>     Total mallocs to perform (default 5000)\n"
                 << "  --allocnodes         After fragmenting, allocate node_count mallocs of size node_size and report addresses\n"
                 << "  --seed <num>         RNG seed\n"
                 << "  -h, --help           Show this message\n";
            return 0;
        } else {
            cerr << "Unknown arg: " << a << "\n";
            return 1;
        }
    }

    if (total_allocs < node_count) total_allocs = node_count * 3; // ensure enough room

    // seed rng
    rng_state = seed;

    cout << "Node size: " << node_size << ", Node count: " << node_count << "\n";
    cout << "Total mallocs to perform: " << total_allocs << "\n";
    cout << "Seed: " << seed << "\n";

    // Build requested sizes array such that at least node_count entries >= node_size
    vector<size_t> req_sizes;
    req_sizes.reserve(total_allocs);

    // Create the guaranteed >= node_size slots (sizes vary between node_size and 4*node_size)
    for (size_t i = 0; i < node_count; ++i) {
        size_t s = rand_range(node_size, node_size * 4);
        req_sizes.push_back(s);
    }

    // Fill the remainder with mixed sizes (some smaller, some larger)
    for (size_t i = req_sizes.size(); i < total_allocs; ++i) {
        // 60% small (1..node_size-1), 30% medium (node_size..2*node_size), 10% large (2*node_size..8*node_size)
        unsigned int r = next_rand() % 100;
        size_t s;
        if (r < 60) {
            s = rand_range(1, (node_size > 1 ? node_size - 1 : 1));
        } else if (r < 90) {
            s = rand_range(node_size, node_size * 2);
        } else {
            s = rand_range(node_size * 2, node_size * 8 + 1);
        }
        req_sizes.push_back(s);
    }

    // Shuffle sizes to scatter big allocations among the rest
    shuffle(req_sizes.begin(), req_sizes.end(), std::default_random_engine(rng_state));

    // Now do mallocs and record
    vector<AllocRec> recs;
    recs.resize(total_allocs);

    size_t count_big_enough = 0;
    for (size_t i = 0; i < total_allocs; ++i) {
        size_t s = req_sizes[i];
        void* p = malloc(s);
        if (!p) {
            cerr << "malloc failed at i=" << i << " size=" << s << "\n";
            // continue and keep record as null
        }
        recs[i].ptr = p;
        recs[i].req_size = s;
#ifdef __GLIBC__
        if (p) recs[i].usable_size = malloc_usable_size(p);
        else recs[i].usable_size = 0;
#else
        recs[i].usable_size = s;
#endif
        recs[i].freed = false;
        recs[i].was_big_enough = (s >= node_size);
        if (recs[i].was_big_enough) ++count_big_enough;
    }

    cout << "Performed " << total_allocs << " mallocs. Allocations with req_size >= node_size: " << count_big_enough << "\n";

    // Collect indices of allocations that are >= node_size and still allocated
    vector<size_t> big_indices;
    for (size_t i = 0; i < recs.size(); ++i) {
        if (recs[i].ptr && recs[i].was_big_enough) big_indices.push_back(i);
    }

    // Ensure we have at least node_count big blocks (we constructed that way, but double-check)
    if (big_indices.size() < node_count) {
        cerr << "Error: not enough big allocations available to free. Found " << big_indices.size() << ", need " << node_count << "\n";
        // proceed with what we have
    }

    // Choose at least node_count of these to free (we will free exactly node_count unless impossible)
    size_t to_free = min(node_count, big_indices.size());

    // Shuffle big_indices using rng
    shuffle(big_indices.begin(), big_indices.end(), std::default_random_engine(rng_state ^ 0xA5A5A5A5));

    vector<size_t> free_indices(big_indices.begin(), big_indices.begin() + to_free);

    // To increase scattering, we can also optionally free a few more random big ones
    size_t extra = min((size_t) (to_free / 10), big_indices.size() - to_free);
    for (size_t i = 0; i < extra; ++i) {
        free_indices.push_back(big_indices[to_free + i]);
    }

    // Now free them (in random order)
    shuffle(free_indices.begin(), free_indices.end(), std::default_random_engine(rng_state ^ 0x55AA55AA));
    for (size_t idx : free_indices) {
        if (recs[idx].ptr && !recs[idx].freed) {
            free(recs[idx].ptr);
            recs[idx].freed = true;
        }
    }

    cout << "Freed " << free_indices.size() << " blocks that were >= node_size (creating holes).\n";

    // Print a small sample of freed blocks (addresses and sizes)
    cout << "Sample freed blocks (addr : req_size usable_size):\n";
    size_t shown = 0;
    for (size_t i = 0; i < recs.size() && shown < 20; ++i) {
        if (recs[i].freed) {
            cout << recs[i].ptr << " : " << recs[i].req_size << " (usable " << recs[i].usable_size << ")\n";
            ++shown;
        }
    }
    if (shown == 0) cout << "<none shown>\n";

    // Optional: allocate node_count nodes of size node_size and see if they land in freed slots
    if (do_alloc_nodes) {
        cout << "\nNow allocating " << node_count << " nodes of size " << node_size << " to observe reuse.\n";
        vector<void*> nodes;
        nodes.reserve(node_count);

        unordered_map<uintptr_t, size_t> freed_ptrs_map;
        for (size_t i = 0; i < recs.size(); ++i) {
            if (recs[i].freed && recs[i].ptr) freed_ptrs_map[(uintptr_t)recs[i].ptr] = i;
        }

        size_t reused_count = 0;
        for (size_t i = 0; i < node_count; ++i) {
            void* p = malloc(node_size);
            nodes.push_back(p);
            uintptr_t up = (uintptr_t)p;
            auto it = freed_ptrs_map.find(up);
            if (it != freed_ptrs_map.end()) {
                ++reused_count;
                cout << "Node " << i << " allocated at " << p << " (REUSED freed block idx=" << it->second
                     << " req_size=" << recs[it->second].req_size << " usable=" << recs[it->second].usable_size << ")\n";
            } else {
                cout << "Node " << i << " allocated at " << p << "\n";
            }
        }

        cout << "\nReused exact freed pointers: " << reused_count << " / " << node_count << "\n";

        // cleanup nodes
        for (void* p : nodes) if (p) free(p);
    } else {
        cout << "\nDone fragmenting heap. Run again with --allocnodes to allocate and observe reuse.\n";
    }

    // Cleanup: free remaining allocated blocks to avoid leak
    for (size_t i = 0; i < recs.size(); ++i) {
        if (recs[i].ptr && !recs[i].freed) {
            free(recs[i].ptr);
            recs[i].freed = true;
        }
    }

    cout << "Cleaned up remaining allocations. Exiting.\n";
    return 0;
}
