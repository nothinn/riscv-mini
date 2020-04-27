// See LICENSE for license details.

package mini

import chisel3._

object Opcode {
  // Special immediate instructions
  val LUI    = BigInt("0110111", 2).U(7.W)
  val AUIPC  = BigInt("0010111", 2).U(7.W)

  // Jump instructions
  val JAL    = BigInt("1101111", 2).U(7.W)
  val JALR   = BigInt("1100111", 2).U(7.W)

  // Branch instructions
  val BRANCH = BigInt("1100011", 2).U(7.W)

  // Load and store instrucdtions
  val STORE  = BigInt("0100011", 2).U(7.W)
  val LOAD   = BigInt("0000011", 2).U(7.W)

  // Arithmetic instructions
  val RTYPE  = BigInt("0110011", 2).U(7.W)
  val ITYPE  = BigInt("0010011", 2).U(7.W)

  val MEMORY = BigInt("0001111", 2).U(7.W)
  val SYSTEM = BigInt("1110011", 2).U(7.W)



  // Fixed Point instructions, riscv_spec-v2.2 page 103, table 19.1:  RISC-V base opcode map
  val LOAD_FP  = BigInt("0000111" ,2).U(7.W)
  val STORE_FP = BigInt("0100111" ,2).U(7.W)
  val MADD     = BigInt("1000011" ,2).U(7.W)
  val MSUB     = BigInt("1000111" ,2).U(7.W)
  val NMADD    = BigInt("1001111" ,2).U(7.W)
  val NMSUB    = BigInt("1001011" ,2).U(7.W)
  val OP_FP    = BigInt("1010011" ,2).U(7.W)
            

  //Custom instructions
  val CUSTOM = BigInt("0101010", 2).U(7.W)
}

object Funct3 {
  // Branch function codes
  val BEQ  = BigInt("000", 2).U(3.W)
  val BNE  = BigInt("001", 2).U(3.W)
  val BLT  = BigInt("100", 2).U(3.W)
  val BGE  = BigInt("101", 2).U(3.W)
  val BLTU = BigInt("110", 2).U(3.W)
  val BGEU = BigInt("111", 2).U(3.W)

  // Load and store function codes
  val LB   = BigInt("000", 2).U(3.W)
  val LH   = BigInt("001", 2).U(3.W)
  val LW   = BigInt("010", 2).U(3.W)
  val LBU  = BigInt("100", 2).U(3.W)
  val LHU  = BigInt("101", 2).U(3.W)
  val SB   = BigInt("000", 2).U(3.W)
  val SH   = BigInt("001", 2).U(3.W)
  val SW   = BigInt("010", 2).U(3.W)

  // Arithmetic R-type and I-type functions codes
  val ADD  = BigInt("000", 2).U(3.W)
  val SLL  = BigInt("001", 2).U(3.W)
  val SLT  = BigInt("010", 2).U(3.W)
  val SLTU = BigInt("011", 2).U(3.W)
  val XOR  = BigInt("100", 2).U(3.W)
  val OR   = BigInt("110", 2).U(3.W)
  val AND  = BigInt("111", 2).U(3.W)
  val SR   = BigInt("101", 2).U(3.W)

  //mul and div operations
  val MUL   = BigInt("000",2).U(3.W)
  val MULH  = BigInt("001",2).U(3.W)
  val MULHSU= BigInt("010",2).U(3.W)
  val MULHU = BigInt("011",2).U(3.W)
  val DIV   = BigInt("100",2).U(3.W)
  val DIVU  = BigInt("101",2).U(3.W)
  val REM   = BigInt("110",2).U(3.W)
  val REMU  = BigInt("111",2).U(3.W)
  

  //Custom instruction under custom
  val SWAP = BigInt("000", 2).U(3.W)

  val CSRRW  = BigInt("001", 2).U(3.W)
  val CSRRS  = BigInt("010", 2).U(3.W)
  val CSRRC  = BigInt("011", 2).U(3.W)
  val CSRRWI = BigInt("101", 2).U(3.W)
  val CSRRSI = BigInt("110", 2).U(3.W)
  val CSRRCI = BigInt("111", 2).U(3.W)
}

object Funct7 {
  val U = BigInt("0000000", 2).U(7.W)
  val S = BigInt("0100000", 2).U(7.W)


  //As defined in riscv-spec, page 133: RV32F Standard Extension
  val FADD_S     = BigInt("0000000", 2).U(7.W)
  val FSUB_S     = BigInt("0000100", 2).U(7.W)
  val FMUL_S     = BigInt("0001000", 2).U(7.W)
  val FDIV_S     = BigInt("0001100", 2).U(7.W)
  val FSQRT_S    = BigInt("0101100", 2).U(7.W)

  val FSGNJ_S    = BigInt("0010000", 2).U(7.W)
  val FSGNJN_S   = BigInt("0010000", 2).U(7.W)
  val FSGNJX_S   = BigInt("0010000", 2).U(7.W)

  val FMIN_S     = BigInt("0010100", 2).U(7.W)
  val FMAX_S     = BigInt("0010100", 2).U(7.W)

  val FCVT_W_S   = BigInt("1100000", 2).U(7.W)
  val FCVT_WU_S  = BigInt("1100000", 2).U(7.W)

  val FMV_X_S    = BigInt("1110000", 2).U(7.W)

  val FEQ_S      = BigInt("1010000", 2).U(7.W)
  val FLT_S      = BigInt("1010000", 2).U(7.W)
  val FLE_S      = BigInt("1010000", 2).U(7.W)

  val FCLASS_S   = BigInt("1110000", 2).U(7.W)
  val FCVT_S_W   = BigInt("1101000", 2).U(7.W)
  val FCVT_S_WU  = BigInt("1101000", 2).U(7.W)
  val FMV_W_X    = BigInt("1111000", 2).U(7.W)

  
}

object RM {
  val RNE   = BigInt("000", 2).U(3.W)
  val RTZ   = BigInt("001", 2).U(3.W)
  val RDN   = BigInt("010", 2).U(3.W)
  val RUP   = BigInt("011", 2).U(3.W)
  val RMM   = BigInt("100", 2).U(3.W)
  val DYN   = BigInt("111", 2).U(3.W)
}

object Funct12 {
  val ECALL  = BigInt("000000000000", 2).U(12.W)
  val EBREAK = BigInt("000000000001", 2).U(12.W)
  val ERET   = BigInt("000100000000", 2).U(12.W)
}
