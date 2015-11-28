package com.mercury.dixtofon;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import java.io.File;
import java.io.IOException;

/**
 * Капец... Тут такого нах*евертил, что аж самому страшно-_-
 */

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = "MainActivity";
    private static final String ACTION_STOP_RECORDING = "stopRecord";
    private static final Intent RECORD_FINISHED_INTENT = new Intent(ACTION_STOP_RECORDING);

    {
        RECORD_FINISHED_INTENT.setType("text/*");
    }

    private RecordingsStore.Recorder recorder;
    private Snackbar recordingSnackbar;
    private FloatingActionButton fab;
    private BroadcastReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        recorder = RecordingsStore.Recorder.getInstance(getFilesDir());
        createSnackBar();
        //После поворота девайса снова показать снекбар
        if (recorder.isRecordingNow())
            recordingSnackbar.show();
        //Получение бродкаста об остановке записи
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, Intent intent) {
                final File tF = recorder.stopRecording();
                final EditText dialogEditText = new EditText(context);
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                dialogEditText.setText(
                        getString(R.string.recording_name) + " " + (RecordingsStore.getInstance().getNumberOfRecords() + 1)
                );
                dialogEditText.setTextColor(getResources().getColor(android.R.color.black));
                dialogEditText.setPadding(10,10,10,10);
                builder.setView(dialogEditText)
                        .setTitle(R.string.enter_name)
                        .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                RecordingsStore.getInstance().saveRecording(MainActivity.this, tF, dialogEditText.getText().toString());
                                Snackbar.make(fab, R.string.recording_saved, Snackbar.LENGTH_SHORT).show();
                                dialog.dismiss();
                            }
                        }).
                        setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                tF.delete();
                                dialog.cancel();
                                Snackbar.make(fab, R.string.save_canceled, Snackbar.LENGTH_SHORT).show();
                            }
                        })
                        .setCancelable(false)
                        .show();
                if (recordingSnackbar.isShown())
                    recordingSnackbar.dismiss();
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, IntentFilter.create(ACTION_STOP_RECORDING, "text/*"));
        //Летающая кнопка
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!recorder.isRecordingNow())
                    try {
                        recorder.startRecording(new MediaRecorder.OnInfoListener() {
                            @Override
                            public void onInfo(MediaRecorder mr, int what, int extra) {
                                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(RECORD_FINISHED_INTENT);
                                }
                            }
                        });
                        createSnackBar();
                        recordingSnackbar.show();
                    } catch (IOException e) {
                        Log.e(LOG_TAG, "Something goes (breaking) bad...: " + e.getMessage());
                    }
            }
        });
        //Хотя, после пары комментариев, оказывается, все не так уж страшно, как оно выглядит
    }

    private void createSnackBar() {
        recordingSnackbar = Snackbar.make(fab, R.string.recording_now, Snackbar.LENGTH_INDEFINITE)
                .setCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        super.onDismissed(snackbar, event);
                        if (recorder.isRecordingNow())
                            if (event == Snackbar.Callback.DISMISS_EVENT_SWIPE) {
                                recorder.cancelRecord();
                                Snackbar.make(fab, R.string.recording_canceled, Snackbar.LENGTH_SHORT).show();
                            }
                    }
                })
                .setAction(R.string.stop_recording, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (recorder.isRecordingNow())
                            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(RECORD_FINISHED_INTENT);
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
    }
}

