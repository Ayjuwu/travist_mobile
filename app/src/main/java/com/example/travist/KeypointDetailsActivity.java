package com.example.travist;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class KeypointDetailsActivity extends AppCompatActivity {
    // Initialisation des variables
    private RequestQueue rq;
    private String token = UserSession.getToken();
    Travel currentTravel;

    Button addToListBtn;
    Keypoint currentKeypoint;

    TextView tvKpName, tvKpPrice, tvKpStartDate, tvKpEndDate;
    ImageView imageView;

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

        // Initialisation de Volley
        rq = Volley.newRequestQueue(this);

        // Initialisation de l'intent et récupération de ses attributs
        Intent i = getIntent();

        int kpId = i.getIntExtra("kpId", -1);
        String precedentActivity = i.getStringExtra("precedentActivity");
        currentTravel = (Travel) i.getSerializableExtra("currentTravel");

        // Appel pour récupérer tous les lieux
        requestKeypoints(kpId);

        // Bouton d’ajout à la liste globale
        addToListBtn = findViewById(R.id.addToList);
        addToListBtn.setOnClickListener(view -> {

            if (currentKeypoint != null) {
                if (precedentActivity.equals("PlanifyTravelActivity")) {
                    // Ajout du keypoint complet dans le singleton
                    KpListHolderPlanify.addKeypoint(currentKeypoint);
                    finish();
                } else {
                    KpListHolderModify.addKeypoint(currentKeypoint);
                    finish();
                }
            } else {
                Toast.makeText(this, "Le lieu n'a pas encore été chargé", Toast.LENGTH_SHORT).show();
            }
        });

    }

    // Méthode WebService pour récupérer tous les lieux
    public void requestKeypoints(int kpId) {
        if (kpId != -1) {
            // String url = "http://192.168.0.110/~mathys.raspolini/travist/public/api/getKeypointById/" + kpId;
            String url = "http://10.0.2.2/~mathys.raspolini/travist/public/api/getKeypointById/" + kpId;

            StringRequest req = new StringRequest(Request.Method.GET, url, this::processKeypoints, this::handleErrors
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
    }

    // Méthode WebService pour procéder à la récupération de tous les lieux
    public void processKeypoints(String response) {
        try {
            tvKpName = findViewById(R.id.tvKpName);
            tvKpPrice = findViewById(R.id.tvKpPrice);
            tvKpStartDate = findViewById(R.id.tvKpStartDate);
            tvKpEndDate = findViewById(R.id.tvKpEndDate);
            imageView = findViewById(R.id.imageSlide);

            JSONObject jo = new JSONObject(response);

            int kpId = jo.getInt("id");
            String kpName = jo.getString("key_point_name");
            float kpPrice = (float) jo.getDouble("key_point_price");
            String kpStartDate = jo.getString("key_point_start_date");
            String kpEndDate = jo.getString("key_point_end_date");
            String kpCover = jo.getString("key_point_cover");
            float gpsX = (float) jo.getDouble("key_point_gps_x");
            float gpsY = (float) jo.getDouble("key_point_gps_y");
            int is_altered = jo.getInt("is_altered_keypoint");
            int cityId = jo.getInt("city_id");

            // On crée une instance d'un lieu et on le stocke dans une variable
            currentKeypoint = new Keypoint(kpId, kpName, kpPrice, kpStartDate, kpEndDate, kpCover,
                    gpsX, gpsY, is_altered, cityId);

            // On décode le blob en base64 de l'image pour l'afficher dans la vue
            byte[] decodedString = Base64.decode(kpCover, Base64.DEFAULT);
            Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            imageView.setImageBitmap(decodedBitmap);

            // On définit le texte pour les TextViews de la vue
            tvKpName.setText("Nom du lieu : " + kpName);
            tvKpPrice.setText("Prix TTC (par personne) : " + String.format("%.2f", kpPrice) + "€");
            tvKpStartDate.setText("Date de début : " + kpStartDate);
            tvKpEndDate.setText("Date de fin : " + kpEndDate);

            // On fait appel aux villes et aux tags pour afficher la suite des informations du lieu courant
            requestCity(kpId);
            requestTags(kpId);
        } catch (JSONException x) {
            Toast.makeText(this, "JSON PARSE ERROR", Toast.LENGTH_LONG).show();
            Log.e("HELLOJWT", "JSON PARSE ERROR: " + response, x);
        }
    }

    // Méthode WebService pour récupérer toutes les villes
    public void requestCity(int kpId) {
        // String url = "http://192.168.0.110/~mathys.raspolini/travist/public/api/getCityByKeypoint/" + kpId;
        String url = "http://10.0.2.2/~mathys.raspolini/travist/public/api/getCityByKeypoint/" + kpId;
        StringRequest req = new StringRequest(Request.Method.GET, url, this::processCity, this::handleErrors
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

    // Méthode WebService pour procéder à la récupération de toutes les villes
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

    // Méthode WebService pour récupérer tous les tags
    public void requestTags(int kpId) {
        // String url = "http://192.168.0.110/~mathys.raspolini/travist/public/api/getTagsByKeypoint/" + kpId;
        String url = "http://10.0.2.2/~mathys.raspolini/travist/public/api/getTagsByKeypoint/" + kpId;
        StringRequest req = new StringRequest(Request.Method.GET, url, this::processTags, this::handleErrors
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

    // Méthode WebService pour procéder à la récupération de tous les tags
    public void processTags(String response) {
        try {
            TextView tvKpTags = findViewById(R.id.tvKpTags);
            JSONArray jsonArray = new JSONArray(response);
            StringBuilder tags = new StringBuilder(); // On crée un StringBuilder pour ajouter tous nos tags les uns à la suite des autres, en chaînes de caractères

            // On met nos tags les uns à la suite des autres
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject tagObj = jsonArray.getJSONObject(i);
                String tagName = tagObj.getString("tag_name");
                if (i != 0) tags.append(" "); // pour éviter le tag par défaut dans la BDD
                tags.append(tagName);
            }

            tvKpTags.setText(tags.toString());
        } catch (JSONException x) {
            Toast.makeText(this, "JSON PARSE ERROR (Tags)", Toast.LENGTH_LONG).show();
            Log.e("HELLOJWT", "JSON PARSE ERROR (Tags): " + response, x);
        }
    }

    // Méthode destinée à la gestion des erreurs
    public void handleErrors(Throwable t) {
        Toast.makeText(this, "SERVERSIDE PROBLEM", Toast.LENGTH_LONG).show();
        Log.e("HELLOJWT", "SERVERSIDE BUG", t);
    }
}