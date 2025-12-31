/* For copyright information, see olden_v1.0/COPYRIGHT */

#include "em3d.h"
// int nonlocals=0; // SAVVINA comment: useless in this version
void compute_nodes(nodelist)
register node_t *nodelist;
{
  register int i;
  register node_t *localnode;
  
  for (; nodelist; ) {
    register double cur_value;
    register int from_count ;
    register double *other_value;
    register double coeff;
    register double value;
    /*register double *coeffs;*/
    /*register node_t **from_nodes;*/

    localnode = nodelist;
    cur_value = *(localnode->value);
    from_count = localnode->from_count-1;

    // #ifdef DEBUG
    //   chatting("cur_value before = %f, node from_count= %d, from_count before =%d\n", cur_value, localnode->from_count, from_count);
    // #endif

    for (i=0; i < from_count; i+=2) {

      other_value = localnode->from_values[i];
      coeff = localnode->coeffs[i];

      if (other_value) value = *other_value;
      else value = 0;

      cur_value -= coeff*value;

      // #ifdef DEBUG
      //   chatting("iteration %d: coeff %f, value %f, cur_value %f\n",i,coeff,value, cur_value);
      // #endif

      other_value = localnode->from_values[i+1];
      coeff = localnode->coeffs[i];
      
      if (other_value) value = *other_value;
      else value = 0;
      
      cur_value -= coeff*value;
      
      // #ifdef DEBUG
      //   chatting("iteration %d: coeff %f, value %f, cur_value %f\n",i,coeff,value, cur_value);
      // #endif
    }

    if (i==from_count)  {
      other_value = localnode->from_values[i];
      coeff = localnode->coeffs[i];
      
      if (other_value) value = *other_value;
      else value = 0;
      
      cur_value -= coeff*value;

      // #ifdef DEBUG
      //   chatting("iteration %d: coeff %f, value %f, cur_value %f\n",i,coeff,value, cur_value);
      // #endif
    }

    *(localnode->value) = cur_value;
    nodelist = localnode->next;
  } /* for */
}
