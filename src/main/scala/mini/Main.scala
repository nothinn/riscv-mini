// See LICENSE for license details.

package mini

import chisel3.stage.ChiselGeneratorAnnotation
import firrtl.options.TargetDirAnnotation

import peripherals._

object Main extends App {
  val params = (new MiniConfig).toInstance

  val targetDirectory = args.head

  new chisel3.stage.ChiselStage().execute(args, Seq(
    ChiselGeneratorAnnotation(() => new Tile(params)),
    TargetDirAnnotation(targetDirectory)
  ))
}

object MainFPGA extends App {
  val params = (new FPGAConfig).toInstance
  implicit val p = (new FPGAConfig).toInstance
  val targetDirectory = args.head


  new chisel3.stage.ChiselStage().execute(args, Seq(
    ChiselGeneratorAnnotation(() => new FPGASoC()),
    TargetDirAnnotation(targetDirectory)
  ))
}

object MainBasys3FPGA extends App {
  val params = (new FPGAConfig).toInstance
  implicit val p = (new FPGAConfig).toInstance

  val targetDirectory = args.head

  new chisel3.stage.ChiselStage().execute(args, Seq(
    ChiselGeneratorAnnotation(() => new Basys3FPGASoC()),
    TargetDirAnnotation(targetDirectory)
  ))
}