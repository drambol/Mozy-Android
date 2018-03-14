LOCAL_PATH:= $(call my-dir)

arm_cflags := -DOPENSSL_BN_ASM_MONT -DAES_ASM -DSHA1_ASM -DSHA256_ASM -DSHA512_ASM
arm_src_files := \
    sha/asm/sha1-armv4-large.S \
    sha/asm/sha256-armv4.S \
    sha/asm/sha512-armv4.S
non_arm_src_files := aes/aes_core.c

local_src_files := \
	sha/sha1_one.c \
	sha/sha1dgst.c \
	sha/sha256.c \
	sha/sha512.c \
	sha/sha_dgst.c \


local_c_includes := \
	$(NDK_PROJECT_PATH) \
	$(NDK_PROJECT_PATH)/include \
	$(NDK_PROJECT_PATH)/include/openssl


local_c_flags := -DNO_WINDOWS_BRAINDEATH

# target
include $(CLEAR_VARS)
include $(LOCAL_PATH)/../android-config.mk
LOCAL_SRC_FILES += $(local_src_files)
LOCAL_CFLAGS += $(local_c_flags)
LOCAL_C_INCLUDES += $(local_c_includes)
LOCAL_C_INCLUDES += $(LOCAL_PATH)/..
LOCAL_C_INCLUDES += $(LOCAL_PATH)/../include
  
LOCAL_LDLIBS += -lz
ifeq ($(TARGET_ARCH),arm)
	LOCAL_SRC_FILES += $(arm_src_files)
	LOCAL_CFLAGS += $(arm_cflags)
else
	LOCAL_SRC_FILES += $(non_arm_src_files)
endif
ifeq ($(TARGET_SIMULATOR),true)
	# Make valgrind happy.
	LOCAL_CFLAGS += -DPURIFY
    LOCAL_LDLIBS += -ldl
endif
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE:= libcrypto
include $(BUILD_SHARED_LIBRARY)
