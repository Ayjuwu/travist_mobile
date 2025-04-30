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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
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
    // initialisation des variables
    private RequestQueue rq;
    private String token = UserSession.getToken();
    private int currentUserId = UserSession.getUserId();

    Button saveBtn;

    EditText etTravelName, etPeopleNumber;
    TextView tvTotalPrice, tvIndividualPrice;

    ViewPager2 viewPager2;
    SliderAdapter sliderAdapter;
    SelectedKeypointsPlanifyAdapter selectedKpAdapter;

    List<Keypoint> allKeypoints = new ArrayList<>();
    List<SliderItem> sliderItems = new ArrayList<>();

    static Map<Integer, String> visitStartDates = new HashMap<>();
    static Map<Integer, String> visitEndDates = new HashMap<>();

    // Initialisations pour le slider
    Handler sliderHandler = new Handler();
    Runnable sliderRunnable = new Runnable() {
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

                        // On reconstruit entièrement le carrousel
                        buildSliderItems();

                        // On notifie les deux adapters
                        selectedKpAdapter.notifyDataSetChanged();
                        sliderAdapter.notifyDataSetChanged();

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
            intent.putExtra("kpId", kpId);
            intent.putExtra("precedentActivity", this.getLocalClassName().toString());
            startActivity(intent);
        });
        viewPager2.setAdapter(sliderAdapter);

        // Initialisation de Volley
        rq = Volley.newRequestQueue(this);

        // Appel du WebService pour récupérer les keypoints
        startLoadingKeypoints();

        // Référence à l'EditText et à la TextView pour les prix
        etPeopleNumber = findViewById(R.id.etPeopleNumber);
        tvTotalPrice = findViewById(R.id.tvTotalPrice);
        tvIndividualPrice = findViewById(R.id.tvIndividualPrice);


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
            createAndSaveNewTravel();

            Intent intent = new Intent(this, Profile.class);
            startActivity(intent);
            finish();
        });
    }

    // Méthode onStart pour lancer le délai initial du carrousel lors du lancement de l'activité
    @Override
    protected void onStart() {
        super.onStart();
        sliderHandler.postDelayed(sliderRunnable, 2250);
    }

    // Méthode onStop pour stopper le carrousel
    @Override
    protected void onStop() {
        super.onStop();
        sliderHandler.removeCallbacks(sliderRunnable);
    }

    // Méthode onResume pour update le carrousel et les adapters au retour d'une activité
    @Override
    protected void onResume() {
        super.onResume();
        buildSliderItems();

        sliderAdapter.notifyDataSetChanged();
        selectedKpAdapter.notifyDataSetChanged();
    }

    private void startLoadingKeypoints() {
        allKeypoints.clear();
        sliderItems.clear();
        KpListHolderPlanify.selectedKeypointsPlanify.clear();
        visitStartDates.clear();
        visitEndDates.clear();

        requestKeypoints();
    }

    // Méthode WebService pour récupérer tous les lieux
    private void requestKeypoints() {
        // String url = "http://192.168.0.110/~mathys.raspolini/travist/public/api/getKeypoints";
        String url = "http://10.0.2.2/~mathys.raspolini/travist/public/api/getKeypoints";
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

    // Méthode WebService pour procéder à la récupération de tous les lieux
    private void processKeypoints(String response) {
        try {
            JSONArray jsonArray = new JSONArray(response);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jo = jsonArray.getJSONObject(i);
                Keypoint kp = new Keypoint(
                        jo.getInt("id"),
                        jo.getString("key_point_name"),
                        (float)jo.getDouble("key_point_price"),
                        jo.getString("key_point_start_date"),
                        jo.getString("key_point_end_date"),
                        jo.getString("key_point_cover"),
                        (float)jo.getDouble("key_point_gps_x"),
                        (float)jo.getDouble("key_point_gps_y"),
                        jo.getInt("is_altered_keypoint"),
                        jo.getInt("city_id")
                );
                allKeypoints.add(kp);
            }

            buildSliderItems();

        } catch (JSONException e) {
            handleError("JSON PARSE ERROR", e.getMessage());
        }
    }

    // Méthode WebService principale pour créer le nouveau voyage, avec toutes les vérifications
    private void createAndSaveNewTravel() {
        etTravelName = findViewById(R.id.etTravelName);
        etPeopleNumber = findViewById(R.id.etPeopleNumber);

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

            // String url = "http://192.168.0.110/~mathys.raspolini/travist/public/api/createTravel";
            String url = "http://10.0.2.2/~mathys.raspolini/travist/public/api/createTravel";

            JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.POST, url, travelData,
                    response -> {
                        try {
                            JSONObject jsonResponse = new JSONObject(String.valueOf(response));
                            boolean success = jsonResponse.optBoolean("success", false);

                            if (success) {
                                JSONObject tData = jsonResponse.getJSONObject("data");

                                if (tData != null) {
                                    int newTravelId = tData.getJSONObject("travel").getInt("id");
                                    insertAssigned(newTravelId, selectedKeypointsList);
                                }
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
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Accept", "application/json");
                    headers.put("Authorization", token);
                    return headers;
                }
            };

            rq.add(jsonRequest);
        });
    }

    // Méthode WebService pour insérer les lieux liés au voyage dans la table associative
    private void insertAssigned(int travelId, List<Keypoint> keypoints) {
        JSONObject assignedData = new JSONObject();
        try {
            // Ajouter l'ID du voyage
            assignedData.put("travel_id", travelId);

            // Ajouter les lieux associés avec leurs dates
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

                JSONObject keypointObj = new JSONObject();
                keypointObj.put("keypoint_id", kp.id);
                keypointObj.put("start_date", start);
                keypointObj.put("end_date", end);
                ja.put(keypointObj);
            }

            assignedData.put("keypoints", ja);
        } catch (JSONException e) {
            e.printStackTrace();
            KpListHolderPlanify.resetKeypoints();
            return;
        }

        // String url = "http://192.168.0.110/~mathys.raspolini/travist/public/api/insertAssigned";
        String url = "http://10.0.2.2/~mathys.raspolini/travist/public/api/insertAssigned";

        JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.POST, url, assignedData,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(String.valueOf(response));
                        Log.i("HELLOJWT", "Réponse assignation keypoints : " + response);
                        if (jsonResponse.optBoolean("success", false)) {
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
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Accept", "application/json");
                headers.put("Authorization", token);
                return headers;
            }
        };

        rq.add(jsonRequest);
    }


    // On vérifie si un voyage avec le même nom existe déjà dans la base de données
    private void isTravelNameExists(OnTravelNameCheckListener listener) {
        // String url = "http://192.168.0.110/~mathys.raspolini/travist/public/api/getTravelsByUser/" + currentUserId;
        String url = "http://10.0.2.2/~mathys.raspolini/travist/public/api/getTravelsByUser/" + currentUserId;
        StringRequest req = new StringRequest(Request.Method.GET, url,
                response -> searchTravelName(response, listener),
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

    // Méthode pour savoir si un nom de voyage existe déjà dans la base
    private void searchTravelName(String response, OnTravelNameCheckListener listener) {
        try {
            EditText etTravelName = findViewById(R.id.etTravelName);
            String nameToCheck = etTravelName.getText().toString().trim();

            JSONArray jsonArray = new JSONArray(response);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject travel = jsonArray.getJSONObject(i);
                String existingName = travel.getString("travel_name").trim();
                if (existingName.equalsIgnoreCase(nameToCheck)) {
                    listener.onCheckComplete(true); // Si le nom existe sur un autre voyage, alors on définit le listener sur true, et on interrompt le code
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

    // Méthode pour construire le carrousel
    private void buildSliderItems() {
        sliderItems.clear();

        List<Keypoint> selected = KpListHolderPlanify.selectedKeypointsPlanify;
        for (Keypoint kp : allKeypoints) {
            if (kp.is_altered == 1) {
                continue;
            }

            // On recalcule les prix
            String pplStr = etPeopleNumber.getText().toString();
            int ppl = 0;
            if (!pplStr.isEmpty()) {
                try {
                    ppl = Integer.parseInt(pplStr);
                } catch (NumberFormatException e) {
                    ppl = 0; // Valeur par défaut en cas d'erreur de conversion
                }
            }
            if (ppl > 0) {
                tvIndividualPrice.setText(KpListHolderPlanify.calculateIndividualPrice() + "€");
                tvTotalPrice.setText(KpListHolderPlanify.calculateTotalPrice(ppl) + "€");
            }

            boolean isSelected = false;
            for (Keypoint sel : selected) {
                if (sel.id == kp.id) {
                    isSelected = true;
                    break;
                }
            }
            if (!isSelected) {
                sliderItems.add(new SliderItem(kp.id, kp.cover));
            }
        }

        // On vérifie s'il y a des éléments dans le carrousel, sinon on le cache
        View carousel = findViewById(R.id.carouselLayout);
        if (sliderItems.isEmpty()) {
            carousel.setVisibility(View.GONE);
        } else {
            carousel.setVisibility(View.VISIBLE);
        }

        sliderAdapter.notifyDataSetChanged();
        selectedKpAdapter.notifyDataSetChanged();
    }

    /* --- Méthodes destinées à la gestion des erreurs et des succès --- */
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
        Log.i("PlanifyTravelActivity", logMessage);
        Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();
        KpListHolderPlanify.resetKeypoints();
        saveBtn.setEnabled(true);
    }
    /* ---------------------------------------------------------------- */
}