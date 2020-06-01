#include <stdio.h>

#include <math.h>

extern void printstr(const char *s, int length);

#define SEVEN_SEGMENT (*(volatile unsigned int *)0x80000010)



#define UART_START (*(volatile unsigned int *)uartsection)
#define UART_END (*(volatile unsigned int *)uartsection)

#define SPI_START_ADDR (*(volatile unsigned int *)0x40000000)
#define SPI_FLASH_START_ADDR (*(volatile unsigned int *)0x40000004)
#define SPI_FLASH_END_ADDR (*(volatile unsigned int *)0x40000008)


#define XADC_FIRST (*(volatile unsigned int *)0x8000107C)

#define ADC_START_ADDR (*(volatile unsigned int *)0x80002000)
#define ADC_AND_MASK (*(volatile unsigned int *)  0x80002004)
#define ADC_OR_MASK (*(volatile unsigned int *)   0x80002008)
#define ADC_SAMPLERATE (*(volatile unsigned int *)0x8000200C)
#define ADC_SETTINGS (*(volatile unsigned int *)  0x80002010)

#define GPIO_LEDS   (*(volatile unsigned int *)  0x80003000)
#define GPIO_INPUTS (*(volatile unsigned int *)  0x80003004)


#define FFT_AND_MASK (*(volatile unsigned int *)0x80003000)
#define FFT_OR_MASK (*(volatile unsigned int *)0x80003004)
#define FFT_SETTINGS (*(volatile unsigned int *)0x80003008)


#define BLOCK_MEM 0x20000000


#define AUDIO_MEM 0x30000000

#include <stdarg.h>

int sprintf(char *restrict s, const char *restrict fmt, ...)
{
	int ret;
	va_list ap;
	va_start(ap, fmt);
	ret = vsprintf(s, fmt, ap);
	va_end(ap);
	return ret;
}

void loop(){


    volatile unsigned short* value = AUDIO_MEM;

    while (1)
    {
        SEVEN_SEGMENT = value[0];
    }
}

#define one 0xff
void fillValues(){

    short vector[] = {one,one,one,0,0,0,0,0,one,one,one,0,0,0,0,0,
        one,one,one,0,0,0,0,0,one,one,one,0,0,0,0,0,
        one,one,one,0,0,0,0,0,one,one,one,0,0,0,0,0,
        one,one,one,0,0,0,0,0,one,one,one,0,0,0,0,0,
        one,one,one,0,0,0,0,0,one,one,one,0,0,0,0,0,
        one,one,one,0,0,0,0,0,one,one,one,0,0,0,0,0,
        one,one,one,0,0,0,0,0,one,one,one,0,0,0,0,0,
        one,one,one,0,0,0,0,0,one,one,one,0,0,0,0,0};

    for(int i = 0; i < 128; i++){
        ((int*)AUDIO_MEM)[i] = vector[i];
    }

    for(int i = 0; i < 128; i++){
        if(((int*)AUDIO_MEM)[i] != vector[i]){
            printstr("Failed",sizeof("Failed"));
        };
    }
}

void calcFFT(){
    FFT_AND_MASK = 256*4-1;
    FFT_OR_MASK = AUDIO_MEM;
    FFT_SETTINGS = 0x1; //Start bit

    while(FFT_SETTINGS != 0); //While running    
}

void printFFT(){

    char buffer[512];

    volatile unsigned int* value = AUDIO_MEM + 128*4;

    int length = 0;

    for(int i = 0; i < 128; i++){
        int val = value[i];



        length = sprintf(buffer,"%03i, 0x%08x",i,val);


        
        printstr(buffer,length);
        printstr("\n",sizeof("\n"));
        
    }
}

int main()
{

    SEVEN_SEGMENT = 0x4242;
    //ADC_START_ADDR = AUDIO_MEM;
    //ADC_AND_MASK   = 0x400 - 1; //1024 addresses
    //ADC_AND_MASK   = 0x010 - 1; //1024 addresses
    //ADC_OR_MASK    = AUDIO_MEM; //Always save at AUDIO mem
    //ADC_SAMPLERATE = 200;
    //ADC_SETTINGS   = 1;
    
    printstr("Applied settings\n",sizeof("Applied settings\n"));

    //loop();

    fillValues();

    printstr("Filled\n",sizeof("Filled\n"));


    calcFFT();

    printstr("Calculated\n",sizeof("Calculated\n"));

    printFFT();

    printstr("Done\n",sizeof("Done\n"));



    return 0;
}