package ml

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

class TestML extends AnyFreeSpec with ChiselScalatestTester {
  "DUT final w should be approx 2.0" in {
    // command: $ sbt test
    test(new TopML(32, 16)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.clock.setTimeout(5_000) // default timeout is 1000
      val epochs = 10

      // Epoch 1 takes 4 cycles, rest take 5, as new w value has to be loaded in
      // TODO: derive clock step from training data in DUT
      dut.clock.step(4)
      dut.io.valid.expect(true)
      for(i <- 1 until epochs) {
        println("------------------------------------------")
        println("Epoch        : " + (i - 1))
        println("weight       : " + dut.io.w.peekDouble())
        println("cost         : " + dut.io.cost.peekDouble())
        println("dcost        : " + dut.io.dcost.peekDouble())
        println("valid        : " + dut.io.valid.peekBoolean())
        dut.clock.step(5)
        dut.io.valid.expect(true) // should always be true at the end of an epoch
      }
      println("------------------------------------------")
    }
  }
}
