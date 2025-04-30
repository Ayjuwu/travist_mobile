package com.example.travist;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class KeypointsListActivity extends AppCompatActivity implements KeypointAllAdapter.OnKpActionListener {
    // Initialisation des variables
    private RequestQueue rq;
    private String token = UserSession.getToken();

    RecyclerView rvKeypoints;
    KeypointAllAdapter kpAdapter;
    List<Keypoint> kpList = new ArrayList<>();
    Map<Integer, Keypoint> pendingKeypoints = new HashMap<>();
    Set<Integer> tagsLoaded = new HashSet<>();

    Map<Integer, String> cityMap = new HashMap<>();
    Map<Integer, String> tagMap = new HashMap<>();
    String defaultCityName = "Non-définie";
    String defaultTagName = "#NaN";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_keypoints_list);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        // Initialisation de Volley
        rq = Volley.newRequestQueue(this);

        // Initialisation de la RecyclerView et de l'Adapter
        rvKeypoints = findViewById(R.id.rvKeypoints);
        rvKeypoints.setLayoutManager(new LinearLayoutManager(this));
        kpAdapter = new KeypointAllAdapter(kpList, this);
        rvKeypoints.setAdapter(kpAdapter);

        // On appelle d'abord la récupération des villes, puis des lieux
        requestAllCities(() -> requestAllTags(this::requestKeypoints));
    }

    // Méthode WebService pour récupérer tous les lieux
    private void requestKeypoints() {
        pendingKeypoints.clear();
        tagsLoaded.clear();
        kpList.clear();
        kpAdapter.notifyDataSetChanged();

        // String url = "http://192.168.0.110/~mathys.raspolini/travist/public/api/getKeypoints";
        String url = "http://10.0.2.2/~mathys.raspolini/travist/public/api/getKeypoints";
        rq.add(
                new StringRequest(Request.Method.GET, url,
                        this::processKeypoints,
                        err -> handleError("Keypoints load error", "Erreur réseau")
                ){
                    @Override
                    public Map<String, String> getHeaders() throws AuthFailureError {
                        Map<String, String> headers = new HashMap<>();
                        headers.put("Accept", "application/json");
                        headers.put("Authorization", token);
                        return headers;
                    }
                }
        );
    }

    // Méthode WebService procéder à la récupération de tous les lieux
    private void processKeypoints(String response) {
        try {
            JSONArray jsonArray = new JSONArray(response);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject kp = jsonArray.getJSONObject(i);

                int id = kp.getInt("id");
                String kpName = kp.getString("key_point_name");
                float kpPrice  = (float) kp.getDouble("key_point_price");
                String kpStartDate = kp.getString("key_point_start_date");
                String kpEndDate = kp.getString("key_point_end_date");
                String kpCover = kp.getString("key_point_cover");
                float kpX = (float) kp.getDouble("key_point_gps_x");
                float kpY = (float) kp.getDouble("key_point_gps_y");
                int is_altered = kp.getInt("is_altered_keypoint");
                int cityId = kp.getInt("city_id");

                Keypoint keypoint = new Keypoint(id, kpName, kpPrice, kpStartDate, kpEndDate, kpCover, kpX,
                        kpY, is_altered, cityId);

                keypoint.setCityName(cityMap.getOrDefault(keypoint.cityId, defaultCityName));
                pendingKeypoints.put(id, keypoint);
                tagsLoaded.remove(id);

                requestTags(id);
            }
        } catch (JSONException e) {
            handleError("JSON error", "Erreur parsing JSON");
        }
    }

    // Méthode WebService pour récupérer toutes les villes
    private void requestAllCities(Runnable next) {
        // String url = "http://192.168.0.110/~mathys.raspolini/travist/public/api/getCities";
        String url = "http://10.0.2.2/~mathys.raspolini/travist/public/api/getCities";
        rq.add(
                new StringRequest(Request.Method.GET, url,
                        resp -> {
                            try {
                                JSONArray jsonArray = new JSONArray(resp);
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    JSONObject o = jsonArray.getJSONObject(i);

                                    int id = o.getInt("id");
                                    String name = o.getString("city_name");
                                    cityMap.put(id, name);
                                }
                                defaultCityName = cityMap.getOrDefault(0, defaultCityName);
                                next.run();
                            } catch (JSONException e) {
                                next.run();
                            }
                        },
                        err -> next.run()
                ) {
                    @Override
                    public Map<String, String> getHeaders() throws AuthFailureError {
                        Map<String, String> headers = new HashMap<>();
                        headers.put("Accept", "application/json");
                        headers.put("Authorization", token);
                        return headers;
                    }
                }
        );
    }

    // Méthode WebService pour récupérer tous les tags
    private void requestAllTags(Runnable next) {
        // String url = "http://192.168.0.110/~mathys.raspolini/travist/public/api/getTags";
        String url = "http://10.0.2.2/~mathys.raspolini/travist/public/api/getTags";
        rq.add(
                new StringRequest(Request.Method.GET, url,
                        resp -> {
                            try {
                                JSONArray arr = new JSONArray(resp);
                                for (int i = 0; i < arr.length(); i++) {
                                    JSONObject o = arr.getJSONObject(i);
                                    int id = o.getInt("id");
                                    String name = o.getString("tag_name");
                                    tagMap.put(id, name);
                                }
                                defaultTagName = tagMap.getOrDefault(0, defaultTagName);
                                next.run();
                            } catch (JSONException e) {
                                next.run();
                            }
                        },
                        err -> next.run()
                ) {
                    @Override
                    public Map<String, String> getHeaders() throws AuthFailureError {
                        Map<String, String> headers = new HashMap<>();
                        headers.put("Accept", "application/json");
                        headers.put("Authorization", token);
                        return headers;
                    }
                }
        );
    }

    // Méthode WebService pour récupérer tous les tags d'un lieu
    private void requestTags(int kpId) {
        // String url = "http://192.168.0.110/~mathys.raspolini/travist/public/api/getTagsByKeypoint/" + kpId;
        String url = "http://10.0.2.2/~mathys.raspolini/travist/public/api/getTagsByKeypoint/" + kpId;
        rq.add(
                new StringRequest(Request.Method.GET, url,
                        response -> {
                            Keypoint kp = pendingKeypoints.get(kpId);
                            String tags;
                            try {
                                JSONArray arr = new JSONArray(response);
                                if (arr.length() == 0) {
                                    tags = defaultTagName;
                                } else {
                                    StringBuilder sb = new StringBuilder();
                                    for (int j = 0; j < arr.length(); j++) {
                                        if (j > 0) sb.append(" ");
                                        sb.append(arr.getJSONObject(j).getString("tag_name"));
                                    }
                                    tags = sb.toString();
                                }
                                if (kp != null) {
                                    kp.setTags(tags);
                                    // on peut ajouter
                                    kpList.add(kp);
                                    kpAdapter.notifyItemInserted(kpList.size() - 1);
                                    pendingKeypoints.remove(kpId);
                                }
                            } catch (JSONException e) {
                                // faute pivot tags
                            }
                        },
                        err -> handleError("Tags load error", "Erreur réseau")
                ) {
                    @Override
                    public Map<String, String> getHeaders() throws AuthFailureError {
                        Map<String, String> headers = new HashMap<>();
                        headers.put("Accept", "application/json");
                        headers.put("Authorization", token);
                        return headers;
                    }
                }
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        requestKeypoints();
    }

    // Méthode appelée dans l'adapter des lieux lorsque l'on modifie l'un d'entre-eux
    @Override
    public void onModify(Keypoint kp) {
        Intent i = new Intent(this, ModifyKeypointActivity.class);
        i.putExtra("kpId", kp.id);
        startActivity(i);
    }

    // Méthode appelée dans l'adapter des lieux lorsque l'on supprime l'un d'entre-eux
    @Override
    public void onDelete(Keypoint kp) {
        // String url = "http://192.168.0.110/~mathys.raspolini/travist/public/api/deleteKeypoint/" + kp.id;
        String url = "http://10.0.2.2/~mathys.raspolini/travist/public/api/deleteKeypoint/" + kp.id;
        rq.add(
                new StringRequest(Request.Method.POST, url,
                        resp -> {
                            kpList.remove(kp);
                            kpAdapter.notifyDataSetChanged();
                            Toast.makeText(this, "Lieu supprimé", Toast.LENGTH_SHORT).show();
                        },
                        err -> handleError("Delete error", "Erreur suppression")
                ) {
                    @Override
                    public Map<String, String> getHeaders() throws AuthFailureError {
                        Map<String, String> headers = new HashMap<>();
                        headers.put("Accept", "application/json");
                        headers.put("Authorization", token);
                        return headers;
                    }
                }
        );
    }

    // Méthode destinée à la gestion des erreurs
    private void handleError(String log, String toast) {
        Log.e("KeypointsList", log);
        Toast.makeText(this, toast, Toast.LENGTH_LONG).show();
    }
}
