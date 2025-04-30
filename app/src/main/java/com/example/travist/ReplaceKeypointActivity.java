package com.example.travist;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReplaceKeypointActivity extends AppCompatActivity {
    // Initialisation des variables
    private RequestQueue rq;
    private String token = UserSession.getToken();
    private Keypoint replacement;
    private int travelId, oldKpId;
    private String oldStart, oldEnd;
    private double oldLat, oldLng;

    ImageView imgCover;
    TextView tvName, tvCity, tvDates, tvPrice;
    Button btnConfirmReplace, btnCancel;
    List<Keypoint> keypointList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_replace_keypoint);

        // Récupération des attributs de l'intent
        travelId = getIntent().getIntExtra("travelId", -1);
        oldKpId = getIntent().getIntExtra("oldKpId", -1);
        oldStart = getIntent().getStringExtra("oldStart");
        oldEnd = getIntent().getStringExtra("oldEnd");
        oldLat = getIntent().getDoubleExtra("oldGpsX", 0);
        oldLng = getIntent().getDoubleExtra("oldGpsY", 0);

        keypointList = KeypointManager.getCurrentKeypoints(); // On attribue la liste à la liste du singleton avec le lieu actuel

        imgCover = findViewById(R.id.imgCover);
        tvName = findViewById(R.id.tvName);
        tvCity = findViewById(R.id.tvCity);
        tvDates = findViewById(R.id.tvDates);
        tvPrice = findViewById(R.id.tvPrice);
        btnConfirmReplace = findViewById(R.id.btnConfirmReplace);
        btnCancel = findViewById(R.id.btnCancel);

        // Initialisation de Volley
        rq = Volley.newRequestQueue(this);

        // On appel la méthode pour récupérer le lieu le plus proche
        fetchNearestKeypoint();

        // Appel du bouton pour remplacer le lieu altéré
        btnConfirmReplace.setOnClickListener(view -> {
            if (replacement != null) {
                doReplaceAndReturn();
            } else {
                Toast.makeText(this, "Aucun point de remplacement disponible", Toast.LENGTH_SHORT).show();
            }
        });

        // Appel du bouton pour revenir en arrière
        btnCancel.setOnClickListener(view -> finish());
    }

    // Méthode pour récupérer le lieu le plus proche du lieu altéré
    private void fetchNearestKeypoint() {
        // String url  = "http://192.168.0.110/~mathys.raspolini/travist/public/api/getNearestKeypointPosition/" + oldLat + "/" + oldLng + "/" + travelId;
        String url  = "http://10.0.2.2/~mathys.raspolini/travist/public/api/getNearestKeypointPosition/" + oldLat + "/" + oldLng + "/" + travelId;

        StringRequest req = new StringRequest(
                Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject jo = new JSONObject(response);
                        replacement = parseKeypoint(jo);

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

    // Méthode pour récupérer la ville liée à un lieu
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

    // Méthode pour charger l'image du lieu
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

    // Méthode pour remplacer le lieu et modifier le voyage
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
            JSONArray jsonArray = new JSONArray();

            for (Keypoint kp : keypointList) {
                JSONObject jo = new JSONObject();
                jo.put("keypoint_id", kp.id);
                jo.put("start_date", kp.startDate);
                jo.put("end_date", kp.endDate);
                jsonArray.put(jo);
            }
            payload.put("keypoints", jsonArray);

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
                @Override public Map<String,String> getHeaders() {
                    Map<String,String> h = new HashMap<>();
                    h.put("Accept", "application/json");
                    h.put("Authorization", token);
                    return h;
                }
            };
            rq.add(post);

        } catch (JSONException ex) {
            ex.printStackTrace();
        }
    }

    // Méthode pour parser le lieu le plus proche et le retourner
    private Keypoint parseKeypoint(JSONObject kp) throws JSONException {
          int id = kp.getInt("id");
          String kpName = kp.getString("key_point_name");
          float kpPrice = (float)kp.getDouble("key_point_price");
          String kpStartDate = kp.getString("key_point_start_date");
          String kpEndDate = kp.getString("key_point_end_date");
          String kpCover = kp.getString("key_point_cover");
          float kpX = (float)kp.getDouble("key_point_gps_x");
          float kpY = (float)kp.getDouble("key_point_gps_y");
          int is_altered = kp.getInt("is_altered_keypoint");
          int cityId = kp.getInt("city_id");

          Keypoint keypoint = new Keypoint(id, kpName, kpPrice, kpStartDate, kpEndDate, kpCover, kpX,
                  kpY, is_altered, cityId);

          return keypoint;
    }
}
