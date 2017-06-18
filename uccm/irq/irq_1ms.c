
#include "irq.h"
#include "irq_1ms.h"

#ifdef __nRF5x_UC__
#include <nrf_delay.h>
#endif

IrqHandler *IRQlist_1ms = IRQ_LIST_NIL;

#ifdef __nRF5x_UC__

bool uccm_irq$RtcIsStarted = false;

void RTC1_IRQHandler(void)
{
    NRF_RTC1->EVENTS_TICK = 0;
    NRF_RTC1->EVENTS_OVRFLW = 0;
    handle_irq(TIMED_IRQ);
    //__SEV();
}

#endif

#ifdef __nRF5x_UC__
void uccm_irq$startRTC1()
{
    if ( uccm_irq$RtcIsStarted ) return;

    NRF_RTC1->PRESCALER = APP_TIMER_PRESCALER;
    NRF_RTC1->INTENSET = RTC_INTENSET_TICK_Msk;
    NRF_RTC1->EVTENSET = RTC_EVTENSET_TICK_Msk;
    NRF_RTC1->TASKS_CLEAR = 1;
    NRF_RTC1->TASKS_START = 1;
    nrf_delay_us(47);

    enable_irq(RTC1_IRQn,HIGH_PRIORITY_IRQ);
    uccm_irq$RtcIsStarted = true;
}

void uccm_irq$stopRTC1()
{
    NRF_RTC1->INTENCLR = RTC_INTENSET_TICK_Msk;
    NRF_RTC1->EVTENCLR = RTC_EVTENSET_TICK_Msk;
    NRF_RTC1->TASKS_STOP = 1;
    nrf_delay_us(47);

    NRF_RTC1->TASKS_CLEAR = 1;
    disable_irq(RTC1_IRQn);
    uccm_irq$RtcIsStarted = false;
}
#endif

#ifdef __stm32Fx_UC__
// it uses raw SysTick_Handler in HAL
void on_sysTick1ms()
{
    handle_irq(TIMED_IRQ);
}
#endif

void register_1msHandler(IrqHandler *irq)
{
    register_irqHandler(irq, &IRQlist_1ms);
#ifdef __nRF5x_UC__
    if ( IRQlist_1ms != IRQ_LIST_NIL && !uccm_irq$RtcIsStarted )
        uccm_irq$startRTC1();
#endif
}

void unregister_1msHandler(IrqHandler *irq)
{
    unregister_irqHandler(irq, &IRQlist_1ms);
#ifdef __nRF5x_UC__
    if ( IRQlist_1ms == IRQ_LIST_NIL && uccm_irq$RtcIsStarted )
        uccm_irq$stopRTC1();
#endif
}
