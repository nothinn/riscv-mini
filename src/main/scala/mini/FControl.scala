// See LICENSE for license details.

package mini

import chisel3._
import chisel3.util.ListLookup
import freechips.rocketchip.config.Parameters

object FControl {
  val Y = true.B
  val N = false.B

  // st_type
  val F_ST_XXX = 0.U(2.W)
  val F_ST_SW  = 1.U(2.W)


  // ld_type
  val F_LD_XXX = 0.U(3.W)
  val F_LD_LW  = 1.U(3.W)

  // wb_sel
  val F_WB_ALU = 0.U(2.W)
  val F_WB_REG = 1.U(2.W)
  val F_WB_MEM = 2.U(2.W)

  import FALU._
  import Instructions._
  
  val default =
    //                                                   wb_en 
    //            Alu_op   kill    st_type  ld_type  wb_sel   | illegal?
    //              |       |      |        |        |       |  |   
             List(FALU_XXX, N, F_ST_XXX, F_LD_XXX, F_WB_ALU, N, Y)
  val map = Array(
    FADD_S-> List(FALU_ADD, N, F_ST_XXX, F_LD_XXX, F_WB_ALU, Y, N),
    FSUB_S-> List(FALU_SUB, N, F_ST_XXX, F_LD_XXX, F_WB_ALU, Y, N),
    
    )

    
}

class FControlSignals(implicit p: Parameters) extends CoreBundle()(p) {
  val inst      = Input(UInt(xlen.W))
  val inst_kill = Output(Bool())
  val alu_op    = Output(UInt(4.W))
  val st_type   = Output(UInt(2.W))
  val ld_type   = Output(UInt(3.W))
  val wb_sel    = Output(UInt(2.W))
  val wb_en     = Output(Bool())
  val illegal   = Output(Bool())
}

class FControl(implicit p: Parameters) extends Module {
  val io = IO(new FControlSignals)
  val ctrlSignals = ListLookup(io.inst, FControl.default, FControl.map)


  // FControl signals for Fetch
  io.inst_kill := ctrlSignals(1).asBool

  // FControl signals for Execute
  io.alu_op  := ctrlSignals(0)
  io.st_type := ctrlSignals(2)

  // FControl signals for Write Back
  io.ld_type := ctrlSignals(3)
  io.wb_sel  := ctrlSignals(4)
  io.wb_en   := ctrlSignals(5).asBool
  io.illegal := ctrlSignals(6)
}
