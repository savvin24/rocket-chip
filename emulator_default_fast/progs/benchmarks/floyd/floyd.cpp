#include <math.h>
#define _ceild(n,d)  ((n)/(d) + 1)
#define _floord(n,d) ((n)/(d))
#define _max(x,y)    ((x) > (y)? (x) : (y))
#define _min(x,y)    ((x) < (y)? (x) : (y))

#include <stdio.h>
#include <stdlib.h>

#include "HPC.h"
#include "crosslayer.h"

#ifndef GRANULARITY
#define GRANULARITY 9
#endif

#define NUM_NODES 36
uint64_t A[NUM_NODES][NUM_NODES];

void init_array()
{
    int i, j;
    
    srand(123);
    for(i= 0; i < NUM_NODES; i++) {
        for(j= 0; j < NUM_NODES; j++) {
            uint64_t ra = 1 + (NUM_NODES * rand() / (2*24 + 1));
            A[i][j] = ra;
        }
    }
    for(i= 0; i < NUM_NODES; i++) {
        A[i][i] = 0;
    }
}

void floydloop()
{
int t1, t2, t3, t4, t5;
 register int lbv, ubv;
 int iter = 0;
/* Start of CLooG code */
if (NUM_NODES >= 1) {
  for (t1=0;t1<=NUM_NODES-1 && iter < 10 ; t1++, iter++) {
    for (t2=0;t2<=_floord(NUM_NODES-1,12);t2++) {
        //pin t4,t1
        #ifndef NOATOM
        for(int i = 0 ; i < 12 ; i++)
            atom_map((void*)&A[12*t2+i][t1], 1, 2);
        #endif
        for (t3=0;t3<=_floord(NUM_NODES-1,2);t3++) {
            //prefetch t4,t5
            #ifndef NOATOM
            for(int i = 0 ; i < 12 ; i++)
                atom_map((void*)&A[12*t2+i][2*t3], _max(1,6/(1<<GRANULARITY)), 1);
            //pin t1,t5 -immediate reuse
            atom_map((void*)&A[t1][2*t3], 1, 2);
            atom_activate(1);
            #endif
            for (t4=12*t2;t4<=_min(NUM_NODES-1,12*t2+11);t4++) {
                for (t5=2*t3;t5<=_min(NUM_NODES-1,2*t3+1);t5++) {
                    A[t4][t5] = ((A[t4][t1] + A[t1][t5]) < A[t4][t5]) ? (A[t4][t1] + A[t1][t5]):A[t4][t5];;
                }
            }
            #ifndef NOATOM
            atom_deactivate(1);
            #endif
        }
    }
  }
}
}

int main()
{

     #ifdef NOATOM
  atom_init(GRANULARITY, true);
  #else
  atom_init(GRANULARITY, false);
  atom_deactivate(0);
  atom_define(1, 0, 64);
  atom_deactivate(2);
  #endif
    int i, j, k, x, y;
    unsigned int distanceYtoX, distanceYtoK, distanceKtoX;
    /*
     * A is the adjacency matrix (square) with
     * dimension length equal to number of nodes in the graph.
     */
    unsigned int width = NUM_NODES;
    unsigned int yXwidth;

    init_array();
HPC perfMon;
perfMon.startMeasurement();
  floydloop();
perfMon.endMeasurement();
perfMon.printStats();
perfMon.printCSV();
}
