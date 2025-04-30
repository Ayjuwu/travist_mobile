package com.example.travist;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SliderAdapter extends RecyclerView.Adapter<SliderAdapter.SliderViewHolder> {

    private List<SliderItem> sliderItems;
    private OnItemClickListener listener;

    // Constructeur qui prend la liste et le listener pour le clic
    public SliderAdapter(List<SliderItem> sliderItems, OnItemClickListener listener) {
        this.sliderItems = sliderItems;
        this.listener = listener;
    }

    // Méthode onCreateViewHolder pour la gestion du layout de l'item, en fonction de sa vue
    @Override
    public SliderViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.kp_cover_item, parent, false);
        return new SliderViewHolder(itemView);
    }

    // Méthode onBindViewHolder pour définir les attributs de l'item
    @Override
    public void onBindViewHolder(SliderViewHolder holder, int position) {
        SliderItem currentItem = sliderItems.get(position);

        byte[] decodedBytes = Base64.decode(currentItem.getImageUrl(), Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        holder.imageView.setImageBitmap(bitmap);

        // On définit le listener du clic sur l'image
        holder.imageView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(currentItem.getKpId());
            }
        });
    }

    // Méthode pour retourner le nombre d'items dans l'adapter
    @Override
    public int getItemCount() {
        return sliderItems.size();
    }

    // Classe ViewHolder pour définir et attribuer les éléments de la vue
    public static class SliderViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public SliderViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageSlide);
        }
    }

    // Interface pour gérer le clic sur un item
    public interface OnItemClickListener {
        void onItemClick(int kpId);
    }
}
