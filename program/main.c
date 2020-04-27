#include <stdio.h>

extern void printstr(const char *s, int length);

#define SEVEN_SEGMENT (*(volatile unsigned int *)0x80000010)



#define UART_START (*(volatile unsigned int *)uartsection)
#define UART_END (*(volatile unsigned int *)uartsection)

#define SPI_START_ADDR (*(volatile unsigned int *)0x40000000)
#define SPI_FLASH_START_ADDR (*(volatile unsigned int *)0x40000004)
#define SPI_FLASH_END_ADDR (*(volatile unsigned int *)0x40000008)


#define XADC_FIRST (*(volatile unsigned int *)0x8000107C)

#define BLOCK_MEM 0x20000000

void loop(){

    float tmp = 4.2;
    while (1)
    {
        //SEVEN_SEGMENT = XADC_FIRST;

        tmp += 1;

        SEVEN_SEGMENT = tmp;
        tmp -= 0.5;
        tmp *= 2.5;
        //SEVEN_SEGMENT = tmp;
        //printf("Float = %f",tmp);
    }
}

int main()
{
    SEVEN_SEGMENT = 0x4242;
    printstr("This is the new main2\n", sizeof("This is the new main2\n"));
    printstr(__TIME__, sizeof(__TIME__));
    printstr("\n",sizeof("\n"));


    //Check that printing works
    printstr("~~~ALL TESTS PASSED~~~",sizeof("~~~ALL TESTS PASSED~~~"));

    int value = 0;


    loop();

    return 0;
}