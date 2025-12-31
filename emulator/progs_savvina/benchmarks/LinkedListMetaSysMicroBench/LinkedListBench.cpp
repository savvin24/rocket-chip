/* LinkedListBench.cpp Functionality */
/* Gets as input the size of a node in Bytes and from that it determines the size of the linked list */
/* Creates head of the list and gives it "head" as data */
/* Creates a new node, populates its data with an integer-sequentially incremented, starting from 1, and inserts it after an existing node, selected randomly,
until the list reaches the expected size */
/* The resulting list is expected to have ids from 1 to maxNumNodes-1, randomly located, and the addresses of nodes will generally not be contiguous */
/* List is traversed once */
/* 5000 indices from 0 to maxNumNodes-1 are randomly selected */
/* For each indice, the benchmark finds where the node having this indice as data is in the list (its address) and inserts a new node after it 
(with data starting from maxNumNodes and reaching up to maxNumNodes + 4999)*/

#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>
#include <time.h>
#include "crosslayer.h"

static int NODESIZE;
static int mapSize;
//#define STRUCTSIZE (1 << 25)
#define STRUCTSIZE (1 << 19)
uint16_t tagID = 1;

#ifndef GRANULARITY
#define GRANULARITY 9
#endif

struct Node
{
  struct Node *next;
  uint32_t data[];
};

void traverse(struct Node *n)
{
  #ifdef DEBUG
    printf("In traverse: ");
  #endif   
  while (n != NULL)
  {
    #ifdef DEBUG
      printf("address %p data %" PRIu32 ", ", n, n->data[0]);
    #endif
    n = n->next;  
  }
  #ifdef DEBUG
    printf("\n");
  #endif
}

void insert(struct Node *insertionpt, const uint32_t id)
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
    printf("curr address %p, id %" PRIu32 "\n", cur, id);
  #endif  

  cur->next = NULL;
  for (size_t i = 0; i < (NODESIZE-8)/sizeof(uint32_t); i++)
    cur->data[i] = id;

  #ifndef NOATOM
    if (tagID < 256)
      atom_map((void*)cur, NODESIZE/(1 << GRANULARITY) + 1, tagID++);
  #endif
  
  if(insertionpt->next!=NULL)
  {
    struct Node *next = insertionpt->next;
    cur->next = next;
    #ifndef NOATOM
      atom_define_lookup((void *)cur, 0, (void *)next);
    #endif
  }
  insertionpt->next = cur;
  #ifndef NOATOM
    atom_define_lookup((void *)insertionpt, 0, (void *)cur);
  #endif
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

struct Node * findNodeBench(struct Node *head, uint32_t id)
{
  #ifdef DEBUG
    printf("In findNodeBench:\n");
  #endif
  while(head->next && head->data[0] != id)
    head = head->next;
  return head;
}

void doTest(struct Node *head, int size, int indices[])
{
  // Do 5K insertions
  // Create & map atom to newly created linked list
  for(int i = 0 ; i < 5000 ; i++)
  {
    #ifdef DEBUG
      printf("In doTest: Just before findNodeBench(head, (indices[%d]=)%d\n)", i, indices[i]);
    #endif
    Node* interm = findNodeBench(head, indices[i]);
    insert(interm, size);
    size++;
  }
}

int main(int argc, char *argv[])
{
  if (argc < 2) {
    fprintf(stderr, "Usage: %s <node_size_in_bytes>\n", argv[0]);
    return 1; // Indicate an error
  }

  NODESIZE = atoi(argv[1]); // given in bytes
  long int maxNumNodes = STRUCTSIZE / NODESIZE; 

  struct Node* head = NULL;
  long int numNodes = 0;

  #ifdef DEBUG
    printf("In main(): NODESIZE %d bytes, maxNumNodes %ld\n", NODESIZE, maxNumNodes);
  #endif
  
  #ifdef NOATOM  
    atom_init(GRANULARITY, true);
  #else
    atom_init(GRANULARITY, false);
  #endif

  head = (struct Node*)malloc(NODESIZE);

  if (head == NULL) {
    fprintf(stderr, "Error: Memory allocation failed for head node.\n");
    return 1; // Indicate an error
  }

  memcpy(head->data, "head", NODESIZE-8);
  head->next = NULL;
  
  numNodes++;

  #ifndef NOATOM
    atom_map((void*)head, NODESIZE/(1 << GRANULARITY) + 1, tagID++);
  #endif

  // #ifdef DEBUG
  //   printf("In main(): head address: %p\n", head);
  // #endif
  traverse(head);

  //construct linked list
  srand(1337);
  while(numNodes < maxNumNodes){
    int a = rand();
    int r = a % numNodes;

    #ifdef DEBUG
      printf("In main(): Inserting after %dth node\n", r);
    #endif

    struct Node *ipoint = findNode(head, r);
    insert(ipoint, numNodes);
    numNodes++;
    if(numNodes % 100 == 0)
    {
      #ifdef DEBUG
        printf("In main(): construction heartbeat: %li\n",numNodes);
      #endif
    }
  }

  #ifdef DEBUG
   printf("In main(): Prepare test indices\n");
  #endif
  int testIndices[5000]; 
  for (int i = 0 ; i < 5000 ; i++)
    testIndices[i] = rand() % numNodes;

  #ifdef DEBUG
    printf("In main(): Warmup run\n");
  #endif
  traverse(head);

  #ifdef DEBUG
    printf("In main(): Just before doTest(head, (numNodes=)%ld, testIndices)\n", numNodes);
  #endif

  doTest(head, numNodes, testIndices);

  #ifdef DEBUG
    printf("In main(): End test\n");
  #endif

  return 0;
}
