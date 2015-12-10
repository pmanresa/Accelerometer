package contextawareness.accelerometer.motiondector;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;


public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        boolean headsetOn = audioManager.isWiredHeadsetOn();
        int volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        final int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        final RadioButton headphoneView = (RadioButton) findViewById(R.id.headphoneDetectedView);
        if (headsetOn == true) {
            headphoneView.toggle();
        }

        final SeekBar volumeBar = (SeekBar) findViewById(R.id.volumeSlider);
        volumeBar.setMax(maxVolume);
        volumeBar.setProgress(volume);
        volumeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        Button tmdButton = (Button) findViewById(R.id.tmdButton);
        tmdButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, TMDActivity.class);
                startActivity(intent);
            }
        });

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (audioManager.isWiredHeadsetOn()) {
                    headphoneView.setChecked(true);
                    //TODO: Start TMD classifier service
                } else {
                    headphoneView.setChecked(false);
                    //TODO: Stop TMD classifier service
                }
            }
        };
        registerReceiver(receiver, intentFilter);

        //Intent serviceIntent = new Intent(this, SensorService.class);
        //startService(serviceIntent);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent serviceIntent = new Intent(this, SensorService.class);
        stopService(serviceIntent);
    }
}
