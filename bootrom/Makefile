base_dir = $(abspath ..)


RISCV_TEST_DIR     ?= $(base_dir)/bootrom

CUSTOM_BMARK_BIN   ?= main
CUSTOM_BMARK_C_SRC ?= main.c syscalls.c
CUSTOM_BMARK_S_SRC ?= crt.S

CUSTOM_BMARK_OBJS := \
	$(foreach f,$(CUSTOM_BMARK_C_SRC),$(patsubst %.c,%.o,$f)) \
	$(foreach f,$(CUSTOM_BMARK_S_SRC),$(patsubst %.S,%.o,$f))

#riscv_bmark_dir = $(RISCV_TEST_DIR)/benchmarks

RISCV_GCC     = riscv64-unknown-elf-gcc
RISCV_OBJDUMP = riscv64-unknown-elf-objdump
RISCV_CFLAGS  = -static -std=gnu99 -fno-common -fno-builtin-printf \
	-I$(RISCV_TEST_DIR)/env -I$(RISCV_TEST_DIR)/common -march=rv32iac -mabi=ilp32
RISCV_LDFLAGS = -T $(RISCV_TEST_DIR)/common/test.ld \
	-I$(RISCV_TEST_DIR)/env -I$(RISCV_TEST_DIR)/common -march=rv32iac -mabi=ilp32

VPATH += $(RISCV_TEST_DIR)/common 

romgen: test

all: clean $(CUSTOM_BMARK_BIN) $(CUSTOM_BMARK_BIN).hex $(CUSTOM_BMARK_BIN).dump

test: clean
	$(RISCV_GCC) -I$(RISCV_TEST_DIR)/env -I$(RISCV_TEST_DIR)/common -static -std=gnu99 -O2 -mabi=ilp32 -march=rv32i -o main.riscv main.c $(RISCV_TEST_DIR)/common/syscalls.c $(RISCV_TEST_DIR)/common/crt.S -static -nostdlib -nostartfiles -lm -lgcc -T $(RISCV_TEST_DIR)/common/test.ld
	$(RISCV_OBJDUMP) -d main.riscv > main.dump.text
	#Dump data section
	$(RISCV_OBJDUMP) -D --start-address=0x10000 --stop-address=0x20000 main.riscv > main.dump.data
	


%.o: %.c
	$(RISCV_GCC) $(RISCV_CFLAGS) -c -o $@ $<

%.o: %.S
	$(RISCV_GCC) $(RISCV_CFLAGS) -D__ASSEMBLY__=1 -c -o $@ $<

$(CUSTOM_BMARK_BIN): $(CUSTOM_BMARK_OBJS)
	$(RISCV_GCC) $(RISCV_LDFLAGS) -o $@ $^ -nostdlib -nostartfiles -lc -lgcc

%.hex: %
	elf2hex 16 32768 $< > $@

%.dump: %
	riscv64-unknown-elf-objdump \
	--disassemble-all --disassemble-zeroes --section=.text --section=.text.startup --section=.data \
	$< > $@

clean:
	rm -rf *.o $(CUSTOM_BMARK_BIN) $(CUSTOM_BMARK_BIN).hex $(CUSTOM_BMARK_BIN).dump

.PHONY: all clean
