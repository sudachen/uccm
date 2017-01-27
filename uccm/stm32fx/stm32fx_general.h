
#pragma once

#include "../board.h" /* only for simantic code highlighting
                         really this file is included by board.h */

#if defined GPIOC
#define UC_HAS_GPIO_PORT_C
#endif

#if defined GPIOD
#define UC_HAS_GPIO_PORT_D
#endif

#if defined GPIOE
#define UC_HAS_GPIO_PORT_E
#endif

#if defined GPIOF
#define UC_HAS_GPIO_PORT_F
#endif

#pragma uccm require(source) += {UCCM}/uccm/stm32fx/stm32fx_support.c

extern void ucConfig_SystemClock_HSE8_72_wUSB(bool hseBypass);
extern void ucSysTick1ms(void);

#if defined _DEEBUG || defined _FORCE_ASSERT

extern void stm32fx_support$successAssertFailed(uint32_t err, const char *file, int line);

__Forceinline
void stm32fx_support$checkSuccess(uint32_t err, const char *file, int line)
{
    if ( err != HAL_OK ) stm32fx_support$successAssertFailed(err,file,line);
}

#define __Assert_Hal_Success \
    switch(0) \
        for(uint32_t C_LOCAL_ID(err);0;stm32fx_support$checkSuccess(C_LOCAL_ID(err),__FILE__,__LINE__)) \
            case 0: C_LOCAL_ID(err) =
#else

#define __Assert_Hal_Success

#endif
