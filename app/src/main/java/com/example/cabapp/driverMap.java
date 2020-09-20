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
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.bumptech.glide.Glide;
import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
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
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class driverMap extends FragmentActivity implements OnMapReadyCallback, RoutingListener {

    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private GoogleMap mMap;
    LocationRequest mLocationRequest;
    LocationCallback mLocationCallback;
    FusedLocationProviderClient fusedLocationProviderClient;
    private Location mLastLocation;

    private List<Polyline> polylines;
    private static final int[] COLORS = new int[]{R.color.primary_dark_material_light};


    private LinearLayout customerInfo;
    private ImageView customerProfileImage;
    private TextView customerName, customerPhone, customerDestination;
    private Button settingsButton, rideStatus, driverHistory;
    private Switch workingSwitch;

    public String user = FirebaseAuth.getInstance().getCurrentUser().getUid();
    public String assignedCustomerId = "";
    public String destination = "";
    public Boolean working = false;
    public Boolean prevWorking = false;
    private float rideDistance;

    private LatLng destinationLatLng, customerPickupLatLng;

    private int status = 0;

    private Button logoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        final SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        logoutButton = findViewById(R.id.logoutButton);
        settingsButton = findViewById(R.id.driverSettings);


        polylines = new ArrayList<>();

        customerInfo = (LinearLayout) findViewById(R.id.customerInfo);
        customerProfileImage = (ImageView) findViewById(R.id.customerProfileImage);
        customerName = (TextView) findViewById(R.id.customerName);
        customerPhone = (TextView) findViewById(R.id.customerPhone);
        customerDestination = (TextView) findViewById(R.id.customerDestination);
        rideStatus = (Button) findViewById(R.id.rideStatus);
        driverHistory = (Button) findViewById(R.id.driverHistory);
        workingSwitch = (Switch) findViewById(R.id.workingSwitch);

        fusedLocationProviderClient = new FusedLocationProviderClient(this);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                onLocationChanged(locationResult.getLastLocation());
            }
        };

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth mAuth = FirebaseAuth.getInstance();
                stopLocationUpdates();
                stopGeoFireWorking();
                stopGeoFireAvailable();
                mAuth.signOut();
                Intent logoutIntent = new Intent(driverMap.this, profileSelection.class);
                logoutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(logoutIntent);
                finish();
            }
        });

        rideStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (status) {
                    case 1:
                        status = 2;
                        erasePolyLines();
                        if (destinationLatLng.latitude != 0.0 && destinationLatLng.longitude != 0.0) {
                            drawRouteToPickup(destinationLatLng);
                        }
                        rideStatus.setText("Ride complete");
                        break;
                    case 2:
                        updateHistory();
                        endRide();
                        break;
                }
            }
        });

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(driverMap.this, driverSettings.class);
                startActivity(intent);
                return;
            }
        });

        driverHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(driverMap.this, history.class);
                intent.putExtra("user", "driver");
                startActivity(intent);
                return;
            }
        });

        workingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    startLocationUpdates();
                } else {
                    if (working)
                        stopGeoFireWorking();
                    else
                        stopGeoFireAvailable();
                    stopLocationUpdates();
                }
            }
        });

        getAssignedCustomer();
        checkIfWorking();
    }

    private void checkIfWorking() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("driversAvailable");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        if (child.getKey().equals(user)) {
                            workingSwitch.setChecked(true);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        ref = FirebaseDatabase.getInstance().getReference().child("driversWorking");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        if (child.getKey().equals(user)) {
                            workingSwitch.setChecked(true);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private DatabaseReference ref;
    private ValueEventListener refListener;

    private void getAssignedCustomer() {

        ref = FirebaseDatabase.getInstance().getReference().child("users").child("driver").child(user).child("customerRequest");
        refListener = ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.exists()) {
                    Map<String, Object> mapC = (Map<String, Object>) snapshot.getValue();
                    status = 1;
                    assignedCustomerId = mapC.get("customerRideId").toString();
                    destination = mapC.get("destination").toString();
                    working = true;
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("users").child("customer").child(assignedCustomerId);
                    ref.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Map<String, Object> map = (Map<String, Object>) snapshot.getValue();

                            if (map.get("name") != null) {
                                customerName.setText("Customer Name: " + map.get("name").toString());
                            }
                            if (map.get("phone") != null) {
                                customerPhone.setText("Customer Phone: " + map.get("phone").toString());
                            }
                            if (map.get("profilePicture") != null) {
                                String imageUri = map.get("profilePicture").toString();
                                Glide.with(driverMap.this).load(imageUri).into(customerProfileImage);
                            }
                            customerDestination.setText("Destination: " + destination);
                            customerInfo.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                    getAssignedCustomerDestination();
                    getAssignedCustomerLocation();
                } else {
                    working = false;
                    endRide();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public void getAssignedCustomerDestination() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("users").child("driver").child(userId).child("customerRequest");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                    Double destinationLt = 0.0, destinationLn = 0.0;
                    if (map.get("destinationLat") != null) {
                        destinationLt = Double.valueOf(map.get("destinationLat").toString());
                    }
                    if (map.get("destinationLng") != null) {
                        destinationLn = Double.valueOf(map.get("destinationLng").toString());
                        destinationLatLng = new LatLng(destinationLt, destinationLn);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private void endRide() {
        rideStatus.setText("Picked customer");
        rideDistance = 0;
        erasePolyLines();

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("users").child("driver").child(userId).child("customerRequest");
        driverRef.removeValue();

        DatabaseReference databaseReference1 = FirebaseDatabase.getInstance().getReference().child("customerRequests");
        GeoFire geoFire = new GeoFire(databaseReference1);
        geoFire.removeLocation(assignedCustomerId);
        assignedCustomerId = "";

        if (customerLocationMarker != null)
            customerLocationMarker.remove();

    }

    private void updateHistory() {

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("users").child("driver").child(userId).child("history");
        DatabaseReference customerRef = FirebaseDatabase.getInstance().getReference().child("users").child("customer").child(assignedCustomerId).child("history");
        DatabaseReference historyRef = FirebaseDatabase.getInstance().getReference().child("history");

        String requestId = historyRef.push().getKey();

        driverRef.child(requestId).setValue(true);
        customerRef.child(requestId).setValue(true);

        HashMap map = new HashMap();
        map.put("driver", userId);
        map.put("customer", assignedCustomerId);
        map.put("timestamp", getTimeStamp());
        map.put("destination", destination);
        map.put("location/from/lat", customerPickupLatLng.latitude);
        map.put("location/from/lng", customerPickupLatLng.longitude);
        map.put("location/to/lat", destinationLatLng.latitude);
        map.put("location/to/lng", destinationLatLng.longitude);
        map.put("distance", rideDistance);
        map.put("rating", 0);

        historyRef.child(requestId).updateChildren(map);
    }

    private Long getTimeStamp() {
        Long time = System.currentTimeMillis() / 1000;
        return time;
    }

    Marker customerLocationMarker;

    private void getAssignedCustomerLocation() {

        DatabaseReference assignedCustomerLocationRef = FirebaseDatabase.getInstance().getReference().child("customerRequests").child(assignedCustomerId).child("l");
        assignedCustomerLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    List<Object> map = (List<Object>) snapshot.getValue();

                    double customerLocationLat = 0;
                    double customerLocationLng = 0;

                    if (map.get(0) != null)
                        customerLocationLat = Double.parseDouble(map.get(0).toString());
                    if (map.get(1) != null)
                        customerLocationLng = Double.parseDouble(map.get(1).toString());

                    customerPickupLatLng = new LatLng(customerLocationLat, customerLocationLng);

                    if (customerLocationMarker != null)
                        customerLocationMarker.remove();
                    customerLocationMarker = mMap.addMarker(new MarkerOptions().position(customerPickupLatLng).title("pickup here").icon(BitmapDescriptorFactory.fromResource(R.mipmap.pickup_foreground)));
                    drawRouteToPickup(customerPickupLatLng);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void drawRouteToPickup(LatLng pickup) {
        Routing routing = new Routing.Builder()
                .key(getString(R.string.googleMapsApi))
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(this)
                .alternativeRoutes(false)
                .waypoints(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), pickup)
                .build();
        routing.execute();
    }


    public void startLocationUpdates() {

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();

        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        settingsClient.checkLocationSettings(locationSettingsRequest);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                new AlertDialog.Builder(this)
                        .setTitle(R.string.title_location_permission)
                        .setMessage(R.string.text_location_permission)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(driverMap.this,
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

    public void onLocationChanged(Location location) {
        //  Toast.makeText(this, "location change", Toast.LENGTH_SHORT).show();

        if (working) {
            rideDistance += mLastLocation.distanceTo(location) / 1000;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mLastLocation = location;
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(18));

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference reference;

        if (working) {
            if (prevWorking != working) {
                stopGeoFireAvailable();
                prevWorking = working;
            }
            reference = FirebaseDatabase.getInstance().getReference().child("driversWorking");
        } else {
            if (prevWorking != working) {
                erasePolyLines();
                stopGeoFireWorking();
                prevWorking = working;
                customerInfo.setVisibility(View.INVISIBLE);
            }
            reference = FirebaseDatabase.getInstance().getReference().child("driversAvailable");
        }

        GeoFire geoFire = new GeoFire(reference);
        geoFire.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));
    }

    public void stopLocationUpdates() {
        if (fusedLocationProviderClient != null)
            fusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //  Toast.makeText(this, "map ready", Toast.LENGTH_SHORT).show();
    }

    public void stopGeoFireAvailable() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("driversAvailable");

        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(user, new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {
                Toast.makeText(driverMap.this, "driver removed", Toast.LENGTH_SHORT).show();

            }
        });
    }

    public void stopGeoFireWorking() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("driversWorking");

        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(user, new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {
                //  Toast.makeText(driverMap.this, "driver removed", Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    public void onRoutingFailure(RouteException e) {
        if (e != null) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Something went wrong, Try again", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRoutingStart() {

    }

    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {
        if (polylines.size() > 0) {
            for (Polyline poly : polylines) {
                poly.remove();
            }
        }

        polylines = new ArrayList<>();
        //add route(s) to the map.
        for (int i = 0; i < route.size(); i++) {

            //In case of more than 5 alternative routes
            int colorIndex = i % COLORS.length;

            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.color(getResources().getColor(COLORS[colorIndex]));
            polyOptions.width(10 + i * 3);
            polyOptions.addAll(route.get(i).getPoints());
            Polyline polyline = mMap.addPolyline(polyOptions);
            polylines.add(polyline);

            Toast.makeText(getApplicationContext(), "Route " + (i + 1) + ": distance - " + route.get(i).getDistanceValue() + ": duration - " + route.get(i).getDurationValue(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRoutingCancelled() {

    }

    private void erasePolyLines() {
        for (Polyline line : polylines) {
            line.remove();
        }
        polylines.clear();
    }
}