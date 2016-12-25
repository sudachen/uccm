
#pragma once
#include "../uccm.h" /* for code stupied hilighters, this file is included from uccm.h */

#define UCCM_LL_INCLUDE(File) _EVAL(_STR(nrf51/ll_##File))

#define UCCM_BOARD_CRYSTAL_MHZ 16
#undef  UCCM_BOARD_CRYSTAL_BYPASS  

