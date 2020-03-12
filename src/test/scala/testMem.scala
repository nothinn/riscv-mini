import chisel3._
import chisel3.iotesters._

import org.scalatest._

import mini._
import peripherals.MyMem

class MemTester(dut: MyMem, size: Int) extends PeekPokeTester(dut) {

  val array = Array.fill[Int](size)(0) //Array to compare with

  //Try to write random data into the array, one byte at a time. This should fail because valid is not asserted
  for (i <- 0 until size) {
    step(1)
    poke(dut.io.req.bits.addr, i)
    val value = rnd.nextInt() & 0xff
    poke(dut.io.req.bits.data, value)
    //array(i) = value
  }

  step(100)

  //expect to only read zeros.
  for (i <- 0 until size) {
    poke(dut.io.req.bits.addr, i)
    step(1)
    expect(dut.io.resp.bits.data, array(i), "Should only contain zeroes")
  }

  step(100)

  //Write random values into the memory, one byte at a time
  poke(dut.io.req.valid,1)
  poke(dut.io.req.bits.mask,1)
  for (i <- 0 until size) {
    poke(dut.io.req.bits.addr, i)
    val value = rnd.nextInt() & 0xff
    poke(dut.io.req.bits.data, value)
    array(i) = value
    step(1)
  }
  poke(dut.io.req.bits.mask,0)
  step(100)

  for (i <- 0 until size) {
    poke(dut.io.req.bits.addr, i)
    step(1)
    val received = peek(dut.io.resp.bits.data) & 0xff
    expect((received).toInt == array(i), received.toString() + " should match array: " + array(i).toString())
  
  }

  step(100)


}

class TestMem extends FlatSpec with Matchers {

  implicit val p = (new FPGAConfig).toInstance
  "TestMem" should "pass" in {
    chisel3.iotesters.Driver.execute(
      Array(
        //"--backend-name",
        //"verilator",
        "--generate-vcd-output",
        "on",
        "--target-dir",
        "test_run_dir/TestMem",
        "--top-name",
        "TestMem"
      ),
      () => new MyMem(1024, 32)
    ) { c =>
      new MemTester(c, 1024)
    } should be(true)
  }
}
