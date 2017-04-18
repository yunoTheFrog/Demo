#include <string.h>
#include "board.h"
#include <stdlib.h>
#include "global_variables.h"
#include "demo_settings.h"
#include "HW_functions.h"
#include "LCD.h"
#include "crc32.h"
#include "ndef_message.h"
#include "ndef_parser.h"
#include "nfc_device.h"

//---------------------------------------------------------------------
//               Helping Defines
//---------------------------------------------------------------------

#define SRAM_MEMORY_START 			0x10000000
#define USER_FLASH_START 			0x4000
#define INTERRUPT_VECTOR_TABLE_SIZE 0x200
#define NTAG_I2C_PLUS

#define SPEED 						FAST

uint16_t blue_blink_num;
uint16_t red_blink_num;
uint16_t green_blink_num;

//-----------------------------------------------------------------
//               Helping functions begin
//-----------------------------------------------------------------

void TIMER32_1_IRQHandler(void) {
	//interrupt when duty cycle value is reached
	if (Chip_TIMER_MatchPending(LPC_TIMER32_1, 0)) {
		Chip_TIMER_ClearMatch(LPC_TIMER32_1, 0);
		Board_LED_Set(0, false);
	}
	//interrupt when duty cycle end of period is reached
	if (Chip_TIMER_MatchPending(LPC_TIMER32_1, 1)) {
		Chip_TIMER_ClearMatch(LPC_TIMER32_1, 1);
		Chip_TIMER_Reset(LPC_TIMER32_1);
		if (LPC_TIMER32_1->MR[0] != 0)
			Board_LED_Set(0, true);
	}
}

//---------------------------------------------------------------------

void SET_LAMP_OUTPUT_DIRECTION() {
	Chip_GPIO_SetPinDIROutput(LPC_GPIO, 0, ORANGE_LAMP);
	Chip_GPIO_SetPinDIROutput(LPC_GPIO, 0, BLUE_LAMP);
	Chip_GPIO_SetPinDIROutput(LPC_GPIO, 0, GREEN_LAMP);
}

//---------------------------------------------------------------------

Bool Check_Ext_Power() {
	return Chip_GPIO_GetPinState(LPC_GPIO, 0, 20);
}

//---------------------------------------------------------------------

void LED_on_if_NFC_detected() {

	uint8_t reg = 0;
	NFC_SetPthruOnOff(ntag_handle, FALSE);
	NFC_SetTransferDir(ntag_handle, RF_TO_I2C);

	NFC_ReadRegister(ntag_handle, NFC_MEM_OFFSET_NS_REG, &reg);
	if (!reg & 0x01) {
		HW_switchLEDs(LEDOFF);
	} else
		LED_switchLEDs(0, 1);
}

//---------------------------------------------------------------------

void check_Buttons(uint8_t *Buttons) {
	if (HW_Get_Button_State(Button1)) {
		*Buttons |= 0x01;
	} else {
		*Buttons &= ~0x01;
	}

	if (HW_Get_Button_State(Button2)) {
		*Buttons |= 0x02;
	} else {
		*Buttons &= ~0x02;
	}

	if (HW_Get_Button_State(Button3)) {
		*Buttons |= 0x04;
	} else {
		*Buttons &= ~0x04;
	}
}

//---------------------------------------------------------------------

