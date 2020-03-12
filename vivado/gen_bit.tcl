if {$argc < 1 || $argc > 1} {
	puts $argc
	puts {Error: Invalid number of arguments}
	puts {Usage: gen_bit.tcl project}
	exit 1
}
lassign $argv project 

open_project $project/$project.xpr



update_compile_order

reset_runs synth_1
reset_runs synth_2
reset_runs impl_1


launch_runs synth_2 -jobs 12
wait_on_run -timeout 30 synth_2
if {[get_property PROGRESS [get_runs synth_2]] != "100%"} {
    error "ERROR: synth_2 failed"
}

launch_runs impl_1 -jobs 12
wait_on_run -timeout 30 impl_1
if {[get_property PROGRESS [get_runs impl_1]] != "100%"} {
    error "ERROR: impl_1 failed"
}
open_run impl_1

set_property BITSTREAM.CONFIG.SPI_BUSWIDTH 4 [current_design]
set_property BITSTREAM.GENERAL.COMPRESS TRUE [current_design]

write_bitstream -force $project 