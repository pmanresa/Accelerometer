package contextawareness.accelerometer.motiondector;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
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
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

import java.util.Arrays;

public class SensorService extends Service implements SensorEventListener, LocationListener {

    public static final String SERVICE_START_STOP_COMMAND = "SERVICE_START_STOP_COMMAND";
    public static final int SERVICE_START = 1;
    public static final int SERVICE_STOP = 2;
    private boolean running = false;

    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private MediaRecorder mediaRecorder;
    private LocationManager locationManager;

    FastVector atts;
    Instances data;

    double[] window1acc = new double[128];
    double[] window2acc = new double[128];
    double[] window1mic = new double[128];
    double[] window2mic = new double[128];
    double[] window1gps = new double[128];
    double[] window2gps = new double[128];
    int w1 = 0;
    int w2 = 0;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        int command = intent.getExtras().getInt(SERVICE_START_STOP_COMMAND);

        if (command == SERVICE_START && !running) {
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setOutputFile("/dev/null");
            try {
                mediaRecorder.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mediaRecorder.start();

            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

            // Initializing sensorManager and accelerometer
            senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

            //Initializing arff variables
            atts = new FastVector();

            atts.addElement(new Attribute("accMax"));
            atts.addElement(new Attribute("accMin"));
            atts.addElement(new Attribute("accSde"));
            atts.addElement(new Attribute("accMean"));
            atts.addElement(new Attribute("accMedian"));
            atts.addElement(new Attribute("micMax"));
            atts.addElement(new Attribute("micMin"));
            atts.addElement(new Attribute("micSde"));
            atts.addElement(new Attribute("micMean"));
            atts.addElement(new Attribute("micMedian"));
            atts.addElement(new Attribute("speedMax"));
            atts.addElement(new Attribute("speedMin"));
            atts.addElement(new Attribute("speedSde"));
            atts.addElement(new Attribute("speedMean"));
            atts.addElement(new Attribute("speedMedian"));

            FastVector classVector = new FastVector(3);
            classVector.addElement("Walking");
            classVector.addElement("Bus");
            classVector.addElement("Biking");
            Attribute ClassAttribute = new Attribute("theClass", classVector);

            atts.addElement(ClassAttribute);

            data = new Instances("MyRelation", atts, 0);

            Notification notification = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("SensorService")
                    .setContentText("Running")
                    .build();
            startForeground(42, notification);

            running = true;

            Toast.makeText(this, "Service Started", Toast.LENGTH_SHORT).show();
        } else if (command == SERVICE_STOP && running) {
            stopForeground(true);
            shutdown();
            running = false;
        }

        return START_NOT_STICKY;
    }

    private void shutdown() {

        Toast.makeText(this, "Service Stopped", Toast.LENGTH_SHORT).show();

        mediaRecorder.stop();
        mediaRecorder.release();

        try {

            File root = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "MotionDetector");
            if(!root.exists()) {
                root.mkdirs();
            }

            File nFile = new File(root,"motion"+System.currentTimeMillis()+".txt");
            FileWriter fW = new FileWriter(nFile,true);
            BufferedWriter writer = new BufferedWriter(fW);
            writer.write(data.toString());
            writer.flush();
            writer.close();

            /*ArffSaver saver = new ArffSaver();
            saver.setInstances(data);
            saver.setFile(new File("")); // Modify arff file saved destination to execute program
            saver.writeBatch();*/

            //senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            senSensorManager.unregisterListener(this);

        } catch (IOException e){
            Context context = getApplicationContext();
            CharSequence text = "Data saved unsuccessfully. Check arff file destination.";
            int duration = Toast.LENGTH_SHORT;

            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        }

        stopSelf();
    }

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
                window1acc[w1] = vector;
                window1mic[w1] = (double) mediaRecorder.getMaxAmplitude();
                double speed = 0D;
                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location != null) {
                    speed = location.getSpeed();
                }
                window1gps[w1] = speed;
                w1++;
            }
            else {
                window1acc[w1] = vector;
                window2acc[w2] = vector;
                double maxAmplitude = (double) mediaRecorder.getMaxAmplitude();
                window1mic[w1] = maxAmplitude;
                window2mic[w2] = maxAmplitude;
                double speed = 0D;
                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location != null) {
                    speed = location.getSpeed();
                }
                window1gps[w1] = speed;
                window2gps[w2] = speed;
                w1++;
                w2++;

                if(w1 >= 128) {
                    double maxAcc = getMax(window1acc);
                    double minAcc = getMin(window1acc);
                    double sdeAcc = getSampleStandardDeviation(window1acc);
                    double meanAcc = getMean(window1acc);
                    double medianAcc = getMedian(window1acc);

                    double maxMic = getMax(window1mic);
                    double minMic = getMin(window1mic);
                    double sdeMic = getSampleStandardDeviation(window1mic);
                    double meanMic = getMean(window1mic);
                    double medianMic = getMedian(window1mic);

                    double maxGps = getMax(window1gps);
                    double minGps = getMin(window1gps);
                    double sdeGps = getSampleStandardDeviation(window1gps);
                    double meanGps = getMean(window1gps);
                    double medianGps = getMedian(window1gps);

                    double[] values = new double[data.numAttributes()];
                    values[0] = maxAcc;
                    values[1] = minAcc;
                    values[2] = sdeAcc;
                    values[3] = meanAcc;
                    values[4] = medianAcc;
                    values[5] = maxMic;
                    values[6] = minMic;
                    values[7] = sdeMic;
                    values[8] = meanMic;
                    values[9] = medianMic;
                    values[10] = maxGps;
                    values[11] = minGps;
                    values[12] = sdeGps;
                    values[13] = meanGps;
                    values[14] = medianGps;
                    data.add(new Instance(1.0, values));
                    w1 = 0;
                }
                if(w2 >= 128) {
                    double maxAcc = getMax(window2acc);
                    double minAcc = getMin(window2acc);
                    double sdeAcc = getSampleStandardDeviation(window2acc);
                    double meanAcc = getMean(window2acc);
                    double medianAcc = getMedian(window2acc);

                    double maxMic = getMax(window2mic);
                    double minMic = getMin(window2mic);
                    double sdeMic = getSampleStandardDeviation(window2mic);
                    double meanMic = getMean(window2mic);
                    double medianMic = getMedian(window2mic);

                    double maxGps = getMax(window2gps);
                    double minGps = getMin(window2gps);
                    double sdeGps = getSampleStandardDeviation(window2gps);
                    double meanGps = getMean(window2gps);
                    double medianGps = getMedian(window2gps);

                    double[] values = new double[data.numAttributes()];
                    values[0] = maxAcc;
                    values[1] = minAcc;
                    values[2] = sdeAcc;
                    values[3] = meanAcc;
                    values[4] = medianAcc;
                    values[5] = maxMic;
                    values[6] = minMic;
                    values[7] = sdeMic;
                    values[8] = meanMic;
                    values[9] = medianMic;
                    values[10] = maxGps;
                    values[11] = minGps;
                    values[12] = sdeGps;
                    values[13] = meanGps;
                    values[14] = medianGps;
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

    public double getMean(double[] a) {
        int ret = 0;
        for(double d : a) {
            ret += d;
        }
        return ret;
    }

    public double getMedian(double[] a) {
        Arrays.sort(a);
        return a[63];
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    public SensorService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onLocationChanged(Location location) { }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) { }

    @Override
    public void onProviderEnabled(String provider) { }

    @Override
    public void onProviderDisabled(String provider) { }
}
