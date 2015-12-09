package contextawareness.accelerometer.motiondector;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
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

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        //boolean headsetOn = audioManager.isWiredHeadsetOn();
        //int volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        //Toast.makeText(this, Integer.toString(volume), Toast.LENGTH_SHORT).show();
        //audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0); // Sets volume, works

        Intent intent = new Intent(this, TMDActivity.class);
        startActivity(intent);

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
