
#pragma once

#include "uccm.h"
#include "leg.h"

typedef struct {
  UcLeg leg;
  UcOutputLegOpt leg_opt;
} UcLED;

__Static_Assert(sizeof(UcLED)==2);

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

#define ONE_LEDON(x,y) __Forceinline void C_CONCAT2(ucSetOn_BoardLED_,C_GETITEM_0 x)(){ ucSet_Leg((C_GETITEM_1 x),((C_GETITEM_2 x)!=LEG_OPEN_DRAIN)?1:0); }
#define ONE_LEDOFF(x,y) __Forceinline void C_CONCAT2(ucSetOff_BoardLED_,C_GETITEM_0 x)(){ ucSet_Leg((C_GETITEM_1 x),((C_GETITEM_2 x)!=LEG_OPEN_DRAIN)?0:1); }
#define ONE_LEDTOGGLE(x,y) __Forceinline void C_CONCAT2(ucToggle_BoardLED_,C_GETITEM_0 x)(){ ucToggle_Leg((C_GETITEM_1 x)); }

C_MAP(ONE_LEDON,C_SPACE,UCCM_BOARD_LEDS)
C_MAP(ONE_LEDOFF,C_SPACE,UCCM_BOARD_LEDS)
C_MAP(ONE_LEDTOGGLE,C_SPACE,UCCM_BOARD_LEDS)

#undef ONE_LEDON
#undef ONE_LEDOFF
#undef ONE_LEDTOGGLE

__Inline
void ucSetup_BoardLEDs()
{
#define SETUP_ONE_LED(x,y) ucSetup_OutputLeg(C_GETITEM_1 x,C_GETITEM_2 x);
    C_MAP(SETUP_ONE_LED,C_SEMICOLON,UCCM_BOARD_LEDS);
#undef SETUP_ONE_LED
}

__Inline
void ucSetOn_BoardLED(uint8_t no)
{
    switch(no)
    {
#define SETON_ONE_LED(x,y) case y: ucSet_Leg((C_GETITEM_1 x),((C_GETITEM_2 x)!=LEG_OPEN_DRAIN)?1:0); break;
    C_MAP(SETON_ONE_LED,C_SEMICOLON,UCCM_BOARD_LEDS);
#undef  SETON_ONE_LED
    default:;
    }
}

__Inline
void ucSetOff_BoardLED(uint8_t no)
{
    switch(no)
    {
#define SETON_ONE_LED(x,y) case y: ucSet_Leg((C_GETITEM_1 x),((C_GETITEM_2 x)!=LEG_OPEN_DRAIN)?0:1); break;
    C_MAP(SETON_ONE_LED,C_SEMICOLON,UCCM_BOARD_LEDS);
#undef  SETON_ONE_LED
    default:;
    }
}

__Inline
void ucToggle_BoardLED(uint8_t no)
{
    switch(no)
    {
#define TOGGLE_ONE_LED(x,y) case y: ucToggle_Leg((C_GETITEM_1 x)); break;
    C_MAP(TOGGLE_ONE_LED,C_SEMICOLON,UCCM_BOARD_LEDS);
#undef  TOGGLE_ONE_LED
    default:;
    }
}
