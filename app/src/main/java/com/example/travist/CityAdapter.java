package com.example.travist;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CityAdapter extends RecyclerView.Adapter<CityAdapter.ViewHolder> {

    private List<City> cities;

    public CityAdapter(List<City> cities) {
        this.cities = cities;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.city_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        City city = cities.get(position);
        holder.bind(city);
    }

    @Override
    public int getItemCount() {
        return cities.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCityId;
        TextView tvCityName;
        TextView tvCityCountryName;

        public ViewHolder(View itemView) {
            super(itemView);
            tvCityId = itemView.findViewById(R.id.tvCityId);
            tvCityName = itemView.findViewById(R.id.tvCityName);
            tvCityCountryName = itemView.findViewById(R.id.tvCityCountry);
        }

        public void bind(City city) {
            tvCityId.setText(String.valueOf(city.id));
            tvCityName.setText(city.name);
            tvCityCountryName.setText(city.countryName);
        }
    }
}