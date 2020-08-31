package com.example.cabapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class customerSettings extends AppCompatActivity {

    private Button confirm, back;
    private EditText name, phone;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabaseReference;

    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_settings);

        confirm = (Button) findViewById(R.id.customerSettingsConfirm);
        back = (Button) findViewById(R.id.customerSettingsBack);

        name = (EditText) findViewById(R.id.customerName);
        phone = (EditText) findViewById(R.id.customerPhone);

        mAuth = FirebaseAuth.getInstance();
        userId = mAuth.getCurrentUser().getUid();
        mDatabaseReference = FirebaseDatabase.getInstance().getReference().child("users").child("customer").child(userId);

        getUserInfo();

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                return;
            }
        });

        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveUserInfo();
            }
        });
    }

    private void saveUserInfo() {

        String nameString, phoneString;
        nameString = name.getText().toString().trim();
        phoneString = phone.getText().toString().trim();

        Map userInfo = new HashMap();
        userInfo.put("name", nameString);
        userInfo.put("phone", phoneString);

        mDatabaseReference.updateChildren(userInfo);

        finish();
    }

    private void getUserInfo() {

        mDatabaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                    Map<String, Object> map = (Map<String, Object>) snapshot.getValue();

                    if (map.get("name") != null) {
                        name.setText(map.get("name").toString());
                    }
                    if (map.get("phone") != null) {
                        phone.setText(map.get("phone").toString());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }
}