package peripherals

import chisel3._
import chisel3.util._

import mini._

import freechips.rocketchip.config.{Field, Parameters}

class UARTIO extends Bundle {
  val rx = Input(Bool())
  val tx = Output(Bool())
}

class TX(frequency: Int, baudRate: Int) extends Module {
  val io = IO(new Bundle {
    val tx = Output(Bool())
    val data = Input(UInt(8.W))
    val strobe = Input(Bool())
    val ready = Output(Bool())
  })

  val BIT_CNT = ((frequency + baudRate / 2) / baudRate - 1).asUInt()

  val shiftReg = RegInit(0x3ff.U)
  val cntReg = RegInit(0.U(20.W))
  val bitsReg = RegInit(0.U(4.W))

  io.ready := (cntReg === 0.U) && (bitsReg === 0.U)
  io.tx := shiftReg(0)

  when(cntReg === 0.U) {

    when(bitsReg =/= 0.U) {
      cntReg := BIT_CNT
      val shift = shiftReg >> 1
      shiftReg := Cat(1.U, shift(8, 0))
      bitsReg := bitsReg - 1.U
    }.otherwise {
      when(io.strobe && ((cntReg === 0.U) && (bitsReg === 0.U))) {
        shiftReg := Cat(Cat(1.U, io.data), 0.U) // one stop bit, data, one start bit
        bitsReg := 10.U
        cntReg := BIT_CNT
      }.otherwise {
        shiftReg := 0x3ff.U
      }
    }

  }.otherwise {
    cntReg := cntReg - 1.U
  }

}

class UART(frequency: Int, baudRate: Int) extends Module {
  val io = IO(new Bundle {
    val pins = new UARTIO()
    val dataIn = Input(UInt(8.W))
    val dataOut = Output(UInt(8.W))
    val strobe = Input(Bool())
    val ready = Output(Bool())
  })

  val tx = Module(new TX(frequency, baudRate))

  io.dataOut := 0.U

  io.pins.tx := tx.io.tx

  tx.io.data := io.dataIn

  tx.io.strobe := WireInit(0.B)

  io.ready := tx.io.ready

  when(io.strobe === 1.B) {
    when(tx.io.ready === 1.B) {
      tx.io.strobe := 1.B
    }
  }
}

//MMIO wrapper
class UARTMMIO(frequency: Int, addressIn: Long, indexIn: Int, nameIn: String = "UART")(implicit val p: Parameters)
    extends MMIOModule
    with MemLink {
  val mmio = IO(new MMIO(log2Ceil(12))) //This many bits needed for address
  val memLink = IO(new MemIO())
  val pins = IO(new UARTIO())

  val index = indexIn

  val numBytes = 12
  //4 bytes.
  //Word 0: first byte is for sending directly, if this is the only one that is written to.
  //Word 1: Start address, from which it will send
  //Word 2: End address. When written to, it triggers the send. The end address is included.
  //If only one byte needs to be sent, start and end address should be the same value.

  val uart = Module(new UART(frequency, 115200))

  pins <> uart.io.pins

  val address = addressIn
  val moduleName = nameIn

  val startAddress = Reg(UInt(32.W))
  val endAddress = Reg(UInt(32.W))

  val running = RegInit(0.B)

  mmio.resp.valid := WireInit(0.B)
  mmio.resp.bits.data := WireInit(0.U)

  //Default values:
  uart.io.dataIn := DontCare
  uart.io.strobe := 0.B

  when(mmio.req.valid && ~running) {
    when(mmio.req.bits.mask =/= 0.U) { //Write

      switch(mmio.req.bits.addr) {
        is(0.U) {
          when(uart.io.ready) {
            uart.io.dataIn := mmio.req.bits.data(7, 0)
            uart.io.strobe := 1.B
          }
        }
        is(4.U) { //Second word, first address to start read from
          startAddress := mmio.req.bits.data
        }
        is(8.U) { //Second word, to read until.
          endAddress := mmio.req.bits.data
          running := 1.B
        }
      }

    }
  }

  memLink.req.valid := WireDefault(0.B)
  memLink.req.bits.addr := startAddress(31,2) ## WireDefault(0.U(2.W))  //Always this one that is used
  memLink.req.bits.mask := 0.U //Always read
  memLink.req.bits.data := DontCare

  memLink.resp.ready := 1.B

  val hasByte = RegInit(0.B)

  val byte = Wire(UInt(8.W))
  byte := RegNext(byte)

  when(running) {
    when(startAddress === endAddress && uart.io.ready === 1.B && hasByte === 1.B) { //Done
      running := 0.B
    }.otherwise {

      when(~hasByte) {
        memLink.req.valid := 1.B
      }

      when(memLink.resp.valid && RegNext(memLink.req.valid) === 1.B) { //Data has been requested
        hasByte := 1.B
        switch(startAddress(1,0)){
          is(0.U){
            byte := memLink.resp.bits.data(7,0)
          }
          is(1.U){
            byte := memLink.resp.bits.data(15,8)
          }
          is(2.U){
            byte := memLink.resp.bits.data(23,16)
          }
          is(3.U){
            byte := memLink.resp.bits.data(31,24)
          }

        }
        memLink.req.valid := 0.B
      }

      when(uart.io.ready && hasByte) {
        hasByte := 0.B
        startAddress := startAddress + 1.U
        uart.io.strobe := 1.B
        uart.io.dataIn := byte
      }
    }
  }.otherwise{
    hasByte := 0.B
  }

  when(RegNext(mmio.req.valid) && RegNext(uart.io.ready) && RegNext(running === 0.B)) {
    mmio.resp.valid := 1.B
  }.otherwise {
    mmio.resp.valid := 0.B
  }
}
