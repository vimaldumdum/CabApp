package com.example.cabapp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.api.Status;
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
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class customerMap extends FragmentActivity implements OnMapReadyCallback {

    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private GoogleMap mMap;

    private Button requestCab;
    private Button settings;

    private Boolean requested = false;

    private String destination = "ABC";
    private LatLng destinationLatLng;

    FusedLocationProviderClient fusedLocationProviderClient;
    LocationRequest mLocationRequest;
    LocationCallback mLocationCallback;
    Location mLastLocation, pickupLocation;

    private ImageView driverProfileImage;
    private TextView driverName, driverPhone, driverCar;
    private LinearLayout driverInfo;
    private RadioGroup radioGroup;
    LinearLayout radioLayout;

    private String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    private String service = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.customerMap);
        mapFragment.getMapAsync(this);

        String apiKey = getString(R.string.api_key);
        Places.initialize(getApplicationContext(), apiKey);
        PlacesClient placesClient = Places.createClient(this);

        destinationLatLng = new LatLng(0.0, 0.0);

        Button logout = findViewById(R.id.logoutCustomer);
        requestCab = findViewById(R.id.requestCab);
        settings = findViewById(R.id.customerSettings);

        driverProfileImage = (ImageView) findViewById(R.id.driverProfileImage);
        driverName = (TextView) findViewById(R.id.driverName);
        driverPhone = (TextView) findViewById(R.id.driverPhone);
        driverCar = (TextView) findViewById(R.id.driverCar);
        driverInfo = (LinearLayout) findViewById(R.id.driverInfo);
        radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        radioLayout = (LinearLayout) findViewById(R.id.radioLayout);

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
                    endRide();
                } else {
                    requested = true;
                    requestCab.setText("requesting cab...");

                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequests");

                    GeoFire geoFire = new GeoFire(ref);
                    geoFire.setLocation(userId, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()));

                    LatLng pickUp = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                    pickupLocation = mLastLocation;
                    pickupMarker = mMap.addMarker(new MarkerOptions().position(pickUp).title("Pickup here").icon(BitmapDescriptorFactory.fromResource(R.mipmap.pickup_foreground)));

                    int selected = radioGroup.getCheckedRadioButtonId();
                    final RadioButton radioButton = (RadioButton) findViewById(selected);

                    if (radioButton.getText() != null) {
                        service = radioButton.getText().toString();
                    }

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

        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME));

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                destination = place.getName().toString();
                destinationLatLng = place.getLatLng();
            }

            @Override
            public void onError(Status status) {
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

                    DatabaseReference mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("users").child("driver").child(key);
                    mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists() == true && snapshot.getChildrenCount() > 0) {
                                Map<String, Object> mapService = (Map<String, Object>) snapshot.getValue();
                                if (driverFound)
                                    return;
                                if (mapService.get("service").equals(service) && !driverFound) {
                                    driverFound = true;
                                    driverFoundId = snapshot.getKey();
                                    Toast.makeText(customerMap.this, "driver found: " + driverFoundId + " " + "radius: " + radius, Toast.LENGTH_SHORT).show();

                                    DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("users").child("driver").child(driverFoundId).child("customerRequest");
                                    HashMap map = new HashMap();
                                    map.put("customerRideId", userId);
                                    map.put("destination", destination);
                                    map.put("destinationLat", destinationLatLng.latitude);
                                    map.put("destinationLng", destinationLatLng.longitude);
                                    driverRef.updateChildren(map);

                                    requestCab.setText("Looking for driver Location");
                                    radioLayout.setVisibility(View.GONE);
                                    driveEnded();
                                    getAssignedDriverInfo();
                                    getDriverLocation();
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });


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

    public void getAssignedDriverInfo() {

        driverInfo.setVisibility(View.VISIBLE);
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("users").child("driver").child(driverFoundId);
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() == true && snapshot.getChildrenCount() > 0) {

                    Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                    String name, car, phone;
                    if (map.get("name") != null) {
                        driverName.setText("Driver Name: " + map.get("name").toString());
                    }
                    if (map.get("phone") != null) {
                        driverPhone.setText("Driver Phone: " + map.get("phone").toString());
                    }
                    if (map.get("profilePicture") != null) {
                        String imageUri = map.get("profilePicture").toString();
                        Glide.with(customerMap.this).load(imageUri).into(driverProfileImage);
                    }
                    if (map.get("car") != null) {
                        driverCar.setText("Driver Car: " + map.get("car").toString());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

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

    private DatabaseReference driveEndedRef;
    private ValueEventListener driverEndedRefListener;

    private void driveEnded() {

        driveEndedRef = FirebaseDatabase.getInstance().getReference().child("users").child("driver").child(driverFoundId).child("customerRequest");
        driverEndedRefListener = driveEndedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.exists()) {

                } else {
                    endRide();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void endRide() {
        requested = false;

        geoQuery.removeAllListeners();
        if (driverLocationRefListener != null)
            driverLocationRef.removeEventListener(driverLocationRefListener);
        if (driverEndedRefListener != null)
            driveEndedRef.removeEventListener(driverEndedRefListener);

        if (driverFoundId != null) {
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("users").child("driver").child(driverFoundId).child("customerRequest");
            databaseReference.removeValue();
            Log.d("removeLog", "sdf");
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
        driverInfo.setVisibility(View.INVISIBLE);
        requestCab.setText("Find cab");

        driverInfo.setVisibility(View.GONE);
        radioLayout.setVisibility(View.VISIBLE);
        driverName.setText("");
        driverCar.setText("");
        driverPhone.setText("");
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