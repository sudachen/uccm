
#pragma once

#include "../board.h" /* only for simantic code highlighting
                         really this file is included by board.h */

#ifndef STM32F303xC
#error you have to define STM32F303xC
#endif

#ifdef __keil_v5
#pragma uccm require(begin) = {STM32F3XX}/Source/Templates/arm/startup_stm32f303xc.s
#pragma uccm asflags+= --pd "STM32F303xC SETA 1"
#else // assume gcc
#pragma uccm require(begin) = {STM32F3XX}/Source/Templates/gcc/startup_stm32f303xc.s
#pragma uccm ldflags+= -T{STM32F3XX}/Source/Templates/gcc/linker/STM32F303XC_FLASH.ld
#endif

#ifdef __keil_v5

#pragma uccm file(firmware.sct)+= \
LR_IROM1 0x08000000 0x00040000 {    ; load region size_region\n\
  ER_IROM1 0x08000000 0x00040000 {  ; load address = execution address\n\
   *.obj (RESET, +First)\n\
   *(InRoot$$Sections)\n\
   .ANY (+RO)\n\
  }\n\
  RW_IRAM1 0x20000000 UNINIT 0x00010000 {  ; RW data\n\
   .ANY (+RW +ZI)\n\
  }\n\
}\n

#endif

#include "stm32fx_f3.h"

