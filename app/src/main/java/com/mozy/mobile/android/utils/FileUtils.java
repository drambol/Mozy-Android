package com.mozy.mobile.android.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;

import com.mozy.mobile.android.R;
import com.mozy.mobile.android.files.CloudFile;
import com.mozy.mobile.android.files.Directory;
import com.mozy.mobile.android.files.LocalFile;
import com.mozy.mobile.android.files.Music;
import com.mozy.mobile.android.files.Photo;
import com.mozy.mobile.android.files.Video;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources.NotFoundException;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.webkit.MimeTypeMap;

public class FileUtils {
    
    public final static int CATEGORY_UNKNOWN = 0;
    public final static int CATEGORY_PHOTOS = 1;
    public final static int CATEGORY_MUSIC = 2;
    public final static int CATEGORY_VIDEOS = 3;
    public final static int CATEGORY_PDF = 4;
    public final static int CATEGORY_MSWORD = 5;
    public final static int CATEGORY_MSEXCEL = 6;
    public final static int CATEGORY_MSPOWERPOINT = 7;
    public static final int CATEGORY_TEXT_FILE = 8;
    
    public final static Integer CATEGORY_PHOTOS_HOLDER = Integer.valueOf(CATEGORY_PHOTOS);
    public final static Integer CATEGORY_MUSIC_HOLDER = Integer.valueOf(CATEGORY_MUSIC);
    public final static Integer CATEGORY_VIDEOS_HOLDER = Integer.valueOf(CATEGORY_VIDEOS);
    public final static Integer CATEGORY_PDF_HOLDER = Integer.valueOf(CATEGORY_PDF);
    public final static Integer CATEGORY_MSWORD_HOLDER = Integer.valueOf(CATEGORY_MSWORD);
    public final static Integer CATEGORY_MSEXCEL_HOLDER = Integer.valueOf(CATEGORY_MSEXCEL);
    public final static Integer CATEGORY_MSPOWERPOINT_HOLDER = Integer.valueOf(CATEGORY_MSPOWERPOINT);   
    public final static Integer CATEGORY_TEXT_FILE_HOLDER = Integer.valueOf(CATEGORY_TEXT_FILE);
    
    //Since we need a synchronized structure, we are using Hashtable.
    private static Hashtable<String, String> extensionToMimeTypeMap;
    private static Hashtable<String, Integer> mimeTypeToCategoryMap;    
    private static final int initialHashTableCapacity = 300;
    
    
    public static final String photoSearch = ".jpg .jpeg .png .gif .bmp .tif";   
    public static final String documentSearch = ".doc .docx .pdf .ppt .pptx .xls .xlsx .txt .rtf .wps .odt .csv .wks .ods .pps .odp .vsd .odg"; 
    public static final String musicSearch = ".mp3 .aac .m4a .wma .wav .aif";
    public static final String videoSearch = ".mov .qt .mp4 .mpg .mpeg .m4v .avi .wmv .3gp .3g2";
    
    /**
     * File type icon drawables
     */
    public static Hashtable<String, Drawable> sFileTypeIcons = new Hashtable<String, Drawable>();
    
    
    // Time calculation values
    private static final long SECOND = 1000;
    private static final long MINUTE = 60000;
    private static final long HOUR = 3600000;
    private static final long DAY = 86400000;
    private static final long WEEK = 604800000;
    private static final long MONTH = 2542000000L;
    private static final long YEAR = 31536000000L;
    
