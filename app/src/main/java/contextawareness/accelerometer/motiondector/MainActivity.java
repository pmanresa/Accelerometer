package contextawareness.accelerometer.motiondector;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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


public class MainActivity extends Activity implements SensorEventListener {

    private SensorManager   senSensorManager;
    private Sensor          senAccelerometer;

    private long            lastUpdate = 0;

    FastVector              atts;
    Instances               data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initializing sensorManager and accelerometer
        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        //Initializing arff variables
        atts = new FastVector();
        atts.addElement(new Attribute("x"));
        atts.addElement(new Attribute("y"));
        atts.addElement(new Attribute("z"));

        data = new Instances("MyRelation",atts,0);

    }

    protected void onPause() {
        super.onPause();
        senSensorManager.unregisterListener(this);
    }

    protected void onResume() {
        super.onResume();
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor mySensor = event.sensor;

        // To get a real accelerometer measurement, contribution of the force of gravity must be removed from the accelerometer data
        float[] gravity = new float[3];
        final float alpha = (float)0.8;

        double[] values = new double[data.numAttributes()];

        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // Isolate the force of gravity with the low-pass filter.
            gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
            gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
            gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

            // Remove the gravity contribution with the high-pass filter.
            float x = event.values[0] - gravity[0]; //X axis
            float y = event.values[1] - gravity[1]; //Y axis
            float z = event.values[2] - gravity[2]; //Z axis

            long curTime = System.currentTimeMillis();

            if ((curTime - lastUpdate) > 100) { //If it has been 100miliseconds since the last sensor measurement update
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;

                //float speed = Math.abs(x + y + z - last_x - last_y - last_z)/ diffTime * 10000; // m/s

                // Saving attributes
                values[0] = (double)x;
                values[1] = (double)y;
                values[2] = (double)z;

                // Loading attributes to data
                data.add(new Instance(1.0, values));
            }

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {

            File root = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "MotionDetector");
            if(!root.exists()) {
                root.mkdirs();
            }

            File nFile = new File(root,"motion.arff");
            FileWriter fW = new FileWriter(nFile,true);
            BufferedWriter writer = new BufferedWriter(fW);
            writer.write(data.toString());
            writer.flush();
            writer.close();

            /*ArffSaver saver = new ArffSaver();
            saver.setInstances(data);
            saver.setFile(new File("")); // Modify arff file saved destination to execute program
            saver.writeBatch();*/

        }catch (IOException e){
            Context context = getApplicationContext();
            CharSequence text = "Data saved unsuccessfully. Check arff file destination.";
            int duration = Toast.LENGTH_SHORT;

            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        }
    }
}
