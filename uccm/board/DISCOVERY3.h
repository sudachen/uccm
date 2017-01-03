
#pragma once
#include "../uccm.h"

#pragma uccm alias(CMSIS) = [cubefx_fw_f3]/Drivers/CMSIS
#pragma uccm alias(STM32F3XX) = [cubefx_fw_f3]/Drivers/CMSIS/Device/ST/STM32F3xx
#pragma uccm alias(STM32HAL) = [cubefx_fw_f3]/Drivers/STM32F3xx_HAL_Driver

#pragma uccm board(discovery3)= -D_BOARD_FILE=DISCOVERY3.h -DSTM32F303xC
#pragma uccm xcflags(*)+= -I[@inc] -I "{CMSIS}/Include" -I "{STM32F3XX}/Include" -I "{STM32HAL}/Inc"

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

#include "../LED.h"
#include "../button.h"

__Inline
void ucSetup_Board()
{
    HAL_Init();
    ucConfig_SystemClock_HSE8_72_wUSB();
    ucSetup_BoardLEDs();
    ucSetup_BoardButtons();
}
