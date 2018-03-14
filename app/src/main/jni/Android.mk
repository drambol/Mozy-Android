#include $(call all-subdir-makefiles)

LOCAL_PATH := $(call my-dir)

$(info $(LOCAL_PATH))


include $(CLEAR_VARS)

LOCAL_MODULE := libiconv-prebuilt
LOCAL_SRC_FILES := lib/libiconv.so 

include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE := libcrypto-prebuilt
LOCAL_SRC_FILES := lib/libcrypto.so 

include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)

subdirs := $(addprefix $(LOCAL_PATH)/,$(addsuffix /Android.mk, crypto) $(addsuffix /Android.mk, libsyzygyvbi))

include $(subdirs)

$(info $(LOCAL_PATH))

include $(CLEAR_VARS)

LOCAL_PATH :=  $(LOCAL_PATH)/..
LOCAL_MODULE:= mzcrypto

$(info $(LOCAL_PATH))
$(info $(LOCAL_MODULE))

LOCAL_C_INCLUDES += $(LOCAL_PATH)/ \
                    $(LOCAL_PATH)/include \



LOCAL_CFLAGS := -std=c99


LOCAL_SRC_FILES := libmzcrypto/mzcrypto/derive.c \
                   libmzcrypto/mzcrypto/detect.c \
                   libmzcrypto/mzcrypto/strutils.c \
                   libmzcrypto/mzcrypto/validate.c \
                   libmzcrypto/mzcrypto/endian.c \
                   libmzcrypto/mzcrypto/ckey.c \
                   mzcryptoAndroidApi.c \

LOCAL_LDLIBS    :=    $(LOCAL_PATH)/lib/libiconv.so \
                      $(LOCAL_PATH)/lib/libcrypto.so \
                     -L$(SYSROOT)/usr/lib -llog  
                     
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)


