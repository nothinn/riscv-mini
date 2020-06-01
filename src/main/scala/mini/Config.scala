// See LICENSE for license details.

package mini

import chisel3.Module
import freechips.rocketchip.config.{Config, Parameters}
import junctions._
import peripherals.PeripherySevenSegKey
import peripherals.SevenSegParams

class MiniConfig extends Config((site, here, up) => {
    // Core
    case XLEN => 32
    case FLEN => 32
    case Trace => false
    case BuildALU    => (p: Parameters) => Module(new ALUArea()(p))
    case BuildImmGen => (p: Parameters) => Module(new ImmGenWire()(p))
    case BuildBrCond => (p: Parameters) => Module(new BrCondArea()(p))
    // Cache
    case NWays => 1 // TODO: set-associative
    case NSets => 256 
    case CacheBlockBytes => 4 * (here(XLEN) >> 3) // 4 x 32 bits = 16B
    // NastiIO
    case NastiKey => new NastiParameters(
      idBits   = 5,
      dataBits = 64,
      addrBits = here(XLEN))
  }
)

class CoreConfig extends Config((site, here, up) => {
    // Core
    case ARCH => Seq('i')
    case XLEN => 32
    case FLEN => 32
    case Trace => false
    case BuildALU    => (p: Parameters) => Module(new ALUArea()(p))
    case BuildImmGen => (p: Parameters) => Module(new ImmGenWire()(p))
    case BuildBrCond => (p: Parameters) => Module(new BrCondArea()(p))
  }
)
class FPGAConfig extends Config((site, here, up) => {
    // Core rv32i
    case ARCH => Seq('i','m')
    case XLEN => 32
    case FLEN => 32
    case Trace => false
    case BuildALU    => (p: Parameters) => Module(new ALUArea()(p))
    case BuildFALU   => (p: Parameters) => Module(new FALUImpl()(p))
    case BuildMALU   => (p: Parameters) => Module(new MALUImplSingleCycleMulOnly()(p))
    case BuildImmGen => (p: Parameters) => Module(new ImmGenWire()(p))
    case BuildBrCond => (p: Parameters) => Module(new BrCondArea()(p))
    case FREQ => 2000000


    //Peripherals
    case PeripherySevenSegKey => List(
      SevenSegParams(address = 0x100000)
    )
  }
)

class Basys3Peripherals extends Config((site,here,up) => {
  case PeripherySevenSegKey => List(
    SevenSegParams(address = BigInt(0x4200000000L))
  )
})

class Basys3Config extends Config(
  new CoreConfig ++
  new Basys3Peripherals
)