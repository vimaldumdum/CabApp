package HistoryRelated;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cabapp.R;

import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryViewHolders> {

    private List<HistoryObject> historyObjectList;
    private Context context;

    public HistoryAdapter(List<HistoryObject> historyObjectList, Context context) {
        this.historyObjectList = historyObjectList;
        this.context = context;
    }

    @NonNull
    @Override
    public HistoryViewHolders onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, null, false);
        RecyclerView.LayoutParams layoutParams = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutView.setLayoutParams(layoutParams);
        HistoryViewHolders rev = new HistoryViewHolders(layoutView);
        return rev;
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolders holder, int position) {
        holder.rideId.setText(historyObjectList.get(position).getRideId());
        holder.time.setText(historyObjectList.get(position).getTime());
    }

    @Override
    public int getItemCount() {
        return historyObjectList.size();
    }
}
