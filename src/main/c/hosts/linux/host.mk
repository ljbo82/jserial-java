ifeq ($(HOST),linux-x64)
    ARTIFACT := jserial-jni-x64.so
else
    $(error Unsupported linux host: $(HOST))
endif
