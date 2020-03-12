package mini

import chisel3._
import chisel3.util._

import freechips.rocketchip.config.{Field, Parameters}

import peripherals._

class MemTester()(implicit val p: Parameters)
    extends MultiIOModule with MemLink {



  val memLink = IO(new MemIO())


  val reg = RegInit(0.U(1.W))
  //reg := ~reg

  memLink.req.valid := reg
  memLink.req.bits.addr := 42.U
  memLink.req.bits.data := 0.U
  memLink.req.bits.mask := 0.U

  memLink.resp.ready := 1.B


  

}
