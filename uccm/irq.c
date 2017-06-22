
#include <uccm/irq.h>

const IrqHandler uccm_irq$Nil = { NULL, NULL };

void handle_irq(IrqHandler *irqList)
{
    IrqHandler *irqPtr = irqList;
    while (irqPtr != IRQ_LIST_NIL)
    {
        IrqHandler *p = irqPtr;
        irqPtr = irqPtr->next;
        if ( p->handler != NULL ) p->handler(p);
    }
}

__Forceinline
int uccm_irq$prio(IrqPriority prio)
{
#ifdef __nRF5x_UC__
    switch(prio) {
        case HIGH_PRIORITY_IRQ: return APP_IRQ_PRIORITY_HIGH;
        case TIMER_PRIORITY_IRQ:return  APP_IRQ_PRIORITY_MID;
        case LOW_PRIORITY_IRQ:  return  APP_IRQ_PRIORITY_LOW;
        case APP_PRIORITY_IRQ:  /* falldown */
        default:                return  APP_IRQ_PRIORITY_THREAD;
    }
#else
    switch(prio) {
        case HIGH_PRIORITY_IRQ: return 0;
        case TIMER_PRIORITY_IRQ:return 2;
        case LOW_PRIORITY_IRQ:  return 3;
        case APP_PRIORITY_IRQ:  /* falldown */
        default:                return 4;
    }
#endif
}

void enable_irq(IrqNo irqNo,IrqPriority prio)
{
    int realPrio = uccm_irq$prio(prio);
    __Assert(prio >= HIGH_PRIORITY_IRQ && prio <= APP_PRIORITY_IRQ);

#if defined __nRF5x_UC__ && defined SOFTDEVICE_PRESENT
    __Assert_Success sd_nvic_SetPriority(irqNo,realPrio);
    __Assert_Success sd_nvic_ClearPendingIRQ(irqNo);
    __Assert_Success sd_nvic_EnableIRQ(irqNo);
#else
    NVIC_SetPriority(irqNo,realPrio);
    NVIC_ClearPendingIRQ(irqNo);
    NVIC_EnableIRQ(irqNo);
#endif
}

void disable_irq(IrqNo irqNo)
{
#if defined __nRF5x_UC__ && defined SOFTDEVICE_PRESENT
    __Assert_Success sd_nvic_DisableIRQ(irqNo);
#else
    NVIC_DisableIRQ(irqNo);
#endif
}

void register_irqHandler(IrqHandler *irq, IrqHandler **irqList)
{
    __Critical
    {
        if ( irq->next != NULL ) return;
        irq->next = *irqList;
        *irqList = irq;
    }
}

void unregister_irqHandler(IrqHandler *irq, IrqHandler **irqList)
{
    IrqHandler **ptr = irqList;
    if ( irq->next == NULL ) return;

    __Critical
    {
        while ( *ptr != IRQ_LIST_NIL && *ptr != irq ) ptr = &(*ptr)->next;

        if ( *ptr != IRQ_LIST_NIL )
        {
            *ptr = (*ptr)->next;
            irq->next = 0;
        }
    }

}
