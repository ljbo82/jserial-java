# Copyright (c) 2022 Leandro José Britto de Oliveira
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

ifndef __include_jni_mk__
__include_jni_mk__ := 1

# ------------------------------------------------------------------------------
ifndef CPP_PROJECT_BUILDER
    $(error [CPP_PROJECT_BUILDER] Missing definition)
endif
# ------------------------------------------------------------------------------

# ------------------------------------------------------------------------------
PROJ_TYPE ?= lib
ifneq ($(PROJ_TYPE),)
    ifneq ($(PROJ_TYPE), lib)
        $(error Invalid PROJ_TYPE: $(PROJ_TYPE))
    endif
endif
# ------------------------------------------------------------------------------

# ------------------------------------------------------------------------------
LIB_TYPE ?= shared
ifneq ($(LIB_TYPE),)
    ifneq ($(LIB_TYPE), shared)
        $(error Invalid LIB_TYPE: $(LIB_TYPE))
    endif
endif
# ------------------------------------------------------------------------------

# ------------------------------------------------------------------------------
include $(CPP_PROJECT_BUILDER)/native-host.mk
include $(CPP_PROJECT_BUILDER)/functions.mk

ifndef HOST
    ifdef NATIVE_HOST
        HOST := $(NATIVE_HOST)
    endif
endif

JDK_OS := $(call FN_TOKEN,$(HOST),-,1)
ifeq ($(JDK_OS),windows)
    JDK_OS := win32
else ifeq ($(JDK_OS),linux)
    JDK_OS := linux
else
    PRE_BUILD_DEPS += --unsupported-host-os
    --unsupported-host-os:
	    $(error Unsupported operating system: $(JDK_OS))
endif
# ------------------------------------------------------------------------------

# ------------------------------------------------------------------------------
INCLUDE_DIRS += lib/jni-headers/$(HOST)/include lib/jni-headers/$(HOST)/include/$(JDK_OS)
# ------------------------------------------------------------------------------

endif # __include_jni_mk__
