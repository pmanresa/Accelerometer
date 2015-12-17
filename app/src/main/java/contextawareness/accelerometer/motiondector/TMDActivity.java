package contextawareness.accelerometer.motiondector;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

public class TMDActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tmd);

        Button startButton = (Button) findViewById(R.id.startServiceButton);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent serviceIntent = new Intent(TMDActivity.this, SensorService.class);
                serviceIntent.putExtra(SensorService.SERVICE_START_STOP_COMMAND, SensorService.SERVICE_START);
                startService(serviceIntent);
            }
        });

        Button stopButton = (Button) findViewById(R.id.stopServiceButton);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent serviceIntent = new Intent(TMDActivity.this, SensorService.class);
                serviceIntent.putExtra(SensorService.SERVICE_START_STOP_COMMAND, SensorService.SERVICE_STOP);
                startService(serviceIntent);
            }
        });

        Button startClassifierButton = (Button) findViewById(R.id.startClassifierButton);
        startClassifierButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent serviceIntent = new Intent(TMDActivity.this, ClassifyService.class);
                serviceIntent.putExtra(SensorService.SERVICE_START_STOP_COMMAND, ClassifyService.SERVICE_START);
                startService(serviceIntent);
            }
        });

        Button stopClassifierButton = (Button) findViewById(R.id.stopClassifierButton);
        stopClassifierButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent serviceIntent = new Intent(TMDActivity.this, ClassifyService.class);
                serviceIntent.putExtra(SensorService.SERVICE_START_STOP_COMMAND, ClassifyService.SERVICE_STOP);
                startService(serviceIntent);
            }
        });
    }

    protected void onPause() {
        super.onPause();
    }

    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
