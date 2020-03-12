package peripherals

import chisel3._
import chisel3.util._

import freechips.rocketchip.config.Parameters

import mini._

class MemoryReq(val addrSize : Int = -1)(implicit p: Parameters) extends CoreBundle()(p) {
  private val size = (if (addrSize == -1) xlen else addrSize)
  val addr = UInt(size.W)
  val data = UInt(xlen.W)
  val mask = UInt((xlen / 8).W) //If all 0, then it is a read
}

class MemoryResp(implicit p: Parameters) extends CoreBundle()(p) {
  val data = UInt(xlen.W)
}

class MMIO(val addrSize : Int = -1)(implicit val p: Parameters) extends Bundle { //Memory mapped io
  val req = Flipped(Valid(new MemoryReq(addrSize)))
  val resp = Valid(new MemoryResp)
}

trait MMIOModule extends MultiIOModule {
  val index : Int
  val moduleName : String
  val address: Long
  val mmio : MMIO
  val pins : Bundle
  val numBytes : Int //Number of byte addressable registers.
}