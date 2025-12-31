/* A small testbench to check the functionality of fatom instructions for only one node */

#include <iostream>
#include <cstdlib>
#include <time.h>
#include "crosslayer.h"
#include <inttypes.h>

using namespace std;

#ifndef GRANULARITY
#define GRANULARITY 9
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


int main() {

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

    uint64_t **arr = (uint64_t **) malloc(20000 * sizeof(uint64_t*));

    for (int i=0; i<20000; i++)
    {
        insert(i);
        #ifndef NOATOM
        if (i < 19999)  atom_map((void *) current_ptr, sizeof(Node)/(1<<GRANULARITY)+1, i); // Map the current pointer to atom 5
        if (i > 0) atom_define(i-1, 0, (void *) current_ptr); // Define the atom with the current pointer
        #endif
        size = rand() % ((80-64) + 1) + 64; // Random size between 64 (512 B = 8 cache lines) and 80 (640 B = 10 cache lines) including 64 and 80)
        arr[i] = (uint64_t*) malloc(size * sizeof(uint64_t));
        dummy_ptr = arr[i];
    }
    
    #ifdef DEBUG
        printList(head);

        printListAddress(head);
    #endif
   

    for (int i=0; i<40000; i++)
    {
        int randIndex = rand() % 20000; // Random index between 0 and 19999
        Node* nthElement = getNthElement(randIndex);
        #ifdef DEBUG
            if (nthElement) printf("%d: %" PRIu64 "\n", randIndex, nthElement->data);
            else printf("Element at index %d not found.\n", randIndex);
        #endif
    }
    
    deleteList(head);

    free(arr);
    
    return 0;
}
