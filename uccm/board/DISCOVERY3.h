
#pragma once
#include "../uccm.h"

#pragma uccm home(cubefx_fw_f3)="%CUBEFX_FW_F3%"
#pragma uccm board(discovery3)="-DBOARD=DISCOVERY3"
#pragma uccm xcflags(armcc)+="--cpu Cortex-M4"
#pragma uccm xcflags(gcc)+="-mcpu=cortex-m4 -mfloat-abi=hard -mfpu=fpv4-sp-d16"
#pragma uccm xcflags+="-I[cubefx_fw_f3]/Drivers/CMSIS/Include -I[cubefx_fw_f3]/Drivers/CMSIS/Device/ST/STM32F3xx/Include -I[cubefx_fw_f3]/Drivers/STM32F3xx_HAL_Driver/Inc"

#define REQUIRE_HAL_DRIVER(Name) [cubefx_fw_f3]/Drivers/STM32F3xx_HAL_Driver/Src/stm32f3xx_hal_##Name##.c
#define HAL_CONFIG [inc]/stm32f3xx_hal_conf.h

#ifdef __keil_v5
#pragma uccm require([cubefx_fw_f3]/Drivers/CMSIS/Device/ST/STM32F3xx/Source/Templates/arm/startup_stm32f303xc.s)
#else // assume gcc
#pragma uccm require([cubefx_fw_f3]/Drivers/CMSIS/Device/ST/STM32F3xx/Source/Templates/gcc/startup_stm32f303xc.s)
#pragma uccm require([cubefx_fw_f3]/Drivers/CMSIS/Device/ST/STM32F3xx/Source/Templates/gcc/linker/STM32F303XC_FLASH.ld)
#endif

#pragma uccm require([cubefx_fw_f3]/Drivers/CMSIS/Device/ST/STM32F3xx/Source/Templates/system_stm32f3xx.c)

#pragma uccm require(REQUIRE_HAL_DRIVER(rcc))
#pragma uccm require(REQUIRE_HAL_DRIVER(rcc_ex))

#pragma uccm generate(HAL_CONFIG) = "\
#pragma once\n\
#define __STM32F3xx_HAL_CONF_H\n\
#define HSE_VALUE ((uint32_t)8000000)\n\
#define HSE_STARTUP_TIMEOUT ((uint32_t)100)\n\
#define HSI_VALUE ((uint32_t)8000000)\n\
#define HSI_STARTUP_TIMEOUT ((uint32_t)5000)\n\
#define LSI_VALUE ((uint32_t)40000)\n\
#define LSE_VALUE ((uint32_t)32768)\n\
#define LSE_STARTUP_TIMEOUT ((uint32_t)5000)\n\
#define EXTERNAL_CLOCK_VALUE ((uint32_t)8000000)\n\
#define VDD_VALUE ((uint32_t)3300)\n\
#define TICK_INT_PRIORITY ((uint32_t)0)\n\
#define USE_RTOS 0\n\
#define PREFETCH_ENABLE 1\n\
#define INSTRUCTION_CACHE_ENABLE 0\n\
#define DATA_CACHE_ENABLE 0\n\
#define HAL_MODULE_ENABLED\n\
#define HAL_DMA_MODULE_ENABLED\n\
#define HAL_RCC_MODULE_ENABLED\n\
#define HAL_FLASH_MODULE_ENABLE\n\
#define HAL_PWR_MODULE_ENABLED\n\
#define HAL_CORTEX_MODULE_ENABLED\n\
"

#define UCCM_LL_INCLUDE(File) _STR(../stm32f3/ll_##File)

#define STM32F303xC
#include <stm32f3xx.h>

#define UCCM_BOARD_LEDS \
    (0,PE8, LEG_PUSH_PULL),(1,PE9, LEG_PUSH_PULL),(2,PE10,LEG_PUSH_PULL),(3,PE11,LEG_PUSH_PULL),\
    (4,PE12,LEG_PUSH_PULL),(5,PE13,LEG_PUSH_PULL),(6,PE14,LEG_PUSH_PULL),(7,PE15,LEG_PUSH_PULL)
#define UCCM_BOARD_LEDS_COUNT 8
#define UCCM_BOARD_BUTTONS (0, PA0, LEG_FLOAT)
#define UCCM_BOARD_BUTTONS_COUNT 1
#define UCCM_BOARD_MCU_FRECUENCY 72000000

#include "../leds.h"
#include "../buttons.h"

__Inline
void board_setup()
{
    board_setup_leds();
    board_setup_buttons();
}

