package units

import chisel3._
import chisel3.util._

import peripherals._

import fft._

import freechips.rocketchip.config.Parameters

import mini._

class FFTMMIO(val address: Long, val index: Int)(implicit val p: Parameters)
    extends MMIOModule
    with MemLink {


  val moduleName: String = "FFT_unit"
  val numBytes: Int = 3*4 //AND mask, OR mask, settings
  val mmio: MMIO = IO(new MMIO(numBytes))
  val memLink: MemIO = IO(new MemIO())


  val andMask = RegInit(0.U)
  val orMask = RegInit(0.U)
  val settings = RegInit(0.U)




  val fft = Module(new FFTSingle(128,16,8))

  fft.io.start := settings(0)
  settings := settings(31,2) ## fft.io.running ## 0.B

  //Memory connection.
  memLink.req.bits.addr := (fft.io.mem.addr << 2 & andMask) | orMask
  memLink.req.bits.data := fft.io.mem.data_out
  fft.io.mem.data_in := memLink.resp.bits.data
  fft.io.mem.valid := memLink.resp.valid
  memLink.req.valid := fft.io.mem.en
  memLink.resp.ready := 1.B
  memLink.req.bits.mask := fft.io.mem.write.asSInt().pad(4).asUInt()


  


  mmio.resp.valid := RegNext(mmio.req.valid)
  mmio.resp.bits.data := DontCare
  when(mmio.req.valid){
    when(mmio.req.bits.mask =/= 0.U){
      switch(mmio.req.bits.addr){
        is(0.U){
          andMask := mmio.req.bits.data
        }   
        is(4.U){
          orMask := mmio.req.bits.data
        }
        is(8.U){
          settings := mmio.req.bits.data
        }
      }
    }
  }


  when(RegNext(mmio.req.valid)){
    when(RegNext(mmio.req.bits.mask === 0.U)){
      switch(RegNext(mmio.req.bits.addr)){
        is(0.U){
          mmio.resp.bits.data := andMask
        }   
        is(4.U){
          mmio.resp.bits.data := orMask
        }
        is(8.U){
          mmio.resp.bits.data := settings
        }
      }
    }
  }






}