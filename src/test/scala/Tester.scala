import chisel3._
import chisel3.iotesters._
import freechips.rocketchip.config.{Field, Parameters}
import org.scalatest._

import peripherals._
import mini._

import scala.io.Source
import scala.collection.mutable.HashMap

import chisel3.util._
import org.scalatest.selenium.WebBrowser.add

//For chiseltester2
import chiseltest._
import chiseltest.experimental.TestOptionBuilder._
import chiseltest.internal.{VerilatorBackendAnnotation, WriteVcdAnnotation}
import org.scalatest.prop.Tables.Table
import java.io._
import chiseltest.legacy.backends.verilator.VerilatorFlags

class TestFPGAReset(dut: FPGASoC) extends PeekPokeTester(dut) {
  val frequency = 32000000
  val baudrate = 115200

  var current = peek(dut.io.uart.tx)

  step(1000000)
  reset(1000000)
  step(1000000)

}



class MemTesterLinear extends FlatSpec with ChiselScalatestTester with Matchers {
  behavior of ("Pre-loaded memory")


  val path = "program/main.hex"


  "mem" should "go through all the values" in {
    implicit val p = (new FPGAConfig).toInstance


      test(new MyMem(4096, 32, path)).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut => 
        
        dut.io.req.valid.poke(1.B)
        
        for(i <- 0 until 4096/4){
          dut.io.req.bits.addr.poke((i*4).U)
          dut.clock.step()
        }
        
      }
    }
  

}




class TestFirmwareLogging extends FlatSpec with ChiselScalatestTester with Matchers{
  //This test logs the value of PC every cycle, to see where the most time is spent.
  behavior of("FPGA SoC system")

  //val path = "program/tests/tf_micro_speech_test/bin/main.hex"
  val path = "program/tests/tf_micro_speech/bin/main.hex"
  
  "tf_micro_speech" should "print ~~~ALL TESTS PASSED~~~" in {

      implicit val p = (new FPGAConfig).toInstance

      test(new FPGASoC(memPath = path)).withAnnotations(Seq(VerilatorBackendAnnotation, WriteVcdAnnotation, VerilatorFlags(Seq("--trace-depth 1", "--threads 12")))) { dut => 

        val pcMap = HashMap[Long, Long]().withDefaultValue(0.toLong)

        fork{
          while(true){

            dut.io.debugBools(1).expect(1.B)
            pcMap(dut.io.debugUInt.peek.litValue.toLong) += 1
            dut.clock.step(1)
          }
        }

        val MAXSTEPS = 1000000000
        dut.clock.setTimeout(MAXSTEPS + 4)

        //UART reader. Outputs a line on a line end.
        val baudrate = 115200
        val frequency= 32000000
        var string = ""
        val stepsPerBit = (frequency / baudrate).toInt
        
        dut.clock.step(10)

        var done = false
        
        while(true & !done){

          

          var char = 0
          
          //Look for start bit falling_edge
          while(dut.io.uart.tx.peek.litValue.toInt == 1){
            dut.clock.step(1)
          }
          
          //Move ahead half a bit and a half, which should be the middle of the first bit
          dut.clock.step(stepsPerBit/2)
          dut.clock.step(stepsPerBit)
          
          for(i <- 0 until 8){
            char = (char)  + (dut.io.uart.tx.peek.litValue.toInt << i)
            
            //Step one bitwidth
            dut.clock.step(stepsPerBit)
          }

          char = char & 0xff
          string = string + char.toChar

          //println(char.toChar)
        
          //print("Got char: ")
          //println(char.toHexString)
          
          if (char.toChar == '\n') {

            if(string.contains("~~~ALL TESTS PASSED~~~")){
              finalize()
              done = true
            }
            if(string.contains("~~~SOME TESTS FAILED~~~")){
              finalize()
              done = true
              dut.reset.expect(1.B) //Fails on this assertion
            }
            

            print("Got string: ")
            print(string)
            string = ""
          }

          //Pass the stop bit
        }


        //Finalize by writing pcMap to file:
        val mapPath = path + ".log.txt"

        val pw = new PrintWriter(new File(mapPath))

        dut.clock.step(10)

        for((key, value) <- pcMap.toSeq.sortBy(_._1)) pw.write("Key: 0x"+key.toHexString+", value:"+value.toString()+"\n")
      
        pw.close

        dut.clock.step(10)
      }


  }


}



