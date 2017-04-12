
#pragma once
#include <uccm/uccm.h>

#ifdef NRF52

#pragma uccm debugger(nrfjprog)+= -f NRF52

#ifdef __keil_v5
#pragma uccm ldflags+= --cpu Cortex-M4.fp
#pragma uccm asflags+= --cpu Cortex-M4.fp
#pragma uccm cflags+=  --cpu Cortex-M4.fp
#else // assume gcc
#pragma uccm cflags+= -mcpu=cortex-m4 -mfloat-abi=hard -mfpu=fpv4-sp-d16
#pragma uccm ldflags+= -mcpu=cortex-m4 -mfloat-abi=hard -mfpu=fpv4-sp-d16
#endif

#ifdef __keil_v5
#pragma uccm ldflags+= --keep=arm_startup_*
#pragma uccm require(begin)+= {TOOLCHAIN}/arm/arm_startup_nrf52.s
#else
#pragma uccm require(begin)+= {TOOLCHAIN}/gcc/gcc_startup_nrf52.S
#endif

#pragma uccm require(begin)+= {TOOLCHAIN}/system_nrf52.c

#include <nrf.h>

#endif // NRF52
