package peripherals

import scala.io.Source

import chisel3._
import chisel3.experimental.BaseModule
import chisel3.util._
import freechips.rocketchip.config.{Field, Parameters}

import mini._
import firrtl.transforms.DontTouchAnnotation
import org.scalatest.selenium.WebBrowser.add

trait AddrInfo {
  var startAddr: Long
  var endAddr:   Long
}

class MemIO()(implicit val p: Parameters) extends Bundle with AddrInfo {
  val req = Decoupled(new MemoryReq)
  val resp = Flipped(Decoupled(new MemoryResp))

  var startAddr = 0L
  var endAddr = 0L
  

}

class romIO(size: Int)(implicit val p: Parameters) extends Bundle {
  val enable = Input(Bool())
  val addr = Input(UInt(log2Ceil(size).W))
  val data = Output(UInt(32.W))
}

trait MemLink {
  val memLink: MemIO
}

trait RomModule extends MultiIOModule {
  val index:        Int
  val moduleName:   String
  val startAddress: Long
  val memLink:      MemIO
  val size:         Int
  val endAdress:    Long
}
trait MemModule extends Module {
  val size:         Int
}



class MyMem(val size: Int, XLEN: Int)(implicit val p: Parameters) extends MemModule {
  val io = IO(Flipped(new MemIO))

  val mem = SyncReadMem(size, Vec(XLEN / 8 * 2, UInt(8.W) )) //*2 because we need to be able to read/write on a double word

  val addr = io.req.bits.addr(log2Ceil(size), 0)
  val data = io.req.bits.data
  val mask = io.req.bits.mask
  val reqVal = io.req.valid

  val dataVec = Wire(Vec(XLEN / 8 * 2, UInt(8.W)))
  val maskVec = Wire(Vec(XLEN / 8 * 2, Bool()))

  for (i <- 0 until XLEN / 8 * 2) {
    dataVec(i) :=(data<<(addr(1,0)*8.U))((i + 1) * 8 - 1, i * 8)
  }

  val maskWire = Wire(UInt((XLEN/8*2).W))

  maskWire := (mask<<addr(1,0))

  maskVec := maskWire.asBools

  io.req.ready := 1.B

  val outputData = Wire(UInt(XLEN.W))

  outputData := mem.read(addr >> 2, reqVal).asUInt

  
  //Default
  io.resp.bits.data := (outputData >> (addr(1,0) * 8.U))(XLEN-1, 0)

  io.resp.valid := 1.B //Response is always valid. May need to actually work on output

  mem.write(addr >> 2, dataVec, maskVec)

}

class BlockMem(size: Int, XLEN: Int)(implicit val p: Parameters) extends Module {
  val io = IO(new Bundle {
    val cacheio = new CacheIO
  })

  val mem = SyncReadMem(size, Vec(XLEN / 8, UInt(8.W)))

  val addr = io.cacheio.req.bits.addr
  val data = io.cacheio.req.bits.data
  val mask = io.cacheio.req.bits.mask
  val reqVal = io.cacheio.req.valid

  val dataVec = dontTouch(Wire(Vec(XLEN / 8, UInt(8.W))))
  val maskVec = dontTouch(Wire(Vec(XLEN / 8, Bool())))

  for (i <- 0 until XLEN / 8) {
    dataVec(i) := data((i + 1) * 8 - 1, i * 8)
  }
  maskVec := mask.asBools

  io.cacheio.resp.bits.data := mem.read(addr, reqVal).asUInt

  io.cacheio.resp.valid := 1.B //Response is always valid. May need to actually work on output

  mem.write(addr, dataVec, maskVec)
}

class MyROM(val size: Int, val index: Int, XLEN: Int, val moduleName: String, path: String, val startAddress: Long)(
  implicit val p:     Parameters
) extends RomModule {
  val memLink = IO(Flipped(new MemIO))

  val rom = Module(new rom(size, XLEN, path))

  val endAdress: Long = startAddress + size.toLong

  rom.io.addr := memLink.req.bits.addr
  rom.io.enable := memLink.req.valid

  memLink.req.ready := 1.B

  memLink.resp.valid := 1.B
  memLink.resp.bits.data := rom.io.data

  dontTouch(memLink.req.valid)
}

class HexROM(val size: Int, path: String)(implicit val p: Parameters) extends Module {
  val io = IO(new romIO(size))

  val array = Array.fill[Int](size)(0) //Byte array
  val bufferedSource = Source.fromFile(path)
  println("Reading in " + path + " to bootrom")

  val pattern =
    raw":([0-9A-F][0-9A-F])([0-9A-F][0-9A-F][0-9A-F][0-9A-F])([0-9A-F][0-9A-F])([0-9A-F]*)([0-9A-F][0-9A-F])".r

  var address = 0
  for (line <- bufferedSource.getLines) {

    val pattern(byteCount, addressLower, record, data, checksum) = line

    record match {
      case "00" =>
        for (count <- 0 until Integer.parseInt(byteCount, 16)) {

          val addr = (Integer.parseInt(addressLower, 16) + (address << 16) + count)
          println(addr.toHexString + address.toHexString + (Integer.parseInt(addressLower, 16)))

          array(addr) = Integer.parseInt(data.substring(count * 2, (count + 1) * 2), 16)
        }
      case "01" =>
      //EOF, nothing happens
      case "04" =>
        address = Integer.parseInt(data, 16)
      case "05" =>
      //Start linear address, nothing happens
    }
  }
  bufferedSource.close

  /*
  println("Data:")
  for (i <- array) {
    print(i.toHexString)
    print(" ")
  }
  println("Done")
   */
  //The ROM
  val inits = array.map(t => t.U(8.W))

  val mem = VecInit(inits)

  val addr = Wire(UInt(log2Ceil(size).W))

  addr := io.addr

  io.data := RegNext(Cat(mem(addr + 3.U), mem(addr + 2.U), mem(addr + 1.U), mem(addr)))
  println("Done reading in " + path + " to bootrom")

}

class rom(size: Int, XLEN: Int, path: String)(implicit val p: Parameters) extends Module {
  val io = IO(new romIO(size))

  val addr = Wire(UInt(log2Ceil(size).W))

  val array = Array.fill[Int](size)(0)

  val bufferedSource = Source.fromFile(path)

  val pattern = " +([0-9a-f]+):\t([0-9a-f]+) .*".r

  println("Reading in " + path + " to bootrom")

  var count = 0

  var minAddress = 0xFFFFFFFFL
  var maxAddress = 0L

  for (line <- bufferedSource.getLines) {
    for (i <- pattern.findFirstIn(line)) {

      val pattern(addr, data) = line

      for (byte <- 0 until data.length() / 2) {
        array((Integer.parseInt(addr, 16) + byte) & (size - 1)) =
          Integer.parseInt(data.slice((data.length() / 2 - byte - 1) * 2, (data.length() / 2 - byte - 1) * 2 + 2), 16)
        count += 1
      }

    }
  }

  println("Data:")
  for (i <- array) {
    print(i.toHexString)
    print(" ")
  }
  println("Done")

  assert(count <= size, "Number of elements must be smaller than room in memory")

  bufferedSource.close

  val inits = array.map(t => t.U(8.W))

  val mem = VecInit(inits)

  addr := io.addr
  io.data := RegNext(Cat(mem(addr + 3.U), mem(addr + 2.U), mem(addr + 1.U), mem(addr)))
  println("Done reading in " + path + " to bootrom")
}
