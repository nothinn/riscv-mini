//This file is used to emulate an EEPROM memory

package peripherals

import chisel3._

import chisel3.util._

import chisel3.experimental.{Analog}


import chisel3.experimental.chiselName

@chiselName
class EEPROMEmulator(frequency: Int, memoryFile: String, size : Int) extends Module {
  val io = IO(new Bundle {
    val S_n = Input(new Bool()) //Chip select, active low
    val Q = Output(new Bool()) //Serial data output
    //val W_n = Input(new Bool()) //Write protect, active low
    //val HOLD_n = Input(new Bool()) //hold, active low
    val D = Input(new Bool()) //Serial Data Input
    val C = Input(new Bool()) //Serial clock

  })


  val memory = Mem(size,UInt(8.W))
  
  //To use the SPI clock
  withClockAndReset(io.C.asClock, io.S_n) {

    val WRITE = WireInit("b00000010".U)
    val READ = WireInit("b00000011".U)

    val bitIn = WireInit(0.B)

    val bitCounter = RegInit(0.U(8.W))

    val byteCounter = RegInit(0.U(8.W))

    val instruction = Reg(Vec(8, Bool()))

    val address = Reg(Vec(24, Bool()))

    val data = Reg(Vec(8, Bool()))

    val incrementer = RegInit(0.U(24.W))

    val outByte = WireInit(0.U(8.W))


    dontTouch(bitCounter)
    dontTouch(byteCounter)
    dontTouch(instruction)
    dontTouch(address)
    dontTouch(data)
    dontTouch(incrementer)
    dontTouch(outByte)
    
    
    

    bitIn := io.D

    bitCounter := bitCounter + 1.U

    io.Q := 0.B

    when(byteCounter === 0.U) { //First byte is always the instruction
      for (i <- 0 until 7) {
        instruction(i + 1) := instruction(i)
      }
      instruction(0) := bitIn
    }.elsewhen(byteCounter === 1.U || byteCounter === 2.U || byteCounter === 3.U) { //Reading in address
        for (i <- 0 until 7) {
          address(i + 1) := address(i)
        }
        address(0) := bitIn
      }
      .otherwise { //Decision based on instruction
        when(instruction.asUInt === WRITE) {
          for (i <- 0 until 7) {
            data(i + 1) := data(i)
          }
          data(0) := bitIn
          when(bitCounter === 7.U) {
            memory.write(address.asUInt + incrementer % size.U, Cat((data.asUInt),bitIn))
          }
        }.elsewhen(instruction.asUInt === READ) {
          outByte := memory.read(address.asUInt + incrementer % size.U)
          io.Q := outByte(7.U - bitCounter.asUInt)
        }

      }
    when(bitCounter === 7.U) {
      bitCounter := 0.U
      when(byteCounter < 4.U) {
        byteCounter := byteCounter + 1.U
      }.otherwise {
        incrementer := incrementer + 1.U //Increments the address
      }
    }
  }

}


