package com.example.travist;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class AdminPanelActivity extends AppCompatActivity {
    Button viewListKeypointsBtn;
    Button addNewKeypointBtn;
    Button viewListTagsBtn;
    Button addNewTagBtn;
    Button viewListCitiesBtn;
    Button addNewCityBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_panel);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        viewListKeypointsBtn = findViewById(R.id.viewListKeypoints);
        addNewKeypointBtn = findViewById(R.id.addNewKeypoint);
        viewListTagsBtn = findViewById(R.id.viewListTags);
        addNewTagBtn = findViewById(R.id.addNewTag);
        viewListCitiesBtn = findViewById(R.id.viewListCities);
        addNewCityBtn = findViewById(R.id.addNewCity);


        viewListKeypointsBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(AdminPanelActivity.this, KeypointsListActivity.class);
                startActivity(intent);
            }
        });

        addNewKeypointBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(AdminPanelActivity.this, PlanifyTravelActivity.class);
                startActivity(intent);
            }
        });

        viewListTagsBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(AdminPanelActivity.this, TagsListActivity.class);
                startActivity(intent);
            }
        });

        addNewTagBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(AdminPanelActivity.this, PlanifyTravelActivity.class);
                startActivity(intent);
            }
        });

        viewListCitiesBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(AdminPanelActivity.this, CitiesListActivity.class);
                startActivity(intent);
            }
        });

        addNewCityBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(AdminPanelActivity.this, PlanifyTravelActivity.class);
                startActivity(intent);
            }
        });
    }
}