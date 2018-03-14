LOCAL_PATH := $(call my-dir)


include $(CLEAR_VARS)


LOCAL_MODULE := syzygyvbi


LOCAL_MODULE_FILENAME := libsyzygyvbi


PROTOBUF_SRC_FILES :=  protobuf-2.5.0/src/google/protobuf/io/coded_stream.cc \
                     protobuf-2.5.0/src/google/protobuf/stubs/common.cc \
                     protobuf-2.5.0/src/google/protobuf/descriptor.cc \
                     protobuf-2.5.0/src/google/protobuf/descriptor.pb.cc \
                     protobuf-2.5.0/src/google/protobuf/descriptor_database.cc \
                     protobuf-2.5.0/src/google/protobuf/dynamic_message.cc \
                     protobuf-2.5.0/src/google/protobuf/extension_set.cc \
                     protobuf-2.5.0/src/google/protobuf/extension_set_heavy.cc \
                     protobuf-2.5.0/src/google/protobuf/generated_message_reflection.cc \
                     protobuf-2.5.0/src/google/protobuf/generated_message_util.cc \
                     protobuf-2.5.0/src/google/protobuf/io/gzip_stream.cc \
                     protobuf-2.5.0/src/google/protobuf/compiler/importer.cc \
                     protobuf-2.5.0/src/google/protobuf/message.cc \
                     protobuf-2.5.0/src/google/protobuf/message_lite.cc \
                     protobuf-2.5.0/src/google/protobuf/stubs/once.cc \
                     protobuf-2.5.0/src/google/protobuf/compiler/parser.cc \
                     protobuf-2.5.0/src/google/protobuf/io/printer.cc \
                     protobuf-2.5.0/src/google/protobuf/reflection_ops.cc \
                     protobuf-2.5.0/src/google/protobuf/repeated_field.cc \
                     protobuf-2.5.0/src/google/protobuf/service.cc \
                     protobuf-2.5.0/src/google/protobuf/stubs/structurally_valid.cc \
                     protobuf-2.5.0/src/google/protobuf/stubs/strutil.cc \
                     protobuf-2.5.0/src/google/protobuf/stubs/substitute.cc \
                     protobuf-2.5.0/src/google/protobuf/text_format.cc \
                     protobuf-2.5.0/src/google/protobuf/io/tokenizer.cc \
                     protobuf-2.5.0/src/google/protobuf/unknown_field_set.cc \
                     protobuf-2.5.0/src/google/protobuf/wire_format.cc \
                     protobuf-2.5.0/src/google/protobuf/wire_format_lite.cc \
                     protobuf-2.5.0/src/google/protobuf/io/zero_copy_stream.cc \
                     protobuf-2.5.0/src/google/protobuf/io/zero_copy_stream_impl.cc \
                     protobuf-2.5.0/src/google/protobuf/io/zero_copy_stream_impl_lite.cc \
                     protobuf-2.5.0/src/google/protobuf/stubs/stringprintf.cc

SYZYGY_SRC_FILES := syzygyvbi/disk_format.cpp \
                        syzygyvbi/wire_format.cpp \
                        syzygyvbi/segmenter.cpp \
                        syzygyvbi/segmenterfbi.cpp \
                        syzygyvbi/segmentervbi.cpp \
                        syzygyvbi/signature.cpp \
                        syzygyvbi/syzygy_stream.cpp \
                        syzygyvbi/syzygy.cpp \
                        syzygyvbi/common/buffer.cpp \
                        syzygyvbi/common/io_util.cpp \
                        syzygyvbi/common/endian.cpp \
                        syzygyvbi/common/crc32.cpp \
                        syzygyvbi/common/io_filter.cpp \
                        syzygyvbi/common/encrypt.cpp \
                        syzygyvbi/common/zip.cpp \
                        syzygyvbi/common/syzygy_exception.cpp \
                        syzygyvbi/syzygy.pb.cc 
                        
ANDROID_API_FILES := syzygyAndroidApi.cpp syzygyUtil.cpp


LOCAL_SRC_FILES := $(PROTOBUF_SRC_FILES) $(SYZYGY_SRC_FILES) $(ANDROID_API_FILES)

LOCAL_EXPORT_C_INCLUDES :=
LOCAL_EXPORT_LDLIBS :=


LOCAL_C_INCLUDES := $(LOCAL_PATH)/protobuf-2.5.0 \
                    $(LOCAL_PATH)/protobuf-2.5.0/src \
                    $(LOCAL_PATH)/../include


LOCAL_LDLIBS := \
                -llog \
                -lz $(LOCAL_PATH)/../lib/libcrypto.so

LOCAL_CPPFLAGS += -frtti

LOCAL_CPPFLAGS += -fexceptions


#include $(BUILD_STATIC_LIBRARY)  
include $(BUILD_SHARED_LIBRARY)

