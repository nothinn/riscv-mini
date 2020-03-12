if {$argc < 2 || $argc > 3} {
	puts $argc
	puts {Error: Invalid number of arguments}
	puts {Usage: gen_mcs.tcl project mcsfile [datafile]}
	exit 1
}
lassign $argv project mcsfile datafile

lassign $project.bit bitfile



write_cfgmem -format mcs -interface spix4 -size 4 \
	-loadbit "up 0x0 $bitfile" \
	-loaddata [expr {$datafile ne "" ? "up 0x200000 $datafile" : ""}] \
	-file $mcsfile -force