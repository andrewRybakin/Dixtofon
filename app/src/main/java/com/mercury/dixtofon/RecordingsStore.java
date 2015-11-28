package com.mercury.dixtofon;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;

public class RecordingsStore {

    private static final String STORE_DIRECTORY =
            Environment.getExternalStorageDirectory().getAbsolutePath() +
                    "/" +
                    Environment.DIRECTORY_MUSIC + "/Dixtofon";
    private static final String LOG_TAG = "RecordingStore";

    private static RecordingsStore instance;
    private int numberOfRecords;
    private File[] recordingsInDirectory;
    private File recordingsDirectory;

    private RecordingsStore() {
        recordingsDirectory = new File(STORE_DIRECTORY);
        recordingsDirectory.mkdirs();
        recordingsInDirectory = new File[0];
        refreshFiles();
        numberOfRecords = recordingsInDirectory.length;
    }

    public static RecordingsStore getInstance() {
        if (instance == null)
            instance = new RecordingsStore();
        return instance;
    }

    public int getNumberOfRecords() {
        return numberOfRecords;
    }

    public void saveRecording(Context c, File tempFile, String givenName) {
        File f = new File(STORE_DIRECTORY, givenName + ".mp4");
        if (f.exists())
            for (int i = 1;
                 (f = new File(STORE_DIRECTORY, givenName + " (" + i + ").mp4")).exists();
                 i++)
                ;
        try {
            Log.d("RecordingStore", STORE_DIRECTORY);
            FileOutputStream fOut = new FileOutputStream(f);
            FileChannel in = new FileInputStream(tempFile).getChannel();
            FileChannel out = fOut.getChannel();
            out.transferFrom(in, 0, in.size());
            in.close();
            out.close();
            tempFile.deleteOnExit();
            numberOfRecords++;
            //Обновление листвю
            LocalBroadcastManager.getInstance(c).sendBroadcast(MainActivityFragment.UPDATE_INTENT.putExtra(MainActivityFragment.EXTRA_NEW_ITEM, f));
        } catch (IOException e) {
            Log.e(LOG_TAG, "aaa", e);
        }
    }

    public ArrayList<File> getAllRecordings() {
        refreshFiles();
        if (recordingsInDirectory == null)
            return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(recordingsInDirectory));
    }

    public void refreshFiles() {
        recordingsInDirectory = recordingsDirectory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.contains(".mp4");
            }
        });
        numberOfRecords = recordingsInDirectory.length;
    }

    public static class Recorder {
        private MediaRecorder mRecorder;

        private static Recorder instance;
        private static File tempFile;
        private boolean recordingNow;
        private static File filesDir;

        public static Recorder getInstance(File dir) {
            filesDir = dir;
            if (instance == null)
                instance = new Recorder();
            return instance;
        }

        private Recorder() {

        }

        private void recreate() {
            try {
                mRecorder = new MediaRecorder();
                tempFile = File.createTempFile("Recording", "", filesDir);
                mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                mRecorder.setOutputFile(tempFile.getAbsolutePath());
                mRecorder.setMaxDuration(10 * 1000);
            } catch (IOException e) {
                Log.d(LOG_TAG, e.getMessage());
            }
        }

        public void startRecording(MediaRecorder.OnInfoListener listener) throws IOException {
            if (!isRecordingNow()) {
                recreate();
                mRecorder.setOnInfoListener(listener);
                mRecorder.prepare();
                if (RecordingsPlayer.getInstance().isNowPlaying())
                    RecordingsPlayer.getInstance().abandon();
                mRecorder.start();
                recordingNow = true;
            }
        }

        public File stopRecording() {
            if (recordingNow) {
                mRecorder.stop();
                mRecorder.release();
                recordingNow = false;
                mRecorder = null;
                return tempFile;
            }
            throw new IllegalStateException("Not Recording");
        }

        public void cancelRecord() {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
            recordingNow = false;
        }

        public boolean isRecordingNow() {
            return recordingNow;
        }
    }
}
