/* Based on dyn_list_general_testbench/dyn_list_general_testbench.variable-node-size.RAND-traversal.clearallcaches.cycles.cpp */
#include <iostream>
#include <vector>  // Using std::vector for dynamic arrays
#include <cstdint>
#include <cstring>
#include <cstdlib>
#include <unistd.h>
#include <sched.h>
#include <time.h>
#include <cmath>
#include "crosslayer.h"
#include <inttypes.h>
#include <fstream> // Required for file operations

using namespace std;

// constexpr size_t LINE_SIZE      = 64;        // bytes per cache line
// constexpr size_t BUFFER_BYTES   = 1 << 20;   // 1 MB buffer, > 512KB L2
// constexpr int PASSES            = 2;         // number of streaming passes

static int NODESIZE;

int ndata = 0;

struct Node {
    struct Node* next;
    uint64_t data[];
};

Node* head_ptr = nullptr;
Node* current_ptr = nullptr;
uint64_t* dummy_ptr = nullptr;

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

void insert(uint64_t data) {
    struct Node* newNode = (struct Node*)malloc(NODESIZE);

    if (newNode == nullptr) {
        cerr << "Error: Memory allocation failed for newNode with data " << data << endl;
        return; // Indicate an error
    }

    // #ifdef DEBUG
    //     printf("Before initialise data loop\n");
    // #endif

    for (size_t i = 0; i < ndata; i++)
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
}

void deleteList() {
    Node* current = head_ptr;
    Node* next;

    while (current != nullptr) {
        #ifndef NOATOM
            atom_unmap((void *) current, mapSize); // Unmap the current pointer from atom 0
        #endif
        next = current->next;
        free(current);
        // #ifndef NOATOM
        //     atom_unmap((void *) current, mapSize); // Unmap the current pointer from atom 0
        // #endif
        current = next;
    }
}

Node* getNthElement(int n) {
    Node* current = head_ptr;

    while (current != nullptr && (current->data[ndata-1] != (uint64_t)n)) {
        current = current->next;
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
        printf("Node %d\n", count);
        current = current->next;
        count++;
    }
}

// // Simple streaming eviction through the buffer
// void stream_evict(vector<uint8_t>& buf, int passes) {
//     volatile uint64_t sink = 0;
//     size_t n = buf.size() / LINE_SIZE;

//     for (int pass = 0; pass < passes; ++pass) {
//         for (size_t i = 0; i < n; ++i) {
//             // Touch one 8-byte word in each cache line
//             auto* p = reinterpret_cast<uint64_t*>(&buf[i * LINE_SIZE]);
//             sink += *p;
//             // #ifdef DEBUG
//             //     printf("%d\n", sink);
//             // #endif
//         }
//     }
// }

