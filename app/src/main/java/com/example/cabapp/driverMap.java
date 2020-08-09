package com.example.cabapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

public class driverMap extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_map);

        Thread thread = new Thread(){

            @Override
            public void run() {
                super.run();
                try {
                    sleep(2000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                finally {
                    FirebaseAuth mAuth = FirebaseAuth.getInstance();
                    mAuth.signOut();
                }
            }
        };

        thread.start();
        Toast.makeText(driverMap.this, "User logged out", Toast.LENGTH_SHORT).show();
    }
}