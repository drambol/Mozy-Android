
package com.mozy.mobile.android.security;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;


import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import android.os.Environment;

import com.mozy.mobile.android.utils.FileUtils;
import com.mozy.mobile.android.utils.LogUtil;

public class AESEncryptDecryptEngine 
{
    protected File infile;
    protected File outfileEncrypted;
    protected File outfileDecrypted;
    protected byte[] key = null;
    
    protected Cipher dcipher;
    protected Cipher ecipher;
    
    
   public AESEncryptDecryptEngine(File infile, byte[] key) 
   {
       this.infile = infile;
       this.key = key;
    }

   public File process()
   {
         File outputFile = null;
            
        // File outputFile = performAESEncrypt();  //only for testing
        
         outputFile = performAESDecrypt(this.infile);
      
         return outputFile;
    }
   
   
   public static byte[] getBytes(Long val)
   {
       ByteBuffer buf = ByteBuffer.allocate(8);
       buf.order(ByteOrder.BIG_ENDIAN);
       buf.putLong(val);
       return buf.array();
   }
          
       
   protected File performAESDecrypt(File inputfile)
   {
       InputStream in = null;
       FileOutputStream out = null;
       File outputFile = null;
       
       try {
           byte[] iv = new byte[]{
                 0x00, 0x00, 0x00,0x00,
                 0x00, 0x00, 0x00, 0x00, 
                 0x00, 0x00, 0x00, 0x00, 
                 0x00, 0x00, 0x00, 0x00
             };
           
           long count = 0;
                   
      
           // Buffer used to transport the bytes from one stream to another
           byte[]  buf = null ;
           
           long numberofBlocks = ((inputfile.length())/4096) + 1;
           //int bytesReadForLastBlock = (int) ((inputfile.length())%16);
           
           int bytesDecrypted = 0;
                   
           SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
           
    
           IvParameterSpec paramSpec = new IvParameterSpec(iv);
           
           try
           {
                dcipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
    
               // CBC requires an initialization vector
               dcipher.init(Cipher.DECRYPT_MODE, skeySpec, paramSpec);
      
               try 
               {        
                   try
                   {
                       in = new FileInputStream(inputfile.getAbsolutePath());         

                        File outputDir = new File(inputfile.getParent() + "/" + FileUtils.decryptHiddenDir);  // saved in hidden decrypted folder
                           
                        
                        outputDir.mkdirs();
                        
                       outputFile = new File(outputDir.getAbsolutePath() + "/" + inputfile.getName());
                        
                        if(outputFile.exists())
                            outputFile.delete();
                           
                        out = new FileOutputStream(outputFile, true);
                   
                        // Bytes read from in will be decrypted
                       in = new CipherInputStream(in, dcipher);
        
                       int numRead = 0;   
                  
                       while (numberofBlocks != 0) 
                       {
                          if(numberofBlocks == 1)  // last block
                          {
                              // Read this 16 bytes chunks for last block
                              buf = new byte[16];

                              while ((numRead = in.read(buf)) >= 0) {
                                  out.write(buf, 0, numRead);
                                  bytesDecrypted = bytesDecrypted + numRead;
                              }
                          }
                          else
                          {
                              if(buf == null)
                                  buf = new byte[4096];
                              else
                                  Arrays.fill(buf,(byte) 0);

                              int tmp = 0;
                              numRead = 0;
                              while (numRead < 4096) {
                                  if((tmp = in.read(buf, 0, 4096 - numRead)) >= 0) {
                                      //Write out decrypted bytes
                                      out.write(buf, 0, tmp);
                                  }
                                  numRead += tmp;
                              }
                              bytesDecrypted = bytesDecrypted + numRead;
                          }


                          numberofBlocks = numberofBlocks - 1;

                           // IV for next block
                           count = count + 1;

                           int ivLen = getBytes(count).length;

                           for(int i = 0 ; i < ivLen; i++)
                             iv[8+i] = getBytes(count)[i];

                           paramSpec = new IvParameterSpec(iv);

                           in.close();

                           // Initialize the new stream

                           dcipher.init(Cipher.DECRYPT_MODE, skeySpec, paramSpec);

                           in = new FileInputStream(inputfile.getAbsolutePath());

                           long bytesSkipped = in.skip(bytesDecrypted);

                           if(bytesSkipped != bytesDecrypted)
                           {
                               // We should not be here, fail and return
                               outputFile = null;
                               break;
                           }

                           in = new CipherInputStream(in, dcipher);
                          }
                   }
                   catch (FileNotFoundException e)
                   {
                       e.printStackTrace();
                       outputFile = null;
                   }
               } 
               catch (java.io.IOException e) 
               {
                   e.printStackTrace();
               }
           }
           catch (Exception e)
           {
               e.printStackTrace(); 
           }
       }
       catch (Exception e) 
       {
           e.printStackTrace();
       }
       finally
       {
            //after processing clean up the files
             try
             {
                 if(in != null)
                     in.close();
                 if(out != null)
                 {
                     out.flush();
                     out.close();
                 }
             }
             catch (IOException closing)
             {
                 LogUtil.error(this, "Stream close failure:" + closing.getMessage());
                 outputFile = null;
             }
       }
       
       return outputFile;
   }



/*  ALTERNATE METHOD
 * This method performs all the decryption and writes
 * the plain text to the buffered output stream created
 * previously.
 */
//    protected boolean performAESDecrypt(File inputfile)
//    {    
//         boolean bResult = true;
//         CipherParameters ivAndKey = null;
//         PaddedBufferedBlockCipher cipher = null;
//         
//         cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()));
//        
//        long numberofBlocks = ((inputfile.length())/4096) + 1;
//        int bytesReadForLastBlock = (int) ((inputfile.length())%16);
//      
//        BufferedInputStream in = null;
//        BufferedOutputStream out = null;
//        
//        
//        
//         try
//         {
//            in = new BufferedInputStream(new FileInputStream(inputfile));
//         }
//         catch (FileNotFoundException fnf)
//         {
//             LogUtil.error(this, "Input file not found ["+infile+"]");
//         }
//         
//          try
//          {
//              
//              File outputFile = new File(inputfile.getParent() + "/" + "De" + inputfile.getName());
//             
//             if(outputFile.exists())
//                 outputFile.delete();
//              out = new BufferedOutputStream(new FileOutputStream(outputFile));
//          }
//          catch (IOException fnf)
//          {
//            LogUtil.error(this,"Output file not created");
//          }
//        
//        /* 
//         * now, read the file, and output the chunks
//         */
//        try
//        {
//            
//            byte[] iv = new byte[]{
//                 0x00, 0x00, 0x00,0x00,
//                  0x00, 0x00, 0x00, 0x00, 
//                  0x00, 0x00, 0x00, 0x00, 
//                  0x00, 0x00, 0x00, 0x00
//              };
//         
//           int outL = 0;
//           byte[] inblock = new byte[16];
//           byte[] outblock = null;
//    
//           long count = 0;
//           int bytesRead = 0;
//            
//            while (numberofBlocks > 0)
//            { 
//                int ivLen = getBytes(count).length;
//                
//                for(int i = 0 ; i < ivLen; i++)
//                    iv[8+i] = getBytes(count)[i];
//                
//                ivAndKey = new ParametersWithIV(new KeyParameter(key), iv);
//        
//                
//               // initialize the cipher for decryption
//               cipher.init(false, ivAndKey);
//    
//              for(int k = 0; k < 256; k++)
//              {
//                   if((bytesRead = in.read(inblock)) != -1)
//                   {
//                       if(inblock.length != 0)
//                           outblock = new byte[cipher.getOutputSize(inblock.length)];
//        
//                       outL = cipher.processBytes(inblock, 0, inblock.length, 
//                                                    outblock, 0);
//                        
//                        /*
//                         * Before we write anything out, we need to make sure
//                         * that we've got something to write out. 
//                         */
//                       if (outL > 0)
//                        {
//                           try
//                            {
//                                out.write(outblock, 0, outL);
//                            }
//                            catch (IOException iowrite)
//                            {
//                                iowrite.printStackTrace();
//                               bResult = false;
//                            } 
//                       }
//                   }
//                   else
//                   {
//                       break;
//                   }
//               }
//                   
//               numberofBlocks = numberofBlocks - 1;
//               count ++;
//           }
//    
////            try
//           {
//               /*
//               * Now, process the bytes that are still buffered
//               * within the cipher.
//                */
////             outL = cipher.doFinal(outblock, 0);
////             if (outL > 0)
////             {
////                    out.write(outblock, 0, outL);
////             }
//                out.flush();
//          }
////           catch (CryptoException ce)
////           {
////                LogUtil.error(this, ce.getMessage());
////               bResult = false;
////           }
//       }
//        catch (IOException ioeread)
//        {
//           ioeread.printStackTrace();
//            bResult = false;
//       } catch (DataLengthException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//            bResult = false;
//        } catch (IllegalStateException e) {
//           // TODO Auto-generated catch block
//            e.printStackTrace();
//            bResult = false;
//        }
//        return bResult;
//} 
       