void LIN_Demo(uint8_t mode, uint16_t speed) {
	//make a init only one time

	uint8_t LCDmessagebuffer[160];

	uint8_t eeprom_array[10];

	uint8_t blink_array[6];
	uint8_t reset_array[11];
	int array_counter = 0;
	uint8_t reg = 0;

	SET_LAMP_OUTPUT_DIRECTION();
	switch (mode) {

	case RESET_MODE:


		memcpy(reset_array, rxbuffer, sizeof(reset_array));

		memset(&reset_array[4],0,7);

		NFC_SetPthruOnOff(ntag_handle, FALSE);

		NFC_WriteBytes(ntag_handle, START_APPMEM_ADRESS, reset_array, 11);
		HAL_Timer_delay_ms(200);

		NFC_SetPthruOnOff(ntag_handle, TRUE);

		LCD_on();

		memcpy(LCDmessagebuffer, "Reset!    ", 10);
		LCDWrite(1, LCDmessagebuffer, 10);

		LED_switchLEDs(LED_ON, RED_LED);
		LED_switchLEDs(LED_ON, BLUE_LED);
		HAL_Timer_delay_ms(400);

		memcpy(LCDmessagebuffer, "          ", 10);
		LCDWrite(1, LCDmessagebuffer, 10);

		break;



	case DEFAULT_MODE:

		/*
		 *
		 *----------------------------------MODE without NFC------------------------------------
		 *
		 *
		 * First 3 positions of eeprom_array are the Configurations of the lamps.
		 * If Blinking is selected, the selected lamp will be turned on and after a delay be turned off again
		 *
		 *
		 */

		NFC_SetPthruOnOff(ntag_handle, TRUE);

		memcpy(eeprom_array, rxbuffer, sizeof(eeprom_array));

		//Configuration is written in as String and need to be converted to Integer
		eeprom_array[0] = eeprom_array[0] - '0';
		eeprom_array[1] = eeprom_array[1] - '0';
		eeprom_array[2] = eeprom_array[2] - '0';
		eeprom_array[3] = eeprom_array[3] - '0';

		//2 Neighbouring Bytes are combined to give the total blink counter
		red_blink_num = eeprom_array[4] << 8 | eeprom_array[5];
		blue_blink_num = eeprom_array[6] << 8 | eeprom_array[7];
		green_blink_num = eeprom_array[8] << 8 | eeprom_array[9];

		for (int i = 0; i < 3; i++) {
			// turn on "BLINKING" lamp
			if (eeprom_array[i] == LAMP_BLINKING)
				SwitchLamp_IncrementCounter(LAMP_ON, ORANGE_LAMP + (i));
			 //else: apply all other configurations
			else
				SwitchLamp_IncrementCounter(eeprom_array[i], ORANGE_LAMP + (i));
		}


		HAL_Timer_delay_ms(speed);
		for (int i = 0; i < 3; i++) {
			//turn off "BLINKING" lamp after delay
			if (eeprom_array[i] == LAMP_BLINKING)
				SwitchLamp_IncrementCounter(LAMP_OFF, ORANGE_LAMP + (i));

		}

		HAL_Timer_delay_ms(speed);
		NFC_SetPthruOnOff(ntag_handle, FALSE);

		//Total Blink counter is seperated into 2 single bytes for transfer to EEPROM
		blink_array[0] = (red_blink_num >> 8);
		blink_array[1] = red_blink_num;
		blink_array[2] = (blue_blink_num >> 8);
		blink_array[3] = blue_blink_num;
		blink_array[4] = (green_blink_num >> 8);
		blink_array[5] = green_blink_num;

		NFC_WriteBytes(ntag_handle, START_COUNTER_ADRESS, blink_array, 6);

		NFC_SetPthruOnOff(ntag_handle, TRUE);

		LCD_on();

		switch (eeprom_array[3]) {

		case 0: {
			memcpy(LCDmessagebuffer, "Guten Tag!", 10);
			LCDWrite(0, LCDmessagebuffer, 10);
			CurrentDisplay = DISPLAY_DEFAULT_MESSAGE;
			break;
		}
		case 1: {
			memcpy(LCDmessagebuffer, "Grias Di! ", 10);
			LCDWrite(0, LCDmessagebuffer, 10);
			CurrentDisplay = DISPLAY_DEFAULT_MESSAGE;
			break;
		}
		case 2: {
			memcpy(LCDmessagebuffer, "Gruezi!   ", 10);
			LCDWrite(0, LCDmessagebuffer, 10);
			CurrentDisplay = DISPLAY_DEFAULT_MESSAGE;
			break;
		}
		case 3: {
			memcpy(LCDmessagebuffer, "Hello!    ", 10);
			LCDWrite(0, LCDmessagebuffer, 10);
			CurrentDisplay = DISPLAY_DEFAULT_MESSAGE;
			break;
		}
		case 4: {
			memcpy(LCDmessagebuffer, "Bonjour!    ", 10);
			LCDWrite(0, LCDmessagebuffer, 10);
			CurrentDisplay = DISPLAY_DEFAULT_MESSAGE;
			break;
		}
		}

		break;



	case NFC_MODE:


		//Configuration is written in as String and need to be converted to Integer
		memcpy(eeprom_array, rxbuffer, sizeof(eeprom_array));

		eeprom_array[0] = eeprom_array[0] - '0';
		eeprom_array[1] = eeprom_array[1] - '0';
		eeprom_array[2] = eeprom_array[2] - '0';
		eeprom_array[3] = eeprom_array[3] - '0';

		//2 Neighbouring Bytes are combined to give the total blink counter
		red_blink_num = eeprom_array[4] << 8 | eeprom_array[5];
		blue_blink_num = eeprom_array[6] << 8 | eeprom_array[7];
		green_blink_num = eeprom_array[8] << 8 | eeprom_array[9];

		LCD_on();

		switch (eeprom_array[3]) {

		case 0: {
			memcpy(LCDmessagebuffer, "Guten Tag!", 10);
			LCDWrite(0, LCDmessagebuffer, 10);
			CurrentDisplay = DISPLAY_DEFAULT_MESSAGE;
			break;
		}
		case 1: {
			memcpy(LCDmessagebuffer, "Grias Di! ", 10);
			LCDWrite(0, LCDmessagebuffer, 10);
			CurrentDisplay = DISPLAY_DEFAULT_MESSAGE;
			break;
		}
		case 2: {
			memcpy(LCDmessagebuffer, "Gruezi!   ", 10);
			LCDWrite(0, LCDmessagebuffer, 10);
			CurrentDisplay = DISPLAY_DEFAULT_MESSAGE;
			break;
		}
		case 3: {
			memcpy(LCDmessagebuffer, "Hello!    ", 10);
			LCDWrite(0, LCDmessagebuffer, 10);
			CurrentDisplay = DISPLAY_DEFAULT_MESSAGE;
			break;
		}
		case 4: {
			memcpy(LCDmessagebuffer, "Bonjour!    ", 10);
			LCDWrite(0, LCDmessagebuffer, 10);
			CurrentDisplay = DISPLAY_DEFAULT_MESSAGE;
			break;
		}
		}

		HAL_Timer_delay_ms(speed / 2);
		if (Check_Ext_Power()) {
			for (int i = 0; i < 3; i++) {
				if (eeprom_array[i] == 1) {
					// turn off "BLINKING" lamp
					SwitchLamp_IncrementCounter(LAMP_ON, ORANGE_LAMP + (i));
				}
				//else: apply all other configurations
				else {
					SwitchLamp_IncrementCounter(eeprom_array[i], ORANGE_LAMP + (i));
				}
			}
		}

		LED_on_if_NFC_detected();
		HAL_Timer_delay_ms(speed);

		if (Check_Ext_Power()) {
			for (int i = 0; i < 3; i++) {
				// turn off "BLINKING" lamp after delay
				if (eeprom_array[i] == 1) {
					SwitchLamp_IncrementCounter(LAMP_OFF, ORANGE_LAMP + (i));
				}
			}
		}

		LED_on_if_NFC_detected();

		HAL_Timer_delay_ms(speed / 2);

		//Total Blink counter is seperated into 2 single bytes for transfer to EEPROM
		blink_array[0] = (red_blink_num >> 8);
		blink_array[1] = red_blink_num;
		blink_array[2] = (blue_blink_num >> 8);
		blink_array[3] = blue_blink_num;
		blink_array[4] = (green_blink_num >> 8);
		blink_array[5] = green_blink_num;

		NFC_SetPthruOnOff(ntag_handle, FALSE);

		NFC_WriteBytes(ntag_handle, START_COUNTER_ADRESS, blink_array,
				sizeof(blink_array));

		break;

	default:
		LED_switchLEDs(1, 2);
		HAL_Timer_delay_ms(100);
		LED_switchLEDs(0, 2);
		HAL_Timer_delay_ms(100);
		break;
	}

	//Check pressed buttons
	uint8_t Buttons;
	check_Buttons(&Buttons);

	// waiting till RF has read
	HAL_Timer_delay_ms(100);

	NFC_SetPthruOnOff(ntag_handle, FALSE);
	NFC_SetTransferDir(ntag_handle, RF_TO_I2C);

	NFC_ReadRegister(ntag_handle, NFC_MEM_OFFSET_NS_REG, &reg);
	if (reg & 0x01) {

	}
}


