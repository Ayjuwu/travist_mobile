package com.example.travist;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SelectedKeypointsAdapter extends RecyclerView.Adapter<SelectedKeypointsAdapter.ViewHolder> {
    private List<Keypoint> keypoints;
    private OnItemRemoveListener onItemRemoveListener;

    public SelectedKeypointsAdapter(List<Keypoint> keypoints, OnItemRemoveListener onItemRemoveListener) {
        this.keypoints = keypoints;
        this.onItemRemoveListener = onItemRemoveListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.selected_kp_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Keypoint keypoint = keypoints.get(position);
        holder.bind(keypoint);
    }

    @Override
    public int getItemCount() {
        return keypoints.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView keypointName;
        private Button removeButton;

        public ViewHolder(View itemView) {
            super(itemView);
            keypointName = itemView.findViewById(R.id.tvKpItemName);
            removeButton = itemView.findViewById(R.id.deleteSelectedBtn);

            // Ajoute un écouteur de clic sur le bouton de suppression
            removeButton.setOnClickListener(v -> {
                // Appeler la méthode onRemoveListener lorsque le bouton est cliqué
                if (onItemRemoveListener != null) {
                    onItemRemoveListener.onRemove(keypoints.get(getAdapterPosition()));
                }
            });
        }

        public void bind(Keypoint keypoint) {
            keypointName.setText(keypoint.name);
        }
    }

    public interface OnItemRemoveListener {
        void onRemove(Keypoint keypoint);
    }
}

