package ml

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

class TestML extends AnyFreeSpec with ChiselScalatestTester {
  "DUT final w should be approx 2.0" in {
    // command: $ sbt test
    test(new TopML(32, 16)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.clock.setTimeout(5_000) // default timeout is 1000
     
      val epochs = 100
      val clock_steps_per_epoch =  dut.trainingDataX.length + 1

      var weight : Double = 0
      var cost   : Double = 0
      var dcost  : Double = 0

      // Epoch 1 takes 4 cycles, rest take 5, as new w value has to be loaded in
      dut.clock.step(clock_steps_per_epoch - 1)
      dut.io.valid.expect(true)
      for(i <- 1 until epochs) {
        weight = dut.io.w.peekDouble()
        cost   = dut.io.cost.peekDouble()
        dcost  = dut.io.dcost.peekDouble()

        println("------------------------------------------")
        println("Epoch        : " + (i - 1))
        println(f"weight       : $weight%4.16f")
        println(f"cost         : $cost%4.16f")
        println(f"dcost        : $dcost%4.16f")
        dut.clock.step(clock_steps_per_epoch)
        dut.io.valid.expect(true) // should always be true at the end of an epoch
      }
      println("------------------------------------------")
    }
  }
}
