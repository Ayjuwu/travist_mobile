package com.example.travist;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

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

public class PlanifyActivity extends AppCompatActivity {
    private RequestQueue rq;
    private String token;
    private Button saveBtn;
    private ViewPager2 viewPager2;
    private SliderAdapter adapter;
    private List<SliderItem> sliderItems = new ArrayList<>();

    UserSession session = UserSession.getInstance();
    int currentUserId = session.getUserId();

    private final Handler sliderHandler = new Handler();
    private final Runnable sliderRunnable = new Runnable() {
        @Override
        public void run() {
            int currentItem = viewPager2.getCurrentItem();
            int nextItem = currentItem + 1;
            if (nextItem >= sliderItems.size()) {
                nextItem = 0;
            }
            viewPager2.setCurrentItem(nextItem, true);
            sliderHandler.postDelayed(this, 3000);
        }
    };
    private TravelDatabaseHelper travelDbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_planify);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialisation UI
        viewPager2 = findViewById(R.id.viewPagerImageSlider);
        saveBtn = findViewById(R.id.saveNewTravelBtn);

        // Initialisation de l'adapter avec un onClick qui ouvre KeypointDetailsActivity
        adapter = new SliderAdapter(sliderItems, kpId -> {
            Intent intent = new Intent(PlanifyActivity.this, KeypointDetailsActivity.class);
            intent.putExtra("token", token);
            intent.putExtra("kpId", kpId);
            startActivity(intent);
        });
        viewPager2.setAdapter(adapter);

        // Initialisation Volley
        rq = Volley.newRequestQueue(this);
        Intent i = getIntent();
        token = i.getStringExtra("token");
        Log.i("HELLOJWT", "token " + token);

        // Appel du WebService pour récupérer les keypoints
        requestDetails();

        // Instanciation du TravelDatabaseHelper
        travelDbHelper = new TravelDatabaseHelper(this);

        // Bouton pour sauvegarder le nouveau voyage
        saveBtn.setOnClickListener(view -> {
            createAndSaveNewTravel();
            Intent intent = new Intent(PlanifyActivity.this, Profile.class);
            intent.putExtra("token", token);
            startActivity(intent);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        sliderHandler.postDelayed(sliderRunnable, 2250);
    }

    @Override
    protected void onStop() {
        super.onStop();
        sliderHandler.removeCallbacks(sliderRunnable);
    }

    private void requestDetails() {
        String url = "http://10.0.2.2/www/PPE_Travist/travist/public/api/getKeypoints";

        StringRequest req = new StringRequest(Request.Method.GET, url, this::processDetails, this::handleErrors) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return new HashMap<>();
            }
        };

        rq.add(req);
    }

    private void processDetails(String response) {
        try {
            JSONArray jsonArray = new JSONArray(response);

            if (jsonArray.length() != 0) {
                sliderItems.clear();

                // Récupérer la liste des keypoints sélectionnés depuis le singleton
                List<Keypoint> selectedKeypoints = KpListHolder.selectedKeypoints;

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject kp = jsonArray.getJSONObject(i);
                    int kpId = kp.getInt("id");
                    // Dans le JSON, le champ s'appelle "is_altered_keypoint" (1 = vrai)
                    int is_altered_keypoint = kp.getInt("is_altered_keypoint");

                    // Si is_altered_keypoint est vrai, on ne l'affiche pas
                    if (is_altered_keypoint == 1) {
                        continue;
                    }

                    // Vérifier si ce keypoint est déjà sélectionné
                    boolean alreadySelected = false;
                    for (Keypoint selected : selectedKeypoints) {
                        if (selected.id == kpId) {
                            alreadySelected = true;
                            break;
                        }
                    }
                    if (alreadySelected) {
                        continue;
                    }

                    // Récupération de la couverture (cover)
                    String kpCover = kp.getString("key_point_cover");

                    // Ajouter l'élément au carrousel
                    sliderItems.add(new SliderItem(kpId, kpCover));
                }

                // Notifier l'adapter des changements
                adapter.notifyDataSetChanged();
            }
        } catch (JSONException x) {
            Toast.makeText(this, "JSON PARSE ERROR", Toast.LENGTH_LONG).show();
            Log.e("HELLOJWT", "JSON PARSE ERROR: " + response, x);
        }
    }

    private void handleErrors(Throwable t) {
        Toast.makeText(this, "SERVERSIDE PROBLEM", Toast.LENGTH_LONG).show();
        Log.e("HELLOJWT", "SERVERSIDE BUG", t);
    }

    private void createAndSaveNewTravel() {
        EditText etTravelName = findViewById(R.id.etTravelName);
        EditText etPeopleNumber = findViewById(R.id.etPeopleNumber);

        String travelName = etTravelName.getText().toString();
        String peopleNumberStr = etPeopleNumber.getText().toString();

        if (travelName.isEmpty()) {
            Toast.makeText(this, "Veuillez indiquer un nom de voyage", Toast.LENGTH_SHORT).show();
            return;
        }

        if (peopleNumberStr.isEmpty()) {
            Toast.makeText(this, "Veuillez indiquer un nombre de voyageurs", Toast.LENGTH_SHORT).show();
            return;
        }

        // Gestion des erreurs pour la conversion du nombre de voyageurs
        int peopleNumber = 0;
        try {
            peopleNumber = Integer.parseInt(peopleNumberStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Veuillez entrer un nombre valide de voyageurs", Toast.LENGTH_SHORT).show();
            return;
        }

        // Récupération des keypoints sélectionnés depuis le singleton
        List<Keypoint> selectedKeypointsList = KpListHolder.selectedKeypoints;
        if (selectedKeypointsList == null || selectedKeypointsList.isEmpty()) {
            Toast.makeText(this, "Vous n'avez pas sélectionné de keypoints !", Toast.LENGTH_SHORT).show();
            return;
        }

        // Initialisation des variables de date et de prix
        String earliestDate = null;
        String latestDate = null;
        float individualPrice = 0f;
        float totalPrice = 0f;

        // Calcul de la date la plus tôt et la date la plus tard, et calcul des prix
        for (Keypoint kp : selectedKeypointsList) {
            individualPrice += kp.price;
            totalPrice += kp.price * peopleNumber;

            String kpStart = kp.startDate;
            String kpEnd = kp.endDate;

            if (earliestDate == null || kpStart.compareTo(earliestDate) < 0) {
                earliestDate = kpStart;
            }
            if (latestDate == null || kpEnd.compareTo(latestDate) > 0) {
                latestDate = kpEnd;
            }
        }

        // Création du JSON à envoyer
        JSONObject travelData = new JSONObject();
        try {
            travelData.put("travel_name", travelName);
            travelData.put("people_number", peopleNumber);
            travelData.put("individual_price", individualPrice);
            travelData.put("total_price", totalPrice);
            travelData.put("travel_start_date", earliestDate);
            travelData.put("travel_end_date", latestDate);

            // Ajouter l'ID de l'utilisateur
            travelData.put("user_id", currentUserId);

            // Log des données envoyées pour debug
            Log.i("HELLOJWT", "Données envoyées : " + travelData.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String url = "http://10.0.2.2/www/PPE_Travist/travist/public/api/createTravel";

        // Création de la requête POST avec Volley
        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        Log.i("HELLOJWT", "Réponse JSON: " + response);
                        boolean success = jsonResponse.optBoolean("success", false);
                        Log.i("HELLOJWT", "Success value: " + success);

                        if (jsonResponse.has("success") && jsonResponse.optBoolean("success", false)) {
                            // Extraction correcte de l'ID du voyage depuis la réponse
                            JSONObject tData = jsonResponse.optJSONObject("data");
                            if (tData != null) {
                                int newTravelId = tData.optJSONObject("travel").optInt("id", -1);
                                Log.i("HELLOJWT", "ID du voyage créé : " + newTravelId);

                                if (newTravelId != -1) {
                                    Toast.makeText(PlanifyActivity.this, "Voyage créé avec succès ! ID: " + newTravelId, Toast.LENGTH_SHORT).show();

                                    // Appeler insertAssigned pour lier les keypoints au voyage
                                    insertAssigned(newTravelId, selectedKeypointsList);

                                    // Rediriger vers la page d'accueil ou autre activité
                                    Intent intent = new Intent(PlanifyActivity.this, Profile.class);
                                    intent.putExtra("token", token);
                                    startActivity(intent);
                                } else {
                                    Toast.makeText(PlanifyActivity.this, "Erreur lors de la création du voyage", Toast.LENGTH_SHORT).show();
                                }
                            }
                        } else {
                            Toast.makeText(PlanifyActivity.this, "Erreur lors de la création du voyage", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(PlanifyActivity.this, "Erreur d'analyse de la réponse serveur", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    error.printStackTrace();
                    Toast.makeText(PlanifyActivity.this, "Erreur de connexion au serveur", Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            public byte[] getBody() {
                return travelData.toString().getBytes();
            }

            @Override
            public String getBodyContentType() {
                return "application/json; charset=UTF-8";
            }

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json; charset=UTF-8");
                return headers;
            }
        };

        rq.add(postRequest);
    }

    private void insertAssigned(int travelId, List<Keypoint> keypoints) {
        JSONObject assignedData = new JSONObject();
        try {
            // Ajouter l'ID du voyage et les keypoints dans le JSON
            assignedData.put("travel_id", travelId);

            // Créer un tableau des keypoints
            JSONArray keypointsArray = new JSONArray();
            for (Keypoint kp : keypoints) {
                JSONObject keypointObj = new JSONObject();
                keypointObj.put("keypoint_id", kp.id);
                keypointObj.put("start_date", kp.startDate);
                keypointObj.put("end_date", kp.endDate);
                keypointsArray.put(keypointObj);
            }
            assignedData.put("keypoints", keypointsArray);

            // Log des données envoyées pour debug
            Log.i("HELLOJWT", "Données envoyées pour assignation : " + assignedData.toString());

        } catch (JSONException e) {
            e.printStackTrace();
        }

        // URL de l'API pour l'assignation des keypoints
        String url = "http://10.0.2.2/www/PPE_Travist/travist/public/api/insertAssigned";

        // Requête POST pour envoyer les données
        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        Log.i("HELLOJWT", "Réponse assignation keypoints : " + response);
                        if (jsonResponse.optBoolean("success", false)) {
                            Toast.makeText(PlanifyActivity.this, "Keypoints assignés avec succès", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(PlanifyActivity.this, "Erreur lors de l'assignation des keypoints", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(PlanifyActivity.this, "Erreur d'analyse de la réponse serveur", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    error.printStackTrace();
                    Toast.makeText(PlanifyActivity.this, "Erreur de connexion au serveur", Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            public byte[] getBody() {
                return assignedData.toString().getBytes();
            }

            @Override
            public String getBodyContentType() {
                return "application/json; charset=UTF-8";
            }

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json; charset=UTF-8");
                return headers;
            }
        };

        rq.add(postRequest);
    }
}
