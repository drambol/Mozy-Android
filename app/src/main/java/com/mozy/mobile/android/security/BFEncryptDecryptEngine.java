
package com.mozy.mobile.android.security;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.engines.BlowfishEngine;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;

import com.mozy.mobile.android.utils.FileUtils;
import com.mozy.mobile.android.utils.LogUtil;

public class BFEncryptDecryptEngine
{
    protected PaddedBufferedBlockCipher cipher = null;
    
    protected byte[] key = null;
    File infile;
    
    protected BufferedInputStream in = null;
    protected BufferedOutputStream out = null;

    
   public BFEncryptDecryptEngine(File infile, byte[] key) 
   {
       this.infile = infile;
       this.key = key;
    }

   public File process()
   {
     File outputFile;
     
    /* 
     * Setup the Blowfish cipher engine, 
     */
       cipher = new PaddedBufferedBlockCipher(new BlowfishEngine());
       
       {
           outputFile = performBFDecrypt(this.infile);
       }
 
        return outputFile;
    }
   
   protected File performBFDecrypt(File inputFile)
   {    
        byte[] buffer = new byte[1024];
        File outputFile = null;
        
        try
        {
             in = new BufferedInputStream(new FileInputStream(infile));
             
             try
             {
                File outputDir = new File(infile.getParent() + "/" + FileUtils.decryptHiddenDir);  // saved in hidden decrypted folder

                outputDir.mkdirs();
                
               outputFile = new File(outputDir.getAbsolutePath() + "/" + infile.getName());
                
                if(outputFile.exists())
                    outputFile.delete();
                
                out = new BufferedOutputStream(new FileOutputStream(outputFile));
             }
             catch (IOException fnf)
             {
                 LogUtil.error(this,"Output file not created");
             }
        }
        catch (FileNotFoundException fnf)
        {
              LogUtil.error(this, "Input file not found ["+infile+"]");
        }
        
       
        
     // initialize the cipher for decryption
        cipher.init(false, new KeyParameter(key));
        
       /* 
        * now, read the file, and output the chunks
        */
       try
       {
           int outL = 0;
           byte[] inblock = null;
           byte[] outblock = null;
           
           int bytesRead = 0;
           
           while ((bytesRead = in.read(buffer)) != -1)
           {
               inblock = new byte[bytesRead];
           
               System.arraycopy(buffer, 0,inblock, 0, bytesRead);
           
               if(inblock.length != 0)
                  outblock = new byte[cipher.getOutputSize(inblock.length)];

               try
               {
                   outL = cipher.processBytes(inblock, 0, inblock.length, 
                                           outblock, 0);
               }
               catch(Exception e)
               {
                   LogUtil.error(this, e.getMessage());
                   outputFile = null;
               }
               /*
                * Before we write anything out, we need to make sure
                * that we've got something to write out. 
                */
               if (outL > 0)
               {
                   out.write(outblock, 0, outL);
               }
           }

           try
           {
               /*
                * Now, process the bytes that are still buffered
                * within the cipher.
                */
               int outLfinal = cipher.doFinal(outblock, outL);
               if (outLfinal > 0)
               {
                   out.write(outblock, outL, outLfinal);
               }
               out.flush();
           }
           catch (CryptoException ce)
           {
               LogUtil.error(this, ce.getMessage());
               outputFile = null;
           }
       }
       catch (IOException ioeread)
       {
           ioeread.printStackTrace();
           outputFile = null;
       }
       
       return outputFile;
   }
}
