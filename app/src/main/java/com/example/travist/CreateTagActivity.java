package com.example.travist;

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

public class CreateTagActivity extends AppCompatActivity {
    private RequestQueue rq;
    private EditText etTagName;
    private Button btnNewSave;
    private String tagName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_tag);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        rq = Volley.newRequestQueue(this);

        etTagName = findViewById(R.id.etTagName);

        btnNewSave = findViewById(R.id.addTagBtn);
        btnNewSave.setOnClickListener(v -> addTag());
    }

    private void addTag() {
        tagName = etTagName.getText().toString().trim();
        if (!tagName.startsWith("#")) {
            tagName = "#" + tagName;
        }

        String url = "http://10.0.2.2/www/PPE_Travist/travist/public/api/createTag";

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("tag_name", tagName);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.POST,
                url,
                jsonBody,
                response -> {
                    Toast.makeText(this, "Tag créé avec succès !", Toast.LENGTH_SHORT).show();
                    finish();
                },
                error -> {
                    error.printStackTrace();
                    Toast.makeText(this,
                            "Erreur de création : " + error.getMessage(),
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

        // N’oublie pas d’avoir initialisé rq = Volley.newRequestQueue(this);
        rq.add(req);
    }
}