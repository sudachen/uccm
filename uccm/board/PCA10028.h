
#pragma once
#include <uccm/uccm.h>
#include <uccm/./././nrf5sdk/nrf5_sdk_r12p2.h>

#pragma uccm board(pca10028)= -D_BOARD_FILE=PCA10028.h -DNRF51422 -DBOARD_PCA10028 -DNRF51
#pragma uccm softdevice(BLE)=> S130

#define UCCM_BOARD_MCU_FRECUENCY 16000000
#define UCCM_BOARD_LEDS (1,PO_21,LEG_OPEN_DRAIN), (2,PO_22,LEG_OPEN_DRAIN), (3,PO_23,LEG_OPEN_DRAIN), (4,PO_24,LEG_OPEN_DRAIN), NIL
#define UCCM_BOARD_LEDS_COUNT C_LIST_LENGTH(UCCM_BOARD_LEDS)

#define UCCM_BOARD_BUTTONS (1,PO_17,LEG_PULL_UP), (2,PO_18,LEG_PULL_UP), (3,PO_19,LEG_PULL_UP), (4,PO_20,LEG_PULL_UP), NIL
#define UCCM_BOARD_BUTTONS_COUNT C_LIST_LENGTH(UCCM_BOARD_BUTTONS)

#define UCCM_LL_INCLUDE(File) <uccm/nrf5sdk/nrf5_##File>
#include <uccm/nrf5sdk/nrf5_1422_xxac.h>

#include "../LED.h"
#include "../button.h"

#if SOFTDEVICE_PRESENT
#include <nrf_sdm.h>
#endif

extern void ucSoftDeviceFaultHandler(uint32_t id, uint32_t pc, uint32_t info);

__Inline
void ucSetup_Board()
{
#if SOFTDEVICE_PRESENT
    nrf_clock_lf_cfg_t const clock_lf_cfg =
    {.source        = NRF_CLOCK_LF_SRC_XTAL,
     .rc_ctiv       = 0,
     .rc_temp_ctiv  = 0,
     .xtal_accuracy = NRF_CLOCK_LF_XTAL_ACCURACY_20_PPM };
    __Assert_Nrf_Success sd_softdevice_enable(&clock_lf_cfg,ucSoftDeviceFaultHandler);
#else
    NRF_CLOCK->LFCLKSRC             = (CLOCK_LFCLKSRC_SRC_XTAL << CLOCK_LFCLKSRC_SRC_Pos);
    NRF_CLOCK->EVENTS_LFCLKSTARTED  = 0;
    NRF_CLOCK->TASKS_LFCLKSTART     = 1;
    while (NRF_CLOCK->EVENTS_LFCLKSTARTED == 0) (void)0;
#endif

    ucSetup_BoardLEDs();
    ucSetup_BoardButtons();
}
