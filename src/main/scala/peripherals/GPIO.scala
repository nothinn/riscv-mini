package peripherals

import chisel3._
import chisel3.util._

import freechips.rocketchip.config.{Field, Parameters}

class GPIOIO extends Bundle{
    val switches = Input(Vec(16,Bool()))
    val leds     = Output(Vec(16,Bool()))
    val but_l    = Input(Bool())
    val but_r    = Input(Bool())
    val but_u    = Input(Bool())
    val but_d    = Input(Bool())
    val but_c    = Input(Bool())
}

class GPIOMMIO(val address: Long, val index : Int)(implicit val p: Parameters) extends MMIOModule with PinIO {

  val numBytes = 8 //21 inputs and 16 outputs

  val mmio = IO(new MMIO())
  val pins = IO(new GPIOIO())

  val moduleName = "GPIO_" + index.toString()

  val leds = Reg(Vec(16,Bool()))
  val out = Reg(UInt(32.W))

  pins.leds := leds

  mmio.resp.bits.data := out
  mmio.resp.valid := RegNext(mmio.req.valid)

  when(mmio.req.valid) {
    switch(mmio.req.bits.addr) {
      is(0.U) {
        when(mmio.req.bits.mask =/= 0.U){
          leds := mmio.req.bits.data(15,0).asBools
        }
        out := leds.asUInt()
      }
      is(4.U){
        out := pins.but_l ## pins.but_r ## pins.but_u ## pins.but_d ## pins.but_c ##  pins.switches.asUInt
      }
    }
  }
}