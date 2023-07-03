package ml

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

// (Number of clock steps) / (Length of training data) is the number of epochs

class TestML extends AnyFreeSpec with ChiselScalatestTester {
  "DUT final w should be approx 2.0" in {
    test(new TopML) { dut =>
      dut.clock.step(999)
      println("final w is: " + dut.io.w.peekDouble())
      println("final cost is" + dut.io.cost.peekDouble())
    }
  }
}
