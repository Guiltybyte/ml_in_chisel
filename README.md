Simple ML Example In Chisel
=======================

Simple machine learning (training) example intended for synthesis on an FPGA.
  
Inspired by [Tsoding](https://www.youtube.com/@TsodingDaily)'s recent ventures with Machine Learning
in C, in particular this project implements the 1st simple example developed during
his [Machine Learning in C (Episode 1)](https://www.youtube.com/watch?v=PGSba51aRYU&t=1309s) Stream.


To summarise, using very rudimentiary mathematics, this HDL describes a circuit which
can train a single parameter function (or neuron at a stretch) to fit the training data,
to produce a model which approximates a 2 times multiplier.
