package contextawareness.accelerometer.motiondector;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;

import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

public class ClassifyService extends Service implements SensorEventListener, LocationListener {

    public static final String SERVICE_START_STOP_COMMAND = "SERVICE_START_STOP_COMMAND";
    public static final int SERVICE_START = 1;
    public static final int SERVICE_STOP = 2;
    private boolean running = false;

    J48 classifier;
    AssetManager aM;

    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private MediaRecorder mediaRecorder;
    private LocationManager locationManager;

    private double latestWindSpeed = 0;
    private long lastWeatherUpdateTime = 0;
    private static final double windThreshold = 20D;
    private static final int weatherUpdateInterval = 360000;
    private static final int micThreshold = 7000;

    private long lastUpdate = 0;

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

            try{
                aM = this.getAssets();
                ObjectInputStream ois = new ObjectInputStream(aM.open("classifier.model"));
                classifier = (J48) ois.readObject();
                ois.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }

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

            Notification notification = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("ClassifyService")
                    .setContentText("Running")
                    .build();
            startForeground(40, notification);



            running = true;

        } else if (command == SERVICE_STOP && running) {
            stopForeground(true);
            mediaRecorder.stop();
            mediaRecorder.release();
            senSensorManager.unregisterListener(this);
            stopSelf();
            running = false;
        }

        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException();
    }

    public ClassifyService() {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor mySensor = event.sensor;

        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastWeatherUpdateTime > weatherUpdateInterval) {
                updateWindSpeedAsync();
                lastWeatherUpdateTime = currentTime;
            }

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
                window1gps[w1] = (double) speed;
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
                    Instance instance = new Instance(1.0, values);

                    Instances dataUnlabeled = new Instances("TestInstances", atts, 0);
                    dataUnlabeled.add(instance);
                    dataUnlabeled.setClassIndex(dataUnlabeled.numAttributes() - 1);
                    try {
                        double value = classifier.classifyInstance(dataUnlabeled.firstInstance());
                        String className = dataUnlabeled.classAttribute().value((int) value);
                        setVolume(className, medianMic);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
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
                    Instance instance = new Instance(1.0, values);

                    Instances dataUnlabeled = new Instances("TestInstances", atts, 0);
                    dataUnlabeled.add(instance);
                    dataUnlabeled.setClassIndex(dataUnlabeled.numAttributes() - 1);
                    try {
                        double value = classifier.classifyInstance(dataUnlabeled.firstInstance());
                        String className = dataUnlabeled.classAttribute().value((int)value);
                        setVolume(className, medianMic);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
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
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    @Override
    public void onLocationChanged(Location location) { }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) { }

    @Override
    public void onProviderEnabled(String provider) { }

    @Override
    public void onProviderDisabled(String provider) { }

    /**
     * Returns volume for given transport mode. tm options are:
     * MainActivity.KEY_PREF_WALK_VOLUME
     * MainActivity.KEY_PREF_BIKE_VOLUME
     * MainActivity.KEY_PREF_BUS_VOLUME
     * @param tm transport mode
     * @return the set volume preference or 2 if non is defined
     */
    private int getVolumePreference(String tm) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPreferences.getInt(tm, 2);
    }

    private void setVolume(String className, double medianMic) {
        String volumeKey;
        boolean isWindConsidered = false;
        if ("Walking".equals(className)) {
            volumeKey = MainActivity.KEY_PREF_WALK_VOLUME;
            isWindConsidered = true;
        } else if ("Bus".equals(className)) {
            volumeKey = MainActivity.KEY_PREF_BUS_VOLUME;
        } else if ("Biking".equals(className)) {
            volumeKey = MainActivity.KEY_PREF_BIKE_VOLUME;
            isWindConsidered = true;
        } else {
            return;
        }
        int volume = getVolumePreference(volumeKey);

        if(isWindConsidered && latestWindSpeed > windThreshold) {
            volume = volume + 2;
        } else if (isWindConsidered && latestWindSpeed > windThreshold/2) {
            volume = volume + 1;
        }
        if (medianMic > micThreshold) {
            volume = volume + 2;
        } else if (medianMic > micThreshold/2) {
            volume = volume + 1;
        }

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,volume,0);

        // Send broadcast to main with the classname
        Intent newIntent = new Intent("tmd");
        newIntent.putExtra("tmd",className);
        LocalBroadcastManager.getInstance(this).sendBroadcast(newIntent);
    }

    private void updateWindSpeedAsync() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location != null) {
                    latestWindSpeed = WeatherClient.getWindSpeed(location.getLatitude(), location.getLongitude());
                }

            }
        }).start();
    }

}
