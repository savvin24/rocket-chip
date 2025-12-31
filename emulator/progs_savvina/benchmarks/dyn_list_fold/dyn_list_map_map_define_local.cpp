/* A small testbench to check the functionality of fatom instructions for only one node */


#include <iostream>
#include <cstdlib>
#include <time.h>
#include "crosslayer.h"

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
uint64_t* dummy_ptr = nullptr;

//uint64_t* dummy_ptr = nullptr;

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
        cout << current << " ";
        current = current->next;
    }
    cout << endl;
}

// funstion to print the list
void printList(Node* head) {
    Node* current = head;
    while (current != nullptr) {
        cout << current->data << " ";
        current = current->next;
    }
    cout << endl;
}

// void deleteNode(int i)
// {
//     Node *current = head;

//     if (i == 1) {

//         #ifndef NOATOM
//             atom_unmap((void*) head, sizeof(Node)/(1<<GRANULARITY)+1);
//         #endif

//         head = current->next;
//         delete current;
//     } else {
//         for (int j = 0; j < i - 2; j++) {
//             current = current->next;
//         }
//         Node *next = current->next;

//         #ifndef NOATOM
//             atom_unmap((void*) next, sizeof(Node)/(1<<GRANULARITY)+1);
//         #endif

//         current->next = next->next;
//         delete next;
//     }
// }

int main() {

    #ifdef DEBUG
        cout << "Size of Node in Bytes" << sizeof(Node) << endl;
        cout << "Size of next pointer of Node in Bytes" << sizeof(Node::next) << endl;
    #endif

    atom_init(GRANULARITY, false);

    srand(time(0));
    uint64_t data = 0;

    insert(data);  

    atom_map((void*) head, sizeof(Node)/(1<<GRANULARITY)+1, 5);

   // int size = rand() % ((64-8) + 1) + 8; // Random size between 8 (64 B = 1 cache line) and 64 (512 B = 8 cache lines) including 8 and 64)

    int size = rand() % ((80-64) + 1) + 64; // Random size between 64 (512 B = 8 cache lines) and 80 (640 B = 10 cache lines) including 64 and 80)

    #ifdef DEBUG
        cout << "Size: " << size << endl;
    #endif

    uint64_t *arr = (uint64_t*) malloc(size*sizeof(uint64_t));

    dummy_ptr = arr;

    data++;
    insert(data);

    atom_map((void*) current_ptr, sizeof(Node)/(1<<GRANULARITY)+1, 6); 

    atom_define(5, 0, current_ptr); // Not sure how to declare current_ptr
    
    #ifdef DEBUG
        printList(head);

        printListAddress(head);
    #endif

    //deleteNode(2);

    Node* n0 = getNthElement(head, 0);
    //Node* n1 = getNthElement(head, 1); // I'm accessing the first two elements of the list, because they will be in different cache lines (because of the arr mallocs that are in between)

    #ifdef DEBUG
         if (n0) cout << "0: " << n0->data << endl;
    //     if (n1) cout << "1: " << n1->data << endl;
    #endif
    
    deleteList(head);

    free(arr);
    return 0;
}