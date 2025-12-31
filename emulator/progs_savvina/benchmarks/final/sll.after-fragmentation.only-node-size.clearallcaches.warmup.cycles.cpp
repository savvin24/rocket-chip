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
unsigned long long memfootprint = 0;

struct Node {
    struct Node* next;
    uint64_t data[];
};

Node* head_ptr = nullptr;
Node* current_ptr = nullptr;

uint32_t mapSize;

// Helper: print cache index bits
void print_addr_info(const char *label, void *ptr) {
    uintptr_t addr = (uintptr_t)ptr;
    unsigned line_offset = addr & 0x3F;            // lower 6 bits (64B line)
    unsigned index_bits  = (addr >> 6) & 0x3F;     // next 6 bits: enough to see conflicts
    uintptr_t page_num   = addr >> 12;             // 4KB page number

    printf("%s %p  page=%" PRIuPTR "  index=0x%x  line_off=0x%x\n",
           label, ptr, page_num, index_bits, line_offset);
}

struct Node* insert(uint64_t data) {
    struct Node* newNode = (struct Node*)malloc(NODESIZE);

    if (newNode == nullptr) {
        cerr << "Error: Memory allocation failed for newNode with data " << data << endl;
        return nullptr; // Indicate an error
    }

    for (size_t i = 0; i < (NODESIZE-sizeof(struct Node*))/sizeof(uint64_t); i++)
    {
        newNode->data[i] = data;
    }

    newNode->next = nullptr;

    if (head_ptr == nullptr) {
        head_ptr = newNode;
    } else {
        current_ptr->next = newNode;  
    }
    current_ptr = newNode;
    return newNode;
}

void deleteList() {
    Node* current = head_ptr;
    Node* next;

    while (current != nullptr) {
        next = current->next;
        // #ifndef NOATOM
        //     atom_unmap((void *) current, mapSize); // Unmap the current pointer from atom 0
        // #endif
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

void printListInfo() {
    Node* current = head_ptr;
    int count = 0;
    while (current != nullptr) {
        print_addr_info("NODE", current);
        // for (size_t i = 0; i < (NODESIZE-sizeof(Node*))/sizeof(uint64_t); i++)
        // {
        //     printf("current->data[%d] = %" PRIu64 "\n", i, current->data[i]);
        // }
        current = current->next;
        count++;
    }
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
            // #ifdef DEBUG
            //     printf("%d\n", sink);
            // #endif
        }
    }
}

