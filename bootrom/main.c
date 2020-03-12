#include <stdio.h>


extern void printstr2(const char* s, int len);

extern void printhex(long x);


#define SEVEN_SEGMENT (*(volatile unsigned int *)0x80000010)



#define UART_START (*(volatile unsigned int *)0x80000004)
#define UART_END (*(volatile unsigned int *)0x80000008)

#define SPI_START_ADDR (*(volatile unsigned int *)0x40000000)
#define SPI_FLASH_START_ADDR (*(volatile unsigned int *)0x40000004)
#define SPI_FLASH_END_ADDR (*(volatile unsigned int *)0x40000008)
#define SPI_STATUS (*(volatile unsigned int *)0x4000000C)



#define BLOCK_MEM 0x20000000



int main()
{

    SEVEN_SEGMENT = 0x1489;

    printstr2("Hello World\n",12);

    SPI_START_ADDR = BLOCK_MEM;
    SPI_FLASH_START_ADDR = 0x00200000;
    SPI_FLASH_END_ADDR = 0x00201000;

    while(SPI_STATUS != 0); //Wait for flash to have loaded in the entire address range


    //Jump to main program.
    __asm volatile("nop");
    __asm volatile("nop");
    __asm volatile("nop");
    __asm volatile("nop");
    __asm volatile("mv ra,zero");
    __asm volatile("lui ra,0x20000");
    __asm volatile("ret");


    return 0;
}