// heap_fragmenter.cpp (bin-aligned version with reusepct & spaciness — corrected)
// Usage: ./heap_fragmenter -n <node_count> -s <NODESIZE> [options]
// Example: ./heap_fragmenter -n 100 -s 64 -a 5000 --allocnodes --reusepct 30 --spaciness 5
//
// Compile: riscv64-unknown-linux-gnu-g++ -O2 -std=c++17 heap_fragmenter.cpp -o heap_fragmenter

#include <bits/stdc++.h>
#ifdef __GLIBC__
#include <malloc.h> // for malloc_usable_size
#endif
#include "crosslayer.h"
using namespace std;

constexpr size_t LINE_SIZE      = 64;        // bytes per cache line
constexpr size_t BUFFER_BYTES   = 1 << 20;   // 1 MB buffer, > 512KB L2
constexpr int PASSES            = 2;         // number of streaming passes

static size_t NODESIZE = 64;

struct Node {
    struct Node* next;
    uint64_t data[];
};

Node* head_ptr = nullptr;
Node* current_ptr = nullptr;

uint32_t mapSize;

struct Node* insert(uint64_t data) {
    struct Node* newNode = (struct Node*)malloc(NODESIZE);

    if (newNode == nullptr) {
        cerr << "Error: Memory allocation failed for newNode with data " << data << endl;
        return nullptr; // Indicate an error
    }

    for (size_t i = 0; i < (NODESIZE-sizeof(struct Node*))/sizeof(uint64_t); i++)
    {
        newNode->data[i] = data;
        #ifdef DEBUG 
           printf("newNode->data[%d] = %" PRIu64 "\n", i, newNode->data[i]);
        #endif
    }

    newNode->next = nullptr;

    #ifdef DEBUG
        printf("In insert for node with data %" PRIu64 ": sizeof(newNode) = %ld, sizeof(*newNode) = %ld\n", data ,sizeof(newNode), sizeof(*newNode));
        printf("In insert for node with data %" PRIu64 ": sizeof(struct Node) = %ld, sizeof(struct Node*) = %ld\n", data ,sizeof(struct Node), sizeof(struct Node*));
    #endif

    if (head_ptr == nullptr) {
        head_ptr = newNode;
    } else {
        current_ptr->next = newNode;  // TODO: Change upate orginal code
    }
    current_ptr = newNode;
    #ifdef DEBUG
    printf("%p\n", current_ptr);
    #endif
    return newNode;
}

void deleteList() {
    Node* current = head_ptr;
    Node* next;

    while (current != nullptr) {
        next = current->next;
        #ifndef NOATOM
            atom_unmap((void *) current, mapSize); // Unmap the current pointer from atom 0
        #endif
        free(current);
        current = next;
    }
}

Node* getNthElement(int n) {
    Node* current = head_ptr;
    int count = 0;

    while (current != nullptr && count < n) {
        current = current->next;
        count++;
    }
    return current;
}

void printListAddress() {
    Node* current = head_ptr;
    while (current != nullptr) {
        printf("%p ", (void*)current);
        current = current->next;
    }
    printf("\n");
}

// funstion to print the list
void printList() {
    Node* current = head_ptr;
    while (current != nullptr) {
        printf("%" PRIu64 " ", current->data[0]);
        current = current->next;
    }
    printf("\n");
}

