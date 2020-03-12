package peripherals

import chisel3._
import freechips.rocketchip.config.{Field, Parameters}

import chisel3.util._
import org.scalatest.selenium.WebBrowser.add
import org.scalatest.run

class SPIIO extends Bundle {
  val chipSelect_n = Output(Bool()) //Chip select
  val sdi = Output(Bool()) //Data Out
  val sdo = Input(Bool()) //Data In
  val wp_n = Output(Bool()) //Data In
  val hld_n = Output(Bool()) //Data In
  val sck = Output(Bool()) //spi clock
}

class SPI(frequency: Int) extends Module {

  val io = IO(new Bundle {
    val pins = new SPIIO()
    val addressStart = Input(UInt(24.W))
    val addressEnd = Input(UInt(24.W))
    val readWrite = Input(new Bool()) //Read is a 0
    val strobe = Input(new Bool())
    val freq = Input(UInt(32.W))
    val busy = Output(new Bool())

    val write = Output(new Bool())
    val wValue = Output(UInt(8.W))
    val wAddress = Output(UInt(24.W)) //0 is the 0th address

  })

  val READ = 0x03.U
  val RES = 0xAB.U

  val sIdle :: sInstruction :: sAddress1 :: sAddress2 :: sAddress3 :: sRead :: sStop :: sUnknown :: Nil = Enum(8)
  val state = RegInit(sIdle)
  val nextState = WireInit(state)

  val address = RegInit(0.U(24.W))

  val bitCounter = Counter(8)
  val byte = WireInit(0.U(8.W))

  val run = WireInit(0.B)
  val inc = WireInit(0.B)

  val shiftReg = RegInit(0.U(8.W))

  dontTouch(state)

  //Default values
  io.busy := 1.B
  val chipSelect_n = RegInit(1.B)
  io.pins.chipSelect_n := chipSelect_n
  io.pins.sdi := byte(7.U - bitCounter.value)
  io.pins.wp_n := 1.B
  io.pins.hld_n := 1.B
  val sck = RegInit(0.B)

  io.pins.sck := sck

  io.write := 0.B

  switch(state) {
    is(sIdle) {
      io.busy := 0.B
      when(io.strobe) {
        chipSelect_n := 0.B //pull cs low
        address := io.addressStart
        when(io.readWrite) {
          state := sInstruction
        }.otherwise {
          state := sInstruction
        }
      }
    }
    is(sUnknown) {
      //assert(false, "This state should never be reached")
      printf("SHOULD NOT HAPPEN")
      state := sIdle
    }
    is(sInstruction) {
      when(io.readWrite) {
        byte := RES
      }.otherwise {
        byte := READ
      }
      run := 1.B
      nextState := sAddress3
    }
    is(sAddress3) {
      byte := address(23, 16)
      run := 1.B
      nextState := sAddress2
    }
    is(sAddress2) {
      byte := address(15, 8)
      run := 1.B
      nextState := sAddress1
    }
    is(sAddress1) {
      byte := address(7, 0)
      run := 1.B
      nextState := sRead
    }
    is(sRead) {
      run := 1.B
      when(address === io.addressEnd) {
        nextState := sStop
      }
      when(inc) { //incrementer high after sending a byte
        address := address + 1.U
        io.write := 1.B

      }
    }
    is(sStop) {
      chipSelect_n := 1.B //pull cs high again
      state := sIdle
    }
  }

  when(run) {
    sck := ~sck
    when(sck) { //Update bitoutput on falling edge
      when(bitCounter.inc()) {
        state := nextState
        inc := 1.B
      }
    }.otherwise { //latch in byte on rising edge
      shiftReg := Cat(shiftReg(6, 0), io.pins.sdo)
    }

  }.otherwise {
    sck := 0.B //Reset sck to 0
  }

  //Write address is the current address
  io.wAddress := address
  io.wValue := shiftReg

}

class SPIMMIO(addressIn: Long, indexIn: Int, name: String = "SPIModule")(implicit val p: Parameters)
    extends MMIOModule
    with MemLink {
  val mmio = IO(new MMIO())
  val pins = IO(new SPIIO())
  val memLink = IO(new MemIO())

  val index:      Int = indexIn
  val address:    Long = addressIn
  val moduleName: String = name

  val numBytes = 16 //4 words

  val spi = Module(new SPI(32000000))

  val regFile = Reg(Vec(4, UInt(32.W))) //four registers of size 32.
  //Reg0: Mem start address
  //Reg1: SPI start address
  //Reg2: SPI end address, triggers strobe.
  //Reg3: Return value, shows if spi is busy.
  

  val strobe = RegInit(0.B) //Strobe for triggering a read from flash to memory

  strobe := 0.B //Default value

  when(mmio.req.valid) {
    when(mmio.req.bits.mask =/= 0.U) {
      regFile((mmio.req.bits.addr >> 2) & 0x3.U) := mmio.req.bits.data

      when((mmio.req.bits.addr & 0xff.U) === 8.U) { //Third byte is the last register which triggers an action.
        strobe := 1.B
      }
    }
  }

  regFile(3) := regFile(3)(31,2)  ## spi.io.busy

  val running = RegInit(0.B)


  mmio.resp.bits.data := regFile(3)(31,1)  ## spi.io.busy

  when((~running && ~spi.io.busy) || RegNext(mmio.req.bits.addr(3,2)) === 3.U) { //Always return status register immediately
    mmio.resp.valid := 1.B
  }.otherwise{
    mmio.resp.valid := 0.B
  }



  spi.io.pins <> pins

  spi.io.addressStart := regFile(1)
  spi.io.addressEnd := regFile(2)

  spi.io.strobe := 0.B

  spi.io.readWrite := 0.B //Always read
  spi.io.freq := 0.U

  when(strobe && spi.io.busy === 0.B) {
    spi.io.strobe := 1.B
  }

  memLink.req.valid := 0.B
  memLink.req.bits.addr := regFile(0) //spi.io.wAddress + 0x40000000L.U
  memLink.req.bits.mask := "b0000".U

  memLink.resp := DontCare  
  memLink.resp.ready := 1.B

  memLink.req.bits.data := spi.io.wValue
  when(spi.io.write) {
    memLink.req.valid := 1.B
    memLink.req.bits.addr := regFile(0)
    memLink.req.bits.mask := "b0001".U
    
    regFile(0) := regFile(0) + 1.U

  } //TODO, change to only allow word addressing. Should be done multiple places

}
