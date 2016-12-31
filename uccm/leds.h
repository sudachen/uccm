
#pragma once

#include "uccm.h"
#include "leg.h"

typedef struct {
  UcLeg leg;
  uint8_t leg_opt; // UcOutputLegOpt
} UcLED;

__Forceinline
void ucSetOn_LED(const UcLED *led)
{
    ucSet_Leg(led->leg,(led->leg_opt!=LEG_OPEN_DRAIN)?1:0);
}

__Forceinline
void ucSetOff_LED(const UcLED *led)
{
    ucSet_Leg(led->leg,(led->leg_opt!=LEG_OPEN_DRAIN)?0:1);
}

#define ONE_LEDON(x) __Forceinline void C_CONCAT4(ucSet,On,_Board,C_GETITEM_0 x)(){ ucSet_Leg((C_GETITEM_1 x),((C_GETITEM_2 x)!=LEG_OPEN_DRAIN)?1:0); }
#define ONE_LEDOFF(x) __Forceinline void C_CONCAT4(ucSet,Off,_Board,C_GETITEM_0 x)(){ ucSet_Leg((C_GETITEM_1 x),((C_GETITEM_2 x)!=LEG_OPEN_DRAIN)?0:1); }

C_MAP(ONE_LEDON,C_SPACE,UCCM_BOARD_LEDS)
C_MAP(ONE_LEDOFF,C_SPACE,UCCM_BOARD_LEDS)

#undef ONE_LEDON
#undef ONE_LEDOFF

__Inline
void ucSetup_BoardLEDs()
{
#define SETUP_ONE_LED(x) ucSetup_OutputLeg(C_GETITEM_1 x,C_GETITEM_2 x);
    C_MAP(SETUP_ONE_LED,C_SEMICOLON,UCCM_BOARD_LEDS);
#undef SETUP_ONE_LED
}

