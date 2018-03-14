package com.mozy.mobile.android.activities.tasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.mozy.mobile.android.R;
import com.mozy.mobile.android.activities.ContextMenuActivity;
import com.mozy.mobile.android.activities.SecuredActivity;
import com.mozy.mobile.android.files.DecryptKey;
import com.mozy.mobile.android.files.LocalFile;
import com.mozy.mobile.android.provisioning.Provisioning;
import com.mozy.mobile.android.security.AESEncryptDecryptEngine;
import com.mozy.mobile.android.security.BFEncryptDecryptEngine;
import com.mozy.mobile.android.security.MzCryptoLibAPI;
import com.mozy.mobile.android.utils.LogUtil;
import com.mozy.mobile.android.utils.SystemState;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;

public class FileDecrypterTask extends AsyncTask<Void, Integer, Integer> 
{
    private ProgressDialog progressDialog = null;
    private DecryptKey decryptKeyHint = new DecryptKey();
    private LocalFile localFile;
    private LocalFile outputFile;
    private String deviceId;
    private String platform;
    private String passPhrase;
    private SecuredActivity contextActivity;
    private final Listener listener;
    
    public static interface Listener {
        void onDecryptionTaskCompleted(LocalFile outputFile);
    }
    
    public FileDecrypterTask(LocalFile localFile, SecuredActivity activity ,String deviceId, String platform, Listener listener)
    {
        
        this.localFile = localFile;
        this.deviceId = deviceId;
        this.platform = platform;
        this.contextActivity = activity;
        this.passPhrase  = Provisioning.getInstance(this.contextActivity).getPassPhraseForContainer(this.deviceId);
        
        this.listener = listener;
    }
    
    
//    @Override
    protected void onPreExecute()
    {
        super.onPreExecute();
        
        
        if(SystemState.isManagedKeyEnabled(this.contextActivity) == false)
        {
            // Read saved Key hint for personal key for the container
            this.decryptKeyHint = getLastSuccessKeyForContainer(this.deviceId);
        }
        else
        {
            //Managed Key Decryption
            this.decryptKeyHint.set_key(SystemState.getManagedKey(this.contextActivity));
        }
        
        String strProgressMessage = this.contextActivity.getString( R.string.Decrypting_File);

        // Show the progress dialog
        this.progressDialog = new ProgressDialog(this.contextActivity);
       // this.progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        this.progressDialog.setMessage(strProgressMessage);
        this.progressDialog.setIndeterminate(true);
        this.progressDialog.setCancelable(true);
        this.progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                this.contextActivity.getText(R.string.cancel_button_text),
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id) {
                                                    cancel(true);
                                                }
        });
        this.progressDialog.show();
    }

    @Override
    protected Integer doInBackground(Void... params)
    {
        this.outputFile = new LocalFile();
        
        if(SystemState.isManagedKeyEnabled(this.contextActivity) == false)
        {
            // Decrypt File
            this.decryptKeyHint = decryptFileForPersonalKey(this.localFile, this.passPhrase, this.deviceId, this.platform, this.decryptKeyHint);
        }
        else
        {
            
            this.decryptKeyHint = decryptFileForManagedKey(this.localFile, this.platform, this.decryptKeyHint);
        }
        return 0;
    }

    @Override
    protected void onPostExecute(Integer status)
    {
        super.onPostExecute(status);
        this.progressDialog.dismiss();
        
        updateEncryptedFileDB();
        
        listener.onDecryptionTaskCompleted(this.outputFile);
    }
    
    
    @Override
    protected void onCancelled() {
        this.progressDialog.dismiss();
        if(listener != null)  
        {
            if(this.outputFile.file != null)
                deleteFile(this.outputFile);
            
            listener.onDecryptionTaskCompleted(null);
        }
    }


   
    /**
     * @param cloudFilePath
     */
    public void updateEncryptedFileDB() {
        
        //save the new key hint and update db, if success and the decryption task not cancelled
        if(decryptKeyHint != null && (isCancelled() == false))
        {
              //Need to save, if personal key
              if(SystemState.isManagedKeyEnabled(this.contextActivity) == false)
              {  
                  saveLastSuccessKeyForContainer(this.deviceId, decryptKeyHint);
              }
              
              updateDBPostDecryption(this.localFile.file.lastModified());
        }
        else
        {
            // Decryption did not succeed

            updateDBPostDecryption(-1);
            
            if(contextActivity != null)
            {
                  ((Activity) contextActivity).showDialog(ContextMenuActivity.PRIVATE_KEY_ENCRYPTED_ERROR_MSG);
            }
        }
    }
    
  

    /**
     * @param cloudFilePath
     */
    protected void updateDBPostDecryption(long decryptedDate) {
        
        // File should exists in DB as it as been successfully downloaded previously
          if((SystemState.mozyFileDB != null) && SystemState.mozyFileDB.existsFileInDB(this.deviceId,localFile.getName()) == true)
          {
              SystemState.mozyFileDB.updateFileWithEncryptionDateInDB(this.deviceId, 
                      localFile.getName(), decryptedDate);
          }
    }
      
