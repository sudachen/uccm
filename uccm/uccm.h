
#pragma once

#ifdef __ARMCC_VERSION
#define __keil_v5
#endif

#pragma uccm board(*)= -D_UCCM_VERSION=100
#pragma uccm xcflags(armcc)+= --c99 --no_wrap_diagnostics --diag_suppress 161,1293,177
#pragma uccm xcflags(gcc)+= --std=c99 -fmessage-length=0 -fdata-sections -ffunction-sections -mthumb -Wno-unknown-pragmas --short-enums

#ifdef _DEBUG
#pragma uccm let(CFLAGS_OPT)?= -g -O0
#elif defined _RELEASE
#ifdef __keil_v5
#pragma uccm let(CFLAGS_OPT)?= -g -O2
#else
#pragma uccm let(CFLAGS_OPT)?= -g -O2 -flto
#endif
#endif

#pragma uccm cflags+= {$CFLAGS_OPT}

#ifdef __keil_v5
#pragma uccm cflags+= --diag_warning=error
#else
#pragma uccm cflags+= -Werror -Wall -Wno-missing-braces -Wno-unused-function
#endif

#ifdef __keil_v5
#pragma uccm cflags+= --apcs interwork --split_sections -D__UVISION_VERSION="520" --asm --interleave --asm-dir [@obj]
#pragma uccm ldflags+= --strict --scatter [@inc]/firmware.sct 
#pragma uccm ldflags+= --info summarysizes --map --xref --callgraph --symbols --info sizes --info totals --info unused --info veneers --list [@build]/firmware.map
#pragma uccm asflags+= --apcs=interwork --pd "__UVISION_VERSION SETA 520"
#ifdef USE_MICROLIB
#pragma uccm cflags+= -D__MICROLIB
#pragma uccm ldflags+= --library_type=microlib -lm
#pragma uccm asflags+= --pd "__MICROLIB SETA 1"
#endif
#else
#pragma uccm ldflags+= {$CFLAGS_OPT} -mthumb -Wl,--gc-sections,--cref,-Map=[@build]/firmware.map
#pragma uccm ldflags+= -lc_nano -lm
#endif

#include <stdint.h>
#include <stdbool.h>
#include <stdlib.h>
#include <string.h>
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

#define __Unreachable __Assert_S(0,"unreachable code")
#define __Assert(x) __Assert_S(x,#x)

#define ucSet_Bits(Where, Bits)   ((Where) |= (Bits))
#define ucClear_Bits(Where, Bits) ((Where) &= ~(Bits))
#define ucGet_Bits(Where, Bits)   ((Where) & (Bits))
#define ucSet_Value(Where, Mask, Bits) ((Where) = (((Where) & (~(Mask))) | (Bits)))

#define __Do_Not_Remove __attribute__((used))

extern void on_fatalError(void);

#pragma uccm require(end) += {UCCM}/uccm/uccm.c

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
    UcFormatPrinter printCallback;
};

void setup_print(void);
void reset_board(void);
void _put_string(const char *text, bool complete);
void print_format(size_t argno, int flags, UcFormatParam *params);
void let_printCompleteAlways(void);

extern void uccm$print32u(UcFormatOpt *opt,UcFormatParam *param);
#define $u(val) { .v = {.u = (val)}, .printCallback = uccm$print32u }
extern void uccm$print32i(UcFormatOpt *opt,UcFormatParam *param);
#define $i(val) { .v = {.i = (val)}, .printCallback = uccm$print32i }
extern void uccm$print32f(UcFormatOpt *opt,UcFormatParam *param);
#define $f(val) { .v = {.f = (val)}, .printCallback = uccm$print32f }
extern void uccm$print32x(UcFormatOpt *opt,UcFormatParam *param);
#define $x(val) { .v = {.u = (val)}, .printCallback = uccm$print32x }
extern void uccm$printOneChar(UcFormatOpt *opt,UcFormatParam *param);
#define $c(val) { .v = {.u = (val)}, .printCallback = uccm$printOneChar }
extern void uccm$printCstr(UcFormatOpt *opt,UcFormatParam *param);
#define $s(val) { .v = {.str = (val)}, .printCallback = uccm$printCstr }
extern void uccm$printPtr(UcFormatOpt *opt,UcFormatParam *param);
#define $p(val) { .v = {.ptr = (void*)(val)}, .printCallback = uccm$printPtr }

#if defined _DEBUG || defined _FORCE_PRINT
#define PRINT(...) UC_PRINTF_VAR(1,0,__VA_ARGS__,NIL)
#define PRINT_IS_ENABLED 1
#else
#define PRINT(...) (void)0
#define PRINT_IS_ENABLED 0
#endif

#define PRINT_ERROR(...) UC_PRINTF_VAR(1,1,__VA_ARGS__,NIL)

#define UC_FORMAT_QUOTE(x,_) x
#define UC_PRINTF_VAR(nL,Wt,Fmt,...) \
    do {\
        UcFormatParam params[] = { {.v = {.str=(Fmt)}}, C_MAP(UC_FORMAT_QUOTE,C_COMMA,__VA_ARGS__)}; \
        print_format(sizeof(params)/sizeof(params[0]),(nL?1:0)|(Wt?2:0),params); \
    } while(0)

extern void uccm$assertFailed(const char *text, const char *file, int line);
extern bool uccm$irqCriticalEnter(void);
extern void uccm$irqCriticalExit(bool);

#define __Critical \
    switch (0) for ( bool uccm$nested; 0; uccm$irqCriticalExit(uccm$nested) ) \
        if(1) { case 0: uccm$nested = uccm$irqCriticalEnter(); goto C_LOCAL_ID(doIt); } \
        else C_LOCAL_ID(doIt):

#pragma uccm file(uccm_dynamic_defs.h) += #pragma once\n\n

#ifndef _UCCM_PREPROCESSING
#include <uccm_dynamic_defs.h>
#endif
