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

    import com.android.volley.Request;
    import com.android.volley.RequestQueue;
    import com.android.volley.VolleyError;
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
        String token;
        Button saveModifyBtn;
        private Travel currentTravel;
        UserSession session = UserSession.getInstance();
        int currentUserId = session.getUserId();
        private RequestQueue rq;
        private List<Keypoint> allKeypoints = new ArrayList<>();
        private List<SliderItem> sliderItems = new ArrayList<>();
        private SliderAdapter sliderAdapter;
        private SelectedKeypointsModifyAdapter selectedKpAdapter;
        private ViewPager2 viewPager2;
        private TextView tvIndividualPrice, tvTotalPrice;
        private EditText etPeopleNumber;

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
            setContentView(R.layout.activity_modify_travel);


            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });


            // Initialisation UI
            viewPager2 = findViewById(R.id.viewPagerModifyImageSlider);
            saveModifyBtn = findViewById(R.id.saveModifyTravelBtn);

            // Initialisation Volley
            rq = Volley.newRequestQueue(this);
            Intent i = getIntent();
            token = i.getStringExtra("token");
            currentTravel = (Travel) i.getSerializableExtra("currentTravel");


            // RecyclerView pour les lieux sélectionnés
            RecyclerView rvSelectedKp = findViewById(R.id.recyclerSelectedKeypoints);
            rvSelectedKp.setLayoutManager(new LinearLayoutManager(this));

            // Utilisation de la variable d'instance existante pour l'adaptateur
            selectedKpAdapter = new SelectedKeypointsModifyAdapter(
                    new SelectedKeypointsModifyAdapter.OnItemRemoveListener() {
                        @Override
                        public void onRemove(Keypoint kp) {
                            // On retire le keypoint
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
                intent.putExtra("token", token);
                intent.putExtra("kpId", kpId);
                intent.putExtra("currentTravel", currentTravel);
                intent.putExtra("precedentActivity", this.getLocalClassName().toString());
                startActivity(intent);
            });
            viewPager2.setAdapter(sliderAdapter);


            // Appel du WebService pour récupérer les keypoints
            startLoadingKeypoints();


            // Référence à l'EditText et à la TextView pour les prix
            etPeopleNumber = findViewById(R.id.etModifyPeopleNumber);
            tvIndividualPrice = findViewById(R.id.tvModifyIndividualPrice);
            tvTotalPrice = findViewById(R.id.tvModifyTotalPrice);


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
                    float totalPrice = KpListHolderModify.calculateTotalPrice(peopleNumber);
                    tvTotalPrice.setText(totalPrice + "€");
                }
            });

            // Bouton pour sauvegarder le nouveau voyage
            saveModifyBtn.setOnClickListener(view -> {
                saveModifyBtn.setEnabled(false);
                updateAndSaveExistingTravel();

                Intent intent = new Intent(this, Profile.class);
                intent.putExtra("token", token);
                startActivity(intent);
                finish();
            });
        }

        @Override
        protected void onResume() {
            super.onResume();
            buildSliderItems();

            sliderAdapter.notifyDataSetChanged();
            selectedKpAdapter.notifyDataSetChanged();
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

        private void startLoadingKeypoints() {
            allKeypoints.clear();
            sliderItems.clear();
            fetchAllKeypoints();
        }

        private void fetchAllKeypoints() {
            String url = "http://10.0.2.2/www/PPE_Travist/travist/public/api/getKeypoints";
            StringRequest req = new StringRequest(Request.Method.GET, url,
                    this::onAllKeypointsLoaded,
                    this::handleErrors
            );
            rq.add(req);
        }

        private void onAllKeypointsLoaded(String response) {
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


        private void fetchTravelKeypoints(int travelId) {
            String url = "http://10.0.2.2/www/PPE_Travist/travist/public/api/getKeypointsByTravel/" + travelId;
            StringRequest req = new StringRequest(Request.Method.GET, url,
                    this::onTravelKeypointsLoaded,
                    this::handleErrors
            );
            rq.add(req);
        }

        private void onTravelKeypointsLoaded(String response) {
            try {
                JSONArray jsonArray = new JSONArray(response);

                // On vide d’abord pour ne pas cumuler
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
                    String visitEnd   = pivot.getString("end_date");

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

        private void buildSliderItems() {
            sliderItems.clear();
            List<Keypoint> selected = KpListHolderModify.selectedKeypointsModify;

            for (Keypoint kp : allKeypoints) {
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

            // recalcul des prix
            String pplStr = etPeopleNumber.getText().toString();
            int ppl = pplStr.isEmpty() ? 0 : Integer.parseInt(pplStr);
            if (ppl > 0) {
                tvIndividualPrice.setText(KpListHolderModify.calculateIndividualPrice() + "€");
                tvTotalPrice.setText(KpListHolderModify.calculateTotalPrice(ppl) + "€");
            }

            // visibilité du carousel
            View carousel = findViewById(R.id.carouselLayoutModify);
            carousel.setVisibility(
                    sliderItems.isEmpty() ? View.GONE : View.VISIBLE
            );

            // notifier les adapters
            sliderAdapter.notifyDataSetChanged();
            selectedKpAdapter.notifyDataSetChanged();
        }

        private interface OnTravelNameCheckListener {
            void onCheckComplete(boolean exists);
        }

        private void updateAndSaveExistingTravel() {
            EditText etTravelName = findViewById(R.id.etModifyTravelName);
            EditText etPeopleNumber = findViewById(R.id.etModifyPeopleNumber);

            String travelName = etTravelName.getText().toString().trim();
            String peopleNumberStr = etPeopleNumber.getText().toString().trim();

            if (travelName.isEmpty()) {
                handleError("Veuillez indiquer un nom de voyage", "Veuillez indiquer un nom de voyage");
                return;
            }

            isTravelNameExistsForUpdate(travelName, exists -> {
                if (exists) {
                    handleError("Ce nom de voyage existe déjà", "Un autre voyage porte ce nom.");
                    saveModifyBtn.setEnabled(true);
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

                List<Keypoint> selectedList = KpListHolderModify.selectedKeypointsModify;
                if (selectedList.isEmpty()) {
                    handleError("Vous n'avez pas de lieu sélectionné", "Sélectionnez au moins un lieu.");
                    return;
                }

                // Synchronisation des dates
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                try {
                    for (Keypoint kp : selectedList) {
                        String start = SelectedKeypointsModifyAdapter.visitStartDates.get(kp.id);
                        String end   = SelectedKeypointsModifyAdapter.visitEndDates.get(kp.id);
                        if (start != null) kp.startDate = start;
                        if (end != null) kp.endDate = end;
                    }
                } catch (Exception ex) {
                    handleError("Erreur de dates", "Vérifiez les dates sélectionnées.");
                    return;
                }

                // Vérification des chevauchements
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

                JSONObject travelData = new JSONObject();
                try {
                    travelData.put("travel_id", currentTravel.id);
                    travelData.put("travel_name", travelName);
                    travelData.put("people_number", peopleNumber);
                    travelData.put("individual_price", KpListHolderModify.calculateIndividualPrice());
                    travelData.put("total_price", KpListHolderModify.calculateTotalPrice(peopleNumber));
                    travelData.put("travel_start_date", sdf.format(findEarliest(selectedList)));
                    travelData.put("travel_end_date",   sdf.format(findLatest(selectedList)));
                    travelData.put("user_id", currentUserId);
                } catch (JSONException e) {
                    handleError("Erreur JSON", "Impossible de préparer la mise à jour.");
                    return;
                }

                String url = "http://10.0.2.2/www/PPE_Travist/travist/public/api/updateTravel/" + currentTravel.id;

                StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                        response -> {
                            try {
                                JSONObject jsonResponse = new JSONObject(response);
                                Log.i("HELLOJWT", "Réponse JSON: " + response);
                                boolean success = jsonResponse.optBoolean("success", false);

                                if (success) {
                                    JSONObject tData = jsonResponse.optJSONObject("data");
                                    if (tData != null) {
                                        Log.i("HELLOJWT", "ID du voyage modifié : " + currentTravel.id);
                                        if (currentTravel.id != -1) {
                                            updateAssigned(currentTravel.id, selectedList);
                                            handleSuccess("Voyage modifié avec succès !", "Voyage modifié avec succès !");
                                        } else {
                                            handleError("Erreur lors de la modification du voyage", "Erreur lors de la modification du voyage");
                                        }
                                    }
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

        private void isTravelNameExistsForUpdate(String nameToCheck, OnTravelNameCheckListener listener) {
            String url = "http://10.0.2.2/www/PPE_Travist/travist/public/api/getTravelsByUser/" + currentUserId;
            StringRequest req = new StringRequest(Request.Method.GET, url,
                    response -> {
                        try {
                            JSONArray jsonArray = new JSONArray(response);
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject t = jsonArray.getJSONObject(i);
                                int id = t.getInt("id");
                                String existing = t.getString("travel_name").trim();
                                if (existing.equalsIgnoreCase(nameToCheck) && id != currentTravel.id) {
                                    listener.onCheckComplete(true);
                                    return;
                                }
                            }
                            listener.onCheckComplete(false);
                        } catch (JSONException e) {
                            listener.onCheckComplete(false);
                        }
                    },
                    error -> listener.onCheckComplete(false)
            );
            rq.add(req);
        }

        private void updateAssigned(int travelId, List<Keypoint> keypoints) {
            JSONObject assignedData = new JSONObject();
            try {
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
            String url = "http://10.0.2.2/www/PPE_Travist/travist/public/api/updateAssigned/" + currentTravel.id;

            StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                    response -> {
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            Log.i("HELLOJWT", "Réponse assignation keypoints : " + response);
                            if (jsonResponse.optBoolean("success", false)) {
                                Toast.makeText(this, "Lieux assignés avec succès", Toast.LENGTH_SHORT).show();
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

        private void handleErrors(VolleyError err) {
            Toast.makeText(this, "Erreur réseau", Toast.LENGTH_SHORT).show();
            Log.e("MODIFY_TRAVEL", err.toString());
        }

        private void handleError(String tag, String msg) {
            Toast.makeText(this, tag + ": " + msg, Toast.LENGTH_LONG).show();
            Log.e("MODIFY_TRAVEL", tag, new Exception(msg));
        }

        private void handleSuccess(String logMessage, String toastMessage) {
            Log.i("PlanifyActivity", logMessage);
            Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();
            KpListHolderModify.resetKeypoints();
            saveModifyBtn.setEnabled(true);
        }
    }
