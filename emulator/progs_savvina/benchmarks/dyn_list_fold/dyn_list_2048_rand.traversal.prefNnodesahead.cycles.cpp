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
uint64_t *dummy_ptr = nullptr;

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
    if (argc < 2) {
        cerr << "Usage: " << argv[0] << " <npref_value>" << endl;
        return 1; // Indicate an error
    }

    int npref = atoi(argv[1]); // Convert the argument string to an integer

    #ifdef DEBUG
        printf("Size of Node in Bytes %zu\n", sizeof(Node));
        printf("Size of next pointer of Node in Bytes %zu\n", sizeof(Node::next));
    #endif

    #ifdef NOATOM
        atom_init(GRANULARITY, true);
    #else
        atom_init(GRANULARITY, false);
    #endif

    srand(time(0));
    int size;

    uint64_t **arr = (uint64_t **) malloc(2048 * sizeof(uint64_t*));

    unsigned long cycles_alloc1, cycles_alloc2, cycles_traversal1, cycles_traversal2;

    asm volatile("rdcycle %0" :"=r" (cycles_alloc1));

    for (int i=0; i<3000; i++)
    {
        insert(i);
        #ifndef NOATOM
        if (i < 2048)  atom_map((void *) current_ptr, sizeof(Node)/(1<<GRANULARITY)+1, i); // Map the current pointer to atom 5
        if ((i > npref) && (i < 2049 + npref)) atom_define(i-(npref + 1), 0, (void *) current_ptr); // Define the atom with the current pointer
        #endif
        if (i < 2047)
        {
            size = rand() % ((80-64) + 1) + 64; // Random size between 64 (512 B = 8 cache lines) and 80 (640 B = 10 cache lines) including 64 and 80)
            arr[i] = (uint64_t*) malloc(size * sizeof(uint64_t));
            dummy_ptr = arr[i];
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

    for (int i=0; i<4000; i++)
    {
        int randIndex = rand() % 3000; // Random index between 0 and 3000
        Node* nthElement = getNthElement(randIndex);
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
