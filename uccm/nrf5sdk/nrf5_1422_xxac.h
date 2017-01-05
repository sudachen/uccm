
#pragma once
#include "../uccm.h"

#ifdef NRF51422

#ifndef NRF51
#define NRF51
#endif

#include "nrf5_1.h"

#ifdef __keil_v5

#pragma uccm file(firmware.sct)+= \
LR_IROM1 0x0001B000 0x00025000  {    ; load region size_region\n\
  ER_IROM1 0x0001B000 0x00025000  {  ; load address = execution address\n\
   *.obj (RESET, +First) \n\
   *(InRoot$$Sections) \n\
   .ANY (+RO) \n\
  } \n\
  RW_IRAM1 0x200013C8 0x00006C38  {  ; RW data \n\
   .ANY (+RW +ZI) \n\
  } \n\
}

#endif // __keil_v5



#endif // NRF51422
