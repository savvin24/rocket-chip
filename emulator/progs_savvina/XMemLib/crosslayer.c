#include <stdlib.h>
#include <stdio.h>
#include <stdint.h>

#include "crosslayer.h"
#include "atomops.h"

// void atom_init(uint32_t granularity, int dummy_init)
// {
  
//   uint64_t allocSize = ((uint64_t)1) << 32;
//   //assert(granularity > 1 && "What");
//   allocSize          >>= granularity; 
  
//   unsigned char* myAmuTable = (unsigned char *) malloc (allocSize);
//   uint64_t* travPtr = (uint64_t*) myAmuTable;
//   for(size_t i = 0; i < (int) allocSize/8; i++)
//     travPtr[i] = 0;
//   //#ifdef DEBUG
//   printf("Loaded AMU table\n");
//   //#endif
//   if(!dummy_init)
//     amu_write_acr(myAmuTable);
    
// }

// ------------------------SAVVINA version ----------------------------
unsigned char* atom_init_8(uint32_t granularity, int dummy_init)
{
  
  uint64_t allocSize = ((uint64_t)1) << 32;
  //assert(granularity > 1 && "What");
  allocSize          >>= granularity; 
  
  unsigned char* myAmuTable = (unsigned char *) malloc (allocSize);
  uint64_t* travPtr = (uint64_t*) myAmuTable; // Savvina comment: All the allocated memory is set to 0
  for(size_t i = 0; i < (int) allocSize/8; i++)
    travPtr[i] = 0;

  // #ifdef DEBUG
  //   printf("In ATOM_INIT: Loaded AMU table\n");
  // #endif

  if(!dummy_init)
  {
    // #ifdef DEBUG
    //   printf("In ATOM_INIT: Just before calling amu_write_acr with address: %p\n", myAmuTable);
    // #endif
    amu_write_acr_8(myAmuTable);

    // #ifdef DEBUG
    //   printf("In ATOM_INIT: Returned from amu_write_acr\n");
    // #endif
    
  }

  return myAmuTable;
}

// ------------------------SAVVINA version - 16 bits ----------------------------
uint16_t* atom_init_16(uint32_t granularity, int dummy_init)
{
  
  uint64_t allocSize = ((uint64_t)1) << 32;
  //assert(granularity > 1 && "What");
  allocSize          >>= granularity; 
  
  uint16_t* myAmuTable = (uint16_t *) malloc (2 * allocSize);
  uint64_t* travPtr = (uint64_t*) myAmuTable; // Savvina comment: All the allocated memory is set to 0
  for(size_t i = 0; i < (int) allocSize/4; i++)
    travPtr[i] = 0;

  // #ifdef DEBUG
  //   printf("In ATOM_INIT: Loaded AMU table\n");
  // #endif

  if(!dummy_init)
  {
    // #ifdef DEBUG
    //   printf("In ATOM_INIT: Just before calling amu_write_acr with address: %p\n", myAmuTable);
    // #endif
    amu_write_acr_16(myAmuTable);

    // #ifdef DEBUG
    //   printf("In ATOM_INIT: Returned from amu_write_acr\n");
    // #endif
    
  }

  return myAmuTable;
}

/*
void atom_define(uint8_t atom_id, uint8_t offset, uint32_t property)
{
  amu_atom_create(property, atom_id, offset);
}

void atom_define_bulk(uint8_t atom_id, uint32_t *properties, uint8_t size)
{
  amu_atom_createBulk(properties, atom_id, size);
}

static inline void atom_map(void* start, uint32_t length, uint8_t atom_id)
{
  amu_write_len(length);
  amu_map(atom_id, (unsigned char*) start);
}


void atom_unmap(void* start, uint32_t length, uint8_t atom_id)
{
  amu_write_len(length);
  amu_unmap(atom_id, (unsigned char*) start);
}

void atom_activate(uint8_t atom_id)
{
  ast_activate(atom_id);
}

void atom_deactivate(uint8_t atom_id)
{
  ast_deactivate(atom_id);
}
*/
