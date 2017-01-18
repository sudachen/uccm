# uCcm

It's the tool for easy studying and fast prototyping embedded solutions based on Cortex-M uCs. Currently it  supports Stm32fx and NRF5x hardware.

What you need to start is only uccm.cmd file. Place it to the root of the empty project. Now you can do the command, for example 
``uccm -y -n -b pca10028 --edit``
It will create simple main file, will get all required frameworks and software, also it will create and start QtCreator project. Qt project is ready to be build in one click and ready to program built firmware to connected board in one click also.


