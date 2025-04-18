package com.example.travist;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.makeramen.roundedimageview.RoundedImageView;

import java.util.List;

public class KeypointAllAdapter extends RecyclerView.Adapter<KeypointAllAdapter.ViewHolder> {

    private List<Keypoint> keypoints;

    public KeypointAllAdapter(List<Keypoint> keypoints) {
        this.keypoints = keypoints;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.kp_item, parent, false);
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
        TextView tvKpId;
        TextView tvKpName;
        TextView tvKpTotalPrice;
        TextView tvKpStartDate;
        TextView tvKpEndDate;
        TextView tvKpCity;
        TextView tvKpX;
        TextView tvKpY;
        RoundedImageView ivKpCover;
        TextView tvKpTags;

        public ViewHolder(View itemView) {
            super(itemView);
            tvKpId = itemView.findViewById(R.id.tvKpId);
            tvKpName = itemView.findViewById(R.id.tvKpName);
            tvKpTotalPrice = itemView.findViewById(R.id.tvKpTotalPrice);
            tvKpStartDate = itemView.findViewById(R.id.tvKpStartDate);
            tvKpEndDate = itemView.findViewById(R.id.tvKpEndDate);
            tvKpCity = itemView.findViewById(R.id.tvKpCity);
            tvKpX = itemView.findViewById(R.id.tvKpX);
            tvKpY = itemView.findViewById(R.id.tvKpY);
            ivKpCover = itemView.findViewById(R.id.ivKpCover);
            tvKpTags = itemView.findViewById(R.id.tvKpTags);
        }

        public void bind(Keypoint kp) {
            tvKpId.setText(String.valueOf(kp.id));
            tvKpName.setText(kp.name);
            tvKpTotalPrice.setText(String.format("%.2f €", kp.price));
            tvKpStartDate.setText(kp.startDate);
            tvKpEndDate.setText(kp.endDate);
            tvKpX.setText(String.format("%.5f", kp.gpsX));
            tvKpY.setText(String.format("%.5f", kp.gpsY));

            byte[] decodedString = Base64.decode(kp.cover, Base64.DEFAULT);
            Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            ivKpCover.setImageBitmap(decodedBitmap);

            tvKpCity.setText(kp.getCityName());
            tvKpTags.setText(kp.getTags());
        }
    }
}