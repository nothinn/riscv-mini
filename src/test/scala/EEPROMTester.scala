import chisel3._
import chisel3.iotesters._

import org.scalatest._

import peripherals._

class testEEPROMTester(dut: EEPROMEmulator) extends PeekPokeTester(dut) {

  //Write to memory:

  poke(dut.io.S_n, 1.B)
  poke(dut.io.C, 0.B)
  poke(dut.io.D, 0.B)

  step(10)

  poke(dut.io.S_n, 0.B) //Chip select

  //Send write instruction
  var instruction = 2

  var bit = 0

  for (bits <- 0 until 8) {
    bit = (instruction >> 7 - bits) & 0x1
    poke(dut.io.D, bit)

    step(1)
    poke(dut.io.C, 1.B)
    step(1)
    poke(dut.io.C, 0.B)
  }

  val address = 42
  //Send address
  for (byte <- 0 until 3) { //3 bytes of address
    for (bits <- 0 until 8) {
      bit = (address >> (2 - byte) * 8 + 8 - bits) & 0x1
      poke(dut.io.D, bit)

      step(1)
      poke(dut.io.C, 1.B)
      step(1)
      poke(dut.io.C, 0.B)
    }
  }

  //send bytes
  for (i <- 5 until 42) {
    for (bits <- 0 until 8) {
      bit = (i >> 8 - bits) & 0x1
      poke(dut.io.D, bit)

      step(1)
      poke(dut.io.C, 1.B)
      step(1)
      poke(dut.io.C, 0.B)
    }
  }
  poke(dut.io.S_n, 1.B)

  step(10)

  //Read bytes
  poke(dut.io.S_n, 0.B)

  //Send read instruction
  instruction = 3

  bit = 0

  for (bits <- 0 until 8) {
    bit = (instruction >> 7 - bits) & 0x1
    poke(dut.io.D, bit)

    step(1)
    poke(dut.io.C, 1.B)
    step(1)
    poke(dut.io.C, 0.B)
  }

  //Send address
  for (byte <- 0 until 3) { //3 bytes of address
    for (bits <- 0 until 8) {
      bit = (address >> (2 - byte) * 8 + 8 - bits) & 0x1
      poke(dut.io.D, bit)

      step(1)
      poke(dut.io.C, 1.B)
      step(1)
      poke(dut.io.C, 0.B)
    }
  }

  var value = 0
  //read bytes
  for (i <- 5 until 42) {
    value = 0
    for (bits <- 0 until 8) {
      //bit = (i >> 8 - bits) & 0x1
      //poke(dut.io.D, bit)


      step(1)
      poke(dut.io.C, 1.B)
      step(1)
      value = (value << 1 ) + peek(dut.io.Q).toInt
      poke(dut.io.C, 0.B)
    }
    println("Value: " + value.toString())

  }

  poke(dut.io.S_n, 1.B)
}

class TestEEPROM extends FlatSpec with Matchers {
  "TestEEPROM" should "pass" in {
    chisel3.iotesters.Driver.execute(
      Array(
//        "--backend-name",
//        "verilator",
        "--generate-vcd-output",
        "on",
        "--target-dir",
        "test_run_dir/testEEPROM",
        "--top-name",
        "TestEEPROM"
      ),
      () => new EEPROMEmulator(32000000, "none", 512)
    ) { c =>
      new testEEPROMTester(c)
    } should be(true)
  }
}