//---------------------------------------------------------------------

//   Read transmitted Configuration and apply

void Check_RF_ReadEEPROM_Execute(uint16_t speed) {

	Chip_GPIO_SetPinDIRInput(LPC_GPIO, 0, 20);
	uint8_t reg = 0;
	NFC_ReadRegister(ntag_handle, NFC_MEM_OFFSET_NS_REG, &reg);
	while (!reg & 0x01) {
		NFC_ReadRegister(ntag_handle, NFC_MEM_OFFSET_NS_REG, &reg);
		NFC_SetPthruOnOff(ntag_handle, FALSE);
		NFC_ReadBytes(ntag_handle, START_APPMEM_ADRESS, rxbuffer, sizeof(rxbuffer));
		HW_switchLEDs(LEDOFF);
		LIN_Demo(DEFAULT_MODE, speed);
	}
}


//---------------------------------------------------------------------

void Main_Loop(uint16_t speed) {
	while (1) {
		LED_on_if_NFC_detected();
		NFC_SetTransferDir(ntag_handle, RF_TO_I2C);
		NFC_ReadBytes(ntag_handle, START_APPMEM_ADRESS, rxbuffer,
				sizeof(rxbuffer));
		//Check for requested action
		char command = rxbuffer[10];
		switch (command) {
		case 'Z':
			LIN_Demo(RESET_MODE, speed);
			break;

		default:
			LED_switchLEDs(LED_OFF, RED_LED);
			LED_switchLEDs(LED_OFF, BLUE_LED);
			LIN_Demo(NFC_MODE, speed);
			break;
		}
	}
	return;
}


