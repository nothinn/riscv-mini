// See LICENSE for license details.

package mini

import chisel3._
import chisel3.util._
import freechips.rocketchip.config.Parameters

object Const {
  val PC_START = 0x200
  val PC_EVEC  = 0x100
}

class DatapathIO(implicit p: Parameters) extends CoreBundle()(p) {
  val host = new HostIO
  val icache = Flipped(new CacheIO)
  val dcache = Flipped(new CacheIO)
  val ctrl = Flipped(new ControlSignals)
  val f_ctrl=Flipped(new FControlSignals)
}

class Datapath(implicit val p: Parameters) extends Module with CoreParams {
  val io      = IO(new DatapathIO)
  val csr     = Module(new CSR)
  val regFile = Module(new RegFile)
  val f_regFile= Module(new FRegFile)
  val alu     = p(BuildALU)(p)
  val f_alu   = p(BuildFALU)(p)
  val immGen  = p(BuildImmGen)(p)
  val brCond  = p(BuildBrCond)(p)

  import Control._
  import FControl._

  /***** Fetch / Execute Registers *****/
  val fe_inst = RegInit(Instructions.NOP)
  val fe_pc   = Reg(UInt())

  /***** Execute / Write Back Registers *****/
  val ew_inst = RegInit(Instructions.NOP) 
  val ew_pc   = Reg(UInt())
  val ew_alu  = Reg(UInt())
  val f_ew_alu= Reg(UInt())
  val csr_in  = Reg(UInt())

  /****** Control signals *****/
  val st_type  = Reg(io.ctrl.st_type.cloneType)
  val ld_type  = Reg(io.ctrl.ld_type.cloneType)
  val wb_sel   = Reg(io.ctrl.wb_sel.cloneType)
  val f_wb_sel   = Reg(io.ctrl.wb_sel.cloneType)
  val wb_en    = Reg(Bool())
  val f_wb_en    = Reg(Bool())
  val csr_cmd  = Reg(io.ctrl.csr_cmd.cloneType)
  val illegal  = Reg(Bool())
  val f_illegal= Reg(Bool())
  val pc_check = Reg(Bool())
 
  /****** Fetch *****/
  val started = RegNext(reset.asBool)
  val stall = !io.icache.resp.valid || !io.dcache.resp.valid
  val pc   = RegInit(Const.PC_START.U(xlen.W) - 4.U(xlen.W))
  val npc  = Mux(stall, pc, Mux(csr.io.expt, csr.io.evec,
             Mux(io.ctrl.pc_sel === PC_EPC,  csr.io.epc,
             Mux(io.ctrl.pc_sel === PC_ALU || brCond.io.taken, alu.io.sum >> 1.U << 1.U,
             Mux(io.ctrl.pc_sel === PC_0, pc, pc + 4.U)))))
  val inst = Mux(started || io.ctrl.inst_kill || brCond.io.taken || csr.io.expt, Instructions.NOP, io.icache.resp.bits.data)
  pc                      := npc 
  io.icache.req.bits.addr := npc
  io.icache.req.bits.data := 0.U
  io.icache.req.bits.mask := 0.U
  io.icache.req.valid     := !stall
  io.icache.abort         := false.B
 
  // Pipelining
  when (!stall) {
    fe_pc   := pc
    fe_inst := inst
  }

  /****** Execute *****/
  // Decode
  io.ctrl.inst  := fe_inst

  // regFile read
  val rd_addr  = fe_inst(11, 7)
  val rs1_addr = fe_inst(19, 15)
  val rs2_addr = fe_inst(24, 20)
  regFile.io.raddr1 := rs1_addr
  regFile.io.raddr2 := rs2_addr


  // fRegFile read
  val f_rd_addr  = fe_inst(11, 7)
  val f_rs1_addr = fe_inst(19, 15)
  val f_rs2_addr = fe_inst(24, 20)
  val f_rs3_addr = fe_inst(31,27)

  f_regFile.io.raddr1 := f_rs1_addr
  f_regFile.io.raddr2 := f_rs2_addr
  f_regFile.io.raddr3 := f_rs3_addr
  



  // gen immdeates
  immGen.io.inst := fe_inst
  immGen.io.sel  := io.ctrl.imm_sel

  // bypass
  val wb_rd_addr = ew_inst(11, 7)
  val rs1hazard = wb_en && rs1_addr.orR && (rs1_addr === wb_rd_addr)
  val rs2hazard = wb_en && rs2_addr.orR && (rs2_addr === wb_rd_addr)
  val rs1 = Mux(wb_sel === WB_ALU && rs1hazard, ew_alu, regFile.io.rdata1) 
  val rs2 = Mux(wb_sel === WB_ALU && rs2hazard, ew_alu, regFile.io.rdata2)

