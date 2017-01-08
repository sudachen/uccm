
#pragma uccm home(nrf5sdk12)= %NRF5_SDK|NRF5_SDK_12%

#pragma uccm alias(NRF_DEVICE) = [nrf5sdk12]/components/device
#pragma uccm alias(NRF_LIBRARIES) = [nrf5sdk12]/components/libraries
#pragma uccm alias(NRF_DRIVERS) = [nrf5sdk12]/components/drivers_nrf
#pragma uccm alias(NRF_BLE) = [nrf5sdk12]/components/ble
#pragma uccm alias(SOFTDEVICE) = [nrf5sdk12]/components/softdevice
#pragma uccm alias(TOOLCHAIN) = [nrf5sdk12]/components/toolchain

#pragma uccm home(nrf5sdk10)= %NRF5_SDK_10%
#pragma uccm alias(SOFTDEVICE10) = [nrf5sdk10]/components/softdevice

#pragma uccm default(softdevice)= BLE
#pragma uccm default(debugger)= nrfjprog
#pragma uccm debugger(nrfjprog)+= -f NRF51
#pragma uccm xcflags(gcc)+= -I "{TOOLCHAIN}/gcc"
#pragma uccm xcflags(armcc)+= -I "{TOOLCHAIN}/arm"
#pragma uccm xcflags(*)+= -I[@inc] \
    -I "{NRF_DEVICE}" \
    -I "{TOOLCHAIN}/cmsis/Include" \
    -I "{TOOLCHAIN}" \
    -I "{NRF_LIBRARIES}/util" \
    -I "{NRF_DRIVERS}/hal" \

#pragma uccm softdevice(S130)= {SOFTDEVICE}/s130/hex/s130_nrf51_2.0.1_softdevice.hex
#pragma uccm xcflags(S130)+= -DSOFTDEVICE_PRESENT -DS130 \
    -I "{SOFTDEVICE}/s130/headers" \
    -I "{SOFTDEVICE}/s130/headers/nrf51" \
    -I "{SOFTDEVICE}/common/softdevice_handler" \

#pragma uccm softdevice(S132)= {SOFTDEVICE}/s132/hex/s132_nrf52_3.0.0_softdevice.hex
#pragma uccm xcflags(S132)+= -DSOFTDEVICE_PRESENT -DS132 \
    -I "{SOFTDEVICE}/s132/headers" \
    -I "{SOFTDEVICE}/s132/headers/nrf52" \
    -I "{SOFTDEVICE}/common/softdevice_handler" \

#pragma uccm softdevice(S110)= {SOFTDEVICE10}/s110/hex/s110_nrf51_8.0.0_softdevice.hex
#pragma uccm xcflags(S110)+= -DSOFTDEVICE_PRESENT -DS110 \
    -I "{SOFTDEVICE10}/s110/headers" \
    -I "{SOFTDEVICE10}/common/softdevice_handler" \

#pragma uccm file(sdk_config.h) += \n\
#pragma once\n\
#ifndef SDK_CONFIG_H\n\
#define SDK_CONFIG_H\n\
#ifndef BLE_ADVERTISING_ENABLED\n\
#define BLE_ADVERTISING_ENABLED\n\
#endif \n\
#endif /*SDK_CONFIG_H*/\n
