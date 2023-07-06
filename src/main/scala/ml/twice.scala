package ml

import chisel3._
import chisel3.experimental._

// TODO figure out best overall width for fixed point value
class TopML extends Module {
  val io = IO(new Bundle {
    val w      = Output(FixedPoint(32.W, 8.BP))
    val cost   = Output(FixedPoint(32.W, 8.BP))
    val dcost  = Output(FixedPoint(32.W, 8.BP))
    val ecost  = Output(FixedPoint(32.W, 8.BP))
    val valid  = Output(Bool())
  })
  val fractalWidth = 8;
  val totalWidth   = 32;

  // Define Training Data and load into LUTs (treat as ROM)
  val trainingDataX = VecInit(0.F(totalWidth.W, fractalWidth.BP),
                              1.F(totalWidth.W, fractalWidth.BP), 
                              2.F(totalWidth.W, fractalWidth.BP),
                              3.F(totalWidth.W, fractalWidth.BP))
                              

  val trainingDataY = VecInit(0.F(totalWidth.W, fractalWidth.BP), 
                              2.F(totalWidth.W, fractalWidth.BP), 
                              4.F(totalWidth.W, fractalWidth.BP),
                              6.F(totalWidth.W, fractalWidth.BP))

  //------------------//
  // Pipeline Stage 0 //
  //------------------//
  
  // Used to specify what element of the training data we are on, increments every cycle
  val index = RegInit(0.U((chisel3.util.log2Ceil(trainingDataY.length) + 1).W))
  val indexMaxVal : UInt = trainingDataY.length.U(index.getWidth.W)

  // initialize Weight to Fixed Point rep of random value between 0 and 10
  // val initialWeight = scala.math.random()*10.0
  val initialWeight = 7.0
  val weight = RegInit(initialWeight.F(totalWidth.W, fractalWidth.BP))
 
  // calculate y (model output) and also (ahead of time) yepsilon (used in the cost function gradient approximation)
  val x        = Wire(FixedPoint(totalWidth.W, fractalWidth.BP))
  val y        = Wire(FixedPoint(totalWidth.W, fractalWidth.BP))
  val yepsilon = Wire(FixedPoint(totalWidth.W, fractalWidth.BP))
  val EPSILON  = 100E-3
  val EPSILON_FIXED  = EPSILON.F(totalWidth.W, fractalWidth.BP)
  val RATE     = 100E-3
  val RATE_FIXED  = RATE.F(totalWidth.W, fractalWidth.BP)

  x := trainingDataX(index)
  y := weight * x
  yepsilon := (weight + EPSILON_FIXED) * x


  // each ind calc takes a clock cycle
  val costFunctionCalculated = WireInit(false.B)
  when(costFunctionCalculated) {
    index := 0.U
    }.elsewhen(index === indexMaxVal) { // this one is probably not needed
    index := index
    }.otherwise {
    index := index + 1.U
    }
 
  //------------------//
  // Pipeline Stage 1 //
  //------------------//
  // Do all calcs in a single tick that way incr can be a free runnning counter 

  val yexpected = Wire(FixedPoint(totalWidth.W, fractalWidth.BP))
  val c         = Wire(FixedPoint(totalWidth.W, fractalWidth.BP))
  val cepsilon  = Wire(FixedPoint(totalWidth.W, fractalWidth.BP))
  yexpected := trainingDataY(index) // potentially dangerous if I do not properly pipeline

  // cost function calculation for individual training elements
  c := (y - yexpected) * (y - yexpected)
  cepsilon := (yepsilon - yexpected) * (yepsilon - yexpected)
  
  val cTotal        = RegInit(0.F(totalWidth.W, fractalWidth.BP))
  val cEpsilonTotal = RegInit(0.F(totalWidth.W, fractalWidth.BP))

  // accumulate cost for all elements
  when(index === indexMaxVal)  {
    cTotal := 0.F(totalWidth.W, fractalWidth.BP)
    cEpsilonTotal := 0.F(totalWidth.W, fractalWidth.BP)
  }.otherwise {
    cTotal := cTotal + c
    cEpsilonTotal := cEpsilonTotal + cepsilon
  }

  // !!!Block until all have completed!!!

  // When index is equal to max value, cost function calculation is done
  when(index === indexMaxVal)  {
    costFunctionCalculated := true.B
    }.otherwise {
    costFunctionCalculated := false.B
    }

  //---------//
  // Stage 2 //
  //---------//
  // This should all happen in zero time, meaning cost_function_calculated signal can also be used to increment the index

  // Get average of cost function accumulations
  val cAverage        = Wire(FixedPoint(totalWidth.W, fractalWidth.BP))
  val cAverageEpsilon = Wire(FixedPoint(totalWidth.W, fractalWidth.BP))
  
  cAverage := cTotal >> 2
  cAverageEpsilon := cEpsilonTotal >> 2 /// trainingDataY.length.F(totalWidth.W, 8.BP)

  // finally calculate the finite difference, as a way to approximate the derivative / gradient
  val dc      = Wire(FixedPoint(totalWidth.W, fractalWidth.BP))

  // constant multiplication to approx division
  println("EPSILON " + EPSILON)
  val E_MULTIPLIER = EPSILON * (scala.math.pow(2, fractalWidth))
  println("E_MULTIPLIER" + E_MULTIPLIER)
  val E_MULTIPLIER_FIXED = E_MULTIPLIER.F(totalWidth.W, fractalWidth.BP)
  val E_1 = 1/EPSILON
  val E_1_FIXED = E_1.F(totalWidth.W, fractalWidth.BP)
  println("1/e " + E_1)

  dc      := ((cAverageEpsilon - cAverage) * E_1_FIXED)
  
  
  // Update w
  when(costFunctionCalculated) {
    weight := weight - (RATE_FIXED * dc)
  }

  // Connect outputs
  io.w      := weight
  io.cost   := cAverage
  io.ecost  := cAverageEpsilon
  io.dcost  := dc
  io.valid  := costFunctionCalculated
}
