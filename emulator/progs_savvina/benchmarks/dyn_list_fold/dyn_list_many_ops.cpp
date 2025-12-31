#include <iostream>
#include <cstdint> // For uint64_t

// Define the Node structure
struct Node {
    uint64_t data;
    Node* next;

    // Constructor to easily create new nodes
    Node(uint64_t val) : data(val), next(nullptr) {}
};

// Function to insert a node at the beginning of the list
void insertAtBeginning(Node*& head, uint64_t data) {
    Node* newNode = new Node(data);
    newNode->next = head;
    head = newNode;
    std::cout << "Inserted " << data << " at the beginning." << std::endl;
}

// Function to insert a node after a specific node (by data value)
void insertAfter(Node* head, uint64_t searchData, uint64_t newData) {
    Node* current = head;
    while (current != nullptr && current->data != searchData) {
        current = current->next;
    }

    if (current == nullptr) {
        std::cout << "Node with data " << searchData << " not found. Cannot insert after." << std::endl;
        return;
    }

    Node* newNode = new Node(newData);
    newNode->next = current->next;
    current->next = newNode;
    std::cout << "Inserted " << newData << " after " << searchData << "." << std::endl;
}

// Function to insert a node at the end of the list
void insertAtEnd(Node*& head, uint64_t data) {
    Node* newNode = new Node(data);
    if (head == nullptr) {
        head = newNode;
        #ifdef DEBUG
            std::cout << "Inserted " << data << " at the end (list was empty)." << std::endl;
        #endif
        return;
    }
    Node* current = head;
    while (current->next != nullptr) {
        current = current->next;
    }
    current->next = newNode;
    #ifdef DEBUG
        std::cout << "Inserted " << data << " at the end." << std::endl;
}

// Function to delete a node by its data value
void deleteNode(Node*& head, uint64_t dataToDelete) {
    if (head == nullptr) {
        std::cout << "List is empty. Cannot delete." << std::endl;
        return;
    }

    // If the head node needs to be deleted
    if (head->data == dataToDelete) {
        Node* temp = head;
        head = head->next;
        delete temp; // Deallocate the memory
        std::cout << "Deleted " << dataToDelete << " from the beginning." << std::endl;
        return;
    }

    Node* current = head;
    Node* prev = nullptr;

    while (current != nullptr && current->data != dataToDelete) {
        prev = current;
        current = current->next;
    }

    if (current == nullptr) {
        std::cout << "Node with data " << dataToDelete << " not found. Cannot delete." << std::endl;
        return;
    }

    // Node found, delete it
    prev->next = current->next;
    delete current; // Deallocate the memory
    std::cout << "Deleted " << dataToDelete << "." << std::endl;
}

// Function to traverse and print the list
void traverseList(Node* head) {
    if (head == nullptr) {
        std::cout << "List is empty." << std::endl;
        return;
    }
    std::cout << "Current list: ";
    Node* current = head;
    while (current != nullptr) {
        std::cout << current->data << " -> ";
        current = current->next;
    }
    std::cout << "nullptr" << std::endl;
}

// Function to free all allocated memory (important to prevent memory leaks)
void clearList(Node*& head) {
    Node* current = head;
    Node* nextNode = nullptr;
    while (current != nullptr) {
        nextNode = current->next;
        delete current;
        current = nextNode;
    }
    head = nullptr; // Set head to nullptr after freeing all nodes
    std::cout << "List cleared and memory freed." << std::endl;
}

int main() {
    Node* head = nullptr; // Initialize an empty list

    // 1. Dynamically allocate nodes and connect them
    std::cout << "--- Initializing List ---" << std::endl;
    insertAtEnd(head, 10);
    insertAtEnd(head, 20);
    insertAtEnd(head, 30);
    traverseList(head); // Should show: 10 -> 20 -> 30 -> nullptr

    // 2. Insert nodes in the inside of the list
    std::cout << "\n--- Inserting Nodes ---" << std::endl;
    insertAtBeginning(head, 5); // Insert at the beginning
    traverseList(head); // Should show: 5 -> 10 -> 20 -> 30 -> nullptr

    insertAfter(head, 20, 25); // Insert 25 after 20
    traverseList(head); // Should show: 5 -> 10 -> 20 -> 25 -> 30 -> nullptr

    insertAfter(head, 5, 7); // Insert 7 after 5
    traverseList(head); // Should show: 5 -> 7 -> 10 -> 20 -> 25 -> 30 -> nullptr

    // 3. Delete/deallocate nodes from within the list
    std::cout << "\n--- Deleting Nodes ---" << std::endl;
    deleteNode(head, 10); // Delete node with data 10
    traverseList(head); // Should show: 5 -> 7 -> 20 -> 25 -> 30 -> nullptr

    deleteNode(head, 5); // Delete the new head node
    traverseList(head); // Should show: 7 -> 20 -> 25 -> 30 -> nullptr

    deleteNode(head, 30); // Delete the last node
    traverseList(head); // Should show: 7 -> 20 -> 25 -> nullptr

    deleteNode(head, 100); // Try to delete a non-existent node
    traverseList(head);

    // Final traversal to read data
    std::cout << "\n--- Final List Traversal ---" << std::endl;
    traverseList(head);

    // Clean up: free all dynamically allocated memory
    clearList(head);
    traverseList(head); // Should show: List is empty.

    return 0;
}