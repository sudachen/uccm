
#pragma once

#include "../board.h" /* only for simantic code highlighting
                         really this file is included by board.h */

#pragma uccm cflags+= -DUSE_HAL_DRIVER

#ifdef __keil_v5
#pragma uccm ldflags+= --cpu Cortex-M4.fp 
#pragma uccm asflags+= --cpu Cortex-M4.fp
#pragma uccm cflags+=  --cpu Cortex-M4.fp
#else // assume gcc
#pragma uccm cflags+= -mcpu=cortex-m4 -mfloat-abi=hard -mfpu=fpv4-sp-d16
#pragma uccm ldflags+= -mcpu=cortex-m4 -mfloat-abi=hard -mfpu=fpv4-sp-d16
#endif

#pragma uccm require(begin)  = {STM32F3XX}/Source/Templates/system_stm32f3xx.c
#pragma uccm require(module) = {STM32HAL}/Src/stm32f3xx_hal.c
#pragma uccm require(module) = {STM32HAL}/Src/stm32f3xx_hal_gpio.c
#pragma uccm require(module) = {STM32HAL}/Src/stm32f3xx_hal_rcc.c
#pragma uccm require(module) = {STM32HAL}/Src/stm32f3xx_hal_rcc_ex.c
#pragma uccm require(module) = {STM32HAL}/Src/stm32f3xx_hal_cortex.c

#ifdef __keil_v5
#pragma uccm ldflags+= --keep=startup_*
#endif

#pragma uccm file(stm32f3xx_hal_conf.h) += \
#pragma once\n\
#define assert_param(expr) ((void)0U)\n\
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
#define HAL_RCC_MODULE_ENABLED\n\
#define HAL_CORTEX_MODULE_ENABLED\n\
#define HAL_GPIO_MODULE_ENABLED\n\
#define HAL_FLASH_MODULE_ENABLE\n\
#include <stm32f3xx_hal.h>\n\
#include <stm32f3xx_hal_gpio.h>\n\
#include <stm32f3xx_hal_rcc.h>\n\
#include <stm32f3xx_hal_cortex.h>\n\
#include <stm32f3xx_hal_flash.h>\n\

/*
#define HAL_DMA_MODULE_ENABLED\n\
#include <stm32f3xx_hal_dma.h>\n\
#define HAL_PWR_MODULE_ENABLED\n\
#include <stm32f3xx_hal_pwr.h>\n\
#define HAL_ADC_MODULE_ENABLED
#include <stm32f3xx_hal_adc.h>
#define HAL_CAN_MODULE_ENABLED
#include <stm32f3xx_hal_can.h>
#define HAL_CEC_MODULE_ENABLED
#include <stm32f3xx_hal_cec.h>
#define HAL_COMP_MODULE_ENABLED
#include <stm32f3xx_hal_comp.h>
#define HAL_CRC_MODULE_ENABLED
#include <stm32f3xx_hal_crc.h>
#define HAL_DAC_MODULE_ENABLED
#include <stm32f3xx_hal_dac.h>
#define HAL_SRAM_MODULE_ENABLED
#include <stm32f3xx_hal_sram.h>
#define HAL_NOR_MODULE_ENABLED
#include <stm32f3xx_hal_nor.h>
#define HAL_NAND_MODULE_ENABLED
#include <stm32f3xx_hal_nand.h>
#define HAL_PCCARD_MODULE_ENABLED
#include <stm32f3xx_hal_pccard.h>
#define HAL_HRTIM_MODULE_ENABLED
#include <stm32f3xx_hal_hrtim.h>
#define HAL_I2C_MODULE_ENABLED
#include <stm32f3xx_hal_i2c.h>
#define HAL_I2S_MODULE_ENABLED
#include <stm32f3xx_hal_i2s.h>
#define HAL_IRDA_MODULE_ENABLED
#include <stm32f3xx_hal_irda.h>
#define HAL_IWDG_MODULE_ENABLED
#include <stm32f3xx_hal_iwdg.h>
#define HAL_OPAMP_MODULE_ENABLED
#include <stm32f3xx_hal_opamp.h>
#define HAL_PCD_MODULE_ENABLED
#include <stm32f3xx_hal_pcd.h>
#define HAL_RTC_MODULE_ENABLED
#include <stm32f3xx_hal_rtc.h>
#define HAL_SDADC_MODULE_ENABLED
#include <stm32f3xx_hal_sdadc.h>
#define HAL_SMARTCARD_MODULE_ENABLED
#include <stm32f3xx_hal_smartcard.h>
#define HAL_SMBUS_MODULE_ENABLED
#include <stm32f3xx_hal_smbus.h>
#define HAL_SPI_MODULE_ENABLED
#include <stm32f3xx_hal_spi.h>
#define HAL_TIM_MODULE_ENABLED
#include <stm32f3xx_hal_tim.h>
#define HAL_TSC_MODULE_ENABLED
#include <stm32f3xx_hal_tsc.h>
#define HAL_UART_MODULE_ENABLED
#include <stm32f3xx_hal_uart.h>
#define HAL_USART_MODULE_ENABLED
#include <stm32f3xx_hal_usart.h>
#define HAL_WWDG_MODULE_ENABLED
#include <stm32f3xx_hal_wwdg.h>
*/

#include <stm32f3xx.h>
#include "stm32fx_general.h"

__Inline
void ucSetup_BoardUSB()
{
    __HAL_RCC_USB_CLK_ENABLE();

    GPIO_InitTypeDef pinDef = {
        .Pin = GPIO_PIN_11|GPIO_PIN_12,
        .Mode = GPIO_MODE_AF_PP,
        .Pull = GPIO_NOPULL,
        .Speed = GPIO_SPEED_FREQ_HIGH,
        .Alternate = GPIO_AF14_USB,
    };

    HAL_GPIO_Init(GPIOA, &pinDef);
    __HAL_RCC_USB_CLK_ENABLE();
}
