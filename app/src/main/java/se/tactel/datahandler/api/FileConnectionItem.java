package se.tactel.datahandler.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.Header;
import org.apache.http.StatusLine;
import org.apache.http.message.BasicHeader;

import se.tactel.datahandler.DataManager.Params;

public class FileConnectionItem implements ConnectionItem {
    
    private File file;
    private InputStream inputStream;
    private boolean closed;
    private String mime_type;
    
    public FileConnectionItem(File file, String mime_type) {
        this.file = file;
        this.closed = false;
        if (mime_type == null) {
            this.mime_type = "";
        } else {
            this.mime_type = mime_type;
        }
        try {
            this.inputStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            this.inputStream = null;
            e.printStackTrace();
        }
    }

    @Override
    public synchronized void abort() {
        closed = true;
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            inputStream = null;
        }
    }

    @Override
    public Header[] getAllHeaders() {
        Header[] headers = new Header[1];
        headers[0] = new BasicHeader("Content-Type", mime_type);
        return headers;
    }

    @Override
    public long getContentLength() {
        return file.length();
    }

    @Override
    public int read(Params params) throws IOException {
        if (closed) {
            throw new IOException("Connection item closed");
        } else if (inputStream == null) {
            throw new IOException("File Not Found");
        }
        return inputStream.read();
    }

    @Override
    public int read(byte[] b, Params params) throws IOException {
        if (closed) {
            throw new IOException("Connection item closed");
        } else if (inputStream == null) {
            throw new IOException("File Not Found");
        }
        return inputStream.read(b);
    }
    
    @Override
    public int read(byte[] b, int off, int len, Params params) throws IOException {
        if (closed) {
            throw new IOException("Connection item closed");
        } else if (inputStream == null) {
            throw new IOException("File Not Found");
        }
        return inputStream.read(b, off, len);
    }

    @Override
    public StatusLine getHttpStatus() {
        return null;
    }
}
