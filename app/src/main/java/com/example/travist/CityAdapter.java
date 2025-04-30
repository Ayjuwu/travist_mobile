package com.example.travist;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CityAdapter extends RecyclerView.Adapter<CityAdapter.ViewHolder> {

    // Interface dédiée pour chaque élément de l'adapter
    public interface OnCityActionListener {
        void onModify(City city);
        void onDelete(City city);
    }

    // Initialisation des variables
    private List<City> cities;
    private CityAdapter.OnCityActionListener listener;

    // Contructeur de l'adapter
    public CityAdapter(List<City> cities, OnCityActionListener listener) {
        this.cities = cities;
        this.listener = listener;
    }

    // Méthode onCreateViewHolder pour la gestion du layout de l'item, en fonction de sa vue
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.city_item, parent, false);
        return new ViewHolder(view);
    }

    // Méthode onBindViewHolder pour définir les attributs de l'item (vue + appel des méthodes du listener => interface)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        City city = cities.get(position);
        holder.tvCityId.setText(String.valueOf(city.id));
        holder.tvCityName.setText(city.name);
        holder.tvCityCountryName.setText(city.countryName);
        holder.btnModify.setOnClickListener(v -> listener.onModify(city));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(city));
    }

    // Méthode pour retourner le nombre d'items dans l'adapter
    @Override
    public int getItemCount() {
        return cities.size();
    }

    // Classe ViewHolder pour définir et attribuer les éléments de la vue
    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCityId;
        TextView tvCityName;
        TextView tvCityCountryName;
        Button btnModify, btnDelete;

        public ViewHolder(View itemView) {
            super(itemView);
            tvCityId = itemView.findViewById(R.id.tvCityId);
            tvCityName = itemView.findViewById(R.id.tvCityName);
            tvCityCountryName = itemView.findViewById(R.id.tvCityCountry);
            btnModify = itemView.findViewById(R.id.modifyCityBtn);
            btnDelete = itemView.findViewById(R.id.deleteCityBtn);
        }
    }
}