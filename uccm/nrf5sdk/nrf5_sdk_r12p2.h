
#include <uccm/uccm.h>
#include <nrf_error.h>
#include <nrf_nvic.h>

#pragma uccm alias(NRF_DEVICE) = [nrf5sdk12]/components/device
#pragma uccm alias(NRF_LIBRARIES) = [nrf5sdk12]/components/libraries
#pragma uccm alias(NRF_DRIVERS) = [nrf5sdk12]/components/drivers_nrf
#pragma uccm alias(NRF_BLE) = [nrf5sdk12]/components/ble
#pragma uccm alias(SOFTDEVICE) = [nrf5sdk12]/components/softdevice
#pragma uccm alias(TOOLCHAIN) = [nrf5sdk12]/components/toolchain
#pragma uccm alias(SOFTDEVICE10) = [nrf5sdk10]/components/softdevice

#ifdef _DEBUG
#pragma uccm cflags+= -DDEBUG_NRF
#endif

#ifdef SOFTDEVICE_PRESENT
#pragma uccm require(module)+= {SOFTDEVICE}/common/softdevice_handler/softdevice_handler.c
#endif

#pragma uccm require(module)+= {NRF_LIBRARIES}/util/app_util_platform.c
#pragma uccm require(module)+= {NRF_LIBRARIES}/util/app_error.c
#pragma uccm require(module)+= {NRF_LIBRARIES}/util/nrf_assert.c

#pragma uccm let(HEAP_SIZE)?= 0
#ifdef SOFTDEVICE_PRESENT
#if defined S130 || defined S132
#pragma uccm let(STACK_SIZE)?= 0x800
#else
#error have no stack size for this softdevice
#endif
#else
#pragma uccm let(STACK_SIZE)?= 0x400
#endif

#ifdef SOFTDEVICE_PRESENT
#ifdef S130
#pragma uccm let(ROM_APP_BASE)= 0x1b000
#pragma uccm let(RAM_APP_BASE)?= 0x01fe8
#else
#error have no memory layout for this softdevice
#endif
#else
#pragma uccm let(ROM_APP_BASE)= 0
#pragma uccm let(RAM_APP_BASE)?= 0
#endif

#pragma uccm default(softdevice)= BLE
#pragma uccm default(debugger)= nrfjprog
#pragma uccm debugger(jrttview)+= -ct usb -speed 4000 -a -if swd

#pragma uccm xcflags(gcc)+= -I "{TOOLCHAIN}/gcc"
#pragma uccm xcflags(armcc)+= -I "{TOOLCHAIN}/arm"
#pragma uccm xcflags(*)+= -I[@inc] \
    -I "{UCCM}/uccm/nrf5sdk/cfg1" \
    -I "{NRF_DEVICE}" \
    -I "{TOOLCHAIN}/cmsis/Include" \
    -I "{TOOLCHAIN}" \
    -I "{NRF_LIBRARIES}/util" \
    -I "{NRF_LIBRARIES}/log" \
    -I "{NRF_LIBRARIES}/log/src" \
    -I "{NRF_LIBRARIES}/fstorage" \
    -I "{NRF_LIBRARIES}/timer" \
    -I "{NRF_LIBRARIES}/experimental_section_vars" \
    -I "{NRF_DRIVERS}/hal" \
    -I "{NRF_DRIVERS}/delay" \

#pragma uccm softdevice(S130)= {SOFTDEVICE}/s130/hex/s130_nrf51_2.0.1_softdevice.hex
#pragma uccm xcflags(S130)+= -DSOFTDEVICE_PRESENT -DS130 -DBLE_STACK_SUPPORT_REQD \
    -I "{SOFTDEVICE}/s130/headers" \
    -I "{SOFTDEVICE}/s130/headers/nrf51" \
    -I "{SOFTDEVICE}/common/softdevice_handler" \
    -DNRF_SD_BLE_API_VERSION=2 \

#pragma uccm softdevice(S132)= {SOFTDEVICE}/s132/hex/s132_nrf52_3.0.0_softdevice.hex
#pragma uccm xcflags(S132)+= -DSOFTDEVICE_PRESENT -DS132 -DBLE_STACK_SUPPORT_REQD \
    -I "{SOFTDEVICE}/s132/headers" \
    -I "{SOFTDEVICE}/s132/headers/nrf52" \
    -I "{SOFTDEVICE}/common/softdevice_handler" \
    -DNRF_SD_BLE_API_VERSION=3 \

