package se.tactel.datahandler;

import org.apache.http.Header;

public class DeleteRequest extends DataRequest 
{
    public DeleteRequest(String uri, Header[] headers) 
    {
        super(uri, headers);
        setMethod(METHOD_DELETE);
    }    
}
