package com.example.cabapp;

import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

import HistoryRelated.HistoryAdapter;
import HistoryRelated.HistoryObject;

public class history extends AppCompatActivity {

    private RecyclerView historyRecyclerView;
    private RecyclerView.Adapter<HistoryRelated.HistoryViewHolders> mHistoryAdapter;
    private RecyclerView.LayoutManager layoutManager;

    String user, userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        user = getIntent().getExtras().getString("user");
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        historyRecyclerView = (RecyclerView) findViewById(R.id.historyRecyclerView);
        historyRecyclerView.setNestedScrollingEnabled(false);
        historyRecyclerView.setHasFixedSize(true);

        layoutManager = new LinearLayoutManager(history.this);
        historyRecyclerView.setLayoutManager(layoutManager);
        mHistoryAdapter = new HistoryAdapter(getDataHistory(), history.this);
        historyRecyclerView.setAdapter(mHistoryAdapter);

        getUserHistory(user);

    }

    private void getUserHistory(String user) {
        //    Toast.makeText(history.this, "inside fun1", Toast.LENGTH_SHORT).show();
        DatabaseReference userHistoryRef = FirebaseDatabase.getInstance().getReference().child("users").child(user).child(userId).child("history");
        userHistoryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //   Toast.makeText(history.this, "inside first evenet listener", Toast.LENGTH_SHORT).show();
                if (snapshot.exists()) {
                    //       Toast.makeText(history.this, "first snapshot exists", Toast.LENGTH_SHORT).show();

                    for (DataSnapshot history : snapshot.getChildren()) {
                        fetchHistory(history.getKey());
                    }
                }
                //       Toast.makeText(history.this, "snapshot doesnt exists", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void fetchHistory(String key) {

        DatabaseReference historyRef = FirebaseDatabase.getInstance().getReference().child("history").child(key);
        historyRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Long timestamp = 0L;
                    for (DataSnapshot child : snapshot.getChildren()) {
                        if (child.getKey().equals("timestamp")) {
                            timestamp = Long.valueOf(child.getValue().toString());
                        }
                    }
                    HistoryObject object = new HistoryObject(snapshot.getKey(), getDate(timestamp));
                    resultHistory.add(object);
                    Log.d("meh", resultHistory.toString());
                    //    Toast.makeText(history.this, "item added", Toast.LENGTH_SHORT).show();
                    mHistoryAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private String getDate(Long timestamp) {
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTimeInMillis(timestamp * 1000);
        String date = DateFormat.format("dd-MM-yyyy hh:mm", calendar).toString();
        return date;
    }


    private ArrayList<HistoryObject> resultHistory = new ArrayList<HistoryObject>();

    private List<HistoryObject> getDataHistory() {
        return resultHistory;
    }
}