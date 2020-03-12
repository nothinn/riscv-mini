package peripherals

import chisel3._
import chisel3.util._
import mini._

import chisel3.experimental._

import freechips.rocketchip.config.{Field, Parameters}

class TLSevenSeg(frequency: Int, params: SevenSegParams)(implicit p: Parameters) extends SevenSegment(frequency)

object SevenSeg {
  val nextId = { var i = -1; () => { i += 1; i } }

  def attach(params: SevenSegAttachParams): TLSevenSeg = {
    implicit val p = params.p
    val sevenSeg = Module(new TLSevenSeg(params.frequency, params.sevenSeg))

    sevenSeg
  }
}

case object PeripherySevenSegKey extends Field[Seq[SevenSegParams]]

case class SevenSegParams(address: BigInt, frequency: Int = 32000000)

case class SevenSegAttachParams(sevenSeg: SevenSegParams, frequency: Int)(implicit val p: Parameters)

trait HasPeripherySevenSeg { this: FPGASoC =>
  val sevenSegNodes = p(PeripherySevenSegKey).map { ps =>
    val frequency = 33000000
    SevenSeg.attach(SevenSegAttachParams(ps, frequency))
  }
}

//Defines the pins for external input/output
trait HasPeripherySevenSegBundle {
  val sevenSeg: SevenSegmentIO
  val mmio:     MMIO
}

//trait HasPeripherySevenSegModuleImp extends LazyModuleImp with HasPeripherySevenSegBundle {
trait HasPeripherySevenSegModuleImp extends Module with HasPeripherySevenSegBundle {
  val outer: HasPeripherySevenSeg
  val sevenSegPins = outer.sevenSegNodes(0).io.pins
  val sevenSegMMIO = outer.sevenSegNodes(0).io.input
}

class SevenSegmentIO() extends Bundle {
  val seg = Output(UInt(7.W))
  val dp = Output(Bool())
  val an = Output(UInt(4.W))
}

class SevenSegment(frequency: Int) extends Module {
  val io = IO(new Bundle {
    val pins = new SevenSegmentIO
    val input = Input(new Valid(UInt(32.W))) //Lower 16 bits are for the value to show. Upper 16 bits are for settings
  })

  //Multiplexing counter:
  val freqCounter = Counter(frequency / 200) //200 hz

  val muxCounter = Counter(4) //For the four displays

  when(freqCounter.inc()) { //Happens at 200 Hz
    muxCounter.inc()
  }

  val b = WireInit(0.U(7.W))

  val value = RegInit(0.U)

  when(io.input.valid) {
    value := io.input.bits
  }

  val showing = WireInit(0.U(4.W)) //The currently shown value

  io.pins.an := "b1111".U

  io.pins.dp := 0.B

  switch(muxCounter.value) {
    is(0.U) {
      showing := value(3, 0)
      io.pins.an := "b1110".U
    }
    is(1.U) {
      showing := value(7, 4)
      io.pins.an := "b1101".U
    }
    is(2.U) {
      showing := value(11, 8)
      io.pins.an := "b1011".U
    }
    is(3.U) {
      showing := value(15, 12)
      io.pins.an := "b0111".U
    }
  }

  switch(showing) {
    is(0.U) { b := "b1111110".U }
    is(1.U) { b := "b0110000".U }
    is(2.U) { b := "b1101101".U }
    is(3.U) { b := "b1111001".U }
    is(4.U) { b := "b0110011".U }
    is(5.U) { b := "b1011011".U }
    is(6.U) { b := "b1011111".U }
    is(7.U) { b := "b1110000".U }
    is(8.U) { b := "b1111111".U }
    is(9.U) { b := "b1110011".U }
    is(10.U) { b := "b1110111".U }
    is(11.U) { b := "b0011111".U }
    is(12.U) { b := "b1001110".U }
    is(13.U) { b := "b0111101".U }
    is(14.U) { b := "b1001111".U }
    is(15.U) { b := "b1000111".U }

  }

  io.pins.seg := ~Reverse(b)

}



class SevenSegmentMMIO(addressIn: Long, indexIn : Int, nameIn : String = "SevenSeg")(implicit val p: Parameters) extends MMIOModule {
  val mmio = IO(new MMIO())
  val pins = IO(new SevenSegmentIO())
  val index = indexIn


  val address = addressIn
  val moduleName = nameIn

  val numBytes = 1
  
  val sevenSegment = Module(new SevenSegment(32000000))

  sevenSegment.io.pins <> pins

  mmio.resp.valid := WireInit(0.B)
  mmio.resp.bits.data := WireInit(0.U)
  

  sevenSegment.io.input.bits := DontCare
  sevenSegment.io.input.valid := 0.B

  when(mmio.req.valid) {
    when(mmio.req.bits.mask =/= 0.U) { //Write
      sevenSegment.io.input.valid := 1.B
      sevenSegment.io.input.bits := mmio.req.bits.data
    }
  }

  
  when(RegNext(mmio.req.valid)) {
    mmio.resp.valid := WireInit(1.B)
  }


  
}
