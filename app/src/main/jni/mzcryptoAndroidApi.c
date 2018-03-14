
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "libmzcrypto/mzcrypto/mzcrypto.h"
#include "libmzcrypto/mzcrypto/derive.h"
#include "libmzcrypto/mzcrypto/endian.h"
#include "libmzcrypto/mzcrypto/strutils.h"
#include "libmzcrypto/mzcrypto/internal.h"

#include <jni.h>

#include <android/log.h>


MCDetector *detector;
MCKey **possibleKeys;
size_t arrayLen;


/*
 * Class:     com_mozy_mobile_android_security_MzCryptoLibAPI
 * Method:    initializeAndSetupHints
 * Signature: (IIIJ)Z
 */

JNIEXPORT jboolean JNICALL Java_com_mozy_mobile_android_security_MzCryptoLibAPI_initializeAndSetupHints
  (JNIEnv *env, jobject obj,  jint platform, jint sourceType, jint cipher, jlong fileSize)
    {
        // First initialize the detector
        detector = MCCreateDetector();
        
        if(detector != NULL)
        {
        
            int retVal = -1;
      
            retVal = MCDetectorSetHint(detector, MC_HINT_PLATFORMTYPE, platform);
            
            if(retVal == -1)
            {
                __android_log_write(ANDROID_LOG_WARN, "initializeAndSetupHints","MC_HINT_PLATFORMTYPE hint not set");
            }
            
             __android_log_print(ANDROID_LOG_DEBUG, "performDetection","MC_HINT_PLATFORMTYPE : %d",platform);
              
     
            retVal = MCDetectorSetHint(detector, MC_HINT_SOURCETYPE, sourceType);   
            
            if(retVal == -1)
            {
                __android_log_write(ANDROID_LOG_WARN, "initializeAndSetupHints","MC_HINT_SOURCETYPE hint not set");
            } 
            
            __android_log_print(ANDROID_LOG_DEBUG, "performDetection","MC_HINT_SOURCETYPE : %d",sourceType);
                
            retVal = MCDetectorSetHint(detector, MC_HINT_FILELEN, fileSize);   // need filesize of encrypted file
            
            if(retVal == -1)
            {
                __android_log_write(ANDROID_LOG_WARN, "initializeAndSetupHints","MC_HINT_FILELEN hint not set");
            }
            
              __android_log_print(ANDROID_LOG_DEBUG, "performDetection","MC_HINT_FILELEN : %d",fileSize);
            
            return JNI_TRUE;
        }
        else
        {
             __android_log_write(ANDROID_LOG_ERROR, "initializeAndSetupHints","MCCreateDetector returned NULL");
            return JNI_FALSE;
        }
      
      }

/*
 * Class:     com_mozy_mobile_android_security_MzCryptoLibAPI
 * Method:    performDetection
 * Signature: (Ljava/lang/String;)Z
 */

JNIEXPORT jboolean JNICALL Java_com_mozy_mobile_android_security_MzCryptoLibAPI_performDetection
  (JNIEnv *env, jobject obj, jstring passphrase)
{
         const jbyte *passphraseStr;
         
         jint passphraseStrLen;
         
         passphraseStr = (*env)->GetStringUTFChars(env, passphrase, NULL);
         
         if(passphraseStr == NULL)
         {
            __android_log_write(ANDROID_LOG_ERROR, "performDetection","passphrase is null");
            return JNI_FALSE;
         }
            
        passphraseStrLen = (*env)->GetStringUTFLength(env, passphrase);
           
        possibleKeys = MCDetectorPerform(detector, passphraseStr, passphraseStrLen, &arrayLen);
        
         __android_log_print(ANDROID_LOG_DEBUG, "performDetection","MCDetectorPerform Number of Keys : %d",arrayLen);
        
       
       (*env)->ReleaseStringUTFChars(env, passphrase, passphraseStr);
       
       if(possibleKeys == NULL)
       {
           __android_log_write(ANDROID_LOG_ERROR, "performDetection","MCDetectorPerform returned NULL");
            return JNI_FALSE; 
       }
            
       
       return JNI_TRUE;
}

/*
 * Class:     com_mozy_mobile_android_security_MzCryptoLibAPI
 * Method:    GetByterangeForValidate
 * Signature: (IJ)V
 */

