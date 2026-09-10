package com.example.temperatureapp;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.card.MaterialCardView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Locale;

public class ShowTemperature extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Marker currentMarker;
    private MaterialCardView resultCard;
    private TextView resultCityText;
    private TextView resultTempText;
    private EditText searchEditText;
    private ImageButton searchButton;
    private final String apiKey = "396a64f3e4a886f25fa5f405b3259990";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        resultCard = findViewById(R.id.resultCard);
        resultCityText = findViewById(R.id.resultCityText);
        resultTempText = findViewById(R.id.resultTempText);
        searchEditText = findViewById(R.id.searchEditText);
        searchButton = findViewById(R.id.searchButton);

        searchButton.setOnClickListener(v -> searchCity());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void searchCity() {
        String location = searchEditText.getText().toString().trim();
        if (location.isEmpty()) return;

        Geocoder geocoder = new Geocoder(this);
        try {
            List<Address> addressList = geocoder.getFromLocationName(location, 1);
            if (addressList != null && !addressList.isEmpty()) {
                Address address = addressList.get(0);
                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                
                if (mMap != null) {
                    if (currentMarker != null) {
                        currentMarker.setPosition(latLng);
                    } else {
                        currentMarker = mMap.addMarker(new MarkerOptions().position(latLng));
                    }
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10));
                    
                    String cityName = address.getLocality();
                    if (cityName == null) cityName = address.getAdminArea();
                    if (cityName == null) cityName = location;
                    
                    getTemperature(latLng.latitude, latLng.longitude, cityName);
                }

                // Hide keyboard
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
                }
            } else {
                Toast.makeText(this, "City not found", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Search error", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setOnMapClickListener(latLng -> {
            // Add or move marker
            if (currentMarker != null) {
                currentMarker.setPosition(latLng);
            } else {
                currentMarker = mMap.addMarker(new MarkerOptions().position(latLng));
            }
            
            String cityName = getCityName(latLng.latitude, latLng.longitude);
            getTemperature(latLng.latitude, latLng.longitude, cityName);
        });
    }

    private String getCityName(double lat, double lon) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses != null && !addresses.isEmpty()) {
                String city = addresses.get(0).getLocality();
                if (city == null) {
                    city = addresses.get(0).getAdminArea();
                }
                return city != null ? city : getString(R.string.unknown_location);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return getString(R.string.unknown_location);
    }

    private void getTemperature(double lat, double lon, String cityName) {
        new Thread(() -> {
            try {
                String apiUrl = "https://api.openweathermap.org/data/2.5/weather?lat="
                        + lat + "&lon=" + lon + "&appid=" + apiKey + "&units=metric";

                URL url = new URL(apiUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()));

                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();

                JSONObject jsonObject = new JSONObject(response.toString());
                JSONObject main = jsonObject.getJSONObject("main");
                double temperature = main.getDouble("temp");

                runOnUiThread(() -> {
                    resultCityText.setText(getString(R.string.city_label, cityName));
                    resultTempText.setText(getString(R.string.temp_label, temperature));
                    resultCard.setVisibility(View.VISIBLE);
                });

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(ShowTemperature.this, R.string.error_get_temperature, Toast.LENGTH_SHORT).show());
                e.printStackTrace();
            }
        }).start();
    }
}