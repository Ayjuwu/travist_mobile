package com.example.travist;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ModifyCityActivity extends AppCompatActivity {
    // Initialisation des variables
    private RequestQueue rq;
    private String token = UserSession.getToken();
    private int cityId;

    EditText etCityName, etCityCountryName;
    String cityName, cityCountryName;
    Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_modify_city);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        // Initialisation de Volley
        rq = Volley.newRequestQueue(this);

        // Initialisation de l'intent et récupération de ses attributs
        Intent i = getIntent();
        cityId = i.getIntExtra("cityId", -1);
        String cityName = i.getStringExtra("cityName");
        String cityCountryName = i.getStringExtra("cityCountryName");

        etCityName = findViewById(R.id.etCityName);
        etCityCountryName = findViewById(R.id.etCountryName);
        btnSave = findViewById(R.id.saveModifiedCity);

        etCityName.setText(cityName);
        etCityCountryName.setText(cityCountryName);

        // Appel du bouton pour modifier un voyage
        btnSave.setOnClickListener(view -> modifyCity());
    }

    // Méthode WebService pour modifier une ville
    private void modifyCity() {
        cityName = etCityName.getText().toString().trim();
        cityCountryName = etCityCountryName.getText().toString().trim();

        // String url = "http://192.168.0.110/~mathys.raspolini/travist/public/api/updateCity/" + cityId;
        String url = "http://10.0.2.2/~mathys.raspolini/travist/public/api/updateCity/" + cityId;

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("city_name", cityName);
            jsonBody.put("city_country", cityCountryName);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.POST, url, jsonBody,
                response -> {
                    Toast.makeText(this, "Ville modifiée avec succès !", Toast.LENGTH_SHORT).show();
                    finish();
                },
                error -> {
                    error.printStackTrace();
                    Toast.makeText(this,
                            "Erreur de modification : " + error.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
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

        rq.add(req);
    }
}