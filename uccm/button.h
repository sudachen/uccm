
#pragma once

#include "uccm.h"
#include "leg.h"

typedef struct {
  UcLeg leg;
  UcInputLegOpt leg_opt;
} UcButton;

__Static_Assert(sizeof(UcButton) == 2);

__Forceinline
bool ucGet_Button(UcButton btn)
{
    bool b = ucGet_Leg(btn.leg);
    return (btn.leg_opt == LEG_PULL_UP||btn.leg_opt == LEG_INVFLOAT)?!b:b;
}

__Inline
void ucSetup_BoardButtons()
{
#define SETUP_ONE_BTN(x,y) ucSetup_InputLeg(C_GETITEM_1 x,C_GETITEM_2 x);
    C_MAP(SETUP_ONE_BTN,C_SEMICOLON,UCCM_BOARD_BUTTONS);
#undef SETUP_ONE_BTN
}

#define GET_ONE_BTN(x,y) __Forceinline bool C_CONCAT2(ucGet_BoardButton_,C_GETITEM_0 x)(){ return ucGet_Button((UcButton){(C_GETITEM_1 x),(C_GETITEM_2 x)}); }

C_MAP(GET_ONE_BTN,C_SPACE,UCCM_BOARD_BUTTONS)

#undef GET_ONE_BTN

__Inline
bool ucGet_BoardButton(uint8_t no)
{
    switch(no)
    {
#define GET_ONE_BTN_OF(x,y) case y: return ucGet_Button((UcButton){(C_GETITEM_1 x),(C_GETITEM_2 x)});
    C_MAP(GET_ONE_BTN_OF,C_SEMICOLON,UCCM_BOARD_BUTTONS);
#undef  GET_ONE_BTN_OF
    default:
        return false;
    }
}
