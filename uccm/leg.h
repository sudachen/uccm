
#pragma once
#include "uccm.h"

typedef struct { uint8_t leg_no; } uccm_leg_t;
#define UCCM_LEG(Group,Leg) ((uccm_leg_t){(Group-'A')<<4 | Leg})

#define PA0  UCCM_LEG('A',0)
#define PA1  UCCM_LEG('A',1)
#define PA2  UCCM_LEG('A',2)
#define PA3  UCCM_LEG('A',3)
#define PA4  UCCM_LEG('A',4)
#define PA5  UCCM_LEG('A',5)
#define PA6  UCCM_LEG('A',6)
#define PA7  UCCM_LEG('A',7)
#define PA8  UCCM_LEG('A',8)
#define PA9  UCCM_LEG('A',9)
#define PA10 UCCM_LEG('A',10)
#define PA11 UCCM_LEG('A',11)
#define PA12 UCCM_LEG('A',12)
#define PA13 UCCM_LEG('A',13)
#define PA14 UCCM_LEG('A',14)
#define PA15 UCCM_LEG('A',15)

#define PB0  UCCM_LEG('B',0)
#define PB1  UCCM_LEG('B',1)
#define PB2  UCCM_LEG('B',2)
#define PB3  UCCM_LEG('B',3)
#define PB4  UCCM_LEG('B',4)
#define PB5  UCCM_LEG('B',5)
#define PB6  UCCM_LEG('B',6)
#define PB7  UCCM_LEG('B',7)
#define PB8  UCCM_LEG('B',8)
#define PB9  UCCM_LEG('B',9)
#define PB10 UCCM_LEG('B',10)
#define PB11 UCCM_LEG('B',11)
#define PB12 UCCM_LEG('B',12)
#define PB13 UCCM_LEG('B',13)
#define PB14 UCCM_LEG('B',14)
#define PB15 UCCM_LEG('B',15)

#define PC0  UCCM_LEG('C',0)
#define PC1  UCCM_LEG('C',1)
#define PC2  UCCM_LEG('C',2)
#define PC3  UCCM_LEG('C',3)
#define PC4  UCCM_LEG('C',4)
#define PC5  UCCM_LEG('C',5)
#define PC6  UCCM_LEG('C',6)
#define PC7  UCCM_LEG('C',7)
#define PC8  UCCM_LEG('C',8)
#define PC9  UCCM_LEG('C',9)
#define PC10 UCCM_LEG('C',10)
#define PC11 UCCM_LEG('C',11)
#define PC12 UCCM_LEG('C',12)
#define PC13 UCCM_LEG('C',13)
#define PC14 UCCM_LEG('C',14)
#define PC15 UCCM_LEG('C',15)

#define PD0  UCCM_LEG('D',0)
#define PD1  UCCM_LEG('D',1)
#define PD2  UCCM_LEG('D',2)
#define PD3  UCCM_LEG('D',3)
#define PD4  UCCM_LEG('D',4)
#define PD5  UCCM_LEG('D',5)
#define PD6  UCCM_LEG('D',6)
#define PD7  UCCM_LEG('D',7)
#define PD8  UCCM_LEG('D',8)
#define PD9  UCCM_LEG('D',9)
#define PD10 UCCM_LEG('D',10)
#define PD11 UCCM_LEG('D',11)
#define PD12 UCCM_LEG('D',12)
#define PD13 UCCM_LEG('D',13)
#define PD14 UCCM_LEG('D',14)
#define PD15 UCCM_LEG('D',15)

#define PE0  UCCM_LEG('E',0)
#define PE1  UCCM_LEG('E',1)
#define PE2  UCCM_LEG('E',2)
#define PE3  UCCM_LEG('E',3)
#define PE4  UCCM_LEG('E',4)
#define PE5  UCCM_LEG('E',5)
#define PE6  UCCM_LEG('E',6)
#define PE7  UCCM_LEG('E',7)
#define PE8  UCCM_LEG('E',8)
#define PE9  UCCM_LEG('E',9)
#define PE10 UCCM_LEG('E',10)
#define PE11 UCCM_LEG('E',11)
#define PE12 UCCM_LEG('E',12)
#define PE13 UCCM_LEG('E',13)
#define PE14 UCCM_LEG('E',14)
#define PE15 UCCM_LEG('E',15)

#define PF0  UCCM_LEG('F',0)
#define PF1  UCCM_LEG('F',1)

typedef enum {
    LEG_FLOAT = 0,
    LEG_PULL_UP,
    LEG_PULL_DOWN,
} uccm_gpio_input_t;

typedef enum {
    LEG_PUSH_PULL = 0,
    LEG_OPEN_DRAIN,
} uccm_gpio_output_t;
