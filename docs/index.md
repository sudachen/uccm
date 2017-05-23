<img class="padding_20" src="assets/devkit.jpg" width="660px" align="center"/>

# Overview
The uccm is an **uC Cortex-M zero-config build tool and modules manager**. It means this building tool does not require additional configuration files. All required information needed to build firmware exists in sources. Also the tool automatically gets modules from guithub detecting required modules by _#include_ C-preprocessor derectives.

# Quick Start with PCA10028 (nRF51)

Let's start with simple example turning on one LED. Firstly we need a main.c file like a following:
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

Secondary, we need the uccm start script which is available on GitHub [uccm100-dist.zip](https://github.com/sudachen/uccm/blob/uccm100/uccm100-dist.zip). 

Now, when we have in one directory main.c and uccm.cmd, go to the directory and start the next command 
```
uccm -y --edit
```

It downloads all required SDK files, gcc compiler and code editor. At final it creates project and opens main.c in code editor.

![create new project](assets/create_project.png)

Since this quick start uses pca10028 board based on BLE powered uC nRF51422, uccm by default configures firmware as used softdevice. Softdevice is the special firmaware operating with radio channel and allowing to communicate via Bluetooth Low Energy. To write sofdevice on the flash do the next command
```
uccm --program-softdevice
```

It erases all chip flash memory and writes softdevice to the begin. Normally it's required to program softdevice only once. For this operation the board should be connected to the PC and powered on.

Ok, now all is ready to start simplified example turning on one LED. For compiling and flashing compiled firmare press start button. It is green trinagle at the bottom of the left panel in the editor. 

![run firmware](assets/run_firmware.png)

Devboard should resets and lights LED1.




