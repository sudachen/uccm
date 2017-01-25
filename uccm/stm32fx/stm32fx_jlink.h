
#pragma once

#pragma uccm let(JLINK_SPEED)?= 4000
#pragma uccm let(JLINK_DEVICE)?= CORTEX-M4

#pragma uccm debugger(jrttview)+= -ct usb -speed 4000 -a -if swd

#pragma uccm file(program.jlink) ~= si 1\n\
speed {$JLINK_SPEED}\n\
device {$JLINK_DEVICE}\n\
loadfile {$FIRMWARE_FILE_HEX}\n\
qc\n

#pragma uccm file(program_reset.jlink) ~= si 1\n\
speed {$JLINK_SPEED}\n\
device {$JLINK_DEVICE}\n\
loadfile {$FIRMWARE_FILE_HEX}\n\
r\n\
g\n\
qc\n

#pragma uccm file(erase.jlink) ~= si 1\n\
speed {$JLINK_SPEED}\n\
device {$JLINK_DEVICE}\n\
RSetType 3\n\
erase\n\
qc\n

#pragma uccm file(reset.jlink) ~= si 1\n\
speed {$JLINK_SPEED}\n\
device {$JLINK_DEVICE}\n\
r\n\
g\n\
qc\n

#pragma uccm file(connect.jlink) ~= si 1\n\
speed {$JLINK_SPEED}\n\
device {$JLINK_DEVICE}\n\
regs\n\
qc\n
