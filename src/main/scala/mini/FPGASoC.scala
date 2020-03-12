// See LICENSE for license details.

package mini

import chisel3._
import chisel3.experimental.BaseModule
import chisel3.util._
import freechips.rocketchip.config.{Field, Parameters}
import junctions._

import peripherals._

import shell.xilinx.basys3shell._
import sifive.fpgashells.ip.xilinx._
import Chisel.experimental.chiselName

class FPGAIO()(implicit val p: Parameters) extends Bundle {
  val host = new HostIO()
  val leds = Output(UInt(16.W))
  val uart = new UARTIO()
  val sevenSeg = new SevenSegmentIO()
  val spi = new SPIIO()
  val debugBools = Output(Vec(8, new Bool()))
  val debugUInt = Output(UInt(8.W))

  val xadc = new XADCIO()

}

trait FPGABase extends BaseModule {
  def io:    FPGAIO
  def clock: Clock
  def reset: Reset
}

class BlockMem(size: Int, XLEN: Int)(implicit val p: Parameters) extends Module {
  val io = IO(new Bundle {
    val cacheio = new CacheIO
  })

  val mem = SyncReadMem(size, Vec(XLEN / 8, UInt(8.W)))

  val addr = io.cacheio.req.bits.addr
  val data = io.cacheio.req.bits.data
  val mask = io.cacheio.req.bits.mask
  val reqVal = io.cacheio.req.valid

  val dataVec = dontTouch(Wire(Vec(XLEN / 8, UInt(8.W))))
  val maskVec = dontTouch(Wire(Vec(XLEN / 8, Bool())))

  for (i <- 0 until XLEN / 8) {
    dataVec(i) := data((i + 1) * 8 - 1, i * 8)
  }
  maskVec := mask.asBools

  io.cacheio.resp.bits.data := mem.read(addr, reqVal).asUInt

  io.cacheio.resp.valid := 1.B //Response is always valid. May need to actually work on output

  mem.write(addr, dataVec, maskVec)
}

class CacheToMemIO(implicit val p: Parameters) extends MultiIOModule {
  val cache = IO(new CacheIO())
  val memLink = IO(new MemIO())

  memLink.req.valid := cache.req.valid
  memLink.req.bits := cache.req.bits

  cache.resp.valid := memLink.resp.valid
  cache.resp.bits.data := memLink.resp.bits.data

  memLink.resp.ready := 1.B //Processor is always ready to receive
}

@chiselName
class FPGASoC(sim : Boolean = true)(implicit val p: Parameters) extends Module {
  //implicit val p = FPGAParams
  val io = IO(new FPGAIO())
  val core = Module(new Core())

  //Define peripherals
  val sevenSeg = Module(new SevenSegmentMMIO(0x80000010L, 0, "SevenSegmentModule"))
  io.sevenSeg := sevenSeg.pins

  
  val spi = Module(new SPIMMIO(0x40000000, 1))
  io.spi <> spi.pins
  val uart = Module(new UARTMMIO(32000000, 0x80000000L, 2))
  io.uart <> uart.pins
  
  val xadc = Module(new XADC(3,0x80001000L, sim))
  io.xadc <> xadc.pins


  val peripherals = Seq[MMIOModule](
    sevenSeg,
    spi,
    uart,
    xadc
  )


  //Test module that makes memory requests to check robustness
  val tester = Module(new MemTester())

  val memLinks = Seq[MemIO]( //ToDo, define which memories each link should be capable of talking to.
    tester.memLink,
    spi.memLink,
    uart.memLink
  )

  val block = Module(new MyMem(4096, 32))
  val bootromtext = Module(new MyROM(2048, 1, 32, "BootRomtext", "/home/simon/riscv-mini/bootrom/main.dump.text", 0x00L)) //Starts at address 0
  val bootromdata = Module(new MyROM(512, 1, 32, "BootRomdata", "/home/simon/riscv-mini/bootrom/main.dump.data", 0x10000L)) //Starts at address 0x10000

  bootromtext.memLink.startAddr = bootromtext.startAddress
  bootromtext.memLink.endAddr = bootromtext.endAdress

  bootromdata.memLink.startAddr = bootromdata.startAddress
  bootromdata.memLink.endAddr = bootromdata.endAdress

