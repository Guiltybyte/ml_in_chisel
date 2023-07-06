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
1. Download the source code

  ```
  git clone https://github.com/Guiltybyte/ml_in_chisel
  cd ml_in_chisel
  ```

2. Run the simulation

  ```
  make test
  ```
  You should see that the final weight is approximately 2.0

3. Run a build with VtR

  If you have [VtR](https://verilogtorouting.org/) installed, you can run the following to run a "build"
  (synthesis -> optimization -> placing -> routing) with the following command
  ```
  make build
  ```
  generated `*.blif` files and logs from the VtR flow run will be in: `generated/vtr/TopML`  
  **Note** currently there is no script included in this repository to generate FASM and/or bitsreams.
