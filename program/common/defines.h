//This file defines the address for the different units available

#define UART_IDLE (*(volatile unsigned char *)0x80000000)
#define UART_OUT (*(volatile unsigned char *)0x80000000)
#define UART_IN (*(volatile unsigned char *)0x80000002)
#define UART_START (*(volatile unsigned int *)0x80000004)
#define UART_END (*(volatile unsigned int *)0x80000008)


#define TEST_SUCCESS_STRING "~~~ALL TESTS PASSED~~~\n"
#define TEST_SUCCESS_STRING_LENGTH sizeof(TEST_SUCCESS_STRING)

#define TEST_FAILED_STRING "~~~SOME TESTS FAILED~~~\n"
#define TEST_FAILED_STRING_LENGTH sizeof(TEST_FAILED_STRING)

