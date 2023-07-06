Simple ML Example In Chisel
=======================

Simple machine learning (training) example intended for synthesis on an FPGA.
  
Inspired by [Tsoding](https://www.youtube.com/@TsodingDaily)'s recent ventures with Machine Learning
in C, in particular this project implements the 1st simple example developed during
his [Machine Learning in C (Episode 1)](https://www.youtube.com/watch?v=PGSba51aRYU&t=1309s) Stream.


To summarise, using very rudimentiary mathematics, this HDL describes a circuit which
can train a single parameter function (or neuron at a stretch) to fit the training data,
to produce a model which approximates a 2 times multiplier.

## Instructions
To download the source code and run the simulation:

```
git clone https://github.com/Guiltybyte/ml_in_chisel
cd ml_in_chisel
sbt test
```

In the test ouput you should see that w, the tunable weight of the single neuron/parameter model is approximately 2.


## TODO
- Refactor and clean up the design code
- Setup scripts for generating verilog from chisel and performing an out of context build with the [VTR](https://verilogtorouting.org/) flow,
i.e. a makefile
