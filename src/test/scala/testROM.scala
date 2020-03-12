/*import chisel3._
import chisel3.iotesters._

import org.scalatest._

import mini._

class TestROM(dut: ROM) extends PeekPokeTester(dut){
    for(i <- 0 until 32){
        poke(dut.io.addr, i.U)
        step(1)
        println(peek(dut.io.data).toString(16)  )
    }
}


class TestROMAll extends FlatSpec with Matchers{
    "TestROM" should "pass" in {
        val path = "/home/simon/riscv-mini/bootrom/main.dump"
        chisel3.iotesters.Driver(() => new ROM(1024,32,path)){c => new TestROM(c)} should be (true)
    }
}*/