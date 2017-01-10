
#pragma once

#ifdef __ARMCC_VERSION
#define __keil_v5
#endif

#pragma uccm board(*)= -D_UCCM_VERSION=100
#pragma uccm xcflags(armcc)+= --c99 --no_wrap_diagnostics --diag_suppress 161
#pragma uccm xcflags(gcc)+= --std=c99 -fmessage-length=0 -fdata-sections -ffunction-sections -mthumb -Wno-unknown-pragmas

#ifdef _DEBUG
#pragma uccm cflags+= -g -O0
#elif defined _RELEASE
#pragma uccm cflags+= -g -O2
#endif

#ifdef __keil_v5
#pragma uccm cflags+= --apcs interwork --split_sections -D__UVISION_VERSION="520" --asm --interleave --asm-dir [@obj]
#pragma uccm ldflags+= --strict --scatter [@inc]/firmware.sct 
#pragma uccm ldflags+= --info summarysizes --map --xref --callgraph --symbols --info sizes --info totals --info unused --info veneers --list [@build]/firmware.map
#pragma uccm asflags+= --apcs=interwork --pd "__UVISION_VERSION SETA 520"
#ifdef USE_MICROLIB
#pragma uccm cflags+= -D__MICROLIB
#pragma uccm ldflags+= --library_type=microlib 
#pragma uccm asflags+= --pd "__MICROLIB SETA 1"
#endif
#else
#pragma uccm ldflags+= -mthumb -Wl,--gc-sections,--cref,-Map=[@build]/firmware.map
#endif

#include <stdint.h>
#include <stdbool.h>
#include "macro.h"

#define __Inline static inline
#define __Forceinline static inline __attribute__((always_inline))

#ifdef __keil_v5
#define __Static_Assert_S(Expr,S) static int C_LOCAL_ID(static_assert)[(Expr)?1:-1] __attribute__((unused))
#else
#define __Static_Assert_S(Expr,S) _Static_assert(Expr,S)
#endif
#define __Static_Assert(Expr) __Static_Assert_S(Expr,#Expr)

#define __Assert(x) (void)0

#define ucSet_Bits(Where, Bits)   ((Where) |= (Bits))
#define ucClear_Bits(Where, Bits) ((Where) &= ~(Bits))
#define ucGet_Bits(Where, Bits)   ((Where) & (Bits))
#define ucSet_Value(Where, Mask, Bits) ((Where) = (((Where) & (~(Mask))) | (Bits)))

