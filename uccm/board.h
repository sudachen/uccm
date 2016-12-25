
#pragma once
#include "uccm.h"

#define UCCM_BOARD_INCLUDE(File) _EVAL(_STR(board/File##.h))

#if defined _BOARD
#include UCCM_BOARD_INCLUDE(_BOARD)
#else
#include "myboard/myboard.h"
#endif

