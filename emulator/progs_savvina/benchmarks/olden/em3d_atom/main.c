/* For copyright information, see olden_v1.0/COPYRIGHT */

#include "em3d.h"
#include "make_graph.h"
#include "crosslayer.h"
#include <string.h>
#ifndef GRANULARITY
#define GRANULARITY 6
#endif

extern int NumNodes;

uint32_t mapSize;

const size_t LINE_SIZE      = 64;        // bytes per cache line
const size_t BUFFER_BYTES   = 1 << 20;   // 1 MB buffer, > 512KB L2
const int PASSES            = 2;         // number of streaming passes

/* SAVVINA ADDITION */
void stream_evict(uint8_t* buf, int passes) {
    volatile uint64_t sink = 0;
    size_t n = BUFFER_BYTES / LINE_SIZE;

    for (int pass = 0; pass < passes; ++pass) {
        for (size_t i = 0; i < n; ++i) {
            // Touch one 8-byte word in each cache line
            uint64_t *p = (uint64_t*)(buf + i * LINE_SIZE);
            sink += *p;
            // #ifdef DEBUG
            //     printf("%d\n", sink);
            // #endif
        }
    }
}

/* SAVVINA ADDITION */
void print_addr_info(const char *label, void *ptr) {
    uintptr_t addr = (uintptr_t)ptr;
    unsigned line_offset = addr & 0x3F;            // lower 6 bits (64B line)
    unsigned index_bits  = (addr >> 6) & 0x3F;     // next 6 bits: enough to see conflicts
    uintptr_t page_num   = addr >> 12;             // 4KB page number

    printf("%s %p  page=%" PRIuPTR "  index=0x%x  line_off=0x%x\n",
           label, ptr, page_num, index_bits, line_offset);
}

/* SAVVINA ADDITION */
void deleteList(graph_t *graph, int id) {
  
  node_t *cur_node;
  cur_node=graph->e_nodes[id];

  node_t* next;
  int count = 1;

  while (cur_node != nullptr) {
      #ifndef NOATOM
          if((count++) < ntagIDs) atom_unmap((void *) cur_node, mapSize); // Unmap the current pointer from atom 0
      #endif
      next = cur_node->next;
      free(cur_node);
      cur_node = next;
  }

  cur_node=graph->h_nodes[id];
  count = 1;

  while (cur_node != nullptr) {
      #ifndef NOATOM
          if((count++) < ntagIDs) atom_unmap((void *) cur_node, mapSize); // Unmap the current pointer from atom 0
      #endif
      next = cur_node->next;
      free(cur_node);
      cur_node = next;
  }
}

void print_graph(graph_t *graph, int id) 
{
  node_t *cur_node;
  cur_node=graph->e_nodes[id];

  int count = 1;

  for(; cur_node; cur_node=cur_node->next)
  {
    print_addr_info("NODE", cur_node);
    // print_addr_info("NEXTPTR", cur_node->next);
    // print_addr_info("VALUEPTR", cur_node->value);
    chatting("E: node n. %d, value %f, from_count %d\n", count++, *cur_node->value,
        cur_node->from_count);
  }
  cur_node=graph->h_nodes[id];
  count = 1;
  for(; cur_node; cur_node=cur_node->next)
  {
    print_addr_info("NODE", cur_node);
    chatting("H: node n. %d, value %f, from_count %d\n", count++, *cur_node->value,
        cur_node->from_count);
  }
}

// extern int nonlocals; // SAVVINA comment: useless in this version

int main(int argc, char *argv[])
{
  int i;
  graph_t *graph;

  #ifdef NOATOM
  unsigned char* MMTptr = atom_init_8(GRANULARITY, true);
  #else
  unsigned char* MMTptr = atom_init_8(GRANULARITY, false);
  #endif
  
  dealwithargs(argc,argv);

  mapSize = sizeof(node_t) / (1 << (int)GRANULARITY) + 1;
  
  graph=initialize_graph();

  unsigned long cycles_compute_start, cycles_compute_end;

  asm volatile("rdcycle %0" : "=r" (cycles_compute_start));

  compute_nodes(graph->e_nodes[0]);
  compute_nodes(graph->h_nodes[0]);

  asm volatile("rdcycle %0" : "=r" (cycles_compute_end));

  chatting("Cycles for computation: %lu\n", 
           cycles_compute_end - cycles_compute_start);

  chatting("Doing em3d with args %d %d %d %d %d %d\n", // SAVVINA comment: in this implementation, chatting is printf
          n_nodes, d_nodes, local_p, NumNodes, ntagIDs, npref);

  chatting("GRANULARITY: %d, mapSize: %" PRIu32 "\n", (int)GRANULARITY, mapSize);

  chatting("size of a node %d\n", sizeof(node_t));
  for(i=0; i<NumNodes;i++) print_graph(graph,i);

// chatting("nonlocals = %d\n",nonlocals); // SAVVINA comment: useless in this version
  printstats();

  /* SAVVINA ADDITIONS */

  for(i = 0; i < NumNodes; i++) deleteList(graph, i);

  free(MMTptr);

  #ifndef NOATOM
    uint64_t **flush_arr = (uint64_t **) malloc(8*sizeof(uint64_t*));
    for(int i=0; i<8; i++)
    {
        flush_arr[i] = (uint64_t*) malloc(sizeof(uint64_t));
        *(flush_arr[i]) = 0x0;
    }

    for(int i=0; i< 256; i++)
    {
        atom_define_bulk(i, (void **)flush_arr, 8); 
    }
  #endif

  uint8_t *buffer;

  // posix_memalign to ensure the starting address of buffer is a multiple of the cache line size (LINE_SIZE)
  if (posix_memalign((void**)&buffer, LINE_SIZE, BUFFER_BYTES) != 0) 
  {
    perror("posix_memalign failed");
    return 1;
  }

  //Fill the buffer with zeros to ensure it's allocated
  memset(buffer, 0, BUFFER_BYTES);

  // Evict L1 + L2 by streaming over buffer
  stream_evict(buffer, PASSES);

  free(buffer);

  asm volatile("fence.i"); // Ensure instruction cache is also cleared

  return 0;
}
