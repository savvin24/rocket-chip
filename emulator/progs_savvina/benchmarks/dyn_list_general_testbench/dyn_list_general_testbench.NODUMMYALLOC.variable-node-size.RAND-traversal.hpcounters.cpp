/* Differences to dyn_list_general_testbench.cycles */
/* Introduces mapSize */
/* TagIDs are assigned starting from 1, not 0. tagID=0 is considered invalid */
/* Have configurable node size (take it as input) */
/* If GRANULARITY was not given a value by Makefile, it will take the value of NODESIZE */
/* Added error statements for all mallocs */
#include <iostream>
#include <cstdlib>
#include <time.h>
#include <cmath>
#include "crosslayer.h"
#include <inttypes.h>
#include "HPC.h"
#include <fstream> // Required for file operations
#include <vector>  // Using std::vector for dynamic arrays


using namespace std;

static int NODESIZE;

struct Node {
    struct Node* next;
    uint64_t data[];
};

Node* head = nullptr;
Node* current_ptr = nullptr;

uint32_t mapSize;

void insert(uint64_t data) {
    struct Node* newNode = (struct Node*)malloc(NODESIZE);

    if (newNode == nullptr) {
        cerr << "Error: Memory allocation failed for newNode with data " << data << endl;
        return; // Indicate an error
    }

    for (size_t i = 0; i < (NODESIZE-8)/sizeof(uint64_t); i++)
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
        printf("%" PRIu64 " ", current->data[0]);
        current = current->next;
    }
    printf("\n");
}


int main(int argc, char *argv[]) { // Modified main function signature

    // Check if all required arguments are provided
    if (argc < 5) {
        cerr << "Usage: " << argv[0] << " <NODESIZE> <nnodes_value> <npref_value> <rand_index_file.txt>" << endl;
        return 1; // Indicate an error
    }

    NODESIZE = atoi(argv[1]); // given in bytes
    int nnodes = atoi(argv[2]);
    int npref = atoi(argv[3]); // Convert the argument string to an integer
    const char* rand_index_filename = argv[4];

    // int nodesize;

    // if (NODESIZE < 32) nodesize = 32; // Minimum node size is 32 bytes
    // else nodesize = NODESIZE;

    #ifndef GRANULARITY
    // #define GRANULARITY log2(nodesize)
    #define GRANULARITY log2(NODESIZE) 
    #endif

    if (NODESIZE % (1 << (int)GRANULARITY) == 0) {
        mapSize = NODESIZE / (1 << (int)GRANULARITY);
    } else {
        mapSize = NODESIZE / (1 << (int)GRANULARITY) + 1;
    }

    #ifdef DEBUG
        // printf("NODESIZE: %d, nodesize: %d, GRANULARITY: %d, mapSize: %" PRIu32 "\n", NODESIZE, nodesize, (int)GRANULARITY, mapSize);
        printf("NODESIZE: %d, GRANULARITY: %d, mapSize: %" PRIu32 "\n", NODESIZE, (int)GRANULARITY, mapSize);
    #endif

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

    int ntravers = randIndex_vec.size(); // Number of traversals is now determined by the file

    if(ntravers != nnodes * 1.7) {
        cerr << "Error: Size of randIndex_vec does not match expected ntravers." << endl;
        return 1; // Indicate an error
    }

    volatile Node *nthElement;

    #ifdef DEBUG
        printf("nnodes: %d, npref: %d, ntravers: %d\n", nnodes, npref, ntravers);
    #endif


    #ifdef NOATOM
        atom_init((int)GRANULARITY, true);
    #else
        atom_init((int)GRANULARITY, false);
    #endif

    HPC perfMon1;
    perfMon1.startMeasurement();

    for (int i=0; i<nnodes; i++)
    {
        insert(i);
        #ifndef NOATOM
        if (i < 255)  atom_map((void *) current_ptr, mapSize, i+1); // Map the current pointer to atom 5
        if ((i > npref) && (i < 256 + npref)) atom_define(i-npref, 0, (void *) current_ptr); // Define the atom with the current pointer
        #endif
    }

    perfMon1.endMeasurement();

    #ifdef DEBUG
        printList(head);

        printListAddress(head);
    #endif

    HPC perfMon2;
    perfMon2.startMeasurement();

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

    perfMon2.endMeasurement();

    perfMon1.printStats();
    perfMon2.printStats();

    deleteList(head);

    // Added for cache cleanup
    uint64_t * buffer = (uint64_t *) malloc(20000 * sizeof(uint64_t)); // Allocate a buffer for 20000 elements

    if(buffer == nullptr) {
        cerr << "Error: Memory allocation failed for buffer." << endl;
        return 1; // Indicate an error
    }

    for (int i = 0; i < 20000; i++)  // Initialize buffer to zero
    {
        buffer[i] = 0;
    }

    //asm volatile("CFLUSH.D.L1"); // Ensure all memory operations are complete before freeing
    
    return 0;
}