// See LICENSE for license details.

package mini

import chisel3._
import chisel3.util._
import freechips.rocketchip.config.Parameters

// Fixed point ALU

object FALU {
  val FALU_ADD    = 0.U(4.W)
  val FALU_SUB    = 1.U(4.W)
  val FALU_MUL    = 2.U(4.W)
  val FALU_DIV    = 3.U(4.W)
  val FALU_MINMAX = 4.U(4.W)
  val FALU_SQRT   = 5.U(4.W)

  val FALU_CNV_W  = 6.U(4.W)
  val FALU_CNV_WU = 7.U(4.W)
  
  
  val FALU_XXX    = 15.U(4.W)
}

class FALUIo(implicit p: Parameters) extends CoreBundle()(p) {
  val rs1 = Input(UInt(flen.W))
  val rs2 = Input(UInt(flen.W))
  val rs3 = Input(UInt(flen.W))
  val alu_op = Input(UInt(4.W))

  val out = Output(UInt(flen.W))
}

import mini.FALU._

abstract class FALU(implicit val p: Parameters) extends Module with CoreParams {
  val io = IO(new FALUIo)
  def logFALUOp(op: Int): Unit = {
    when(io.alu_op === op.U) {
      printf("A == %d, B == %d, opcode == %d\n", io.rs1, io.rs2, io.alu_op)
    }
  }
}

class FALUImpl(implicit p: Parameters) extends FALU()(p) {


  val MAX_VALUE = WireDefault(0x7fffffffL.S.asFixedPoint(16.BP))
  val MIN_VALUE = WireDefault((-2147483648).S.asFixedPoint(16.BP))

  val op1 = io.rs1.asFixedPoint(16.BP)
  val op2 = io.rs2.asFixedPoint(16.BP)
  val op3 = io.rs3.asFixedPoint(16.BP)

  val result = WireDefault(0.F(64.W,32.BP))
  //ToDo: Implement limiter such that too high values will get clipped to max/min
  result := MuxLookup(io.alu_op, op2, Seq(
      FALU_ADD  -> (op1 + op2),
      FALU_SUB  -> (op1 - op2),
      FALU_MUL  -> (op1 * op2),
      FALU_DIV  -> (op1 / op2),
      

      FALU_XXX  -> (0.F(32.W,16.BP))
      
      ))

  val clipped = WireDefault(0.F(32.W,16.BP))

  when(result > MAX_VALUE){
    clipped := MAX_VALUE
  }.elsewhen(result < MIN_VALUE){
    clipped := MIN_VALUE
  }.otherwise{
    clipped := result
  }


  when(io.alu_op === FALU_CNV_W || io.alu_op === FALU_CNV_WU){
    io.out := (op1.asUInt) >> 16
  }.otherwise{
    io.out := clipped.asUInt()

  }

}