// Simple streaming eviction through the buffer
void stream_evict(vector<uint8_t>& buf, int passes) {
    volatile uint64_t sink = 0;
    size_t n = buf.size() / LINE_SIZE;

    for (int pass = 0; pass < passes; ++pass) {
        for (size_t i = 0; i < n; ++i) {
            // Touch one 8-byte word in each cache line
            auto* p = reinterpret_cast<uint64_t*>(&buf[i * LINE_SIZE]);
            sink += *p;
            #ifdef DEBUG
                printf("%d\n", sink);
            #endif
        }
    }
}

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
    size_t node_count = 100;
    size_t npref = 0;
    size_t total_allocs = 5000;
    // bool do_alloc_nodes = true;
    unsigned int seed = (unsigned int)time(nullptr);
    const char* rand_index_filename;

    // New parameters
    size_t reuse_pct = 70;     // % of initial nodes expected to be reused
    // size_t spaciness = 0;      // noise allocations between nodes

    // Parse args
    for (int i = 1; i < argc; ++i) {
        string a = argv[i];
        if (a == "-s" || a == "--nodesize") { NODESIZE = strtoull(argv[++i], nullptr, 0); }
        else if (a == "-n" || a == "--nodes") { node_count = strtoull(argv[++i], nullptr, 0); }
        else if (a == "-np" || a == "--npref") { npref = strtoull(argv[++i], nullptr, 0); }
        else if (a == "-t" || a == "--traversal") { rand_index_filename = argv[++i]; } // FIX: Assign the argument string directly
        else if (a == "-a" || a == "--allocs") { total_allocs = strtoull(argv[++i], nullptr, 0); }
        // else if (a == "--allocnodes") { do_alloc_nodes = true; }
        else if (a == "--seed") { seed = (unsigned int)strtoul(argv[++i], nullptr, 0); }
        else if (a == "--reusepct") { reuse_pct = strtoull(argv[++i], nullptr, 0); }
        // else if (a == "--spaciness") { spaciness = strtoull(argv[++i], nullptr, 0); }
        else if (a == "-h" || a == "--help") {
            cout << "Usage: " << argv[0] << " -n <node_count> -s <NODESIZE> [options]\n"
                 << "Options:\n"
                 << "  -np, -npref <N>      Prefetch Distance\n"
                 << "  -t, --traversal <file> File containing traversal indices\n"
                 << "  -a, --allocs <N>     Total mallocs to perform (default 5000)\n"
                 << "  --allocnodes         After fragmenting, allocate node_count nodes\n"
                 << "  --seed <num>         RNG seed\n"
                 << "  --reusepct <N>       Target %% of initial nodes reused from freed chunks (default 70)\n";
                //  << "  --spaciness <N>      Noise allocs between freed nodes (default 0)\n";
            return 0;
        } else {
            cerr << "Unknown arg: " << a << "\n";
            return 1;
        }
    }

    rng_state = seed;
    if (total_allocs < node_count) total_allocs = node_count * 3;

    cout << "Node size: " << NODESIZE << ", Node count: " << node_count << "\n";
    cout << "Total mallocs: " << total_allocs << ", Seed: " << seed << "\n";
    // cout << "Target reuse: " << reuse_pct << "%, Spaciness: " << spaciness << "\n";

    #ifndef GRANULARITY
    #define GRANULARITY log2(NODESIZE) 
    #endif

    mapSize = NODESIZE / (1 << (int)GRANULARITY) + 1;

    if (rand_index_filename == nullptr) {
        cerr << "Error: Traversal file not specified. Use -t or --traversal.\n";
        return 1;
    }

    vector<int> randIndex_vec; // Use std::vector for dynamic sizing
    ifstream rand_index_file(rand_index_filename);
    if (!rand_index_file.is_open()) {
        cerr << "Error: Could not open random index file " << rand_index_filename << endl;
        return 1;
    }
    int r_val;
    while (rand_index_file >> r_val) {
        randIndex_vec.push_back(r_val);
    }
    rand_index_file.close();

    #ifdef DEBUG
        // printf("NODESIZE: %d, nodesize: %d, GRANULARITY: %d, mapSize: %" PRIu32 "\n", NODESIZE, nodesize, (int)GRANULARITY, mapSize);
        printf("NODESIZE: %d, GRANULARITY: %d, mapSize: %" PRIu32 "\n", NODESIZE, (int)GRANULARITY, mapSize);
    #endif

    int ntravers = randIndex_vec.size();

    volatile Node *nthElement;

    #ifdef DEBUG
        printf("node_count: %d, npref: %d, ntravers: %d\n", node_count, npref, ntravers);
    #endif


    #ifndef NOATOM
        unsigned char* MMTptr = atom_init((int)GRANULARITY, false);
    #else
        unsigned char* MMTptr = atom_init((int)GRANULARITY, true);
        asm volatile("" :: "r"(MMTptr) : "memory");
    #endif

    vector<Req> reqs;
    reqs.reserve(total_allocs);

    // Step 1: create controlled bin-aligned blocks (as Req entries)
    size_t reuse_target = (node_count*2) * reuse_pct / 100;
    for (size_t i = 0; i < reuse_target; ++i) {
        size_t s;
        // Eligible for reuse: same bin as NODESIZE
        s = rand_range(NODESIZE, NODESIZE + 16);
        reqs.push_back({s, true}); // mark as initial
    }

    // size_t reuse_target = (node_count*2) * reuse_pct / 100;
    // for (size_t i = 0; i < (node_count*2); ++i) {
    //     size_t s;
    //     if (i < reuse_target) {
    //         // Eligible for reuse: same bin as NODESIZE
    //         s = rand_range(NODESIZE, NODESIZE + 16);
    //     } else {
    //         // Not reusable: deliberately mismatched (keep it outside target small bin)
    //         s = rand_range(NODESIZE * 2, NODESIZE * 4);
    //     }
    //     reqs.push_back({s, true}); // mark as initial
    // }

    // Step 2: fill rest with random sizes, but avoid accidentally adding
    // sizes in the small target bin [NODESIZE .. NODESIZE+16] to keep control over reuse.
    for (size_t i = reqs.size(); i < total_allocs; ++i) {
        unsigned int r = next_rand() % 100;
        size_t s;
        if (r < 60) {
            // small
            s = rand_range(1, (NODESIZE > 1) ? (NODESIZE - 1) : 1);
        } else if (r < 90) {
            // medium: choose a medium that avoids the target small bin
            size_t low = NODESIZE + 17;
            size_t high = NODESIZE * 2;
            if (high < low) high = low; // guard for small NODESIZE values
            s = rand_range(low, high);
        } else {
            // large
            s = rand_range(NODESIZE * 2, NODESIZE * 8);
        }
        reqs.push_back({s, false});
    }

    // Step 2.5: biased shuffle with distance-aware repulsion
    shuffle(reqs.begin(), reqs.end(), default_random_engine(rng_state));

    // if (spaciness > 0) {
    //     size_t n = reqs.size();
    //     int max_window = static_cast<int>(spaciness * 2); // how far to "look" around

    //     for (size_t i = 0; i < n; ++i) {
    //         if (!reqs[i].is_initial) continue;

    //         // check neighbors within window
    //         for (int d = 1; d <= max_window; ++d) {
    //             size_t j = i + d;
    //             if (j >= n) break;

    //             if (reqs[j].is_initial) {
    //                 // Closer => higher chance of repulsion
    //                 unsigned int roll = next_rand() % (10 * d);
    //                 if (roll < spaciness) {
    //                     // try to push this j-th initial one step further
    //                     if (j + 1 < n && !reqs[j + 1].is_initial) {
    //                         swap(reqs[j], reqs[j + 1]);
    //                     }
    //                 }
    //             }
    //         }
    //     }
    // }

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
        recs[i].was_big_enough = (reqs[i].size >= NODESIZE) && (reqs[i].size <= (NODESIZE + 16));
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
    //size_t to_free = min(node_count, bin_indices.size());
    size_t to_free = bin_indices.size();
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
    // if (do_alloc_nodes) {
    cout << "\nNow allocating " << node_count << " nodes of size "
            << NODESIZE << "\n";
    unordered_map<uintptr_t, size_t> freed_map;
    for (size_t i = 0; i < recs.size(); ++i) {
        if (recs[i].freed && recs[i].ptr)
            freed_map[(uintptr_t)recs[i].ptr] = i;
    }

    unsigned long cycles_alloc_start, cycles_alloc_end;
    unsigned long cycles_alloc_total = 0;
    size_t reused = 0;
    for (size_t i = 0; i < node_count; ++i) {

        asm volatile("rdcycle %0" : "=r" (cycles_alloc_start));
        
        struct Node* p = insert(i);

        if (p == nullptr) {
            cerr << "Error: insert failed for node " << i << "\n";
            return 1; // skip to next
        }

        #ifndef NOATOM
            if (i < (node_count - 1))  atom_map((void *) current_ptr, mapSize, i+1); // Map the current pointer to atom 5
            if ((i > npref) && (i < node_count + npref)) atom_define(i-npref, 0, (void *) current_ptr); // Define the atom with the current pointer
        #endif

        asm volatile("rdcycle %0" : "=r" (cycles_alloc_end));

        cycles_alloc_total += (cycles_alloc_end - cycles_alloc_start);

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
    // }

    #ifdef DEBUG
        printList();

        printListAddress();
    #endif

    unsigned long cycles_traversal1, cycles_traversal2;

    asm volatile("rdcycle %0" :"=r" (cycles_traversal1));

    for (int i=0; i<ntravers; i++)
    {        
            nthElement = getNthElement(randIndex_vec[i]);
            if(nthElement == nullptr) {
                cerr << "Error: Element at index " << randIndex_vec[i] << " not found." << endl;
                return 1; // Skip to the next iteration if the element is not found
            }
            #ifdef DEBUG
                printf("%d: %" PRIu64 "\n", randIndex_vec[i], nthElement->data);
            #endif
    } 
       
    asm volatile("rdcycle %0" :"=r" (cycles_traversal2));
    cycles_traversal2 -= cycles_traversal1;

    printf ("Cycles for allocation: %lu\n", cycles_alloc_total);
    printf ("Cycles for traversal: %lu\n", cycles_traversal2);

    deleteList();
    free(MMTptr);

    // Cleanup
    for (auto &r : recs) {
        if (r.ptr && !r.freed) free(r.ptr);
    }

    // Allocate buffer aligned to cache line size
    vector<uint8_t> buffer(BUFFER_BYTES);

    //Fill the buffer with a known pattern to ensure it's allocated
    memset(buffer.data(), 0, buffer.size());

    // Evict L1 + L2 by streaming over buffer
    stream_evict(buffer, PASSES);

    asm volatile("fence.i"); // Ensure instruction cache is also cleared

    return 0;
}
