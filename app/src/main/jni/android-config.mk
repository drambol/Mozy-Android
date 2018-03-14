#
# These flags represent the build-time configuration of openssl for
# android
#
# They were pruned from the "Makefile" generated by running
# "./Configure linux-generic32 no-idea no-bf no-cast no-seed no-md2 -DL_ENDIAN" in the openssl distribution directory
#

LOCAL_CFLAGS += -DOPENSSL_THREADS -D_REENTRANT -DDSO_DLFCN -DHAVE_DLFCN_H -DL_ENDIAN -DOPENSSL_NO_HW
LOCAL_CFLAGS += -DOPENSSL_NO_CAMELLIA -DOPENSSL_NO_CAST -DOPENSSL_NO_CMS -DOPENSSL_NO_GMP -DOPENSSL_NO_IDEA -DOPENSSL_NO_MDC2 -DOPENSSL_NO_RC5 -DOPENSSL_NO_RFC3779 -DOPENSSL_NO_SEED -DOPENSSL_NO_TLSEXT -DOPENSSL_NO_MD2 -DOPENSSL_NO_EC -DOPENSSL_NO_ECDH -DOPENSSL_NO_ECDSA -DOPENSSL_NO_OCSP -DOPENSSL_NO_AES
