package ml

import chisel3._
import chisel3.experimental._

// TODO figure out best overall width for fixed point value
class TopML extends Module {
  val io = IO(new Bundle {
    val w = Output(FixedPoint(32.W, 8.BP))
    val cost = Output(FixedPoint(32.W, 8.BP))
  })


  // Define Training Data and load into LUTs (treat as ROM)
  val trainingDataX = VecInit(0.F(32.W, 8.BP),
                              1.F(32.W, 8.BP), 
                              2.F(32.W, 8.BP),
                              3.F(32.W, 8.BP))
                              

  val trainingDataY = VecInit(0.F(32.W, 8.BP), 
                              2.F(32.W, 8.BP), 
                              4.F(32.W, 8.BP),
                              6.F(32.W, 8.BP))
                              

  //------------------//
  // Pipeline Stage 0 //
  //------------------//

  // Used to specify what element of the training data we are on, increments every cycle
  val index = RegInit(0.U((chisel3.util.log2Ceil(trainingDataY.length) + 1).W))
  val indexMaxVal : UInt = trainingDataY.length.U(index.getWidth.W)

  // initialize Weight to Fixed Point rep of random value between 0 and 10
  val initialWeight = scala.math.random()*10.0
  println("initialWeight: " + initialWeight)
  val weight = RegInit(initialWeight.F(32.W, 8.BP))

  // calculate y (model output) and also (ahead of time) yepsilon (used in the cost function gradient approximation)
  val x = Wire(FixedPoint(32.W, 8.BP))
  val y = Wire(FixedPoint(32.W, 8.BP))
  val yepsilon = Wire(FixedPoint(32.W, 8.BP))
  val epsilon  = 100E-3.F(32.W, 8.BP)

  x := trainingDataX(index)
  y := weight * x
  yepsilon := (weight + epsilon) * x

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

  val yexpected = Wire(FixedPoint(32.W, 8.BP))
  val c = Wire(FixedPoint(32.W, 8.BP))
  val cepsilon = Wire(FixedPoint(32.W, 8.BP))
  yexpected := trainingDataY(index) // potentially dangerous if I do not properly pipeline

  // cost function calculation for individual training elements
  c := (y - yexpected) * (y - yexpected)
  cepsilon := (yepsilon - yexpected) * (yepsilon - yexpected)
  
  val cTotal = RegInit(0.F(32.W, 8.BP))
  val cEpsilonTotal = RegInit(0.F(32.W, 8.BP))

  // accumulate cost for all elements
  cTotal := cTotal + c
  cEpsilonTotal := cEpsilonTotal + cepsilon

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
  val cAverage = Wire(FixedPoint(32.W, 8.BP))
  val cAverageEpsilon = Wire(FixedPoint(32.W, 8.BP))
  
  cAverage := cTotal >> 2
  cAverageEpsilon := cEpsilonTotal >> 2 /// trainingDataY.length.F(32.W, 8.BP)

  // finally calculate the finite difference, as a way to approximate the derivative / gradient
  val dc = Wire(FixedPoint(32.W, 8.BP))

  dc := ((cAverageEpsilon - cAverage).asSInt / epsilon.asSInt).asFixedPoint(8.BP)

  // Update w
  when(costFunctionCalculated) {
    weight := weight - dc
  }
  // Connect outputs
  io.w := weight
  io.cost := dc
}