  // bypass RegFile
  val f_wb_rd_addr = ew_inst(11, 7)
  val f_rs1hazard = f_wb_en && f_rs1_addr.orR && (f_rs1_addr === f_wb_rd_addr)
  val f_rs2hazard = f_wb_en && f_rs2_addr.orR && (f_rs2_addr === f_wb_rd_addr)
  val f_rs3hazard = f_wb_en && f_rs3_addr.orR && (f_rs3_addr === f_wb_rd_addr)
  val f_rs1 = Mux(f_wb_sel === F_WB_ALU && f_rs1hazard, f_ew_alu, f_regFile.io.rdata1) 
  val f_rs2 = Mux(f_wb_sel === F_WB_ALU && f_rs2hazard, f_ew_alu, f_regFile.io.rdata2)
  val f_rs3 = Mux(f_wb_sel === F_WB_ALU && f_rs3hazard, f_ew_alu, f_regFile.io.rdata3)
  
 
  // ALU operations
  alu.io.A := Mux(io.ctrl.A_sel === A_RS1, rs1, fe_pc)
  alu.io.B := Mux(io.ctrl.B_sel === B_RS2, rs2, immGen.io.out)
  alu.io.alu_op := io.ctrl.alu_op

  // F_ALU operations
  f_alu.io.rs1 := f_rs1 //Mux(io.f_ctrl.A_sel === A_RS1, rs1, fe_pc)
  f_alu.io.rs2 := f_rs2 //Mux(io.f_ctrl.B_sel === B_RS2, rs2, immGen.io.out)
  f_alu.io.rs3 := f_rs3 //Mux()
  f_alu.io.alu_op := io.f_ctrl.alu_op


  // Branch condition calc
  brCond.io.rs1 := rs1 
  brCond.io.rs2 := rs2
  brCond.io.br_type := io.ctrl.br_type

  // D$ access
  val daddr   = Mux(stall, ew_alu, alu.io.sum) >> 2.U << 2.U
  val woffset = (alu.io.sum(1) << 4.U).asUInt | (alu.io.sum(0) << 3.U).asUInt

  val fp_store= WireDefault(0.B)

  when(ew_inst(6,0) === "b0100111".U){ //STORE-FP
    fp_store := 1.B
  }

  io.dcache.req.valid     := !stall && (io.ctrl.st_type.orR || io.ctrl.ld_type.orR)
  io.dcache.req.bits.addr := daddr 
  io.dcache.req.bits.data := Mux(fp_store, f_rs2, rs2 << woffset)
  io.dcache.req.bits.mask := MuxLookup(Mux(stall, st_type, io.ctrl.st_type), 
              "b0000".U, Seq(
    ST_SW ->  "b1111".U,
    ST_SH -> ("b11".U << alu.io.sum(1,0)),
    ST_SB -> ("b1".U  << alu.io.sum(1,0))))

  

  
  // Pipelining
  when(reset.asBool || !stall && csr.io.expt) {
    st_type   := 0.U
    ld_type   := 0.U
    wb_en     := false.B
    csr_cmd   := 0.U
    illegal   := false.B
    pc_check  := false.B
  }.elsewhen(!stall && !csr.io.expt) {
    ew_pc     := fe_pc
    ew_inst   := fe_inst
    ew_alu    := alu.io.out
    f_ew_alu  := f_alu.io.out
    csr_in    := Mux(io.ctrl.imm_sel === IMM_Z, immGen.io.out, rs1)
    st_type   := io.ctrl.st_type
    ld_type   := io.ctrl.ld_type
    wb_sel    := io.ctrl.wb_sel
    wb_en     := io.ctrl.wb_en
    csr_cmd   := io.ctrl.csr_cmd
    illegal   := io.ctrl.illegal
    pc_check  := io.ctrl.pc_sel === PC_ALU
  }

  // Load
  val loffset = (ew_alu(1) << 4.U).asUInt | (ew_alu(0) << 3.U).asUInt
  val lshift  = io.dcache.resp.bits.data >> loffset
  val load    = MuxLookup(ld_type, io.dcache.resp.bits.data.zext, Seq(
    LD_LH  -> lshift(15, 0).asSInt, LD_LB  -> lshift(7, 0).asSInt,
    LD_LHU -> lshift(15, 0).zext,   LD_LBU -> lshift(7, 0).zext) )
    
  // CSR access
  csr.io.stall    := stall
  csr.io.in       := csr_in
  csr.io.cmd      := csr_cmd
  csr.io.inst     := ew_inst
  csr.io.pc       := ew_pc
  csr.io.addr     := ew_alu
  csr.io.illegal  := illegal
  csr.io.pc_check := pc_check
  csr.io.ld_type  := ld_type
  csr.io.st_type  := st_type
  io.host <> csr.io.host 

  // Regfile Write
  val regWrite = MuxLookup(wb_sel, ew_alu.zext, Seq(
    WB_MEM -> load,
    WB_PC4 -> (ew_pc + 4.U).zext,
    WB_CSR -> csr.io.out.zext) ).asUInt 

  regFile.io.wen   := wb_en && !stall && !csr.io.expt 
  regFile.io.waddr := wb_rd_addr
  regFile.io.wdata := regWrite

  // Abort store when there's an excpetion
  io.dcache.abort := csr.io.expt

  if (p(Trace)) {
    printf("PC: %x, INST: %x, REG[%d] <- %x\n", ew_pc, ew_inst,
      Mux(regFile.io.wen, wb_rd_addr, 0.U),
      Mux(regFile.io.wen, regFile.io.wdata, 0.U))
  }
}
