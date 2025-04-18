package com.example.travist;

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

public class KeypointsListActivity extends AppCompatActivity {
    RequestQueue rq;
    RecyclerView rvKeypoints;
    KeypointAllAdapter kpAdapter;
    List<Keypoint> kpList;
    private Map<Integer, Keypoint> pendingKeypoints = new HashMap<>();
    private Set<Integer> cityLoadedIds = new HashSet<>();
    private Set<Integer> tagsLoadedIds = new HashSet<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_keypoints_list);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        rq = Volley.newRequestQueue(this);

        kpList = new ArrayList<>();

        rvKeypoints = findViewById(R.id.rvKeypoints);
        rvKeypoints.setLayoutManager(new LinearLayoutManager(this));

        kpAdapter = new KeypointAllAdapter(kpList);
        rvKeypoints.setAdapter(kpAdapter);

        requestKeypoints();
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

            if (jsonArray.length() != 0) {
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject kp = jsonArray.getJSONObject(i);

                    int kpId = kp.getInt("id");
                    String kpName = kp.getString("key_point_name");
                    float kpPrice = (float) kp.getDouble("key_point_price");
                    String kpStartDate = kp.getString("key_point_start_date");
                    String kpEndDate = kp.getString("key_point_end_date");
                    String kpCover = kp.getString("key_point_cover");
                    float kpX = (float) kp.getDouble("key_point_gps_x");
                    float kpY = (float) kp.getDouble("key_point_gps_y");
                    int is_altered_keypoint = kp.getInt("is_altered_keypoint");
                    int cityId = kp.getInt("city_id");

                    Keypoint keypoint = new Keypoint(kpId, kpName, kpPrice, kpStartDate, kpEndDate, kpCover,
                            kpX, kpY, is_altered_keypoint, cityId);

                    pendingKeypoints.put(kpId, keypoint);
                    requestCity(kpId);
                    requestTags(kpId);;
                }

                kpAdapter.notifyDataSetChanged();
            }
        } catch (JSONException x) {
            handleError("JSON PARSE ERROR: " + response, "Erreur de traitement des données JSON");
        }
    }

    public void requestCity(int kpId) {
        String url = "http://10.0.2.2/www/PPE_Travist/travist/public/api/getCityByKeypoint/" + kpId;
        StringRequest req = new StringRequest(Request.Method.GET, url, response -> {
            try {
                JSONObject jo = new JSONObject(response);
                String cityName = jo.getString("city_name");

                Keypoint kp = pendingKeypoints.get(kpId);
                if (kp != null) {
                    kp.setCityName(cityName);
                    cityLoadedIds.add(kpId);
                    tryAddKeypoint(kpId);
                }
            } catch (JSONException e) {
                handleError("City JSON error: " + response, "Erreur JSON (ville)");
            }
        }, this::handleErrors);

        rq.add(req);
    }

    public void requestTags(int kpId) {
        String url = "http://10.0.2.2/www/PPE_Travist/travist/public/api/getTagsByKeypoint/" + kpId;
        StringRequest req = new StringRequest(Request.Method.GET, url, response -> {
            try {
                JSONArray jsonArray = new JSONArray(response);
                StringBuilder tags = new StringBuilder();
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject tagObj = jsonArray.getJSONObject(i);
                    if (i != 0) tags.append(" ");
                    tags.append(tagObj.getString("tag_name"));
                }

                Keypoint kp = pendingKeypoints.get(kpId);
                if (kp != null) {
                    kp.setTags(tags.toString());
                    tagsLoadedIds.add(kpId);
                    tryAddKeypoint(kpId);
                }
            } catch (JSONException e) {
                handleError("Tags JSON error: " + response, "Erreur JSON (tags)");
            }
        }, this::handleErrors);

        rq.add(req);
    }

    private void tryAddKeypoint(int kpId) {
        if (cityLoadedIds.contains(kpId) && tagsLoadedIds.contains(kpId)) {
            Keypoint kp = pendingKeypoints.remove(kpId);
            if (kp != null) {
                kpList.add(kp);
                kpAdapter.notifyItemInserted(kpList.size() - 1);
            }

            cityLoadedIds.remove(kpId);
            tagsLoadedIds.remove(kpId);
        }
    }

    private void handleErrors(Throwable t) {
        handleError("SERVERSIDE BUG", "Erreur du côté serveur");
    }

    private void handleError(String logMessage, String toastMessage) {
        Log.e("KeypointsListActivity", logMessage);
        Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();
    }
}