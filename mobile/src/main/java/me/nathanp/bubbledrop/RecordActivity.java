package me.nathanp.bubbledrop;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.icu.text.Collator;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import static android.media.MediaRecorder.*;

public class RecordActivity extends AppCompatActivity implements OnInfoListener {

    private static final int PERMISSION_REQUEST_AUDIO = 1;
    private static final String TAG = "RecordActivity";


    private RelativeLayout mLayout;
    private ImageButton mFabRecord;
    private ImageButton mFabCancel;
    private ProgressBar mRecordProgress;

    private MediaRecorder mRecorder;
    private MediaPlayer mPlayer;
    private File mFileName;

    private int mState = STATE_RECORD_READY;

    private static final int[] STATE_SET_RECORD_READY = {R.attr.state_record, -R.attr.state_check};
    private static final int[] STATE_SET_RECORDING = {R.attr.state_record, -R.attr.state_check};
    private static final int[] STATE_SET_CHECK = {-R.attr.state_record, R.attr.state_check};

    private static final int STATE_RECORD_READY = 0;
    private static final int STATE_RECORDING = 1;
    private static final int STATE_CHECK = 2;

    private Handler mHandler = new Handler();
    final Runnable mUpdater = new Runnable() {
        public void run() {
            mHandler.postDelayed(this, getResources().getInteger(R.integer.recording_interval));
            mRecordProgress.incrementProgressBy(getResources().getInteger(R.integer.recording_interval));

            int maxAmplitude = mRecorder.getMaxAmplitude();
            //TODO: do something cool with the amplitude
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        mLayout = findViewById(R.id.recording_layout);
        mFabRecord = findViewById(R.id.fab_record);
        mFabCancel = findViewById(R.id.fab_cancel);
        mRecordProgress = findViewById(R.id.record_progress);
        mRecordProgress.setMax(getResources().getInteger(R.integer.recorded_audio_duration_ms));

        mFabRecord.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        mFabRecord.performClick();
                        startRecording();
                        mState = STATE_RECORDING;
                        return true;
                    }
                    case MotionEvent.ACTION_UP: {
                        if (mState == STATE_RECORDING) {
                            if (stopRecording()) {
                                finishedRecording();
                                mState = STATE_CHECK;
                            }
                        }
                        return true;
                    }
                }
                return false;
            }
        });

        mFabRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mState == STATE_CHECK) {
                    stopPlayback();
                    Intent data = new Intent();
                    data.putExtra("recordingFilename", mFileName.getPath());
                    setResult(RESULT_OK, data);
                    finish();
                }
            }
        });

        mFabCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mState = STATE_RECORD_READY;
                discardRecording();
                setResult(RESULT_CANCELED);
                finish();
            }
        });


        ActivityCompat.requestPermissions(RecordActivity.this,
                new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                PERMISSION_REQUEST_AUDIO);

        String dir = Environment.getExternalStorageDirectory().getPath();
        Log.e(TAG, dir);
        mFileName = new File(dir, getString(R.string.recorded_audio_filename) + ".3gp");
//        try {
//        } catch (IOException e) {
//            Log.e(TAG, e.getMessage());
//        }
    }

    private void startRecording() {

        mFabRecord.setImageState(STATE_SET_RECORDING, true);
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(mFileName.getPath());
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC_ELD);
        mRecorder.setMaxDuration(getResources().getInteger(R.integer.recorded_audio_duration_ms));
        mRecorder.setOnInfoListener(this);

        try {
            mRecorder.prepare();
            mRecorder.start();
            mRecordProgress.setVisibility(View.VISIBLE);
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }
        mRecordProgress.setProgress(0);
        mHandler.post(mUpdater);
    }

    private boolean stopRecording() {
        if (mRecorder != null && mState == STATE_RECORDING) {
            mHandler.removeCallbacks(mUpdater);
            try {
                mRecorder.stop();
                mRecorder.release();
                return true;
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
                return false;
            }
        }
        return false;
    }

    private void finishedRecording() {
        mRecorder = null;
        mState = STATE_CHECK;
        showCheck(true);
        showCancel(true);
        startPlayback();
    }

    public void showCheck(boolean show) {
        if (show) {
            mFabRecord.setImageState(STATE_SET_CHECK, true);
            mRecordProgress.setVisibility(View.INVISIBLE);
            mFabRecord.setImageDrawable(getDrawable(R.drawable.ic_check));
        } else {
            mFabRecord.setImageDrawable(getDrawable(R.drawable.ic_mic));
            mFabRecord.setImageState(STATE_SET_RECORD_READY, true);
        }
    }

    public void showCancel(boolean show) {
//        if (show) {
//            mFabCancel.setVisibility(View.VISIBLE);
//        } else {
//            mFabCancel.hide();
//        }
    }

    public void discardRecording() {
        stopRecording();
        stopPlayback();
        showCancel(false);
        showCheck(false);
        mRecordProgress.setVisibility(View.INVISIBLE);
    }

    void startPlayback() {
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(mFileName.getPath());
            mPlayer.setLooping(true);
            mPlayer.prepare();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
        mPlayer.start();
    }

    void stopPlayback() {
        if (mPlayer != null) {

            mPlayer.stop();

            mPlayer = null;
        }
    }

    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {
        switch (what) {
            case MEDIA_RECORDER_INFO_MAX_DURATION_REACHED: {
                finishedRecording();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_AUDIO: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Ok...
                } else {
                    // Let's tell the user why they're wrong to refuse our permissions.
                    Snackbar.make(mLayout, getString(R.string.recording_permission_rationale), Snackbar.LENGTH_INDEFINITE).show();
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        discardRecording();
        setResult(RESULT_CANCELED);
        finish();
    }

    public void cancel(View v) {
        discardRecording();
        setResult(RESULT_CANCELED);
        finish();
    }
}
