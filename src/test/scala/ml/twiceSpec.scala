package ml

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

// (Number of clock steps) / (Length of training data) is the number of epochs

class TestML extends AnyFreeSpec with ChiselScalatestTester {
  "DUT final w should be approx 2.0" in {
    test(new TopML).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.clock.setTimeout(10_000)
      val epochs = 10

      // Epoch 1 takes 4 cycles, rest take 5
      dut.clock.step(4)
      dut.io.valid.expect(true)

      for(i <- 1 until epochs) {
        println("weight: " + dut.io.w.peekDouble())
        println("cost  : " + dut.io.cost.peekDouble())
        println("dcost : " + dut.io.dcost.peekDouble())
        println("valid : " + dut.io.valid.peekBoolean())
        dut.clock.step(5)
        dut.io.valid.expect(true)
        // should always be true at the end of an epoch
      }
    }
  }
}