int main(int argc, char *argv[]) { 

    // Check if all required arguments are provided
    if (argc < 6) {
        cerr << "Usage: " << argv[0] << " <NODESIZE> <nnodes_value> <npref_value> <size_file.txt> <rand_index_file.txt>" << endl;
        return 1; 
    }

    NODESIZE = atoi(argv[1]); // given in bytes
    int nnodes = atoi(argv[2]);
    int npref = atoi(argv[3]); 
    const char* size_filename = argv[4];
    const char* rand_index_filename = argv[5];

    ndata = (NODESIZE - sizeof(struct Node*)) / sizeof(uint64_t);

    #ifndef GRANULARITY
    #define GRANULARITY log2(NODESIZE) 
    #endif

    mapSize = NODESIZE / (1 << (uint32_t)GRANULARITY) + 1;

    // Read size array from file
    vector<int> size_vec; // Use std::vector for dynamic sizing
    ifstream size_file(size_filename);
    if (!size_file.is_open()) {
        cerr << "Error: Could not open size file " << size_filename << endl;
        return 1;
    }
    int s_val;
    while (size_file >> s_val) {
        size_vec.push_back(s_val);
    }
    size_file.close();

    // Read randIndex array from file
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

    int ndummyallocs = size_vec.size();
    int ntravers = randIndex_vec.size(); // Number of traversals is now determined by the file

    if(ndummyallocs != nnodes - 1) {
        cerr << "Error: Size of size_vec does not match nnodes - 1." << endl;
        return 1; // Indicate an error
    }

    volatile Node *nthElement;


    // ----------------------- SAVVINA VERSION OF ATOM_INIT ------------------------
    // #ifndef NOATOM
    //     unsigned char* MMTptr = atom_init((int)GRANULARITY, false);
    // #else
    //     unsigned char* MMTptr = atom_init((int)GRANULARITY, true);
    // #endif

    #ifndef NOATOM
        atom_init((uint32_t)GRANULARITY, 0);
    #else
        atom_init((uint32_t)GRANULARITY, 1);
    #endif

    // Allocate arr based on the actual size_vec
    // volatile uint64_t **arr = (volatile uint64_t **) malloc(ndummyallocs * sizeof(uint64_t*));
    uint64_t **arr = (uint64_t **) malloc(ndummyallocs * sizeof(uint64_t*));

    if (arr == nullptr) {
        cerr << "Error: Memory allocation failed for arr." << endl;
        return 1; // Indicate an error
    }

    unsigned long cycles_alloc1, cycles_alloc2, cycles_traversal1, cycles_traversal2;

    asm volatile("rdcycle %0" :"=r" (cycles_alloc1));

    for (int i=0; i<nnodes; i++)
    {
        // #ifdef DEBUG
        // printf("Before insert(%d)\n", i);
        // #endif

        insert(i);

        // #ifdef DEBUG
        // printf("After insert(%d)\n", i);
        // #endif

        #ifndef NOATOM
        if (i < (nnodes - 1))  atom_map((void *) current_ptr, mapSize, i+1); // Map the current pointer to atom 5
        if ((i > npref) && (i < nnodes + npref)) atom_define(i-npref, 0, (void *) current_ptr); // Define the atom with the current pointer
        #endif

        // #ifdef DEBUG
        //     printf("Before dummy alloc\n");
        // #endif

        // Check if i is within the bounds of size_vec for allocation
        if (i < ndummyallocs)
        {
            // #ifdef DEBUG
            //     printf("Before arr[%d] malloc\n", i);
            // #endif
            // arr[i] = (volatile uint64_t*) malloc(size_vec[i] * sizeof(uint64_t));

            arr[i] = (uint64_t*) malloc(size_vec[i] * sizeof(uint64_t));
            dummy_ptr = arr[i];
            // #ifdef DEBUG
            //     printf("After arr[%d] malloc\n", i);
            // #endif

            if (arr[i] == nullptr) {
                cerr << "Error: Memory allocation failed for arr[" << i << "]." << endl;
                return 1;
            }
        }
    }

    asm volatile("rdcycle %0" :"=r" (cycles_alloc2));

    cycles_alloc2 -= cycles_alloc1;

    asm volatile("rdcycle %0" :"=r" (cycles_traversal1));

    for (int i=0; i<ntravers; i++)
    {        
            nthElement = getNthElement(randIndex_vec[i]);
            if(nthElement == nullptr) {
                cerr << "Error: Element at index " << randIndex_vec[i] << " not found." << endl;
                return 1; 
            }
    } 
       
    asm volatile("rdcycle %0" :"=r" (cycles_traversal2));
    cycles_traversal2 -= cycles_traversal1;

    printf ("Cycles for allocation: %lu\n", cycles_alloc2);
    printf ("Cycles for traversal: %lu\n", cycles_traversal2);

    #ifdef DEBUG
        printf("N. of nodes %d, NODESIZE: %d, GRANULARITY: %d, mapSize: %" PRIu32 ", prefetch distance %d\n", nnodes, NODESIZE, (int)GRANULARITY, mapSize, npref);
        for (int i=0; i<ndummyallocs; i++)
        {
            printf("size_vec[%d] = %d\n", i, size_vec[i]);
        }
        for (int i=0; i<ntravers; i++)
        {
            printf("randIndex_vec[%d] = %d\n", i, randIndex_vec[i]);
        }
        printf("Start of arr %p\n", (void*)arr);
        for (int i=0; i<ndummyallocs; i++)
        {
            printf("arr[%d] = %p\n", i, arr[i]);
        }
        printListInfo();
    #endif

    // Free the dynamically allocated arrays
    if (arr != nullptr) {
        for (int i = 0; i < ndummyallocs; ++i) {
            if (arr[i] != nullptr) {
                free((void*)arr[i]);  // cast away volatile
            }
        }
        free((void*)arr);  // cast away volatile
    }

    #ifndef NOATOM
        uint64_t **flush_arr = (uint64_t **) malloc(8*sizeof(uint64_t*));
        for(int i=0; i<8; i++)
        {
            flush_arr[i] = (uint64_t*) malloc(sizeof(uint64_t));
            *(flush_arr[i]) = 0x0;
        }

        for(int i=0; i< 256; i++)
        {
            atom_define_bulk(i, (void **)flush_arr, 8); 
        }
    #endif

    deleteList();

    // ------------------------ SAVVINA VERSION OF ATOM_INIT CLEANUP ------------------------
    // free(MMTptr);

    //  // Allocate buffer aligned to cache line size
    // vector<uint8_t> buffer(BUFFER_BYTES);

    // //Fill the buffer with a known pattern to ensure it's allocated
    // memset(buffer.data(), 0, buffer.size());

    // // Evict L1 + L2 by streaming over buffer
    // stream_evict(buffer, PASSES);

    // asm volatile("fence.i"); // Ensure instruction cache is also cleared
    
    return 0;
}