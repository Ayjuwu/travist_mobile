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
import java.util.List;
import java.util.Map;

public class TagsListActivity extends AppCompatActivity {

    RequestQueue rq;
    RecyclerView rvTags;
    TagAdapter tagAdapter;
    List<Tag> tagList;


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

        rq = Volley.newRequestQueue(this);

        tagList = new ArrayList<>();

        rvTags = findViewById(R.id.rvTags);
        rvTags.setLayoutManager(new LinearLayoutManager(this));

        tagAdapter = new TagAdapter(tagList);
        rvTags.setAdapter(tagAdapter);

        requestTags();
    }

    private void requestTags() {
        String url = "http://10.0.2.2/www/PPE_Travist/travist/public/api/getTags";

        StringRequest req = new StringRequest(Request.Method.GET, url, this::processTags, this::handleErrors) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return new HashMap<>();
            }
        };

        rq.add(req);
    }

    private void processTags(String response) {
        try {
            JSONArray jsonArray = new JSONArray(response);

            if (jsonArray.length() != 0) {
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject tag = jsonArray.getJSONObject(i);

                    int tagId = tag.getInt("id");
                    String tagName = tag.getString("tag_name");

                    Tag t = new Tag(tagId, tagName);
                    tagList.add(t);
                }

                tagAdapter.notifyDataSetChanged();
            }
        } catch (JSONException x) {
            handleError("JSON PARSE ERROR: " + response, "Erreur de traitement des données JSON");
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