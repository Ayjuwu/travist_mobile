package com.example.travist;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
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

public class Profile extends AppCompatActivity {
    RequestQueue rq;
    String token;
    Button planifyBtn;
    Button travelDetailsBtn;
    RecyclerView recyclerView;
    TravelAdapter adapter;
    List<Travel> travelList = new ArrayList<>();

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        rq = Volley.newRequestQueue(this);
        Intent i = getIntent();
        token = i.getStringExtra("token");
        Log.i("HELLOJWT", "token " + token);
        recyclerView = findViewById(R.id.recyclerView);

        this.requestDetails();
        planifyBtn = findViewById(R.id.planifyTravelBtn);
        planifyBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(Profile.this, PlanifyActivity.class);
                intent.putExtra("token", token);
                startActivity(intent);
            }
        });
    }

    public void requestDetails() {

        // String url = "http://192.168.0.110/~mathys.raspolini/travist/public/api/profile";
        String url="http://10.0.2.2/www/PPE_Travist/travist/public/api/profile";
        StringRequest req = new StringRequest(Request.Method.GET, url, this::processDetails, this::handleErrors) {
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Authorization", token);
                return params;
            }
        };

        rq.add(req);
    }

    public void processDetails(String response) {
        try {
            JSONObject joData = new JSONObject(response)
                    .getJSONObject("data")
                    .getJSONObject("profile")
                    .getJSONObject("data");

            // Récupération des données utilisateur
            String userName = joData.getString("user_name");
            int userId = joData.getInt("id");

            // Stocker dans le UserSession
            UserSession session = UserSession.getInstance();
            session.setUserId(userId);
            session.setUserName(userName);
            session.setToken(token);

            // Mise à jour de l'interface utilisateur
            TextView tvPs = findViewById(R.id.tvPseudo);
            tvPs.setText(userName);

            // Appel pour récupérer les voyages, en utilisant userId
            String url = "http://10.0.2.2/www/PPE_Travist/travist/public/api/getTravelsByUser/" + userId;
            StringRequest req = new StringRequest(Request.Method.GET, url, this::processUserTravels, this::handleErrors) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    return new HashMap<>();
                }
            };

            rq.add(req);
        } catch (JSONException x) {
            Toast.makeText(this, "JSON PARSE ERROR", Toast.LENGTH_LONG).show();
            Log.e("HELLOJWT", "JSON PARSE ERROR: " + response, x);
        }
    }

    public void processUserTravels(String response) {
        try {
            JSONArray jsonArray = new JSONArray(response);

            TextView tvNoTravel = findViewById(R.id.noTravelTextView);
            ImageView ivNoTravel = findViewById(R.id.noTravelImageView);

            if (jsonArray.length() == 0) {
                tvNoTravel.setText("Vous n'avez aucun trajet planifié");
                ivNoTravel.setImageResource(R.drawable.no_travel_icon);

                // Assurer la visibilité de l'image et du texte
                tvNoTravel.setVisibility(View.VISIBLE);
                ivNoTravel.setVisibility(View.VISIBLE);

                // Modifier la taille de l'icône
                ViewGroup.LayoutParams params = ivNoTravel.getLayoutParams();
                params.width = 600; // Ajuster à ta taille souhaitée
                params.height = 400; // Ajuster à ta taille souhaitée
                ivNoTravel.setLayoutParams(params);
            } else {
                // On cache le message et l'icône si des voyages existent
                tvNoTravel.setVisibility(View.GONE);
                ivNoTravel.setVisibility(View.GONE);

                // On vide la liste au cas où il y aurait déjà des données
                travelList.clear();

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject travel = jsonArray.getJSONObject(i);

                    // Extraction des informations du voyage depuis le JSON.
                    int id = travel.getInt("id");
                    String travelName = travel.getString("travel_name");
                    int peopleNumber = travel.getInt("people_number");
                    float individualPrice = (float) travel.getDouble("individual_price");
                    float totalPrice = (float) travel.getDouble("total_price");
                    String startDate = travel.getString("travel_start_date");
                    String endDate = travel.getString("travel_end_date");
                    int userId = travel.getInt("user_id");

                    Travel t = new Travel(id, travelName, peopleNumber, individualPrice, totalPrice, startDate, endDate, userId);
                    travelList.add(t);
                }

                // Mettre à jour la RecyclerView sur le thread principal (mieux que thread de fond).
                runOnUiThread(() -> {
                    adapter = new TravelAdapter(travelList, token);
                    recyclerView.setLayoutManager(new LinearLayoutManager(this));
                    recyclerView.setAdapter(adapter);
                });
            }
        } catch (JSONException e) {
            Toast.makeText(this, "JSON PARSE ERROR", Toast.LENGTH_LONG).show();
            Log.e("HELLOJWT", "JSON PARSE ERROR: " + response, e);
        }
    }

    public void handleErrors(Throwable t) {
        Toast.makeText(this, "SERVERSIDE PROBLEM", Toast.LENGTH_LONG).show();
        Log.e("HELLOJWT", "SERVERSIDE BUG", t);
    }
}