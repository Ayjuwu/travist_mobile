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
import com.android.volley.VolleyError;
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

public class ModifyTravelActivity extends AppCompatActivity {
    //Initialisation des variables
    private RequestQueue rq;
    private String token = UserSession.getToken();
    private int currentUserId = UserSession.getUserId();

    Button saveModifyBtn;
    TextView tvIndividualPrice, tvTotalPrice;
    EditText etPeopleNumber, etTravelName;

    Travel currentTravel;

    List<Keypoint> allKeypoints = new ArrayList<>();
    List<SliderItem> sliderItems = new ArrayList<>();

    SliderAdapter sliderAdapter;
    SelectedKeypointsModifyAdapter selectedKpAdapter;
    ViewPager2 viewPager2;

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
        setContentView(R.layout.activity_modify_travel);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialisation de Volley
        rq = Volley.newRequestQueue(this);

        // Attribution de l'intent et récupération du voyage courant
        Intent i = getIntent();
        currentTravel = (Travel) i.getSerializableExtra("currentTravel");

        // Initialisation du ViewPager2
        viewPager2 = findViewById(R.id.viewPagerModifyImageSlider);

        // RecyclerView pour les lieux sélectionnés
        RecyclerView rvSelectedKp = findViewById(R.id.recyclerSelectedKeypoints);
        rvSelectedKp.setLayoutManager(new LinearLayoutManager(this));

        // On crée une nouvelle instance de l'adapter en rajoutant une méthode onRemove pour chaque lieu, appelée lors de leur suppression
        selectedKpAdapter = new SelectedKeypointsModifyAdapter(
                new SelectedKeypointsModifyAdapter.OnItemRemoveListener() {
                    @Override
                    public void onRemove(Keypoint kp) {
                        // On retire le lieu
                        KpListHolderModify.selectedKeypointsModify.remove(kp);
                        visitStartDates.remove(kp.id);
                        visitEndDates.remove(kp.id);

                        // On reconstruit entièrement le carrousel
                        buildSliderItems();

                        // On notifie les deux adapters
                        selectedKpAdapter.notifyDataSetChanged();
                        sliderAdapter.notifyDataSetChanged();
                    }
                }
        );
        rvSelectedKp.setAdapter(selectedKpAdapter);


        // Initialisation de l'adapter du carrousel
        sliderAdapter = new SliderAdapter(sliderItems, kpId -> {
            Intent intent = new Intent(this, KeypointDetailsActivity.class);
            intent.putExtra("kpId", kpId);
            intent.putExtra("currentTravel", currentTravel);
            intent.putExtra("precedentActivity", this.getLocalClassName().toString());
            startActivity(intent);
        });
        viewPager2.setAdapter(sliderAdapter);

        // Appel du WebService pour récupérer les keypoints
        startLoadingKeypoints();

        // Attribution des champs de la vue
        etTravelName = findViewById(R.id.etModifyTravelName);
        etPeopleNumber = findViewById(R.id.etModifyPeopleNumber);
        tvIndividualPrice = findViewById(R.id.tvModifyIndividualPrice);
        tvTotalPrice = findViewById(R.id.tvModifyTotalPrice);

        etTravelName.setText(currentTravel.name);
        etPeopleNumber.setText(String.valueOf(currentTravel.peopleNumber));