class TestAllFirmwareTest extends FlatSpec with ChiselScalatestTester with Matchers with ParallelTestExecution {


  def readFile(filename: String): Seq[String] = {
    val bufferedSource = io.Source.fromFile(filename)
    val lines = (for (line <- bufferedSource.getLines()) yield line).toList
    bufferedSource.close
    lines
  }

  behavior of("FPGA SoC system")

  //Read all of the available test paths:
  //val paths = Seq("/home/simon/riscv-mini/program/tests/print_test/bin/main.hex")
  val paths = readFile("program/tests/testList.txt")

  

  paths.foreach{ path =>

    var testName = path.substring(14)
    testName = testName.substring(0,testName.indexOf("."))

    testName should "print ~~~ALL TESTS PASSED~~~" in {

      //val hexPath = "/home/simon/riscv-mini/program/main.hex"
      val hexPath = path

      implicit val p = (new FPGAConfig).toInstance

      test(new FPGASoC(memPath = hexPath)).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut => 


        fork{
          while(true){

            dut.io.debugBools(1).expect(1.B)
            dut.clock.step(1)
          }
        }

        val MAXSTEPS = 1000000000
        dut.clock.setTimeout(MAXSTEPS + 4)

        //UART reader. Outputs a line on a line end.
        val baudrate = 115200
        val frequency= 32000000
        var string = ""
        val stepsPerBit = (frequency / baudrate).toInt
        
        dut.clock.step(10)

        var done = false
        
        while(true & !done){

          

          var char = 0
          
          //Look for start bit falling_edge
          while(dut.io.uart.tx.peek.litValue.toInt == 1){
            dut.clock.step(1)
          }
          
          //Move ahead half a bit and a half, which should be the middle of the first bit
          dut.clock.step(stepsPerBit/2)
          dut.clock.step(stepsPerBit)
          
          for(i <- 0 until 8){
            char = (char)  + (dut.io.uart.tx.peek.litValue.toInt << i)
            
            //Step one bitwidth
            dut.clock.step(stepsPerBit)
          }

          char = char & 0xff
          string = string + char.toChar

          //println(char.toChar)
        
          //print("Got char: ")
          //println(char.toHexString)
          
          if (char.toChar == '\n') {

            if(string.contains("~~~ALL TESTS PASSED~~~")){
              finalize()
              done = true
            }
            if(string.contains("~~~SOME TESTS FAILED~~~")){
              finalize()
              done = true
              dut.reset.expect(1.B) //Fails on this assertion
            }
            

            print("Got string: ")
            print(string)
            string = ""
          }

          //Pass the stop bit
        }

      }
  }

  }

}


class TestFPGAMultiProcess extends FlatSpec with ChiselScalatestTester with Matchers {
  behavior.of("FPGA SoC System")

  val path = "program/main.fixed.hex"
  

