package com.mozy.mobile.android.files;

import java.io.File;
import java.io.IOException;

public class LocalFile extends MozyFile
{
       public File file;
        
       public boolean delete()
        {
            boolean status = file.delete();
            return status;
        }
       
       public LocalFile(String path)
       {
          file = new File(path);
       }
       
       public LocalFile(File localfile)
       {
          file = new File(localfile.getPath());
       }
       
       public LocalFile() 
       {
           file = null;
       }

    public String getName()
       {
           return file.getName();
       }
       
       public long getUpdated()
       {
           return file.lastModified();
       }
       
       public long getSize()
       {
           return file.length();
       }
       
       
       public String getPath()
       {
           return file.getPath();
       }
       
       
       public boolean createNewFile() throws IOException
       {
          boolean bResult = false;
          try 
          {
               bResult = file.createNewFile();
           } 
          catch (Exception  e) 
          {
               String path = null;
               try {
                 path = file.getCanonicalPath();
               } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
               }
               if(path != null)
               {
                   int newEnd = path.lastIndexOf(java.io.File.separatorChar);
                   String parentDirectory = path.substring(0, newEnd);
                   java.io.File directory = new java.io.File(parentDirectory);
                   directory.mkdirs();
                   bResult = file.createNewFile();
               }
          }
          return bResult;
       }
}
