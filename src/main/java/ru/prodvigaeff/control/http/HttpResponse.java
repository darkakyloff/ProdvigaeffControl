package ru.prodvigaeff.control.http;

public class HttpResponse
{
    private final int statusCode;
    private final String body;
    
    public HttpResponse(int statusCode, String body)
    {
        this.statusCode = statusCode;
        this.body = body;
    }
    
    public int getStatusCode() { return statusCode; }
    public String getBody() { return body; }
    
    public boolean isSuccess()
    {
        return statusCode >= 200 && statusCode < 300;
    }
}