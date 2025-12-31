/* For copyright information, see olden_v1.0/COPYRIGHT */

#include "em3d.h"

// #ifndef TORONTO
// #include <cm/cmmd.h>
// #include <fcntl.h>
// #endif

int NumNodes;

// SAVVINA commented out
// #ifndef TORONTO
// void filestuff()
// {
//   CMMD_fset_io_mode(stdout, CMMD_independent);
//   fcntl(fileno(stdout), F_SETFL, O_APPEND);
//   if (CMMD_self_address()) exit(0);
//   __InitRegs(0);
// }
// #endif

void dealwithargs(int argc, char *argv[])
{
  if (argc > 1)
    n_nodes = atoi(argv[1]);
  else
    n_nodes = 256;

  if (argc > 2)
    d_nodes = atoi(argv[2]);
  else
    d_nodes = 3;

  if (argc > 3)
    local_p = atoi(argv[3]);
  else
    local_p = 75;

  if (argc > 4)
    NumNodes = atoi(argv[4]);    
  else
    NumNodes = 1;

  if (argc > 5)
    ntagIDs = atoi(argv[5]);
  else
    ntagIDs = 256;

  if (argc > 6)
    npref = atoi(argv[6]);
  else
    npref = 0;
}


