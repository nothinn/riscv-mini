	.file	"relocation.c"
	.option nopic
	.section	.text.startup,"ax",@progbits
	.align	1
	.globl	main
	.type	main, @function
main:
	lui	a5,%hi(global_symbol)
	ld	a0,%lo(global_symbol)(a5)
	snez	a0,a0
	ret
	.size	main, .-main
	.comm	global_symbol,16,8
	.ident	"GCC: (GNU) 7.2.0"
