package com.example.travist;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CitiesListActivity extends AppCompatActivity implements CityAdapter.OnCityActionListener {
    RequestQueue rq;
    RecyclerView rvTags;
    CityAdapter cityAdapter;
    List<City> cityList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cities_list);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        rq = Volley.newRequestQueue(this);

        cityList = new ArrayList<>();

        rvTags = findViewById(R.id.rvCities);
        rvTags.setLayoutManager(new LinearLayoutManager(this));

        cityAdapter = new CityAdapter(cityList, this);
        rvTags.setAdapter(cityAdapter);

        requestCities();
    }

    @Override
    protected void onResume() {
        super.onResume();
        requestCities();
    }

    private void requestCities() {
        String url = "http://10.0.2.2/www/PPE_Travist/travist/public/api/getCities";

        StringRequest req = new StringRequest(Request.Method.GET, url, this::processCities, this::handleErrors) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return new HashMap<>();
            }
        };

        rq.add(req);
    }

    private void processCities(String response) {
        try {
            JSONArray jsonArray = new JSONArray(response);
            cityList.clear();
            if (jsonArray.length() != 0) {
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject city = jsonArray.getJSONObject(i);

                    int cityId = city.getInt("id");
                    String cityName = city.getString("city_name");
                    String cityCountry = city.getString("city_country");

                    if (cityId != 0) {
                        City c = new City(cityId, cityName, cityCountry);
                        cityList.add(c);
                    }
                }

                cityAdapter.notifyDataSetChanged();
            }
        } catch (JSONException x) {
            handleError("JSON PARSE ERROR: " + response, "Erreur de traitement des données JSON");
        }
    }

    @Override
    public void onModify(City city) {
        Intent intent = new Intent(this, ModifyCityActivity.class);
        intent.putExtra("cityId", city.id);
        intent.putExtra("cityName", city.name);
        intent.putExtra("cityCountryName", city.countryName);
        startActivity(intent);
    }

    @Override
    public void onDelete(City city) {
        String url = "http://10.0.2.2/www/PPE_Travist/travist/public/api/deleteCity/" + city.id;
        StringRequest req = new StringRequest(Request.Method.DELETE, url,
                response -> {
                    cityList.remove(city);
                    cityAdapter.notifyDataSetChanged();
                    Toast.makeText(this, "Ville supprimée", Toast.LENGTH_SHORT).show();
                },
                error -> Toast.makeText(this, "Erreur suppression", Toast.LENGTH_SHORT).show()
        );
        rq.add(req);
    }

    private void handleErrors(Throwable t) {
        handleError("SERVERSIDE BUG", "Erreur du côté serveur");
    }

    private void handleError(String logMessage, String toastMessage) {
        Log.e("CitiesListActivity", logMessage);
        Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();
    }
}