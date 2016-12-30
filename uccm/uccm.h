
#pragma once

#pragma uccm home(armcc)= %ARM_KCC_HOME%
#pragma uccm info(armcc)= Keil v5 C/C++ compiling tool

#pragma uccm home(gcc)= %ARM_GCC_HOME%
#pragma uccm info(gcc)= GNU C ARM NONE EABI compiling tool
#pragma uccm download(gcc)= "https://launchpad.net/gcc-arm-embedded/4.8/4.8-2014-q3-update/+download/gcc-arm-none-eabi-4_8-2014q3-20140805-win32.zip"

#pragma uccm home(cubefx_fw_f3)= %CUBEFX_FW_F3%
#pragma uccm info(cubefx_fw_f3)= CubeFx Framework for stm32f3 
#pragma uccm download(cubefx_fw_f3)= "https://uccmpaks.keepmywork.com/cubefx_fw_f3.zip"


#ifdef __ARMCC_VERSION
#define __keil_v5
#endif

#pragma uccm board(*)= -D_UCCM_VERSION=100
#pragma uccm xcflags(armcc)+= --c99 --no_wrap_diagnostics --diag_suppress 161
#pragma uccm xcflags(gcc)+= --std=c99 -fmessage-length=0 -fdata-sections -ffunction-sections -mthumb -Wno-unknown-pragmas

#ifdef __keil_v5
#else
#pragma uccm ldflags+= -mthumb -Wl,--gc-sections
#endif

#include <stdint.h>
#include "macro.h"

#define __Inline static inline
#define __Forceinline static inline __attribute__((always_inline))

#define __Static_Assert_S(Expr,S) _Static_assert(Expr,S)
#define __Static_Assert(Expr) __Static_Assert_S(Expr,#Expr)

#define ucSet_Bits(Where, Bits)   ((Where) |= (Bits))
#define ucClear_Bits(Where, Bits) ((Where) &= ~(Bits))
#define ucGet_Bits(Where, Bits)   ((Where) & (Bits))
#define ucSet_Value(Where, Mask, Bits) ((Where) = (((Where) & (~(Mask))) | (Bits)))

