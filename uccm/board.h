
#pragma once
#include "uccm.h"

#define UCCM_BOARD_INCLUDE_S(BoardFile) #BoardFile
#define UCCM_BOARD_INCLUDE_(File) UCCM_BOARD_INCLUDE_S(board/File)
#define UCCM_BOARD_INCLUDE(File) UCCM_BOARD_INCLUDE_(File)

#if defined _BOARD_FILE
#include UCCM_BOARD_INCLUDE(_BOARD_FILE)
#else
#error you have to specify -D_BOARD_FILE=boardfile.h from uccm/board or from directory board in current project root
#endif

