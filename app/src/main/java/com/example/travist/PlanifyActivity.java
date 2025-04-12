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
    RequestQueue rq;
    String token;
    private Button saveBtn;

    private ViewPager2 viewPager2;
    private SliderAdapter adapter;
    private List<SliderItem> sliderItems = new ArrayList<>();

    private Handler sliderHandler = new Handler();  // Handler pour gérer le défilement
    private Runnable sliderRunnable = new Runnable() {
        @Override
        public void run() {
            int currentItem = viewPager2.getCurrentItem();
            int nextItem = currentItem + 1;  // Passer à l'élément suivant
            if (nextItem >= sliderItems.size()) {
                nextItem = 0;  // Boucler à partir du début
            }
            viewPager2.setCurrentItem(nextItem, true);  // Défilement avec animation fluide
            sliderHandler.postDelayed(this, 3000);  // Redéfinir la tâche pour l'exécution dans 3 secondes
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_planify_page);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialisation du ViewPager et de l’adapter
        viewPager2 = findViewById(R.id.viewPagerImageSlider);
        adapter = new SliderAdapter(sliderItems);
        viewPager2.setAdapter(adapter);

        // Volley
        rq = Volley.newRequestQueue(this);
        Intent i = getIntent();
        token = i.getStringExtra("token");
        Log.i("HELLOJWT", "token " + token);

        // Appel des données
        this.requestDetails();

        // Bouton retour à MainActivity
        saveBtn = findViewById(R.id.saveNewTravelBtn);
        saveBtn.setOnClickListener(view -> {
            Intent intent = new Intent(PlanifyActivity.this, MainActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Lancer le défilement automatique lorsque l'activité démarre
        sliderHandler.postDelayed(sliderRunnable, 2250); // Délai
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Arrêter le défilement automatique lorsque l'activité s'arrête
        sliderHandler.removeCallbacks(sliderRunnable);
    }

    public void requestDetails() {
        String url = "http://10.0.2.2/www/PPE_Travist/travist/public/api/getKeypoints";

        StringRequest req = new StringRequest(Request.Method.GET, url, this::processDetails, this::handleErrors) {
            public Map<String, String> getHeaders() throws AuthFailureError {
                // Ajoute ici ton header Authorization si besoin
                return new HashMap<>();
            }
        };

        rq.add(req);
    }

    public void processDetails(String response) {
        try {
            JSONArray jsonArray = new JSONArray(response);

            if (jsonArray.length() != 0) {
                sliderItems.clear(); // On vide le tableau si non-vide

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject kp = jsonArray.getJSONObject(i);
                    String kpCover = kp.getString("key_point_cover");
                    sliderItems.add(new SliderItem(kpCover));
                }

                // Notifier l'adapter des changements
                adapter.notifyDataSetChanged();
            }

        } catch (JSONException x) {
            Toast.makeText(this, "JSON PARSE ERROR", Toast.LENGTH_LONG).show();
            Log.e("HELLOJWT", "JSON PARSE ERROR: " + response, x);
        }
    }

    public void handleErrors(Throwable t) {
        Toast.makeText(this, "SERVERSIDE PROBLEM", Toast.LENGTH_LONG).show();
        Log.e("HELLOJWT", "SERVERSIDE BUG", t);
    }
}