#pragma uccm softdevice(S110)= {SOFTDEVICE10}/s110/hex/s110_nrf51_8.0.0_softdevice.hex
#pragma uccm xcflags(S110)+= -DSOFTDEVICE_PRESENT -DS110 -DBLE_STACK_SUPPORT_REQD\
    -I "{SOFTDEVICE10}/s110/headers" \
    -I "{SOFTDEVICE10}/common/softdevice_handler" \

#pragma uccm file(sdk_config_1.h) += \n\
#pragma once\n

#pragma uccm require(source) += {UCCM}/uccm/nrf5sdk/nrf5_support.c

extern void on_nrfError(uint32_t error);
extern void nfr5_support$successAssertFailed(uint32_t err, const char *file, int line);

__Forceinline
void nrf5_support$checkSuccess(uint32_t err, const char *file, int line)
{
    if ( err != NRF_SUCCESS )
        nfr5_support$successAssertFailed(err,file,line);
}

__Forceinline
void nrf5_support$checkSuccessIfSupported(uint32_t err, const char *file, int line)
{
    if ( err != NRF_ERROR_NOT_SUPPORTED && err != NRF_SUCCESS )
        nfr5_support$successAssertFailed(err,file,line);
}

#if defined _DEEBUG || defined _FORCE_ASSERT

#define __Assert_Success \
    switch(0) \
        for(uint32_t C_LOCAL_ID(err);0;nrf5_support$checkSuccess(C_LOCAL_ID(err),__FILE__,__LINE__)) \
            case 0: C_LOCAL_ID(err) =
#else

#define __Assert_Success

#define __Success \
    switch(0) \
        for(uint32_t C_LOCAL_ID(err);0;nrf5_support$checkSuccess(C_LOCAL_ID(err),__FILE__,__LINE__)) \
            case 0: C_LOCAL_ID(err) =

#define __Supported \
    switch(0) \
        for(uint32_t C_LOCAL_ID(err);0;nrf5_support$checkSuccessIfSupported(C_LOCAL_ID(err),__FILE__,__LINE__)) \
            case 0: C_LOCAL_ID(err) =

#endif


#ifdef __keil_v5

#pragma uccm file(firmware.sct)~= LR_IROM1 {$ROM_BASE}+{$ROM_APP_BASE} {$ROM_SIZE}-{$ROM_APP_BASE}  {\n\
  ER_IROM1 {$ROM_BASE}+{$ROM_APP_BASE} {$ROM_SIZE}-{$ROM_APP_BASE}  {
#pragma uccm file(firmware.sct)+= \n\
    *.obj (RESET, +First) \n\
    *(InRoot$$Sections) \n\
    .ANY (+RO) \n\
  }
#pragma uccm file(firmware.sct)~= \n  RW_IRAM1 {$RAM_BASE}+{$RAM_APP_BASE} {$RAM_SIZE}-{$RAM_APP_BASE}  {\n    .ANY (+RW +ZI)\n  }\n}

#else

#pragma uccm file(firmware.ld)~= SEARCH_DIR({TOOLCHAIN}/gcc) \n
#pragma uccm file(firmware.ld)+= GROUP(-lgcc -lc -lnosys) \n\nMEMORY\n{\n
#pragma uccm file(firmware.ld)~= FLASH (rx) : ORIGIN = {$ROM_BASE}+{$ROM_APP_BASE}, LENGTH = {$ROM_SIZE}-{$ROM_APP_BASE}\n
#pragma uccm file(firmware.ld)~= RAM (rwx) :  ORIGIN = {$RAM_BASE}+{$RAM_APP_BASE}, LENGTH = {$RAM_SIZE}-{$RAM_APP_BASE}\n}\n

#pragma uccm file(firmware.ld)+= \n\
SECTIONS\n\
{\n\
  .fs_data :\n\
  {\n\
    PROVIDE(__start_fs_data = .);\n\
    KEEP(*(.fs_data))\n\
    PROVIDE(__stop_fs_data = .);\n\
  } > RAM\n\
  .pwr_mgmt_data :\n\
  {\n\
    PROVIDE(__start_pwr_mgmt_data = .);\n\
    KEEP(*(.pwr_mgmt_data))\n\
    PROVIDE(__stop_pwr_mgmt_data = .);\n\
  } > RAM\n\
} INSERT AFTER .data;\n\
\n\
INCLUDE "nrf51_common.ld"\n

#pragma uccm ldflags+= -T[@inc]/firmware.ld

#endif



