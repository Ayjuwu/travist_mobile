package com.example.travist;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TravelAdapter extends RecyclerView.Adapter<TravelAdapter.TravelViewHolder> {

    private List<Travel> travelList;

    public TravelAdapter(List<Travel> travelList) {
        this.travelList = travelList;
    }

    @NonNull
    @Override
    public TravelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflater le layout pour chaque élément (ici un TextView)
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.travel_btn_item, parent, false);
        return new TravelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TravelViewHolder holder, int position) {
        Travel travel = travelList.get(position);
        Log.i("TRAVEL_ADAPTER", "Binding travel at position: " + position + ", name: " + travel.name);
        holder.btnTravelName.setText(travel.name);
    }

    @Override
    public int getItemCount() {
        return travelList.size();
    }

    // ViewHolder pour chaque item de la RecyclerView
    public static class TravelViewHolder extends RecyclerView.ViewHolder {
        Button btnTravelName;
        public TravelViewHolder(@NonNull View itemView) {
            super(itemView);
            // Associer la TextView avec l'id du layout
            btnTravelName = itemView.findViewById(R.id.btnTravelName);
        }
    }
}


