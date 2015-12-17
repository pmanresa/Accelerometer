package contextawareness.accelerometer.motiondector;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class WeatherClient {

    public static double getWindSpeed(double latitude, double longitude) {
        String responseBody = null;
        InputStream in = null;

        try {
            URL url = new URL("http://api.openweathermap.org/data/2.5/weather?lat=" + latitude + "&lon=" + longitude
                    + "&appid=3f1b17000b81f74862174f3639a013d7");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            in = new BufferedInputStream(urlConnection.getInputStream());
            Scanner scanner = new Scanner(in);
            scanner.useDelimiter("\\A");
            if (scanner.hasNext()) {
                responseBody = scanner.next();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (responseBody == null) {
            return 0D;
        }
        double windSpeed = 0D;
        try {
            JSONObject weatherObject = new JSONObject(responseBody);
            JSONObject windObject = weatherObject.getJSONObject("wind");
            windSpeed = windObject.getDouble("speed");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return windSpeed;
    }

}
