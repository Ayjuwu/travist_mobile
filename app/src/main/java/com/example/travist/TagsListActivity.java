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

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TagsListActivity extends AppCompatActivity implements TagAdapter.OnTagActionListener {

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

        tagAdapter = new TagAdapter(tagList, this);
        rvTags.setAdapter(tagAdapter);

        requestTags();
    }

    @Override
    protected void onResume() {
        super.onResume();
        requestTags();
    }


    private void requestTags() {
        String url = "http://10.0.2.2/www/PPE_Travist/travist/public/api/getTags";
        StringRequest req = new StringRequest(Request.Method.GET, url,
                this::processTags,
                err -> Toast.makeText(this, "Erreur réseau", Toast.LENGTH_SHORT).show()
        );
        rq.add(req);
    }

    private void processTags(String response) {
        try {
            JSONArray arr = new JSONArray(response);
            tagList.clear();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                tagList.add(new Tag(
                        o.getInt("id"),
                        o.getString("tag_name")
                ));
            }
            tagAdapter.notifyDataSetChanged();
        } catch (JSONException e) {
            Toast.makeText(this, "Erreur JSON", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onModify(Tag tag) {
        Intent intent = new Intent(this, ModifyTagActivity.class);
        intent.putExtra("tagId", tag.id);
        intent.putExtra("tagName", tag.name);
        startActivity(intent);
    }

    @Override
    public void onDelete(Tag tag) {
        String url = "http://10.0.2.2/www/PPE_Travist/travist/public/api/deleteTag/" + tag.id;
        StringRequest req = new StringRequest(Request.Method.DELETE, url,
                response -> {
                    tagList.remove(tag);
                    tagAdapter.notifyDataSetChanged();
                    Toast.makeText(this, "Tag supprimé", Toast.LENGTH_SHORT).show();
                },
                error -> Toast.makeText(this, "Erreur suppression", Toast.LENGTH_SHORT).show()
        );
        rq.add(req);
    }
}