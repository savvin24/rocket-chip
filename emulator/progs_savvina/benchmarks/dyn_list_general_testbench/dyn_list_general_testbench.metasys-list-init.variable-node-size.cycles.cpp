/* Differences to LinkedListBench.cpp */
#include <iostream>
#include <cstdlib>
#include <time.h>
#include <cmath>
#include "crosslayer.h"
#include <inttypes.h>
#include "encoding.h"
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

    #ifdef DEBUG
        printf("newNode start address %p\n", newNode);
    #endif

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

void insert_after(struct Node *insertionpt, uint64_t data) {
    struct Node* newNode = (struct Node*)malloc(NODESIZE);

    if (newNode == nullptr) {
        cerr << "Error: Memory allocation failed for newNode with data " << data << endl;
        return; // Indicate an error
    }

    #ifdef DEBUG
        printf("newNode start address %p\n", newNode);
    #endif

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

    if(insertionpt->next!=NULL)
    {
        struct Node *next = insertionpt->next;
        newNode->next = next;
    }
    insertionpt->next = newNode;
}

void insert_after_initial(struct Node *insertionpt, const uint64_t id) // SAVVINA COMMENT: Copied from LinkedListBench.cpp. Change "data" type from "uint32_t" to "uint64_t"
{
  #ifdef DEBUG
    printf("In insert: ");
  #endif
  struct Node *cur = (struct Node*)malloc(NODESIZE);

  if (cur == NULL) {
    fprintf(stderr, "Error: Memory allocation failed for cur.\n");
    return; // Indicate an error
  }

  #ifdef DEBUG
    printf("curr address %p, id %" PRIu64 "\n", cur, id);
  #endif  

  cur->next = NULL;
  for (size_t i = 0; i < (NODESIZE-8)/sizeof(uint64_t); i++)
    cur->data[i] = id;
  
  if(insertionpt->next!=NULL)
  {
    struct Node *next = insertionpt->next;
    cur->next = next;
  }
  insertionpt->next = cur;
}

struct Node * findNode(struct Node *head, int i)
{
  while(i>0 && head->next)
  {
    i--;
    head = head->next;
  }
  return head;
}

void deleteList(Node* head) {
    Node* current = head;
    Node* next;

    while (current != nullptr) {
        next = current->next;
        free(current);
        current = next;
    }
}

