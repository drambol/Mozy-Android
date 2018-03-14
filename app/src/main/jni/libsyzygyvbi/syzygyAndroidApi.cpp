#include <fstream>
#include <jni.h>
#include <android/log.h>
#include "com_mozy_mobile_android_security_SyzygyVbiAPI.h"
#include "syzygyUtil.h"


/*
 * Class:     com_mozy_mobile_android_security_SyzygyVbiAPI
 * Method:    restore
 * Signature: (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL SyzygyVbiAPI_restore
  (JNIEnv *env, jobject thiz, jstring infile, jstring outfile, jbyteArray decryptKey)
{
	int ret = -1;
	__android_log_print(ANDROID_LOG_DEBUG, "SyzygyVbiAPI","%s: %d enter", __FUNCTION__, __LINE__);
	const char *ifstr = (env)->GetStringUTFChars(infile, 0);
	const char *ofstr = (env)->GetStringUTFChars(outfile, 0);
	char *dkarr = (char*)env->GetByteArrayElements(decryptKey, 0);
	int dklen = env->GetArrayLength(decryptKey);


	if (ifstr == NULL || ofstr == NULL || dkarr == NULL) {
		goto bail;
	}

	ret = restore(ifstr, ofstr, dkarr);
	__android_log_print(ANDROID_LOG_DEBUG, "SyzygyVbiAPI","%s: %d dkarr=%s", __FUNCTION__, __LINE__, dkarr);

bail:
	if (ifstr)
		(env)->ReleaseStringUTFChars(infile, ifstr);
	if (ofstr)
		(env)->ReleaseStringUTFChars(outfile, ofstr);
	if (dkarr)
		(env)->ReleaseByteArrayElements(decryptKey, (jbyte*)dkarr, 0);

	__android_log_print(ANDROID_LOG_DEBUG, "SyzygyVbiAPI","%s: %d exit", __FUNCTION__, __LINE__);
	return (jint)ret;
}

/*
 * Class:     com_mozy_mobile_android_security_SyzygyVbiAPI
 * Method:    baseline
 * Signature: (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL SyzygyVbiAPI_baseline
  (JNIEnv *env, jobject thiz, jstring infile, jstring outfile, jbyteArray encryptKey)
{
	int ret = -1;
	__android_log_print(ANDROID_LOG_DEBUG, "SyzygyVbiAPI","%s: %d enter", __FUNCTION__, __LINE__);
	const char *ifstr = (env)->GetStringUTFChars(infile, 0);
	const char *ofstr = (env)->GetStringUTFChars(outfile, 0);
	char *ekarr = (char*)env->GetByteArrayElements(encryptKey, 0);
	int eklen = env->GetArrayLength(encryptKey);


	if (ifstr == NULL || ofstr == NULL || ekarr == NULL) {
		goto bail;
	}

	ret = baseline(ifstr, ofstr, ekarr);
	__android_log_print(ANDROID_LOG_DEBUG, "SyzygyVbiAPI","%s: %d ekarr=%s", __FUNCTION__, __LINE__, ekarr);

bail:
	if (ifstr)
		(env)->ReleaseStringUTFChars(infile, ifstr);
	if (ofstr)
		(env)->ReleaseStringUTFChars(outfile, ofstr);
	if (ekarr)
		(env)->ReleaseByteArrayElements(encryptKey, (jbyte *)ekarr, 0);

	__android_log_print(ANDROID_LOG_DEBUG, "SyzygyVbiAPI","%s: %d exit", __FUNCTION__, __LINE__);
	return (jint)ret;
}

JNIEXPORT jbyteArray JNICALL SyzygyVbiAPI_compressUserKey
  (JNIEnv *env, jobject thiz, jstring passPhrase)
{
	__android_log_print(ANDROID_LOG_DEBUG, "SyzygyVbiAPI_compressUserKey","%s: %d start", __FUNCTION__, __LINE__);
	const char *passstr = (env)->GetStringUTFChars(passPhrase, 0);
	if (passstr == NULL) {
		__android_log_print(ANDROID_LOG_DEBUG, "SyzygyVbiAPI_compressUserKey","%s: %d exit", __FUNCTION__, __LINE__);
		return NULL;
	}

	__android_log_print(ANDROID_LOG_DEBUG, "SyzygyVbiAPI_compressUserKey","%s: %d passstr=%s", __FUNCTION__, __LINE__, passstr);
	std::string s = compressUserKey(passstr);
	__android_log_print(ANDROID_LOG_DEBUG, "SyzygyVbiAPI_compressUserKey","%s: %d strlen=%d", __FUNCTION__, __LINE__, s.length());

	jbyteArray firstMacArray = env->NewByteArray(48);
	if (firstMacArray == NULL) {
		(env)->ReleaseStringUTFChars(passPhrase, passstr);
		__android_log_print(ANDROID_LOG_DEBUG, "SyzygyVbiAPI_compressUserKey","%s: %d exit", __FUNCTION__, __LINE__);
		return NULL;
	}
	__android_log_print(ANDROID_LOG_DEBUG, "SyzygyVbiAPI_compressUserKey","%s: %d", __FUNCTION__, __LINE__);

	jbyte *bytes = env->GetByteArrayElements(firstMacArray, 0);
	__android_log_print(ANDROID_LOG_DEBUG, "SyzygyVbiAPI_compressUserKey","%s: %d", __FUNCTION__, __LINE__);
	for (int i = 0; i < 48; i++ ) {
		bytes[i] = s[i];
	}

	env->SetByteArrayRegion(firstMacArray, 0, 48, bytes);

	(env)->ReleaseStringUTFChars(passPhrase, passstr);
	__android_log_print(ANDROID_LOG_DEBUG, "SyzygyVbiAPI_compressUserKey","%s: %d exit", __FUNCTION__, __LINE__);
	return firstMacArray; ;
}

JNIEXPORT jint JNICALL SyzygyVbiAPI_test
  (JNIEnv *env, jobject thiz)
{
	return (jint)326;
}

/******************************************************************************************************/
static const char *classPathName = "com/mozy/mobile/android/security/SyzygyVbiAPI";

