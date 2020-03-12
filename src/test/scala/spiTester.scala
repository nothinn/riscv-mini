import chisel3._
import chisel3.iotesters._

import org.scalatest._

import peripherals._

class TestSPItest(dut: SPI) extends PeekPokeTester(dut) {
  poke(dut.io.addressStart, 42)
  poke(dut.io.addressEnd, 64)
  poke(dut.io.readWrite, 0)
  poke(dut.io.strobe, 0)

  step(1)

  while (peek(dut.io.busy) == BigInt(1)) {
    step(1)
  }

  poke(dut.io.strobe, 1)
  step(1)
  poke(dut.io.strobe, 0)
  for (i <- 0 until 64) { //Pass first 4 bytes
    step(1)
  }

  for (value <- 42 to 64) {

    for (bit <- 0 until 8) {
      poke(dut.io.pins.sdo, (value >> 7-bit) & 0x1)
      step(2)
    }
  }

  while (peek(dut.io.busy) == BigInt(1)) {
    step(1)
  }

    step(1)

  while (peek(dut.io.busy) == BigInt(1)) {
    step(1)
  }

  poke(dut.io.strobe, 1)
  step(1)
  poke(dut.io.strobe, 0)
  for (i <- 0 until 64) { //Pass first 4 bytes
    step(1)
  }

  for (value <- 42 to 64) {

    for (bit <- 0 until 8) {
      poke(dut.io.pins.sdo, (value >> 7-bit) & 0x1)
      step(2)
    }
  }

  while (peek(dut.io.busy) == BigInt(1)) {
    step(1)
  }

}

class TestSPI extends FlatSpec with Matchers {
  "TestSPI" should "pass" in {
    chisel3.iotesters.Driver.execute(
      Array(
        "--backend-name",
        "verilator",
        "--generate-vcd-output",
        "on",
        "--target-dir",
        "test_run_dir/testSPI",
        "--top-name",
        "TestSPI"
      ),
      () => new SPI(33000000)
    ) { c =>
      new TestSPItest(c)
    } should be(true)
  }
}
