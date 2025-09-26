package ru.prodvigaeff.control.http;

public class HttpBuilder
{
    private HttpRequest request;
    
    public static HttpBuilder get(String url)
    {
        HttpBuilder builder = new HttpBuilder();
        builder.request = new HttpRequest(url, "GET");
        return builder;
    }
    
    public static HttpBuilder post(String url)
    {
        HttpBuilder builder = new HttpBuilder();
        builder.request = new HttpRequest(url, "POST");
        return builder;
    }
    
    public HttpBuilder header(String key, String value)
    {
        request.addHeader(key, value);
        return this;
    }
    
    public HttpBuilder body(String body)
    {
        request.setBody(body);
        return this;
    }
    
    public HttpBuilder json(Object obj)
    {
        request.addHeader("Content-Type", "application/json");
        String jsonBody = ru.prodvigaeff.control.utils.JsonUtil.toJson(obj);
        if (jsonBody != null) request.setBody(jsonBody);
        return this;
    }
    
    public HttpBuilder auth(String token)
    {
        request.addHeader("Authorization", "Bearer " + token);
        return this;
    }
    
    public HttpResponse execute()
    {
        return HttpUtil.execute(request);
    }
}