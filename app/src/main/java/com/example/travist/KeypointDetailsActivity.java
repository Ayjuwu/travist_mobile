package com.example.travist;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.makeramen.roundedimageview.RoundedImageView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class KeypointDetailsActivity extends AppCompatActivity {
    RequestQueue rq;
    String token;
    private Button addToListBtn;
    // Stocke le keypoint chargé depuis l'API
    private Keypoint currentKeypoint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_keypoint_details);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        rq = Volley.newRequestQueue(this);
        token = getIntent().getStringExtra("token");
        int kpId = getIntent().getIntExtra("kpId", -1);

        // Appel des détails
        requestDetails(kpId);

        // Bouton d’ajout à la liste globale
        addToListBtn = findViewById(R.id.addToList);
        addToListBtn.setOnClickListener(view -> {
            if (currentKeypoint != null) {
                // Ajout du keypoint complet dans le singleton
                KpListHolder.addKeypoint(currentKeypoint);
                Log.d("KPLIST", "Ajouté : " + currentKeypoint.id + " -> Liste actuelle : " + KpListHolder.selectedKeypoints.toString());
            } else {
                Toast.makeText(KeypointDetailsActivity.this, "Le keypoint n'a pas encore été chargé", Toast.LENGTH_SHORT).show();
            }

            // Retour à l'activité Planify
            Intent intent = new Intent(KeypointDetailsActivity.this, PlanifyActivity.class);
            intent.putExtra("token", token);
            startActivity(intent);
        });

    }

    public void requestDetails(int kpId) {
        if (kpId != -1) {
            String url = "http://10.0.2.2/www/PPE_Travist/travist/public/api/getKeypointById/" + kpId;

            StringRequest req = new StringRequest(Request.Method.GET, url, this::processDetails, this::handleErrors) {
                public Map<String, String> getHeaders() throws AuthFailureError {
                    return new HashMap<>();
                }
            };

            rq.add(req);
        }
    }

    public void processDetails(String response) {
        try {
            TextView tvKpName = findViewById(R.id.tvKpName);
            TextView tvKpPrice = findViewById(R.id.tvKpPrice);
            TextView tvKpStartDate = findViewById(R.id.tvKpStartDate);
            TextView tvKpEndDate = findViewById(R.id.tvKpEndDate);
            RoundedImageView imageView = findViewById(R.id.imageSlide);

            JSONObject jo = new JSONObject(response);

            int kpId = jo.getInt("id");
            String kpName = jo.getString("key_point_name");
            float kpPrice = (float) jo.getDouble("key_point_price");
            String kpStartDate = jo.getString("key_point_start_date");
            String kpEndDate = jo.getString("key_point_end_date");
            String kpCover = jo.getString("key_point_cover");
            // Pour les autres champs, utilisation de opt* pour éviter des exceptions
            float gpsX = (float) jo.optDouble("gpsX", 0.0);
            float gpsY = (float) jo.optDouble("gpsY", 0.0);
            boolean is_altered = jo.optBoolean("is_altered", false);
            int cityId = jo.optInt("cityId", 0);

            // Création de l'objet Keypoint et stockage en variable
            currentKeypoint = new Keypoint(kpId, kpName, kpPrice, kpStartDate, kpEndDate, kpCover,
                    gpsX, gpsY, is_altered, cityId);

            // Mise à jour de l'affichage
            byte[] decodedString = Base64.decode(kpCover, Base64.DEFAULT);
            Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            imageView.setImageBitmap(decodedBitmap);

            tvKpName.setText("Nom du lieu : " + kpName);
            tvKpPrice.setText("Prix TTC (par personne) : " + String.format("%.2f", kpPrice) + "€");
            tvKpStartDate.setText("Date de début : " + kpStartDate);
            tvKpEndDate.setText("Date de fin : " + kpEndDate);

            requestCity(kpId);
            requestTags(kpId);
        } catch (JSONException x) {
            Toast.makeText(this, "JSON PARSE ERROR", Toast.LENGTH_LONG).show();
            Log.e("HELLOJWT", "JSON PARSE ERROR: " + response, x);
        }
    }

    public void requestTags(int kpId) {
        String url = "http://10.0.2.2/www/PPE_Travist/travist/public/api/getTagsByKeypoint/" + kpId;
        StringRequest req = new StringRequest(Request.Method.GET, url, this::processTags, this::handleErrors) {
            public Map<String, String> getHeaders() { return new HashMap<>(); }
        };
        rq.add(req);
    }

    public void processTags(String response) {
        try {
            TextView tvKpTags = findViewById(R.id.tvKpTags);
            JSONArray jsonArray = new JSONArray(response);
            StringBuilder tags = new StringBuilder();

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject tagObj = jsonArray.getJSONObject(i);
                String tagName = tagObj.getString("tag_name");
                if (i != 0) tags.append(" ");
                tags.append(tagName);
            }

            tvKpTags.setText(tags.toString());
        } catch (JSONException x) {
            Toast.makeText(this, "JSON PARSE ERROR (Tags)", Toast.LENGTH_LONG).show();
            Log.e("HELLOJWT", "JSON PARSE ERROR (Tags): " + response, x);
        }
    }

    public void requestCity(int kpId) {
        String url = "http://10.0.2.2/www/PPE_Travist/travist/public/api/getCityByKeypoint/" + kpId;
        StringRequest req = new StringRequest(Request.Method.GET, url, this::processCity, this::handleErrors) {
            public Map<String, String> getHeaders() { return new HashMap<>(); }
        };
        rq.add(req);
    }

    public void processCity(String response) {
        try {
            TextView tvKpCity = findViewById(R.id.tvKpCity);
            JSONObject jo = new JSONObject(response);
            String cityName = jo.getString("city_name");
            tvKpCity.setText("Ville la plus proche : " + cityName);
        } catch (JSONException x) {
            Toast.makeText(this, "JSON PARSE ERROR (City)", Toast.LENGTH_LONG).show();
            Log.e("HELLOJWT", "JSON PARSE ERROR (City): " + response, x);
        }
    }

    public void handleErrors(Throwable t) {
        Toast.makeText(this, "SERVERSIDE PROBLEM", Toast.LENGTH_LONG).show();
        Log.e("HELLOJWT", "SERVERSIDE BUG", t);
    }
}
