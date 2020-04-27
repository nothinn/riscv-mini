// See LICENSE for license details.

package mini

import chisel3._
import chisel3.util._
import freechips.rocketchip.config.Parameters

// Multiplication and division ALU

object MALU {
  val MALU_MUL    = 0.U(4.W)
  val MALU_MULH   = 1.U(4.W)
  val MALU_MULHU  = 2.U(4.W)
  val MALU_MULHSU = 3.U(4.W)
  val MALU_DIV    = 4.U(4.W)
  val MALU_DIVU   = 5.U(4.W)
  val MALU_REM    = 6.U(4.W)
  val MALU_REMU   = 7.U(4.W)
  
  val MALU_XXX    = 15.U(4.W)
}

class MALUIo(implicit p: Parameters) extends CoreBundle()(p) {
  val rs1 = Input(UInt(xlen.W))
  val rs2 = Input(UInt(xlen.W))
  val alu_op = Input(UInt(4.W))

  val out = Output(UInt(xlen.W))
}

import mini.MALU._

abstract class MALU(implicit val p: Parameters) extends Module with CoreParams {
  val io = IO(new MALUIo)
  def logMALUOp(op: Int): Unit = {
    when(io.alu_op === op.U) {
      printf("A == %d, B == %d, opcode == %d\n", io.rs1, io.rs2, io.alu_op)
    }
  }
}

class MALUImplSingleCycle(implicit p: Parameters) extends MALU()(p) {

  val op1 = io.rs1
  val op2 = io.rs2

  

  //ToDo: Implement limiter such that too high values will get clipped to max/min
  io.out := MuxLookup(io.alu_op, DontCare, Seq(
      MALU_MUL  -> (op1 * op2)(xlen-1,0),
      MALU_MULH  -> (op1.asSInt * op2.asSInt())(xlen*2-1, xlen).asUInt,
      MALU_MULHU  -> (op1 * op2)(xlen*2-1, xlen),
      MALU_MULHSU  -> (op1.asSInt * op2)(xlen*2-1, xlen).asUInt,
      
      MALU_DIV -> (op1.asSInt / op2.asSInt()).asUInt,
      MALU_DIVU -> (op1 / op2),

      MALU_REM -> (op1.asSInt % op2.asSInt()).asUInt,
      MALU_REMU-> (op1 % op2),

      MALU_XXX  -> DontCare
      
      ))

}
