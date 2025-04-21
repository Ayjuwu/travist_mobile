package com.example.travist;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReplaceKeypointActivity extends AppCompatActivity {
    private static final int IMAGE_MAX_WIDTH = 800, IMAGE_MAX_HEIGHT = 600;

    private int travelId, oldKpId;
    private String oldStart, oldEnd;
    private double oldLat, oldLng;

    private Keypoint replacement;
    private RequestQueue rq;

    private ImageView imgCover;
    private TextView tvName, tvCity, tvDates, tvPrice;
    private Button btnConfirmReplace, btnCancel;
    private List<Keypoint> keypointList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_replace_keypoint);

        travelId = getIntent().getIntExtra("travelId", -1);
        oldKpId = getIntent().getIntExtra("oldKpId", -1);
        oldStart = getIntent().getStringExtra("oldStart");
        oldEnd = getIntent().getStringExtra("oldEnd");
        oldLat = getIntent().getDoubleExtra("oldGpsX", 0);
        oldLng = getIntent().getDoubleExtra("oldGpsY", 0);
        keypointList = KeypointManager.getCurrentKeypoints();

        imgCover = findViewById(R.id.imgCover);
        tvName = findViewById(R.id.tvName);
        tvCity = findViewById(R.id.tvCity);
        tvDates = findViewById(R.id.tvDates);
        tvPrice = findViewById(R.id.tvPrice);
        btnConfirmReplace = findViewById(R.id.btnConfirmReplace);
        btnCancel = findViewById(R.id.btnCancel);

        rq = Volley.newRequestQueue(this);

        fetchNearestKeypoint();

        btnConfirmReplace.setOnClickListener(v -> {
            if (replacement != null) {
                doReplaceAndReturn();
            } else {
                Toast.makeText(this, "Aucun point de remplacement disponible", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> finish());
    }

    private void fetchNearestKeypoint() {
        // String url  = "http://192.168.0.110/~mathys.raspolini/travist/public/api/getNearestKeypointPosition/" + oldLat + "/" + oldLng + "/" + travelId;
        String url  = "http://10.0.2.2/~mathys.raspolini/travist/public/api/getNearestKeypointPosition/" + oldLat + "/" + oldLng + "/" + travelId;

        StringRequest req = new StringRequest(
                Request.Method.GET, url,
                response -> {
                    try {
                        // Afficher la réponse JSON brute dans le log
                        Log.d("ReplaceKeypointActivity", "Response JSON: " + response);

                        JSONObject o = new JSONObject(response);
                        replacement = parseKeypoint(o);

                        // MAJ UI
                        tvName.setText(replacement.name);
                        tvCity.setText(replacement.getCityName());
                        tvDates.setText(replacement.startDate + " - " + replacement.endDate);
                        tvPrice.setText(replacement.price + "€");

                        loadCoverImage(replacement.cover);
                        fetchCityForKeypoint(replacement.id);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Erreur parsing keypoint", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    error.printStackTrace();
                    Toast.makeText(this, "Erreur réseau getNearestKeypoint", Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override public Map<String, String> getHeaders() {
                return Collections.emptyMap();
            }
        };
        rq.add(req);
    }

    private void fetchCityForKeypoint(int keypointId) {
        // String url = "http://192.168.0.110/~mathys.raspolini/travist/public/api/getCityByKeypoint/" + keypointId;
        String url = "http://10.0.2.2/~mathys.raspolini/travist/public/api/getCityByKeypoint/" + keypointId;

        StringRequest req = new StringRequest(
                Request.Method.GET, url,
                response -> {
                    try {
                        // La réponse est directement l'objet City JSON
                        JSONObject city = new JSONObject(response);
                        String cityName = city.optString("city_name", "");
                        tvCity.setText(cityName);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Erreur parsing city", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    error.printStackTrace();
                    Toast.makeText(this, "Erreur réseau getCityByKeypoint", Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override public Map<String, String> getHeaders() {
                return Collections.emptyMap();
            }
        };
        rq.add(req);
    }


    private void loadCoverImage(String base64String) {
        if (base64String == null || base64String.isEmpty()) {
            return;
        }

        try {
            byte[] decodedString = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

            imgCover.setImageBitmap(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doReplaceAndReturn() {
        try {
            for (int i = 0; i < keypointList.size(); i++) {
                if (keypointList.get(i).id == oldKpId) {
                    replacement.startDate = oldStart;
                    replacement.endDate   = oldEnd;
                    keypointList.set(i, replacement);
                    break;
                }
            }

            JSONObject payload = new JSONObject();
            payload.put("travel_id", travelId);
            JSONArray ja = new JSONArray();
            for (Keypoint kp : keypointList) {
                JSONObject o = new JSONObject();
                o.put("keypoint_id", kp.id);
                o.put("start_date",  kp.startDate);
                o.put("end_date",    kp.endDate);
                ja.put(o);
            }
            payload.put("keypoints", ja);

            // String url = "http://192.168.0.110/~mathys.raspolini/travist/public/api/updateAssigned/" + travelId;
            String url = "http://10.0.2.2/~mathys.raspolini/travist/public/api/updateAssigned/" + travelId;
            StringRequest post = new StringRequest(
                    Request.Method.POST, url,
                    resp -> {
                        KeypointManager.clearKeypoints();
                        KeypointManager.addKeypoints(keypointList);
                        setResult(RESULT_OK);
                        finish();
                    },
                    err -> {
                        err.printStackTrace();
                        Toast.makeText(this, "Erreur updateAssigned", Toast.LENGTH_SHORT).show();
                    }
            ) {
                @Override public byte[] getBody() {
                    return payload.toString().getBytes();
                }
                @Override public String getBodyContentType() {
                    return "application/json; charset=UTF-8";
                }
                @Override public Map<String,String> getHeaders() {
                    Map<String,String> h = new HashMap<>();
                    h.put("Content-Type","application/json; charset=UTF-8");
                    return h;
                }
            };
            rq.add(post);

        } catch (JSONException ex) {
            ex.printStackTrace();
        }
    }

    private Keypoint parseKeypoint(JSONObject o) throws JSONException {
        Keypoint kp = new Keypoint(
                o.getInt("id"),
                o.getString("key_point_name"),
                (float)o.getDouble("key_point_price"),
                o.getString("key_point_start_date"),
                o.getString("key_point_end_date"),
                o.optString("key_point_cover"),
                (float)o.getDouble("key_point_gps_x"),
                (float)o.getDouble("key_point_gps_y"),
                o.getInt("is_altered_keypoint"),
                o.getInt("city_id")
        );
        return kp;
    }
}
