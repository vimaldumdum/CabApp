package com.example.cabapp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;

public class customerMap extends FragmentActivity implements OnMapReadyCallback {

    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private GoogleMap mMap;

    private Button requestCab;
    private Button settings;

    private Boolean requested = false;

    FusedLocationProviderClient fusedLocationProviderClient;
    LocationRequest mLocationRequest;
    LocationCallback mLocationCallback;
    Location mLastLocation, pickupLocation;

    private String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.customerMap);
        mapFragment.getMapAsync(this);

        Button logout = findViewById(R.id.logoutCustomer);
        requestCab = findViewById(R.id.requestCab);
        settings = findViewById(R.id.customerSettings);

        fusedLocationProviderClient = new FusedLocationProviderClient(this);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                onLocationChange(locationResult.getLastLocation());
            }
        };

        startLocationUpdates();

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                FirebaseAuth.getInstance().signOut();

                stopLocationUpdates();
                Intent logoutIntent = new Intent(customerMap.this, profileSelection.class);

                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequests");
                GeoFire geoFire = new GeoFire(ref);
                geoFire.removeLocation(userId);

                logoutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(logoutIntent);
                finish();
            }
        });

        requestCab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (requested) {
                    requested = false;
                    geoQuery.removeAllListeners();
                    if (driverLocationRefListener != null)
                        driverLocationRef.removeEventListener(driverLocationRefListener);

                    if (driverFoundId != null) {
                        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("users").child("driver").child(driverFoundId);
                        databaseReference.setValue(true);
                        driverFoundId = null;
                    }
                    driverFound = false;

                    DatabaseReference databaseReference1 = FirebaseDatabase.getInstance().getReference().child("customerRequests");
                    GeoFire geoFire = new GeoFire(databaseReference1);
                    geoFire.removeLocation(userId);

                    if (driverMarker != null)
                        driverMarker.remove();
                    if (pickupMarker != null)
                        pickupMarker.remove();

                    requestCab.setText("Find cab");
                } else {
                    requested = true;
                    requestCab.setText("requesting cab...");

                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequests");

                    GeoFire geoFire = new GeoFire(ref);
                    geoFire.setLocation(userId, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()));

                    LatLng pickUp = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                    pickupLocation = mLastLocation;
                    pickupMarker = mMap.addMarker(new MarkerOptions().position(pickUp).title("Pickup here").icon(BitmapDescriptorFactory.fromResource(R.mipmap.pickup_foreground)));

                    findAvailableDriver(1);
                }


            }
        });

        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(customerMap.this, customerSettings.class);
                startActivity(intent);
                return;
            }
        });

    }

    private Marker pickupMarker;
    private boolean driverFound = false;
    private String driverFoundId;

    GeoQuery geoQuery;
    private void findAvailableDriver(final int radius) {

        final DatabaseReference driverReference = FirebaseDatabase.getInstance().getReference("driversAvailable");

        GeoFire driverGeoFire = new GeoFire(driverReference);
        geoQuery = driverGeoFire.queryAtLocation(new GeoLocation(pickupLocation.getLatitude(), pickupLocation.getLongitude()), radius);
        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (!driverFound && requested) {
                    driverFound = true;
                    driverFoundId = key;
                    Toast.makeText(customerMap.this, "driver found: " + driverFoundId + " " + "radius: " + radius, Toast.LENGTH_SHORT).show();

                    DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("users").child("driver").child(driverFoundId);
                    HashMap map = new HashMap();
                    map.put("customerRideId", userId);
                    driverRef.updateChildren(map);

                    requestCab.setText("Looking for driver Location");
                    getDriverLocation();

                }
            }

            @Override
            public void onKeyExited(String key) {
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
            }

            @Override
            public void onGeoQueryReady() {

                if (!driverFound)
                    findAvailableDriver(radius + 1);
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
            }
        });
    }

    Marker driverMarker;
    private DatabaseReference driverLocationRef;
    private ValueEventListener driverLocationRefListener;

    private void getDriverLocation() {

        driverLocationRef = FirebaseDatabase.getInstance().getReference().child("driversWorking").child(driverFoundId).child("l");
        driverLocationRefListener = driverLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.exists()) {
                    List<Object> map = (List<Object>) snapshot.getValue();

                    double driverLocationLat = 0;
                    double driverLocationLng = 0;

                    requestCab.setText("Driver Location Found");

                    if (map.get(0) != null) {
                        driverLocationLat = Double.parseDouble(map.get(0).toString());
                    }
                    if (map.get(1) != null) {
                        driverLocationLng = Double.parseDouble(map.get(1).toString());
                    }

                    LatLng driverLocationLatLng = new LatLng(driverLocationLat, driverLocationLng);

                    if (driverMarker != null)
                        driverMarker.remove();
                    driverMarker = mMap.addMarker(new MarkerOptions().position(driverLocationLatLng).title("your driver").icon(BitmapDescriptorFactory.fromResource(R.mipmap.texi_foreground)));

                    Location driverLocation = new Location("");
                    driverLocation.setLatitude(driverLocationLat);
                    driverLocation.setLongitude(driverLocationLng);
                    double distance = pickupLocation.distanceTo(driverLocation);

                    if (distance < 100) {
                        requestCab.setText("Driver arrived");
                    } else {
                        Toast.makeText(customerMap.this, "driver is " + String.valueOf(distance) + "m away.", Toast.LENGTH_SHORT).show();
                    }

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        Toast.makeText(this, "map ready", Toast.LENGTH_SHORT).show();
    }

    public void startLocationUpdates() {

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(4000);
        mLocationRequest.setFastestInterval(2000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();

        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        settingsClient.checkLocationSettings(locationSettingsRequest);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle(R.string.title_location_permission)
                        .setMessage(R.string.text_location_permission)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(customerMap.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION);
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            Toast.makeText(this, "Permission failed", Toast.LENGTH_SHORT).show();
            return;
        }

        fusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
    }

    public void onLocationChange(Location location) {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);
        mLastLocation = location;
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(18));

    }

    public void stopLocationUpdates() {

        fusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
    }
}