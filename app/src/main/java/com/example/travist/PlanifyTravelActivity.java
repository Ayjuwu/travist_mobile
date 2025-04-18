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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PlanifyTravelActivity extends AppCompatActivity {
    private RequestQueue rq;
    private String token;
    private Button saveBtn;
    private ViewPager2 viewPager2;
    private SliderAdapter sliderAdapter;
    private SelectedKeypointsPlanifyAdapter selectedKpAdapter;
    private List<SliderItem> sliderItems = new ArrayList<>();
    UserSession session = UserSession.getInstance();
    int currentUserId = session.getUserId();
    public static Map<Integer, String> visitStartDates = new HashMap<>();
    public static Map<Integer, String> visitEndDates = new HashMap<>();

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
        selectedKpAdapter = new SelectedKeypointsPlanifyAdapter(
                new SelectedKeypointsPlanifyAdapter.OnItemRemoveListener() {
                    @Override
                    public void onRemove(Keypoint kp) {
                        KpListHolderPlanify.selectedKeypointsPlanify.remove(kp);
                        visitStartDates.remove(kp.id);
                        visitEndDates.remove(kp.id);
                        requestKeypoints();
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
                }
        );
        rvSelectedKp.setAdapter(selectedKpAdapter);


        // Initialisation de l'adapter du carrousel
        sliderAdapter = new SliderAdapter(sliderItems, kpId -> {
            Intent intent = new Intent(this, KeypointDetailsActivity.class);
            intent.putExtra("token", token);
            intent.putExtra("kpId", kpId);
            intent.putExtra("precedentActivity", this.getLocalClassName().toString());
            startActivity(intent);
        });
        viewPager2.setAdapter(sliderAdapter);


        // Initialisation Volley
        rq = Volley.newRequestQueue(this);
        Intent i = getIntent();
        token = i.getStringExtra("token");
        Log.i("HELLOJWT", "token " + token);


        // Appel du WebService pour récupérer les keypoints
        requestKeypoints();


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
                float totalPrice = KpListHolderPlanify.calculateTotalPrice(peopleNumber);
                tvTotalPrice.setText(totalPrice + "€");
            }
        });

        // Bouton pour sauvegarder le nouveau voyage
        saveBtn.setOnClickListener(view -> {
            saveBtn.setEnabled(false);
            createAndSaveNewTravel();
            Intent intent = new Intent(this, Profile.class);
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

    private void requestKeypoints() {
        String url = "http://10.0.2.2/www/PPE_Travist/travist/public/api/getKeypoints";

        StringRequest req = new StringRequest(Request.Method.GET, url, this::processKeypoints, this::handleErrors) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return new HashMap<>();
            }
        };

        rq.add(req);
    }

    private void processKeypoints(String response) {
        try {
            JSONArray jsonArray = new JSONArray(response);

            TextView tvTotalPrice = findViewById(R.id.tvTotalPrice);
            TextView tvIndividualPrice = findViewById(R.id.tvIndividualPrice);
            EditText etPeopleNumber = findViewById(R.id.etPeopleNumber);

            String peopleNumberStr = etPeopleNumber.getText().toString();
            int peopleNumber = Integer.parseInt(peopleNumberStr);

            if (jsonArray.length() != 0) {
                sliderItems.clear();
                List<Keypoint> selectedKeypoints = KpListHolderPlanify.selectedKeypointsPlanify;

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
                    tvIndividualPrice.setText(KpListHolderPlanify.calculateIndividualPrice() + "€");
                    tvTotalPrice.setText(KpListHolderPlanify.calculateTotalPrice(peopleNumber) + "€");
                }

                ConstraintLayout carouselContainer = findViewById(R.id.carouselLayout);

                int totalAvailableKeypoints = jsonArray.length();
                int selectedKeypointsCount = KpListHolderPlanify.selectedKeypointsPlanify.size();

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

            int peopleNumber;
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

            List<Keypoint> selectedKeypointsList = KpListHolderPlanify.selectedKeypointsPlanify;
            if (selectedKeypointsList == null || selectedKeypointsList.isEmpty()) {
                handleError("Vous n'avez pas sélectionné de lieu !", "Vous n'avez pas sélectionné de lieu !");
                return;
            }

            // Synchronisation des dates sélectionnées
            // Pour chaque keypoint, on met à jour ses dates à partir des maps statiques
            for (Keypoint kp : selectedKeypointsList) {
                String selectedStartDate = SelectedKeypointsPlanifyAdapter.visitStartDates.get(kp.id);
                String selectedEndDate = SelectedKeypointsPlanifyAdapter.visitEndDates.get(kp.id);
                if (selectedStartDate != null && !selectedStartDate.isEmpty()) {
                    kp.startDate = selectedStartDate;
                }
                if (selectedEndDate != null && !selectedEndDate.isEmpty()) {
                    kp.endDate = selectedEndDate;
                }
            }

            // Vérification individuelle des dates pour chaque keypoint
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            try {
                for (Keypoint kp : selectedKeypointsList) {
                    Date start = sdf.parse(kp.startDate);
                    Date end = sdf.parse(kp.endDate);
                    // Vérifier que la date de début est strictement antérieure à la date de fin
                    if (start != null && end != null && !start.before(end)) {
                        handleError("La date de début du lieu \"" + kp.name + "\" doit être avant sa date de fin.",
                                "Veuillez corriger les dates pour " + kp.name);
                        return;
                    }
                }
            } catch (ParseException e) {
                e.printStackTrace();
                handleError("Erreur de format de date", "Vérifiez les dates sélectionnées pour chaque lieu.");
                return;
            }

            // Vérification des chevauchements
            try {
                for (int i = 0; i < selectedKeypointsList.size(); i++) {
                    Keypoint kp1 = selectedKeypointsList.get(i);
                    Date s1 = sdf.parse(kp1.startDate);
                    Date e1 = sdf.parse(kp1.endDate);
                    for (int j = i + 1; j < selectedKeypointsList.size(); j++) {
                        Keypoint kp2 = selectedKeypointsList.get(j);
                        Date s2 = sdf.parse(kp2.startDate);
                        Date e2 = sdf.parse(kp2.endDate);

                        if (!(e1.before(s2) || e1.equals(s2) || e2.before(s1) || e2.equals(s1))) {
                            handleError("Les dates du lieu '" + kp1.name + "' chevauchent celles du lieu '" + kp2.name + "'.",
                                    "Veuillez choisir des dates non chevauchantes.");
                            return;
                        }
                    }
                }
            } catch (ParseException ex) {
                ex.printStackTrace();
                handleError("Erreur de format de date", "Vérifiez le format des dates sélectionnées.");
                return;
            }

            // Calcul des prix et des dates globales
            float individualPrice = 0f;
            float totalPrice = 0f;
            Date earliestDate = null;
            Date latestDate = null;
            try {
                for (Keypoint kp : selectedKeypointsList) {
                    individualPrice += kp.price;
                    totalPrice += kp.price * peopleNumber;

                    Date currentStart = sdf.parse(kp.startDate);
                    Date currentEnd = sdf.parse(kp.endDate);
                    if (earliestDate == null || currentStart.before(earliestDate)) {
                        earliestDate = currentStart;
                    }
                    if (latestDate == null || currentEnd.after(latestDate)) {
                        latestDate = currentEnd;
                    }
                }
            } catch (ParseException e) {
                e.printStackTrace();
                handleError("Erreur de format de date", "Les dates sélectionnées sont invalides.");
                return;
            }

            // Création de l'objet JSON
            JSONObject travelData = new JSONObject();
            try {
                travelData.put("travel_name", travelName);
                travelData.put("people_number", peopleNumber);
                travelData.put("individual_price", individualPrice);
                travelData.put("total_price", totalPrice);
                travelData.put("travel_start_date", sdf.format(earliestDate));
                travelData.put("travel_end_date", sdf.format(latestDate));
                travelData.put("user_id", currentUserId);
            } catch (JSONException e) {
                e.printStackTrace();
                KpListHolderPlanify.resetKeypoints();
                return;
            }

            Log.i("HELLOJWT", "Données envoyées : " + travelData.toString());

            String url = "http://10.0.2.2/www/PPE_Travist/travist/public/api/createTravel";

            StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                    response -> {
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            Log.i("HELLOJWT", "Réponse JSON: " + response);
                            boolean success = jsonResponse.optBoolean("success", false);

                            if (success) {
                                JSONObject tData = jsonResponse.optJSONObject("data");
                                if (tData != null) {
                                    int newTravelId = tData.optJSONObject("travel").optInt("id", -1);
                                    Log.i("HELLOJWT", "ID du voyage créé : " + newTravelId);
                                    if (newTravelId != -1) {
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
            // Ajouter l'ID du voyage
            assignedData.put("travel_id", travelId);

            // Ajouter les keypoints associés avec leurs dates
            JSONArray ja = new JSONArray();
            for (Keypoint kp : keypoints) {
                // Récupérer les dates depuis les maps
                String start = visitStartDates.get(kp.id);
                String end = visitEndDates.get(kp.id);

                // Si aucune date n'est renseignée dans la map, on prend les dates de base du keypoint
                if (start == null || start.isEmpty()) {
                    start = kp.startDate;
                    visitStartDates.put(kp.id, start);
                }
                if (end == null || end.isEmpty()) {
                    end = kp.endDate;
                    visitEndDates.put(kp.id, end);
                }

                Log.d("ASSIGN_DEBUG", "Keypoint ID: " + kp.id + " Start: " + start + " End: " + end);

                JSONObject keypointObj = new JSONObject();
                keypointObj.put("keypoint_id", kp.id);
                keypointObj.put("start_date", start);
                keypointObj.put("end_date", end);
                ja.put(keypointObj);
            }

            assignedData.put("keypoints", ja);
            Log.i("HELLOJWT", "Données envoyées pour assignation : " + assignedData.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            KpListHolderPlanify.resetKeypoints();
            return;
        }

        String url = "http://10.0.2.2/www/PPE_Travist/travist/public/api/insertAssigned";

        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        Log.i("HELLOJWT", "Réponse assignation keypoints : " + response);
                        if (jsonResponse.optBoolean("success", false)) {

                            Intent intent = new Intent(this, Profile.class);
                            intent.putExtra("token", token);
                            startActivity(intent);
                            finish();
                        } else {
                            handleError("Erreur lors de l'assignation des lieux", "Erreur lors de l'assignation des lieux");
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

    private void handleErrors(Throwable t) {
        handleError("SERVERSIDE BUG", "Erreur du côté serveur");
    }

    private void handleError(String logMessage, String toastMessage) {
        Log.e("PlanifyActivity", logMessage);
        Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();
        KpListHolderPlanify.resetKeypoints();
        saveBtn.setEnabled(true);
    }

    private void handleSuccess(String logMessage, String toastMessage) {
        Log.i("PlanifyActivity", logMessage);
        Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();
        KpListHolderPlanify.resetKeypoints();
        saveBtn.setEnabled(true);
    }
}