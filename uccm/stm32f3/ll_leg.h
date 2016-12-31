
#pragma once

#include "../board.h"
#include "../leg.h"

__Inline
void ucSetup_InputLeg(UcLeg leg, UcInputLegOpt opt)
{
}

__Inline
void ucSetup_OutputLeg(UcLeg leg, UcOutputLegOpt opt)
{
}

__Inline
void ucSetup_Analog(UcLeg leg)
{
}

__Forceinline
bool ucGet_Leg(UcLeg leg)
{
    return false;
}

__Forceinline
void ucSet_Leg(UcLeg leg, bool val)
{
}

__Forceinline
bool ucToggle_Leg(UcLeg leg)
{
    return false;
}