  "SoC" should "print Hello world" in {

    implicit val p = (new FPGAConfig).toInstance

    test(new FPGASoC(memPath = "program/main.hex")).withAnnotations(Seq(VerilatorBackendAnnotation, WriteVcdAnnotation)) { dut =>
      fork {

        val hashMap: HashMap[Int, Int] = new HashMap() //Address to byte

        val bufferedSource = Source.fromFile(path)
        println("Reading in " + path + " to SPI memory")

        val pattern =
          raw":([0-9A-F][0-9A-F])([0-9A-F][0-9A-F][0-9A-F][0-9A-F])([0-9A-F][0-9A-F])([0-9A-F]*)([0-9A-F][0-9A-F])".r

        var address1 = 0
        for (line <- bufferedSource.getLines) {

          val pattern(byteCount, addressLower, record, data, checksum) = line

          record match {
            case "00" =>
              for (count <- 0 until Integer.parseInt(byteCount, 16)) {

                val addr = (Integer.parseInt(addressLower, 16) + (address1 << 16) + count)

                val byte = Integer.parseInt(data.substring(count * 2, (count + 1) * 2), 16)

                if (byte != 0) {
                  hashMap += (addr -> byte)
                }

              }
            case "01" =>
            //EOF, nothing happens
            case "04" =>
              address1 = Integer.parseInt(data, 16)
            case "05" =>
            //Start linear address, nothing happens
          }
        }
        bufferedSource.close

        val MAXSTEPS = 10000000
        dut.clock.setTimeout(MAXSTEPS + 4)
        var steps = 0

        while (steps < MAXSTEPS) {
          print(dut.io.spi.chipSelect_n.peek)
          while (dut.io.spi.chipSelect_n.peek.litValue == 1 && steps < MAXSTEPS) {
            dut.clock.step(1)

            steps += 1
          }

          println("chipselect went low")

          //chipselect is now asserted

          var lastSCK = dut.io.spi.sck.peek.litValue.toInt

          var bits = 0
          var bitsOut = 7

          var byte = 0
          var bytes = 0

          var address = 0

          var outByte = 0

          while (dut.io.spi.chipSelect_n.peek.litValue == 0 && steps < MAXSTEPS) {
            //Read in 8 bits
            //Looking for rising/falling edge of clk
            (lastSCK, dut.io.spi.sck.peek.litValue.toInt) match {
              case (0, 0) => //Nothing happens
              case (0, 1) => //Rising edge, load in bit
                byte = (((byte << 1) + dut.io.spi.sdi.peek.litValue) & 0xff).toInt //Update byte
                bits += 1
                //println("Rising_edge, value: " + peek(dut.io.spi.sdi) )
                if (bits == 8) {
                  bits = 0
                  bytes += 1

                  bytes match {
                    case 1 => //instruction
                      assert(byte == 3, "Instruction should be a 3, which is a read but was a " + byte.toString())
                    case 2 => //address3
                      address = (address & 0x00ffff) + (byte << 16)
                    case 3 => //address2
                      address = (address & 0xff00ff) + (byte << 8)
                    case 4 => //address1
                      address = (address & 0xffff00) + byte
                    case x if x >= 5 => //subsequent reads does nothing
                      address += 1
                  }
                }
              case (1, 0) => //Falling edge, update output bit
                if (bytes >= 4) { //Only update address when outputting bytes
                  outByte = hashMap.getOrElseUpdate(address, 0)
                  dut.io.spi.sdo.poke(((outByte >> bitsOut) & 0x1).B)
                  bitsOut -= 1

                  if (bitsOut < 0) {
                    bitsOut = 7
                  }
                }
              case (1, 1) => //nothing happens
            }

            lastSCK = dut.io.spi.sck.peek.litValue.toInt
            dut.clock.step(1); steps += 1

          }
        }
      }
      /*********************************
       * ^^^^^ Describes serial interface
       * 
       * vvvvv Describes UART reader
       * *******************************/

      fork{ //UART reader. Outputs a line on a line end.
        val baudrate = 115200
        val frequency= 32000000
        var string = ""
        val stepsPerBit = (frequency / baudrate).toInt
        
        dut.clock.step(10)

        
        while(true){
          var char = 0
          
          //Look for start bit falling_edge
          while(dut.io.uart.tx.peek.litValue.toInt == 1){
            dut.clock.step(1)
          }
          
          //Move ahead half a bit and a half, which should be the middle of the first bit
          dut.clock.step(stepsPerBit/2)
          dut.clock.step(stepsPerBit)
          
          for(i <- 0 until 8){
            char = (char)  + (dut.io.uart.tx.peek.litValue.toInt << i)
            
            //Step one bitwidth
            dut.clock.step(stepsPerBit)
          }

          char = char & 0xff
          string = string + char.toChar

          //println(char.toChar)
        
          //print("Got char: ")
          //println(char.toHexString)
          
          if (char.toChar == '\n') {
            print("Got string: ")
            print(string)
            string = ""
          }

          //Pass the stop bit
        }
          
      }

      //Number of steps in total
      dut.clock.step(5000000)
    }
  }
}

class TestFPGAtest(dut: FPGASoC, path: String) extends PeekPokeTester(dut) {
  val hashMap: HashMap[Int, Int] = new HashMap() //Address to byte

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

