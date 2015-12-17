package contextawareness.accelerometer.motiondector;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;


public class MainActivity extends Activity {

    public static final String KEY_PREF_WALK_VOLUME = "KEY_PREF_WALK_VOLUME";
    public static final String KEY_PREF_BIKE_VOLUME = "KEY_PREF_BIKE_VOLUME";
    public static final String KEY_PREF_BUS_VOLUME = "KEY_PREF_BUS_VOLUME";

    private BroadcastReceiver localReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String className = intent.getStringExtra("tmd");
            TextView tmView = (TextView) findViewById(R.id.tmView);
            tmView.setText(className);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        final AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        final int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        SeekBar walkingBar = (SeekBar) findViewById(R.id.walkingVolumeSlider);
        SeekBar bikingBar = (SeekBar) findViewById(R.id.bikingVolumeSlider);
        SeekBar busBar = (SeekBar) findViewById(R.id.busVolumeSlider);
        walkingBar.setMax(maxVolume);
        bikingBar.setMax(maxVolume);
        busBar.setMax(maxVolume);
        walkingBar.setProgress(sharedPreferences.getInt(KEY_PREF_WALK_VOLUME, maxVolume));
        bikingBar.setProgress(sharedPreferences.getInt(KEY_PREF_BIKE_VOLUME, maxVolume));
        busBar.setProgress(sharedPreferences.getInt(KEY_PREF_BUS_VOLUME, maxVolume));

        walkingBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                SharedPreferences.Editor prefEditor = sharedPreferences.edit();
                prefEditor.putInt(KEY_PREF_WALK_VOLUME, progress);
                prefEditor.apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        bikingBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                SharedPreferences.Editor prefEditor = sharedPreferences.edit();
                prefEditor.putInt(KEY_PREF_BIKE_VOLUME, progress);
                prefEditor.apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        busBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                SharedPreferences.Editor prefEditor = sharedPreferences.edit();
                prefEditor.putInt(KEY_PREF_BUS_VOLUME, progress);
                prefEditor.apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });


        Button tmdButton = (Button) findViewById(R.id.tmdButton);
        tmdButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, TMDActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(localReceiver);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(localReceiver, new IntentFilter("tmd"));
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(localReceiver);
        super.onDestroy();
    }
}
