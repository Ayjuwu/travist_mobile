package com.example.travist;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TravelAdapter extends RecyclerView.Adapter<TravelAdapter.TravelViewHolder> {

    private List<Travel> travels;
    private String token;

    public TravelAdapter(List<Travel> travels, String token) {
        this.travels = travels;
        this.token = token;
    }

    @Override
    public TravelViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.travel_btn_item, parent, false);
        return new TravelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(TravelViewHolder holder, int position) {
        Travel t = travels.get(position);
        holder.bind(t);
    }

    @Override
    public int getItemCount() {
        return travels.size();
    }

    class TravelViewHolder extends RecyclerView.ViewHolder {
        Button btnDetails;

        public TravelViewHolder(View itemView) {
            super(itemView);
            btnDetails = itemView.findViewById(R.id.btnTravelDetails);
        }

        void bind(Travel travel) {
            btnDetails.setText(travel.name);
            btnDetails.setOnClickListener(v -> {
                Intent intent = new Intent(itemView.getContext(), TravelDetailsActivity.class);
                intent.putExtra("currentTravel", travel);
                intent.putExtra("token", token);
                itemView.getContext().startActivity(intent);
            });
        }
    }
}



