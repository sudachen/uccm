
#include <uccm/board.h>

void app_error_fault_handler(uint32_t id, uint32_t pc, uint32_t info)
{
    __disable_irq();
    PRINT_ERROR("NRF FAULT\n\tid=%?, pc=#%?, info=%?",$u(id),$x(pc),$u(info));
    on_fatalError();
}

void on_nrfError(uint32_t err)
{
    __disable_irq();
    PRINT_ERROR("NRF ERROR\n\terror code %08x", $u(err));
    on_fatalError();
}

extern void nfr5_support$successAssertFailed(uint32_t err, const char *file, int line)
{
    __disable_irq();
    PRINT_ERROR("NRF ASSERT FAILED\n\tat %?:%?\n\terror code %08x", $s(file), $i(line), $u(err));
    on_fatalError();
}

extern void nfr5_support$printError(uint32_t err, const char *file, int line)
{
    PRINT_ERROR("NRF PRINT ON FAIL\n\tat %?:%?\n\terror code %08x", $s(file), $i(line), $u(err));
}