//    /**
//     * @param inputFilename
//     * @param passPhrase
//     * @param platform
//     * @param hint
//     * @return
//     */
//    public DecryptKey getKeySchemeForFile(File inputFile,String passPhrase, String platform, DecryptKey hint)
//    {
//         DecryptKey decryptKey = null;
//         
//         // Call the mzcrypto lib here
//        
//         
//        // byte[] theByteArray = generateKeyFromPassPhrase(passPhrase, "AES", "V3", platform);
//         //decryptKey = new DecryptKey(theByteArray, "AES", "V3");
//         
//         byte[] theByteArray = generateKeyFromPassPhrase(passPhrase, "BF", "V1", platform);
//         decryptKey = new DecryptKey(theByteArray, "BF", "V1");
//        
//        
//        LogUtil.debug("FileDecrypterTask:getKeySchemeForFile", "\n" + decryptKey.get_scheme());
//         
//        return decryptKey;
//    }

///**
// * @param inputFile
// * @param passPhrase
// * @param deviceId
// * @param platform
// * @param decryptKeyHint
// * @return
// */
protected DecryptKey decryptFileForPersonalKey(LocalFile inputFile,  String passPhrase, String deviceId, String platform, DecryptKey decryptKeyHint)
{
      DecryptKey decryptKey = null;
      String inputFileName = "";
      
      if(inputFile != null)
          inputFileName = inputFile.file.getName();

     
     if(inputFile != null &&  inputFileName.length() != 0)
     {
        if(passPhrase.length() != 0 &&  (isCancelled() == false) ) 
        {
       
          decryptKey =  detectKeyForFile(inputFile, passPhrase, platform, decryptKeyHint);
          //decryptKey  = getKeySchemeForFile(inputFile.file, passPhrase, platform,  decryptKeyHint);
           
            
          if(decryptKey != null)
          {
            String decryptKeyStr = byteToHex(decryptKey.get_key());                
            LogUtil.debug("File Decrypt Key:", decryptKeyStr + "\n");
          }
          else
              LogUtil.debug("File Decrypt Key:", "No key found");   
            
            
           if(decryptKey != null) 
            { 
              if ((decryptKey.get_key().length !=  0) && (decryptKey.get_scheme().length() != 0))
              {
                if(decryptKey.get_scheme().equals("AES") && isCancelled() == false)
                {
                    this.outputFile.file = decryptFile_AES(inputFile.file, decryptKey.get_scheme(), decryptKey.get_key());
                }
                else if(decryptKey.get_scheme().equals("BF") && isCancelled() == false)
               {
                    this.outputFile.file = decryptFile_BF(inputFile.file, decryptKey.get_scheme(), decryptKey.get_key());
                }
              }
            }
        }
      }
     
     if(this.outputFile.file == null) 
     {
         decryptKey = null; 
        // No need to delete the downloaded file
     }
     else
     {
        if(isCancelled() == true)
        {
            decryptKey = null; 
            // No need to delete the downloaded file
            
            if(this.outputFile.file != null)
                deleteFile(this.outputFile);
        }
     }
     
      return decryptKey;
}


    ///**
    //* @param inputFile
    //* @param platform
    //* @param decryptKeyHint
    //* @return
    //*/
    protected DecryptKey decryptFileForManagedKey(LocalFile inputFile, String platform, DecryptKey decryptKey)
    {
        String inputFileName = "";
       
       if(inputFile != null)
       inputFileName = inputFile.file.getName();
  
  
       if(inputFile != null &&  inputFileName.length() != 0)
       {
         if(isCancelled() == false)
         {       
               if(decryptKey != null)
               {
                 String decryptKeyStr = byteToHex(decryptKey.get_key());                
                 LogUtil.debug("File Decrypt Key:", decryptKeyStr + "\n");
               }
               else
                   LogUtil.debug("File Decrypt Key:", "No key found");   
             
             
                if(decryptKey != null) 
                 { 
                    if ((decryptKey.get_key().length !=  0))
                    {        
                         if(getPlatformID(platform) == SystemState.PLATFORM_WINDOWS && isCancelled() == false)
                         {
                             byte[] AESKey = truncateKeyForAES(decryptKey.get_key());
                             this.outputFile.file = decryptFile_AES(inputFile.file, decryptKey.get_scheme(), AESKey);
                         }
                         else if(getPlatformID(platform) == SystemState.PLATFORM_LINUX && isCancelled() == false)
                        {   
                             this.outputFile.file = decryptFile_BF(inputFile.file, decryptKey.get_scheme(), decryptKey.get_key());
                        }
                    }
                 }
             }
       }
      
      if(this.outputFile.file == null) 
      {
          decryptKey = null; 
         // No need to delete the downloaded file
      }
      else
      {
         if(isCancelled() == true)
         {
             decryptKey = null; 
             // No need to delete the downloaded file
             
             if(this.outputFile.file != null)
                 deleteFile(this.outputFile);
         }
      
      }
      
       return decryptKey;
    }


    /**
     * @param decryptKey
     */
    public byte[] truncateKeyForAES(byte[] decryptKey) {
        //For AES truncate it to 32 bytes
         
         byte[] hashBytes = new byte[32];
         
         for(int i = 0; i < 32; i++ )
         {
             hashBytes[i] = decryptKey[i];
         }
         
         return hashBytes;
    }
