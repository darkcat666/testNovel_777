package com.darkcat.android.testnobel;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

public class StoryMakerService extends Service {

    private MediaProjection mediaProjection;
    private MediaRecorder mediaRecorder;
    private VirtualDisplay virtualDisplay;

    // MyService.java (Serviceクラス内)
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        // サービスを停止する
        stopSelf();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        startNotification();

        int resultCode = intent.getIntExtra("resultCode", Activity.RESULT_CANCELED);

        Intent data = intent.getParcelableExtra("data");

        MediaProjectionManager projectionManager =
                (MediaProjectionManager)
                        getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        mediaProjection =
                projectionManager.getMediaProjection(resultCode, data);

        startRecording();

        return START_NOT_STICKY;
    }

    private void startRecording() {

        try {

            mediaRecorder = new MediaRecorder();

            mediaRecorder.setVideoSource(
                    MediaRecorder.VideoSource.SURFACE
            );

            mediaRecorder.setOutputFormat(
                    MediaRecorder.OutputFormat.MPEG_4
            );

            mediaRecorder.setOutputFile(
                    getExternalFilesDir(null)
                            + "/record.mp4"
            );

            mediaRecorder.setVideoEncoder(
                    MediaRecorder.VideoEncoder.H264
            );

            mediaRecorder.setVideoSize(720, 1280);

            mediaRecorder.setVideoFrameRate(30);

            mediaRecorder.setVideoEncodingBitRate(
                    5 * 1000 * 1000
            );

            mediaRecorder.prepare();

            virtualDisplay =
                    mediaProjection.createVirtualDisplay(
                            "ScreenRecord",
                            720,
                            1280,
                            getResources().getDisplayMetrics().densityDpi,
                            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                            mediaRecorder.getSurface(),
                            null,
                            null
                    );

            mediaRecorder.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startNotification() {

        String channelId = "record_channel";

        NotificationChannel channel =
                new NotificationChannel(
                        channelId,
                        "録画",
                        NotificationManager.IMPORTANCE_LOW
                );

        NotificationManager manager =
                getSystemService(NotificationManager.class);

        manager.createNotificationChannel(channel);

        Notification notification =
                new NotificationCompat.Builder(this, channelId)
                        .setContentTitle("録画中")
                        .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                        .build();

        startForeground(1, notification);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}