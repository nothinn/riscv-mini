OUTPUT_ARCH( "riscv" )
ENTRY(_start)

MEMORY{
  BLOCKRAM (rwx) : ORIGIN = 0x20000000, LENGTH = 256K
}

SECTIONS
{
  PROVIDE(__stack_top$ = ORIGIN(BLOCKRAM) + LENGTH(BLOCKRAM));

  . = 0x20000000;
  .text.init : { *(.text.init) }
  .text : {
        KEEP(*(.stack))
        KEEP(*(.vectors))
        KEEP(*(.text))
        . = ALIGN(4);
        KEEP(*(.rodata))
        . = ALIGN(4);
    } >BLOCKRAM

  .sdata : {
    __global_pointer$ = . + 0x800;
  } >BLOCKRAM
}