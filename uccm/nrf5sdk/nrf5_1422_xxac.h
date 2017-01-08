
#pragma once
#include "../uccm.h"

#ifdef NRF51422

#ifndef NRF51
#define NRF51
#endif

#include "nrf5_1.h"

#ifdef __keil_v5

#if defined SOFTDEVICE_PRESENT
#ifdef S130
#pragma uccm file(firmware.sct)+= LR_IROM1 0x0001B000 0x00025000  {\n  ER_IROM1 0x0001B000 0x00025000  {
#else
#error have no memory layout for this softdevice
#endif
#else
#pragma uccm file(firmware.sct)+= LR_IROM1 0x00000000 0x00040000  {\n  ER_IROM1 0x00000000 0x00040000  {
#endif

#pragma uccm file(firmware.sct)+= \n\
    *.obj (RESET, +First) \n\
    *(InRoot$$Sections) \n\
    .ANY (+RO) \n\
  }

#if defined SOFTDEVICE_PRESENT
#ifdef S130
#pragma uccm file(firmware.sct)+= \n  RW_IRAM1 0x200013C8 0x00006C38  {\n    .ANY (+RW +ZI)\n  }\n}
#else
#error have no memory layout for this softdevice
#endif
#else
#pragma uccm file(firmware.sct)+= \n  RW_IRAM1 0x20000000 0x00008000  {\n    .ANY (+RW +ZI)\n  }\n}
#endif

#else

#pragma uccm file(firmware.ld)~= SEARCH_DIR({TOOLCHAIN}/gcc) \n
#pragma uccm file(firmware.ld)+= GROUP(-lgcc -lc -lnosys) \n\nMEMORY\n{\n

#if defined SOFTDEVICE_PRESENT
#ifdef S130
#pragma uccm file(firmware.ld)+= FLASH (rx) : ORIGIN = 0x0001b000, LENGTH = 0x25000\nRAM (rwx) :  ORIGIN = 0x200013c8, LENGTH = 0x6c38\n
#else
#error have no memory layout for this softdevice
#endif
#else
#pragma uccm file(firmware.ld)+= FLASH (rx) : ORIGIN = 0x00000000, LENGTH = 0x40000\nRAM (rwx) :  ORIGIN = 0x20000000, LENGTH = 0x8000\n
#endif

#pragma uccm file(firmware.ld)+= }\n\nINCLUDE "nrf51_common.ld"\n
#pragma uccm ldflags+= -T[@inc]/firmware.ld

#endif // __keil_v5

#endif // NRF51422
