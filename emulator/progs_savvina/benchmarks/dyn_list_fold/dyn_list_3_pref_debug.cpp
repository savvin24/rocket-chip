/* A small testbench to check the functionality of fatom instructions for only one node */

#include <iostream>
#include <cstdlib>
#include <time.h>
#include "crosslayer.h"
#include <inttypes.h>

using namespace std;

#ifndef GRANULARITY
#define GRANULARITY 4
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

Node* getNthElement(Node* head, int n) {
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

    uint64_t **arr = (uint64_t **) malloc(3*sizeof(uint64_t*));
    int size;
    void ** attributes = (void **) malloc(3 * sizeof(void*));

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
    uint64_t data = 0;

    insert(data);  

    #ifndef NOATOM 
        atom_map((void*) head, sizeof(Node)/(1<<GRANULARITY)+1, 100);
    #endif

    for (int i=0; i<3; i++)
    {
        size = rand() % ((80-64) + 1) + 64; // Random size between 64 (512 B = 8 cache lines) and 80 (640 B = 10 cache lines) including 64 and 80)

        #ifdef DEBUG
            printf("Size: %d\n", size);
        #endif

        arr[i] = (uint64_t*) malloc(size * sizeof(uint64_t));
        dummy_ptr = arr[i];

        data++;
        insert(data);
        attributes[i] = (void*) current_ptr;
    }

    atom_define_bulk(100, attributes, 3);

    #ifdef DEBUG
        printList(head);

        printListAddress(head);
    #endif
   
    Node* n0 = getNthElement(head, 0);

    #ifdef DEBUG
        if (n0) printf("0: %" PRIu64 "\n", n0->data);
    #endif

    free(arr);
    
    return 0;
}
