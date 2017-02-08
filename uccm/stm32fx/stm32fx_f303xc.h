
#pragma once

#include "../board.h" /* only for simantic code highlighting
                         really this file is included by board.h */

#define __stm32Fx_UC__ 0xf3030C

#ifndef STM32F303xC
#error you have to define STM32F303xC
#endif

#pragma uccm let(JLINK_DEVICE)?= STM32F303XC

#ifdef __keil_v5
#pragma uccm copy(startup_stm32f303xc.s) = {STM32F3XX}/Source/Templates/arm/startup_stm32f303xc.s
#pragma uccm replace(startup_stm32f303xc.s) ~= |^\s*Stack_Size\s*EQU\s*([x0-9a-fA-F]+)\s*$|{$STACK_SIZE}|
#pragma uccm replace(startup_stm32f303xc.s) ~= |^\s*Heap_Size\s*EQU\s*([x0-9a-fA-F]+)\s*$|{$HEAP_SIZE}|
#pragma uccm require(begin) = [@inc]/startup_stm32f303xc.s

#else // assume gcc
#pragma uccm require(begin) = {STM32F3XX}/Source/Templates/gcc/startup_stm32f303xc.s
#pragma uccm copy(STM32F303XC_FLASH.ld) = {STM32F3XX}/Source/Templates/gcc/linker/STM32F303XC_FLASH.ld
#pragma uccm replace(STM32F303XC_FLASH.ld) ~= |^\s*_Min_Stack_Size\s*=\s*([x0-9a-fA-F]+)\s*;.*$|{$STACK_SIZE}|
#pragma uccm replace(STM32F303XC_FLASH.ld) ~= |^\s*_Min_Heap_Size\s*=\s*([x0-9a-fA-F]+)\s*;.*$|{$HEAP_SIZE}|
#pragma uccm ldflags+= -T[@inc]/STM32F303XC_FLASH.ld
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
