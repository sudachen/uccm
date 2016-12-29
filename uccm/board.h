
#pragma once
#include "uccm.h"

#define UCCM_BOARD_INCLUDE_(File) _EVAL(<uccm/board/File.h>)
#define UCCM_BOARD_INCLUDE(File) UCCM_BOARD_INCLUDE_(File)

#if defined _BOARD
#include UCCM_BOARD_INCLUDE(_BOARD)
#else
#include "myboard/myboard.h"
#endif

