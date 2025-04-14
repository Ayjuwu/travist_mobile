package com.example.travist;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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
    private SliderAdapter sliderAdapter;
    private SelectedKeypointsAdapter selectedKpAdapter;
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


        // RecyclerView pour les lieux sélectionnés
        RecyclerView rvSelectedKp = findViewById(R.id.recyclerSelectedKeypoints);
        rvSelectedKp.setLayoutManager(new LinearLayoutManager(this));

        // Utilisation de la variable d'instance existante pour l'adaptateur
        selectedKpAdapter = new SelectedKeypointsAdapter(KpListHolder.selectedKeypoints, new SelectedKeypointsAdapter.OnItemRemoveListener() {

            @Override
            public void onRemove(Keypoint kp) {
                KpListHolder.selectedKeypoints.remove(kp);
                requestDetails();
                selectedKpAdapter.notifyDataSetChanged();

                // Ajout dans le carrousel si pas déjà présent
                boolean alreadyInSlider = false;
                for (SliderItem item : sliderItems) {
                    if (item.getKpId() == kp.id) {
                        alreadyInSlider = true;
                        break;
                    }
                }
                if (!alreadyInSlider) {
                    sliderItems.add(new SliderItem(kp.id, kp.cover));
                    sliderAdapter.notifyDataSetChanged();
                }
            }
        });

        rvSelectedKp.setAdapter(selectedKpAdapter);


        // Initialisation de l'adapter du carrousel
        sliderAdapter = new SliderAdapter(sliderItems, kpId -> {
            Intent intent = new Intent(PlanifyActivity.this, KeypointDetailsActivity.class);
            intent.putExtra("token", token);
            intent.putExtra("kpId", kpId);
            startActivity(intent);
        });
        viewPager2.setAdapter(sliderAdapter);


        // Initialisation Volley
        rq = Volley.newRequestQueue(this);
        Intent i = getIntent();
        token = i.getStringExtra("token");
        Log.i("HELLOJWT", "token " + token);


        // Appel du WebService pour récupérer les keypoints
        requestDetails();


        // Instanciation du TravelDatabaseHelper
        travelDbHelper = new TravelDatabaseHelper(this);


        // Référence à l'EditText et à la TextView pour les prix
        EditText etPeopleNumber = findViewById(R.id.etPeopleNumber);
        TextView tvTotalPrice = findViewById(R.id.tvTotalPrice);


        // Ajout du TextWatcher pour mettre à jour le prix total dès la modification de la valeur
        etPeopleNumber.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                int peopleNumber = 1;
                try {
                    peopleNumber = Integer.parseInt(s.toString());
                } catch (NumberFormatException e) {
                    peopleNumber = 1;
                }
                float totalPrice = KpListHolder.calculateTotalPrice(peopleNumber);
                tvTotalPrice.setText(totalPrice + "€");
            }
        });

        // Bouton pour sauvegarder le nouveau voyage
        saveBtn.setOnClickListener(view -> {
            saveBtn.setEnabled(false);
            createAndSaveNewTravel();
            Intent intent = new Intent(PlanifyActivity.this, Profile.class);
            intent.putExtra("token", token);
            startActivity(intent);
            finish();
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

            TextView tvTotalPrice = findViewById(R.id.tvTotalPrice);
            TextView tvIndividualPrice = findViewById(R.id.tvIndividualPrice);
            EditText etPeopleNumber = findViewById(R.id.etPeopleNumber);

            String peopleNumberStr = etPeopleNumber.getText().toString();
            int peopleNumber = Integer.parseInt(peopleNumberStr);

            if (jsonArray.length() != 0) {
                sliderItems.clear();
                List<Keypoint> selectedKeypoints = KpListHolder.selectedKeypoints;

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject kp = jsonArray.getJSONObject(i);
                    int kpId = kp.getInt("id");
                    int is_altered_keypoint = kp.getInt("is_altered_keypoint");
                    if (is_altered_keypoint == 1) {
                        continue;
                    }
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
                    String kpCover = kp.getString("key_point_cover");
                    sliderItems.add(new SliderItem(kpId, kpCover));
                }

                if (peopleNumber >= 1) {
                    tvIndividualPrice.setText(KpListHolder.calculateIndividualPrice() + "€");
                    tvTotalPrice.setText(KpListHolder.calculateTotalPrice(peopleNumber) + "€");
                }

                ConstraintLayout carouselContainer = findViewById(R.id.carouselLayout);

                int totalAvailableKeypoints = jsonArray.length();
                int selectedKeypointsCount = KpListHolder.selectedKeypoints.size();

                if (selectedKeypointsCount >= totalAvailableKeypoints) {
                    carouselContainer.setVisibility(View.GONE);
                } else {
                    carouselContainer.setVisibility(View.VISIBLE);
                }

                sliderAdapter.notifyDataSetChanged();
                selectedKpAdapter.notifyDataSetChanged();
            }
        } catch (JSONException x) {
            handleError("JSON PARSE ERROR: " + response, "Erreur de traitement des données JSON");
        }
    }

    private void handleErrors(Throwable t) {
        handleError("SERVERSIDE BUG", "Erreur du côté serveur");
    }

    private void handleError(String logMessage, String toastMessage) {
        Log.e("PlanifyActivity", logMessage);
        Toast.makeText(PlanifyActivity.this, toastMessage, Toast.LENGTH_LONG).show();
        KpListHolder.resetKeypoints();
        saveBtn.setEnabled(true);
    }

    private void handleSuccess(String logMessage, String toastMessage) {
        Log.i("PlanifyActivity", logMessage);
        Toast.makeText(PlanifyActivity.this, toastMessage, Toast.LENGTH_LONG).show();
        KpListHolder.resetKeypoints();
        saveBtn.setEnabled(true);
    }

    private void createAndSaveNewTravel() {
        EditText etTravelName = findViewById(R.id.etTravelName);
        EditText etPeopleNumber = findViewById(R.id.etPeopleNumber);

        String travelName = etTravelName.getText().toString();
        String peopleNumberStr = etPeopleNumber.getText().toString();

        if (travelName.isEmpty()) {
            handleError("Veuillez indiquer un nom de voyage", "Veuillez indiquer un nom de voyage");
            return;
        }

        isTravelNameExists(nameExists -> {
            if (nameExists) {
                handleError("Ce nom de voyage existe déjà", "Un voyage avec ce nom existe déjà.");
                saveBtn.setEnabled(true);
                return;
            }

            if (peopleNumberStr.isEmpty()) {
                handleError("Veuillez indiquer un nombre de voyageurs", "Veuillez indiquer un nombre de voyageurs");
                return;
            }

            // Gestion des erreurs pour la conversion du nombre de voyageurs
            int peopleNumber = 1;
            try {
                peopleNumber = Integer.parseInt(peopleNumberStr);
            } catch (NumberFormatException e) {
                handleError("Veuillez entrer un nombre valide de voyageurs", "Veuillez entrer un nombre valide de voyageurs");
                return;
            }

            if (peopleNumber < 1) {
                handleError("Veuillez entrer un nombre valide de voyageurs", "Veuillez entrer un nombre valide de voyageurs");
                return;
            }

            // Récupération des keypoints sélectionnés depuis le singleton
            List<Keypoint> selectedKeypointsList = KpListHolder.selectedKeypoints;
            if (selectedKeypointsList == null || selectedKeypointsList.isEmpty()) {
                handleError("Vous n'avez pas sélectionné de keypoints !", "Vous n'avez pas sélectionné de keypoints !");
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
                KpListHolder.resetKeypoints();
                return;
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
                                        // Appeler insertAssigned pour lier les keypoints au voyage
                                        insertAssigned(newTravelId, selectedKeypointsList);
                                        handleSuccess("Voyage créé avec succès !", "Voyage créé avec succès !");
                                    } else {
                                        handleError("Erreur lors de la création du voyage", "Erreur lors de la création du voyage");
                                    }
                                }
                            } else {
                                handleError("Erreur lors de la création du voyage", "Erreur lors de la création du voyage");
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            handleError("Erreur d'analyse de la réponse serveur", "Erreur d'analyse de la réponse serveur");
                        }
                    },
                    error -> {
                        error.printStackTrace();
                        handleError("Erreur de connexion au serveur", "Erreur de connexion au serveur");
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
        });
    }

    private void insertAssigned(int travelId, List<Keypoint> keypoints) {
        JSONObject assignedData = new JSONObject();
        try {
            // Ajouter l'ID du voyage et les keypoints dans le JSON
            assignedData.put("travel_id", travelId);
            JSONArray keypointsArray = new JSONArray();
            for (Keypoint kp : keypoints) {
                JSONObject keypointObj = new JSONObject();
                keypointObj.put("keypoint_id", kp.id);
                keypointObj.put("start_date", kp.startDate);
                keypointObj.put("end_date", kp.endDate);
                keypointsArray.put(keypointObj);
            }
            assignedData.put("keypoints", keypointsArray);
            Log.i("HELLOJWT", "Données envoyées pour assignation : " + assignedData.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            KpListHolder.resetKeypoints();
            return;
        }

        String url = "http://10.0.2.2/www/PPE_Travist/travist/public/api/insertAssigned";

        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        Log.i("HELLOJWT", "Réponse assignation keypoints : " + response);
                        if (jsonResponse.optBoolean("success", false)) {
                            Toast.makeText(PlanifyActivity.this, "Lieux assignés avec succès", Toast.LENGTH_SHORT).show();
                            // Une fois l'assignation réussie, vous pouvez lancer l'activité suivante.
                            Intent intent = new Intent(PlanifyActivity.this, Profile.class);
                            intent.putExtra("token", token);
                            startActivity(intent);
                            finish();
                        } else {
                            handleError("Erreur lors de l'assignation des keypoints", "Erreur lors de l'assignation des keypoints");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        handleError("Erreur d'analyse de la réponse serveur", "Erreur d'analyse de la réponse serveur");
                    }
                },
                error -> {
                    error.printStackTrace();
                    handleError("Erreur de connexion au serveur", "Erreur de connexion au serveur");
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

    // On vérifie si un voyage avec le même nom existe déjà dans la base de données
    private void isTravelNameExists(OnTravelNameCheckListener listener) {
        String url = "http://10.0.2.2/www/PPE_Travist/travist/public/api/getTravelsByUser/" + currentUserId;
        StringRequest req = new StringRequest(Request.Method.GET, url,
                response -> searchTravelName(response, listener),
                this::handleErrors
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return new HashMap<>();
            }
        };

        rq.add(req);
    }

    private void searchTravelName(String response, OnTravelNameCheckListener listener) {
        try {
            EditText etTravelName = findViewById(R.id.etTravelName);
            String nameToCheck = etTravelName.getText().toString().trim();

            JSONArray jsonArray = new JSONArray(response);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject travel = jsonArray.getJSONObject(i);
                String existingName = travel.getString("travel_name").trim();
                if (existingName.equalsIgnoreCase(nameToCheck)) {
                    // Le nom existe déjà
                    listener.onCheckComplete(true);
                    return;
                }
            }
            // Aucun nom correspondant
            listener.onCheckComplete(false);
        } catch (JSONException e) {
            e.printStackTrace();
            listener.onCheckComplete(false);
        }
    }
}
