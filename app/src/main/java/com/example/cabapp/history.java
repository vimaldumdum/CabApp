package com.example.cabapp;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import HistoryRelated.HistoryAdapter;
import HistoryRelated.HistoryObject;

public class history extends AppCompatActivity {

    private RecyclerView historyRecyclerView;
    private RecyclerView.Adapter mHistoryAdapter;
    private RecyclerView.LayoutManager layoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        historyRecyclerView = (RecyclerView) findViewById(R.id.historyRecyclerView);
        historyRecyclerView.setNestedScrollingEnabled(false);
        historyRecyclerView.setHasFixedSize(true);

        layoutManager = new LinearLayoutManager(history.this);
        historyRecyclerView.setLayoutManager(layoutManager);
        mHistoryAdapter = new HistoryAdapter(getDataHistory(), history.this);
        historyRecyclerView.setAdapter(mHistoryAdapter);

        for (int i = 0; i < 100; i++) {
            HistoryObject object = new HistoryObject(Integer.toString(i));
            resultHistory.add(object);
        }
    }

    private ArrayList resultHistory = new ArrayList<HistoryObject>();

    private List<HistoryObject> getDataHistory() {
        return resultHistory;
    }
}