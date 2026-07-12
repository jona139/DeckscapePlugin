package com.deckscape.runelite;

import com.deckscape.runelite.model.PackType;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Slf4j
@Singleton
public final class DeckscapeSyncClient
{
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient httpClient;
    private final Gson gson = new Gson();

    @Inject
    public DeckscapeSyncClient(OkHttpClient httpClient)
    {
        this.httpClient = httpClient;
    }

    public CompletableFuture<JsonObject> request(String apiUrl, String apiKey, String deviceToken, String action, JsonObject payload)
    {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        if (apiUrl == null || apiUrl.isEmpty() || apiKey == null || apiKey.isEmpty() || deviceToken == null || deviceToken.isEmpty())
        {
            future.completeExceptionally(new Exception("Sync is not configured or not linked."));
            return future;
        }

        String url = apiUrl;
        if (!url.endsWith("/")) url += "/";
        url += "functions/v1/economy";

        JsonObject bodyJson = payload != null ? payload : new JsonObject();
        bodyJson.addProperty("action", action);

        RequestBody body = RequestBody.create(JSON, gson.toJson(bodyJson));
        Request request = new Request.Builder()
            .url(url)
            .post(body)
            .addHeader("apikey", apiKey)
            .addHeader("Authorization", "Bearer " + apiKey)
            .addHeader("X-Deckscape-Device-Token", deviceToken)
            .build();

        httpClient.newCall(request).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException
            {
                try (Response r = response)
                {
                    if (!r.isSuccessful())
                    {
                        String err = r.body() != null ? r.body().string() : "Request failed";
                        try {
                            JsonObject errObj = gson.fromJson(err, JsonObject.class);
                            if (errObj.has("error")) {
                                future.completeExceptionally(new Exception(errObj.get("error").getAsString()));
                                return;
                            }
                        } catch (Exception ignored) {}
                        future.completeExceptionally(new Exception("HTTP " + r.code() + ": " + err));
                        return;
                    }
                    String bodyStr = r.body() != null ? r.body().string() : "{}";
                    JsonObject json = gson.fromJson(bodyStr, JsonObject.class);
                    future.complete(json);
                }
                catch (Exception e)
                {
                    future.completeExceptionally(e);
                }
            }
        });

        return future;
    }

    public CompletableFuture<JsonObject> pair(String apiUrl, String apiKey, String pairingCode)
    {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        if (apiUrl == null || apiUrl.isEmpty() || apiKey == null || apiKey.isEmpty())
        {
            future.completeExceptionally(new Exception("Deckscape server is not configured."));
            return future;
        }
        String url = apiUrl.endsWith("/") ? apiUrl : apiUrl + "/";
        url += "functions/v1/economy";
        JsonObject bodyJson = new JsonObject();
        bodyJson.addProperty("action", "pair_runelite");
        bodyJson.addProperty("pairingCode", pairingCode == null ? "" : pairingCode.trim().toUpperCase());
        Request request = new Request.Builder().url(url).post(RequestBody.create(JSON, gson.toJson(bodyJson)))
            .addHeader("apikey", apiKey).addHeader("Authorization", "Bearer " + apiKey).build();
        httpClient.newCall(request).enqueue(jsonCallback(future));
        return future;
    }

    private Callback jsonCallback(CompletableFuture<JsonObject> future)
    {
        return new Callback()
        {
            @Override public void onFailure(Call call, IOException e) { future.completeExceptionally(e); }
            @Override public void onResponse(Call call, Response response) throws IOException
            {
                try (Response r = response)
                {
                    String body = r.body() == null ? "{}" : r.body().string();
                    JsonObject json = gson.fromJson(body, JsonObject.class);
                    if (!r.isSuccessful()) { future.completeExceptionally(new Exception(json != null && json.has("error") ? json.get("error").getAsString() : "HTTP " + r.code())); return; }
                    future.complete(json == null ? new JsonObject() : json);
                }
                catch (Exception e) { future.completeExceptionally(e); }
            }
        };
    }
}
