
#pragma once

#include "../board.h" /* only for simantic code highlighting
                         really this file is included by board.h */
#include "../leg.h"   /* for the same purpose */


extern GPIO_TypeDef *const ucConst_GPIO_TABLE[]; // it's in ROM

__Inline
void ucSetup_InputLeg(UcLeg leg, UcInputLegOpt opt)
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
void ucSetup_InputLegset(UcLeg first_leg, UcInputLegOpt opt, unsigned count)
{
    GPIO_InitTypeDef pinDef = {
        .Pin = (0xffff>>(16-count)) << (first_leg.leg_no & 0xf),
        .Mode = GPIO_MODE_INPUT,
        .Pull = opt == LEG_PULL_UP ? GPIO_PULLUP :
                opt == LEG_PULL_DOWN ? GPIO_PULLDOWN :
                GPIO_NOPULL,
        .Speed = GPIO_SPEED_FREQ_HIGH,
        .Alternate = 0
    };

    HAL_GPIO_Init(ucConst_GPIO_TABLE[first_leg.leg_no>>4],&pinDef);
}

__Inline
void ucSetup_OutputLeg(UcLeg leg, UcOutputLegOpt opt)
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
void ucSetup_OutputLegset(UcLeg first_leg, UcOutputLegOpt opt, unsigned count)
{
    GPIO_InitTypeDef pinDef = {
        .Pin = (0xffff>>(16-count)) << (first_leg.leg_no & 0xf),
        .Mode = opt == LEG_OPEN_DRAIN ? GPIO_MODE_OUTPUT_OD : GPIO_MODE_OUTPUT_PP,
        .Pull = GPIO_NOPULL,
        .Speed = GPIO_SPEED_FREQ_HIGH,
        .Alternate = 0
    };

    HAL_GPIO_Init(ucConst_GPIO_TABLE[first_leg.leg_no>>4],&pinDef);
}

__Inline
void ucSetup_Analog(UcLeg leg)
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
bool ucGet_Leg(UcLeg leg)
{
    return ucConst_GPIO_TABLE[leg.leg_no>>4]->IDR & (1<<(leg.leg_no&0xf)) != 0;
}

__Inline
uint16_t ucGet_Legset(UcLeg leg, unsigned count)
{
    uint16_t mask = (0xffff >> (16-count)) << (leg.leg_no&0xf);
    return (ucConst_GPIO_TABLE[leg.leg_no>>4]->IDR & mask) >> (leg.leg_no&0xf);
}

__Forceinline
void ucSet_Leg(UcLeg leg, bool value)
{
    if ( value )
        ucConst_GPIO_TABLE[leg.leg_no>>4]->BSRR = (1<<(leg.leg_no&0xf));
    else
        ucConst_GPIO_TABLE[leg.leg_no>>4]->BRR = (1<<(leg.leg_no&0xf));
}

__Forceinline
void ucToggle_Leg(UcLeg leg)
{
    ucConst_GPIO_TABLE[leg.leg_no>>4]->ODR^=(1<<(leg.leg_no&0xf));
}

__Inline
void ucSet_Legset(UcLeg first_leg, unsigned count, uint16_t value)
{
    uint16_t mask = (0xffff >> (16-count)) << (first_leg.leg_no&0xf);
    value = mask & (value << (first_leg.leg_no&0xf));
    GPIO_TypeDef *GPIOx = ucConst_GPIO_TABLE[first_leg.leg_no>>4];
    GPIOx->ODR = (GPIOx->ODR & mask) | value;
}
