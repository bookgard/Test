package com.bjnet.recordingaudio;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import static com.bjnet.recordingaudio.GlobalConfig.AUDIO_FORMAT;
import static com.bjnet.recordingaudio.GlobalConfig.CHANNEL_CONFIG;
import static com.bjnet.recordingaudio.GlobalConfig.SAMPLE_RATE_INHZ;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int MY_PERMISSIONS_REQUEST = 1001;
    private static final String TAG = "jqd";

    private Button play;
    private Button pause;

    /**
     * 需要申请的运行时权限
     */
    private String[] permissions = new String[]{
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    /**
     * 被用户拒绝的权限列表
     */
    private List<String> mPermissionList =new ArrayList<>();
    private boolean isRecording;
    private AudioRecord audioRecord;
    private Button change;
    private AudioTrack audioTrack;
    private byte[] audioData;
    private FileInputStream fileInputStream;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        play=(Button)findViewById(R.id.play);
        play.setOnClickListener(this);
        change=(Button)findViewById(R.id.change);
        change.setOnClickListener(this);
        pause=(Button)findViewById(R.id.pause);
        pause.setOnClickListener(this);

        checkPermissions();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.play:
                Button button=(Button) view;
                if(button.getText().toString().equals(getString(R.string.start_record))){
                    button.setText(getString(R.string.stop_record));
                    startRecord();
                }else {
                    button.setText(getString(R.string.start_record));
                    stopRecord();
                }
                break;
            case R.id.change:
                PcmToWavUtil pcmToWavUtil =new PcmToWavUtil(SAMPLE_RATE_INHZ,CHANNEL_CONFIG,AUDIO_FORMAT);
                File pcmFile =new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC),"test.pcm");
                File wavFile =new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC),"test.wav");
                if(!wavFile.mkdirs()){
                    Log.e(TAG,"wavFile Directory not created");
                }
                if(wavFile.exists()){
                    wavFile.delete();
                }
                pcmToWavUtil.pcmToWav(pcmFile.getAbsolutePath(),wavFile.getAbsolutePath());

                break;
            case R.id.pause:
                Button btn =(Button)view;
                String string = btn.getText().toString();
                if(string.equals(getString(R.string.start_play))){
                    btn.setText(getString(R.string.stop_play));
                    playInModeStream();
                }else{
                    btn.setText(getString(R.string.start_play));
                    stopPlay();
                }
                break;

            default:
                break;

        }

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, permissions[i] + " 权限被用户禁止！");
                }
            }
            // 运行时权限的申请不是本demo的重点，所以不再做更多的处理，请同意权限申请。
        }
    }
public void startRecord(){
        final int minBufferSize= AudioRecord.getMinBufferSize(SAMPLE_RATE_INHZ,CHANNEL_CONFIG,AUDIO_FORMAT);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,SAMPLE_RATE_INHZ,
                CHANNEL_CONFIG,AUDIO_FORMAT,minBufferSize);
        final byte data[] =new byte[minBufferSize];
        final File file =new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC),"test.pcm");
        if(!file.mkdirs()){
            Log.e(TAG,"Directroy not created");
        }
        if(file.exists()){
            file.delete();
        }
        audioRecord.startRecording();
        isRecording= true;

        new Thread(new Runnable() {
            @Override
            public void run() {
                FileOutputStream os=null;
                try{
                    os=new FileOutputStream(file);
                }catch (FileNotFoundException e){
                    e.printStackTrace();
                }
                if(null!=os){
                    while(isRecording){
                        int read=audioRecord.read(data,0,minBufferSize);
                        if(AudioRecord.ERROR_INVALID_OPERATION!=read){
                            try{
                                os.write(data);
                            }catch (IOException e){
                                e.printStackTrace();
                            }
                        }
                    }
                    try{
                        Log.i(TAG,"run:close file output stream !");
                        os.close();
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
            }
        }).start();
}
public void stopRecord(){
        isRecording= false;
        if(null!=audioRecord){
            audioRecord.stop();
            audioRecord.release();
            audioRecord=null;
    }
}
private void checkPermissions(){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            for(int i=0;i<permissions.length;i++){
                if(ContextCompat.checkSelfPermission(this,permissions[i])!=PackageManager.PERMISSION_GRANTED){
                    mPermissionList.add(permissions[i]);
                }
            }
            if(!mPermissionList.isEmpty()){
                String[] permissions=mPermissionList.toArray(new String[mPermissionList.size()]);
                ActivityCompat.requestPermissions(this,permissions,MY_PERMISSIONS_REQUEST);

            }
        }
}

private void playInModeStream(){
        int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
        final int minBufferSize =AudioTrack.getMinBufferSize(SAMPLE_RATE_INHZ,channelConfig,AUDIO_FORMAT);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        audioTrack = new AudioTrack(
                new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                new AudioFormat.Builder().setSampleRate(SAMPLE_RATE_INHZ)
                        .setEncoding(AUDIO_FORMAT)
                         .setChannelMask(channelConfig)
                        .build(),
                minBufferSize,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE);
    }
    audioTrack.play();
        File file = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC),"test.pcm");
        try{
            fileInputStream = new FileInputStream(file);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try{
                        byte[] tempBuffer=new byte[minBufferSize];
                        while (fileInputStream.available()>0){
                            int readCount =fileInputStream.read(tempBuffer);
                            if(readCount==AudioTrack.ERROR_INVALID_OPERATION||
                            readCount== AudioTrack.ERROR_BAD_VALUE){
                                continue;
                            }
                            if(readCount!=0&&readCount!=-1){
                                audioTrack.write(tempBuffer,0,readCount);
                            }
                        }
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
            }).start();
        }catch (IOException e){
            e.printStackTrace();
        }
}
private void playInModeStatic(){
        new AsyncTask<Void,Void,Void>(){

            @Override
            protected Void doInBackground(Void... voids) {
                try{
                    InputStream in =getResources().openRawResource(R.raw.ding);
                    try{
                        ByteArrayOutputStream out=new ByteArrayOutputStream();
                        for(int b;(b=in.read())!=-1;){
                            out.write(b);
                        }
                        Log.d(TAG,"Got the data");
                        audioData =out.toByteArray();
                    }finally {
                        in.close();
                    }
                }catch (IOException e){
                    Log.wtf(TAG,"Failed to read",e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                Log.i(TAG,"Creating track...audioData.length ="+audioData.length);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    audioTrack = new AudioTrack(
                            new AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_MEDIA)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                    .build(),
                            new AudioFormat.Builder().setSampleRate(22050)
                                    .setEncoding(AudioFormat.ENCODING_PCM_8BIT)
                                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                    .build(),
                            audioData.length,
                            AudioTrack.MODE_STATIC,
                            AudioManager.AUDIO_SESSION_ID_GENERATE);
                }
                Log.d(TAG,"Writing audio data...");
                audioTrack.write(audioData,0,audioData.length);
                Log.d(TAG,"Start playback");
                audioTrack.play();
                Log.d(TAG,"Playing");

            }
        }.execute();
}

private void stopPlay(){
        if(audioTrack !=null){
            Log.d(TAG,"Stopping");
            audioTrack.stop();
            Log.d(TAG,"Releasing");
            audioTrack.release();
            Log.d(TAG,"Nulling");
        }
}

}