        // Ajout du TextWatcher pour mettre à jour le prix total dès la modification de la valeur
        etPeopleNumber.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            // On parse le nombre de voyageur en String et on calcule le prix en temps réel après chaque changement de texte
            @Override
            public void afterTextChanged(android.text.Editable s) {
                int peopleNumber = 1; // Initialisation à 1 par voyageur par défaut
                try {
                    peopleNumber = Integer.parseInt(s.toString());
                } catch (NumberFormatException e) {
                    peopleNumber = 1;
                }
                float totalPrice = KpListHolderModify.calculateTotalPrice(peopleNumber); // KpListHolderModify étant le singleton dédié au voyage
                tvTotalPrice.setText(totalPrice + "€");
            }
        });

        // Attribution et appel du bouton de modification de voyage
        saveModifyBtn = findViewById(R.id.saveModifyTravelBtn);
        saveModifyBtn.setOnClickListener(view -> {
            updateAndSaveExistingTravel();
        });
    }

    // Méthode onStart pour lancer le délai initial du carrousel lors du lancement de l'activité
    @Override
    protected void onStart() {
        super.onStart();
        sliderHandler.postDelayed(sliderRunnable, 2250);
    }

    // Méthode onResume pour actualiser les éléments de la vue au retour d'une activité
    @Override
    protected void onResume() {
        super.onResume();
        buildSliderItems();

        sliderAdapter.notifyDataSetChanged();
        selectedKpAdapter.notifyDataSetChanged();
    }

    // Méthode onStop pour stopper le carrousel
    @Override
    protected void onStop() {
        super.onStop();
        sliderHandler.removeCallbacks(sliderRunnable);
    }

    // Méthode pour reload les lieux lors du lancement de l'activité
    private void startLoadingKeypoints() {
        // On clear les listes au cas où elles ne seraent pas vides avant la récupération
        allKeypoints.clear();
        sliderItems.clear();

        requestKeypoints();
    }

    // Méthode WebService pour récupérer tous les lieux disponibles
    private void requestKeypoints() {
        // String url = "http://192.168.0.110/~mathys.raspolini/travist/public/api/getKeypoints";
        String url = "http://10.0.2.2/~mathys.raspolini/travist/public/api/getKeypoints";
        StringRequest req = new StringRequest(Request.Method.GET, url,
                this::processKeypoints,
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

    // Méthode WebService pour procéder à la récupération de tous les lieux disponibles
    private void processKeypoints(String response) {
        try {
            JSONArray jsonArray = new JSONArray(response);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jo = jsonArray.getJSONObject(i);

                int id = jo.getInt("id");
                String kpName = jo.getString("key_point_name");
                float kpPrice = (float) jo.getDouble("key_point_price");
                String kpStartDate = jo.getString("key_point_start_date");
                String kpEndDate = jo.getString("key_point_end_date");
                String kpCover = jo.getString("key_point_cover");
                float kpX = (float) jo.getDouble("key_point_gps_x");
                float kpY = (float) jo.getDouble("key_point_gps_y");
                int is_altered = jo.getInt("is_altered_keypoint");
                int cityId = jo.getInt("city_id");

                Keypoint kp = new Keypoint(id, kpName, kpPrice, kpStartDate, kpEndDate, kpCover,
                        kpX, kpY, is_altered, cityId);
                allKeypoints.add(kp);
            }

            fetchTravelKeypoints(currentTravel.id);

        } catch (JSONException e) {
            handleError("JSON PARSE ERROR all KP", e.getMessage());
        }
    }

    // Méthode WebService pour récupérer tous les lieux reliés à un voyage
    private void fetchTravelKeypoints(int travelId) {
        // String url = "http://192.168.0.110/~mathys.raspolini/travist/public/api/getKeypointsByTravel/" + travelId;
        String url = "http://10.0.2.2/~mathys.raspolini/travist/public/api/getKeypointsByTravel/" + travelId;
        StringRequest req = new StringRequest(Request.Method.GET, url,
                this::onTravelKeypointsLoaded,
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

    // Méthode WebService pour procéder à la récupération tous les lieux reliés à un voyage
    private void onTravelKeypointsLoaded(String response) {
        try {
            JSONArray jsonArray = new JSONArray(response);

            // On vide toutes les listes avant de procéder au traitement
            KpListHolderModify.selectedKeypointsModify.clear();
            SelectedKeypointsModifyAdapter.visitStartDates.clear();
            SelectedKeypointsModifyAdapter.visitEndDates.clear();
            SelectedKeypointsModifyAdapter.availStartDates.clear();
            SelectedKeypointsModifyAdapter.availEndDates.clear();

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jo = jsonArray.getJSONObject(i);

                // On récupère les dates de visite (pivot)
                JSONObject pivot = jo.getJSONObject("pivot");
                String visitStart = pivot.getString("start_date");
                String visitEnd = pivot.getString("end_date");

                int id = jo.getInt("id");
                String kpName = jo.getString("key_point_name");
                float kpPrice = (float) jo.getDouble("key_point_price");
                String kpStartDate = jo.getString("key_point_start_date");
                String kpEndDate = jo.getString("key_point_end_date");
                String kpCover = jo.getString("key_point_cover");
                float kpX = (float) jo.getDouble("key_point_gps_x");
                float kpY = (float) jo.getDouble("key_point_gps_y");
                int is_altered = jo.getInt("is_altered_keypoint");
                int cityId = jo.getInt("city_id");

                SelectedKeypointsModifyAdapter.availStartDates.put(id, kpStartDate);
                SelectedKeypointsModifyAdapter.availEndDates.put(id, kpEndDate);

                SelectedKeypointsModifyAdapter.visitStartDates.put(id, visitStart);
                SelectedKeypointsModifyAdapter.visitEndDates.put(id, visitEnd);

                Keypoint kp = new Keypoint(id, kpName, kpPrice, visitStart, visitEnd, kpCover,
                        kpX, kpY, is_altered, cityId);

                KpListHolderModify.addKeypoint(kp);
            }

            buildSliderItems();

        } catch (JSONException e) {
            handleError("JSON PARSE ERROR travel KPs", e.getMessage());
        }
    }

    // Méthode pour contruire le carrousel
    private void buildSliderItems() {
        sliderItems.clear();
        List<Keypoint> selected = KpListHolderModify.selectedKeypointsModify;

        for (Keypoint kp : allKeypoints) {
            if (kp.is_altered == 1) {
                continue;
            }

            // On recalcule les prix
            String pplStr = etPeopleNumber.getText().toString();
            int ppl = 1;
            if (!pplStr.isEmpty()) {
                try {
                    ppl = Integer.parseInt(pplStr);
                } catch (NumberFormatException e) {
                    ppl = 1; // Valeur par défaut en cas d'erreur de conversion
                }
            }
            if (ppl > 1) {
                tvIndividualPrice.setText(KpListHolderModify.calculateIndividualPrice() + "€");
                tvTotalPrice.setText(KpListHolderModify.calculateTotalPrice(ppl) + "€");
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
        View carousel = findViewById(R.id.carouselLayoutModify);
        if (sliderItems.isEmpty()) {
            carousel.setVisibility(View.GONE);
        } else {
            carousel.setVisibility(View.VISIBLE);
        }

        // notifier les adapters
        sliderAdapter.notifyDataSetChanged();
        selectedKpAdapter.notifyDataSetChanged();
    }

    // Interface pour la vérification du nom de voyage, utilisée dans la méthode isTravelNameExistsForUpdate
    private interface OnTravelNameCheckListener {
        void onCheckComplete(boolean exists);
    }

    // Méthode WebService pour enregistrer les modifications du voyage
    private void updateAndSaveExistingTravel() {
        String travelName = etTravelName.getText().toString().trim();
        String peopleNumberStr = etPeopleNumber.getText().toString().trim();

        // On s'assure que le nom de voyage rentré n'est pas vide
        if (travelName.isEmpty()) {
            handleError("Veuillez indiquer un nom de voyage", "Veuillez indiquer un nom de voyage");
            return;
        }

        // Une fois qu'on sait que le nom du voyage n'existe pas ailleurs, on procède aux autre vérifications
        isTravelNameExistsForUpdate(travelName, exists -> {
            if (exists) {
                handleError("Ce nom de voyage existe déjà", "Un autre voyage porte ce nom.");
                saveModifyBtn.setEnabled(true);
                return;
            }

            // Si le nombre de voyageurs est vide, on interrompt le code et on renvoie un message d'erreur
            if (peopleNumberStr.isEmpty()) {
                handleError("Veuillez indiquer un nombre de voyageurs", "Veuillez indiquer un nombre de voyageurs");
                return;
            }

            // Si le nombre de voyageurs est invalide, on interrompt le code et on renvoie un message d'erreur
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

            // Si aucun lieu n'a été sélectionné pour créer un nouveau voyage, on interrompt le code et on renvoie un message d'erreur
            List<Keypoint> selectedList = KpListHolderModify.selectedKeypointsModify;
            if (selectedList.isEmpty()) {
                handleError("Vous n'avez pas de lieu sélectionné", "Sélectionnez au moins un lieu.");
                return;
            }

            // Synchronisation des dates via SimpleDateFormat
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            try {
                for (Keypoint kp : selectedList) {
                    String start = SelectedKeypointsModifyAdapter.visitStartDates.get(kp.id);
                    String end = SelectedKeypointsModifyAdapter.visitEndDates.get(kp.id);
                    if (start != null) kp.startDate = start;
                    if (end != null) kp.endDate = end;
                }
            } catch (Exception ex) {
                handleError("Erreur de dates", "Vérifiez les dates sélectionnées.");
                return;
            }

            // Vérification des chevauchements, si les dates se chevauchent alors on interrompt le code et on renvoie un message d'erreur
            try {
                for (int i = 0; i < selectedList.size(); i++) {
                    Keypoint kp1 = selectedList.get(i);
                    Date s1 = sdf.parse(kp1.startDate);
                    Date e1 = sdf.parse(kp1.endDate);
                    for (int j = i + 1; j < selectedList.size(); j++) {
                        Keypoint kp2 = selectedList.get(j);
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
                for (Keypoint kp : selectedList) {
                    individualPrice += kp.price;
                    totalPrice += kp.price * peopleNumber;

                    Date currentStart = sdf.parse(kp.startDate);
                    Date currentEnd = sdf.parse(kp.endDate);

                    if (currentStart.after(currentEnd)) {
                        handleError("Dates invalides", "La date de début pour le lieu " + kp.name + " est après la date de fin.");
                        return;
                    }

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

            // On attribut les données nécessaires pour la modification du voyage
            JSONObject travelData = new JSONObject();
            try {
                travelData.put("travel_id", currentTravel.id);
                travelData.put("travel_name", travelName);
                travelData.put("people_number", peopleNumber);
                travelData.put("individual_price", KpListHolderModify.calculateIndividualPrice());
                travelData.put("total_price", KpListHolderModify.calculateTotalPrice(peopleNumber));
                travelData.put("travel_start_date", sdf.format(findEarliest(selectedList)));
                travelData.put("travel_end_date", sdf.format(findLatest(selectedList)));
                travelData.put("user_id", currentUserId);

                Log.i("JSON ATTENDU", "JSON : " + travelData);
            } catch (JSONException e) {
                handleError("Erreur JSON", "Impossible de préparer la mise à jour.");
                return;
            }

            // On procède au WebService pour enregistrer les modifications du voyage

            // String url = "http://192.168.0.110/~mathys.raspolini/travist/public/api/updateTravel/" + currentTravel.id;
            String url = "http://10.0.2.2/~mathys.raspolini/travist/public/api/updateTravel/" + currentTravel.id;

            JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.POST, url, travelData,
                    response -> {
                        try {
                            boolean success = response.getBoolean("success");

                            if (success) {
                                updateAssigned(currentTravel.id, selectedList);
                                handleSuccess("Voyage modifié avec succès !", "Voyage modifié avec succès !");
                            } else {
                                handleError("Erreur lors de la modification du voyage", "Erreur lors de la modification du voyage");
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

    // Méthode WebService pour mettre à jour les lieux assignés au voyage
    private void updateAssigned(int travelId, List<Keypoint> keypoints) {
        JSONObject assignedData = new JSONObject();
        try {
            // On assigne les valeurs aux données envoyées
            assignedData.put("travel_id", travelId);
            JSONArray jsonArray = new JSONArray();
            for (Keypoint kp : keypoints) {
                JSONObject jo = new JSONObject();
                jo.put("keypoint_id", kp.id);
                String s = SelectedKeypointsModifyAdapter.visitStartDates.getOrDefault(kp.id, kp.startDate);
                String e = SelectedKeypointsModifyAdapter.visitEndDates.getOrDefault(kp.id, kp.endDate);
                jo.put("start_date", s);
                jo.put("end_date", e);
                jsonArray.put(jo);
            }
            assignedData.put("keypoints", jsonArray);
        } catch (JSONException ex) {
            handleError("Erreur JSON", "Impossible de préparer l'assignation.");
            return;
        }

        // On procède à la requête WebService pour modifier les lieux assignés

        // String url = "http://192.168.0.110/~mathys.raspolini/travist/public/api/updateAssigned/" + currentTravel.id;
        String url = "http://10.0.2.2/~mathys.raspolini/travist/public/api/updateAssigned/" + currentTravel.id;

        JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.POST, url, assignedData,
                response -> {
                    try {
                        if (response.getBoolean("success")) {
                            Intent intent = new Intent(this, Profile.class);
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
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Accept", "application/json");
                headers.put("Authorization", token);
                return headers;
            }
        };

        rq.add(jsonRequest);
    }

    // Méthode WebService pour vérifier si un nom de voyage existe déjà ailleurs dans la base de donnée avant la modification
    private void isTravelNameExistsForUpdate(String nameToCheck, OnTravelNameCheckListener listener) {
        // String url = "http://192.168.0.110/~mathys.raspolini/travist/public/api/getTravelsByUser/" + currentUserId;
        String url = "http://10.0.2.2/~mathys.raspolini/travist/public/api/getTravelsByUser/" + currentUserId;
        StringRequest req = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONArray jsonArray = new JSONArray(response);
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject t = jsonArray.getJSONObject(i);
                            int id = t.getInt("id");

                            String existing = t.getString("travel_name").trim();
                            if (existing.equalsIgnoreCase(nameToCheck) && id != currentTravel.id) {
                                listener.onCheckComplete(true); // Si le nom existe sur un autre voyage, alors on définit le listener sur true, et on interrompt le code
                                return;
                            }
                        }
                        listener.onCheckComplete(false);
                    } catch (JSONException e) {
                        listener.onCheckComplete(false);
                    }
                },
                error -> listener.onCheckComplete(false)
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

    // Méthode pour trouver la date la plus tôt parmi les lieux
    private Date findEarliest(List<Keypoint> keypoints) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Date earliest = null;
        for (Keypoint kp : keypoints) {
            try {
                Date start = sdf.parse(kp.startDate);
                if (start != null && (earliest == null || start.before(earliest))) {
                    earliest = start;
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return earliest;
    }

    // Méthode pour trouver la date la plus tard parmi les lieux
    private Date findLatest(List<Keypoint> keypoints) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Date latest = null;
        for (Keypoint kp : keypoints) {
            try {
                Date end = sdf.parse(kp.endDate);
                if (end != null && (latest == null || end.after(latest))) {
                    latest = end;
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return latest;
    }

    /* --- Méthodes destinées à la gestion des erreurs et des succès --- */
    private void handleErrors(VolleyError err) {
        Toast.makeText(this, "Erreur réseau", Toast.LENGTH_SHORT).show();
        Log.e("MODIFY_TRAVEL", err.toString());
    }

    private void handleError(String tag, String msg) {
        Toast.makeText(this, tag + ": " + msg, Toast.LENGTH_LONG).show();
        Log.e("MODIFY_TRAVEL", tag, new Exception(msg));
    }

    private void handleSuccess(String logMessage, String toastMessage) {
        Log.i("ModifyTravelActivity", logMessage);
        Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();
        KpListHolderModify.resetKeypoints();
        saveModifyBtn.setEnabled(true);
    }
    /* ---------------------------------------------------------------- */
}