//---------------------------------------------------------------------

void CopyInterruptToSRAM(void) {
	unsigned int * flashPtr, *ramPtr;
	unsigned int * uLimit = (unsigned int *) (USER_FLASH_START
			+ INTERRUPT_VECTOR_TABLE_SIZE);

	ramPtr = (unsigned int *) SRAM_MEMORY_START; //load SRAM starting at 0x1000 0000
	flashPtr = (unsigned int *) USER_FLASH_START; //start of the interrupt vector table

	while (flashPtr < uLimit) {

		*ramPtr = *flashPtr;
		ramPtr++;
		flashPtr++;
	}
}


//---------------------------------------------------------------------

#if defined (NTAG_I2C_PLUS)
void factory_reset_Tag() {
	HW_switchLEDs(REDLED);
	HAL_Timer_delay_ms(100);

	//reset default eeprom memory values (smart poster)
	NFC_WriteBytes(ntag_handle, NTAG_MEM_ADRR_I2C_ADDRESS,
			Default_BeginingOfMemory, Default_BeginingOfMemory_length);

	//reset pages from 8 to 56
	uint8_t page = 8;
	while (page < 56) {
		NFC_WriteBlock(ntag_handle, page, Null_Block, NTAG_I2C_BLOCK_SIZE);
		page++;
	}
	//reset pages 56,57,58
	NFC_WriteBlock(ntag_handle, 56, Default_Page_56, NTAG_I2C_BLOCK_SIZE);
	NFC_WriteBlock(ntag_handle, 57, Default_Page_57, NTAG_I2C_BLOCK_SIZE);
	NFC_WriteBlock(ntag_handle, 58, Default_Page_58, NTAG_I2C_BLOCK_SIZE);

	HW_switchLEDs(GREENLED);
	HAL_Timer_delay_ms(100);
}
#else
void factory_reset_Tag()
{
	// config registers memory address for NTAG I2C 1K version
	uint8_t config = NTAG_MEM_BLOCK_CONFIGURATION_1k;

	//default config register values as defined by the datasheet
	uint8_t default_config_reg[NTAG_I2C_BLOCK_SIZE]= {0x01, 0x00, 0xF8, 0x48, 0x08,0x01, 0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00};
	HW_switchLEDs(REDLED);
	HAL_Timer_delay_ms(100);

	//reset default eeprom memory values (smart poster)
	NFC_WriteBytes(ntag_handle, NFC_MEM_ADDR_START_USER_MEMORY,
			Default_NDEF_Message, Default_NDEF_Message_length);

	//reset default config registers
	NTAG_WriteBlock(ntag_handle, config, default_config_reg, NTAG_I2C_BLOCK_SIZE);

	HW_switchLEDs(GREENLED);
	HAL_Timer_delay_ms(100);
}
#endif

//---------------------------------------------------------------------

