
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
#pragma uccm cflags+= --diag_warning=error
#else
#pragma uccm cflags+= -Werror
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
#include <stdlib.h>
#include "macro.h"

#define __Inline static inline
#define __Forceinline static inline __attribute__((always_inline))
#define __Weak  __attribute__((weak))

#ifdef __keil_v5
#define __Static_Assert_S(Expr,S) static int C_LOCAL_ID(static_assert)[(Expr)?1:-1] __attribute__((unused))
#else
#define __Static_Assert_S(Expr,S) _Static_assert(Expr,S)
#endif
#define __Static_Assert(Expr) __Static_Assert_S(Expr,#Expr)

#if defined _DEBUG || defined _FORCE_ASSERT
#define __Assert_S(x,Text) if (x); else uccm$assertFailed(Text,__FILE__,__LINE__)
#else
#define __Assert_S(x,Text) (void)0
#endif

#define __Unreachable() __Assert_S(0,"unreachable code")
#define __Assert(x) __Assert_S(x,#x)

#define ucSet_Bits(Where, Bits)   ((Where) |= (Bits))
#define ucClear_Bits(Where, Bits) ((Where) &= ~(Bits))
#define ucGet_Bits(Where, Bits)   ((Where) & (Bits))
#define ucSet_Value(Where, Mask, Bits) ((Where) = (((Where) & (~(Mask))) | (Bits)))

#define __Do_Not_Remove __attribute__((used))

enum
{
    UC_ERROR_IN_SETUP_BOARD = 1,
    UC_ERROR_IN_ASSERT,
    UC_ERROR_IN_SOFTDEVICE,
    UC_ERROR_IN_SOFTDEVICE_INIT,
    UC_ERROR_IN_IRQ_HANDLER,
    UC_ERROR_IN_IRQ_HARDFAULT,
};

extern void ucFatalError(uint32_t where);

#pragma uccm require(end) += {UCCM}/uccm/uccm.c

void ucSetup_Print(void);

typedef struct UcFormatParam UcFormatParam;
typedef struct UcFormatOpt UcFormatOpt;
typedef void (*UcFormatPrinter)(UcFormatOpt *opt,UcFormatParam *param);

struct uccm$E { void(*e)(struct uccm$E); };

struct UcFormatParam {
    union {
        struct uccm$E e;
        uint32_t u;
        int32_t  i;
        float    f;
        const char *str;
        void *ptr;
    } v;
    UcFormatPrinter print;
};

void ucPutS(const char *text, bool complete);
void ucPrintF(size_t argno, const char *fmt, int flags, UcFormatParam *params);

#define C_FORMAT_QUOTE(x,_) x

extern void uccm$print32u(UcFormatOpt *opt,UcFormatParam *param);
#define $u(val) { .v = {.u = (val)}, .print = uccm$print32u }
extern void uccm$print32i(UcFormatOpt *opt,UcFormatParam *param);
#define $i(val) { .v = {.i = (val)}, .print = uccm$print32i }
extern void uccm$print32f(UcFormatOpt *opt,UcFormatParam *param);
#define $f(val) { .v = {.f = (val)}, .print = uccm$print32f }
extern void uccm$printOneChar(UcFormatOpt *opt,UcFormatParam *param);
#define $c(val) { .v = {.u = (val)}, .print = uccm$printOneChar }
extern void uccm$printCstr(UcFormatOpt *opt,UcFormatParam *param);
#define $s(val) { .v = {.str = (val)}, .print = uccm$printCstr }
extern void uccm$printPtr(UcFormatOpt *opt,UcFormatParam *param);
#define $p(val) { .v = {.ptr = (val)}, .print = uccm$printPtr }

#if defined _DEBUG || defined _FORCE_PRINT
#define ucPrint(...) ucPrintF_Var(1,0,__VA_ARGS__,NIL)
#else
#define ucPrint(...) (void)0
#endif

#define ucError(...) ucPrintF_Var(1,1,__VA_ARGS__,NIL)

#define ucPrintF_Var(nL,Wt,Fmt,...) \
    do {\
        UcFormatParam params[] = {C_MAP(C_FORMAT_QUOTE,C_COMMA,__VA_ARGS__)}; \
        ucPrintF(sizeof(params)/sizeof(params[0]),(Fmt),(nL?1:0)|(Wt?2:0),params); \
    } while(0)

extern void uccm$assertFailed(const char *text, const char *file, int line);
