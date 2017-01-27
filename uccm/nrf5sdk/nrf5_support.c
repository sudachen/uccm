
#include <uccm/board.h>

nrf_nvic_state_t nrf_nvic_state = {0};

void ucSoftDeviceFaultHandler(uint32_t id, uint32_t pc, uint32_t info)
{
    ucFatalError(UC_ERROR_IN_SOFTDEVICE);
}

extern void nfr5_support$successAssertFailed(uint32_t err, const char *file, int line)
{
    ucError("NRF ASSERT FAILED \n\tat %?:%?\n\terror code %08x", $s(file), $i(line), $u(err));
    ucFatalError(UC_ERROR_IN_ASSERT);
}
