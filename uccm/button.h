
#pragma once

#include "uccm.h"
#include "leg.h"

typedef struct {
  UcLeg leg;
  UcInputLegOpt leg_opt;
} UcButton;

__Static_Assert(sizeof(UcButton) == 2);

__Forceinline
bool get_button(UcButton btn)
{
    bool b = get_leg(btn.leg);
    return (btn.leg_opt == LEG_PULL_UP||btn.leg_opt == LEG_INVFLOAT)?!b:b;
}

__Inline
void setup_boardButtons()
{
#define SETUP_ONE_BTN(x,y) setup_inputLeg(C_GETITEM_1 x,C_GETITEM_2 x);
    C_MAP(SETUP_ONE_BTN,C_SEMICOLON,UCCM_BOARD_BUTTONS);
#undef SETUP_ONE_BTN
}

#define GET_ONE_BTN(x,y) __Forceinline bool C_CONCAT2(get_boardButton_,C_GETITEM_0 x)(){ return get_button((UcButton){(C_GETITEM_1 x),(C_GETITEM_2 x)}); }

C_MAP(GET_ONE_BTN,C_SPACE,UCCM_BOARD_BUTTONS)

#undef GET_ONE_BTN

#define ENUM_ONE_BTN(x,y) C_CONCAT2(BOARD_BUTTON_,C_GETITEM_0 x) = y

enum BoardButtons
{
C_MAP(ENUM_ONE_BTN,C_COMMA,UCCM_BOARD_BUTTONS)
};
#undef ENUM_ONE_BTN

__Inline
bool get_boardButton(uint8_t no)
{
    switch(no)
    {
#define GET_ONE_BTN_OF(x,y) case y: return get_button((UcButton){(C_GETITEM_1 x),(C_GETITEM_2 x)});
    C_MAP(GET_ONE_BTN_OF,C_SEMICOLON,UCCM_BOARD_BUTTONS);
#undef  GET_ONE_BTN_OF
    default:
        return false;
    }
}
