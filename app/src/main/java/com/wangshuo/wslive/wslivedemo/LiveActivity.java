package com.wangshuo.wslive.wslivedemo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.EditText;
import android.widget.Toast;

import java.util.LinkedList;

import me.lake.librestreaming.core.listener.RESConnectionListener;
import me.lake.librestreaming.filter.hardvideofilter.BaseHardVideoFilter;
import me.lake.librestreaming.filter.hardvideofilter.HardVideoGroupFilter;
import me.lake.librestreaming.ws.StreamAVOption;
import me.lake.librestreaming.ws.StreamLiveCameraView;
import me.lake.librestreaming.ws.filter.hardfilter.GPUImageBeautyFilter;
import me.lake.librestreaming.ws.filter.hardfilter.WatermarkFilter;
import me.lake.librestreaming.ws.filter.hardfilter.extra.GPUImageCompatibleFilter;

public class LiveActivity extends AppCompatActivity {
    private static final String TAG = LiveActivity.class.getSimpleName();
    private StreamLiveCameraView mLiveCameraView;
    private StreamAVOption streamAVOption;
    private String rtmpUrl = "rtmp://ossrs.net/" + StatusBarUtils.getRandomAlphaString(3) + '/' + StatusBarUtils.getRandomAlphaDigitString(5);

    private LiveUI mLiveUI;
    private EditText etRtmpUrl;

    private final int MY_PERMISSIONS_RECORD_AUDIO = 1;
    private final int MY_PERMISSIONS_CAMERA = 2;
    private final int MY_PERMISSIONS_CAMERA_AND_AUDIO = 3;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live);
        StatusBarUtils.setTranslucentStatus(this);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
                ||
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED)
        {
            //ask for authorisation
            requestCameraAndAudioPermission();
        } else {
            //start your camera
            readyToStart();
        }
    }

    private void readyToStart() {
        initLiveConfig();
        etRtmpUrl = (EditText) this.findViewById(R.id.et_rtmpUrl);
        rtmpUrl = etRtmpUrl.getText().toString();
        mLiveUI = new LiveUI(this,mLiveCameraView,rtmpUrl);

    }

    private void requestCameraAndAudioPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
        || ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            //When permission is not granted by user, show them message why this permission is needed.
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA)) {
                Toast.makeText(this, "Please grant permissions to camera and audio", Toast.LENGTH_LONG).show();

                //Give user option to still opt-in the permissions
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO},
                        MY_PERMISSIONS_CAMERA_AND_AUDIO);

            } else {
                // Show user dialog to grant permission to record audio
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA,
                                Manifest.permission.RECORD_AUDIO},
                        MY_PERMISSIONS_CAMERA_AND_AUDIO);
            }
        }
        //If permission is granted, then go ahead recording audio
        else if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            //Go ahead with recording audio now
            readyToStart();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,  int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_CAMERA) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }
        }

        if (requestCode == MY_PERMISSIONS_RECORD_AUDIO ) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted, yay!
                Toast.makeText(this, "Permissions granted to record audio", Toast.LENGTH_LONG).show();
            } else {
                // permission denied, boo! Disable the
                // functionality that depends on this permission.
                Toast.makeText(this, "Permissions Denied to record audio", Toast.LENGTH_LONG).show();
            }
        }

        if (requestCode == MY_PERMISSIONS_CAMERA_AND_AUDIO) {
            if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                readyToStart();
            } else {
                Toast.makeText(this, "Permissions Denied to use camera and record audio", Toast.LENGTH_LONG).show();
            }
        }
    }//end onRequestPermissionsResult

    /**
     * 设置推流参数
     */
    public void initLiveConfig() {
        mLiveCameraView = (StreamLiveCameraView) findViewById(R.id.stream_previewView);

        //参数配置 start
        streamAVOption = new StreamAVOption();
        streamAVOption.streamUrl = rtmpUrl;
        streamAVOption.cameraIndex = 0;
        streamAVOption.videoWidth = 1920;
        streamAVOption.videoHeight = 1080;

        //参数配置 end

        mLiveCameraView.init(this, streamAVOption);
        mLiveCameraView.addStreamStateListener(resConnectionListener);
        LinkedList<BaseHardVideoFilter> files = new LinkedList<>();
        files.add(new GPUImageCompatibleFilter(new GPUImageBeautyFilter()));
        //files.add(new WatermarkFilter(BitmapFactory.decodeResource(getResources(),R.mipmap.live),new Rect(100,100,200,200)));
        mLiveCameraView.setHardVideoFilter(new HardVideoGroupFilter(files));
    }

    RESConnectionListener resConnectionListener = new RESConnectionListener() {
        @Override
        public void onOpenConnectionResult(int result) {
            //result 0成功  1 失败
            rtmpUrl = etRtmpUrl.getText().toString();
            Toast.makeText(LiveActivity.this,"Start streaming status: "+result+ " Stream address: "+rtmpUrl,Toast.LENGTH_LONG).show();
        }

        @Override
        public void onWriteError(int errno) {
            Toast.makeText(LiveActivity.this,"Streaming error, try again.",Toast.LENGTH_LONG).show();
        }

        @Override
        public void onCloseConnectionResult(int result) {
            //result 0成功  1 失败
            Toast.makeText(LiveActivity.this,"Stop streaming status: "+result,Toast.LENGTH_LONG).show();
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLiveCameraView.destroy();
    }


}
