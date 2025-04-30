package com.example.travist;

import android.content.Intent;
import android.os.Bundle;
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
import java.util.List;
import java.util.Map;

public class TagsListActivity extends AppCompatActivity implements TagAdapter.OnTagActionListener {
    // Initialisation des variables
    private RequestQueue rq;
    private String token = UserSession.getToken();

    RecyclerView rvTags;
    TagAdapter tagAdapter;
    List<Tag> tagList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_tags_list);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialisation de Volley
        rq = Volley.newRequestQueue(this);

        // Initialisation de la RecyclerView et de l'Adapter
        rvTags = findViewById(R.id.rvTags);
        rvTags.setLayoutManager(new LinearLayoutManager(this));

        tagAdapter = new TagAdapter(tagList, this);
        rvTags.setAdapter(tagAdapter);

        // Appel pour récupérer tous les tags
        requestTags();
    }

    // Méthode onResume pour update la RecyclerView en rappelant le WebService lorsque l'on revient d'une activité
    @Override
    protected void onResume() {
        super.onResume();
        requestTags();
    }

    // Méthode WebService pour récupérer tous les tags
    private void requestTags() {
        // String url = "http://192.168.0.110/~mathys.raspolini/travist/public/api/getTags";
        String url = "http://10.0.2.2/~mathys.raspolini/travist/public/api/getTags";
        StringRequest req = new StringRequest(Request.Method.GET, url,
                this::processTags,
                err -> Toast.makeText(this, "Erreur réseau", Toast.LENGTH_SHORT).show()
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

    // Méthode WebService pour procéder à la récupération de tous les tags
    private void processTags(String response) {
        try {
            JSONArray arr = new JSONArray(response);
            tagList.clear();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);

                if (o.getInt("id") != 0) {
                    tagList.add(new Tag(
                            o.getInt("id"),
                            o.getString("tag_name")
                    ));
                }
            }
            tagAdapter.notifyDataSetChanged();
        } catch (JSONException e) {
            Toast.makeText(this, "Erreur JSON", Toast.LENGTH_SHORT).show();
        }
    }

    // Méthode appelée dans l'adapter des tags lorsque l'on modifie l'un d'entre-eux
    @Override
    public void onModify(Tag tag) {
        Intent intent = new Intent(this, ModifyTagActivity.class);
        intent.putExtra("tagId", tag.id);
        intent.putExtra("tagName", tag.name);
        startActivity(intent);
    }

    // Méthode appelée dans l'adapter des tags lorsque l'on supprime l'un d'entre-eux
    @Override
    public void onDelete(Tag tag) {
        // String url = "http://192.168.0.110/~mathys.raspolini/travist/public/api/deleteTag/" + tag.id;
        String url = "http://10.0.2.2/~mathys.raspolini/travist/public/api/deleteTag/" + tag.id;
        StringRequest req = new StringRequest(Request.Method.POST, url,
                response -> {
                    tagList.remove(tag);
                    tagAdapter.notifyDataSetChanged();
                    Toast.makeText(this, "Tag supprimé", Toast.LENGTH_SHORT).show();
                },
                error -> Toast.makeText(this, "Erreur suppression", Toast.LENGTH_SHORT).show()
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