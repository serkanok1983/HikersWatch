package com.example.android.hikerswatch;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    LocationManager locationManager;
    LocationListener locationListener;
    TextView weatherTextView;

    public class DownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {
            String result = "";
            URL url;
            HttpURLConnection urlConnection = null;
            try {
                url = new URL(urls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream in = urlConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(in);
                int data = reader.read();
                while (data!=-1) {
                    char current = (char) data;
                    result += current;
                    data = reader.read();
                }
                return result;
            } catch (Exception e) {
                makeToast();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            try {
                String message = "";
                JSONObject jsonObject = new JSONObject(result);
                String weatherinfo = jsonObject.getString("weather");
                JSONArray arr = new JSONArray(weatherinfo);
                for (int i=0; i<arr.length(); i++) {
                    JSONObject jsonPart = arr.getJSONObject(i);
                    String main = "";
                    String description = "";
                    main = jsonPart.getString("main");
                    description = jsonPart.getString("description");
                    if (main != "" && description != "") {
                        message += main + ": " + description + "\r\n";
                    }
                }
                JSONObject maininfo = jsonObject.getJSONObject("main");
                String temp = maininfo.getString("temp");
                if (temp != "") {
                    double ctemp = Double.parseDouble(temp) - 273.15;
                    message += String.format("%.2f",ctemp) + " deg C";
                }
                if (message != "") {
                    weatherTextView.setText(message);
                } else {
                    makeToast();
                }
            } catch (JSONException e) {
                makeToast();
            }
        }
    }

    public void makeToast() {
        Toast.makeText(getApplicationContext(), "Hava durumu bulunamadı.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startListening();
        }
    }

    public void startListening() {
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED) {
            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        }
    }

    public void updateLocationInfo(Location location) {
        //Log.i("location", location.toString());
        TextView latitudeTextView = (TextView) findViewById(R.id.latitudeTextView);
        TextView longitudeTextView = (TextView) findViewById(R.id.longitudeTextView);
        TextView accuracyTextView = (TextView) findViewById(R.id.accuracyTextView);
        TextView altitudeTextView = (TextView) findViewById(R.id.altitudeTextView);
        TextView addressTextView = (TextView) findViewById(R.id.addressTextView);
        latitudeTextView.setText(String.format("%.7f",location.getLatitude()));
        longitudeTextView.setText(String.format("%.7f",location.getLongitude()));
        accuracyTextView.setText(String.valueOf(location.getAccuracy()));
        altitudeTextView.setText(String.valueOf(location.getAltitude()));
        Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
        String address = "Adres bulunamadı.";
        try {
            List<Address> listAddresses = geocoder.getFromLocation(location.getLatitude(),location.getLongitude(),1);
            if (listAddresses!=null&&listAddresses.size()>0) {
                //Log.i("Adres:", listAddresses.get(0).toString());
                address = "";
                if (listAddresses.get(0).getAddressLine(0) != null) {
                    address += listAddresses.get(0).getAddressLine(0) + "\n";
                }
                if (listAddresses.get(0).getSubAdminArea() != null) {
                    address += listAddresses.get(0).getSubAdminArea() + ", ";
                }
                if (listAddresses.get(0).getAdminArea() != null) {
                    address += listAddresses.get(0).getAdminArea() + ", ";
                }
                if (listAddresses.get(0).getCountryName() != null) {
                    address += listAddresses.get(0).getCountryName();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        addressTextView.setText(address);

        try {
            DownloadTask task = new DownloadTask();
            task.execute("http://api.openweathermap.org/data/2.5/weather?lat="+String.valueOf(location.getLatitude())+"&lon="+String.valueOf(location.getLongitude())+"&APPID=9626bfcc23a8bf04ae7d33a02cdb0d27");
        } catch (Exception e) {
            e.printStackTrace();
            makeToast();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        weatherTextView = (TextView) findViewById(R.id.weatherTextView);

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                updateLocationInfo(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}
        };

        if (Build.VERSION.SDK_INT < 23) {
            startListening();
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
            } else {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,100000,100000,locationListener);
                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location != null) {
                    updateLocationInfo(location);
                }
            }
        }
    }
}
