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

public class ModifyTagActivity extends AppCompatActivity {
    private RequestQueue rq;
    private EditText etTagName;
    String tagName;
    private Button btnSave;
    private int tagId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_modify_tag);


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });


        rq = Volley.newRequestQueue(this);


        Intent i = getIntent();
        tagId   = i.getIntExtra("tagId", -1);
        String tagName = i.getStringExtra("tagName");
        if (tagId == -1) {
            Toast.makeText(this, "ID de tag invalide", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        etTagName = findViewById(R.id.etTagName);
        btnSave = findViewById(R.id.saveModifiedTag);


        etTagName.setText(tagName);

        btnSave.setOnClickListener(v -> modifyTag());
    }

    private void modifyTag() {
        tagName = etTagName.getText().toString().trim();
        if (!tagName.startsWith("#")) {
            tagName = "#" + tagName;
        }

        String url = "http://10.0.2.2/www/PPE_Travist/travist/public/api/updateTag/" + tagId;

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
                    Toast.makeText(this, "Tag modifié avec succès !", Toast.LENGTH_SHORT).show();
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

        // N’oublie pas d’avoir initialisé rq = Volley.newRequestQueue(this);
        rq.add(req);
    }
}
