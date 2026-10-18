package com.darkcat.android.testnobel;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class VoiceBoxClient {
    private static final String TAG = "VoiceBoxClient";
    private final String baseUrl;
    private final OkHttpClient client;

    public interface SynthesisCallback {
        void onComplete(boolean success);
    }

    public VoiceBoxClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.client = new OkHttpClient();
    }

    public void synthesize(String text, int speaker, File outputFile, SynthesisCallback callback) {
        String queryUrl = baseUrl + "/audio_query?text=" + text + "&speaker=" + speaker;
        Request queryRequest = new Request.Builder()
                .url(queryUrl)
                .post(RequestBody.create(new byte[0], null))
                .build();

        client.newCall(queryRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "audio_query failed", e);
                callback.onComplete(false);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onComplete(false);
                    return;
                }

                String queryJson = response.body().string();
                String synthesisUrl = baseUrl + "/synthesis?speaker=" + speaker;

                MediaType JSON = MediaType.parse("application/json");
                RequestBody synthesisBody = RequestBody.create(queryJson, JSON);
                Request synthesisRequest = new Request.Builder()
                        .url(synthesisUrl)
                        .post(synthesisBody)
                        .build();

                client.newCall(synthesisRequest).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.e(TAG, "synthesis failed", e);
                        callback.onComplete(false);
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (!response.isSuccessful() || response.body() == null) {
                            callback.onComplete(false);
                            return;
                        }

                        try (InputStream inputStream = response.body().byteStream();
                             FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
//                                outputStream.write(buffer, bytesRead);
                                outputStream.write(buffer);
                            }
                            callback.onComplete(true);
                        } catch (IOException e) {
                            Log.e(TAG, "writing WAV failed", e);
                            callback.onComplete(false);
                        }
                    }
                });
            }
        });
    }
}
