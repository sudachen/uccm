
#pragma once
#include <uccm/uccm.h>

#ifdef NRF51

#ifdef __keil_v5
#pragma uccm ldflags+= --cpu Cortex-M0
#pragma uccm asflags+= --cpu Cortex-M0
#pragma uccm cflags+=  --cpu Cortex-M0
#else // assume gcc
#pragma uccm cflags+= -mcpu=cortex-m0 -mfloat-abi=soft
#pragma uccm ldflags+= -mcpu=cortex-m0 -mfloat-abi=soft
#endif

#ifdef __keil_v5
#pragma uccm ldflags+= --keep=arm_startup_*
#pragma uccm require(begin)+= {TOOLCHAIN}/arm/arm_startup_nrf51.s
#else
#pragma uccm require(begin)+= {TOOLCHAIN}/gcc/gcc_startup_nrf51.S
#endif

#pragma uccm require(begin)+= {TOOLCHAIN}/system_nrf51.c

#include <nrf.h>

#endif // NRF51
