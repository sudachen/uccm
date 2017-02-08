
#pragma once

#include "../board.h" /* only for simantic code highlighting
                         really this file is included by board.h */
#include "../leg.h"   /* for the same purpose */


extern GPIO_TypeDef *const ucConst_GPIO_TABLE[]; // it's in ROM

__Inline
void setup_inputLeg(UcLeg leg, UcInputLegOpt opt)
{
    GPIO_InitTypeDef pinDef = {
        .Pin = 1 << (leg.leg_no & 0xf),
        .Mode = GPIO_MODE_INPUT,
        .Pull = opt == LEG_PULL_UP ? GPIO_PULLUP :
                opt == LEG_PULL_DOWN ? GPIO_PULLDOWN :
                GPIO_NOPULL,
        .Speed = GPIO_SPEED_FREQ_HIGH,
        .Alternate = 0
    };

    HAL_GPIO_Init(ucConst_GPIO_TABLE[leg.leg_no>>4],&pinDef);
}

__Inline
void setup_inputLegset(UcLeg firstLeg, UcInputLegOpt opt, unsigned count)
{
    GPIO_InitTypeDef pinDef = {
        .Pin = (0xffff>>(16-count)) << (firstLeg.leg_no & 0xf),
        .Mode = GPIO_MODE_INPUT,
        .Pull = opt == LEG_PULL_UP ? GPIO_PULLUP :
                opt == LEG_PULL_DOWN ? GPIO_PULLDOWN :
                GPIO_NOPULL,
        .Speed = GPIO_SPEED_FREQ_HIGH,
        .Alternate = 0
    };

    HAL_GPIO_Init(ucConst_GPIO_TABLE[firstLeg.leg_no>>4],&pinDef);
}

__Inline
void setup_outputLeg(UcLeg leg, UcOutputLegOpt opt)
{
    GPIO_InitTypeDef pinDef = {
        .Pin = 1 << (leg.leg_no & 0xf),
        .Mode = opt == LEG_OPEN_DRAIN ? GPIO_MODE_OUTPUT_OD : GPIO_MODE_OUTPUT_PP,
        .Pull = GPIO_NOPULL,
        .Speed = GPIO_SPEED_FREQ_HIGH,
        .Alternate = 0
    };

    HAL_GPIO_Init(ucConst_GPIO_TABLE[leg.leg_no>>4],&pinDef);
}

__Inline
void setup_outputLegset(UcLeg firstLeg, UcOutputLegOpt opt, unsigned count)
{
    GPIO_InitTypeDef pinDef = {
        .Pin = (0xffff>>(16-count)) << (firstLeg.leg_no & 0xf),
        .Mode = opt == LEG_OPEN_DRAIN ? GPIO_MODE_OUTPUT_OD : GPIO_MODE_OUTPUT_PP,
        .Pull = GPIO_NOPULL,
        .Speed = GPIO_SPEED_FREQ_HIGH,
        .Alternate = 0
    };

    HAL_GPIO_Init(ucConst_GPIO_TABLE[firstLeg.leg_no>>4],&pinDef);
}

__Inline
void setup_analogLeg(UcLeg leg)
{
    GPIO_InitTypeDef pinDef = {
        .Pin = 1 << (leg.leg_no & 0xf),
        .Mode = GPIO_MODE_ANALOG,
        .Pull = GPIO_NOPULL,
        .Speed = GPIO_SPEED_FREQ_HIGH,
        .Alternate = 0
    };

    HAL_GPIO_Init(ucConst_GPIO_TABLE[leg.leg_no>>4],&pinDef);
}

__Inline
bool get_leg(UcLeg leg)
{
    return (ucConst_GPIO_TABLE[leg.leg_no>>4]->IDR & (1<<(leg.leg_no&0xf))) != 0;
}

__Inline
uint16_t get_legset(UcLeg leg, unsigned count)
{
    uint16_t mask = (0xffff >> (16-count)) << (leg.leg_no&0xf);
    return (ucConst_GPIO_TABLE[leg.leg_no>>4]->IDR & mask) >> (leg.leg_no&0xf);
}

__Forceinline
void set_leg(UcLeg leg, bool value)
{
    if ( value )
        ucConst_GPIO_TABLE[leg.leg_no>>4]->BSRR = (1<<(leg.leg_no&0xf));
    else
        ucConst_GPIO_TABLE[leg.leg_no>>4]->BRR = (1<<(leg.leg_no&0xf));
}

__Forceinline
void toggle_leg(UcLeg leg)
{
    ucConst_GPIO_TABLE[leg.leg_no>>4]->ODR^=(1<<(leg.leg_no&0xf));
}

__Inline
void set_legset(UcLeg firstLeg, unsigned count, uint16_t value)
{
    uint16_t mask = (0xffff >> (16-count)) << (firstLeg.leg_no&0xf);
    value = mask & (value << (firstLeg.leg_no&0xf));
    GPIO_TypeDef *GPIOx = ucConst_GPIO_TABLE[firstLeg.leg_no>>4];
    GPIOx->ODR = (GPIOx->ODR & mask) | value;
}