int main(int argc, char** argv) {
    size_t node_count = 100;
    size_t npref = 0; // prefetch distance (e.g. npref = 0 -> associate node i with node i+1, npref = 1 -> associate node i with node i+2 etc)
    size_t total_allocs = 100000;

    const char* traversal_index_filename = nullptr;

    size_t reuse_pct = 70;     // % of initial nodes expected to be allocated in holes

    // Parse args
    for (int i = 1; i < argc; ++i) {
        string a = argv[i];
        if (a == "-s" || a == "--nodesize") { NODESIZE = strtoull(argv[++i], nullptr, 0); }
        else if (a == "-n" || a == "--nodes") { node_count = strtoull(argv[++i], nullptr, 0); }
        else if (a == "-np" || a == "--npref") { npref = strtoull(argv[++i], nullptr, 0); }
        else if (a == "-t" || a == "--traversal") { traversal_index_filename = argv[++i]; } 
        else if (a == "-a" || a == "--allocs") { total_allocs = strtoull(argv[++i], nullptr, 0); }
        else if (a == "--reusepct") { reuse_pct = strtoull(argv[++i], nullptr, 0); }
        else if (a == "-h" || a == "--help") {
            cout << "Usage: " << argv[0] << " -n <node_count> -s <NODESIZE> [options]\n"
                 << "Options:\n"
                 << "  -np, -npref <N>      Prefetch Distance\n"
                 << "  -t, --traversal <file> File containing traversal indices\n"
                 << "  -a, --allocs <N>     Total mallocs to perform (default 5000)\n"
                 << "  --allocnodes         After fragmenting, allocate node_count nodes\n"
                 << "  --reusepct <N>       Target %% of initial nodes reused from freed chunks (default 70)\n";
            return 0;
        } else {
            cerr << "Unknown arg: " << a << "\n";
            return 1;
        }
    }

    #ifndef GRANULARITY
    #define GRANULARITY log2(NODESIZE) 
    #endif

    mapSize = NODESIZE / (1 << (int)GRANULARITY) + 1;

    if (traversal_index_filename == nullptr) {
        cerr << "Error: Traversal file not specified. Use -t or --traversal.\n";
        return 1;
    }

    vector<int> traversalIndex_vec; // Use std::vector for dynamic sizing
    ifstream traversal_index_file(traversal_index_filename);
    if (!traversal_index_file.is_open()) {
        cerr << "Error: Could not open random index file " << traversal_index_filename << endl;
        return 1;
    }
    int t_val;
    while (traversal_index_file >> t_val) {
        traversalIndex_vec.push_back(t_val);
    }
    traversal_index_file.close();

    int ntravers = traversalIndex_vec.size();

    // Allocate tagID table in main memory (even at baseline)
    #ifndef NOATOM
        unsigned char* MMTptr = atom_init((int)GRANULARITY, false);
    #else
        unsigned char* MMTptr = atom_init((int)GRANULARITY, true);
        // asm volatile("" :: "r"(MMTptr) : "memory");
    #endif

    unsigned long cycles_alloc_start, cycles_alloc_end;

    asm volatile("rdcycle %0" : "=r" (cycles_alloc_start));

    for (size_t i = 0; i < node_count; ++i) {
        struct Node* p = insert(i);

        if (p == nullptr) {
            cerr << "Error: insert failed for node " << i << "\n";
            return 1; 
        }

        #ifndef NOATOM
            if (i < (node_count - 1))  atom_map((void *) current_ptr, mapSize, i+1); // Map the current pointer to atom 5
            if ((i > npref) && (i < node_count + npref)) atom_define(i-npref, 0, (void *) current_ptr); // Define the atom with the current pointer
        #endif
    }

    asm volatile("rdcycle %0" : "=r" (cycles_alloc_end));

    cycles_alloc_end -= cycles_alloc_start;

    volatile Node *nthElement;

    nthElement = getNthElement(node_count - 1); // Warmup Run

    unsigned long cycles_traversal1, cycles_traversal2;

    asm volatile("rdcycle %0" :"=r" (cycles_traversal1));

    for (int i=0; i<ntravers; i++)
    {        
        nthElement = getNthElement(traversalIndex_vec[i]);
        if(nthElement == nullptr) {
            cerr << "Error: Element at index " << traversalIndex_vec[i] << " not found." << endl;
            return 1;
        }
    } 
       
    asm volatile("rdcycle %0" :"=r" (cycles_traversal2));
    cycles_traversal2 -= cycles_traversal1;

    printf ("Cycles for allocation: %lu\n", cycles_alloc_end);
    printf ("Cycles for traversal: %lu\n", cycles_traversal2);

    #ifdef DEBUG
        printf("N. of nodes %d, NODESIZE: %d, GRANULARITY: %d, mapSize: %" PRIu32 ", prefetch distance %d, N. of mallocs: %d, Percentage of nodes that are allocated in holes: %d\n", node_count, NODESIZE, (int)GRANULARITY, mapSize, npref, total_allocs, reuse_pct); 
        printf("Total memory allocated by mallocs: %llu bytes\n", memfootprint);
        malloc_info(0, stdout);
        for (int i=0; i<ntravers; i++)
        {
            printf("traversalIndex_vec[%d] = %d\n", i, traversalIndex_vec[i]);
        }
        printListInfo();
    #endif

    free(MMTptr);
    deleteList();

    // Allocate buffer aligned to cache line size
    vector<uint8_t> buffer(BUFFER_BYTES);

    //Fill the buffer with a known pattern to ensure it's allocated
    memset(buffer.data(), 0, buffer.size());

    // Evict L1 + L2 by streaming over buffer
    stream_evict(buffer, PASSES);

    asm volatile("fence.i"); // Ensure instruction cache is also cleared

    return 0;
}
