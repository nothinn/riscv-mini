// See LICENSE for license details.

package mini

import chisel3._
import chisel3.testers._
import chisel3.util._
import mini._

class FALUTester(falu: => FALU)(implicit p: freechips.rocketchip.config.Parameters) extends BasicTester with TestUtils {
  import FALU._
  val dut = Module(falu)
  val fctrl = Module(new FControl)
  val flen = p(FLEN)
  println("Number of instructions:")
  println(insts.size)

  val (cntr, done) = Counter(true.B, insts.size)
  val rs1  = Seq.fill(insts.size)(rnd.nextInt()) map toBigInt
  val rs2  = Seq.fill(insts.size)(rnd.nextInt()) map toBigInt
  val rs3  = Seq.fill(insts.size)(rnd.nextInt()) map toBigInt
  
  val add  = VecInit((rs1 zip rs2) map { case (a, b) => toBigInt(a.toInt + b.toInt).U(flen.W) })
  val diff = VecInit((rs1 zip rs2) map { case (a, b) => toBigInt(a.toInt - b.toInt).U(flen.W) })
  val and  = VecInit((rs1 zip rs2) map { case (a, b) => (a & b).U(flen.W) })
  val or   = VecInit((rs1 zip rs2) map { case (a, b) => (a | b).U(flen.W) })
  val xor  = VecInit((rs1 zip rs2) map { case (a, b) => (a ^ b).U(flen.W) })
  val slt  = VecInit((rs1 zip rs2) map { case (a, b) => (if (a.toInt < b.toInt) 1 else 0).U(flen.W) })
  val sltu = VecInit((rs1 zip rs2) map { case (a, b) => (if (a < b) 1 else 0).U(flen.W) })
  val sll  = VecInit((rs1 zip rs2) map { case (a, b) => toBigInt(a.toInt << (b.toInt & 0x1f)).U(flen.W) })
  val srl  = VecInit((rs1 zip rs2) map { case (a, b) => toBigInt(a.toInt >>> (b.toInt & 0x1f)).U(flen.W) })
  val sra  = VecInit((rs1 zip rs2) map { case (a, b) => toBigInt(a.toInt >> (b.toInt & 0x1f)).U(flen.W) })
  val swap = VecInit((rs1 zip rs2) map { case (a, b) => toBigInt(Integer.reverse(a.toInt)).U(flen.W)})
  val out = Mux(dut.io.alu_op === FALU_ADD,  add(cntr),
             Mux(dut.io.alu_op === FALU_SUB,  diff(cntr),
             Mux(dut.io.alu_op === FALU_MUL,  add(cntr),
             Mux(dut.io.alu_op === FALU_DIV,   add(cntr),
             Mux(dut.io.alu_op === FALU_MINMAX,  add(cntr),
             Mux(dut.io.alu_op === FALU_SQRT,  add(cntr), 0.U))))))

  val instructions = VecInit(insts)
  fctrl.io.inst := instructions(cntr)
  dut.io.alu_op := fctrl.io.alu_op
  dut.io.rs1 := VecInit(rs1 map (_.U))(cntr)
  dut.io.rs2 := VecInit(rs2 map (_.U))(cntr)
  dut.io.rs3 := VecInit(rs3 map (_.U))(cntr)


  when(done) { stop(); stop() } // from VendingMachine example...
  printf("Counter: %d, OP: 0x%x, RS1: 0x%x, RS2: 0x%x, RS3: 0x%x, OUT: 0x%x ?= 0x%x\n",
    cntr, dut.io.alu_op, dut.io.rs1, dut.io.rs2, dut.io.rs3, dut.io.out, out) 
  
  assert(dut.io.out === out)
}

class FALUTests extends org.scalatest.FlatSpec {
  implicit val p = (new MiniConfig).toInstance
  "FALUImpl" should "pass" in {
    assert(TesterDriver execute (() => new FALUTester(new FALUImpl())))
  }
}