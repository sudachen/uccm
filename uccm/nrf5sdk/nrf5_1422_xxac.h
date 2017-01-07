
#pragma once
#include "../uccm.h"

#ifdef NRF51422

#ifndef NRF51
#define NRF51
#endif

#include "nrf5_1.h"

#ifdef __keil_v5

#if defined SOFTDEVICE_PRESENT && defined S130
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
#else

#ifdef SOFTDEVICE_PRESENT
#error have no memory layout for this softdevice
#endif

#pragma uccm file(firmware.sct)+= \
LR_IROM1 0x00000000 0x00040000  {    ; load region size_region\n\
  ER_IROM1 0x00000000 0x00040000  {  ; load address = execution address\n\
   *.obj (RESET, +First) \n\
   *(InRoot$$Sections) \n\
   .ANY (+RO) \n\
  } \n\
  RW_IRAM1 0x20000000 0x00008000  {  ; RW data \n\
   .ANY (+RW +ZI) \n\
  } \n\
}

#endif // SOFTDEVICE_PRESENT

#else

#pragma uccm file(firmware.ld)~= \
SEARCH_DIR({TOOLCHAIN}/gcc) \n

#if defined SOFTDEVICE_PRESENT && defined S130
#pragma uccm file(firmware.ld)+= \
GROUP(-lgcc -lc -lnosys) \n\
\n\
MEMORY\n\
{\n\
  FLASH (rx) : ORIGIN = 0x0001b000, LENGTH = 0x25000\n\
  RAM (rwx) :  ORIGIN = 0x200013c8, LENGTH = 0x6c38\n\
}\n\
\n\
INCLUDE "nrf51_common.ld"\n
#else

#ifdef SOFTDEVICE_PRESENT
#error have no memory layout for this softdevice
#endif

#pragma uccm file(firmware.ld)+= \
GROUP(-lgcc -lc -lnosys) \n\
\n\
MEMORY\n\
{\n\
  FLASH (rx) : ORIGIN = 0x00000000, LENGTH = 0x40000\n\
  RAM (rwx) :  ORIGIN = 0x20000000, LENGTH = 0x8000\n\
}\n\
\n\
INCLUDE "nrf51_common.ld"\n

#endif //SOFTDEVICE_PRESENT

#pragma uccm ldflags+= -T[@inc]/firmware.ld

#endif // __keil_v5



#endif // NRF51422
