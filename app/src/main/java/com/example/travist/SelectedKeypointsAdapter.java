package com.example.travist;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SelectedKeypointsAdapter extends RecyclerView.Adapter<SelectedKeypointsAdapter.ViewHolder> {

    // Listener pour le bouton de suppression
    public interface OnItemRemoveListener {
        void onRemove(Keypoint kp);
    }

    private List<Keypoint> selectedKeypoints;
    private OnItemRemoveListener removeListener;

    public SelectedKeypointsAdapter(List<Keypoint> selectedKeypoints/*,  OnItemRemoveListener  */) {
        this.selectedKeypoints = selectedKeypoints;
        this.removeListener = removeListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.selected_kp_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Keypoint kp = selectedKeypoints.get(position);
        holder.tvKpItemName.setText(kp.name);

        // Préremplissage des EditText avec les dates par défaut
        holder.etStartDate.setText(kp.startDate);
        holder.etEndDate.setText(kp.endDate);

        // Listener du bouton "Supprimer"
        holder.btnDelete.setOnClickListener(v -> {
            if (removeListener != null) {
                removeListener.onRemove(kp);
            }
        });
    }

    @Override
    public int getItemCount() {
        return selectedKeypoints.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvKpItemName;
        EditText etStartDate, etEndDate;
        Button btnDelete;

        public ViewHolder(View itemView) {
            super(itemView);
            tvKpItemName = itemView.findViewById(R.id.tvKpItemName);
            etStartDate = itemView.findViewById(R.id.etKpItemStartDate);
            etEndDate = itemView.findViewById(R.id.etKpItemEndDate);
            btnDelete = itemView.findViewById(R.id.deleteSelectedBtn);
        }
    }
}
