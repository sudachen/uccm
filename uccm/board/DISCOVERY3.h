
#pragma once
#include "../uccm.h"

#pragma uccm alias(CMSIS) = [cubefx_fw_f3]/Drivers/CMSIS
#pragma uccm alias(STM32F3XX) = [cubefx_fw_f3]/Drivers/CMSIS/Device/ST/STM32F3xx
#pragma uccm alias(STM32HAL) = [cubefx_fw_f3]/Drivers/STM32F3xx_HAL_Driver

#pragma uccm board(discovery3)= -D_BOARD_FILE=DISCOVERY3.h -DSTM32F303xC
#pragma uccm xcflags(armcc)+= --cpu Cortex-M4.fp
#pragma uccm xcflags(gcc)+= -mcpu=cortex-m4 -mfloat-abi=hard -mfpu=fpv4-sp-d16
#pragma uccm xcflags(*)+= -I[@inc] -I "{CMSIS}/Include" -I "{STM32F3XX}/Include" -I "{STM32HAL}/Inc"
#pragma uccm cflags+= -DUSE_HAL_DRIVER

#ifdef __keil_v5
#pragma uccm require(begin) = {STM32F3XX}/Source/Templates/arm/startup_stm32f303xc.s
#pragma uccm ldflags+= --cpu Cortex-M4.fp 
#pragma uccm asflags+= --cpu Cortex-M4.fp --pd "STM32F303xC SETA 1"
#else // assume gcc
#pragma uccm require(begin) = {STM32F3XX}/Source/Templates/gcc/startup_stm32f303xc.s
#pragma uccm ldflags+= -mcpu=cortex-m4 -mfloat-abi=hard -mfpu=fpv4-sp-d16 -T{STM32F3XX}/Source/Templates/gcc/linker/STM32F303XC_FLASH.ld
#endif

#pragma uccm require(begin)  = {STM32F3XX}/Source/Templates/system_stm32f3xx.c
#pragma uccm require(module) = {STM32HAL}/Src/stm32f3xx_hal.c
#pragma uccm require(module) = {STM32HAL}/Src/stm32f3xx_hal_gpio.c
#pragma uccm require(module) = {STM32HAL}/Src/stm32f3xx_hal_rcc.c
#pragma uccm require(module) = {STM32HAL}/Src/stm32f3xx_hal_cortex.c

#ifdef __keil_v5
#pragma uccm file(firmware.sct)+= \
LR_IROM1 0x08000000 0x00040000 {    ; load region size_region\n\
  ER_IROM1 0x08000000 0x00040000 {  ; load address = execution address\n\
   *.obj (RESET, +First)\n\
   *(InRoot$$Sections)\n\
   .ANY (+RO)\n\
  }\n\
  RW_IRAM1 0x20000000 UNINIT 0x00010000 {  ; RW data\n\
   .ANY (+RW +ZI)\n\
  }\n\
}\n\

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

#define UCCM_LL_INCLUDE(File) <uccm/stm32f3/ll_##File>

#include <stm32f3xx.h>

#define UCCM_BOARD_LEDS \
    (r0,PE8, LEG_PUSH_PULL),(r1,PE9, LEG_PUSH_PULL),(r2,PE10,LEG_PUSH_PULL),(r3,PE11,LEG_PUSH_PULL),\
    (r4,PE12,LEG_PUSH_PULL),(r5,PE13,LEG_PUSH_PULL),(r6,PE14,LEG_PUSH_PULL),(r7,PE15,LEG_PUSH_PULL)

#define UCCM_BOARD_LEDS_COUNT 8
#define UCCM_BOARD_BUTTONS (0, PA0, LEG_FLOAT)
#define UCCM_BOARD_BUTTONS_COUNT 1
#define UCCM_BOARD_MCU_FRECUENCY 72000000

#include "../leds.h"
#include "../buttons.h"

__Inline
void ucSetup_Board()
{
    ucSetup_BoardLEDs();
}

