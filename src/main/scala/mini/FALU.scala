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

  io.out := MuxLookup(io.alu_op, io.rs2, Seq(
      FALU_ADD  -> (io.rs1 + io.rs2),
      FALU_SUB  -> (io.rs1 - io.rs2),

      
      FALU_XXX  -> 0.U,
      
      ))

}
