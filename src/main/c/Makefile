# Copyright (c) 2022 Leandro José Britto de Oliveira.
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.

export CPP_PROJECT_BUILDER ?= $(abspath lib/serial/test/arduino/arduino-core-avr/builder/cpp-project-builder)

PROJ_NAME    := jserial-jni
PROJ_TYPE    := lib
PROJ_VERSION := 0.1.0

ifeq ($(origin O),command line)
    $(error [O] Setting output directory through command line is not allowed for this project)
endif

CFLAGS += -std=gnu99 -fvisibility=hidden -DLIB_VERSION=\"$(PROJ_VERSION)\"

--serial:
	$(MAKE) -C lib/serial LIB_TYPE=static EXTRA_CFLAGS='-fPIC -DSTATIC_LIB' O=../../output BUILD_SUBDIR=serial

define LAZY +=
# ------------------------------------------------------------------------------
PRE_BUILD_DEPS += $$(O_BUILD_DIR)/serial/dist.marker
LDFLAGS        += -L$(O_DIST_DIR)/lib -lserial0

$$(O_BUILD_DIR)/serial/dist.marker: --serial
INCLUDE_DIRS += $$(O_DIST_DIR)/include
# ------------------------------------------------------------------------------
endef

include jni.mk
include $(CPP_PROJECT_BUILDER)/builder.mk
