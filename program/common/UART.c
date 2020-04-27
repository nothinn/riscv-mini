
#include "defines.h"

void printstr(const char* s, int length){
    UART_START = (int)s;
    UART_END = (int)s + length;
}