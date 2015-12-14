package com.mercury.dixtofon;

import android.app.AlertDialog;
import android.app.ListFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TwoLineListItem;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class MainActivityFragment extends ListFragment {

    private static final String ACTION_UPDATE_LIST = "UpdateF*ckingList";
    public static final Intent UPDATE_INTENT = new Intent(ACTION_UPDATE_LIST);
    private static final String LOG_TAG = "MainFragment";

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            rescanStore();
        }
    };

    static {
        UPDATE_INTENT.setType("text/*");
    }

    private RecordsArrayAdapter rAdapter;
    private RecordingsPlayer rPlayer;
    private ArrayList<File> fileList;

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rescanStore();
        rPlayer = RecordingsPlayer.getInstance();
        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                startPlaying(position);
            }
        });
        registerForContextMenu(getListView());
        /*getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                final File item = rAdapter.getItem(position);
                builder.setMessage(getString(R.string.delete_message) + " " + item.getName() + "?")
                        .setCancelable(true)
                        .setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                rAdapter.remove(item);
                                item.delete();
                                RecordingsStore.getInstance().refreshFiles();
                                if (rPlayer.isNowPlaying())
                                    rPlayer.abandon();
                                Snackbar.make(getListView(), getString(R.string.deleted), Snackbar.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
                return true;
            }
        });*/
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver, IntentFilter.create(ACTION_UPDATE_LIST, "text/*"));
    }

    private void startPlaying(int position) {
        File itemFile = rAdapter.getItem(position);
        if (rPlayer.isPlayingFile(itemFile))
            if (rPlayer.isNowPlaying())
                rPlayer.pause();
            else
                rPlayer.continuePlaying();
        else
            try {
                rPlayer.play(itemFile);
                Snackbar.make(getListView(), getString(R.string.playing) + " " + itemFile.getName(), Snackbar.LENGTH_LONG).show();
            } catch (IOException e) {
                Snackbar.make(getListView(), R.string.error_playing, Snackbar.LENGTH_LONG).show();
            }
    }

    private void pausePlaying() {
        if (rPlayer.isNowPlaying())
            rPlayer.pause();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        File f = (File) getListView().getItemAtPosition(((AdapterView.AdapterContextMenuInfo) menuInfo).position);
        menu.add(
                (rPlayer.isPlayingFile(f) && rPlayer.isNowPlaying()) ?
                        R.string.pause_playing :
                        R.string.play
        );
        menu.add(R.string.rename);
        menu.add(R.string.delete);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        if (item.getTitle().equals(getString(R.string.play)))
            startPlaying(info.position);
        else if (item.getTitle().equals(getString(R.string.pause_playing)))
            pausePlaying();
        else if (item.getTitle().equals(getString(R.string.rename)))
            rename(info.position);
        else if (item.getTitle().equals(getString(R.string.delete)))
            delete(info.position);
        return super.onContextItemSelected(item);

    }

    private void delete(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final File item = rAdapter.getItem(position);
        builder.setMessage(getString(R.string.delete_message) + " " + item.getName() + "?")
                .setCancelable(true)
                .setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (rPlayer.isNowPlaying())
                            rPlayer.abandon();
                            Snackbar.make(getListView(), (item.delete())?getString(R.string.deleted):getString(R.string.not_delete), Snackbar.LENGTH_SHORT).show();
                        rescanStore();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void rename(int position) {
        final File tF = fileList.get(position);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final EditText dialogEditText = new EditText(getActivity());
        dialogEditText.setText(
                tF.getName().substring(0, tF.getName().lastIndexOf('.'))
        );
        dialogEditText.setTextColor(getResources().getColor(android.R.color.black));
        dialogEditText.setPadding(10, 10, 10, 10);
        builder.setView(dialogEditText)
                .setTitle(R.string.enter_name)
                .setPositiveButton(R.string.rename, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        File f = new File(
                                RecordingsStore.STORE_DIRECTORY,
                                dialogEditText.getText().toString() + ".mp4");
                        if (f.exists())
                            for (int i = 1;
                                 (f = new File(RecordingsStore.STORE_DIRECTORY, dialogEditText.getText().toString() + " (" + i + ").mp4")).exists();
                                 i++)
                                ;
                        if (tF.renameTo(f))
                            Snackbar.make(getListView(), R.string.recording_renamed, Snackbar.LENGTH_SHORT).show();
                        else
                            Snackbar.make(getListView(), R.string.recording_renaming_fail, Snackbar.LENGTH_SHORT).show();
                        dialog.dismiss();
                        rescanStore();
                    }
                }).
                setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .setCancelable(false)
                .show();
    }

    private void rescanStore() {
        RecordingsStore.getInstance().refreshFiles();
        fileList = RecordingsStore.getInstance().getAllRecordings();
        rAdapter = new RecordsArrayAdapter(getActivity(), android.R.layout.simple_list_item_2, fileList);
        rAdapter.setNotifyOnChange(true);
        setListAdapter(rAdapter);
    }

    private class RecordsArrayAdapter extends ArrayAdapter<File> {

        private String formatData(Calendar c) {
            int day = c.get(Calendar.DAY_OF_MONTH);
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);
            return String.format("%2s %s %2d %s:%s",
                    ((day < 10) ? "0" : "") + day,
                    c.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()),
                    c.get(Calendar.YEAR),
                    ((hour < 10) ? "0" : "") + hour,
                    ((minute < 10) ? "0" : "") + minute
            );
        }

        public RecordsArrayAdapter(Context context, int resource, ArrayList<File> objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            File myItem = getItem(position);
            TwoLineListItem view;
            if (convertView != null) {
                view = (TwoLineListItem) convertView;
            } else {
                LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = (TwoLineListItem) inflater.inflate(
                        android.R.layout.simple_list_item_2, null);
            }
            view.getText1().setText(myItem.getName().substring(0, myItem.getName().lastIndexOf(".")));
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(myItem.lastModified());
            view.getText2().setText(formatData(c));
            return view;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(receiver);
    }
}
