# ONESHELL means make will run all recipes within a single shell
# .ONESHELL:

TOP_MODULE = TopML
VTR_ROOT = /home/jordanm/VtR/vtr-verilog-to-routing
PROJ_ROOT = $(dir $(realpath $(lastword $(MAKEFILE_LIST))))
BUILD_DIR = $(PROJ_ROOT)/generated/vtr/$(TOP_MODULE)
VERILOG_DIR = $(PROJ_ROOT)/generated/chisel

test:
	sbt test

# Runs OOC build for EArch Architecture (does not generate bitstream)
build:
	sbt run
	# . is poisix source command (i.e. defined in lightweight bin/sh that ubuntu uses)
	. ${VTR_ROOT}/.venv/bin/activate
	mkdir -p ${BUILD_DIR}
	cd ${BUILD_DIR}
	${VTR_ROOT}/vtr_flow/scripts/run_vtr_flow.py \
		${VERILOG_DIR}/${TOP_MODULE}.v \
		${VTR_ROOT}/vtr_flow/arch/timing/EArch.xml \
		-temp_dir ${BUILD_DIR} \
		--route_chan_width 100

fasm:
	. ${VTR_ROOT}/.venv/bin/activate
	cd ${BUILD_DIR}
	${VTR_ROOT}/build/utils/fasm/genfasm ${BUILD_DIR}/EArch.xml ${BUILD_DIR}/TopML.abc.blif
