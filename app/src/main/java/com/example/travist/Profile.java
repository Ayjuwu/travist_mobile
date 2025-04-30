package com.example.travist;

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

    // Initialisation des variables
    private RequestQueue rq;
    private String token = UserSession.getToken();

    Button planifyBtn;
    List<Travel> travelList = new ArrayList<>();

    RecyclerView recyclerView;
    TravelAdapter travelAdapter;

    TextView tvNoTravel;
    ImageView ivNoTravel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialisation de Volley
        rq = Volley.newRequestQueue(this);

        // Attribution de la RecyclerView + nouvelle instance de l'adapter pour les voyages
        recyclerView = findViewById(R.id.recyclerView);
        travelAdapter = new TravelAdapter(travelList);

        // On set la RecyclerView dans cette activité et on y attribue l'adapter
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(travelAdapter);

        // Appels du WebService pour les informations utilisateur +  listing des ses voyages
        requestDetails();

        // Attribution et appel du bouton de planification d'un nouveau voyage (listener)
        planifyBtn = findViewById(R.id.planifyTravelBtn);
        planifyBtn.setOnClickListener(view -> {
            Intent intent = new Intent(this, PlanifyTravelActivity.class);
            startActivity(intent);
        });
    }

    // Méthode onResume pour actualiser les éléments de la vue au retour d'une activité
    @Override
    protected void onResume() {
        super.onResume();
        travelList.clear();
        requestDetails();
    }

    // Méthode WebService pour récupérer les informations utilisateurs
    private void requestDetails() {
        // String url = "http://192.168.0.110/~mathys.raspolini/travist/public/api/profile";
        String url="http://10.0.2.2/~mathys.raspolini/travist/public/api/profile";

        StringRequest req = new StringRequest(Request.Method.GET, url, this::processDetails, this::handleErrors) {
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Authorization", token);
                return params;
            }
        };

        rq.add(req);
    }

    // Méthode WebService pour procéder à la récupération et l'attribution de l'utilisateur
    private void processDetails(String response) {
        try {
            // On initialise le JSONObject et sa data, et on y récupère l'objet data dans l'objet profile
            JSONObject joData = new JSONObject(response)
                    .getJSONObject("data")
                    .getJSONObject("profile")
                    .getJSONObject("data");

            // Récupération des données utilisateur
            String userName = joData.getString("user_name");
            int userId = joData.getInt("id");

            // On définit aussi l'id user dans le UserSession grâce à la requête WebService
            UserSession.setUserId(userId);

            // Attribution des données dans le TextView de la vue
            TextView tvPs = findViewById(R.id.tvPseudo);
            tvPs.setText(userName);

            requestUserTravels();
        } catch (JSONException x) {
            Toast.makeText(this, "JSON PARSE ERROR", Toast.LENGTH_LONG).show();
            Log.e("HELLOJWT", "JSON PARSE ERROR: " + response, x);
        }
    }

    private void requestUserTravels() {
        // String url="http://192.168.0.110/~mathys.raspolini/travist/public/api/getTravelsByUser/" + userId;
        String url = "http://10.0.2.2/~mathys.raspolini/travist/public/api/getTravelsByUser/" + UserSession.getUserId();

        // Appel pour récupérer les voyages de l'utilisateur (pas besoin de HashMap ici)
        StringRequest req = new StringRequest(Request.Method.GET, url, this::processUserTravels, this::handleErrors
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

    // Méthode WebService pour procéder à la récupératon des voyages de l'utilisateur
    private void processUserTravels(String response) {
        try {
            // On récupère le tableau JSON retourné puisqu'il s'agit d'un tableau de voyages
            JSONArray jsonArray = new JSONArray(response);

            // Attribution des UI liées aux voyages
            tvNoTravel = findViewById(R.id.noTravelTextView);
            ivNoTravel = findViewById(R.id.noTravelImageView);

            // Si l'utilisateur n'a aucun voyage, alors on attribue un texte par défaut au TextView, et on met une image par défaut au lieu du listing
            if (jsonArray.length() == 0) {
                tvNoTravel.setText("Vous n'avez aucun trajet planifié");
                ivNoTravel.setImageResource(R.drawable.no_travel_icon);

                // Assurer la visibilité de l'image et du texte
                tvNoTravel.setVisibility(View.VISIBLE);
                ivNoTravel.setVisibility(View.VISIBLE);

                // Modifier la taille de l'image
                ViewGroup.LayoutParams params = ivNoTravel.getLayoutParams();
                params.width = 600; // Ajuster la largeur souhaitée
                params.height = 400; // Ajuster la hauteux souhaitée
                ivNoTravel.setLayoutParams(params);
            } else {
                // On cache le message et l'icône si des voyages existent
                tvNoTravel.setVisibility(View.GONE);
                ivNoTravel.setVisibility(View.GONE);

                // On vide la liste au cas où il y aurait déjà des données
                travelList.clear();

                // Pour chaque élément du tableau JSON, on extrait les valeurs, puis on crée un nouvel Objet voyage qu'on ajoute à la liste pour la RecyclerView
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

                // Mettre à jour la RecyclerView en notifiant l'adapter
                travelAdapter.notifyDataSetChanged();
            }
        } catch (JSONException e) {
            Toast.makeText(this, "JSON PARSE ERROR", Toast.LENGTH_LONG).show();
            Log.e("HELLOJWT", "JSON PARSE ERROR: " + response, e);
        }
    }

    // Méthode destinée à la gestion des erreurs
    public void handleErrors(Throwable t) {
        Toast.makeText(this, "SERVERSIDE PROBLEM", Toast.LENGTH_LONG).show();
        Log.e("HELLOJWT", "SERVERSIDE BUG", t);
    }
}