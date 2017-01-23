
#pragma once
#include "../uccm.h"

#ifdef NRF51422

#ifndef NRF51
#define NRF51
#endif

#define __nRF5x_UC__ 0x51422ac0

#include "nrf5_1.h"

#pragma uccm debugger(jrttview)+= -d NRF51422_XXAC

#ifdef __keil_v5

#pragma uccm file(firmware.sct)~= LR_IROM1 0x00000000+{$ROM_APP_BASE} 0x00040000-{$ROM_APP_BASE}  {\n\
  ER_IROM1 0x00000000+{$ROM_APP_BASE} 0x00040000-{$ROM_APP_BASE}  {
#pragma uccm file(firmware.sct)+= \n\
    *.obj (RESET, +First) \n\
    *(InRoot$$Sections) \n\
    .ANY (+RO) \n\
  }
#pragma uccm file(firmware.sct)~= \n  RW_IRAM1 0x20000000+{$RAM_APP_BASE} 0x00008000-{$RAM_APP_BASE}  {\n    .ANY (+RW +ZI)\n  }\n}

#else

#pragma uccm file(firmware.ld)~= SEARCH_DIR({TOOLCHAIN}/gcc) \n
#pragma uccm file(firmware.ld)+= GROUP(-lgcc -lc -lnosys) \n\nMEMORY\n{\n
#pragma uccm file(firmware.ld)~= FLASH (rx) : ORIGIN = 0x00000000+{$ROM_APP_BASE}, LENGTH = 0x40000-{$ROM_APP_BASE}\n
#pragma uccm file(firmware.ld)~= RAM (rwx) :  ORIGIN = 0x20000000+{$RAM_APP_BASE}, LENGTH = 0x08000-{$RAM_APP_BASE}\n
#pragma uccm file(firmware.ld)+= }\n\nINCLUDE "nrf51_common.ld"\n
#pragma uccm ldflags+= -T[@inc]/firmware.ld

#endif // __keil_v5

#endif // NRF51422
