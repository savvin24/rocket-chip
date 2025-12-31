#ifndef ATOMOPS_H
#define ATOMOPS_H

#ifdef __cplusplus
extern "C" {
#endif

#include <stdint.h>

static inline void amu_clear_acr()
{
    asm volatile ("amu_clear_acr");
}

static inline void amu_write_acr_8(unsigned char* value)
{
    // #ifdef DEBUG
    //     printf("In amu_write_acr: Just before calling corresp. instruction with argument %p\n", value);
    // #endif
    asm volatile ("amu_write_acr %[value]\n\t" ::
            [value] "r" (value));
}

static inline void amu_write_acr_16(uint16_t* value)
{
    // #ifdef DEBUG
    //     printf("In amu_write_acr: Just before calling corresp. instruction with argument %p\n", value);
    // #endif
    asm volatile ("amu_write_acr %[value]\n\t" ::
            [value] "r" (value));
}

static inline unsigned long amu_read_acr()
{
    int value;
    asm volatile ("amu_read_acr %[value]\n\t" :
            [value] "=r" (value));
    return value;
}

static inline void amu_write_len(unsigned long value)
{
    asm volatile ("amu_write_len %[value]\n\t" ::
            [value] "r" (value));
}

static inline unsigned long amu_read_len()
{
    int value;
    asm volatile("amu_read_len %[value]\n\t" :
            [value] "=r" (value));
    return value;
}

static inline void amu_write_stride(unsigned long value)
{
    asm volatile ("amu_write_stride %[value]\n\t" ::
            [value] "r" (value));
}

static inline unsigned long amu_read_stride()
{
    int value;
    asm volatile("amu_read_stride %[value]\n\t" :
            [value] "=r" (value));
    return value;
}

static inline void amu_write_rowcnt(unsigned long value)
{
    asm volatile ("amu_write_rowcnt %[value]\n\t" ::
            [value] "r" (value));
}

static inline unsigned long amu_read_rowcnt()
{
    int value;
    asm volatile("amu_read_rowcnt %[value]\n\t" :
            [value] "=r" (value));
    return value;
}

static inline unsigned long amu_map(unsigned long atomID, unsigned char * ptr)
{
    asm volatile("amu_map %[ptr], %[atomID] \n\t" ::
            [ptr] "r" (ptr), [atomID] "r" (atomID));
    return 0;
}

//SAVVINA added start
static inline unsigned long amu_atom_create_lookup(void* attribute_ptr, unsigned char* ptr, unsigned int offset)
{
    //printk("Create atom with attribute = %d and id = %d \n",attribute,atomID);
    asm volatile ("fatom_select_lookup %[node]\n\t"
        :: [node] "r" (ptr));
    asm volatile ("fatom_load %[offset], %[attribute]\n\t"
        :: [offset] "r" (offset), [attribute] "r" (attribute_ptr)
        );

    return 0;
}

static inline unsigned long amu_atom_createBulk_lookup(unsigned int attributes[], unsigned char* ptr, unsigned int upperLim)
{      
    asm volatile ("fatom_select_lookup %[node]\n\t"
        :: [node] "r" (ptr));
    for (unsigned int __i = 0 ; __i < upperLim ; __i++)
    {
        unsigned int __atom_attrib = attributes[__i];
        asm volatile ("fatom_load %[offset], %[attribute]\n\t"
            :: [offset] "r" (__i), [attribute] "r" (__atom_attrib)
        );
    }
    return 0;
}
//SAVVINA added end

static inline unsigned long amu_atom_create(void* attribute_ptr, unsigned long atomID, unsigned int offset)
{
    //printk("Create atom with attribute = %d and id = %d \n",attribute,atomID);
    asm volatile ("fatom_select %0, %1\n\t"
        :: "r" (atomID), "r" (atomID)
        );
    asm volatile ("fatom_load %[offset], %[attribute]\n\t"
        :: [offset] "r" (offset), [attribute] "r" (attribute_ptr)
        );

    return 0;
}

// DEFAULT
// static inline unsigned long amu_atom_createBulk(unsigned int attributes[], unsigned int atomID, unsigned int upperLim)
// {      
//     asm volatile ("fatom_select %0, %1\n\t"
//         :: "r" (atomID), "r" (atomID)
//         );
//     for (unsigned int __i = 0 ; __i < upperLim ; __i++)
//     {
//         unsigned int __atom_attrib = attributes[__i];
//         asm volatile ("fatom_load %[offset], %[attribute]\n\t"
//             :: [offset] "r" (__i), [attribute] "r" (__atom_attrib)
//         );
//     }
//     return 0;
// }

static inline unsigned long amu_atom_createBulk(void** attributes, unsigned int atomID, unsigned int upperLim)
{      
    asm volatile ("fatom_select %0, %1\n\t"
        :: "r" (atomID), "r" (atomID)
        );
    for (unsigned int __i = 0 ; __i < upperLim ; __i++)
    {
        void* __atom_attrib = attributes[__i];
        asm volatile ("fatom_load %[offset], %[attribute]\n\t"
            :: [offset] "r" (__i), [attribute] "r" (__atom_attrib)
        );
    }
    return 0;
}

static inline unsigned long amu_unmap(unsigned char * ptr)
{
    unsigned long atomID = 0;
    asm volatile("amu_map %[ptr], %[atomID] \n\t" ::
            [ptr] "r" (ptr), [atomID] "r" (atomID));
    return 0;
}

static inline unsigned long amu_map_2d(unsigned long atomID, void * ptr)
{
    asm volatile("amu_map_2d %[ptr], %[atomID] \n\t" ::
            [ptr] "r" (ptr), [atomID] "r" (atomID));
    return 0;
}

static inline unsigned long amu_unmap_2d(unsigned long atomID, void * ptr)
{
    atomID = 0;
    asm volatile("amu_map_2d %[ptr], %[atomID] \n\t" ::
            [ptr] "r" (ptr), [atomID] "r" (atomID));
    return 0;
}

static inline void ast_activate(unsigned long atomID)
{
    asm volatile("ast_activate %[atomID] \n\t" ::
            [atomID] "r" (atomID));
}

static inline void ast_deactivate(unsigned long atomID)
{
    asm volatile("ast_deactivate %[atomID] \n\t" ::
            [atomID] "r" (atomID));
}

static inline void bc_atom_select(unsigned long atomID)
{
    asm volatile("bc_atom_sel %[atomID], %[atomID2] \n\t" ::
            [atomID] "r" (atomID), [atomID2] "r" (atomID));
}


#ifdef __cplusplus
}
#endif

#endif
