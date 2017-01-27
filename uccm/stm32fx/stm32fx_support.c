
#include <uccm/board.h>

__Weak
void ucSysTick1ms(void)
{
    // nothing is here
    // for late implemenation in user code
}

void SysTick_Handler(void)
{
  HAL_IncTick();
  HAL_SYSTICK_IRQHandler();
  ucSysTick1ms();
}

void stm32fx_support$successAssertFailed(uint32_t err, const char *file, int line)
{
    ucError("HAL ASSERT FAILED \n\tat %?:%?\n\terror code %08x", $s(file), $i(line), $u(err));
    ucFatalError(UC_ERROR_IN_ASSERT);
}

void ucConfig_SystemClock_HSE8_72_wUSB(bool hseBypass)
{

  RCC_OscInitTypeDef RCC_OscInitStruct;
  RCC_ClkInitTypeDef RCC_ClkInitStruct;
  RCC_PeriphCLKInitTypeDef PeriphClkInit;

  RCC_OscInitStruct.OscillatorType = RCC_OSCILLATORTYPE_HSE;
  RCC_OscInitStruct.HSEState = hseBypass ? RCC_HSE_BYPASS : RCC_HSE_ON;
  RCC_OscInitStruct.HSEPredivValue = RCC_HSE_PREDIV_DIV1;
  RCC_OscInitStruct.PLL.PLLState = RCC_PLL_ON;
  RCC_OscInitStruct.PLL.PLLSource = RCC_PLLSOURCE_HSE;
  RCC_OscInitStruct.PLL.PLLMUL = RCC_PLL_MUL9;

  __Assert_Hal_Success HAL_RCC_OscConfig(&RCC_OscInitStruct);

  RCC_ClkInitStruct.ClockType = RCC_CLOCKTYPE_HCLK|RCC_CLOCKTYPE_SYSCLK
                              |RCC_CLOCKTYPE_PCLK1|RCC_CLOCKTYPE_PCLK2;
  RCC_ClkInitStruct.SYSCLKSource = RCC_SYSCLKSOURCE_PLLCLK;
  RCC_ClkInitStruct.AHBCLKDivider = RCC_SYSCLK_DIV1;
  RCC_ClkInitStruct.APB1CLKDivider = RCC_HCLK_DIV2;
  RCC_ClkInitStruct.APB2CLKDivider = RCC_HCLK_DIV1;

  __Assert_Hal_Success HAL_RCC_ClockConfig(&RCC_ClkInitStruct, FLASH_LATENCY_2);

  PeriphClkInit.PeriphClockSelection = RCC_PERIPHCLK_USB;
  PeriphClkInit.USBClockSelection = RCC_USBCLKSOURCE_PLL_DIV1_5;

  __Assert_Hal_Success HAL_RCCEx_PeriphCLKConfig(&PeriphClkInit);

  HAL_SYSTICK_Config(HAL_RCC_GetHCLKFreq()/1000);
  HAL_SYSTICK_CLKSourceConfig(SYSTICK_CLKSOURCE_HCLK);
  HAL_NVIC_SetPriority(SysTick_IRQn, 0, 0);

  __HAL_RCC_SYSCFG_CLK_ENABLE();
  HAL_NVIC_SetPriorityGrouping(NVIC_PRIORITYGROUP_0);
  HAL_NVIC_SetPriority(MemoryManagement_IRQn, 0, 0);
  HAL_NVIC_SetPriority(BusFault_IRQn, 0, 0);
  HAL_NVIC_SetPriority(UsageFault_IRQn, 0, 0);
  HAL_NVIC_SetPriority(SVCall_IRQn, 0, 0);
  HAL_NVIC_SetPriority(DebugMonitor_IRQn, 0, 0);
  HAL_NVIC_SetPriority(PendSV_IRQn, 0, 0);
  HAL_NVIC_SetPriority(SysTick_IRQn, 0, 0);

#ifdef GPIOE
  __HAL_RCC_GPIOE_CLK_ENABLE();
#endif
#ifdef GPIOC
  __HAL_RCC_GPIOC_CLK_ENABLE();
#endif
#ifdef GPIOF
  __HAL_RCC_GPIOF_CLK_ENABLE();
#endif
#ifdef GPIOD
  __HAL_RCC_GPIOD_CLK_ENABLE();
#endif

  __HAL_RCC_GPIOA_CLK_ENABLE();
  __HAL_RCC_GPIOB_CLK_ENABLE();
}


#ifndef GPIOC
#define UC_GPIOC NULL
#else
#define UC_GPIOC GPIOC
#endif
#ifndef GPIOD
#define UC_GPIOD NULL
#else
#define UC_GPIOD GPIOD
#endif
#ifndef GPIOE
#define UC_GPIOE NULL
#else
#define UC_GPIOE GPIOE
#endif
#ifndef GPIOF
#define UC_GPIOF NULL
#else
#define UC_GPIOF GPIOF
#endif

GPIO_TypeDef * const ucConst_GPIO_TABLE[] =
{
    GPIOA,GPIOB,UC_GPIOC,UC_GPIOD,UC_GPIOE,UC_GPIOF
#ifdef _DEBUG
    ,0,0,0,0,0,0,0,0,0,0
#endif
};

#undef UC_GPIOC
#undef UC_GPIOD
#undef UC_GPIOE
#undef UC_GPIOF
