package com.example.travist;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
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
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TravelDetailsActivity extends AppCompatActivity {
    String token;
    RequestQueue rq;
    TextView tvTravelName, tvNbPeople, tvIndividualPrice, tvTotalPrice, tvStartDate, tvEndDate;
    Button deleteTravelBtn, modifyTravelBtn;
    RecyclerView rvKpTravelDetails;
    private MapView mapView;
    private KeypointAdapter kpAdapter;
    private Travel currentTravel;

    private static final int REPLACE_KEYPOINT_REQUEST = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_travel_details);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        rq = Volley.newRequestQueue(this);
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

        deleteTravelBtn = findViewById(R.id.deleteTravelBtn);
        modifyTravelBtn = findViewById(R.id.modifyTravelBtn);

        Intent intent = getIntent();
        token = intent.getStringExtra("token");
        currentTravel = (Travel) intent.getSerializableExtra("currentTravel");

        tvTravelName.setText(currentTravel.name);
        tvNbPeople.setText(String.valueOf(currentTravel.peopleNumber));
        tvIndividualPrice.setText(currentTravel.individualPrice + "€");
        tvTotalPrice.setText(currentTravel.totalPrice + "€");
        tvStartDate.setText(currentTravel.startDate);
        tvEndDate.setText(currentTravel.endDate);

        rvKpTravelDetails.setLayoutManager(new LinearLayoutManager(this));
        kpAdapter = new KeypointAdapter(this, KeypointManager.getCurrentKeypoints(), currentTravel.id);
        rvKpTravelDetails.setAdapter(kpAdapter);

        fetchKeypointsForTravel(currentTravel.id);

        deleteTravelBtn.setOnClickListener(view -> deleteTravel(currentTravel.id));

        modifyTravelBtn.setOnClickListener(view -> {
            Intent i = new Intent(this, ModifyTravelActivity.class);
            i.putExtra("token", token);
            i.putExtra("currentTravel", currentTravel);
            startActivity(i);
        });
    }
    @Override
    public void onResume() {
        super.onResume();
        fetchKeypointsForTravel(currentTravel.id);
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REPLACE_KEYPOINT_REQUEST && resultCode == RESULT_OK) {
            // Mise à jour visuelle après remplacement
            fetchKeypointsForTravel(currentTravel.id);
        }
    }

    public void deleteTravel(int travelId) {
        // String url = "http://192.168.0.110/~mathys.raspolini/travist/public/api/deleteTravel/" + travelId;
        String url = "http://10.0.2.2/~mathys.raspolini/travist/public/api/deleteTravel/" + travelId;

        StringRequest req = new StringRequest(Request.Method.DELETE, url,
                this::processCurrentTravelDeletion,
                this::handleErrors
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Accept", "application/json");
                headers.put("Authorization", token);
                return headers;
            }
        };

        rq.add(req);
    }

    public void processCurrentTravelDeletion(String response) {
        try {
            JSONObject json = new JSONObject(response);
            if (json.getBoolean("success")) {
                Toast.makeText(this, "Voyage supprimé avec succès", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, Profile.class);
                intent.putExtra("token", token);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Erreur lors de la suppression", Toast.LENGTH_SHORT).show();
            }
        } catch (JSONException e) {
            Toast.makeText(this, "Erreur de réponse", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchKeypointsForTravel(int travelId) {
        // String url = "http://192.168.0.110/~mathys.raspolini/travist/public/api/getKeypointsByTravel/" + travelId;
        String url = "http://10.0.2.2/~mathys.raspolini/travist/public/api/getKeypointsByTravel/" + travelId;

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    List<GeoPoint> geoPoints = new ArrayList<>();
                    mapView.getOverlays().clear();
                    KeypointManager.clearKeypoints();

                    try {
                        JSONArray jsonArray = new JSONArray(response);
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
                            int is_altered = jo.optInt("is_altered_keypoint", 0);
                            int cityId = jo.optInt("city_id", 0);
                            String cityName = jo.getJSONObject("city").getString("city_name");

                            Keypoint kp = new Keypoint(id, name, price, startDate, endDate, cover, gpsX, gpsY, is_altered, cityId);
                            kp.setCityName(cityName);
                            KeypointManager.addKeypoint(kp);

                            GeoPoint point = new GeoPoint(kp.gpsX, kp.gpsY);
                            geoPoints.add(point);

                            Marker marker = new Marker(mapView);
                            marker.setPosition(point);
                            marker.setTitle(kp.name);
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                            mapView.getOverlays().add(marker);
                        }

                        Polyline polyline = new Polyline();
                        polyline.setPoints(geoPoints);
                        mapView.getOverlays().add(polyline);

                        if (!geoPoints.isEmpty()) {
                            mapView.getController().setZoom(12.0);
                            mapView.getController().setCenter(geoPoints.get(0));
                        }

                        mapView.invalidate();
                        kpAdapter.notifyDataSetChanged();

                    } catch (JSONException e) {
                        Toast.makeText(this, "Erreur JSON", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                },
                error -> {
                    Toast.makeText(this, "Erreur serveur", Toast.LENGTH_SHORT).show();
                    error.printStackTrace();
                });

        rq.add(request);
    }

    public void onDeleteAlteredKeypoint(Keypoint kp) {
        // String url = "http://192.168.0.110/~mathys.raspolini/travist/public/api/deleteAssigned/" + currentTravel.id + "/" + kp.id;
        String url = "http://10.0.2.2/~mathys.raspolini/travist/public/api/deleteAssigned/" + currentTravel.id + "/" + kp.id;

        StringRequest request = new StringRequest(Request.Method.DELETE, url,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        boolean success = json.getBoolean("success");

                        if (success) {
                            JSONObject travelJson = json.getJSONObject("travel");

                            // Mise à jour des informations du voyage
                            currentTravel.individualPrice = (float) travelJson.getDouble("individual_price");
                            currentTravel.totalPrice = (float) travelJson.getDouble("total_price");
                            currentTravel.startDate = travelJson.optString("travel_start_date", "N/A");
                            currentTravel.endDate = travelJson.optString("travel_end_date", "N/A");

                            // Mise à jour de l'UI
                            tvIndividualPrice.setText(String.format("%.2f €", currentTravel.individualPrice));
                            tvTotalPrice.setText(String.format("%.2f €", currentTravel.totalPrice));
                            tvStartDate.setText(currentTravel.startDate);
                            tvEndDate.setText(currentTravel.endDate);

                            Toast.makeText(this, "Lieu supprimé du voyage", Toast.LENGTH_SHORT).show();

                            // Recharger la liste des keypoints pour mettre à jour la carte + recycler
                            fetchKeypointsForTravel(currentTravel.id);
                        } else {
                            Toast.makeText(this, "Erreur lors de la suppression", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(this, "Erreur JSON", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                },
                error -> {
                    Toast.makeText(this, "Erreur suppression", Toast.LENGTH_SHORT).show();
                    error.printStackTrace();
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", token);
                headers.put("Accept", "application/json");
                return headers;
            }
        };

        rq.add(request);
    }

    public void handleErrors(Throwable t) {
        Toast.makeText(this, "Erreur serveur", Toast.LENGTH_LONG).show();
        Log.e("ERROR", "BUG", t);
    }
}
