package com.example.travist;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
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

        // Initialisation adapter avec onClick
        adapter = new SliderAdapter(sliderItems, kpId -> {
            Intent intent = new Intent(PlanifyActivity.this, KeypointDetailsActivity.class);
            intent.putExtra("kpId", kpId);
            startActivity(intent);
        });

        viewPager2.setAdapter(adapter);

        // Volley
        rq = Volley.newRequestQueue(this);
        Intent i = getIntent();
        token = i.getStringExtra("token");
        Log.i("HELLOJWT", "token " + token);

        // Appel au WebService
        requestDetails();

        // Bouton retour à l’accueil
        saveBtn.setOnClickListener(view -> {
            Intent intent = new Intent(this, MainActivity.class);
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

                // Récupérer la liste des lieux déjà sélectionnés à partir du singleton
                ArrayList<Integer> selectedKpList = KpListHolder.kpListId;

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject kp = jsonArray.getJSONObject(i);

                    int kpId = kp.getInt("id");
                    int is_altered_keypoint = kp.getInt("is_altered_keypoint");

                    // S'il is_altered_keypoint est vrai, on ne l'affiche pas.
                    if (is_altered_keypoint == 1) {
                        continue;  // Passer ce lieu
                    }

                    // Vérifier si ce lieu est déjà dans la liste des lieux sélectionnés
                    if (selectedKpList.contains(kpId)) {
                        continue;  // Passer ce lieu également
                    }

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
}
