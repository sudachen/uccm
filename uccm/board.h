
#pragma once
#include <uccm/uccm.h>

#define UCCM_BOARD_INCLUDE(File) C_STR(board/File)

#if defined _BOARD_FILE
#include UCCM_BOARD_INCLUDE(_BOARD_FILE)
#else
#error you have to specify -D_BOARD_FILE=boardfile.h from uccm/board or from directory board in current project root
#endif

#pragma uccm let(HEAP_SIZE)?= 0
#pragma uccm let(STACK_SIZE)?= 0x400

#pragma uccm cflags+= -D__STACK_SIZE={$STACK_SIZE} -D__HEAP_SIZE={$HEAP_SIZE}
#ifdef __keil_v5
#pragma uccm asflags+= --pd "__STACK_SIZE SETA {$STACK_SIZE}" --pd "__HEAP_SIZE SETA {$HEAP_SIZE}"
#endif
