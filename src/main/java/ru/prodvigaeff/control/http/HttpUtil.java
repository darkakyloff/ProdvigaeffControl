package ru.prodvigaeff.control.http;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import ru.prodvigaeff.control.utils.EnvUtil;
import ru.prodvigaeff.control.utils.Logger;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

public class HttpUtil
{
    private static final CloseableHttpClient client = HttpClients.createDefault();
    private static final int DEFAULT_MAX_RETRIES = EnvUtil.getInt("HTTP_MAX_RETRIES", 3);
    private static final int DEFAULT_RETRY_DELAY_MS = EnvUtil.getInt("HTTP_RETRY_DELAY_MS", 1000);

    public static HttpResponse execute(HttpRequest request)
    {
        return executeWithRetry(request, DEFAULT_MAX_RETRIES);
    }

    public static HttpResponse executeWithRetry(HttpRequest request, int maxRetries)
    {
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++)
        {
            try
            {
                Logger.debugRequest(request.getMethod(), request.getUrl(), request.getBody());

                HttpUriRequestBase httpRequest = createHttpRequest(request);
                request.getHeaders().forEach(httpRequest::addHeader);

                if (request.getBody() != null && httpRequest instanceof HttpPost)
                {
                    StringEntity entity = new StringEntity(request.getBody(), StandardCharsets.UTF_8);
                    ((HttpPost) httpRequest).setEntity(entity);
                }

                HttpResponse response = client.execute(httpRequest, httpResponse -> {
                    int statusCode = httpResponse.getCode();
                    String body = EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
                    return new HttpResponse(statusCode, body);
                });

                Logger.debugResponse(response.getStatusCode(), response.getBody());

                if (shouldRetry(response.getStatusCode(), attempt, maxRetries))
                {
                    Logger.warn("HTTP " + response.getStatusCode() + " для " + request.getUrl() +
                            ". Повтор " + attempt + "/" + maxRetries);
                    sleepBeforeRetry(attempt);
                    continue;
                }

                return response;
            }
            catch (UnknownHostException e)
            {
                lastException = e;
                Logger.error("Хост недоступен: " + request.getUrl() + ". Попытка " + attempt + "/" + maxRetries);
            }
            catch (SocketTimeoutException e)
            {
                lastException = e;
                Logger.error("Таймаут соединения для " + request.getUrl() + ". Попытка " + attempt + "/" + maxRetries);
            }
            catch (Exception e)
            {
                lastException = e;
                Logger.error("HTTP ошибка для " + request.getUrl() + ": " + e.getMessage() +
                        ". Попытка " + attempt + "/" + maxRetries);
            }

            if (attempt < maxRetries)
            {
                sleepBeforeRetry(attempt);
            }
        }

        Logger.error("HTTP запрос не выполнен после " + maxRetries + " попыток: " + request.getUrl());

        if (lastException != null)
        {
            throw new HttpRequestException("Не удалось выполнить HTTP запрос после " + maxRetries + " попыток", lastException);
        }

        return new HttpResponse(500, "Ошибка подключения после " + maxRetries + " попыток");
    }

    private static boolean shouldRetry(int statusCode, int attempt, int maxRetries)
    {
        if (attempt >= maxRetries) return false;

        return statusCode >= 500 || statusCode == 429 || statusCode == 408 || statusCode == 502 || statusCode == 503 || statusCode == 504;
    }

    private static void sleepBeforeRetry(int attempt)
    {
        try
        {
            int delay = DEFAULT_RETRY_DELAY_MS * (int) Math.pow(2, attempt - 1);
            Thread.sleep(delay);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            Logger.warn("Прервана задержка перед повтором HTTP запроса");
        }
    }

    private static HttpUriRequestBase createHttpRequest(HttpRequest request)
    {
        return switch (request.getMethod()) {
            case "GET" -> new HttpGet(request.getUrl());
            case "POST" -> new HttpPost(request.getUrl());
            default -> throw new IllegalArgumentException("Неподдерживаемый HTTP метод: " + request.getMethod());
        };
    }

    public static class HttpRequestException extends RuntimeException
    {
        public HttpRequestException(String message, Throwable cause)
        {
            super(message, cause);
        }
    }
}