static JNINativeMethod methods[] = {
		{"test", "()I", (void*)SyzygyVbiAPI_test},
        {"restore", "(Ljava/lang/String;Ljava/lang/String;[B)I", (void*)SyzygyVbiAPI_restore},
        {"baseline", "(Ljava/lang/String;Ljava/lang/String;[B)I", (void*)SyzygyVbiAPI_baseline},
        {"compressUserKey", "(Ljava/lang/String;)[B", (void*)SyzygyVbiAPI_compressUserKey},
};

static int registerNativeMethods (JNIEnv * env, const char* className,
    JNINativeMethod* gMethods, int numMethods)
{
    jclass clazz;
    clazz = env->FindClass(className);
    if (!clazz) {
        return JNI_FALSE;
    }
    if (env->RegisterNatives(clazz, gMethods, numMethods) < 0) {
        return JNI_FALSE;
    }

    return JNI_TRUE;
}


static int registerNatives( JNIEnv* env)
{
  if (!registerNativeMethods(env, classPathName,
                 methods, sizeof (methods) / sizeof(methods[0]))) {
    return JNI_FALSE;
  }

  return JNI_TRUE;
}

typedef union {
    JNIEnv* env;
    void* venv;
} UnionJNIEnvToVoid;

jint JNI_OnLoad( JavaVM* vm, void * reserved)
{
	__android_log_print(ANDROID_LOG_DEBUG, "SyzygyVbiAPI","onload");

    UnionJNIEnvToVoid uenv;
    uenv.venv = 0;
    jint result = -1;
    JNIEnv* env = 0;
    if (vm->GetEnv(&uenv.venv, JNI_VERSION_1_4) != JNI_OK) {
        goto bail;
    }
    env = uenv.env;
    if (registerNatives(env) != JNI_TRUE) {
        goto bail;
    }
    result = JNI_VERSION_1_4;
bail:
	__android_log_print(ANDROID_LOG_DEBUG, "SyzygyVbiAPI","onload, exit");
    return result;
}
