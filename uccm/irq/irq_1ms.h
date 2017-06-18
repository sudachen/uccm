
/*
 *
 * Stm32Fx HAL sets 1ms interval for SysTick timer by default,
 *   please do not change this behaviour!
 *
 */


#pragma once
#include <uccm/board.h>
#include "irq.h"

#ifdef __nRF5x_UC__
#include <app_util_platform.h>
#include <app_timer.h>
#define APP_TIMER_PRESCALER 32
#endif

#pragma uccm require(end) += {UCCM}/uccm/irq/irq_1ms.c

extern IrqHandler *IRQlist_1ms; // virtual IRQ signalled every 1ms
#define TIMED_IRQ IRQlist_1ms

void register_1msHandler(struct IrqHandler *irq);
void unregister_1msHandler(struct IrqHandler *irq);
