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

        atts.addElement(new Attribute("max"));
        atts.addElement(new Attribute("min"));
        atts.addElement(new Attribute("sde"));

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

    double[] window1 = new double[128];
    double[] window2 = new double[128];
    int w1 = 0;
    int w2 = 0;

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor mySensor = event.sensor;

        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0]; //X axis
            float y = event.values[1]; //Y axis
            float z = event.values[2]; //Z axis

            // Euclidean force vector - gravity.
            //double vector = Math.sqrt(Math.pow(x,2) + Math.pow(y,2) + Math.pow(z,2)) - 9.82;

            // Euclidian force vector - gravity measured on the phone (calibration).
            double vector = Math.sqrt(Math.pow(x,2) + Math.pow(y,2) + Math.pow(z,2)) - 9.62;


            if(w1 < 64 && w2 == 0) {
                window1[w1] = vector;
                w1++;
            }
            else {
                window1[w1] = vector;
                window2[w2] = vector;
                w1++;
                w2++;

                if(w1 >= 128) {
                    double max = getMax(window1);
                    double min = getMin(window1);
                    double sde = getSampleStandardDeviation(window1);

                    double[] values = new double[data.numAttributes()];
                    values[0] = max;
                    values[1] = min;
                    values[2] = sde;
                    data.add(new Instance(1.0, values));
                    w1 = 0;
                }
                if(w2 >= 128) {
                    double max = getMax(window2);
                    double min = getMin(window2);
                    double sde = getSampleStandardDeviation(window2);

                    double[] values = new double[data.numAttributes()];
                    values[0] = max;
                    values[1] = min;
                    values[2] = sde;
                    data.add(new Instance(1.0, values));
                    w2 = 0;
                }
            }
        }
    }

    public double getMax(double[] a) {
        double max = 0;
        for(double d : a) {
            if(d > max) max = d;
        }
        return max;
    }

    public double getMin(double[] a) {
        double min = 1000;
        for(double d : a) {
            if(d < min) min = d;
        }
        return min;
    }

    public double getSampleStandardDeviation(double[] a) {
        double mean = 0;
        for(double d : a) {
            mean += d;
        }
        mean = mean / 128;
        double sum = 0;
        for(double d : a) {
            sum += Math.pow(d-mean,2);
        }
        sum = sum / 127; // sum / N-1, which is the sample standard deviation,
                         // as opposed to the population standard deviation which is sum / N.
        double sde = Math.sqrt(sum);
        return sde;
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

            File nFile = new File(root,"motion.txt");
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