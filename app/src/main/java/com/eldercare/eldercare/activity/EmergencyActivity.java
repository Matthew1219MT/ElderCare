package com.eldercare.eldercare.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.eldercare.eldercare.R;
import com.eldercare.eldercare.databinding.ActivityEmergencyBinding;
import com.eldercare.eldercare.model.Hospital;
import com.eldercare.eldercare.view.BaseActivity;
import com.eldercare.eldercare.view.V_HomePage;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EmergencyActivity extends BaseActivity implements OnMapReadyCallback {
    private ActivityEmergencyBinding binding;

    private FusedLocationProviderClient fusedLocationClient;
    LocationRequest locationRequest;
    LocationCallback locationCallback;
    private GoogleMap map;
    PlacesClient placesClient;
    private LatLng userLocation;
    private Marker userMarker;
    private Marker hospitalMarker;
    private final List<Hospital> hospitalList = new ArrayList<>();
    private int selectedIndex = -1;
    private FloatingActionButton fabHome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityEmergencyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION
        }, 1);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "AIzaSyBVQV99Ofwb0CcfrugUUXYWX-jEA_4g2Lk");
        }
        placesClient = Places.createClient(this);

        Button requestButton = findViewById(R.id.button); // your horizontal button
        requestButton.setOnClickListener(v -> {
            if (hospitalMarker == null) {
                Toast.makeText(this, "No hospital selected!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Find the selected hospital from your hospitalList
            Hospital selected = null;
            LatLng markerPos = hospitalMarker.getPosition();
            for (Hospital h : hospitalList) {
                if (Math.abs(h.getLat() - markerPos.latitude) < 0.0001 &&
                        Math.abs(h.getLng() - markerPos.longitude) < 0.0001) {
                    selected = h;
                    break;
                }
            }

            if (selected == null) {
                Toast.makeText(this, "Selected hospital not found in the list!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selected.getContactNbr() == null || selected.getContactNbr().isEmpty()) {
                fetchHospitalDetails(selected);
            } else {
                makePhoneCall(selected.getContactNbr());
            }
        });

        fabHome = findViewById(R.id.fabHome);

        fabHome.setOnClickListener(v -> {
            Intent intent = new Intent(EmergencyActivity.this, V_HomePage.class);
            startActivity(intent);
        });

    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {

        map = googleMap;

        locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 20000
        ).build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Log.d("DEBUG_LOCATION", "Got last known location first");
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    Log.d("DEBUG_LOCATION", "Lat: " + location.getLatitude() + ", Lng: " + location.getLongitude());
                    userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    map.clear();
                    selectedIndex = -1;
                    userMarker = map.addMarker(new MarkerOptions()
                            .position(userLocation)
                            .title("You are here"));
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 14));
                    fetchNearbyHospitals(userLocation);
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Log.d("DEBUG_LOCATION", "Ready to request loc update.");
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());

    }

    private void fetchNearbyHospitals(LatLng location) {

        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                "?location=" + location.latitude + "," + location.longitude +
                "&radius=5000" +
                "&type=hospital" +
                "&key=AIzaSyBVQV99Ofwb0CcfrugUUXYWX-jEA_4g2Lk";

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Response response) throws IOException {
                String json = response.body().string();
                try {
                    JSONObject jsonObject = new JSONObject(json);
                    JSONArray results = jsonObject.getJSONArray("results");
                    hospitalList.clear();
                    for(int i=0; i<results.length(); ++i){
                        JSONObject place = results.getJSONObject(i);
                        String placeId = place.getString("place_id");
                        JSONObject geometry = place.getJSONObject("geometry");
                        JSONObject loc = geometry.getJSONObject("location");

                        double lat = loc.getDouble("lat");
                        double lng = loc.getDouble("lng");
                        String name = place.optString("name", "Unknown");
                        String addr = place.optString("vicinity", "No address");

                        hospitalList.add(new Hospital(placeId, name, addr, lat, lng));
                    }

                    runOnUiThread(() -> showHospitalCards());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void fetchHospitalDetails(Hospital hospital) {
        String placeId = hospital.getPlace_id(); // you need to save place_id from Nearby Search
        String url = "https://maps.googleapis.com/maps/api/place/details/json" +
                "?place_id=" + placeId +
                "&fields=name,formatted_phone_number" +
                "&key=AIzaSyBVQV99Ofwb0CcfrugUUXYWX-jEA_4g2Lk";

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request req, IOException e) { e.printStackTrace(); }

            @Override
            public void onResponse(Response response) throws IOException {
                String json = response.body().string();
                try {
                    JSONObject obj = new JSONObject(json);
                    JSONObject result = obj.getJSONObject("result");
                    String phone = result.optString("formatted_phone_number", null);
                    hospital.setContactNbr(phone);
                    if(!phone.isEmpty()){
                        makePhoneCall(phone);
                    } else {
                        Toast.makeText(EmergencyActivity.this, "No contact number for this hospital!", Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(EmergencyActivity.this, "Failed to fetch hospital details", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    private void showHospitalCards() {
        binding.cardContainer.removeAllViews();

        for (int i = 0; i < hospitalList.size(); i++) {
            Hospital hospital = hospitalList.get(i);

            CardView card = new CardView(this);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            cardParams.setMargins(0, 0, 0, 16);
            card.setLayoutParams(cardParams);
            card.setRadius(12f);
            card.setCardElevation(8f);

            if (i == selectedIndex) {
                card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.eldercare_primary));
            } else {
                card.setCardBackgroundColor(Color.WHITE);
            }

            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(24, 24, 24, 24);

            TextView nameView = new TextView(this);
            nameView.setText(hospital.getName());
            nameView.setTextSize(18f);
            nameView.setTypeface(null, Typeface.BOLD);

            TextView addressView = new TextView(this);
            addressView.setText(hospital.getAddress());
            addressView.setTextSize(14f);

            Button button = new Button(this);
            button.setText("View on Map");
            button.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.teal_700));
            button.setTextColor(Color.WHITE);
            int finalI = i;
            button.setOnClickListener(v -> {
                selectedIndex = finalI;
                focusHospitalOnMap(finalI);
                showHospitalCards();
            });

            layout.addView(nameView);
            layout.addView(addressView);
            layout.addView(button);
            card.addView(layout);

            binding.cardContainer.addView(card);
        }
    }

    private void focusHospitalOnMap(int index) {
        Hospital hospital = hospitalList.get(index);
        if (hospital == null || map == null || userLocation == null) return;

        // remove old hospital marker but keep the user marker
        if (hospitalMarker != null) {
            hospitalMarker.remove();
        }

        LatLng hospitalLoc = new LatLng(hospital.getLat(), hospital.getLng());
        hospitalMarker = map.addMarker(new MarkerOptions()
                .position(hospitalLoc)
                .title(hospital.getName())
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

        // adjust camera to show both
        LatLngBounds bounds = new LatLngBounds.Builder()
                .include(userLocation)
                .include(hospitalLoc)
                .build();
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 200));

        Toast.makeText(this, "Focusing on " + hospital.getName(), Toast.LENGTH_SHORT).show();
    }

    private void makePhoneCall(String nbr){
        Intent callIntent = new Intent(Intent.ACTION_DIAL);
        callIntent.setData(Uri.parse("tel:" + nbr));
        startActivity(callIntent);
    }
}