          val byte = Integer.parseInt(data.substring(count * 2, (count + 1) * 2), 16)

          if (byte != 0) {
            hashMap += (addr -> byte)
          }

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

  val MAXSTEPS = 1000000
  var steps = 0

  while (steps < MAXSTEPS) {

    while (peek(dut.io.spi.chipSelect_n).toInt == 1 && steps < MAXSTEPS) {
      step(1)
      steps += 1
    }

    println("chipselect went low")

    //chipselect is now asserted

    var lastSCK = peek(dut.io.spi.sck)

    var bits = 0
    var bitsOut = 7

    var byte = 0
    var bytes = 0

    var address = 0

    var outByte = 0

    while (peek(dut.io.spi.chipSelect_n).toInt == 0 && steps < MAXSTEPS) {
      //Read in 8 bits
      //Looking for rising/falling edge of clk
      (lastSCK.toInt, peek(dut.io.spi.sck).toInt) match {
        case (0, 0) => //Nothing happens
        case (0, 1) => //Rising edge, load in bit
          byte = (((byte << 1) + peek(dut.io.spi.sdi)) & 0xff).toInt //Update byte
          bits += 1
          //println("Rising_edge, value: " + peek(dut.io.spi.sdi) )
          if (bits == 8) {
            bits = 0
            bytes += 1

            bytes match {
              case 1 => //instruction
                expect(byte == 3, "Instruction should be a 3, which is a read but was a " + byte.toString())
              case 2 => //address3
                address = (address & 0x00ffff) + (byte << 16)
              case 3 => //address2
                address = (address & 0xff00ff) + (byte << 8)
              case 4 => //address1
                address = (address & 0xffff00) + byte
              case x if x >= 5 => //subsequent reads does nothing
                address += 1
            }
          }
        case (1, 0) => //Falling edge, update output bit
          if (bytes >= 4) { //Only update address when outputting bytes
            outByte = hashMap.getOrElseUpdate(address, 0)
            poke(dut.io.spi.sdo, (outByte >> bitsOut) & 0x1)
            bitsOut -= 1

            if (bitsOut < 0) {
              bitsOut = 7
            }
          }
        case (1, 1) => //nothing happens
      }

      lastSCK = peek(dut.io.spi.sck)
      step(1); steps += 1

    }

  }

}
class TestFPGAtestBootloader(dut: FPGASoC) extends PeekPokeTester(dut) {
  val frequency = 32000000
  val baudrate = 115200

  var current = peek(dut.io.uart.tx)

  var done = false
  var counter = 0

  val bits = frequency / baudrate

  while (peek(dut.io.uart.tx) != 0 && counter < 100000) {
    counter = counter + 1
    step(1)
  }

  step(bits) //Go past first bit
  step(bits / 2)

  step(bits * 10 * 20)

}
/*
class TestMemArbiterFPGA(dut: MemArbiterFPGA) extends PeekPokeTester(dut) {
  step(1)
  poke(dut.io.dcache.abort, 0.B)
  poke(dut.io.dcache.req.valid, 1.B)

  for (i <- 0 until 1024) {

    poke(dut.io.dcache.req.bits.addr, (i + 0x40000000).U)
    poke(dut.io.dcache.req.bits.data, i.U)
    poke(dut.io.dcache.req.bits.mask, 0xff.U)

    step(1)
  }

  for (i <- 0 until 1024) {

    poke(dut.io.dcache.req.bits.addr, (i + 0x40000000).U)
    poke(dut.io.dcache.req.bits.mask, 0x00.U)
    step(1)
    expect(dut.io.dcache.resp.bits.data, i)
  }

  step(1)
  poke(dut.io.dcache.req.bits.mask, 0.U) //Read
  step(10)

  poke(dut.io.dcache.req.bits.mask, 0.U)
  poke(dut.io.dcache.req.bits.addr, "x80000000".U)
  step(1)

  var i = 0 //Wait for uart to be ready
  while (peek(dut.io.dcache.resp.bits.data) != 0 && i < 100000) {
    step(1)
    i = i + 1
  }

  poke(dut.io.dcache.req.bits.mask, 1.U)
  poke(dut.io.dcache.req.bits.addr, "x80000001".U) //Answer

  step(1)

  poke(dut.io.dcache.req.bits.mask, 0.U)
  poke(dut.io.dcache.req.bits.addr, "x80000001".U) //Answer

  i = 0
  while (peek(dut.io.dcache.resp.bits.data) != 0 && i < 100000) {
    step(1)
    i = i + 1
  }

  step(100)

}*/

