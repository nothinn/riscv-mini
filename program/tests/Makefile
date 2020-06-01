#This makefile goes through all subdirectories and calls their makefile. After this, it creates a file for each of the functional tests

#The submake must generate the working file: bin/main.hex

SUBDIRS := $(wildcard */.)

export RISCV_ARCH = rv32i


#Mark certain folders as don't run:
DONTRUN := tf_micro_speech_test/. print_test/. simple_op_test/. swap_instruction_test/. #tf_hello_world_test/.



compile: clean $(SUBDIRS)
$(SUBDIRS):
	
	if [ '$(filter $@,$(DONTRUN))' = '' ] ; then $(MAKE) -C $@ ; echo program/tests/$(subst .,bin/main.hex,$@) >> testList.txt ; fi


test: compile
	cd ../.. && sbt "testOnly TestAllFirmwareTest"

clean:
	rm -f *.txt

.PHONY: compile clean test $(SUBDIRS)
