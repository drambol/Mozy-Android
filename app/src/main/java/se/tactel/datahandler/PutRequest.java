package se.tactel.datahandler;

import org.apache.http.Header;
import org.apache.http.HttpEntity;

public class PutRequest extends DataRequest
{
    public PutRequest(String uri, Header[] headers)
    {
        super(uri, headers);
        setMethod(METHOD_PUT);
    }

    public PutRequest(String uri, Header[] headers, HttpEntity entity)
    {
        super(uri, headers, entity);
        setMethod(METHOD_PUT);
    }
}
