
#pragma uccm alias(CMSIS) = [cubefx_fw_f3]/Drivers/CMSIS
#pragma uccm alias(STM32F3XX) = [cubefx_fw_f3]/Drivers/CMSIS/Device/ST/STM32F3xx
#pragma uccm alias(STM32HAL) = [cubefx_fw_f3]/Drivers/STM32F3xx_HAL_Driver

#pragma uccm default(debugger)= stlink

#pragma uccm xcflags(*)+= -I[@inc] -I "{CMSIS}/Include" -I "{STM32F3XX}/Include" -I "{STM32HAL}/Inc"
