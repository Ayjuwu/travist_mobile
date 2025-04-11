package com.example.travist;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.travel_btn_item, parent, false);
        return new TravelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TravelViewHolder holder, int position) {
        Travel travel = travelList.get(position);
        holder.tvName.setText(travel.name);
    }

    @Override
    public int getItemCount() {
        return travelList.size();
    }

    public static class TravelViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;

        public TravelViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvTravelName);
        }
    }
}

