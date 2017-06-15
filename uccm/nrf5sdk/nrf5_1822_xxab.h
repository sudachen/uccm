
#pragma once
#include "../uccm.h"

#ifdef NRF51822

#ifndef NRF51
#define NRF51
#endif

#define __nRF5x_UC__ 0x51822ab0

#include "nrf5_1.h"

#pragma uccm debugger(jrttview)+= -d NRF51822_XXAB
#pragma uccm let(ROM_BASE)= 0x00000000
#pragma uccm let(ROM_SIZE)= 0x00020000
#pragma uccm let(RAM_BASE)= 0x20000000
#pragma uccm let(RAM_SIZE)= 0x00004000

#endif // NRF51822
