# Adding a custom instruction


## Opcode.scala
The opcode 0101010 is used for custom instructions. This is defined in Opcode.scala

The new instruction needs to have a Funct3 field, which is also defined in Opcode.scala (see SWAP)

If the instruction uses Funct7 or Funct12 this is also defined here.

## ALU.scala
This is the ALU, where the operation is added. These are simple single cycle operations.

Add a new entry under "object ALU"

Add the operation under ALUSimple and ALUArea

## Control.scala
Add intruction under Control.default matrix. See SWAP.

## Instructions.scala
Define the bitpattern for the instruction. It must use the same opcode but can use different funct3

## ALUTests.scala
Add a describtion of the operation written in scala.

## TestUtils.scala
Add line for new instruction (used by tester). See SWAP as example

## Utils.scala
ISATests, maybe add the new one? This requires a new test. Looking into that.