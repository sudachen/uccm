
#pragma once
#include <uccm/uccm.h>
#include <uccm/./././stm32fx/stm32fx_f3_fw_r1p6.h>

#pragma uccm board(discovery3)= -D_BOARD_FILE=DISCOVERY3.h -DSTM32F303xC

#ifndef STM32F303xC
#error you have to define STM32F303xC
#endif

#define UCCM_LL_INCLUDE(File) <uccm/stm32fx/stm32fx_##File>
#include <uccm/stm32fx/stm32fx_f303xc.h>

#define UCCM_BOARD_LEDS \
    (Blue1,PE8, LEG_PUSH_PULL),(Red1,PE9, LEG_PUSH_PULL),(Orange1,PE10,LEG_PUSH_PULL),(Green1,PE11,LEG_PUSH_PULL),\
    (Blue2,PE12,LEG_PUSH_PULL),(Red2,PE13,LEG_PUSH_PULL),(Orange2,PE14,LEG_PUSH_PULL),(Green2,PE15,LEG_PUSH_PULL),\
    NIL
#define UCCM_BOARD_LEDS_COUNT C_LIST_LENGTH(UCCM_BOARD_LEDS)

#define UCCM_BOARD_BUTTONS (User,PA0,LEG_PULL_DOWN), NIL
#define UCCM_BOARD_BUTTONS_COUNT C_LIST_LENGTH(UCCM_BOARD_BUTTONS)

#define UCCM_BOARD_MCU_FRECUENCY 72000000
#define UCCM_TIMER_CLOCK_FREQ 40000 // LSI is used
#define UCCM_TIMER_CLOCK_PRESCALER 39

#include <uccm/LED.h>
#include <uccm/button.h>

__Inline
void setup_board()
{
    HAL_Init();
    config_systemClock_HSE8_72_wUSB(/*bypass=*/true);
    setup_boardLEDs();
    setup_boardButtons();
}
