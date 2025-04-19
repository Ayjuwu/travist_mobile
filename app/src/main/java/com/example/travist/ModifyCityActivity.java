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

    private RequestQueue rq;
    private EditText etCityName;
    private EditText etCityCountryName;
    String cityName;
    String cityCountryName;
    private Button btnSave;
    private int cityId;

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


        rq = Volley.newRequestQueue(this);


        Intent i = getIntent();
        cityId   = i.getIntExtra("cityId", -1);
        String cityName = i.getStringExtra("cityName");
        String cityCountryName = i.getStringExtra("cityCountryName");
        if (cityId == -1) {
            Toast.makeText(this, "ID de ville invalide", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        etCityName = findViewById(R.id.etCityName);
        etCityCountryName = findViewById(R.id.etCountryName);
        btnSave = findViewById(R.id.saveModifiedCity);


        etCityName.setText(cityName);
        etCityCountryName.setText(cityCountryName);

        btnSave.setOnClickListener(v -> modifyCity());
    }

    private void modifyCity() {
        cityName = etCityName.getText().toString().trim();
        cityCountryName = etCityCountryName.getText().toString().trim();

        String url = "http://10.0.2.2/www/PPE_Travist/travist/public/api/updateCity/" + cityId;

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("city_name", cityName);
            jsonBody.put("city_country", cityCountryName);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.POST,
                url,
                jsonBody,
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
            public Map<String,String> getHeaders() throws AuthFailureError {
                Map<String,String> hdrs = new HashMap<>();
                hdrs.put("Content-Type","application/json; charset=UTF-8");
                return hdrs;
            }
        };

        rq.add(req);
    }
}