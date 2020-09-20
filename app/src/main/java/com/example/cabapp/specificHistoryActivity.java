package com.example.cabapp;

import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class specificHistoryActivity extends AppCompatActivity implements OnMapReadyCallback, RoutingListener {

    private TextView rout, distance, name, phone, date;
    private ImageView profileImage;
    private RatingBar ratingBar;

    private GoogleMap mMap;
    private SupportMapFragment mapFragment;
    private LatLng destinationLatLng, pickupLatLng;

    private List<Polyline> polylines;
    private static final int[] COLORS = new int[]{R.color.primary_dark_material_light};

    private String userId, userType, rideId, customerId, driverId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_specific_history);

        destinationLatLng = new LatLng(0, 0);
        pickupLatLng = new LatLng(0, 0);
        polylines = new ArrayList<>();

        rout = (TextView) findViewById(R.id.route);
        distance = (TextView) findViewById(R.id.distance);
        name = (TextView) findViewById(R.id.name);
        phone = (TextView) findViewById(R.id.phone);
        date = (TextView) findViewById(R.id.date);
        ratingBar = (RatingBar) findViewById(R.id.ratingBar);

        profileImage = (ImageView) findViewById(R.id.historyProfileImage);

        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.historyMap);
        mapFragment.getMapAsync(this);

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Bundle bundle = getIntent().getExtras();
        rideId = bundle.getString("rideId");
        //     Log.d("messaaggee", rideId);

        getUserType();
        //   Log.d("messaaggee", userType);
        //  Log.d("messaaggee", otherUserId);
        //  populateOtherUserInfo();

    }

    private void populateOtherUserInfo(String type, String otherId) {

        DatabaseReference otherUserRef = FirebaseDatabase.getInstance().getReference().child("users").child(type).child(otherId);
        otherUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                    //    Toast.makeText(specificHistoryActivity.this, "Second fun", Toast.LENGTH_SHORT).show();
                    if (map.get("name") != null) {
                        name.setText(map.get("name").toString());
                    }
                    if (map.get("phone") != null) {
                        phone.setText(map.get("phone").toString());
                    }
                    if (map.get("profilePicture") != null) {
                        Glide.with(specificHistoryActivity.this).load(map.get("profilePicture")).into(profileImage);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void getUserType() {
        //  Log.d("messaaggee", rideId);

        DatabaseReference rideRef = FirebaseDatabase.getInstance().getReference().child("history").child(rideId);
        rideRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        if (child.getKey().equals("customer")) {
                            customerId = child.getValue().toString();
                            if (!customerId.equals(userId)) {
                                userType = "driver";
                                populateOtherUserInfo("customer", customerId);
                            }
                        }
                        if (child.getKey().equals("driver")) {
                            driverId = child.getValue().toString();
                            if (!driverId.equals(userId)) {
                                userType = "customer";
                                populateOtherUserInfo("driver", driverId);
                                updateRating();
                            }
                        }
                        Long time = 0L;
                        if (child.getKey().equals("timestamp")) {
                            time = Long.valueOf(child.getValue().toString());
                        }
                        if (child.getKey().equals("rating")) {
                            ratingBar.setRating(Integer.parseInt(child.getValue().toString()));
                        }
                        date.setText(getDate(time));
                        if (child.getKey().equals("destination")) {
                            rout.setText(child.getValue().toString());
                        }
                        if (child.getKey().equals("location")) {
                            pickupLatLng = new LatLng(Double.parseDouble(child.child("from").child("lat").toString()), Double.parseDouble(child.child("from").child("lng").toString()));
                            destinationLatLng = new LatLng(Double.parseDouble(child.child("to").child("lat").toString()), Double.parseDouble(child.child("to").child("lng").toString()));
                            if (destinationLatLng != new LatLng(0, 0))
                                drawRouteToPickup();
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void updateRating() {
        ratingBar.setVisibility(View.VISIBLE);
        ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float v, boolean b) {

                DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("history").child(rideId);
                ref.child("rating").setValue(v);

                DatabaseReference ref2 = FirebaseDatabase.getInstance().getReference().child("users").child("driver").child(driverId).child("rating").child(rideId);
                ref2.setValue(v);
            }
        });
    }

    private String getDate(Long timestamp) {
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTimeInMillis(timestamp * 1000);
        String date = DateFormat.format("dd-MM-yyyy hh:mm", calendar).toString();
        return date;
    }

    private void drawRouteToPickup() {
        Routing routing = new Routing.Builder()
                .key(getString(R.string.googleMapsApi))
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(this)
                .alternativeRoutes(false)
                .waypoints(pickupLatLng, destinationLatLng)
                .build();
        routing.execute();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
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