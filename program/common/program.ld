OUTPUT_ARCH( "riscv" )
ENTRY(_start)




MEMORY{
/*  BOOTROM (rx) : ORIGIN = 0x0, LENGTH = 2K*/
  BLOCKRAM (rwx) : ORIGIN = 0x20000000, LENGTH = 536K
}

SECTIONS
{
  . = 0x20000000;
  .text.init : { *(.text.init) }
  .text : { *(.text) }

  .sdata    :
  {
    __global_pointer$ = . + 0x800;
  }
}