    public static final String decryptHiddenDir = ".Decrypted";
    public static final String encryptHiddenDir = ".Encrypted";
    public static final String uploadHiddenDir = ".Upload";

        
    static
    {
        // mime-type to category mapping
        mimeTypeToCategoryMap = new Hashtable<String, Integer>();
    
        // mime-types in the photos category
        mimeTypeToCategoryMap.put("image/gif", CATEGORY_PHOTOS_HOLDER);
        mimeTypeToCategoryMap.put("image/jpeg", CATEGORY_PHOTOS_HOLDER);
        mimeTypeToCategoryMap.put("image/png", CATEGORY_PHOTOS_HOLDER);
        mimeTypeToCategoryMap.put("image/bmp", CATEGORY_PHOTOS_HOLDER);
        mimeTypeToCategoryMap.put("image/x-ms-bmp", CATEGORY_PHOTOS_HOLDER);
        mimeTypeToCategoryMap.put("image/x-xbitmap", CATEGORY_PHOTOS_HOLDER);
        mimeTypeToCategoryMap.put("image/x-xpixmap", CATEGORY_PHOTOS_HOLDER);
        mimeTypeToCategoryMap.put("image/tiff", CATEGORY_PHOTOS_HOLDER);
        
        // mime-types in the music category
        mimeTypeToCategoryMap.put("audio/aac", CATEGORY_MUSIC_HOLDER);
        mimeTypeToCategoryMap.put("audio/basic", CATEGORY_MUSIC_HOLDER);
        mimeTypeToCategoryMap.put("audio/flac", CATEGORY_MUSIC_HOLDER);
        mimeTypeToCategoryMap.put("audio/mp3", CATEGORY_MUSIC_HOLDER);    
        mimeTypeToCategoryMap.put("audio/mp4a-latm", CATEGORY_MUSIC_HOLDER);
        mimeTypeToCategoryMap.put("audio/mpeg", CATEGORY_MUSIC_HOLDER);
        mimeTypeToCategoryMap.put("audio/mid", CATEGORY_MUSIC_HOLDER);
        mimeTypeToCategoryMap.put("audio/ogg", CATEGORY_MUSIC_HOLDER);
        mimeTypeToCategoryMap.put("audio/x-mpegurl", CATEGORY_MUSIC_HOLDER);
        mimeTypeToCategoryMap.put("audio/x-aac", CATEGORY_MUSIC_HOLDER);
        mimeTypeToCategoryMap.put("audio/x-aiff", CATEGORY_MUSIC_HOLDER);
        mimeTypeToCategoryMap.put("audio/x-m4a", CATEGORY_MUSIC_HOLDER);
        mimeTypeToCategoryMap.put("audio/mp4a-latm", CATEGORY_MUSIC_HOLDER);
        mimeTypeToCategoryMap.put("audio/x-mpeg-3", CATEGORY_MUSIC_HOLDER);
        mimeTypeToCategoryMap.put("audio/x-ms-wma", CATEGORY_MUSIC_HOLDER);
        mimeTypeToCategoryMap.put("audio/x-wav", CATEGORY_MUSIC_HOLDER);
        mimeTypeToCategoryMap.put("audio/x-pn-realaudio", CATEGORY_MUSIC_HOLDER);
        
        // mime-types in the pdf category        
        mimeTypeToCategoryMap.put("application/pdf", CATEGORY_PDF);
        // mime-types in the Word category        
        mimeTypeToCategoryMap.put("application/msword", CATEGORY_MSWORD_HOLDER);
        mimeTypeToCategoryMap.put("application/vnd.ms-word", CATEGORY_MSWORD_HOLDER);        
        mimeTypeToCategoryMap.put("application/vnd.ms-word.document.macroEnabled.12", CATEGORY_MSWORD_HOLDER);
        mimeTypeToCategoryMap.put("application/vnd.ms-word.template.macroEnabled.12", CATEGORY_MSWORD_HOLDER);
        mimeTypeToCategoryMap.put("application/vnd.openxmlformats-officedocument.wordprocessingml.document", CATEGORY_MSWORD_HOLDER);
        mimeTypeToCategoryMap.put("application/vnd.openxmlformats-officedocument.wordprocessingml.template", CATEGORY_MSWORD_HOLDER);
        // mime-types in the Excel category        
        mimeTypeToCategoryMap.put("application/vnd.ms-excel", CATEGORY_MSEXCEL_HOLDER);
        mimeTypeToCategoryMap.put("application/vnd.ms-excel.sheet.binary.macroEnabled.12", CATEGORY_MSEXCEL_HOLDER);
        mimeTypeToCategoryMap.put("application/vnd.ms-excel.sheet.macroEnabled.12", CATEGORY_MSEXCEL_HOLDER);
        mimeTypeToCategoryMap.put("application/vnd.ms-excel.template.macroEnabled.12", CATEGORY_MSEXCEL_HOLDER);        
        mimeTypeToCategoryMap.put("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", CATEGORY_MSEXCEL_HOLDER);
        mimeTypeToCategoryMap.put("application/vnd.openxmlformats-officedocument.spreadsheetml.template", CATEGORY_MSEXCEL_HOLDER);
        // mime-types in the Powerpoint category        
        mimeTypeToCategoryMap.put("application/vnd.ms-powerpoint", CATEGORY_MSPOWERPOINT_HOLDER);
        mimeTypeToCategoryMap.put("application/vnd.ms-powerpoint.addin.macroEnabled.12", CATEGORY_MSPOWERPOINT_HOLDER);
        mimeTypeToCategoryMap.put("application/vnd.ms-powerpoint.presentation.macroEnabled.12", CATEGORY_MSPOWERPOINT_HOLDER);        
        mimeTypeToCategoryMap.put("application/vnd.ms-powerpoint.template.macroEnabled.12", CATEGORY_MSPOWERPOINT_HOLDER);
        mimeTypeToCategoryMap.put("application/vnd.ms-powerpoint.slideshow.macroEnabled.12", CATEGORY_MSPOWERPOINT_HOLDER);        
        mimeTypeToCategoryMap.put("application/vnd.openxmlformats-officedocument.presentationml.presentation", CATEGORY_MSPOWERPOINT_HOLDER);
        mimeTypeToCategoryMap.put("application/vnd.openxmlformats-officedocument.presentationml.slideshow", CATEGORY_MSPOWERPOINT_HOLDER);        
        mimeTypeToCategoryMap.put("application/vnd.openxmlformats-officedocument.presentationml.template", CATEGORY_MSPOWERPOINT_HOLDER);
        // mime-types in the video category
        mimeTypeToCategoryMap.put("video/3gpp", CATEGORY_VIDEOS_HOLDER);
        mimeTypeToCategoryMap.put("video/3gpp2", CATEGORY_VIDEOS_HOLDER);
        mimeTypeToCategoryMap.put("video/mp4", CATEGORY_VIDEOS_HOLDER);
        mimeTypeToCategoryMap.put("video/mpeg", CATEGORY_VIDEOS_HOLDER);        
        mimeTypeToCategoryMap.put("video/quicktime", CATEGORY_VIDEOS_HOLDER);
        mimeTypeToCategoryMap.put("video/x-m4v", CATEGORY_VIDEOS_HOLDER);
        mimeTypeToCategoryMap.put("video/x-msvideo", CATEGORY_VIDEOS_HOLDER);        
        mimeTypeToCategoryMap.put("video/x-sgi-movie", CATEGORY_VIDEOS_HOLDER);
        mimeTypeToCategoryMap.put("video/x-flv", CATEGORY_VIDEOS_HOLDER);        
        mimeTypeToCategoryMap.put("video/x-ms-wmv", CATEGORY_VIDEOS_HOLDER);
        mimeTypeToCategoryMap.put("video/x-ms-asf", CATEGORY_VIDEOS_HOLDER);
        mimeTypeToCategoryMap.put("video/x-la-asf", CATEGORY_VIDEOS_HOLDER);
        mimeTypeToCategoryMap.put("video/quicktime", CATEGORY_VIDEOS_HOLDER);
        
        // mime-types in the text catogery
        
        mimeTypeToCategoryMap.put("text/h323", CATEGORY_TEXT_FILE_HOLDER);
        mimeTypeToCategoryMap.put("text/plain", CATEGORY_TEXT_FILE_HOLDER);
        mimeTypeToCategoryMap.put("text/plain", CATEGORY_TEXT_FILE_HOLDER);
        mimeTypeToCategoryMap.put("text/plain", CATEGORY_TEXT_FILE_HOLDER);
        mimeTypeToCategoryMap.put("text/css", CATEGORY_TEXT_FILE_HOLDER);
        mimeTypeToCategoryMap.put("text/plain", CATEGORY_TEXT_FILE_HOLDER);
        mimeTypeToCategoryMap.put("text/x-setext", CATEGORY_TEXT_FILE_HOLDER);
        mimeTypeToCategoryMap.put("text/plain", CATEGORY_TEXT_FILE_HOLDER);
        mimeTypeToCategoryMap.put("text/x-component", CATEGORY_TEXT_FILE_HOLDER);
        mimeTypeToCategoryMap.put("text/html", CATEGORY_TEXT_FILE_HOLDER);
        mimeTypeToCategoryMap.put("text/html", CATEGORY_TEXT_FILE_HOLDER);
        mimeTypeToCategoryMap.put("text/webviewhtml", CATEGORY_TEXT_FILE_HOLDER);
        mimeTypeToCategoryMap.put("text/richtext", CATEGORY_TEXT_FILE_HOLDER);
        mimeTypeToCategoryMap.put("text/scriptlet", CATEGORY_TEXT_FILE_HOLDER);
        mimeTypeToCategoryMap.put("text/html", CATEGORY_TEXT_FILE_HOLDER);
        mimeTypeToCategoryMap.put("text/plain", CATEGORY_TEXT_FILE_HOLDER);
        mimeTypeToCategoryMap.put("text/iuls", CATEGORY_TEXT_FILE_HOLDER);
        mimeTypeToCategoryMap.put("text/xml", CATEGORY_TEXT_FILE_HOLDER);
       
        
        // file extension to mime-type mapping       
        extensionToMimeTypeMap = new Hashtable<String, String>(initialHashTableCapacity);
        extensionToMimeTypeMap.put("323","text/h323");
        extensionToMimeTypeMap.put("3gp","video/3gpp");
        extensionToMimeTypeMap.put("3g2","video/3gpp2");
        extensionToMimeTypeMap.put("aac","audio/aac");
        extensionToMimeTypeMap.put("acx","application/internet-property-stream");
        extensionToMimeTypeMap.put("ai","application/postscript");
        extensionToMimeTypeMap.put("aif","audio/x-aiff");
        extensionToMimeTypeMap.put("aifc","audio/x-aiff");
        extensionToMimeTypeMap.put("aiff","audio/x-aiff");
        extensionToMimeTypeMap.put("arw","image/x-sony-arw");
        extensionToMimeTypeMap.put("asf","video/x-ms-asf");
        extensionToMimeTypeMap.put("asr","video/x-ms-asf");
        extensionToMimeTypeMap.put("asx","video/x-ms-asf");
        extensionToMimeTypeMap.put("au","audio/basic");
        extensionToMimeTypeMap.put("avi","video/x-msvideo");
        extensionToMimeTypeMap.put("axs","application/olescript");
        extensionToMimeTypeMap.put("bas","text/plain");
        extensionToMimeTypeMap.put("bcpio","application/x-bcpio");
        extensionToMimeTypeMap.put("bin","application/octet-stream");
        extensionToMimeTypeMap.put("bmp", "image/x-ms-bmp");
        extensionToMimeTypeMap.put("bmp","image/bmp");
        extensionToMimeTypeMap.put("c","text/plain");
        extensionToMimeTypeMap.put("cat","application/vnd.ms-pkiseccat");
        extensionToMimeTypeMap.put("cdf","application/x-cdf");
        extensionToMimeTypeMap.put("cdf","application/x-netcdf");
        extensionToMimeTypeMap.put("cer","application/x-x509-ca-cert");
        extensionToMimeTypeMap.put("class","application/octet-stream");
        extensionToMimeTypeMap.put("clp","application/x-msclip");
        extensionToMimeTypeMap.put("cmx","image/x-cmx");
        extensionToMimeTypeMap.put("cpio","application/x-cpio");
        extensionToMimeTypeMap.put("cpp","text/plain");
        extensionToMimeTypeMap.put("cr2","image/x-canon-cr2");
        extensionToMimeTypeMap.put("crd","application/x-mscardfile");
        extensionToMimeTypeMap.put("crl","application/pkix-crl");
        extensionToMimeTypeMap.put("crt","application/x-x509-ca-cert");
        extensionToMimeTypeMap.put("crw","image/x-canon-crw");
        extensionToMimeTypeMap.put("csh","application/x-csh");
        extensionToMimeTypeMap.put("css","text/css");
        extensionToMimeTypeMap.put("csv","text/plain");
        extensionToMimeTypeMap.put("dcr","application/x-director");
        extensionToMimeTypeMap.put("der","application/x-x509-ca-cert");
        extensionToMimeTypeMap.put("dir","application/x-director");
        extensionToMimeTypeMap.put("dll","application/x-msdownload");
        extensionToMimeTypeMap.put("dms","application/octet-stream");
        extensionToMimeTypeMap.put("dng","image/x-adobe-dng");
        extensionToMimeTypeMap.put("doc","application/msword");
        extensionToMimeTypeMap.put("docm","application/vnd.ms-word.document.macroEnabled.12");
        extensionToMimeTypeMap.put("docx","application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        extensionToMimeTypeMap.put("dot","application/msword");
        extensionToMimeTypeMap.put("dotm","application/vnd.ms-word.template.macroEnabled.12");
        extensionToMimeTypeMap.put("dotx","application/vnd.openxmlformats-officedocument.wordprocessingml.template");
        extensionToMimeTypeMap.put("dvi","application/x-dvi");
        extensionToMimeTypeMap.put("dxr","application/x-director");
        extensionToMimeTypeMap.put("emf","image/x-emf");
        extensionToMimeTypeMap.put("eml","message/rfc822");
        extensionToMimeTypeMap.put("eps","application/postscript");
        extensionToMimeTypeMap.put("erf","image/x-epson-erf");
        extensionToMimeTypeMap.put("etx","text/x-setext");
        extensionToMimeTypeMap.put("evy","application/envoy");
        extensionToMimeTypeMap.put("exe","application/octet-stream");
        extensionToMimeTypeMap.put("fif","application/fractals");
        extensionToMimeTypeMap.put("flac","audio/flac");
        extensionToMimeTypeMap.put("flr","x-world/x-vrml");
        extensionToMimeTypeMap.put("gif","image/gif");
        extensionToMimeTypeMap.put("gtar","application/x-gtar");
        extensionToMimeTypeMap.put("gz","application/x-gzip");
        extensionToMimeTypeMap.put("h","text/plain");
        extensionToMimeTypeMap.put("hdf","application/x-hdf");
        extensionToMimeTypeMap.put("hlp","application/winhlp");
        extensionToMimeTypeMap.put("hqx","application/mac-binhex40");
        extensionToMimeTypeMap.put("hta","application/hta");
        extensionToMimeTypeMap.put("htc","text/x-component");
        extensionToMimeTypeMap.put("htm","text/html");
        extensionToMimeTypeMap.put("html","text/html");
        extensionToMimeTypeMap.put("htt","text/webviewhtml");
        extensionToMimeTypeMap.put("ico","image/x-icon");
        extensionToMimeTypeMap.put("iii","application/x-iphone");
        extensionToMimeTypeMap.put("ins","application/x-internet-signup");
        extensionToMimeTypeMap.put("iso","application/x-iso9660-image");
        extensionToMimeTypeMap.put("isp","application/x-internet-signup");
        extensionToMimeTypeMap.put("jfif","image/pipeg");
        extensionToMimeTypeMap.put("jpe","image/jpeg");
        extensionToMimeTypeMap.put("jpeg","image/jpeg");
        extensionToMimeTypeMap.put("jpg","image/jpeg");
        extensionToMimeTypeMap.put("js","application/x-javascript");
        extensionToMimeTypeMap.put("latex","application/x-latex");
        extensionToMimeTypeMap.put("lha","application/octet-stream");
        extensionToMimeTypeMap.put("lsf","video/x-la-asf");
        extensionToMimeTypeMap.put("lsx","video/x-la-asf");
        extensionToMimeTypeMap.put("lzh","application/x-lzh");
        extensionToMimeTypeMap.put("m13","application/x-msmediaview");
        extensionToMimeTypeMap.put("m14","application/x-msmediaview");
        extensionToMimeTypeMap.put("m3u","audio/x-mpegurl");
        extensionToMimeTypeMap.put("m4a","audio/x-m4a");
        extensionToMimeTypeMap.put("m4p","audio/mp4a-latm");
        extensionToMimeTypeMap.put("man","application/x-troff-man");
        extensionToMimeTypeMap.put("mdb","application/x-msaccess");
        extensionToMimeTypeMap.put("me","application/x-troff-me");
        extensionToMimeTypeMap.put("mht","message/rfc822");
        extensionToMimeTypeMap.put("mhtml","message/rfc822");
        extensionToMimeTypeMap.put("mid","audio/mid");
        extensionToMimeTypeMap.put("mny","application/x-msmoney");
        extensionToMimeTypeMap.put("mov","video/quicktime");
        extensionToMimeTypeMap.put("movie","video/x-sgi-movie");
        extensionToMimeTypeMap.put("mp2","video/mpeg");
        extensionToMimeTypeMap.put("mp3","audio/mpeg");
        extensionToMimeTypeMap.put("mp4","video/mp4");
        extensionToMimeTypeMap.put("mpa","video/mpeg");
        extensionToMimeTypeMap.put("mpe","video/mpeg");
        extensionToMimeTypeMap.put("mpeg","video/mpeg");
        extensionToMimeTypeMap.put("mpg","video/mpeg");
        extensionToMimeTypeMap.put("mpp","application/vnd.ms-project");
        extensionToMimeTypeMap.put("mpv2","video/mpeg");
        extensionToMimeTypeMap.put("mrw","image/x-minolta-mrw");
        extensionToMimeTypeMap.put("ms","application/x-troff-ms");
        extensionToMimeTypeMap.put("msg","application/vnd.ms-outlook");
        extensionToMimeTypeMap.put("mvb","application/x-msmediaview");
        extensionToMimeTypeMap.put("nc","application/x-netcdf");
        extensionToMimeTypeMap.put("nef","image/x-nikon-nef");
        extensionToMimeTypeMap.put("nrw","image/x-nikon-nrw");
        extensionToMimeTypeMap.put("nws","message/rfc822");
        extensionToMimeTypeMap.put("oda","application/oda");
        extensionToMimeTypeMap.put("odb","application/vnd.oasis.opendocument.database");
        extensionToMimeTypeMap.put("odc","application/vnd.oasis.opendocument.chart");
        extensionToMimeTypeMap.put("odf","application/vnd.oasis.opendocument.formula");
        extensionToMimeTypeMap.put("odft","application/vnd.oasis.opendocument.formula-template");
        extensionToMimeTypeMap.put("odg","application/vnd.oasis.opendocument.graphics");
        extensionToMimeTypeMap.put("odi","application/vnd.oasis.opendocument.image");
        extensionToMimeTypeMap.put("odp","application/vnd.oasis.opendocument.presentation");
        extensionToMimeTypeMap.put("ods","application/vnd.oasis.opendocument.spreadsheet");
        extensionToMimeTypeMap.put("odt","application/vnd.oasis.opendocument.text");
        extensionToMimeTypeMap.put("oga","audio/ogg");
        extensionToMimeTypeMap.put("ogg","audio/ogg");
        extensionToMimeTypeMap.put("one","application/onenote");
        extensionToMimeTypeMap.put("onepkg","application/onenote");
        extensionToMimeTypeMap.put("onetmp","application/onenote");
        extensionToMimeTypeMap.put("onetoc","application/onenote");
        extensionToMimeTypeMap.put("onetoc2","application/onenote");
        extensionToMimeTypeMap.put("orf","image/x-olympus-orf");
        extensionToMimeTypeMap.put("otc","application/vnd.oasis.opendocument.chart-template");
        extensionToMimeTypeMap.put("otg","application/vnd.oasis.opendocument.graphics-template");
        extensionToMimeTypeMap.put("oth","application/vnd.oasis.opendocument.text-web");
        extensionToMimeTypeMap.put("oti","application/vnd.oasis.opendocument.image-template");
        extensionToMimeTypeMap.put("otm","application/vnd.oasis.opendocument.text-master");
        extensionToMimeTypeMap.put("otp","application/vnd.oasis.opendocument.presentation-template");
        extensionToMimeTypeMap.put("ots","application/vnd.oasis.opendocument.spreadsheet-template");
        extensionToMimeTypeMap.put("ott","application/vnd.oasis.opendocument.text-template");
        extensionToMimeTypeMap.put("p10","application/pkcs10");
        extensionToMimeTypeMap.put("p12","application/x-pkcs12");
        extensionToMimeTypeMap.put("p7b","application/x-pkcs7-certificates");
        extensionToMimeTypeMap.put("p7c","application/x-pkcs7-mime");
        extensionToMimeTypeMap.put("p7m","application/x-pkcs7-mime");
        extensionToMimeTypeMap.put("p7r","application/x-pkcs7-certreqresp");
        extensionToMimeTypeMap.put("p7s","application/x-pkcs7-signature");
        extensionToMimeTypeMap.put("pbm","image/x-portable-bitmap");
        extensionToMimeTypeMap.put("pdf","application/pdf");
        extensionToMimeTypeMap.put("pef","image/x-pentax-pef");
        extensionToMimeTypeMap.put("pfx","application/x-pkcs12");
        extensionToMimeTypeMap.put("pgm","image/x-portable-graymap");
        extensionToMimeTypeMap.put("pko","application/yndms-pkipko");
        extensionToMimeTypeMap.put("pma","application/x-perfmon");
        extensionToMimeTypeMap.put("pmc","application/x-perfmon");
        extensionToMimeTypeMap.put("pml","application/x-perfmon");
        extensionToMimeTypeMap.put("pmr","application/x-perfmon");
        extensionToMimeTypeMap.put("pmw","application/x-perfmon");
        extensionToMimeTypeMap.put("png","image/png");
        extensionToMimeTypeMap.put("pnm","image/x-portable-anymap");
        extensionToMimeTypeMap.put("pot","application/vnd.ms-powerpoint");
        extensionToMimeTypeMap.put("potm","application/vnd.ms-powerpoint.template.macroEnabled.12");
        extensionToMimeTypeMap.put("potx","application/vnd.openxmlformats-officedocument.presentationml.template");
        extensionToMimeTypeMap.put("ppam","application/vnd.ms-powerpoint.addin.macroEnabled.12");
        extensionToMimeTypeMap.put("ppm","image/x-portable-pixmap");
        extensionToMimeTypeMap.put("pps","application/vnd.ms-powerpoint");
        extensionToMimeTypeMap.put("ppsm","application/vnd.ms-powerpoint.slideshow.macroEnabled.12");
        extensionToMimeTypeMap.put("ppsx","application/vnd.openxmlformats-officedocument.presentationml.slideshow");
        extensionToMimeTypeMap.put("ppt","application/vnd.ms-powerpoint");
        extensionToMimeTypeMap.put("pptm","application/vnd.ms-powerpoint.presentation.macroEnabled.12");
        extensionToMimeTypeMap.put("pptx","application/vnd.openxmlformats-officedocument.presentationml.presentation");
        extensionToMimeTypeMap.put("prf","application/pics-rules");
        extensionToMimeTypeMap.put("ps","application/postscript");
        extensionToMimeTypeMap.put("pub","application/x-mspublisher");
        extensionToMimeTypeMap.put("qt","video/quicktime");
        extensionToMimeTypeMap.put("ra","audio/x-pn-realaudio");
        extensionToMimeTypeMap.put("raf","image/x-fuji-raf");
        extensionToMimeTypeMap.put("ram","audio/x-pn-realaudio");
        extensionToMimeTypeMap.put("rar","application/x-rar-compressed");
        extensionToMimeTypeMap.put("ras","image/x-cmu-raster");
        extensionToMimeTypeMap.put("raw","image/x-panasonic-raw");
        extensionToMimeTypeMap.put("rgb","image/x-rgb");
        extensionToMimeTypeMap.put("rm","application/vnd.rn-realmedia");
        extensionToMimeTypeMap.put("rmi","audio/mid");
        extensionToMimeTypeMap.put("roff","application/x-troff");
        extensionToMimeTypeMap.put("rtf","application/rtf");
        extensionToMimeTypeMap.put("rtx","text/richtext");
        extensionToMimeTypeMap.put("rw2","image/x-panasonic-rw2");
        extensionToMimeTypeMap.put("scd","application/x-msschedule");
        extensionToMimeTypeMap.put("sct","text/scriptlet");
        extensionToMimeTypeMap.put("setpay","application/set-payment-initiation");
        extensionToMimeTypeMap.put("setreg","application/set-registration-initiation");
        extensionToMimeTypeMap.put("sh","application/x-sh");
        extensionToMimeTypeMap.put("shar","application/x-shar");
        extensionToMimeTypeMap.put("sit","application/x-stuffit");
        extensionToMimeTypeMap.put("sldm","application/vnd.ms-powerpoint.slide.macroEnabled.12");
        extensionToMimeTypeMap.put("sldx","application/vnd.openxmlformats-officedocument.presentationml.slide");
        extensionToMimeTypeMap.put("snd","audio/basic");
        extensionToMimeTypeMap.put("spc","application/x-pkcs7-certificates");
        extensionToMimeTypeMap.put("spl","application/futuresplash");
        extensionToMimeTypeMap.put("sr2","image/x-sony-sr2");
        extensionToMimeTypeMap.put("src","application/x-wais-source");
        extensionToMimeTypeMap.put("sst","application/vnd.ms-pkicertstore");
        extensionToMimeTypeMap.put("stl","application/vnd.ms-pkistl");
        extensionToMimeTypeMap.put("stm","text/html");
        extensionToMimeTypeMap.put("sv4cpio","application/x-sv4cpio");
        extensionToMimeTypeMap.put("sv4crc","application/x-sv4crc");
        extensionToMimeTypeMap.put("t","application/x-troff");
        extensionToMimeTypeMap.put("tar","application/x-tar");
        extensionToMimeTypeMap.put("tcl","application/x-tcl");
        extensionToMimeTypeMap.put("tex","application/x-tex");
        extensionToMimeTypeMap.put("texi","application/x-texinfo");
        extensionToMimeTypeMap.put("texinfo","application/x-texinfo");
        extensionToMimeTypeMap.put("tgz","application/x-compressed");
        extensionToMimeTypeMap.put("thmx","application/vnd.ms-officetheme");
        extensionToMimeTypeMap.put("tif","image/tiff");
        extensionToMimeTypeMap.put("tiff","image/tiff");
        extensionToMimeTypeMap.put("tr","application/x-troff");
        extensionToMimeTypeMap.put("trm","application/x-msterminal");
        extensionToMimeTypeMap.put("tsv","text/tab-separated-values");
        extensionToMimeTypeMap.put("txt","text/plain");
        extensionToMimeTypeMap.put("uls","text/iuls");
        extensionToMimeTypeMap.put("ustar","application/x-ustar");
        extensionToMimeTypeMap.put("vcf","text/x-vcard");
        extensionToMimeTypeMap.put("vrml","x-world/x-vrml");
        extensionToMimeTypeMap.put("wav","audio/x-wav");
        extensionToMimeTypeMap.put("wcm","application/vnd.ms-works");
        extensionToMimeTypeMap.put("wdb","application/vnd.ms-works");
        extensionToMimeTypeMap.put("wks","application/vnd.ms-works");
        extensionToMimeTypeMap.put("wma","audio/x-ms-wma");
        extensionToMimeTypeMap.put("wmf","application/x-msmetafile");
        extensionToMimeTypeMap.put("wps","application/vnd.ms-works");
        extensionToMimeTypeMap.put("wrl","x-world/x-vrml");
        extensionToMimeTypeMap.put("wrz","x-world/x-vrml");
        extensionToMimeTypeMap.put("x3f","image/x-sigma-x3f");
        extensionToMimeTypeMap.put("xaf","x-world/x-vrml");
        extensionToMimeTypeMap.put("xbm","image/x-xbitmap");
        extensionToMimeTypeMap.put("xla","application/vnd.ms-excel");
        extensionToMimeTypeMap.put("xlam","application/vnd.ms-excel.addin.macroEnabled.12");
        extensionToMimeTypeMap.put("xlc","application/vnd.ms-excel");
        extensionToMimeTypeMap.put("xlm","application/vnd.ms-excel");
        extensionToMimeTypeMap.put("xls","application/vnd.ms-excel");
        extensionToMimeTypeMap.put("xlsb","application/vnd.ms-excel.sheet.binary.macroEnabled.12");
        extensionToMimeTypeMap.put("xlsm","application/vnd.ms-excel.sheet.macroEnabled.12");
        extensionToMimeTypeMap.put("xlsx","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        extensionToMimeTypeMap.put("xlt","application/vnd.ms-excel");
        extensionToMimeTypeMap.put("xltm","application/vnd.ms-excel.template.macroEnabled.12");
        extensionToMimeTypeMap.put("xltx","application/vnd.openxmlformats-officedocument.spreadsheetml.template");
        extensionToMimeTypeMap.put("xlw","application/vnd.ms-excel");
        extensionToMimeTypeMap.put("xml","text/xml");
        extensionToMimeTypeMap.put("xof","x-world/x-vrml");
        extensionToMimeTypeMap.put("xpm","image/x-xpixmap");
        extensionToMimeTypeMap.put("xwd","image/x-xwindowdump");
        extensionToMimeTypeMap.put("z","application/x-compress");
        extensionToMimeTypeMap.put("zip","application/zip");
        extensionToMimeTypeMap.put("flv","video/x-flv");
        extensionToMimeTypeMap.put("wmv","video/x-ms-wmv");
        
    }

     public static String getMimeTypeFromFileName(String strFileName) 
    {
        String strFileExtension = strFileName.substring(strFileName.lastIndexOf('.')+1).toLowerCase(Locale.getDefault());
        String strContentType = MimeTypeMap.getSingleton().getExtensionFromMimeType(strFileExtension);
        if(strContentType == null)
        {
            strContentType = extensionToMimeTypeMap.get(strFileExtension);
        }
        return strContentType;
    }
     
     public static int getCategory(String mimeType)
     {
         if (mimeType != null)
         {
             Integer type = mimeTypeToCategoryMap.get(mimeType);
         
             if (type != null)
             {
                 return type.intValue();
             }
             else
             {
                 return CATEGORY_UNKNOWN;
             }
         }
         else
         {
            return CATEGORY_UNKNOWN;             
         }
     }
     
     
     public static String getStoragePathForMozy()
     {
         String path = null;
         final String state = Environment.getExternalStorageState();
         
         if (Environment.MEDIA_MOUNTED.equals(state)) {
         
               File file = Environment.getExternalStorageDirectory();
              
               path = file.getPath();
               
               // Mozy Path
               
               path = path + "/Mozy";
         }
         return path;
     }
     
     public static String getDownloadDirectoryPath(Context context, CloudFile cloudFile , String containerTitle)
     {
         String path = null;
          
         path = FileUtils.getStoragePathForMozy();
         
         if(path != null)
         {
               if (cloudFile instanceof Photo || cloudFile instanceof Video)
               {
                   path = path  +"/Photos and Videos" + "/" + containerTitle;
               }
               else if (cloudFile instanceof Music)
               {
                  path = path  + "/Music"+ "/" + containerTitle;
               }
               else 
               {
                  path = path  + "/Docs" + "/" +  containerTitle;
               }
         }
         return path;
     }

     public static final String getLocalFilePathForCloudFile(Context context, String deviceTitle, final CloudFile cloudFile) {
        
        String localFilePath = null;
        
        if(cloudFile != null)
        {
             String downloadPath = FileUtils.getDownloadDirectoryPath(context, cloudFile, deviceTitle);
             
             if(downloadPath  != null)
             {
                 localFilePath = downloadPath  + "/" + cloudFile.getTitle();
             }
        }
         return localFilePath;
     }
     
     /**
      * @param folderName
      * @return
      */
     public static String getFolderNameFromPath(String folderName) {
         final char slash = '/';
         folderName = folderName.substring(0, folderName.length() -1);
         int index = folderName.lastIndexOf(slash);
         
         if(index > 0)
         {
                 folderName = folderName.substring(index+1, folderName.length());
         }
         return folderName;
     }
     
     
     public static Drawable getIconForUploadFile(Context context, CloudFile cloudFile)
     {
         Drawable returnValue = null;
         int iconId = 0;
         
         if(cloudFile != null)
         {
             // Key to the hashtable will be the mime_type with the extension tacked on.
             int extensionStart = cloudFile.getName().lastIndexOf(".");
    
             String extension = cloudFile.getName().substring(extensionStart + 1);
             
             String mimeType = FileUtils.getMimeTypeFromFileName(cloudFile.getName());
    
             String hashKey = mimeType + "_" + extension;
    
             returnValue = sFileTypeIcons.get(hashKey);
    
             // If there is no hash value, then create one.
             if (returnValue == null)
             {
                 int fileCategory = FileUtils.getCategory(mimeType);

                 iconId = getIconIDForNonEncryptedFiles(fileCategory);
                    
                 try
                 {
                     returnValue = context.getResources().getDrawable(iconId);
                 }
                 catch(NotFoundException e)
                 {
                     iconId = R.drawable.file_blank;
                     returnValue = context.getResources().getDrawable(iconId);
                 }
             }
         }

         return returnValue;
     }
     
     
     // Figure out the proper icon for a cloudFile type. This is dependent on what applications the user has installed
     // on their phone.
     public static Drawable getIcon(Context context, LocalFile downloadedFile, String deviceId, boolean deviceEncrypted)
     {
         Drawable returnValue = null;
         int iconId = 0;
         
         if(downloadedFile != null)
         {
             // Key to the hashtable will be the mime_type with the extension tacked on.
             int extensionStart = downloadedFile.getName().lastIndexOf(".");
    
             String extension = downloadedFile.getName().substring(extensionStart + 1);
             
             String mimeType = FileUtils.getMimeTypeFromFileName(downloadedFile.getName());
    
             String hashKey = mimeType + "_" + extension;
    
             returnValue = sFileTypeIcons.get(hashKey);
    
             // If there is no hash value, then create one.
             if (returnValue == null)
             {
                 // Check if there is any viewer for this file type.
                 Intent intent = new Intent(Intent.ACTION_VIEW);
    
                 PackageManager manager = context.getPackageManager();
                 List<ResolveInfo> apps = manager.queryIntentActivities(intent, 0);
    
                 // If there is one application that views this file type, we will use its icon. If there are
                 // zero or more than one, we will generate the icon.
                 if (apps.size() == 1)
                 {
                     returnValue = apps.get(0).loadIcon(manager);
                 }
                 else
                 {
                     int fileCategory = FileUtils.getCategory(mimeType);
                     if(deviceEncrypted == false  || SystemState.isDownloadedFileDecrypted(downloadedFile,deviceId) == true
                             || (SystemState.isManagedKeyEnabled(context)))
                     {
                         iconId = getIconIDForNonEncryptedFiles(fileCategory);
                     }
                     else
                     {
                         iconId = getIconIdForEncryptedFiles(fileCategory);
                     }
                 }
                 try
                 {
                     returnValue = context.getResources().getDrawable(iconId);
                 }
                 catch(NotFoundException e)
                 {
                     iconId = R.drawable.file_blank;
                     returnValue = context.getResources().getDrawable(iconId);
                 }
             }
         }

         return returnValue;
     }
     
     
     // Figure out the proper icon for a cloudFile type. This is dependent on what applications the user has installed
     // on their phone.
     public static Drawable getIcon(Context context, CloudFile cloudFile, String deviceTitle, String deviceId, boolean deviceEncrypted)
     {
         Drawable returnValue = null;
         boolean isFileDecrypted = false;
         String filePath = null;

         if(cloudFile != null)
         {
             if (cloudFile instanceof Directory)
             {
                 return context.getResources().getDrawable(R.drawable.folder);
             }
    
             // Key to the hashtable will be the mime_type with the extension tacked on.
             int extensionStart = cloudFile.getTitle().lastIndexOf(".");
    
             String extension = cloudFile.getTitle().substring(extensionStart + 1);
    
             String hashKey = cloudFile.getMimeType() + "_" + extension;
             
             final LocalFile localFile  = FileUtils.getLocalFileForCloudFile(context, deviceTitle, cloudFile);
             
             if(localFile  != null && deviceEncrypted == true)
             {
                 // check for entry of local decrypted file on disk
                 isFileDecrypted = SystemState.isLocalFileDecrypted(localFile, cloudFile, deviceId);
             }
             
             if (deviceEncrypted == false || isFileDecrypted == true) 
                 returnValue = sFileTypeIcons.get(hashKey);
    
             // If there is no hash value, then create one.
             if (returnValue == null)
             {
                 // Check if there is any viewer for this file type.
                 Intent intent = new Intent(Intent.ACTION_VIEW);
                 
                 if(localFile != null)
                     filePath = localFile.getPath();
                 
                 if(filePath != null && cloudFile != null)
                 {
                     Uri uri = Uri.parse(filePath);
                     intent.setDataAndType(uri, cloudFile.getMimeType());
                 }
    
                 PackageManager manager = context.getPackageManager();
                 List<ResolveInfo> apps = manager.queryIntentActivities(intent, 0);
    
                 // If there is one application that views this file type, we will use its icon. If there are
                 // zero or more than one, we will generate the icon.
                 if (apps.size() == 1 && ( deviceEncrypted == false || isFileDecrypted == true))
                 {
                     returnValue = apps.get(0).loadIcon(manager);
                 }
                 else
                 {
                     int fileCategory = cloudFile.getCategory();
                     int iconId = 0;
                     
                     if(deviceEncrypted == false  || isFileDecrypted == true
                             || (SystemState.isManagedKeyEnabled(context)))
                     {
                         iconId = getIconIDForNonEncryptedFiles(fileCategory);
                     }
                     else
                     {
                         iconId = getIconIdForEncryptedFiles(fileCategory);
                     }
                     try
                     {
                         returnValue = context.getResources().getDrawable(iconId);
                     }
                     catch(NotFoundException e)
                     {
                         iconId = R.drawable.file_blank;
                         returnValue = context.getResources().getDrawable(iconId);
                     }
                 }
                 
                 if (deviceEncrypted == false || isFileDecrypted == true)
                 {
                     if(sFileTypeIcons.containsKey(hashKey) == false)
                     {
                         sFileTypeIcons.put(hashKey, returnValue);
                     }
                 }
            }
        }

         return returnValue;
     }

    /**
     * @param fileCategory
     * @return
     */
    public static int getIconIdForEncryptedFiles(int fileCategory) {
        int iconId;
        switch (fileCategory)
         {
             case FileUtils.CATEGORY_PHOTOS:
                   iconId = R.drawable.file_img_locked;
                   break;
             case FileUtils.CATEGORY_MUSIC:
                   iconId = R.drawable.file_music_locked;
                   break;
             case FileUtils.CATEGORY_VIDEOS:
                   iconId = R.drawable.file_video_locked;
                   break;
             case FileUtils.CATEGORY_MSEXCEL:
                   iconId = R.drawable.file_xls_locked;
                   break;
             case FileUtils.CATEGORY_MSPOWERPOINT:
                   iconId = R.drawable.file_ppt_locked;
                   break;
             case FileUtils.CATEGORY_MSWORD:
                   iconId = R.drawable.file_word_locked;
                   break;
             case FileUtils.CATEGORY_PDF:
                   iconId = R.drawable.file_pdf_locked;
                   break;
             default:
                    iconId = R.drawable.file_blank_locked;
                    break;
         }
        return iconId;
    }

    /**
     * @param fileCategory
     * @return
     */
    public static int getIconIDForNonEncryptedFiles(int fileCategory) {
        int iconId;
        switch (fileCategory)
         {
             case FileUtils.CATEGORY_PHOTOS:
                   iconId = R.drawable.file_img;
                   break;
             case FileUtils.CATEGORY_MUSIC:
                   iconId = R.drawable.file_music;
                   break;
             case FileUtils.CATEGORY_VIDEOS:
                   iconId = R.drawable.file_video;
                   break;
             case FileUtils.CATEGORY_MSEXCEL:
                   iconId = R.drawable.file_xls;
                   break;
             case FileUtils.CATEGORY_MSPOWERPOINT:
                   iconId = R.drawable.file_ppt;
                   break;
             case FileUtils.CATEGORY_MSWORD:
                   iconId = R.drawable.file_word;
                   break;
             case FileUtils.CATEGORY_PDF:
                   iconId = R.drawable.file_pdf;
                   break;
             default:
                    iconId = R.drawable.file_blank;
                    break;
         }
        return iconId;
    }

     
    
    /**
     * @param fileCategory
     * @return
     */
    public static int getUploadIconIDForNonEncryptedFiles(int fileCategory) {
        int iconId;
        switch (fileCategory)
         {
             case FileUtils.CATEGORY_PHOTOS:
                   iconId = R.drawable.image_icon_large;
                   break;
             case FileUtils.CATEGORY_MUSIC:
                   iconId = R.drawable.home_music;
                   break;
             case FileUtils.CATEGORY_VIDEOS:
                   iconId = R.drawable.home_videos;
                   break;
             case FileUtils.CATEGORY_MSEXCEL:
             case FileUtils.CATEGORY_MSPOWERPOINT:
             case FileUtils.CATEGORY_MSWORD:
             case FileUtils.CATEGORY_PDF:
                   iconId = R.drawable.home_docs;
                   break;
             default:
                    iconId = R.drawable.anim_sync1;
                    break;
         }
        return iconId;
    }
     /*
      * Generate the text for the details line of a file entry in the listview.
      */
     public static String getDetails(Context context, long lastModified, double fileSize)
     {
             String s = context.getResources().getString(R.string.file_list_detail);

             s = s.replace("$DATE", FileUtils.getLastModified(context, lastModified));

             // Last updated
             //SimpleDateFormat date = new SimpleDateFormat("MM/FF/yy");
             //s = s.replace("$DATE", date.format(cloudFile.getUpdated()));

             final int decr = 1024;
             int step = 0;
             //R.array.file_sizes_array is {"bytes", "KB", "MB", "GB", "TB", "PB"}
             String[] postFix = context.getResources().getStringArray(R.array.file_sizes_array);
             while((fileSize / decr) > 0.9)
             {
                 fileSize = fileSize / decr;
                 step++;
             }
             String sizeString = String.format(Locale.getDefault(), step == 0 ? "%.1f %s" : "%.1f%s", fileSize, postFix[step]);
             s = s.replace("$SIZE", sizeString);

             return s;
     }

    
     
     
     public static String getLastModified(Context context, long lastModified)
     {
         StringBuffer s = new StringBuffer();

         Calendar cal = Calendar.getInstance();
         Date parsed = new Date(lastModified);
         Date now = new Date();
         long endL = now.getTime();
         long startL = parsed.getTime() + (cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET));
         long deltaMS = endL-startL;
         String units;
         long delta = 0;
         Locale loc = Locale.getDefault();
         String lang = loc.getLanguage();

         if (deltaMS < (MINUTE)) // if less than 1 minute
         {
             delta = deltaMS/SECOND;
             units = delta > 1 ? context.getResources().getString(R.string.seconds) : context.getResources().getString(R.string.second);
         }
         else if (deltaMS < (HOUR)) // if less then 1 hour
         {
             delta = deltaMS/(MINUTE);
             units = delta > 1 ? context.getResources().getString(R.string.minutes) : context.getResources().getString(R.string.minute);
         }
         else if (deltaMS < (DAY)) // less than one day
         {
             delta = deltaMS/(HOUR);
             units = delta > 1 ? context.getResources().getString(R.string.hours) : context.getResources().getString(R.string.hour);
         }
         else  if (deltaMS < (WEEK)) // less than one week
         {
             delta = deltaMS/(DAY);
             units = delta > 1 ? context.getResources().getString(R.string.days) : context.getResources().getString(R.string.day);
         }
         else  if (deltaMS < (MONTH)) // less than one month
         {
             delta = deltaMS/(WEEK);
             units = delta > 1 ? context.getResources().getString(R.string.weeks) : context.getResources().getString(R.string.week);
         }
         else  if (deltaMS < (YEAR)) // less than one year
         {
             delta = deltaMS/(MONTH);
             units = delta > 1 ? context.getResources().getString(R.string.months) : context.getResources().getString(R.string.month);
         }
         else // more than a year
         {
             delta = deltaMS/(YEAR);
             units = delta > 1 ? context.getResources().getString(R.string.years) : context.getResources().getString(R.string.year);
         }

         if (lang.equals("de"))
         {
             s.append(context.getResources().getString(R.string.ago));
             s.append(" " + delta);
             s.append(" " + units);
         }
         else
         {
             s.append(delta);
             s.append(" " + units);
             s.append(" " + context.getResources().getString(R.string.ago));
         }

         return (s.toString());
     }
     
     
     /**
      * @param cloudFile
      * @return
      */
     public static final LocalFile getLocalFileForCloudFile(Context context, String deviceTitle, final CloudFile cloudFile) {
         LocalFile localFile = null;
         
         String filePath = getLocalFilePathForCloudFile(context, deviceTitle, cloudFile);
         
         if(filePath != null)
         {
                 localFile  = new LocalFile(filePath);
         }
         return localFile;
     }
     
     public static boolean isFileofType(LocalFile localFile, int category)
     {
         
         if(localFile != null)
         {
             String mimeType = FileUtils.getMimeTypeFromFileName(localFile.getName());
             return (category == FileUtils.getCategory(mimeType));
         }
         else
            return false;  
         
         
     }  
     /**
      * @param file
      * @return
      */
     public static boolean isFilePhoto(LocalFile file) {
         return isFileofType(file, FileUtils.CATEGORY_PHOTOS);
     }
     
     
     public static byte[] readBytesFromUrl(String urlStr)
     {
         
         ArrayList<Byte> al = new ArrayList<Byte>(); 
         byte[] data = null;   
         try
         {      
             URL url = new URL(urlStr);
             HttpURLConnection con = (HttpURLConnection) url.openConnection();
             
             StringBuilder strbld = new StringBuilder();      
             
             BufferedInputStream bufferedInput = null;
             byte[] buffer = new byte[1024];
             
             try {
                 
                 //Construct the BufferedInputStream object
            	 //con.getInputStream();
                 bufferedInput = new BufferedInputStream(con.getInputStream());
                 
                 int bytesRead = 0;
                 
                 //Keep reading from the file while there is any content
                 //when the end of the stream has been reached, -1 is returned
                 while ((bytesRead = bufferedInput.read(buffer)) != -1) {
                         //Array has been filled up to length
                         for (int i = 0; i < bytesRead; i++) 
                             al.add(buffer[i]);
                 }
                 
             } catch (IOException ex) {
                 ex.printStackTrace();
             } finally {
                 //Close the BufferedInputStream
                 try {
                     if (bufferedInput != null)
                         bufferedInput.close();
                 } catch (IOException ex) {
                     ex.printStackTrace();
                 }
                 con.disconnect();
             }
             
             data = new byte[al.size()];
             
             for(int i = 0; i < al.size(); i++)
                 data[i] = al.get(i).byteValue();
             
             LogUtil.debug("FileUtils", "readBytesFromUrl():" + strbld.toString());
           }
           catch(MalformedURLException ex)
           {
               LogUtil.debug("FileUtils", "readBytesFromUrl() exception" + ex); 
           }
           catch (Exception ex)
           {
               LogUtil.debug("FileUtils", "readBytesFromUrl() exception" + ex);  
           }
                    
           return data;
     }
     
     public static final LocalFile getDecryptedFileForLocalFile(Context context, final LocalFile localFile)
     {
    	 File outputDir = new File(localFile.file.getParent() + "/" + FileUtils.decryptHiddenDir);
    	 LocalFile decryptedFile = new LocalFile(outputDir.getAbsolutePath() + "/" + localFile.file.getName());
    	 if (decryptedFile.file.exists())
    		 return decryptedFile;
    	 return null;
     }

	public static LocalFile createTempUploadFile(LocalFile src) throws Exception
	{
		File outputDir = new File(src.file.getParent() + "/" + FileUtils.uploadHiddenDir);
		outputDir.mkdirs();
		
		LocalFile tmpFile = new LocalFile(outputDir.getAbsolutePath() + "/" + src.file.getName());
		tmpFile.createNewFile();
		copyFile(src.file, tmpFile.file, true);
		return tmpFile;
	}
	public static void copyFile(File fromFile, File toFile,Boolean rewrite ) throws Exception
	{
		if (!fromFile.exists()) {
			return;
		}
		if (!fromFile.isFile()) {
			return ;
		}
		if (!fromFile.canRead()) {
			return ;
		}
		if (!toFile.getParentFile().exists()) {
			toFile.getParentFile().mkdirs();
		}
		if (toFile.exists() && rewrite) {
			toFile.delete();
		}
		
		try {
			java.io.FileInputStream fosfrom = new java.io.FileInputStream(fromFile);
			java.io.FileOutputStream fosto = new java.io.FileOutputStream(toFile);
			byte bt[] = new byte[1024];
			int c;
			while ((c = fosfrom.read(bt)) > 0) {
				fosto.write(bt, 0, c);
			}
			fosfrom.close();
			fosto.close();
		} catch (Exception ex) {
			throw ex;
		}
	}
}
