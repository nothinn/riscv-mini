package peripherals

import chisel3._
import chisel3.util._

import freechips.rocketchip.config.{Field, Parameters}

class XADCIO extends Bundle {
  val vauxp6 = Input(Bool())
  val vauxn6 = Input(Bool())
  val vauxp7 = Input(Bool())
  val vauxn7 = Input(Bool())
  val vauxp14 = Input(Bool())
  val vauxn14 = Input(Bool())
  val vauxp15 = Input(Bool())
  val vauxn15 = Input(Bool())
  val vn_in = Input(Bool())
  val vp_in = Input(Bool())
}

class XLXI_7_IO() extends Bundle{
    val daddr_in = Input(UInt(7.W)) // (Address_in),
    val dclk_in = Input(Clock()) // (CLK100MHZ),
    val den_in = Input(Bool()) // (enable),
    val di_in = Input(Bool()) // (0),
    val dwe_in = Input(Bool()) // (0),
    val busy_out = Output(Bool()) // (),
    val vauxp6 = Input(Bool()) //(vauxp6),
    val vauxn6 = Input(Bool()) //(vauxn6),
    val vauxp7 = Input(Bool()) //(vauxp7),
    val vauxn7 = Input(Bool()) //(vauxn7),
    val vauxp14 = Input(Bool()) //(vauxp14),
    val vauxn14 = Input(Bool()) //(vauxn14),
    val vauxp15 = Input(Bool()) //(vauxp15),
    val vauxn15 = Input(Bool()) //(vauxn15),
    val vn_in = Input(Bool()) // (vn_in),
    val vp_in = Input(Bool()) // (vp_in),
    val alarm_out = Output(Bool()) // (),
    val do_out = Output(UInt(16.W)) // (data),
    val eoc_out = Output(Bool()) // (enable),
    val channel_out = Output(Bool()) // (),
    val drdy_out = Output(Bool()) // (ready)
}

trait XLXI_IO{
    def io : XLXI_7_IO
}

class XLXI_7() extends BlackBox with XLXI_IO {
  val io = IO(new XLXI_7_IO())
}

class XLXI_7_sim extends Module with XLXI_IO {
  val io = IO(new XLXI_7_IO())

  //io.daddr_in := DontCare
  //io.dclk_in := clock
  //io.den_in := DontCare
  //io.di_in := DontCare
  //io.dwe_in := DontCare
  io.busy_out := DontCare
  //io.vauxp6 := DontCare
  //io.vauxn6 := DontCare
  //io.vauxp7 := DontCare
  //io.vauxn7 := DontCare
  //io.vauxp14 := DontCare
  //io.vauxn14 := DontCare
  //io.vauxp15 := DontCare
  //io.vauxn15 := DontCare
  //io.vn_in := DontCare
  //io.vp_in := DontCare
  io.alarm_out := 0.B
  val counter = RegInit(0.U(16.W))
  counter := counter + 1.U
  io.do_out :=  io.daddr_in ## counter
  io.eoc_out := 0.B
  io.channel_out := 0.U

  

  val ready = RegInit(0.B)
  ready := ~ready
  io.drdy_out := ready
}

class XADC(val index: Int, val address: Long, val sim: Boolean)(implicit val p: Parameters) extends MMIOModule {
  val mmio = IO(new MMIO())
  val pins = IO(new XADCIO())

  val numBytes = 64 * 4 //64 registers, word addressed
  val moduleName = "XADC"

  val xadcModule = (if (sim) Module(new XLXI_7_sim()) else Module(new XLXI_7()))

  val xadc = xadcModule.io

  val ready = Wire(Bool())

  val addr = Wire(UInt(16.W))
  val addrReg = Reg(UInt(16.W))

  when(mmio.req.valid) {
    addr := mmio.req.bits.addr(31,2)
    addrReg := mmio.req.bits.addr(31,2)
  }.otherwise {
    addr := addrReg
  }

  mmio.resp.valid := RegNext(mmio.req.valid)// && ready
  mmio.resp.bits.data := xadc.do_out

  //Input pins, measuring voltage
  xadc.vauxp6 := pins.vauxp6
  xadc.vauxn6 := pins.vauxn6
  xadc.vauxp7 := pins.vauxp7
  xadc.vauxn7 := pins.vauxn7
  xadc.vauxp14 := pins.vauxp14
  xadc.vauxn14 := pins.vauxn14
  xadc.vauxp15 := pins.vauxp15
  xadc.vauxn15 := pins.vauxn15
  xadc.vn_in := pins.vn_in
  xadc.vp_in := pins.vp_in

  xadc.daddr_in := addr

  xadc.dclk_in := clock

  xadc.di_in := 0.B
  xadc.dwe_in := 0.B

  val enable = Wire(Bool())
  xadc.den_in := enable
  xadc.eoc_out <> enable

  ready := xadc.drdy_out

}
