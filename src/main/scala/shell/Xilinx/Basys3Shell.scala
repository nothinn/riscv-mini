// See LICENSE for license details.
package shell.xilinx.basys3shell

import Chisel._
import chisel3.{WireInit}
import chisel3.RawModule
import chisel3.core.{attach, Input, Output,StringParam, DoubleParam}
import chisel3.experimental.{withClockAndReset, Analog}

import sifive.fpgashells.ip.xilinx.{IOBUF}

import freechips.rocketchip.config._

import peripherals._

class clk_wiz_0 extends BlackBox {
  val io = IO(new Bundle {
    val clk_in1 = Input(Clock())
    val clk_out1 = Output(Clock())
    val clk_out2 = Output(Clock())
    val clk_out3 = Output(Clock())
    val reset = Input(Bool())
    val locked = Output(Bool())
  })
}



//-------------------------------------------------------------------------
// STARTUPE2
//-------------------------------------------------------------------------
class STARTUPE2 extends BlackBox(
  Map("PROG_USR" -> new StringParam("FALSE"),
  "SIM_CCLK_FREQ" -> new DoubleParam(10.0))
  )
{
  val io = new Bundle{
    val CFGCLK = Output(Clock())
    val CFGMCLK = Output(Clock())
    val EOS = Output(Bool())
    val PREQ = Output(Bool())
    val CLK = Input(Clock())
    val GSR = Input(Bool())
    val GTS = Input(Bool())
    val KEYCLEARB = Input(Bool())
    val PACK = Input(Bool())
    val USRCCLKO = Input(Bool())
    val USRCCLKTS = Input(Bool())
    val USRDONEO = Input(Bool())
    val USRDONETS = Input(Bool())
  }
}


//-------------------------------------------------------------------------
// Basys3Shell
//-------------------------------------------------------------------------
abstract class Basys3Shell(implicit val p: Parameters) extends RawModule {

  //-----------------------------------------------------------------------
  // Interface
  //-----------------------------------------------------------------------

  // Clock & Reset
  val CLK100MHZ = IO(Input(Clock()))

  //val reset       = IO(Input(Bool()))

  // Green LEDs
  val LED = IO(Vec(16, Analog(1.W)))

  // Sliding switches
  val sw = IO(Vec(16, Analog(1.W)))

  // Buttons. First 3 used as GPIO, the last is used as wakeup
  val btnC = IO(Input(Bool())) //Used for reset
  val resetIn = Wire(Bool())

  //-----------------------------------------------------------------------
  // Clock Generator
  //-----------------------------------------------------------------------
  // Mixed-mode clock generator

  val ip_mmcm = Module(new clk_wiz_0())

  ip_mmcm.io.clk_in1 := CLK100MHZ
  val clock_8MHz = WireInit(ip_mmcm.io.clk_out1) // 8.388 MHz = 32.768 kHz * 256
  val clock_64MHz = WireInit(ip_mmcm.io.clk_out2) // 65 Mhz
  val clock_32MHz = WireInit(ip_mmcm.io.clk_out3) // 65/2 Mhz
  val btnU = IO(Analog(1.W))
  ip_mmcm.io.reset := WireInit(IOBUF(btnU))
  val mmcm_locked = WireInit(ip_mmcm.io.locked)

  resetIn := btnC.asUInt.asBool || !mmcm_locked

  val btnL = IO(Analog(1.W))
  val btnR = IO(Analog(1.W))
  val btnD = IO(Analog(1.W))

  //qspi on-board flash
  val QspiCSn = IO(Analog(1.W))
  val QspiSI = IO(Analog(1.W))
  val QspiSO = IO(Analog(1.W))
  val QspiWn = IO(Analog(1.W))
  val QspiHoldn = IO(Analog(1.W))
  

  //7 segment display
  val seg = IO(Vec(7, Analog(1.W)))
  val dp = IO(Analog(1.W))
  val an = IO(Vec(4, Analog(1.W)))

  //PMOD headers
  val JA = IO(Vec(8, Analog(1.W)))
  val JB = IO(Vec(8, Analog(1.W)))
  val JC = IO(Vec(8, Analog(1.W)))

  //VGA connector
  val vgaRed = IO(Vec(4, Analog(1.W)))
  val vgaBlue = IO(Vec(4, Analog(1.W)))
  val vgaGreen = IO(Vec(4, Analog(1.W)))

  val Hsync = IO(Analog(1.W))
  val Vsync = IO(Analog(1.W))

  //USB-RS232 Interface // UART0
  val RsRx = IO(Analog(1.W))
  val RsTx = IO(Analog(1.W))


  //XADC
  val vauxn6 = IO(Input(Bool()))
  val vauxn7 = IO(Input(Bool()))
  val vauxn14 = IO(Input(Bool()))
  val vauxn15 = IO(Input(Bool()))
  val vauxp6 = IO(Input(Bool()))
  val vauxp7 = IO(Input(Bool()))
  val vauxp14 = IO(Input(Bool()))
  val vauxp15 = IO(Input(Bool()))

  val vp_in = IO(Input(Bool()))
  val vn_in = IO(Input(Bool()))
  



  //-----------------------------------------------------------------------
  // Wire declarations
  //-----------------------------------------------------------------------

  //------------------------
  // SevenSegment
  //------------------------

  def connectSevenSegment(dut: HasPeripherySevenSegModuleImp): Unit = {
    val sevenSegParams = p(PeripherySevenSegKey)
    if (!sevenSegParams.isEmpty) {
      for (i <- 0 until 7) {
        IOBUF(seg(i), dut.sevenSegPins.seg(i))
      }

      for (i <- 0 until 4) {
        IOBUF(an(i), dut.sevenSegPins.an(i))
      }
      IOBUF(dp, dut.sevenSegPins.dp)
    }
  }

}
