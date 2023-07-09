package ml

import chisel3._
import chisel3.experimental._
import chisel3.stage.ChiselStage

class TopML(tw: Int, fw: Int) extends Module {
  val io = IO(new Bundle {
    val w      = Output(FixedPoint(32.W, 8.BP))
    val cost   = Output(FixedPoint(32.W, 8.BP))
    val dcost  = Output(FixedPoint(32.W, 8.BP))
    val valid  = Output(Bool())
  })

  // ML related params
  val EPSILON  = 1E-3
  val RATE     = 10E-3


  def model(x: FixedPoint, w: FixedPoint): FixedPoint = w * x
  def cost(ya: FixedPoint, ye: FixedPoint): FixedPoint = (ya - ye) * (ya - ye)
  def avrg(t: FixedPoint): FixedPoint = t >> chisel3.util.log2Ceil(trainingDataY.length)


  //-----------//
  // SECTION 0 //
  //-----------//
  // Iterate over the training data and calculate the individual cost for each entry given c
  // Define Training Data and load into LUTs (treat as ROM)

  val trainingDataX = VecInit(0.F(tw.W, fw.BP),
                              1.F(tw.W, fw.BP),
                              2.F(tw.W, fw.BP),
                              3.F(tw.W, fw.BP))

  val trainingDataY = VecInit(0.F(tw.W, fw.BP),
                              2.F(tw.W, fw.BP),
                              4.F(tw.W, fw.BP),
                              6.F(tw.W, fw.BP))



  // Used to specify what element of the training data we are on, increments every cycle
  val index = RegInit(0.U((chisel3.util.log2Ceil(trainingDataY.length) + 1).W))
  val indexMaxVal : UInt = trainingDataY.length.U(index.getWidth.W)

  // initialize Weight to Fixed Point rep of random value between 0 and 10
  val initialWeight = scala.math.random()*10.0
  println("initial weight: " + initialWeight)
  val weight = RegInit(initialWeight.F(tw.W, fw.BP))

  val x              = Wire(FixedPoint(tw.W, fw.BP))
  val y              = Wire(FixedPoint(tw.W, fw.BP))
  val yepsilon       = Wire(FixedPoint(tw.W, fw.BP))
  val EPSILON_FIXED  = EPSILON.F(tw.W, fw.BP)
  val RATE_FIXED     = RATE.F(tw.W, fw.BP)

  // calculate y (model output) and also (ahead of time) yepsilon (used in the cost function gradient approximation)
  x        := trainingDataX(index)
  y        := model(x, weight)
  yepsilon := model(x, weight + EPSILON_FIXED)


  // each training data element calc takes a clock cycle
  val costFunctionCalculated = WireInit(false.B)
  when(costFunctionCalculated) {
    index := 0.U
    }.elsewhen(index === indexMaxVal) {
    index := index
    }.otherwise {
    index := index + 1.U
    }

  // All calcs done in a single tick hence incr can be a free runnning counter 
  val yexpected = Wire(FixedPoint(tw.W, fw.BP))
  val c         = Wire(FixedPoint(tw.W, fw.BP))
  val cepsilon  = Wire(FixedPoint(tw.W, fw.BP))
  yexpected := trainingDataY(index)

  // cost function calculation for individual training elements
  c        := cost(y, yexpected)
  cepsilon := cost(yepsilon, yexpected)

  val cTotal        = RegInit(0.F(tw.W, fw.BP))
  val cEpsilonTotal = RegInit(0.F(tw.W, fw.BP))

  // accumulate cost for all elements
  when(index === indexMaxVal)  {
    cTotal := 0.F(tw.W, fw.BP)
    cEpsilonTotal := 0.F(tw.W, fw.BP)
    costFunctionCalculated := true.B
  }.otherwise {
    cTotal := cTotal + c
    cEpsilonTotal := cEpsilonTotal + cepsilon
    costFunctionCalculated := false.B
  }


  //-----------//
  // SECTION 1 //
  //-----------//
  // Calculate finite difference and update w

  // Get average of cost function accumulations
  val cAverage        = Wire(FixedPoint(tw.W, fw.BP))
  val cAverageEpsilon = Wire(FixedPoint(tw.W, fw.BP))
  
  // equivalent to division by 2^2 = 4 which happens to be the length of the training data
  cAverage        := avrg(cTotal)
  cAverageEpsilon := avrg(cEpsilonTotal)

  // constant multiplication to approx division
  val E       = 1/EPSILON
  val E_FIXED = E.F(tw.W, fw.BP)
  val dc   = Wire(FixedPoint(tw.W, fw.BP))

  // finally calculate the finite difference, as a way to approximate the derivative / gradient
  dc := ((cAverageEpsilon - cAverage) * E_FIXED)

  // Update w
  when(costFunctionCalculated) {
    weight := weight - (RATE_FIXED * dc)
  }

  // Connect outputs
  io.w      := weight
  io.cost   := cAverage
  io.dcost  := dc
  io.valid  := costFunctionCalculated
}

// command: $ sbt run
object emitTwiceVerilog extends App {
  emitVerilog (new TopML(32, 16), Array("--target-dir", "generated/chisel"))
}
