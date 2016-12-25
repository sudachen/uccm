

#pragma uccm require_uccm_revision(1+)
#pragma uccm home(armcc) = "%ARM_KCC_HOME%"
#pragma uccm home(gcc) = "%ARM_GCC_HOME%"

#ifdef __ARMCC_VERSION
#define __keil_v5
#endif

#pragma uccm cflags(armcc)+="--c99 --no_wrap_diagnostics --diag_suppress 161"
#pragma uccm cflags(gcc)+="--std=c99 -fmessage-length=0 -Wl,--gc-sections -fdata-sections -ffunction-sections -mthumb -Wno-unknown-pragmas"

#include <stdint.h>
#include "macro.h"

#define __Inline static inline
#define __Forceinline static inline __attribute__((always_inline))

#define __Static_Assert_S(Expr,S) _Static_assert(Expr,S)
#define __Static_Assert(Expr) __Static_Assert_S(Expr,#Expr)

#define set_bits(Where, Bits)   ((Where) |= (Bits))
#define clear_bits(Where, Bits) ((Where) &= ~(Bits))
#define get_bits(Where, Bits)   ((Where) & (Bits))
#define set_value(Where, Mask, Bits) ((Where) = (((Where) & (~(Mask))) | (Bits)))

