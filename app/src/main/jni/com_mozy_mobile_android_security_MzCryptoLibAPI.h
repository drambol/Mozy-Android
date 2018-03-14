/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_mozy_mobile_android_security_MzCryptoLibAPI */

#ifndef _Included_com_mozy_mobile_android_security_MzCryptoLibAPI
#define _Included_com_mozy_mobile_android_security_MzCryptoLibAPI
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_mozy_mobile_android_security_MzCryptoLibAPI
 * Method:    initializeAndSetupHints
 * Signature: (IIIJ)Z
 */
JNIEXPORT jboolean JNICALL Java_com_mozy_mobile_android_security_MzCryptoLibAPI_initializeAndSetupHints
  (JNIEnv *, jobject, jint, jint, jint, jlong);

/*
 * Class:     com_mozy_mobile_android_security_MzCryptoLibAPI
 * Method:    performDetection
 * Signature: (Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_com_mozy_mobile_android_security_MzCryptoLibAPI_performDetection
  (JNIEnv *, jobject, jstring);

/*
 * Class:     com_mozy_mobile_android_security_MzCryptoLibAPI
 * Method:    GetByterangeForValidate
 * Signature: (IJ)V
 */
JNIEXPORT jboolean JNICALL Java_com_mozy_mobile_android_security_MzCryptoLibAPI_GetByterangeForValidate
  (JNIEnv *, jobject, jlong);

/*
 * Class:     com_mozy_mobile_android_security_MzCryptoLibAPI
 * Method:    validateKey
 * Signature: ([B[B[BJ)[B

 */
JNIEXPORT jbyteArray JNICALL Java_com_mozy_mobile_android_security_MzCryptoLibAPI_validateKey
  (JNIEnv *, jobject, jbyteArray, jbyteArray,jlong);

/*
 * Class:     com_mozy_mobile_android_security_MzCryptoLibAPI
 * Method:    cleanUp
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_mozy_mobile_android_security_MzCryptoLibAPI_cleanUp
  (JNIEnv *, jobject);
  
/*
 * Class:     com_mozy_mobile_android_security_MzCryptoLibAPI_getCkey
 * Method:    get ckey
 * Signature: 
 */  
JNIEXPORT jbyteArray JNICALL Java_com_mozy_mobile_android_security_MzCryptoLibAPI_getCkey
  (JNIEnv *, jobject,  jbyteArray)

#ifdef __cplusplus
}
#endif
#endif