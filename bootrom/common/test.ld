OUTPUT_ARCH( "riscv" )
ENTRY(_start)

SECTIONS
{
  . = 0x0200;
  .text.init : { *(.text.init) }
  .text : { *(.text) }

  . = 0x10000;
  /*ROM data for bootloader*/
  .rodata : { *(.rodata) }

  /*Data area*/
  . = 0x4000000;
  .data : { *(.data) }
  .bss : { *(.bss) }


  .sdata : {
    __global_pointer$ = 0x20000000 + 0x800;
    *(.srodata.cst16) *(.srodata.cst8) *(.srodata.cst4) *(.srodata.cst2) *(.srodata*)
    *(.sdata .sdata.* .gnu.linkonce.s.*)
  }

}