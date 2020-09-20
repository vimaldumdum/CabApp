package HistoryRelated;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cabapp.R;
import com.example.cabapp.specificHistoryActivity;

public class HistoryViewHolders extends RecyclerView.ViewHolder implements View.OnClickListener {

    public TextView rideId, time;

    public HistoryViewHolders(@NonNull View itemView) {
        super(itemView);

        itemView.setOnClickListener(this);
        rideId = (TextView) itemView.findViewById(R.id.rideId);
        time = (TextView) itemView.findViewById(R.id.time);
    }

    @Override
    public void onClick(View view) {
        Intent intent = new Intent(view.getContext(), specificHistoryActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("rideId", rideId.getText().toString());
        //  intent.putExtra("rideId", rideId.getText().toString());
        //  Log.d("mehssage", rideId.getText().toString());
        intent.putExtras(bundle);
        view.getContext().startActivity(intent);
    }
}
