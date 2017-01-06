
#pragma once
#include <uccm/uccm.h>
#include <uccm/./././nrf5sdk/nrf5_sdk_r12p2.h>

#pragma uccm alias(NRF_DEVICE) = [nrf5sdk12+]/components/device
#pragma uccm alias(NRF_LIBRARIES) = [nrf5sdk12+]/components/libraries
#pragma uccm alias(NRF_DRIVERS) = [nrf5sdk12+]/components/drivers_nrf
#pragma uccm alias(NRF_BLE) = [nrf5sdk12+]/components/ble
#pragma uccm alias(SOFTDEVICE) = [nrf5sdk12+]/components/softdevice
#pragma uccm alias(TOOLCHAIN) = [nrf5sdk12+]/components/toolchain

#pragma uccm default(vendorware)= BLE
#pragma uccm default(debugger)= nrfjprog
#pragma uccm debugger(nrfjprog)+= -f NRF51
#pragma uccm vendorware(BLE)= {SOFTDEVICE}/130/hex/s130_nrf51_2.0.1_softdevice.hex
#pragma uccm board(pca10028)= -D_BOARD_FILE=PCA10028.h -DNRF51422 -DBOARD_PCA10028 -DNRF51
#pragma uccm xcflags(BLE)+= -DSOFTDEVICE_PRESENT -DS130
#pragma uccm xcflags(gcc)+= -I "{TOOLCHAIN}/gcc"
#pragma uccm xcflags(armcc)+= -I "{TOOLCHAIN}/arm"
#pragma uccm xcflags(*)+= -I[@inc] \
    -I "{NRF_DEVICE}" \
    -I "{TOOLCHAIN}/cmsis/Include" \
    -I "{TOOLCHAIN}" \
    -I "{SOFTDEVICE}/130/headers" \
    -I "{SOFTDEVICE}/130/headers/nrf51" \
    -I "{SOFTDEVICE}/common/softdevice_handler" \
    -I "{NRF_DRIVERS}/hal" \

#define UCCM_BOARD_MCU_FRECUENCY 16000000
#define UCCM_BOARD_LEDS (1,PO_21,LEG_OPEN_DRAIN), (2,PO_22,LEG_OPEN_DRAIN), (3,PO_23,LEG_OPEN_DRAIN), (4,PO_24,LEG_OPEN_DRAIN), NIL
#define UCCM_BOARD_LEDS_COUNT C_LIST_LENGTH(UCCM_BOARD_LEDS)

#define UCCM_BOARD_BUTTONS (1,PO_17,LEG_PULL_UP), (2,PO_18,LEG_PULL_UP), (3,PO_19,LEG_PULL_UP), (4,PO_20,LEG_PULL_UP), NIL
#define UCCM_BOARD_BUTTONS_COUNT C_LIST_LENGTH(UCCM_BOARD_BUTTONS)

#define UCCM_BOARD_HAS_I2C_BUTTONS
#define UCCM_BOARD_HAS_I2C_LEDS

#define UCCM_LL_INCLUDE(File) <uccm/nrf5sdk/nrf5_##File>
#include <uccm/nrf5sdk/nrf5_1422_xxac.h>

#include "../LED.h"
#include "../button.h"

void ucSetup_Board()
{
    ucSetup_BoardLEDs();
    ucSetup_BoardButtons();
}