JNIEXPORT jboolean JNICALL Java_com_mozy_mobile_android_security_MzCryptoLibAPI_GetByterangeForValidate
  (JNIEnv * env, jobject obj, jlong filelen)
{
   // Get the byterange that we need in order to validate the key for BF
    size_t rangeStart, rangeLen;
    
   int retVal;
   
   jclass clazz;
   
   jfieldID  fid;
    
    
    MCByterangeForValidate(MC_BF_CIPHER, filelen, &rangeStart, &rangeLen);
    
    if(retVal == -1)
    {
        __android_log_write(ANDROID_LOG_ERROR, "GetByterangeForValidate","MCByterangeForValidate for BF returned -1");
        return JNI_FALSE; 
    }
    else
    {
         // Blowfish Range Start
    
         clazz = (*env)->GetObjectClass (env, obj);
        
    
         fid = (*env)->GetFieldID (env, clazz, "BFFileRangeStart", "J");
        
        // If this field does not exist then return null.
        if (fid == 0)
        {
            __android_log_write(ANDROID_LOG_ERROR, "GetByterangeForValidate","BFFileRangeStart not found");
        }
        else
            __android_log_print(ANDROID_LOG_DEBUG, "GetByterangeForValidate","BF Range Start  : %zu",rangeStart);
               
    
        (*env)->SetLongField (env, obj, fid, rangeStart);
       
       
         // Blowfish Range Length
         
         clazz = (*env)->GetObjectClass (env, obj);
       
    
        fid = (*env)->GetFieldID (env, clazz,  "BFFileRangeLen", "J");
        
        
        // If this field does not exist then return null.
        if (fid == 0)
        {
              __android_log_write(ANDROID_LOG_ERROR, "GetByterangeForValidate","BFFileRangeLen not found");
                return JNI_FALSE;
        }
        else
            __android_log_print(ANDROID_LOG_DEBUG, "GetByterangeForValidate","BF Range Length: %zu",rangeLen);
      
        (*env)->SetLongField (env, obj, fid, rangeLen);
    }
    
    
    // Get the byte range that we need in order to validate the key for AES

    retVal = MCByterangeForValidate(MC_AES_CIPHER, filelen, &rangeStart, &rangeLen);
    
    if(retVal == -1)
    {
        __android_log_write(ANDROID_LOG_ERROR, "GetByterangeForValidate","MCByterangeForValidate for AES returned -1");
    }
    else
    {
    
        // AES Range Start
        
        fid = (*env)->GetFieldID (env, clazz, "AESFileRangeStart", "J");
        
        // If this field does not exist then return null.
        if (fid == 0)
        {
                __android_log_write(ANDROID_LOG_ERROR, "GetByterangeForValidate","AESFileRangeStart not found");
                return JNI_FALSE;
        }
        else
           __android_log_print(ANDROID_LOG_DEBUG, "GetByterangeForValidate","AES Range Start: %zu",rangeStart);
                
    
        (*env)->SetLongField (env, obj, fid, rangeStart);
        
        
        // AES Range Length
         
    
        fid = (*env)->GetFieldID (env, clazz,  "AESFileRangeLen", "J");
        
        
        // If this field does not exist then return null.
        if (fid == 0)
        {
            __android_log_write(ANDROID_LOG_ERROR, "GetByterangeForValidate","AESFileRangeLen not found");
            return JNI_FALSE;
        }
        else
            __android_log_print(ANDROID_LOG_DEBUG, "GetByterangeForValidate","AES Range Length: %zu",rangeLen);
                
    
        (*env)->SetLongField (env, obj, fid, rangeLen);
    }

    return JNI_TRUE;
}

/*
 * Class:     com_mozy_mobile_android_security_MzCryptoLibAPI
 * Method:    validateKey
 * Signature: ([B[B[BJ)V
 */

