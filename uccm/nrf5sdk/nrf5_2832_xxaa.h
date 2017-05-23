
#pragma once
#include "../uccm.h"

#ifdef NRF52832

#ifndef NRF52
#define NRF52
#endif

#define __nRF5x_UC__ 0x52832aa0
#include "nrf5_2.h"

#pragma uccm debugger(jrttview)+= -d NRF52832_XXAA
#pragma uccm let(ROM_BASE)= 0x00000000
#pragma uccm let(ROM_SIZE)= 0x00080000
#pragma uccm let(RAM_BASE)= 0x20000000
#pragma uccm let(RAM_SIZE)= 0x00010000

#endif // NRF52832

