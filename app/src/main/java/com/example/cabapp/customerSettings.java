package com.example.cabapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class customerSettings extends AppCompatActivity {

    private ImageView profilePicture;

    private Button confirm, back;

    private EditText nameText, phoneText;

    String userId;

    FirebaseAuth mAuth;
    DatabaseReference ref;

    Uri profilePictureUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_settings);

        profilePicture = (ImageView) findViewById(R.id.profilePicture);
        confirm = (Button) findViewById(R.id.confirmButton);
        back = (Button) findViewById(R.id.backButton);
        nameText = (EditText) findViewById(R.id.nameCustomer);
        phoneText = (EditText) findViewById(R.id.phoneCustomer);

        mAuth = FirebaseAuth.getInstance();
        userId = mAuth.getCurrentUser().getUid();
        ref = FirebaseDatabase.getInstance().getReference().child("users").child("customer").child(userId);

        getUserInfo();

        profilePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, 101);
            }
        });

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == Activity.RESULT_OK) {
            final Uri imageUri = data.getData();
            profilePictureUri = imageUri;
            profilePicture.setImageURI(imageUri);
        }
    }

    private void saveUserInfo() {

        String name, phone;
        name = nameText.getText().toString();
        phone = phoneText.getText().toString();

        Map map = new HashMap();
        map.put("name", name);
        map.put("phone", phone);

        if (profilePictureUri != null) {
            profilePicture.setDrawingCacheEnabled(true);
            profilePicture.buildDrawingCache();

            Bitmap bitmap = ((BitmapDrawable) profilePicture.getDrawable()).getBitmap();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] data = baos.toByteArray();

            final StorageReference firebaseStorage = FirebaseStorage.getInstance().getReference().child("profilePictures").child(userId);

            UploadTask uploadTask = firebaseStorage.putBytes(data);

            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(customerSettings.this, "profile upload failed", Toast.LENGTH_SHORT).show();
                }
            });


            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    firebaseStorage.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            Uri downloadUri = uri;
                            String uriString = downloadUri.toString();
                            Map map = new HashMap();
                            map.put("profilePicture", uriString);
                            ref.updateChildren(map);
                            Toast.makeText(customerSettings.this, "upload successful", Toast.LENGTH_SHORT).show();
                            Toast.makeText(customerSettings.this, uriString, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
        }
        ref.updateChildren(map);
        finish();
    }

    public void getUserInfo() {

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                    Map<String, Object> map = (Map<String, Object>) snapshot.getValue();

                    if (map.get("name") != null) {
                        nameText.setText(map.get("name").toString());
                    }
                    if (map.get("phone") != null) {
                        phoneText.setText(map.get("phone").toString());
                    }
                    if (map.get("profilePicture") != null) {
                        Uri imageUri = Uri.parse(map.get("profilePicture").toString());
                        Glide.with(customerSettings.this).load(imageUri).into(profilePicture);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }
}