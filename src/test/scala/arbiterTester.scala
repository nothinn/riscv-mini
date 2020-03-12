import chisel3._
import chisel3.iotesters._

import chisel3.util._

import org.scalatest._

import peripherals._
import mini._
import chisel3.util.Arbiter

class TestArbiterFPGA(dut: Arbiter[MemoryReq]) extends PeekPokeTester(dut) {

  step(1)
  poke(dut.io.out.ready, 0)
  step(1)

  step(1)
  for (i <- 0 until 4) {
    poke(dut.io.in(i).bits.addr, (i + 1) * 4)
    poke(dut.io.in(i).bits.data, (i + 1) * 8)

    poke(dut.io.in(i).valid, 1)
  }

  step(1)
  for (j <- 0 until 10) {
    step(1)
    poke(dut.io.out.ready, j & 0x1)

    for (i <- 0 until 4) {
      println(peek(dut.io.in(i).ready).toString())
      println(peek(dut.io.chosen).toString())

      if (peek(dut.io.in(i).ready) == 1 && peek(dut.io.chosen) == i) {
        println("HERE")
      }
    }
    poke(dut.io.in(peek(dut.io.chosen).asUInt()).valid, 0)
  }

  step(4)

}
/*
class TestArb extends FlatSpec with Matchers {
  "TestArb" should "pass" in {
    implicit val p = (new FPGAConfig).toInstance
    chisel3.iotesters.Driver.execute(
      Array(
        "--backend-name",
        "verilator",
        "--generate-vcd-output",
        "on",
        "--target-dir",
        "test_run_dir/testArb",
        "--top-name",
        "TestArb"
      ),
      () => new Arbiter(new MemoryReq, 4)
    ) { c =>
      new TestArbiterFPGA(c)
    } should be(true)
  }
}
*/
