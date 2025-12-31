/* A small testbench to check the functionality of fatom instructions for only one node */

#include <iostream>
#include <cstdlib>
#include <time.h>
#include "crosslayer.h"
#include <inttypes.h>
#include "encoding.h"

using namespace std;

#ifndef GRANULARITY
#define GRANULARITY 4 // 16 Bytes = Node size
#endif

struct Node {
    uint64_t data;
    Node* next;
};

Node* head = nullptr;
Node* current_ptr = nullptr;

void insert(uint64_t data) {
    Node* newNode = new Node();
    newNode->data = data;
    newNode->next = nullptr;

    if (head == nullptr) {
        head = newNode;
        current_ptr = newNode;
    } else {
        current_ptr->next = newNode;
        current_ptr = newNode;
    }
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

    // Check if npref argument is provided
    if (argc < 5) {
        cerr << "Usage: " << argv[0] << " <npref_value>" << endl;
        return 1; // Indicate an error
    }

    int nnodes = atoi(argv[1]); 
    int npref = atoi(argv[2]); // Convert the argument string to an integer

    int ntravers = nnodes * 1.7; // Number of traversals

    int size[nnodes-1];
    int randIndex[ntravers];

    for (int i=0; i<nnodes-1; i++)
    {
        size[i] = ;
    }

    for (int i=0; i<ntravers; i++)
    {
        randIndex[i] = ;
    } 

    Node *nthElement;

    #ifdef DEBUG
        printf("Size of Node in Bytes %zu\n", sizeof(Node));
        printf("Size of next pointer of Node in Bytes %zu\n", sizeof(Node::next));
    #endif

    #ifdef NOATOM
        atom_init(GRANULARITY, true);
    #else
        atom_init(GRANULARITY, false);
    #endif

    volatile uint64_t **arr = (uint64_t **) malloc((nnodes -1) * sizeof(uint64_t*)); // TODO: Try volatile

    unsigned long cycles_alloc1, cycles_alloc2, cycles_traversal1, cycles_traversal2;

    asm volatile("rdcycle %0" :"=r" (cycles_alloc1));

    for (int i=0; i<nnodes; i++)
    {
        insert(i);
        #ifndef NOATOM
        if (i < 256)  atom_map((void *) current_ptr, sizeof(Node)/(1<<GRANULARITY)+1, i); // Map the current pointer to atom 5
        if ((i > npref) && (i < 257 + npref)) atom_define(i-(npref + 1), 0, (void *) current_ptr); // Define the atom with the current pointer
        #endif

        if (i < nnodes -1)
        {
            arr[i] = (uint64_t*) malloc(size[i] * sizeof(uint64_t));

            #ifdef DEBUG
                printf("Dummy allocation of size: %d\n", size);
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
        nthElement = getNthElement(randIndex[i]);
        #ifdef DEBUG
            if (nthElement) printf("%d: %" PRIu64 "\n", randIndex, nthElement->data);
            else printf("Element at index %d not found.\n", randIndex);
        #endif
    }

    asm volatile("rdcycle %0" :"=r" (cycles_traversal2));
    cycles_traversal2 -= cycles_traversal1;

    printf ("Cycles for allocation: %lu\n", cycles_alloc2);
    printf ("Cycles for traversal: %lu\n", cycles_traversal2);
    
    deleteList(head);

    free(arr);


    
    return 0;
}