  block.io.startAddr = 0x20000000L
  block.io.endAddr = 0x30000000L

  println(
    "Bootrom start and stop: " + bootromtext.memLink.startAddr.toString() + ", " + bootromtext.memLink.endAddr
      .toString()
  )

  val memories = Seq[MemIO](
    block.io,
    bootromtext.memLink,
    bootromdata.memLink
  )

  val memArbiter = Module(new MemArbiterFPGA(peripherals, memLinks, memories))

  for ((mmio, index) <- peripherals.zipWithIndex) {
    memArbiter.io.mmios(index) <> mmio.mmio
  }

  for ((mem, index) <- memLinks.zipWithIndex) {
    memArbiter.io.memLinks(index) <> mem
  }
  for ((mem, index) <- memories.zipWithIndex) {
    memArbiter.io.memories(index) <> mem
  }

  core.io.icache <> memArbiter.io.icache
  core.io.dcache <> memArbiter.io.dcache

  core.io.host <> io.host

  val counter = Counter((1 << 32) - 1) //2^16-1

  counter.inc

  io.leds := counter.value(31, 16)

  io.debugBools := DontCare
  io.debugBools(0) := spi.memLink.req.valid
  io.debugUInt := spi.memLink.req.bits.data

}

class Basys3FPGASoC()(implicit override val p: Parameters) extends Basys3Shell {

  withClockAndReset(clock_32MHz, resetIn) {
    implicit val p = (new FPGAConfig).toInstance

    val dut = Module(new FPGASoC(sim=false))



    //Connecting the XADC
    dut.io.xadc.vauxn6 := vauxn6 
    dut.io.xadc.vauxn7 := vauxn7 
    dut.io.xadc.vauxn14 := vauxn14 
    dut.io.xadc.vauxn15 := vauxn15 
    dut.io.xadc.vn_in := vn_in

    dut.io.xadc.vauxp6 := vauxp6 
    dut.io.xadc.vauxp7 := vauxp7 
    dut.io.xadc.vauxp14 := vauxp14 
    dut.io.xadc.vauxp15 := vauxp15 
    dut.io.xadc.vp_in := vp_in


    
    


    //LEDs for debugging purposes
    IOBUF(LED(0), clock_32MHz.asUInt().asBool())
    IOBUF(LED(1), resetIn.asBool())
    IOBUF(LED(2), dut.io.debugBools(0).asBool())

    for (i <- 8 until 16) {
      IOBUF(LED(i), dut.io.debugUInt(i - 8))
    }

    //Seven Segment
    for (i <- 0 until 7) {
      IOBUF(seg(i), dut.io.sevenSeg.seg(i))
    }

    for (i <- 0 until 4) {
      IOBUF(an(i), dut.io.sevenSeg.an(i))
    }
    IOBUF(dp, dut.io.sevenSeg.dp)

    dut.io.uart.rx := IOBUF(RsRx)

    IOBUF(RsTx, dut.io.uart.tx)

    dontTouch(dut.io.host)
    dut.io.host <> DontCare

    //Map SPI to jtag at first
    IOBUF(JA(0), dut.io.spi.chipSelect_n)
    IOBUF(JA(1), dut.io.spi.sck)
    IOBUF(JA(2), dut.io.spi.sdi)
    IOBUF(JA(3), dut.io.spi.wp_n)
    IOBUF(JA(4), dut.io.spi.hld_n)

    dut.io.spi.sdo := IOBUF(QspiSO)
    IOBUF(QspiCSn, dut.io.spi.chipSelect_n)
    IOBUF(QspiSI, dut.io.spi.sdi)
    IOBUF(QspiWn, dut.io.spi.wp_n)
    IOBUF(QspiHoldn, dut.io.spi.hld_n)

    //Connect clock for qspi through STARTUPE2
    val startupe2 = Module(new STARTUPE2())
    //startupe2.io.CLK := false.B.asClock
    startupe2.io.GSR := 0.U
    startupe2.io.GTS := 0.U
    startupe2.io.KEYCLEARB := 0.U
    startupe2.io.PACK := 0.U
    startupe2.io.USRCCLKO := dut.io.spi.sck
    startupe2.io.USRCCLKTS := 0.U
    startupe2.io.USRDONEO := 0.U
    startupe2.io.USRDONETS := 1.U

  }
}