  //   private byte [] readKeyFromFile(File keyfile)
  //   {
//         byte [] key = null;
//         try
//         {
//             // read the key, and decode from hex encoding
//             BufferedInputStream keystream = 
//                 new BufferedInputStream(new FileInputStream(keyfile));
//             int len = keystream.available();
//             byte[] keyhex = new byte[len];
//             keystream.read(keyhex, 0, len);
//             key = Hex.decode(keyhex);
//         }
//         catch (IOException ioe)
//         {
//            LogUtil.error(this,"Decryption key file not found, "+ "or not valid ["+keyfile+"]");
//         }
//      return key;
//   }
     
      
       
   // TEST METHODS    
       
   protected File performAESEncrypt()
   {
      
       InputStream in = null;
       OutputStream out = null;
       File outputfile  = null;
       
       try {
           byte[] iv = new byte[]{
                 0x00, 0x00, 0x00,0x00,
                 0x00, 0x00, 0x00, 0x00, 
                 0x00, 0x00, 0x00, 0x00, 
                 0x00, 0x00, 0x00, 0x00
             };
           
           long count = 0;
 
           // Buffer used to transport the bytes from one stream to another
           byte[] buf = new byte [4096];
           
           SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
           IvParameterSpec paramSpec = new IvParameterSpec(iv);
          
           
           infile = createFileWithRandomData();
           
           try
           {
                ecipher = Cipher.getInstance("AES/CBC/PKCS7Padding");

               // CBC requires an initialization vector
               ecipher.init(Cipher.ENCRYPT_MODE, skeySpec, paramSpec);
           }
           catch (Exception e)
           {
               e.printStackTrace();
           }
          
           
           try {
               in = new FileInputStream(infile.getAbsolutePath());
           } catch (FileNotFoundException e) {
               // TODO Auto-generated catch block
               e.printStackTrace();
           }
       
            try {
                   outputfile = new File ((infile.getParent() + "/" + "En" + infile.getName()));
                   
                   if(outputfile.exists())
                       outputfile.delete();
                   
                   out = new FileOutputStream(outputfile, true);
               } catch (FileNotFoundException e) {
                   // TODO Auto-generated catch block
                   e.printStackTrace();
               }
               
               
           try{
               // Bytes read from in will be decrypted
               out = new CipherOutputStream(out, ecipher);
               
               // Read in the cleartext bytes and write to out to encrypt
               int numRead = 0;
               while (in != null && ((numRead = in.read(buf)) >= 0)){
                   
                   //Write out decrypted bytes
                   out.write(buf, 0, numRead);
                   
                   // IV for next block
                   count = count + 1;
                   
                   
                   int ivLen = getBytes(count).length;
                 
                   for(int i = 0 ; i < ivLen; i++)
                     iv[8+i] = getBytes(count)[i];
                 
                   paramSpec = new IvParameterSpec(iv);
                   
                   out.flush();
                   out.close();
                 
                   ecipher.init(Cipher.ENCRYPT_MODE, skeySpec, paramSpec);
                   
                   out = new FileOutputStream(outputfile, true);
                   
                   out = new CipherOutputStream(out, ecipher);
                   
               }
           }
           catch (java.io.IOException e){
               e.printStackTrace();
           }

   }
   catch (Exception e) {
       e.printStackTrace();
   }
   finally
   {
        //after processing clean up the files
         try
         {
             if(in != null)
                 in.close();
             
             if(out != null)
             {
                 out.flush();
                 out.close();
             }
         }
         catch (IOException closing)
        {
             LogUtil.error(this, "Stream close failure:" + closing.getMessage());
        }
   }
   
   return outputfile;
}

    /**
     * @throws IOException
     */
    private File createFileWithRandomData() throws IOException {
           
           String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + "/Mozy/Mozy Docs" + "/" +  "temp.txt";
           File myFile = new File(path);
           
           Writer out  = null;
           FileOutputStream outputFile = null;
           
           if(myFile.exists())
           { 
               myFile.delete();
           }
           myFile.createNewFile();

           
           Random generator = new Random();   
           //size of the file
           int noOfStrings = generator.nextInt(5)*200; 
           
           if(noOfStrings == 0) noOfStrings = 1;
           
           SecureRandom random = new SecureRandom();
           
           try {
               
              outputFile =   new FileOutputStream(myFile, true);
             
              out = new OutputStreamWriter(new FileOutputStream(myFile), "UTF-8");
              
              for(int i = 0; i < noOfStrings; i++)
              {
                  String str = (new BigInteger(130, random).toString(32));
                  out.write(str);
              }
              
           } 
           catch (IOException e) 
           {
               LogUtil.error(this, "Error: " + e);
           }
           finally
           {
               if(out != null)
                   out.flush();
               
               out.close();
               
               if(outputFile != null)
                   outputFile.close();
           }
           
           return myFile;
    }
}

