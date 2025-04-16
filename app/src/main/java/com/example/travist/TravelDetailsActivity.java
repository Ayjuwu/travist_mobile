package com.example.travist;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.List;

public class TravelDetailsActivity extends AppCompatActivity {
    String token;
    TextView tvTravelName;
    TextView tvNbPeople;
    TextView tvIndividualPrice;
    TextView tvTotalPrice;
    TextView tvStartDate;
    TextView tvEndDate;
    RecyclerView rvKpTravelDetails;
    private MapView mapView;

    private TravelDatabaseHelper travelDbHelper;
    private List<Keypoint> keypointList = new ArrayList<>();
    private KeypointAdapter kpAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_travel_details);

        // Appliquer le padding lié aux system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialisation osmdroid
        Configuration.getInstance().setUserAgentValue(getPackageName());
        mapView = findViewById(R.id.mapView);
        mapView.setMultiTouchControls(true);

        tvTravelName = findViewById(R.id.tvTravelDetailsName);
        tvNbPeople = findViewById(R.id.tvNbPeopleTravelDetails);
        tvIndividualPrice = findViewById(R.id.tvIndividualPriceTravelDetails);
        tvTotalPrice = findViewById(R.id.tvTotalPriceTravelDetails);
        tvStartDate = findViewById(R.id.tvStartDateTravelDetails);
        tvEndDate = findViewById(R.id.tvEndDateTravelDetails);
        rvKpTravelDetails = findViewById(R.id.rvKpTravelDetails);

        // Configuration du RecyclerView
        rvKpTravelDetails.setLayoutManager(new LinearLayoutManager(this));
        kpAdapter = new KeypointAdapter(keypointList);
        rvKpTravelDetails.setAdapter(kpAdapter);

        Intent intent = getIntent();
        token = intent.getStringExtra("token");
        Travel currentTravel = (Travel) intent.getSerializableExtra("currentTravel");

        // Affichage des détails du voyage
        tvTravelName.setText(currentTravel.name);
        tvNbPeople.setText(String.valueOf(currentTravel.peopleNumber));
        tvIndividualPrice.setText(currentTravel.individualPrice + "€");
        tvTotalPrice.setText(currentTravel.totalPrice + "€");
        tvStartDate.setText(currentTravel.startDate);
        tvEndDate.setText(currentTravel.endDate);

        fetchKeypointsForTravel(currentTravel.id);
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume(); // <- important
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause(); // <- important
    }

    private void fetchKeypointsForTravel(int travelId) {
        String url = "http://10.0.2.2/www/PPE_Travist/travist/public/api/getKeypointsByTravel/" + travelId;
        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest request = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Ajout des marqueurs et du tracé
                        List<GeoPoint> geoPoints = new ArrayList<>();

                        try {
                            // Nettoyage carte
                            mapView.getOverlays().clear();

                            JSONArray jsonArray = new JSONArray(response);
                            keypointList.clear();
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jo = jsonArray.getJSONObject(i);
                                int id = jo.getInt("id");
                                String name = jo.getString("key_point_name");
                                float price = (float) jo.getDouble("key_point_price");

                                String startDate = jo.getJSONObject("pivot").getString("start_date");
                                String endDate = jo.getJSONObject("pivot").getString("end_date");
                                String cover = jo.optString("key_point_cover", "");
                                float gpsX = (float) jo.optDouble("key_point_gps_x", 0);
                                float gpsY = (float) jo.optDouble("key_point_gps_y", 0);
                                boolean is_altered = jo.optBoolean("is_altered_keypoint", false);
                                int cityId = jo.optInt("city_id", 0);
                                String cityName = jo.getJSONObject("city").getString("city_name");

                                Keypoint kp = new Keypoint(id, name, price, startDate, endDate, cover, gpsX, gpsY, is_altered, cityId);
                                kp.setCityName(cityName);
                                keypointList.add(kp);

                                GeoPoint point = new GeoPoint(kp.gpsX, kp.gpsY);
                                geoPoints.add(point);

                                Marker marker = new Marker(mapView);
                                marker.setPosition(point);
                                marker.setTitle(kp.name);
                                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                                mapView.getOverlays().add(marker);

                                // Tracer le trajet
                                Polyline polyline = new Polyline();
                                polyline.setPoints(geoPoints);
                                mapView.getOverlays().add(polyline);

                                // Centrer la carte sur le premier point
                                if (!geoPoints.isEmpty()) {
                                    mapView.getController().setZoom(12.0);
                                    mapView.getController().setCenter(geoPoints.get(0));
                                }

                                // Mise à jour de la vue
                                mapView.invalidate();
                            }
                            kpAdapter.notifyDataSetChanged();
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(TravelDetailsActivity.this, "Erreur lors de la récupération des keypoints.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
                Toast.makeText(TravelDetailsActivity.this, "Erreur de connexion au serveur.", Toast.LENGTH_SHORT).show();
            }
        });
        queue.add(request);
    }
}