/**
 * @param decryptKeyHint
 * @return
 */
private int getSchemeID(DecryptKey decryptKeyHint) {
    int schemeid = -1;
    
    if(decryptKeyHint != null)
    {
        if(decryptKeyHint.get_scheme().equals("BF"))
            schemeid = SystemState.CIPHER_BLOWFISH;
        else
            schemeid = SystemState.CIPHER_AES; 
    }
    return schemeid;
}

/**
 * @param platform
 * @return
 */
private int getPlatformID(String platform) {
    int platformid = 0;   // Defaults to windows
    
    if(platform.equals("windows"))
        platformid = SystemState.PLATFORM_WINDOWS;
    else
        platformid = SystemState.PLATFORM_LINUX;
    return platformid;
}

/**
 * @param inputFile
 * @param passPhrase
 * @param decryptKeyHint
 * @param decryptKey
 * @param mzCryptoInstance
 * @param platformid
 * @param schemeid
 */
private DecryptKey detectKeyForFile(LocalFile inputFile, String passPhrase,  String platform, DecryptKey decryptKeyHint) {  
    
    DecryptKey decryptKey = null;
    
    try
    {
        MzCryptoLibAPI mzCryptoInstance = new MzCryptoLibAPI();

        int platformid = getPlatformID(platform);
        
        int schemeid = getSchemeID(decryptKeyHint);
        
        // Call into MzCrypto Library
        if(mzCryptoInstance.initializeAndSetupHints(platformid, SystemState.SRCTYPE_PASSPHRASE, schemeid,  inputFile.file.length()) == true)
        {
            // We have the set of keys at this point
            if(mzCryptoInstance.performDetection(passPhrase) == true)
            {
                // Gets the key for the file
                if(mzCryptoInstance.GetByterangeForValidate(inputFile.file.length()) == true)
                {
                    
                   byte[]  BFBuf =  getBytesForRangeInFile(inputFile, mzCryptoInstance, SystemState.CIPHER_BLOWFISH);
                   byte[]  AESBuf =  getBytesForRangeInFile(inputFile, mzCryptoInstance, SystemState.CIPHER_AES);
                   
                   byte[]  detectedKeyForFile;
                   
                                
                   // We have the key
                    if((detectedKeyForFile = mzCryptoInstance.validateKey(AESBuf, BFBuf, inputFile.file.length())) != null )
                    {
                        decryptKey = new DecryptKey();
                        
                        if(mzCryptoInstance.getCipher() == SystemState.CIPHER_BLOWFISH)
                        {
                            decryptKey.set_scheme("BF");
                            decryptKey.set_key(detectedKeyForFile);
                        }
                        else if(mzCryptoInstance.getCipher() == SystemState.CIPHER_AES)
                        {
                            
                            byte[] AESKey = truncateKeyForAES(detectedKeyForFile);                     
                            decryptKey.set_key(AESKey);
                            decryptKey.set_scheme("AES");
                        }      
                    }
                    mzCryptoInstance.cleanUp();
                }
            }
        }
    }
    catch(RuntimeException e)
    {
        decryptKey = null;
    }
    
    return decryptKey;
}



