package com.truedreamz.audiovideorecorder;

import android.Manifest;
import android.app.Activity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.VideoView;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.Timer;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.truedreamz.audiovideorecorder.R.string.mScreenRecord;

public class MainActivity extends AppCompatActivity implements OnProgressBarListener{

    public final static String APP_PATH = "/AKC/";
    public final static String TAG = "MainActivity";

    Button btnAudioRecord,btnScreenRecord;
    ImageView imageViewMode;

    String strAudioSavePathInDevice = null;
    MediaRecorder mediaRecorder ;

    private static final int AUDIO_REQ_PERMISSION_CODE = 1;
    private static final int VIDEO_REQ_PERMISSION_CODE = 2;

    private static final int SCREEN_RECORD_REQUEST_CODE = 1000;

    MediaPlayer mediaPlayer ;

    int recordTime,playTime;
    Handler handler;
    Boolean isRecording;
    ImageButton imgAudioPlay;
    private NumberProgressBar mNumberProgress;
    private Timer timer;
    private RelativeLayout layout_audio_confirm;

    // 10 seconds duration
    private int mMaxAudioDuration=10*1000;

    private ToggleButton mToggleButton;
    private Context mContext;
    private ScreenRecorderUtils mScreenRecord;
    private ImageButton imgBtnPlayVideo;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext=this.getApplicationContext();
        mScreenRecord=new ScreenRecorderUtils(mContext,MainActivity.this);

        btnAudioRecord=(Button)findViewById(R.id.btnAudioRecord);
        btnScreenRecord=(Button)findViewById(R.id.btnScreenRecord);
        imageViewMode=(ImageView)findViewById(R.id.imageViewMode);
        layout_audio_confirm=(RelativeLayout)findViewById(R.id.layout_audio_confirm);
        imgAudioPlay=(ImageButton)findViewById(R.id.imgAudioPlay);
        mToggleButton = (ToggleButton) findViewById(R.id.toggleScreenRecord);
        imgBtnPlayVideo=(ImageButton)findViewById(R.id.imgBtnPlayVideo);

        btnAudioRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageViewMode.setImageResource(R.drawable.mic_pic);
                if(checkPermission()){
                    imgBtnPlayVideo.setVisibility(View.GONE);
                    mToggleButton.setVisibility(View.GONE);
                    layout_audio_confirm.setVisibility(View.VISIBLE);
                    resetAudioRecording();
                    startAudioRecording(mMaxAudioDuration);
                }else{
                    requestPermission();
                }
            }
        });

        btnScreenRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mToggleButton.setVisibility(View.VISIBLE);
                layout_audio_confirm.setVisibility(View.GONE);
                imageViewMode.setImageResource(R.drawable.video);
            }
        });

        mNumberProgress = (NumberProgressBar)findViewById(R.id.numberbarAudio);
        mNumberProgress.setOnProgressBarListener(this);
        mNumberProgress.setSuffix("s");
        handler=new Handler();
        isRecording=false;

        // Check screen record permission
        checkScreenRecordPermission();
    }

    public  void startAudioRecording(int maxDuration)
    {
        Toast.makeText(MainActivity.this, "Recording started,", Toast.LENGTH_SHORT).show();
        mediaRecorderReady();
        // Starting record time
        recordTime=0;
        mNumberProgress.setMax(maxDuration/1000);
        try
        {
            mediaRecorder.prepare();
            mediaRecorder.start();
        }
        catch (IllegalStateException e)
        {
            Log.e(TAG,"Audio record excep:"+e.getMessage());
        } catch (IOException e)
        {
            Log.e(TAG,"Audio record excep:"+e.getMessage());
        }

        // Change isRecroding flag to true
        isRecording=true;
        // Post the record progress
        handler.post(UpdateRecordTime);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                imgAudioPlay.setVisibility(View.VISIBLE);
                isRecording=false;
                mediaRecorder.stop();
                Toast.makeText(MainActivity.this, "Recording Completed.", Toast.LENGTH_SHORT).show();
            }
        },maxDuration);
    }

    public void mediaRecorderReady(){
        mediaRecorder=new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
        String strAudiofile=getAudioFileName();
        mediaRecorder.setOutputFile(strAudiofile);
    }

    Runnable UpdatePlayTime=new Runnable(){
        public void run(){
            if(mediaPlayer.isPlaying()){
                // Update play time and SeekBar
                playTime+=1;
                mNumberProgress.setProgress(playTime);
                // Delay 1s before next call
                handler.postDelayed(this, 1000);
            }
        }
    };

    Runnable UpdateRecordTime=new Runnable(){
        public void run(){
            if(isRecording){
                recordTime+=1;
                mNumberProgress.setProgress(recordTime);
                // Delay 1s before next call
                handler.postDelayed(this, 1000);
            }
        }
    };


    public String getAudioFileName(){
        File dir_texture = new File(getExternalFilesDir(null)+ APP_PATH);
        if(!dir_texture.exists()){
            dir_texture.mkdir();
        }
        return (dir_texture.getAbsolutePath() +"/"+ "audio.mp3");
    }

    private void stopRecording() {
        if (null != mediaRecorder) {
            if(isRecording) mediaRecorder.stop();
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;

            if(mediaPlayer!=null){
                mediaPlayer.reset();
                mediaPlayer.release();
                mediaPlayer=null;
            }
        }
    }

    private void resetAudioRecording(){
        if(handler!=null){
            handler.removeCallbacks(UpdatePlayTime);
            handler.removeCallbacks(UpdateRecordTime);
        }
        stopRecording();
        imgAudioPlay.setVisibility(View.GONE);
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                WRITE_EXTERNAL_STORAGE, RECORD_AUDIO}, AUDIO_REQ_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case AUDIO_REQ_PERMISSION_CODE:
                if (grantResults.length > 0) {
                    boolean StoragePermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean RecordPermission = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (StoragePermission && RecordPermission) {
                        Toast.makeText(MainActivity.this, "Permission Granted", Toast.LENGTH_LONG).show();
                    }
                    else{
                        Toast.makeText(MainActivity.this,"Permission Denied",Toast.LENGTH_LONG).show();
                    }
                }
                break;
            case VIDEO_REQ_PERMISSION_CODE: {
                if ((grantResults.length > 0) && (grantResults[0] +
                        grantResults[1]) == PackageManager.PERMISSION_GRANTED) {
                    mScreenRecord.onToggleScreenShare(true);
                } else {
                    mToggleButton.setChecked(false);

                    Snackbar.make(findViewById(android.R.id.content), "Permissions",
                            Snackbar.LENGTH_INDEFINITE).setAction("ENABLE",
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent intent = new Intent();
                                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                                    intent.setData(Uri.parse("package:" + getPackageName()));
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                                    startActivity(intent);
                                }
                            }).show();
                }
                return;
            }
        }
    }

    public boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(), RECORD_AUDIO);
        return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED;
    }

    public void onAudioPlayListener(View v){
        // set start time
        playTime=0;
        mNumberProgress.setMax(mMaxAudioDuration/1000);
        mNumberProgress.setProgress(0);

        mediaPlayer = new MediaPlayer();

        try {
            String strAudiofile=getAudioFileName();
            mediaPlayer.setDataSource(strAudiofile);
            mediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mediaPlayer.start();
        // Post the play progress
        handler.post(UpdatePlayTime);
    }

    public void onAudioRecordConfirmListener(View v){
        layout_audio_confirm.setVisibility(View.GONE);
        resetAudioRecording();
    }

    public void onAudioRecordRedoListener(View v){
        resetAudioRecording();
        startAudioRecording(mMaxAudioDuration);
    }

    @Override
    public void onProgressChange(int current, int max) {
        if(current == max) {
            Toast.makeText(getApplicationContext(), "Its Done.", Toast.LENGTH_SHORT).show();
        }
    }


    /*Screen record*/
    private void checkScreenRecordPermission(){
        mToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(mContext,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) + ContextCompat
                        .checkSelfPermission(MainActivity.this,
                                Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale
                            (MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                            ActivityCompat.shouldShowRequestPermissionRationale
                                    (MainActivity.this, Manifest.permission.RECORD_AUDIO)) {
                        mToggleButton.setChecked(false);
                        Snackbar.make(findViewById(android.R.id.content), "Permissions",
                                Snackbar.LENGTH_INDEFINITE).setAction("ENABLE",
                                new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        ActivityCompat.requestPermissions(MainActivity.this,
                                                new String[]{Manifest.permission
                                                        .WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO},
                                                VIDEO_REQ_PERMISSION_CODE);
                                    }
                                }).show();
                    } else {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission
                                        .WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO},
                                VIDEO_REQ_PERMISSION_CODE);
                    }
                } else {
                    if(mToggleButton.isChecked()) {
                        mScreenRecord.onToggleScreenShare(true);
                        imgBtnPlayVideo.setVisibility(View.GONE);
                        Toast.makeText(mContext,"Screen recording is started,",Toast.LENGTH_SHORT).show();
                    }else{
                        mScreenRecord.onToggleScreenShare(false);
                        imgBtnPlayVideo.setVisibility(View.VISIBLE);
                        Toast.makeText(mContext,"Screen recording is done, Tap below icon to play video.",Toast.LENGTH_LONG).show();

                    }
                }
            }
        });
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SCREEN_RECORD_REQUEST_CODE) {
            if (resultCode != RESULT_OK) {
                Toast.makeText(this,
                        "Screen Cast Permission Denied", Toast.LENGTH_SHORT).show();
                mToggleButton.setChecked(false);
                return;
            }
            mScreenRecord.getActivityResult(resultCode,data);
        }
    }

    public void onPlayVideoListener(View v){
        String videoFile=getVideoFileName();
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(videoFile));
        intent.setDataAndType(Uri.parse(videoFile), "video/mp4");
        startActivity(intent);
    }

    public String getVideoFileName(){
        File dir_texture = new File(mContext.getExternalFilesDir(null)+ APP_PATH);
        if(!dir_texture.exists()){
            dir_texture.mkdir();
        }
        return (dir_texture.getAbsolutePath() +"/"+ "video.mp4");
    }
}