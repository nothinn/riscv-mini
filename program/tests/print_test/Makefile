
RISCV_GCC     ?= riscv64-unknown-elf-gcc
RISCV_LINKER  ?= riscv64-unknown-elf-ld
RISCV_OBJDUMP ?= riscv64-unknown-elf-objdump
RISCV_OBJCOPY ?= riscv64-unknown-elf-objcopy

SBT_COMMAND ?= sbt "testOnly 

TOOLS ?= ../../tools

#COPY_FLAGS ?= --change-addresses -0x001fe00000

#Used architechture. Can be set in makefile above if wanted.
#Can also be "hardcoded" to test that an instruction set works.
RISCV_ARCH ?= rv32i
RISCV_ABI  ?= ilp32

#Add all needed include directories
IDIR   += ../../common/
#Add all directories with sources
SRCDIR += ../../common/ ./src/


CC = $(RISCV_GCC)
LINKER = $(RISCV_GCC)


CFLAGS=-I$(IDIR) -mabi=$(RISCV_ABI) -march=$(RISCV_ARCH) -static -nostdlib -nostartfiles -lm -lgcc
RISCV_LDFLAGS = -T ../../common/program.ld ../../common/crt.S \
	-static -nostdlib -nostartfiles -lm -lgcc \
	-march=$(RISCV_ARCH) -mabi=$(RISCV_ABI)

LFLAGS=$(RISCV_LDFLAGS)


OBJDIR   = obj
BINDIR   = bin

SOURCES := $(wildcard $(addsuffix *.c,$(SRCDIR)))
OBJECTS := $(patsubst %.c,%.o,$(SOURCES))

#Make hex
bin/main.hex: $(BINDIR)
	$(RISCV_OBJCOPY) -O $(DUMP_FLAGS) ihex $</main.riscv $</main.hex -g
	cd bin && python3 ../$(TOOLS)/ihex2hex.py main.hex

#Linking
$(BINDIR): $(OBJECTS)
	$(LINKER) $(addprefix obj/,$(notdir $(OBJECTS))) $(LFLAGS) -o $@/main.riscv
	@echo "Linking complete!"
	$(RISCV_OBJDUMP) -D $(BINDIR)/main.riscv > dump.txt

#Compiling
$(OBJECTS): %.o : %.c
	$(CC) $(CFLAGS) -c $< -o $(OBJDIR)/$(@F)
	@echo "Compiled "$<" successfully!"


test:


.PHONY: clean

clean:
	rm -rf bin/*
	rm -rf obj/*
	rm dump.txt