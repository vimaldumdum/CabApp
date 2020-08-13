package com.example.cabapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class profileSelection extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_selection);

        final Button customer, driver;

        customer = findViewById(R.id.customerButton);
        driver = findViewById(R.id.driverButton);

        customer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent customerIntent = new Intent(profileSelection.this, customerLogin.class);
                startActivity(customerIntent);
            }
        });

        driver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent driverIntent = new Intent(profileSelection.this, driverLogin.class);
                startActivity(driverIntent);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }

    public void updateUI() {

        Intent driverMapIntent = new Intent(profileSelection.this, driverMap.class);
        startActivity(driverMapIntent);
    }

  /*  @Override
    protected void onStart() {
        super.onStart();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if(user!=null)
            updateUI();
    }*/
}