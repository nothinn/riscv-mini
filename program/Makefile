base_dir = $(abspath ..)


RISCV_TEST_DIR     ?= $(base_dir)/program

CUSTOM_BMARK_BIN   ?= main
CUSTOM_BMARK_C_SRC ?= main.c syscalls.c
CUSTOM_BMARK_S_SRC ?= crt.S

CUSTOM_BMARK_OBJS := \
	$(foreach f,$(CUSTOM_BMARK_C_SRC),$(patsubst %.c,%.o,$f)) \
	$(foreach f,$(CUSTOM_BMARK_S_SRC),$(patsubst %.S,%.o,$f))

#riscv_bmark_dir = $(RISCV_TEST_DIR)/benchmarks

RISCV_GCC     = riscv64-unknown-elf-gcc
RISCV_OBJDUMP = riscv64-unknown-elf-objdump
RISCV_OBJCOPY = riscv64-unknown-elf-objcopy

RISCV_CFLAGS  = -static -std=gnu99 -fno-common -fno-builtin-printf \
	-I$(RISCV_TEST_DIR)/env -I$(RISCV_TEST_DIR)/common -march=rv32iac -mabi=ilp32
RISCV_LDFLAGS = -T $(RISCV_TEST_DIR)/common/test.ld \
	-I$(RISCV_TEST_DIR)/env -I$(RISCV_TEST_DIR)/common -march=rv32iac -mabi=ilp32

VPATH += $(RISCV_TEST_DIR)/common 

romgen: test

all: clean $(CUSTOM_BMARK_BIN) $(CUSTOM_BMARK_BIN).hex $(CUSTOM_BMARK_BIN).dump

test: clean
	$(RISCV_GCC) -I$(RISCV_TEST_DIR)/env -I$(RISCV_TEST_DIR)/common -static -std=gnu99 -O2 -mabi=ilp32 -march=rv32i -o main.riscv main.c $(RISCV_TEST_DIR)/common/crt.S $(RISCV_TEST_DIR)/common/UART.c -static -nostartfiles -lm -lgcc -T $(RISCV_TEST_DIR)/common/program.ld
	$(RISCV_OBJDUMP) -D main.riscv > main.dump.text
	$(RISCV_OBJCOPY) --change-addresses -0x001fe00000 -O ihex main.riscv main.hex -g
	#python3 tools/hex_float_to_fixed.py main.hex main.dump.text -0x001fe00000
	#mv main.hex.fixed main.fixed.hex
	mv main.hex main.fixed.hex
	python3 tools/ihex2hex.py main.fixed.hex
	$(RISCV_OBJCOPY) -I ihex -O binary main.fixed.hex main.bin
	
	
tf:
	cd /home/simon/git/tensorflow/ && make -f tensorflow/lite/micro/tools/make/Makefile TARGET=riscv_mini micro_speech_test
	cp /home/simon/git/tensorflow/tensorflow/lite/micro/tools/make/gen/riscv_mini_rv32i/bin/micro_speech_test .
	$(RISCV_OBJDUMP) -D micro_speech_test > dump.txt
	$(RISCV_OBJCOPY) --change-addresses -0x001fe00000 -O ihex micro_speech_test main.hex
	python ihex2hex.py main.hex
	#cd .. && sbt "testOnly TestFPGAMultiProcess"

%_test:
	cd /home/simon/git/tensorflow/ && make -B -f tensorflow/lite/micro/tools/make/Makefile TARGET=riscv_mini $@
	cp /home/simon/git/tensorflow/tensorflow/lite/micro/tools/make/gen/riscv_mini_rv32i/bin/$@ .
	$(RISCV_OBJDUMP) -D $@ > dump.txt
	$(RISCV_OBJCOPY) --change-addresses -0x001fe00000 -O ihex $@ main.hex
	python ihex2hex.py main.hex
	sbt "testOnly TestFPGAMultiProcess"

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


spike:
	spike --isa=rv32i $(RISCV)/riscv32-unknown-elf/bin/pk main.riscv