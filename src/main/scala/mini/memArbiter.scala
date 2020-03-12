package mini

import peripherals._

import chisel3._
import chisel3.experimental.BaseModule
import chisel3.util._
import freechips.rocketchip.config.{Field, Parameters}
import junctions._
import org.scalatest.selenium.WebBrowser.add

class CacheIOToMemIO(implicit p: Parameters) extends Module {
  val io = IO(new Bundle {
    val memLink = Flipped(new MMIO())
    val cacheLink = new CacheIO()
  })

  io.memLink.req := io.cacheLink.req
  io.cacheLink.resp := io.memLink.resp

}

class MemArbiterFPGA(peripheries: Seq[MMIOModule], memoryLinks: Seq[MemIO], mems: Seq[MemIO])(implicit p: Parameters)
    extends MultiIOModule {

  val io = IO(new Bundle {
    val icache = new CacheIO()
    val dcache = new CacheIO()
    val mmios = Vec(peripheries.length, Flipped(new MMIO())) //for peripherals connected as registers
    val memLinks = Vec(memoryLinks.length, Flipped(new MemIO()))
    val memories = Vec(mems.length, new MemIO()) //Memories that should be connected

//TODO move block memory and rom to FPGASoC and connect them through io.memories. This should be with dynamic addressing.

  })

  //Copy variables to internal port.
  for ((mem1, mem2) <- mems.zip(io.memories)) {
    mem2.endAddr = mem1.endAddr
    mem2.startAddr = mem1.startAddr
  }

  //val dcacheToMemlink = Module(new CacheIOToMemIO())

  //dcacheToMemlink.io.cacheLink <> io.dcache

  //Memory modules
  val dmem = Module(new BlockMem(1024, 32))
  val bootromtext = Module(new rom(2048, 32, "/home/simon/riscv-mini/bootrom/main.dump.text"))
  val bootromdata = Module(new rom(1024, 32, "/home/simon/riscv-mini/bootrom/main.dump.data"))

  //Memory mapped modules
  /*val uart = Module(new UART(32000000, 115200))
  val sevenSeg = Module(new SevenSegment(32000000))*/

  val address = WireInit(io.dcache.req.bits.addr)
  val mask = WireInit(io.dcache.req.bits.mask)
  val data = WireInit(io.dcache.req.bits.data)

  bootromdata.io.addr := address
  bootromdata.io.enable := 0.B

  //io.dcache.resp.valid := WireInit(1.B)

  dmem.io.cacheio.req := io.dcache.req
  //io.dcache.resp := dmem.io.cacheio.resp

  dmem.io.cacheio.abort := 0.B

  io.dcache.resp.valid := 1.B

  when((address) < 0x20000.U && (address) >= 0x10000.U) {
    bootromdata.io.enable := 1.B
  }.otherwise {
    bootromdata.io.enable := 0.B
  }

  when((address) < 0x80000000L.U && (address) >= 0x40000000L.U) {
    dmem.io.cacheio.req.valid := 1.B
  }.otherwise {
    dmem.io.cacheio.req.valid := 0.B
  }

  //Instruction memory
  bootromtext.io.addr := io.icache.req.bits.addr
  bootromtext.io.enable := 1.B
  io.icache.resp.valid := 1.B
  //io.icache.resp.bits.data := bootromtext.io.data

  io.icache.resp.bits.data := WireDefault(0xAAAAAAAAL.U)
  io.dcache.resp.bits.data := WireDefault(0xAAAAAAAAL.U)

  val started = RegInit(0.B)
  when(started === 0.B) {
    started := 1.B
  }

  //Used such that it will always default to 1, once the system has started.
  //io.icache.resp.valid := WireDefault(started)
  io.dcache.resp.valid := WireDefault(started)

  val iValid = WireDefault(io.icache.req.valid)

  val iInnerValid = WireDefault(0.B)
  val dInnerValid = WireDefault(0.B)

  val iAddr = WireDefault(io.icache.req.bits.addr)
  val iData = WireDefault(io.icache.req.bits.data)
  val iDataOut = WireDefault(0.U(32.W))
  val dDataOut = WireDefault(0.U(32.W))

  val iDataOutReg = RegInit(0.U(32.W)) //(iDataOut)
  val dDataOutReg = RegInit(0.U(32.W)) //(dDataOut)

  val iMask = WireDefault(io.icache.req.bits.mask)

  val sGood :: sWaitD :: sWaitI :: sWaitID :: Nil = Enum(4)
  val state = RegInit(sGood)
  val nextState = WireDefault(state)

  //Used for state managing.
  val iWaiting = WireDefault(0.B)
  val dWaiting = WireDefault(0.B)
  val dWaitingPeriph = WireDefault(0.B)

  dontTouch(dWaitingPeriph)

  when(iWaiting && dWaiting) {
    nextState := sWaitID
  }.elsewhen(iWaiting) {
      nextState := sWaitI
    }
    .elsewhen(dWaiting) {
      nextState := sWaitD
    }
    .otherwise {
      nextState := sGood
    }

  state := nextState


  val stateMuxed = WireDefault(sGood)

  when(~dWaitingPeriph){
    stateMuxed := state
  }.otherwise{
    switch(state){
      is(sGood){
        stateMuxed := sWaitD
      }
      is(sWaitI){
        stateMuxed := sWaitID
      }
      is(sWaitD){
        stateMuxed := sWaitD
      }
      is(sWaitID){
        stateMuxed := sWaitID
      }
    }
  }

  switch(stateMuxed) {
    is(sGood) {

      when(RegNext(state === sGood)) { //When running normally
        io.icache.resp.bits.data := iDataOut
        io.dcache.resp.bits.data := dDataOut
      }.elsewhen(RegNext(state === sWaitI)) { //Takes the delayed data
          io.icache.resp.bits.data := iDataOut
          io.dcache.resp.bits.data := dDataOutReg
        }
        .elsewhen(RegNext(state === sWaitD)) { //Takes the delayed instruction
          io.icache.resp.bits.data := iDataOutReg
          io.dcache.resp.bits.data := dDataOut
        }
        .elsewhen(RegNext(state === sWaitD)) { //Takes both delayed data and instruction
          io.icache.resp.bits.data := iDataOutReg
          io.dcache.resp.bits.data := dDataOutReg
        }

      io.icache.resp.valid := 1.B
      io.dcache.resp.valid := 1.B

      iInnerValid := io.icache.req.valid
      dInnerValid := io.dcache.req.valid
      /* Should not be done here but in the states, when data is available.
      when(nextState === sWaitD) {
        iDataOutReg := iDataOut
      }
      when(nextState === sWaitI){
        dDataOutReg := dDataOut
      }*/
    }
    is(sWaitI) {
      io.icache.resp.valid := 0.B
      io.dcache.resp.valid := 1.B

      iInnerValid := 1.B // Always high when waiting for itself
      dInnerValid := io.dcache.req.valid

      io.icache.resp.bits.data := DontCare

      when(RegNext(state === sWaitI)) { //If been waiting for more than one cycle
        io.dcache.resp.bits.data := dDataOutReg
      }.otherwise {
        io.dcache.resp.bits.data := dDataOut
        dDataOutReg := dDataOut
      }
    }
    is(sWaitD) {
      io.icache.resp.valid := 1.B
      io.dcache.resp.valid := 0.B

      iInnerValid := io.icache.req.valid
      dInnerValid := 1.B

      when(RegNext(state === sWaitD)) { //If been waiting for more than one cycle
        io.icache.resp.bits.data := iDataOutReg
      }.otherwise {
        io.icache.resp.bits.data := iDataOut
        iDataOutReg := iDataOut
      }
      io.dcache.resp.bits.data := DontCare
    }
    is(sWaitID) {
      io.icache.resp.valid := 0.B
      io.dcache.resp.valid := 0.B

      iInnerValid := 1.B //Always high when waiting for itself
      dInnerValid := 1.B
      io.icache.resp.bits.data := DontCare
      io.dcache.resp.bits.data := DontCare
    }
  }

  //iValid goes high for one cycle and then goes low if response is not given next cycle
  //If the next is 0, that means it did not get its request.
  //When this happens, the "internal" valid should still be kept high, until it gets its request answered.
  //A register also needs to keep it valid, if the stall was due to dcache.

  for (memLink <- io.memLinks) {
    memLink.resp.bits := DontCare //Default value. Overwritten below
  }

  for (memLink <- io.memLinks) {
    memLink.resp.valid := 0.B
    memLink.resp.bits.data := DontCare // 0xAAAAAAAAL.U
  }

  for (mem <- io.memories) {

    println("Connecting memory")

    //Arbitration on the memory modules (bootrom(text/data) and block memory for now)
    val blockArbiter = Module(new Arbiter(Flipped(new MemoryReq), memoryLinks.length + 2))
    for (((requestee, requester), index) <- blockArbiter.io.in.zip(io.memLinks).zipWithIndex) {
      requestee <> requester.req

      val inRange = WireDefault(requester.req.bits.addr >= mem.startAddr.U && requester.req.bits.addr < mem.endAddr.U)

      when(inRange) {
        requestee.valid := requester.req.valid
      }.otherwise {
        requestee.valid := 0.B
      }
      //Memory is ready the next cycle

      when(RegNext(inRange)) {
        requester.resp.valid := RegNext(blockArbiter.io.chosen === index.U)
        when(requester.resp.valid) {
          requester.resp.bits := mem.resp.bits //Available after one cycle.
        }
      }
    }

    //Connect instruction memory to arbiter:

    //
    //io.icache.resp.valid should only be low, if there has been a request which was not valid. This is set as default above the loop
    //io.icache.req.valid will only be high for the first request. If the next is low, we keep the value going out.

    val iInRange = WireDefault(iAddr >= mem.startAddr.U && iAddr < mem.endAddr.U)
    val iChosen = WireDefault(blockArbiter.io.chosen === (memoryLinks.length + 1).U)

    val dInRange = WireDefault(address >= mem.startAddr.U && address < mem.endAddr.U)
    val dChosen = WireDefault(blockArbiter.io.chosen === (memoryLinks.length).U)

    when(iInRange) { //Only happens for this memory
      blockArbiter.io.in(memoryLinks.length + 1).valid := iInnerValid
    }.otherwise {
      blockArbiter.io.in(memoryLinks.length + 1).valid := 0.B
    }

    when(dInRange) { //Only has a valid when address is in range
      blockArbiter.io.in(memoryLinks.length).valid := dInnerValid // io.dcache.req.valid
    }.otherwise {
      blockArbiter.io.in(memoryLinks.length).valid := 0.B
    }

    dontTouch(iChosen)
    dontTouch(iInnerValid)
    dontTouch(dChosen)
    dontTouch(dInnerValid)

    //Control state depending on both icache and dcache
    when(iInRange && ~dInRange) { //only when instruction request accesses this memory
      when(~iChosen && iInnerValid) {
        iWaiting := 1.B
      }
    }.elsewhen(dInRange && ~iInRange) { //Only data request
        when(~dChosen && dInnerValid) {
          dWaiting := 1.B
        }
      }
      .elsewhen(iInRange && dInRange) { //both data and instruction request
        when(~dChosen && dInnerValid) {
          dWaiting := 1.B
        }
        when(~iChosen && iInnerValid) {
          iWaiting := 1.B
        }
      }

    blockArbiter.io
      .in(memoryLinks.length + 1)
      .bits
      .addr := io.icache.req.bits.addr //Check if this changes when not sucessfull
    blockArbiter.io
      .in(memoryLinks.length + 1)
      .bits
      .data := io.icache.req.bits.data //Check if this changes when not sucessfull
    blockArbiter.io
      .in(memoryLinks.length + 1)
      .bits
      .mask := io.icache.req.bits.mask //Check if this changes when not sucessfull

    blockArbiter.io.in(memoryLinks.length).bits.addr := io.dcache.req.bits.addr
    blockArbiter.io.in(memoryLinks.length).bits.data := io.dcache.req.bits.data
    blockArbiter.io.in(memoryLinks.length).bits.mask := io.dcache.req.bits.mask

    //When data has been received and the icache was chosen
    when(RegNext(iInRange && iChosen)) {
      iDataOut := mem.resp.bits.data
    }

    when(RegNext(dInRange && dChosen)) {
      dDataOut := mem.resp.bits.data
    }

    mem.resp.ready := 1.B
    blockArbiter.io.out <> mem.req

    mem.req.bits.addr := blockArbiter.io.out.bits
      .addr(log2Ceil(mem.endAddr - mem.startAddr), 0) //Only send through the needed bits.

    println("MemStart:0x" + mem.startAddr.toHexString)
    println("MemEnd:  0x" + mem.endAddr.toHexString)

    //Connect dcache to arbiter
  }

  //Peripherals
  for ((module, mmio) <- peripheries.zip(io.mmios)) {
    mmio.req.bits.addr := address
    mmio.req.bits.mask := mask
    mmio.req.bits.data := data
    when(address >= module.address.asUInt() && address < module.address.U + module.numBytes.U) {
      mmio.req.valid := dInnerValid
    }.otherwise {
      mmio.req.valid := WireInit(0.B)
    }
    val dataReg = Reg(UInt(32.W))
    when((mmio.req.valid) === 1.B && RegNext(mmio.req.valid) === 0.B) { //If valid was high but response is not, take the old
      dataReg := data
    }
    when(mmio.req.valid === 1.B && RegNext(mmio.req.valid === 1.B) && mmio.resp.valid === 0.B) { //When request is high for 2 cycles and no response yet.
      mmio.req.bits.data := dataReg
    }
  }

  val addressReg = RegInit(0.U)
  addressReg := address

  for ((module, mmio) <- peripheries.zip(io.mmios)) {
    when(
      RegNext(dInnerValid) && addressReg >= module.address.asUInt() && addressReg < module.address.U + module.numBytes.U
    ) {
      //io.dcache.resp := mmio.resp

      //io.dcache.resp.valid := mmio.resp.valid

      //ToDo: Change to check that mmio has in fact a valid output
      dWaitingPeriph := ~mmio.resp.valid

      dDataOut := mmio.resp.bits.data
    }
  }
}
