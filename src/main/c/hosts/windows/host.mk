ifeq ($(HOST),windows-x64)
    ARTIFACT := jserial-jni-x64.dll
else
    $(error Unsupported windows host: $(HOST))
endif