JNIEXPORT jbyteArray JNICALL Java_com_mozy_mobile_android_security_MzCryptoLibAPI_validateKey
  (JNIEnv * env, jobject obj , jbyteArray AESBuf, jbyteArray BFBuf, jlong fileSize)
  {
        jbyte *AESbuffer = NULL;
        jsize AESbufferLen = 0;
        jbyteArray barr = NULL;
          
        if(AESBuf != NULL)
        {
            AESbuffer = (*env)->GetByteArrayElements(env, AESBuf, NULL);
              
            if(AESbuffer == NULL)
            {
              __android_log_write(ANDROID_LOG_ERROR, "validateKey","AESbuffer null");
                 return JNI_FALSE;
            }
            
            AESbufferLen = (*env)->GetArrayLength(env, AESBuf);
           
        }
             
             
        jbyte *BFbuffer = NULL;
        jsize BFbufferLen = 0;
        
        if(BFBuf != NULL)
        {
          
            BFbuffer = (*env)->GetByteArrayElements(env, BFBuf, NULL);
          
            if(BFbuffer == NULL)
            {
                __android_log_write(ANDROID_LOG_ERROR, "validateKey","BFbuffer null");
                 return NULL;
            }
                 
                 
            BFbufferLen = (*env)->GetArrayLength(env, BFBuf);
        }
               
             
        // Loop through and find a verified key
       
       int i=0;
       int success = -1;
       MCKey *key ;
       
       for (i=0; i<arrayLen; i++) 
       {
          key = possibleKeys[i];
          
          __android_log_print(ANDROID_LOG_DEBUG, "validateKey", "key cipher type(0=BF, 1=AES): %i", key->cipher);
          
          char keyStr[128];          
          memcpy(keyStr, key->data, key->len);     
          keyStr[key->len] = '\0';
          
          __android_log_print(ANDROID_LOG_DEBUG, "validateKey", "key : %s", keyStr);
        
          //if (storedKey == key)
          //  printf("libcrypto returned the same hashed key!!!");
        
        if(key->cipher == MC_BF_CIPHER)
            success =  MCValidateKey(key, fileSize, &BFbuffer[0], BFbufferLen); 
        else if(key->cipher == MC_AES_CIPHER)
            success =  MCValidateKey(key, fileSize, &AESbuffer[0], AESbufferLen); 
        else
        {
            __android_log_write(ANDROID_LOG_ERROR, "validateKey","Invalid cipher for the key");
            return NULL;
        }
        
         if (success == 0) 
         {
             __android_log_print(ANDROID_LOG_DEBUG, "validateKey", "Found Key");
              break;
          }
       }
       
       
    if (success == 0) 
    {
        jclass clazz = (*env)->GetObjectClass (env, obj);
        
    
        jfieldID fid = (*env)->GetFieldID (env, clazz, "cipher", "I");
        // If this field does not exist then return null.
        if (fid == 0)
        {
                __android_log_write(ANDROID_LOG_ERROR, "validateKey","cipher field not found");
                return NULL;
        }
                
        (*env)->SetIntField (env, obj, fid, key->cipher);
        
         __android_log_print(ANDROID_LOG_DEBUG, "validateKey","cipher set %i", key->cipher);
            
          barr = (*env)->NewByteArray(env, key->len);
          if(barr == NULL)
          {
                __android_log_write(ANDROID_LOG_ERROR, "validateKey","NewByteArray returned null");
                return NULL;
          }
      
      
          jbyte *jBytes = (*env)->GetByteArrayElements(env, barr, 0);
          
          
          memcpy(jBytes,key->data, key->len);
         
      
          (*env)->SetByteArrayRegion (env,barr, 0, key->len, jBytes);
          
          __android_log_print(ANDROID_LOG_DEBUG, "validateKey","key length %d", key->len );
        
          __android_log_print(ANDROID_LOG_DEBUG, "validateKey","key set" );
      }
      else
        __android_log_write(ANDROID_LOG_DEBUG, "validateKey","No Key validated");
       
      if(AESbuffer != NULL)
            (*env)->ReleaseByteArrayElements(env, AESBuf, AESbuffer, 0);
            
      if(BFbuffer != NULL)
            (*env)->ReleaseByteArrayElements(env, BFBuf, BFbuffer, 0);
       
       
        return barr;
  }


/*
 * Class:     com_mozy_mobile_android_security_MzCryptoLibAPI
 * Method:    cleanUp
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_mozy_mobile_android_security_MzCryptoLibAPI_cleanUp
  (JNIEnv *env, jobject obj)
{

    // Reset Detector
    
    MCDetectorReset(detector);
    
    // Clean up the array
     MCCleanupKeyArray(possibleKeys, arrayLen);
  
    // Clean up the detector
     MCDetectorCleanup(detector);
    
}


/*
 * Class:     com_mozy_mobile_android_security_MzCryptoLibAPI_getCkey
 * Method:    ckey
 * Signature: ([B)V
 */
JNIEXPORT jbyteArray JNICALL Java_com_mozy_mobile_android_security_MzCryptoLibAPI_getCkey
  (JNIEnv *env, jobject obj,  jbyteArray busBuf)
  {
  
    jsize bufferLen = 0;
    jbyte *busBuffer = NULL;
    
    if(busBuf != NULL)
    {
        busBuffer = (*env)->GetByteArrayElements(env, busBuf, NULL);
          
        if(busBuffer == NULL)
        {
          __android_log_write(ANDROID_LOG_ERROR, "getCKey","busBuffer null");
             return NULL;
        }
        
        bufferLen = (*env)->GetArrayLength(env, busBuf);
        
        __android_log_print(ANDROID_LOG_DEBUG, "getCKey","bufferLen %d", bufferLen);
        
        char inBufStr[256];          
        memcpy(inBufStr, busBuffer, bufferLen);     
        inBufStr[bufferLen] = '\0';
          
       __android_log_print(ANDROID_LOG_DEBUG, "getCKey", "Input buffer : %s", inBufStr);
    }
     
     jbyteArray barr = NULL;
     
     char * outKey  = malloc(bufferLen - 28);
     unsigned int outLen = bufferLen - 28;
     
     int result = MCDecodeCKey(&busBuffer[0], bufferLen, outKey, &outLen);
     
     __android_log_print(ANDROID_LOG_DEBUG, "getCKey","outLen %d", outLen);
    
    if (-1 == result) {
       __android_log_write(ANDROID_LOG_ERROR,"getCKey","   UNABLE TO DECRYPT CKEY: FAIL\n");
        return NULL;
    }
 
      barr = (*env)->NewByteArray(env, outLen);
      if(barr == NULL)
      {
            __android_log_write(ANDROID_LOG_ERROR, "getCKey","NewByteArray returned null");
            return NULL;
      }
      
      
      jbyte *jBytes = (*env)->GetByteArrayElements(env, barr, 0);
      
      memcpy(jBytes,&outKey[0],outLen);
     
      (*env)->SetByteArrayRegion (env,barr, 0, outLen, jBytes);
      
      __android_log_print(ANDROID_LOG_DEBUG, "getCKey","outLen %d", outLen );

      free(outKey);
      return barr;
  }
 