class TestFPGA2 extends FlatSpec with Matchers {
  "TestFPGA2" should "pass" in {
    implicit val p = (new FPGAConfig).toInstance

    chisel3.iotesters.Driver.execute(
      Array(
        "--backend-name",
        "verilator",
        "--generate-vcd-output",
        "on",
        "--target-dir",
        "test_run_dir/testSoCReset",
        "--top-name",
        "TestFPGA2"
      ),
      () => new FPGASoC()
    ) { c =>
      new TestFPGAReset(c)
    } should be(true)
  }
}

class FPGASoCWithFlash()(implicit val p: Parameters) extends Module {
  val io = IO(new FPGAIO())

  val soc = Module(new FPGASoC())

  soc.io <> io

  val hexRom = Module(new HexROM(4194304, "program/main.hex")) //4 megabyte

  val byte = Reg(UInt(8.W))

  val bitCounter = RegInit(0.U)
  val byteCounter = RegInit(0.U)

  val byteOut = Wire(UInt(8.W))

  val address = RegInit(UInt(24.W))

  soc.io.spi.sdo := byteOut(bitCounter)

  when(io.spi.chipSelect_n === 0.B) {
    when(io.spi.sck === 1.B && RegNext(io.spi.sck) === 0.B) { //Rising edge
      byte := Cat(byte(6, 0), io.spi.sdi)

      when(bitCounter === 7.U) {
        when(byteCounter === 0.U) {}
          .elsewhen(byteCounter === 1.U) {
            address := Cat(byte, address(15, 0))
          }
          .elsewhen(byteCounter === 2.U) {
            address := Cat(address(23, 16), byte, address(7, 0))
          }
          .elsewhen(byteCounter === 3.U) {
            address := Cat(address(23, 8), byte)
          }
          .otherwise {}
      }
    }

    when(io.spi.sck === 0.B && RegNext(io.spi.sck) === 1.B) { //Falling edge
      when(bitCounter === 7.U) { //Last bit of byte
        bitCounter := 0.U
        when(byteCounter === 4.U) {
          byteCounter := byteCounter
        }.otherwise {
          byteCounter := byteCounter + 1.U
        }
      }.otherwise {
        bitCounter := bitCounter + 1.U
      }

    }

  }.otherwise {
    bitCounter := 0.U
    byteCounter := 0.U
  }

  hexRom.io.addr := address

  hexRom.io.enable := 1.B

  byteOut := hexRom.io.data(7, 0)

}

class TestFPGA extends FlatSpec with Matchers {
  "TestFPGA" should "pass" in {
    implicit val p = (new FPGAConfig).toInstance

    chisel3.iotesters.Driver.execute(
      Array(
        "--backend-name",
        "VCS",
        "--generate-vcd-output",
        "on",
        "--target-dir",
        "test_run_dir/testSoC",
        "--top-name",
        "TestFPGA"
      ),
      () => new FPGASoC()
    ) { c =>
      new TestFPGAtest(c, "program/main.hex")
    } should be(true)
  }
}
/*
class TestMemArbiter extends FlatSpec with Matchers {
  "TestMemArbiter" should "pass" in {
    implicit val p = (new FPGAConfig).toInstance
    chisel3.iotesters.Driver.execute(
      Array(
        "--backend-name",
        "verilator",
        "--generate-vcd-output",
        "on",
        "--target-dir",
        "test_run_dir/testMemArbiter",
        "--top-name",
        "TestMemArbiter"
      ),
      () =>
        new MemArbiterFPGA(
          Seq[MMIOModule](
            new SevenSegmentMMIO(0x420000, 0, "SevenSeg")
          ),
          Seq[MemIO](),
          Seq[MemIO]()
        )
    ) { c =>
      new TestMemArbiterFPGA(c)
    } should be(true)
  }
}
 */
