/* A small testbench to check the functionality of fatom instructions for only one node */

#include <iostream>
#include <cstdlib>
#include <time.h>
#include "crosslayer.h"
#include <inttypes.h>
#include "encoding.h"
#include <fstream> // Required for file operations
#include <vector>  // Using std::vector for dynamic arrays


using namespace std;

#ifndef GRANULARITY
#define GRANULARITY 9 // 16 Bytes = Node size
#endif

struct Node {
    uint64_t data;
    Node* next;
};

Node* head = nullptr;
Node* current_ptr = nullptr;
//volatile uint64_t* dummy_ptr = nullptr;
uint64_t* dummy_ptr = nullptr;

void insert(uint64_t data) {
    Node* newNode = new Node();
    newNode->data = data;
    newNode->next = nullptr;

    if (head == nullptr) {
        head = newNode;
    } else {
        current_ptr->next = newNode;  // TODO: Change upate orginal code
    }
    current_ptr = newNode;
    #ifdef DEBUG
    printf("%p\n", current_ptr);
    #endif
}

void deleteList(Node* head) {
    Node* current = head;
    Node* next;

    while (current != nullptr) {
        next = current->next;
        delete current;
        current = next;
    }
}

Node* getNthElement(int n) {
    Node* current = head;
    int count = 0;

    while (current != nullptr) {
        if (count == n) {
            return current;
        }
        count++;
        current = current->next;
    }
    return nullptr;
}

void printListAddress(Node* head) {
    Node* current = head;
    while (current != nullptr) {
        printf("%p ", (void*)current);
        current = current->next;
    }
    printf("\n");
}

// funstion to print the list
void printList(Node* head) {
    Node* current = head;
    while (current != nullptr) {
        printf("%" PRIu64 " ", current->data);
        current = current->next;
    }
    printf("\n");
}


int main(int argc, char *argv[]) { // Modified main function signature

    // Check if all required arguments are provided
    if (argc < 5) {
        cerr << "Usage: " << argv[0] << " <nnodes_value> <npref_value> <size_file.txt> <rand_index_file.txt>" << endl;
        return 1; // Indicate an error
    }

    int nnodes = atoi(argv[1]);
    int npref = atoi(argv[2]); // Convert the argument string to an integer
    const char* size_filename = argv[3];
    const char* rand_index_filename = argv[4];

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

    if(ntravers != nnodes * 1.7) {
        cerr << "Error: Size of randIndex_vec does not match expected ntravers." << endl;
        return 1; // Indicate an error
    }

    volatile Node *nthElement;

    #ifdef DEBUG
        printf("nnodes: %d, npref: %d, ndummyallocs: %d, ntravers: %d\n", nnodes, npref, ndummyallocs, ntravers);
    #endif


    #ifdef NOATOM
        atom_init(GRANULARITY, true);
    #else
        atom_init(GRANULARITY, false);
    #endif

    // Allocate arr based on the actual size_vec
    //volatile uint64_t **arr = (volatile uint64_t **) malloc(ndummyallocs * sizeof(uint64_t*));
    uint64_t **arr = (uint64_t **) malloc(ndummyallocs * sizeof(uint64_t*));

    unsigned long cycles_alloc1, cycles_alloc2, cycles_traversal1, cycles_traversal2;

    asm volatile("rdcycle %0" :"=r" (cycles_alloc1));

    for (int i=0; i<nnodes; i++)
    {
        insert(i);
        #ifndef NOATOM
        if (i < 256)  atom_map((void *) current_ptr, sizeof(Node)/(1<<GRANULARITY)+1, i); // Map the current pointer to atom 5
        if ((i > npref) && (i < 257 + npref)) atom_define(i-(npref + 1), 0, (void *) current_ptr); // Define the atom with the current pointer
        #endif

        // Check if i is within the bounds of size_vec for allocation
        if (i < ndummyallocs)
        {
            //arr[i] = (volatile uint64_t*) malloc(size_vec[i] * sizeof(uint64_t));
            arr[i] = (uint64_t*) malloc(size_vec[i] * sizeof(uint64_t));
            dummy_ptr = arr[i]; // Store the pointer to the last allocated array
            if (arr[i] == nullptr) {
                cerr << "Error: Memory allocation failed for arr[" << i << "]." << endl;
                return 1;
            }

            #ifdef DEBUG
                printf("Dummy allocation of address:%p and size: %d\n", arr[i], size_vec[i]);
            #endif
        }
    }

    asm volatile("rdcycle %0" :"=r" (cycles_alloc2));

    cycles_alloc2 -= cycles_alloc1;

    #ifdef DEBUG
        printList(head);

        printListAddress(head);
    #endif

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

    printf ("Cycles for allocation: %lu\n", cycles_alloc2);
    printf ("Cycles for traversal: %lu\n", cycles_traversal2);

    deleteList(head);

    // Free all allocated sub-arrays in arr
    for (size_t i = 0; i < ndummyallocs; ++i) {
        free((void *)arr[i]);
    }

    // Added for cache cleanup
    uint64_t * buffer = (uint64_t *) malloc(20000 * sizeof(uint64_t)); // Allocate a buffer for 20000 elements

    for (int i = 0; i < 20000; i++)  // Initialize buffer to zero
    {
        buffer[i] = 0;
    }

    //asm volatile("CFLUSH.D.L1"); // Ensure all memory operations are complete before freeing
    
    return 0;
}