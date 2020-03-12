OUTPUT_ARCH( "riscv" )
ENTRY(_start)

SECTIONS
{
  . = 0x20000000;
  .text.init : { *(.text.init) }
  .text : { *(.text) }
  
}