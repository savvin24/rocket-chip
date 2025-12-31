#ifndef CROSSLAYER_H
#define CROSSLAYER_H

#ifdef __cplusplus
extern "C" {
#endif

#include "atomops.h"
#include <stdint.h>
#include <inttypes.h>

// ------------- SAVVINA VERSION OF ATOM_INIT -------------
unsigned char* atom_init_8(uint32_t granularity, int dummy_init);
// void atom_init(uint32_t granularity, int dummy_init);
uint16_t* atom_init_16(uint32_t granularity, int dummy_init);

//SAVVINA added start
static inline void atom_define_lookup(void* start, uint8_t offset, void * property){
  amu_atom_create_lookup(property, (unsigned char*) start, offset);

  // #ifdef DEBUG
  // printf("IN ATOM_DEFINE_LOOKUP: Returned from amu_atom_create_lookup\n");
  // #endif
}

static inline void atom_define_bulk_lookup(void* start, uint32_t *properties, uint8_t size){
  amu_atom_createBulk_lookup(properties, (unsigned char*) start, size);
}
//SAVVINA added end

static inline void atom_define(uint16_t atom_id, uint8_t offset, void * property){ // SAVVINA 14/7 CHANGED PAT
  amu_atom_create(property, atom_id, offset);

  // #ifdef DEBUG
  // printf("IN ATOM_DEFINE: Returned from amu_atom_create with atom id %" PRIu16 "\n", atom_id);
  // #endif
}

// DEFAULT
// static inline void atom_define_bulk(uint8_t atom_id, uint32_t *properties, uint8_t size){
//   amu_atom_createBulk(properties, atom_id, size);
// }

static inline void atom_define_bulk(uint8_t atom_id, void ** properties, uint8_t size){
  amu_atom_createBulk(properties, atom_id, size);

  // #ifdef DEBUG
  // printf("IN ATOM_DEFINE_BULK: Returned from amu_atom_createBulk\n");
  // #endif
}

static inline void atom_map(void* start, uint32_t length, uint16_t atom_id){ // SAVVINA 14/7 CHANGED PAT
  amu_write_len(length);

  // #ifdef DEBUG
  // printf("IN ATOM_MAP: Returned from amu_write_len\n");
  // #endif

  amu_map(atom_id, (unsigned char*) start);

  // #ifdef DEBUG
  // printf("IN ATOM_MAP: Returned from amu_map with atom id %" PRIu16 "\n", atom_id);
  // #endif
}

static inline void atom_unmap(void* start, uint32_t length){
  amu_write_len(length);

  // #ifdef DEBUG
  //   printf("IN ATOM_UNMAP: Returned from amu_write_len\n");
  // #endif

  amu_unmap((unsigned char*) start);

  // #ifdef DEBUG
  //   printf("IN ATOM_UNMAP: Returned from amu_unmap\n");
  // #endif
}

static inline void atom_activate(uint8_t atom_id){
  ast_activate(atom_id);

  // #ifdef DEBUG
  // printf("IN ATOM_ACTIVATE: Returned from ast_activate\n");
  // #endif
}

static inline void atom_deactivate(uint8_t atom_id){
  ast_deactivate(atom_id);

  // #ifdef DEBUG
  // printf("IN ATOM_DEACTIVATE: Returned from ast_deactivate\n");
  // #endif
}

static inline void atom_select(uint8_t atom_id){
  bc_atom_select(atom_id);
  
  // #ifdef DEBUG
  // printf("IN ATOM_SELECT: Returned from bc_atom_select\n");
  // #endif
}

#ifdef __cplusplus
}
#endif

#endif