package com.example.cabapp;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

public class specificHistoryActivity extends AppCompatActivity implements OnMapReadyCallback {

    private TextView route, distance, name, phone, date;
    private ImageView profileImage;
    private GoogleMap mMap;
    private SupportMapFragment mapFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_specific_history);

        route = (TextView) findViewById(R.id.route);
        distance = (TextView) findViewById(R.id.distance);
        name = (TextView) findViewById(R.id.name);
        phone = (TextView) findViewById(R.id.phone);
        date = (TextView) findViewById(R.id.date);

        profileImage = (ImageView) findViewById(R.id.historyProfileImage);

        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.historyMap);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

    }
}