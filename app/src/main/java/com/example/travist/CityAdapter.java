package com.example.travist;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CityAdapter extends RecyclerView.Adapter<CityAdapter.ViewHolder> {

    public interface OnCityActionListener {
        void onModify(City city);
        void onDelete(City city);
    }

    private List<City> cities;
    private CityAdapter.OnCityActionListener listener;

    public CityAdapter(List<City> cities, OnCityActionListener listener) {
        this.cities = cities;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.city_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        City city = cities.get(position);
        holder.tvCityId.setText(String.valueOf(city.id));
        holder.tvCityName.setText(city.name);
        holder.tvCityCountryName.setText(city.countryName);
        holder.btnModify.setOnClickListener(v -> listener.onModify(city));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(city));
    }

    @Override
    public int getItemCount() {
        return cities.size();
    }

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