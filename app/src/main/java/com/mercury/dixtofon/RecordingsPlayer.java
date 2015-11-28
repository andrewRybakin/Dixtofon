package com.mercury.dixtofon;

import android.media.MediaPlayer;

import java.io.File;
import java.io.IOException;

public class RecordingsPlayer {

    private static RecordingsPlayer instance;
    private MediaPlayer mPlayer;
    private File file;
    private boolean isPaused;

    private RecordingsPlayer() {
        isPaused = false;
    }

    public static RecordingsPlayer getInstance() {
        if (instance == null)
            instance = new RecordingsPlayer();
        return instance;
    }

    public void play(File f) throws IOException {
        if (mPlayer == null)
            mPlayer = new MediaPlayer();
        if (mPlayer.isPlaying() || isPaused) {
            mPlayer.stop();
            mPlayer.release();
            mPlayer = null;
            mPlayer = new MediaPlayer();
        }
        file = f;
        mPlayer.setDataSource(f.getAbsolutePath());
        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mPlayer.release();
                mPlayer = null;
                file = null;
            }
        });
        mPlayer.prepare();
        mPlayer.start();
    }

    public void pause() {
        isPaused = true;
        mPlayer.pause();
    }

    public void abandon() {
        isPaused = false;
        mPlayer.stop();
        mPlayer.release();
        mPlayer = null;
    }

    public void continuePlaying() {
        mPlayer.start();
        isPaused = false;
    }

    public boolean isNowPlaying() {
        if (mPlayer == null)
            return false;
        return mPlayer.isPlaying();
    }

    public boolean isPlayingFile(File f) {
        if (file == null)
            return false;
        return file.equals(f);
    }

}
