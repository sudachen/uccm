
#include <uccm/board.h>

//nrf_nvic_state_t nrf_nvic_state = {0};

// NTF weak callback
void app_error_fault_handler(uint32_t id, uint32_t pc, uint32_t info)
{
    ucFatalError();
}

void ucNrfErrorHandler(uint32_t err)
{
    ucError("NRF ERROR HANDLER \n\terror code %08x", $u(err));
    ucFatalError();
}

void ucSoftDeviceFaultHandler(uint32_t id, uint32_t pc, uint32_t info)
{
    ucFatalError();
}

extern void nfr5_support$successAssertFailed(uint32_t err, const char *file, int line)
{
    ucError("NRF ASSERT FAILED \n\tat %?:%?\n\terror code %08x", $s(file), $i(line), $u(err));
    ucFatalError();
}
