package ru.prodvigaeff.control.http;

import java.util.HashMap;
import java.util.Map;

public class HttpRequest
{
    private String url;
    private String method;
    private Map<String, String> headers = new HashMap<>();
    private String body;
    
    public HttpRequest(String url, String method)
    {
        this.url = url;
        this.method = method;
    }
    
    public String getUrl() { return url; }
    public String getMethod() { return method; }
    public Map<String, String> getHeaders() { return headers; }
    public String getBody() { return body; }
    
    public HttpRequest addHeader(String key, String value)
    {
        headers.put(key, value);
        return this;
    }
    
    public HttpRequest setBody(String body)
    {
        this.body = body;
        return this;
    }
}