void Setup() {
	HW_switchLEDs(LEDOFF);

	// Initialize peripherals
	HAL_BSP_BoardInit();

	// Setup Pins on the microcontroller
	HW_setup_Board_for_use_with_NTAG();

	// enable delay timer
	HAL_Timer_Init();

	// Initialize the Interrupt Service Routine
	HAL_ISR_Init();

	// Initialize I2C
	i2cHandleMaster = HAL_I2C_InitDevice
	(HAL_I2C_INIT_DEFAULT);
	SystemCoreClockUpdate();

	// Set interrupt for time measurement
	SysTick_Config(SystemCoreClock / 1000); // produce a timer interrupt every 1ms

	// Initialize the NTAG I2C components
	ntag_handle = NFC_InitDevice(NFC_TEST_DEVICE, i2cHandleMaster);
	HAL_ISR_RegisterCallback(0, ISR_LEVEL_LO, NULL, NULL);

	// Enable IRQ for BOD
	Chip_SYSCTL_EnableBODReset();

	HW_init_Peripherals();
}

//---------------------------------------------------------------------

void LED_blink(LED led) {
	HW_switchLEDs(LEDOFF);
	HAL_Timer_delay_ms(100);
	HW_switchLEDs(led);
	HAL_Timer_delay_ms(100);
	HW_switchLEDs(LEDOFF);
	HAL_Timer_delay_ms(100);
	HW_switchLEDs(led);
	HAL_Timer_delay_ms(100);
	HW_switchLEDs(LEDOFF);
	HAL_Timer_delay_ms(100);
	HW_switchLEDs(led);
	HAL_Timer_delay_ms(100);
	HW_switchLEDs(LEDOFF);
	HAL_Timer_delay_ms(100);

}

//---------------------------------------------------------------------
void LCDWrite(int LCDrow, uint8_t Data[], int len) {
	uint8_t I2CMasterLCDBuffer[0x42]; //maximum data is 0x40 plus I2C Address + command byte
	uint32_t i;

	I2CMasterLCDBuffer[0] = LCD_I2C_Address;
	I2CMasterLCDBuffer[1] = COMMAND;
	if (LCDrow == 0)
		I2CMasterLCDBuffer[2] = Comm_SetDDRAMAddress; // Write to data RAM at address 0x00
	else
		I2CMasterLCDBuffer[2] = Comm_SetDDRAMAddress | 0x40; // Write to data RAM at address 0x40, which is the starting address of the second line
	Chip_I2CM_Write(LPC_I2C, &I2CMasterLCDBuffer[0], 3);

	I2CMasterLCDBuffer[0] = LCD_I2C_Address;
	I2CMasterLCDBuffer[1] = DATA;
	for (i = 0; i < len; i++)
		I2CMasterLCDBuffer[i + 2] = Data[i];

	Chip_I2CM_Write(LPC_I2C, &I2CMasterLCDBuffer[0], len + 2);
}
//---------------------------------------------------------------------
//               Helping functions end
//---------------------------------------------------------------------


/*
 * main Program
 * @return should never return
 */
int main(void) {

	//all interrupts are disabled prior to any interrupt vector table is moved
	__disable_irq();

	//relocate the interrupt vector table to SRAM
	CopyInterruptToSRAM();

	// The MAP bits in the SYSMEMREMAP register is set to 0x1,
	// indicating the vector table is located in the SRAM and not in the flash area
	Chip_SYSCTL_Map(REMAP_USER_RAM_MODE);

	//all interrupts are enabled after the interrupt vector table has been moved to SRAM
	__enable_irq();

	// Initialize main buffer used to read and write user memory
	uint8_t Buttons = 0;



	Setup();
	LCDInit();
	HAL_BSP_BoardInit();
	InitTimer();
	check_Buttons(&Buttons);

	//If button 2 is pressed on start-up, reset the tag memory to the default (smart poster NDEF)
	if (Buttons == 0x02) {
		factory_reset_Tag();

	}

#ifdef INTERRUPT
	// If Interrupted Mode is enabled set the FD Pin to react on the SRAM
	NFC_SetFDOffFunction(ntag_handle,
			I2C_LAST_DATA_READ_OR_WRITTEN_OR_RF_SWITCHED_OFF_11b);
	NFC_SetFDOnFunction(ntag_handle, DATA_READY_BY_I2C_OR_DATA_READ_BY_RF_11b);
#endif



	Check_RF_ReadEEPROM_Execute(SPEED);


	Main_Loop(SPEED);


	return 0;
}

