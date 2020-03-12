default: compile

base_dir   = $(abspath .)
src_dir    = $(base_dir)/src/main
gen_dir    = $(base_dir)/generated-src
out_dir    = $(base_dir)/outputs


BUILD_DIR := $(base_dir)/builds/basys3Build
FPGA_DIR := $(base_dir)/fpga-shells/xilinx
MODEL := FPGAChip
PROJECT := mini.MainBasys3FPGA
export CONFIG := FPGAConfig
export BOARD := basys3
export BOOTROM_DIR := $(base_dir)/bootrom


SBT       = sbt
SBT_FLAGS = -ivy $(base_dir)/.ivy2

sbt:
	$(SBT) $(SBT_FLAGS)

fpga: $(gen_dir)/fpga.v
$(gen_dir)/fpga.v: $(wildcard $(src_dir)/scala/*.scala)
	cd program && $(MAKE) test
	$(SBT) $(SBT_FLAGS) "run $(gen_dir)"

fpgatest:
	echo $(gen_dir)
	cd program && $(MAKE) test
	$(SBT) $(SBT_FLAGS) "testOnly TestFPGAMultiProcess $(gen_dir)"

gen_bit:
	cd vivado && pwd && vivado -mode batch -nolog -source gen_bit.tcl -tclargs basys3RISCV

gen_mcs:
	cd vivado && pwd && vivado -mode batch -nolog -source gen_mcs.tcl -tclargs basys3RISCV basys3.msc ../program/main.bin

gen_asic:
	scp generated-src/FPGASoC.v EDA4:./master/top.v 
	ssh -C EDA4 "cd master && tcsh -c \"source /apps/misc/02205/scripts/setup_synopsys_dv.csh && dc_shell -f scripts/compile.tcl\" " > EDA4_log.txt
	#TO BE DONE: ssh -C EDA4 "cd master && tcsh -c \"source /apps/misc/02205/scripts/setup_icc.csh && icc_shell -shared_license 

compile: $(gen_dir)/Tile.v

$(gen_dir)/Tile.v: $(wildcard $(src_dir)/scala/*.scala)
	$(SBT) $(SBT_FLAGS) "run $(gen_dir)"

CXXFLAGS += -std=c++11 -Wall -Wno-unused-variable

# compile verilator
VERILATOR = verilator --cc --exe
VERILATOR_FLAGS = --assert -Wno-STMTDLY -O3 --trace \
	--top-module Tile -Mdir $(gen_dir)/VTile.csrc \
	-CFLAGS "$(CXXFLAGS) -include $(gen_dir)/VTile.csrc/VTile.h" 

$(base_dir)/VTile: $(gen_dir)/Tile.v $(src_dir)/cc/top.cc $(src_dir)/cc/mm.cc $(src_dir)/cc/mm.h
	$(VERILATOR) $(VERILATOR_FLAGS) -o $@ $< $(word 2, $^) $(word 3, $^)
	$(MAKE) -C $(gen_dir)/VTile.csrc -f VTile.mk

verilator: $(base_dir)/VTile

# isa tests + benchmarks with verilator
test_hex_files = $(wildcard $(base_dir)/src/test/resources/*.hex)
test_out_files = $(foreach f,$(test_hex_files),$(patsubst %.hex,%.out,$(out_dir)/$(notdir $f)))

$(test_out_files): $(out_dir)/%.out: $(base_dir)/VTile $(base_dir)/src/test/resources/%.hex
	mkdir -p $(out_dir)
	$^ $(patsubst %.out,%.vcd,$@) 2> $@

run-tests: $(test_out_files)

# run custom benchamrk
custom_bmark_hex ?= $(base_dir)/custom-bmark/main.hex
custom_bmark_out  = $(patsubst %.hex,%.out,$(out_dir)/$(notdir $(custom_bmark_hex)))
$(custom_bmark_hex):
	$(MAKE) -C custom-bmark

$(custom_bmark_out): $(base_dir)/VTile $(custom_bmark_hex)
	mkdir -p $(out_dir)
	$^ $(patsubst %.out,%.vcd,$@) 2> $@

run-custom-bmark: $(custom_bmark_out) 
	
# unit tests + integration tests 
test:
	$(SBT) $(SBT_FLAGS) test

clean:
	rm -rf $(gen_dir) $(out_dir) test_run_dir

cleanall: clean
	rm -rf target project/target

.PHONY: sbt compile verilator run-tests run-custom-bmark test clean cleanall


include common.mk
