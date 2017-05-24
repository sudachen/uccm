<img class="padding_20" src="assets/devkit.jpg" width="660px" align="center"/>

# Overview
The uccm is an **uC Cortex-M zero-config building tool and modules manager**. It means this building tool does not require any additional configuration files. All required information needed to build your firmware is available in the source code. Also the tool automatically gets modules from guithub detecting required modules by _#include_ C-preprocessor directives.

# Quick Start with PCA10028 (nRF51)

Let's start with a simple example turning on one LED. First, we need a main.c file like the following:
```c 
#pragma uccm default(board) = pca10028
#pragma uccm let(HEAP_SIZE) = 0
#include <uccm/board.h> // borad depended defininitions

int main()
{
    setup_board();
    setOn_boardLED(0);
    for(;;) __NOP();
}
```

Second, we need the uccm start script which is available on GitHub [uccm100-dist.zip](https://github.com/sudachen/uccm/blob/uccm100/uccm100-dist.zip). 

Now, when we have in one directory main.c and uccm.cmd, go to the directory and start the next command 
```
uccm -y --edit
```

It downloads all required SDK files, gcc compiler and code editor. Finally, it creates a project and opens main.c in the code editor.

![create new project](assets/create_project.png)

Since this quick start uses pca10028 board based on BLE powered uC nRF51422, uccm by default configures the firmware as used softdevice. Softdevice is a special firmware operating with radio channel and allowing communications via Bluetooth Low Energy. To write a sofdevice on the flash, run the next command
```
uccm --program-softdevice
```

It erases all chip flash memory and writes softdevice to the beginning. Normally, you will need to program softdevice only once. For this operation the board should be connected to the PC and powered on.

Ok, now all is ready to start simplified example turning on one LED. For compiling and flashing compiled firmare press the Start button. It is the green triangle at the bottom of the left panel in the editor. 

![run firmware](assets/run_firmware.png)

Devboard should reset and light LED1.




