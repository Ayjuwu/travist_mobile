package com.example.travist;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class KeypointAdapter extends RecyclerView.Adapter<KeypointAdapter.ViewHolder> {

    private List<Keypoint> keypoints;

    public KeypointAdapter(List<Keypoint> keypoints) {
        this.keypoints = keypoints;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.travel_kp_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Keypoint kp = keypoints.get(position);
        holder.bind(kp);
    }

    @Override
    public int getItemCount() {
        return keypoints.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvKpName;
        TextView tvKpDates;
        TextView tvKpCity;
        TextView tvKpTotalPrice;

        public ViewHolder(View itemView) {
            super(itemView);
            tvKpName = itemView.findViewById(R.id.tvKpNameDetails);
            tvKpDates = itemView.findViewById(R.id.tvKpDatesDetails);
            tvKpCity = itemView.findViewById(R.id.tvKpCityDetails);
            tvKpTotalPrice = itemView.findViewById(R.id.tvKpTotalPriceDetails);
        }

        public void bind(Keypoint kp) {
            tvKpName.setText(kp.name);
            tvKpDates.setText(kp.startDate + " - " + kp.endDate);
            tvKpCity.setText(kp.getCityName());
            tvKpTotalPrice.setText(String.format("%.2f €", kp.price));
        }
    }
}
