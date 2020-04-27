// See LICENSE for license details.

package mini

import chisel3._
import freechips.rocketchip.config.Parameters

class FRegFileIO(implicit p: Parameters) extends CoreBundle()(p) {
  val raddr1 = Input(UInt(5.W))
  val raddr2 = Input(UInt(5.W))
  val raddr3 = Input(UInt(5.W))
  val rdata1 = Output(UInt(xlen.W))
  val rdata2 = Output(UInt(xlen.W))
  val rdata3 = Output(UInt(xlen.W))

  val wen    = Input(Bool())
  val waddr  = Input(UInt(5.W))
  val wdata  = Input(UInt(xlen.W))
}

class FRegFile(implicit val p: Parameters) extends Module with CoreParams { //ToDo: Check if orR is better than having a mux that just chooses zero sometimes.
  val io = IO(new FRegFileIO)
  val regs = Mem(32, UInt(xlen.W))
  io.rdata1 := Mux(io.raddr1.orR, regs(io.raddr1), 0.U)
  io.rdata2 := Mux(io.raddr2.orR, regs(io.raddr2), 0.U)
  io.rdata3 := Mux(io.raddr3.orR, regs(io.raddr2), 0.U)
  
  when(io.wen & io.waddr.orR) {
    regs(io.waddr) := io.wdata
  }
}
