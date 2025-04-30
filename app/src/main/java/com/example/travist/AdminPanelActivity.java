package com.example.travist;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class AdminPanelActivity extends AppCompatActivity {
    // Initialisation des variables
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

        // Attribution des variables
        viewListKeypointsBtn = findViewById(R.id.viewListKeypoints);
        addNewKeypointBtn = findViewById(R.id.addNewKeypoint);
        viewListTagsBtn = findViewById(R.id.viewListTags);
        addNewTagBtn = findViewById(R.id.addNewTag);
        viewListCitiesBtn = findViewById(R.id.viewListCities);
        addNewCityBtn = findViewById(R.id.addNewCity);


        // Redirection vers la liste des lieux
        viewListKeypointsBtn.setOnClickListener(view -> {
            Intent intent = new Intent(this, KeypointsListActivity.class);
            startActivity(intent);
        });

        // Redirection vers le formulaire de création d'un lieu
        addNewKeypointBtn.setOnClickListener(view -> {
            Intent intent = new Intent(this, CreateKeypointActivity.class);
            startActivity(intent);
        });

        // Redirection vers la liste des tags
        viewListTagsBtn.setOnClickListener(view -> {
            Intent intent = new Intent(this, TagsListActivity.class);
            startActivity(intent);
        });

        // Redirection vers le formulaire de création d'un tag
        addNewTagBtn.setOnClickListener(view -> {
            Intent intent = new Intent(this, CreateTagActivity.class);
            startActivity(intent);
        });

        // Redirection vers la liste des villes
        viewListCitiesBtn.setOnClickListener(view -> {
            Intent intent = new Intent(this, CitiesListActivity.class);
            startActivity(intent);
        });

        // Redirection vers le formulaire de création d'une ville
        addNewCityBtn.setOnClickListener(view -> {
            Intent intent = new Intent(this, CreateCityActivity.class);
            startActivity(intent);
        });
    }
}