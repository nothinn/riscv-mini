//This peripheral samples an analog signal at a given frequency and stores it into a circular buffer based on a bitmask.
package units

import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum

import freechips.rocketchip.config.Parameters

import mini.FREQ
import org.scalatest.run

import peripherals._
import firrtl.options.DoNotTerminateOnExit
import treadle.vcd.diff.DontDiffValues
import chisel3.internal.firrtl.Width

class ADCMMIO(val index : Int, val address: Long)(implicit val p: Parameters)  extends MMIOModule with MemLink {


  override val moduleName: String = "XADC_ADC" + index.toString()

  override val mmio: MMIO = IO(new MMIO())

  override val memLink: MemIO = IO(new MemIO())

  val xadc_mmio = IO(Flipped(new MMIO()))


  override val numBytes: Int = 4*5 //startAddress, andMask, orMask, samplerate, settings


  val startAddress = RegInit(0.U)
  val andMask = RegInit(0.U)
  val orMask = RegInit(0.U)

  val samplerate = RegInit(0.U) //Clock cycles per sample
  val settings = RegInit(0.U) //1 on 0th bit means that the adc is running.
  val running = WireDefault(settings(0))

  mmio.resp.valid := RegNext(mmio.req.valid)
  mmio.resp.bits.data := DontCare
  when(mmio.req.valid){
    when(mmio.req.bits.mask =/= 0.U){
      switch(mmio.req.bits.addr){
        is(0.U){
          startAddress := mmio.req.bits.data
        }   
        is(4.U){
          andMask := mmio.req.bits.data
        }
        is(8.U){
          orMask := mmio.req.bits.data
        }
        is(12.U){
          samplerate := mmio.req.bits.data
        }
        is(16.U){
          settings := mmio.req.bits.data
        }
      }
    }
  }





  //Wait for samplerate and then take a new sample and store it into memory
  val sampleCounter = RegInit(0.U(32.W))
  val sample = RegInit(0.U)


  object State extends ChiselEnum {
    val sWaitSample, sGetSample, sStoreSample = Value
  }

  val state = RegInit(State.sWaitSample)

  //Default values for states:
  xadc_mmio.req.valid := 0.B
  xadc_mmio.req.valid := 0.B
  memLink.req.valid := 0.B
  

  sampleCounter := sampleCounter + 1.U
  switch(state){
    is(State.sWaitSample){
      when(sampleCounter === samplerate){
        sampleCounter := 0.U
        state := State.sGetSample
      }
    }
    is(State.sGetSample){
      xadc_mmio.req.valid := 1.B
      when(xadc_mmio.resp.valid){
        state := State.sStoreSample
        sample := xadc_mmio.resp.bits.data
        xadc_mmio.req.valid := 0.B
      }
    }
    is(State.sStoreSample){
      memLink.req.valid := 1.B
      when(memLink.resp.valid){
        state := State.sWaitSample
        startAddress := startAddress + 2.U
        memLink.req.valid := 0.B
      }
    }
  }

  memLink.resp.ready := 1.B

  xadc_mmio.req.bits.addr := 7.U
  xadc_mmio.req.bits.data := DontCare
  xadc_mmio.req.bits.mask := DontCare

  memLink.req.bits.mask := "b0011".U //Always write 16 bits
  memLink.req.bits.addr := (startAddress & andMask) | orMask
  memLink.req.bits.data := sample

  //Default values
  //memLink.req.valid := RegNext(memLink.req.valid)
  //memLink.req.ready := 1.B
  memLink.resp.bits.data := DontCare
  memLink.resp.valid := DontCare
}