
RISCV_GCC     ?= riscv64-unknown-elf-gcc
RISCV_OBJDUMP ?= riscv64-unknown-elf-objdump
RISCV_OBJCOPY ?= riscv64-unknown-elf-objcopy
export RISCV_ARCH    ?= rv32im


default:
	mkdir -p bin
	cd /home/simon/git/tensorflow/ && make -f tensorflow/lite/micro/tools/make/Makefile TARGET=riscv_mini hello_world_test
	cp /home/simon/git/tensorflow/tensorflow/lite/micro/tools/make/gen/riscv_mini_$(RISCV_ARCH)/bin/hello_world_test bin/
	$(RISCV_OBJDUMP) -D bin/hello_world_test > dump.txt
	$(RISCV_OBJCOPY) --change-addresses -0x001fe00000 -O ihex bin/hello_world_test bin/main.hex
	python3 ../../tools/ihex2hex.py bin/main.hex



clean:
	rm -rf bin/
	rm dump.txt
