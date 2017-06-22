
#pragma once

#include <uccm/board.h>

#ifdef __nRF5x_UC__
#include <app_util_platform.h>
#endif

#pragma uccm require(end) += {UCCM}/uccm/irq.c

typedef enum IrqPriority IrqPriority;

enum IrqPriority
{
    HIGH_PRIORITY_IRQ = 1,
    TIMER_PRIORITY_IRQ,
    LOW_PRIORITY_IRQ,
    APP_PRIORITY_IRQ,
};

#if defined __nRF5x_UC__ || defined __stm32Fx_MCU__
typedef IRQn_Type IrqNo;
#else
typedef int IrqNo;
#endif

void enable_irq(IrqNo irqNo,IrqPriority prio);
void disable_irq(IrqNo irqNo);

typedef struct IrqHandler IrqHandler;
struct IrqHandler
{
    void (*handler)(IrqHandler *self);
    IrqHandler *next;
};

extern const IrqHandler uccm_irq$Nil; // place it into ROM
#define IRQ_LIST_NIL ((IrqHandler*)&uccm_irq$Nil) // Yep, I know what I do

void register_irqHandler(struct IrqHandler *irq, IrqHandler **irqList);
void unregister_irqHandler(struct IrqHandler *irq, IrqHandler **irqList);
void handle_irq(struct IrqHandler *irqList);

