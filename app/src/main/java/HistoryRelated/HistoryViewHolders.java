package HistoryRelated;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cabapp.R;

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

    }
}
