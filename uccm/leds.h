
#pragma once

#include "uccm.h"
#include "gpio.h"

__Forceinline
void board_set_led_on(uccm_leg_t leg, uccm_gpio_output_t leg_opt)
{
    gpio_set(leg,(leg_opt!=LEG_OPEN_DRAIN)?1:0);
}

__Forceinline
void board_set_led_off(uccm_leg_t leg, uccm_gpio_output_t leg_opt)
{
    gpio_set(leg,(leg_opt!=LEG_OPEN_DRAIN)?0:1);
}

#define VOID_BOARD_SET_LED_(N,Act) __Forceinline void board_set_led##N##_##Act()
#define VOID_BOARD_SET_LED(N,Act) VOID_BOARD_SET_LED_(N,Act)

#define ONE_LEDON_DEFINE(x) VOID_BOARD_LED(_GETITEM_0 x, _on) { board_set_led_on(_GETITEM_1 x,_GETITEM_2 x);  }
_MAP(ONE_LEDON_DEFINE,_SPACE,UCCM_BOARD_LEDS)
#undef ONE_LEDON_DEFINE

#define ONE_LEDOFF_DEFINE(x) VOID_BOARD_LED(_GETITEM_0 x, _off) { board_set_led_off(_GETITEM_1 x,_GETITEM_2 x);  }
_MAP(ONE_LEDOFF_DEFINE,_SPACE,UCCM_BOARD_LEDS)
#undef ONE_LEDOFF_DEFINE

#undef VOID_BOARD_SET_LED_
#undef VOID_BOARD_SET_LED

__Inline
void board_setup_leds()
{
#define SETUP_ONE_LED(x) gpio_setup_output(_GETITEM_1 x,_GETITEM_2 x);
    _MAP(SETUP_ONE_LED,_SEMICOLON,UCCM_BOARD_LEDS);
#undef SETUP_ONE_LED
}

