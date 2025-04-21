package com.example.travist;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class KeypointAdapter extends RecyclerView.Adapter<KeypointAdapter.ViewHolder> {

    private final List<Keypoint> keypoints;
    private final Context context;
    private final int travelId;
    public static final int REQ_REPLACE_KP = 1001;

    public KeypointAdapter(Context context, List<Keypoint> keypoints, int travelId) {
        this.context = context;
        this.keypoints = keypoints;
        this.travelId = travelId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.travel_kp_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Keypoint kp = keypoints.get(position);
        holder.tvKpName.setText(kp.name);
        holder.tvKpDates.setText(kp.startDate + " - " + kp.endDate);
        holder.tvKpCity.setText(kp.getCityName());
        holder.tvKpTotalPrice.setText(String.format("%.2f €", kp.price));

        if (kp.is_altered == 1) {
            holder.tvAlteredState.setVisibility(View.VISIBLE);
            holder.buttonsContainer.setVisibility(View.VISIBLE);

            holder.btnDelete.setOnClickListener(v -> {
                if (context instanceof TravelDetailsActivity) {
                    ((TravelDetailsActivity) context).onDeleteAlteredKeypoint(kp);
                }
            });

            holder.btnModify.setOnClickListener(v -> {
                Intent i = new Intent(context, ReplaceKeypointActivity.class);
                i.putExtra("travelId", travelId);
                i.putExtra("oldKpId", kp.id);
                i.putExtra("oldStart", kp.startDate);
                i.putExtra("oldEnd", kp.endDate);
                i.putExtra("oldLat", kp.gpsX);
                i.putExtra("oldLng", kp.gpsY);

                if (context instanceof TravelDetailsActivity) {
                    ((TravelDetailsActivity) context).startActivityForResult(i, REQ_REPLACE_KP);
                }
            });
        } else {
            holder.tvAlteredState.setVisibility(View.GONE);
            holder.buttonsContainer.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return keypoints.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvKpName, tvKpDates, tvKpCity, tvKpTotalPrice, tvAlteredState;
        LinearLayout buttonsContainer;
        Button btnDelete, btnModify;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvKpName = itemView.findViewById(R.id.tvKpNameDetails);
            tvKpDates = itemView.findViewById(R.id.tvKpDatesDetails);
            tvKpCity = itemView.findViewById(R.id.tvKpCityDetails);
            tvKpTotalPrice = itemView.findViewById(R.id.tvKpTotalPriceDetails);
            tvAlteredState = itemView.findViewById(R.id.tvAlteredState);
            buttonsContainer = itemView.findViewById(R.id.alteredButtonsContainer);
            btnDelete = itemView.findViewById(R.id.btnDeleteKp);
            btnModify = itemView.findViewById(R.id.btnModifyKp);
        }
    }

    public void updateKeypoint(Keypoint updatedKeypoint) {
        // Trouver la position du keypoint mis à jour dans la liste et update l'adapter
        for (int i = 0; i < keypoints.size(); i++) {
            if (keypoints.get(i).id == updatedKeypoint.id) {
                keypoints.set(i, updatedKeypoint);
                notifyItemChanged(i);
                break;
            }
        }
    }
}
