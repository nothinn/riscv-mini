#include <stdio.h>

extern void printstr(const char *s);

#define SEVEN_SEGMENT (*(volatile unsigned int *)0x80000010)



#define UART_START (*(volatile unsigned int *)0x80000004)
#define UART_END (*(volatile unsigned int *)0x80000008)

#define SPI_START_ADDR (*(volatile unsigned int *)0x40000000)
#define SPI_FLASH_START_ADDR (*(volatile unsigned int *)0x40000004)
#define SPI_FLASH_END_ADDR (*(volatile unsigned int *)0x40000008)


#define XADC_FIRST (*(volatile unsigned int *)0x8000107C)

#define BLOCK_MEM 0x20000000


int main()
{
    SEVEN_SEGMENT = 0x4242;
    printstr("This is the new main2\n");
    printstr(__TIME__);
    printstr("\n");

    int value = 0;

    float tmp = 4.2;

    while (1)
    {
        SEVEN_SEGMENT = XADC_FIRST;

        tmp += 1;

        SEVEN_SEGMENT = tmp;
    }
    return 0;
}