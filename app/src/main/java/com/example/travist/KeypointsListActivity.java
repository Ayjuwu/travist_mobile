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
    private RecyclerView rvKeypoints;
    private KeypointAllAdapter kpAdapter;
    private List<Keypoint> kpList = new ArrayList<>();
    private Map<Integer, Keypoint> pendingKeypoints = new HashMap<>();
    private Set<Integer> tagsLoaded = new HashSet<>();

    private Map<Integer, String> cityMap = new HashMap<>();
    private Map<Integer, String> tagMap = new HashMap<>();
    private String defaultCityName = "Non-définie";
    private String defaultTagName = "#NaN";

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

        rvKeypoints = findViewById(R.id.rvKeypoints);
        rvKeypoints.setLayoutManager(new LinearLayoutManager(this));
        kpAdapter = new KeypointAllAdapter(kpList, this);
        rvKeypoints.setAdapter(kpAdapter);

        // charger d'abord les référentiels, puis les keypoints
        requestAllCities(() -> requestAllTags(this::requestKeypoints));
    }

    private void requestKeypoints() {
        pendingKeypoints.clear();
        tagsLoaded.clear();
        kpList.clear();
        kpAdapter.notifyDataSetChanged();

        String url = "http://10.0.2.2/www/PPE_Travist/travist/public/api/getKeypoints";
        Volley.newRequestQueue(this).add(
                new StringRequest(Request.Method.GET, url,
                        this::processKeypoints,
                        err -> handleError("Keypoints load error", "Erreur réseau")){
                    @Override public Map<String,String> getHeaders() throws AuthFailureError { return new HashMap<>(); }
                }
        );
    }

    private void processKeypoints(String response) {
        try {
            JSONArray arr = new JSONArray(response);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject kpJson = arr.getJSONObject(i);
                int id = kpJson.getInt("id");
                Keypoint kp = new Keypoint(
                        id,
                        kpJson.getString("key_point_name"),
                        (float) kpJson.getDouble("key_point_price"),
                        kpJson.getString("key_point_start_date"),
                        kpJson.getString("key_point_end_date"),
                        kpJson.getString("key_point_cover"),
                        (float) kpJson.getDouble("key_point_gps_x"),
                        (float) kpJson.getDouble("key_point_gps_y"),
                        kpJson.getInt("is_altered_keypoint"),
                        kpJson.getInt("city_id")
                );

                // fallback city
                kp.setCityName(cityMap.getOrDefault(kp.cityId, defaultCityName));
                pendingKeypoints.put(id, kp);
                tagsLoaded.remove(id);

                requestTags(id);
            }
        } catch (JSONException e) {
            handleError("JSON error", "Erreur parsing JSON");
        }
    }

    private void requestAllCities(Runnable next) {
        String url = "http://10.0.2.2/www/PPE_Travist/travist/public/api/getCities";
        Volley.newRequestQueue(this).add(
                new StringRequest(Request.Method.GET, url,
                        resp -> {
                            try {
                                JSONArray arr = new JSONArray(resp);
                                for (int i = 0; i < arr.length(); i++) {
                                    JSONObject o = arr.getJSONObject(i);
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
                ){
                    @Override public Map<String, String> getHeaders() throws AuthFailureError { return new HashMap<>(); }
                }
        );
    }

    private void requestAllTags(Runnable next) {
        String url = "http://10.0.2.2/www/PPE_Travist/travist/public/api/getTags";
        Volley.newRequestQueue(this).add(
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
                ){
                    @Override public Map<String, String> getHeaders() throws AuthFailureError { return new HashMap<>(); }
                }
        );
    }

    private void requestTags(int kpId) {
        String url = "http://10.0.2.2/www/PPE_Travist/travist/public/api/getTagsByKeypoint/" + kpId;
        Volley.newRequestQueue(this).add(
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
                        err -> handleError("Tags load error", "Erreur réseau")){
                    @Override public Map<String,String> getHeaders() throws AuthFailureError { return new HashMap<>(); }
                }
        );
    }

    @Override
    public void onModify(Keypoint kp) {
        Intent i = new Intent(this, ModifyKeypointActivity.class);
        i.putExtra("kpId", kp.id);
        startActivity(i);
    }

    @Override
    public void onDelete(Keypoint kp) {
        String url = "http://10.0.2.2/www/PPE_Travist/travist/public/api/deleteKeypoint/" + kp.id;
        Volley.newRequestQueue(this).add(
                new StringRequest(Request.Method.DELETE, url,
                        resp -> {
                            kpList.remove(kp);
                            kpAdapter.notifyDataSetChanged();
                            Toast.makeText(this, "Lieu supprimé", Toast.LENGTH_SHORT).show();
                        },
                        err -> handleError("Delete error", "Erreur suppression")){
                    @Override public Map<String,String> getHeaders() throws AuthFailureError { return new HashMap<>(); }
                }
        );
    }

    private void handleError(String log, String toast) {
        Log.e("KeypointsList", log);
        Toast.makeText(this, toast, Toast.LENGTH_LONG).show();
    }
}
