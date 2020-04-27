#include "UART.h"

#include "defines.h"



int addTest(){

    int a = 10;
    int b = 20;

    int c = a + b;
    if(c == 30){
        return 0;
    }else{
        return 1;
    }
}

int mulTest(){
    int a = 10;
    int b = 20;
    int c = a * b;
    if(c == 200){
        return 0;
    }else{
        return 2;
    }
}

int divTest(){
    int a = 10;
    int b = 20;
    int c = b / a;
    if(c == 2){
        return 0;
    }else{
        return 4;
    }
}



int main(){

    int test = 0;
    test += addTest();
    test += mulTest();
    test += divTest();


    if(test == 0){
        printstr(TEST_SUCCESS_STRING,TEST_SUCCESS_STRING_LENGTH);
    }else{
        printstr(TEST_FAILED_STRING,TEST_FAILED_STRING_LENGTH);
    }
}