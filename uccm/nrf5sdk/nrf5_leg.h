
#pragma once

#include "../board.h" /* only for simantic code highlighting
                         really this file is included by board.h */
#include "../leg.h"   /* for the same purpose */

#include <nrf_gpio.h>

#define PO_00 PA0
#define PO_01 PA1
#define PO_02 PA2
#define PO_03 PA3
#define PO_04 PA4
#define PO_05 PA5
#define PO_06 PA6
#define PO_07 PA7
#define PO_08 PA8
#define PO_09 PA9
#define PO_10 PA10
#define PO_11 PA11
#define PO_12 PA12
#define PO_13 PA13
#define PO_14 PA14
#define PO_15 PA15
#define PO_16 PB0
#define PO_17 PB1
#define PO_18 PB2
#define PO_19 PB3
#define PO_20 PB4
#define PO_21 PB5
#define PO_22 PB6
#define PO_23 PB7
#define PO_24 PB8
#define PO_25 PB9
#define PO_26 PB10
#define PO_27 PB11
#define PO_28 PB12
#define PO_29 PB13
#define PO_30 PB14
#define PO_31 PB15

__Inline
void setup_inputLeg(UcLeg leg, UcInputLegOpt opt)
{
    if ( leg.leg_no < 32 )
        nrf_gpio_cfg(
                leg.leg_no,
                NRF_GPIO_PIN_DIR_INPUT,
                NRF_GPIO_PIN_INPUT_CONNECT,
                opt == LEG_PULL_UP ? NRF_GPIO_PIN_PULLUP :
                opt == LEG_PULL_DOWN ? NRF_GPIO_PIN_PULLDOWN :
                NRF_GPIO_PIN_NOPULL,
                NRF_GPIO_PIN_S0S1,
                NRF_GPIO_PIN_NOSENSE);
}

__Inline
void setup_inputLegset(UcLeg firstLeg, UcInputLegOpt opt, unsigned count)
{
    unsigned pin_no = firstLeg.leg_no;

    __Assert(count <= 16);

    for (; pin_no < 32 && count--; ++pin_no )
        nrf_gpio_cfg(
                pin_no,
                NRF_GPIO_PIN_DIR_INPUT,
                NRF_GPIO_PIN_INPUT_CONNECT,
                opt == LEG_PULL_UP ? NRF_GPIO_PIN_PULLUP :
                opt == LEG_PULL_DOWN ? NRF_GPIO_PIN_PULLDOWN :
                NRF_GPIO_PIN_NOPULL,
                NRF_GPIO_PIN_S0S1,
                NRF_GPIO_PIN_NOSENSE);
}

__Forceinline
void setup_outputLeg(UcLeg leg, UcOutputLegOpt opt)
{
    if ( leg.leg_no < 32 )
        nrf_gpio_cfg(
                leg.leg_no,
                NRF_GPIO_PIN_DIR_OUTPUT,
                NRF_GPIO_PIN_INPUT_DISCONNECT,
                NRF_GPIO_PIN_NOPULL,
                opt == LEG_OPEN_DRAIN ? NRF_GPIO_PIN_H0D1 : NRF_GPIO_PIN_S0S1,
                NRF_GPIO_PIN_NOSENSE);
}

__Forceinline
void setup_outputLegset(UcLeg firstLeg, UcOutputLegOpt opt, unsigned count)
{
    unsigned pin_no = firstLeg.leg_no;

    __Assert(count <= 16);

    for (; pin_no < 32 && count--; ++pin_no )
        nrf_gpio_cfg(
                pin_no,
                NRF_GPIO_PIN_DIR_OUTPUT,
                NRF_GPIO_PIN_INPUT_DISCONNECT,
                NRF_GPIO_PIN_NOPULL,
                opt == LEG_OPEN_DRAIN ? NRF_GPIO_PIN_H0D1 : NRF_GPIO_PIN_S0S1,
                NRF_GPIO_PIN_NOSENSE);
}

__Inline
void setup_analogLeg(UcLeg leg)
{
}

__Inline
bool get_leg(UcLeg leg)
{
    return (NRF_GPIO->IN & ((uint32_t)1<<(leg.leg_no&0x1f))) != 0;
}

__Inline
uint16_t get_legset(UcLeg leg, unsigned count)
{
    __Assert(count <= 16);

    uint32_t mask = (0xffffffff >> (32-count)) << (leg.leg_no&0x1f);
    return (uint16_t)((NRF_GPIO->IN & mask) >> (leg.leg_no&0x1f));
}

__Forceinline
void set_leg(UcLeg leg, bool value)
{
    if ( value )
        NRF_GPIO->OUTSET = ((uint32_t)1<<(leg.leg_no&0x1f));
    else
        NRF_GPIO->OUTCLR = ((uint32_t)1<<(leg.leg_no&0x1f));
}

__Forceinline
void toggle_leg(UcLeg leg)
{
    uint32_t state = NRF_GPIO->OUT;
    uint32_t mask = (uint32_t)1 << (leg.leg_no&0x1f);
    if ( state & mask )
        NRF_GPIO->OUTCLR = mask;
    else
        NRF_GPIO->OUTSET = mask;
}

__Inline
void set_legset(UcLeg firstLeg, unsigned count, uint16_t value)
{
    uint32_t mask = (0xffffffff >> (32-count)) << (firstLeg.leg_no&0x1f);
    uint32_t bits = (uint32_t)value << (firstLeg.leg_no&0x1f);

    __Assert(count <= 16);

    NRF_GPIO->OUTSET = bits & mask;
    NRF_GPIO->OUTCLR = ~bits & mask;
}