static final String HEXES = "0123456789ABCDEF";

public String byteToHex( byte [] raw ) {
    if ( raw == null ) {
      return null;
    }
    final StringBuilder hex = new StringBuilder( 2 * raw.length );
    for ( final byte b : raw ) {
      hex.append(HEXES.charAt((b & 0xF0) >> 4))
         .append(HEXES.charAt((b & 0x0F)));
    }
    return hex.toString();
}


/**
 * @param inputFile
 * @param mzCryptoInstance
 * @return
 */
private byte[] getBytesForRangeInFile(LocalFile inputFile,
        MzCryptoLibAPI mzCryptoInstance, int scheme) {
    
    FileInputStream br = null;

    long rangelen  =  mzCryptoInstance.getFileRangeLen(scheme);
    long rangeStart = mzCryptoInstance.getFileRangeStart(scheme);
    
    if((rangelen == -1) || (rangeStart == -1)) return null;
    
    byte[] buf = new byte[(int) rangelen];                            
    try {
        
        br = new FileInputStream(inputFile.file);
        
        long actualSkippedBytes = 0;
        
        actualSkippedBytes = br.skip(rangeStart);
        
        if(actualSkippedBytes < rangeStart)
            return null;
        
        
        int bytesActualRead = 0;
        int offset = 0;
        
        while (offset < rangelen
                && (bytesActualRead=br.read(buf, offset, (int) (rangelen -offset))) >= 0) {
             offset += bytesActualRead;
         }

         // Ensure all the bytes have been read in
         if (offset < rangelen) {
             throw new IOException("Could not completely read file "+inputFile.file.getName());
         }

      } catch (FileNotFoundException ex) {
        buf = null;
      } catch (IOException ex) {
          buf = null;
      }
      finally
      {
          try {
              if(br != null)
                  br.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
      }
      
      return buf;
}


    public File decryptFile_AES(File inputFile, String scheme, byte[] key) 
    {
        AESEncryptDecryptEngine de = new AESEncryptDecryptEngine(inputFile, key);
        return (de.process());
        
    }
    
    
    public File decryptFile_BF(File inputFile, String scheme, byte[] key) 
    {
        File outputFile = null;
        
        if(inputFile.getPath().length() != 0 &&  key.length != 0)
        {
           BFEncryptDecryptEngine de = new BFEncryptDecryptEngine(inputFile, key);
           outputFile =  de.process();
        }
        return outputFile;
    }
    
    
    /**
     * @param deviceName
     * @param decryptKey
     */
    private void saveLastSuccessKeyForContainer(String deviceId, DecryptKey decryptKey)
    {
        // Compare against current key, if different update
        Provisioning.getInstance(contextActivity).setPersonalKeyHint( deviceId, decryptKey);
    }
    
    
    /**
     * @param container
     * @return
     */
    private DecryptKey getLastSuccessKeyForContainer(String deviceId)
    {
        DecryptKey decryptKey = null;
        
        decryptKey = Provisioning.getInstance(contextActivity).getKeyHintForContainer(deviceId);
        
        return decryptKey;
    }
    
    
    private boolean deleteFile(LocalFile outputFile2)
    {
      // Make sure the file or directory exists and isn't write protected
        if (!outputFile2.file.exists())
          throw new IllegalArgumentException(
              "Delete: no such file or directory: " + outputFile2.getPath());

        if (!outputFile2.file.canWrite())
          throw new IllegalArgumentException("Delete: write protected: "
              + outputFile2.getPath());
        
        return (outputFile2.delete());

    }
    
    //
////Test Method
///**
// * @param passPhrase
// * @param scheme
// * @param version
// * @param platform
// * @return
// */
//public byte[] generateKeyFromPassPhrase(String passPhrase, String scheme, String version, String platform)
//{
//    byte [] key = null;
//    
//    if(passPhrase.length()  != 0)
//    {
//       if(platform.equals("windows"))
//       {
//           if(scheme.equals("BF") && (version.equals("V1")  || version.equals("V2")))
//           {
//               try {
//                key =   generateHashForBF(passPhrase);
//               } catch (NoSuchAlgorithmException e) {
//                   // TODO Auto-generated catch block
//                   e.printStackTrace();
//               } catch (UnsupportedEncodingException e) {
//                   // TODO Auto-generated catch block
//                   e.printStackTrace();
//               }
//           }
//           if(scheme.equals("AES") && version.equals("V3"))
//           {
//              try {
//               key =   generateHashForSHA512(passPhrase);
//               } catch (NoSuchAlgorithmException e) {
//                 // TODO Auto-generated catch block
//                 e.printStackTrace();
//              } catch (UnsupportedEncodingException e) {
//                 // TODO Auto-generated catch block
//                 e.printStackTrace();
//              }
//           }
//       }
//       else if(platform.equals("mac"))
//       {
//           if(scheme.equals("BF") && (version.equals("V1")  || version.equals("V2")))
//            {
//               try {
//                   key =   generateHashForBF(passPhrase);
//                  } catch (NoSuchAlgorithmException e) {
//                      // TODO Auto-generated catch block
//                      e.printStackTrace();
//                  } catch (UnsupportedEncodingException e) {
//                      // TODO Auto-generated catch block
//                      e.printStackTrace();
//                  }
//            }
//       }
//    }
//   return key;
//}



//    /**
//     * @param passPhrase
//     * @return
//     * @throws NoSuchAlgorithmException
//     * @throws UnsupportedEncodingException
//     */
//    public byte [] generateHashForSHA512(String passPhrase) throws NoSuchAlgorithmException, UnsupportedEncodingException
//    {
//       byte[] plainText = passPhrase.getBytes("UTF8");
//      //
//      // Get a message digest object using the SHA-512 algorithm
//       MessageDigest messageDigest = MessageDigest.getInstance("SHA-512");              
//      //
//      // Print out the provider used
//       LogUtil.debug("FileDecrypterTask", "\n" + messageDigest.getProvider().getInfo());
//      //
//      // Calculate the digest and print it out
//       messageDigest.update( plainText);
//       
//       
//    // Create the digest from the message
//       byte[] aMessageDigest = messageDigest.digest();
//       
//       
//       messageDigest = MessageDigest.getInstance("SHA-512");
//       
//       messageDigest.update(aMessageDigest);
//       
//       aMessageDigest = messageDigest.digest();
//       
//       LogUtil.debug("FileDecrypterTask", "\nDigest: " + Cryptation.byteArrayToHexString(aMessageDigest));
//       
//       byte[] hashBytes = new byte[32];
//       
//       for(int i = 0; i < 32; i++ )
//       {
//           hashBytes[i] = aMessageDigest[i];
//       }
//       return hashBytes;
//    }
    
    
//  /**
//  * @param passPhrase
//  * @return
//  * @throws NoSuchAlgorithmException
//  * @throws UnsupportedEncodingException
//  */
// public byte [] generateHashForBF(String passPhrase) throws NoSuchAlgorithmException, UnsupportedEncodingException
// {
//    byte[] hashBytes = null;
//    
//    final int BFMaxKeyByteSize = 56;
//    
//    byte[] plainText = passPhrase.getBytes("UTF8");
//   //
//    int keyLength = plainText.length;
//    
//    if(keyLength < BFMaxKeyByteSize)
//    {
//        hashBytes = new byte[keyLength];
//    }
//    else
//    {
//        hashBytes = new byte[BFMaxKeyByteSize];
//    }
//    
//    for(int i = 0; i < hashBytes.length; i++ )
//    {
//        hashBytes[i] = plainText[i];
//    }
//    
//    
//    return hashBytes;
// }

}