Node* getNthElement(int n) {
    Node* current = head;
    int count = 0;

    #ifdef DEBUG
        printf("In getNthElement for n = %d: current = ", n);
    #endif
    while (current != nullptr) {
        #ifdef DEBUG
            printf("%p ", current);
        #endif
        if (count == n) {
            #ifdef DEBUG
                printf("\n");
            #endif
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
    if (argc < 6) {
        cerr << "Usage: " << argv[0] << " <NODESIZE> <nnodes_value> <npref_value> <metasys_list_init_file.txt> <rand_index_file.txt>" << endl;
        return 1; // Indicate an error
    }

    NODESIZE = atoi(argv[1]); // given in bytes
    int nnodes = atoi(argv[2]);
    int npref = atoi(argv[3]); // Convert the argument string to an integer
    const char* metasys_list_init_filename = argv[4];
    const char* rand_index_filename = argv[5];

    #ifndef GRANULARITY
    #define GRANULARITY log2(NODESIZE) 
    #endif

    /* The if-else block that follows is theoretically more correct. However, in practice it doesn't take into account the extra
     * 16 bytes allocated for each node and therefore yields slowdowns.
     */
    // if (NODESIZE % (1 << (int)GRANULARITY) == 0) {
    //     mapSize = NODESIZE / (1 << (int)GRANULARITY);
    // } else {
    //     mapSize = NODESIZE / (1 << (int)GRANULARITY) + 1;
    // }

    mapSize = NODESIZE / (1 << (int)GRANULARITY) + 1;

    #ifdef DEBUG
        printf("NODESIZE: %d, GRANULARITY: %d, mapSize: %" PRIu32 "\n", NODESIZE, (int)GRANULARITY, mapSize);
        // printf("NODESIZE: %d, nodesize: %d, GRANULARITY: %d, mapSize: %" PRIu32 "\n", NODESIZE, nodesize, (int)GRANULARITY, mapSize);
    #endif

    // Read size array from file
    vector<int> metasys_list_init_vec; // Use std::vector for dynamic sizing
    ifstream metasys_list_init_file(metasys_list_init_filename);
    if (!metasys_list_init_file.is_open()) {
        cerr << "Error: Could not open size file " << metasys_list_init_filename << endl;
        return 1;
    }
    int s_val;
    while (metasys_list_init_file >> s_val) {
        metasys_list_init_vec.push_back(s_val);
    }
    metasys_list_init_file.close();

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

    int ninitindices = metasys_list_init_vec.size();
    int ntravers = randIndex_vec.size(); // Number of traversals is now determined by the file

    if(ninitindices != nnodes - 1) {
        cerr << "Error: Size of metasys_list_init_vec does not match nnodes - 1." << endl;
        return 1; // Indicate an error
    }

    if(ntravers != nnodes * 1.7) {
        cerr << "Error: Size of randIndex_vec does not match expected ntravers." << endl;
        return 1; // Indicate an error
    }

    volatile Node *nthElement;

    #ifdef DEBUG
        printf("nnodes: %d, npref: %d, ninitindices: %d, ntravers: %d\n", nnodes, npref, ninitindices, ntravers);
    #endif

    #ifdef NOATOM
        atom_init((int)GRANULARITY, true);
    #else
        atom_init((int)GRANULARITY, false);
    #endif

    unsigned long cycles_listcreate1, cycles_listcreate2, cycles_metasys_insns_1, cycles_metasys_insns_2, cycles_traversal1, cycles_traversal2;

    asm volatile("rdcycle %0" :"=r" (cycles_listcreate1));

    // head = (struct Node*)malloc(NODESIZE);

    // if (head == NULL) {
    //     fprintf(stderr, "Error: Memory allocation failed for head node.\n");
    //     return 1; // Indicate an error
    // }
    
    // for (size_t i = 0; i < (NODESIZE-8)/sizeof(uint64_t); i++)
    // {
    //     head->data[i] = 0;
    //     #ifdef DEBUG 
    //        printf("head->data[%d] = %" PRIu64 "\n", i, head->data[i]);
    //     #endif
    // }

    // head->next = nullptr;

    insert(0); // Insert the head node with data 0

    for (int i = 1; i < nnodes; i++)
    {
        struct Node* insertion_point = findNode(head, metasys_list_init_vec[i-1]);
        if (insertion_point == nullptr) {
            cerr << "Error: Insertion point not found for index " << metasys_list_init_vec[i-1] << endl;
            return 1; // Indicate an error
        }
        #ifdef DEBUG
            printf("metasys_list_init_vec[i-1]: %d, insertion_point: %p\n", metasys_list_init_vec[i-1], insertion_point);
        #endif
        insert_after(insertion_point, i);
    }

    asm volatile("rdcycle %0" :"=r" (cycles_listcreate2));

    cycles_listcreate2 -= cycles_listcreate1;

    asm volatile("rdcycle %0" :"=r" (cycles_metasys_insns_1));

    current_ptr = head;

    #ifndef NOATOM
    for (int i=0; i<nnodes; i++)
    {
        #ifdef DEBUG
           printf("Call metasys instructions for node at address %p\n", current_ptr);
        #endif
        if (i < 255)  atom_map((void *) current_ptr, mapSize, i+1); // Map the current pointer to atom 5
        if ((i > npref) && (i < 256 + npref)) atom_define(i-npref, 0, (void *) current_ptr); // Define the atom with the current pointer
        current_ptr = current_ptr->next;
    }
    #endif

    asm volatile("rdcycle %0" :"=r" (cycles_metasys_insns_2));
    cycles_metasys_insns_2 -= cycles_metasys_insns_1;

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

    printf ("Cycles for list creation: %lu\n", cycles_listcreate2);
    printf ("Cycles for metasys instructions: %lu\n", cycles_metasys_insns_2);
    printf ("Cycles for traversal: %lu\n", cycles_traversal2);

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