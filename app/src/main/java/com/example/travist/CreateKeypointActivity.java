package com.example.travist;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
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
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateKeypointActivity extends AppCompatActivity {
    // Initialisation des variables
    private String token = UserSession.getToken();
    private RequestQueue rq;

    private static final int PICK_IMAGE = 1001;

    EditText etName, etPrice, etStartDate, etEndDate, etX, etY;
    Spinner spinnerCity;
    LinearLayout tagsContainer;
    Button btnAddTag, btnChooseImage, btnSave;
    ImageView ivCover;

    List<City> cityList = new ArrayList<>();
    List<Tag> tagList = new ArrayList<>();
    List<Spinner> tagSpinners = new ArrayList<>();
    String base64Cover = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_keypoint);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets b = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(b.left, b.top, b.right, b.bottom);
            return insets;
        });

        // Initialisation de Volley
        rq = Volley.newRequestQueue(this);

        etName = findViewById(R.id.etKpName);
        etPrice = findViewById(R.id.etKpPrice);
        etStartDate = findViewById(R.id.etKpStartDate);
        etEndDate = findViewById(R.id.etKpEndDate);
        etX = findViewById(R.id.etKpX);
        etY = findViewById(R.id.etKpY);
        spinnerCity = findViewById(R.id.spinnerCity);
        tagsContainer = findViewById(R.id.tagsContainer);
        btnAddTag = findViewById(R.id.btnAddTag);
        btnChooseImage = findViewById(R.id.btnChooseImage);
        ivCover = findViewById(R.id.ivCoverPreview);
        btnSave = findViewById(R.id.addNewKeypoint);

        // Désactiver la saisie directe sur les dates
        disableDirectInputAndShowPicker(etStartDate);
        disableDirectInputAndShowPicker(etEndDate);

        // Appel des méthodes pour charger récupérer les villes et les tags
        loadCities();
        loadTags();

        // Ajout du premier select des tags
        addTagSpinner();

        // Appel des boutons d'ajout d'un tag, de choix d'image et de création d'un lieu
        btnAddTag.setOnClickListener(view -> addTagSpinner());
        btnChooseImage.setOnClickListener(view -> pickImage());
        btnSave.setOnClickListener(view -> createKeypoint());
    }

    // Méthode pour désactiver les DatePicker
    private void disableDirectInputAndShowPicker(EditText et) {
        et.setInputType(0);
        et.setFocusable(false);
        et.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this,
                    (DatePicker view, int y, int m, int d) -> {
                        String formatted = String.format("%04d-%02d-%02d", y, m+1, d);
                        et.setText(formatted);
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
            ).show();
        });
    }

    // Méthode WebService pour récupérer les villes
    private void loadCities() {
        // String url = "http://192.168.0.110/~mathys.raspolini/travist/public/api/getCities";
        String url = "http://10.0.2.2/~mathys.raspolini/travist/public/api/getCities";
        rq.add(new StringRequest(Request.Method.GET, url,
                this::onCitiesLoaded,
                err -> Toast.makeText(this, "Erreur chargement villes", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Accept", "application/json");
                headers.put("Authorization", token);
                return headers;
            }
        });
    }

    // Méthode WebService pour attribuer les villes chargées dans leur select
    private void onCitiesLoaded(String response) {
        try {
            JSONArray jsonArray = new JSONArray(response);
            cityList.clear();
            List<String> names = new ArrayList<>();

            for (int i=0; i < jsonArray.length(); i++) {
                JSONObject o = jsonArray.getJSONObject(i);

                if (o.getInt("id") != 0) {
                    cityList.add(new City(o.getInt("id"), o.getString("city_name"), o.getString("city_country")));
                    names.add(o.getString("city_name"));
                }
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, names);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerCity.setAdapter(adapter);
        } catch (JSONException e) {
            Toast.makeText(this, "Erreur JSON villes", Toast.LENGTH_SHORT).show();
        }
    }

    // Méthode WebService pour récupérer les tags
    private void loadTags() {
        // String url = "http://192.168.0.110/~mathys.raspolini/travist/public/api/getTags";
        String url = "http://10.0.2.2/~mathys.raspolini/travist/public/api/getTags";
        rq.add(new StringRequest(Request.Method.GET, url,
                this::onTagsLoaded,
                err -> Toast.makeText(this, "Erreur chargement tags", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Accept", "application/json");
                headers.put("Authorization", token);
                return headers;
            }
        });
    }

    // Méthode WebService pour attribuer les tags chargés dans leur select
    private void onTagsLoaded(String response) {
        try {
            JSONArray arr = new JSONArray(response);
            tagList.clear();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);

                if (o.getInt("id") != 0) {
                    tagList.add(new Tag(o.getInt("id"), o.getString("tag_name")));
                }
            }
            // rafraîchir TOUS les spinners déjà ajoutés
            for (Spinner sp : tagSpinners) populateTagSpinner(sp);
        } catch (JSONException e) {
            Toast.makeText(this, "Erreur JSON tags", Toast.LENGTH_SHORT).show();
        }
    }

    // Méthode pour créer un nouveau select de tags
    private void addTagSpinner() {
        Spinner sp = new Spinner(this);
        populateTagSpinner(sp);
        tagsContainer.addView(sp);
        tagSpinners.add(sp);
    }

    // Méthode pour peupler le select des tags
    private void populateTagSpinner(Spinner spinner) {
        List<String> names = new ArrayList<>();
        for (Tag t : tagList) names.add(t.name);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    // Méthode pour choisir l'image à uploader
    private void pickImage() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("image/*");
        startActivityForResult(i, PICK_IMAGE);
    }

    // Méthode onActivityResult pour ajouter l'image uploadée lors du retour sur l'activité
    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);

        if (req == PICK_IMAGE && res == Activity.RESULT_OK && data != null) {
            try (InputStream is = getContentResolver().openInputStream(data.getData())) {
                Bitmap bmp = BitmapFactory.decodeStream(is);
                ivCover.setVisibility(View.VISIBLE);
                ivCover.setImageBitmap(bmp);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                base64Cover = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Erreur lecture image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Méthode WebService pour créer un nouveau lieu avec vérifications
    private void createKeypoint() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        Date d1;
        Date d2;
        try {
            d1 = sdf.parse(String.valueOf(etStartDate.getText()));
            d2 = sdf.parse(String.valueOf(etEndDate.getText()));

            // Si les dates ne coincide pas (date de début après la fin), on interrompt le code et on renvoie une erreur
            if (d1.after(d2)) {
                Toast.makeText(this, "Veuillez choisir une date de début avant la date de fin", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        // Si le nom du lieu est vide, on interrompt le code et on renvoie une erreur
        if (TextUtils.isEmpty(etName.getText())) {
            etName.setError("Obligatoire"); etName.requestFocus();
            return;
        }

        // Si le prix du lieu est vide, on interrompt le code et on renvoie une erreur
        if (TextUtils.isEmpty(etPrice.getText())) {
            etPrice.setError("Obligatoire"); etPrice.requestFocus();
            return;
        }

        // Si la date de début de visite du lieu est vide, on interrompt le code et on renvoie une erreur
        if (TextUtils.isEmpty(etStartDate.getText())) {
            Toast.makeText(this, "Veuillez choisir date de début", Toast.LENGTH_SHORT).show();
            etStartDate.performClick();
            return;
        }

        // Si la date de fin de visite du lieu est vide, on interrompt le code et on renvoie une erreur
        if (TextUtils.isEmpty(etEndDate.getText())) {
            Toast.makeText(this, "Veuillez choisir date de fin", Toast.LENGTH_SHORT).show();
            etEndDate.performClick();
            return;
        }

        // Si une ville n'est pas sélectionnée, on interrompt le code et on renvoie une erreur
        if (Spinner.INVALID_POSITION == spinnerCity.getSelectedItemPosition()) {
            Toast.makeText(this, "Veuillez choisir une ville", Toast.LENGTH_SHORT).show();
            return;
        }

        // Si une image n'est pas uploadée, on interrompt le code et on renvoie une erreur
        if (base64Cover.isEmpty()) {
            Toast.makeText(this, "Veuillez sélectionner une image", Toast.LENGTH_SHORT).show();
            btnChooseImage.performClick();
            return;
        }

        // Si au moins un tag n'est pas sélectionné, on interrompt le code et on renvoie une erreur
        JSONArray tagsArr = new JSONArray();
        for (Spinner sp : tagSpinners) {
            int pos = sp.getSelectedItemPosition();
            if (pos < 0) {
                Toast.makeText(this, "Un tag n'est pas sélectionné", Toast.LENGTH_SHORT).show();
                return;
            }
            tagsArr.put(tagList.get(pos).id);
        }

        try {
            JSONObject body = new JSONObject();
            body.put("key_point_name", etName.getText().toString().trim());
            body.put("key_point_price", Float.parseFloat(etPrice.getText().toString().trim()));
            body.put("key_point_start_date", etStartDate.getText().toString().trim());
            body.put("key_point_end_date", etEndDate.getText().toString().trim());
            body.put("key_point_cover", base64Cover);
            body.put("key_point_gps_x", Double.parseDouble(etX.getText().toString().trim()));
            body.put("key_point_gps_y", Double.parseDouble(etY.getText().toString().trim()));
            body.put("is_altered_keypoint", 0);
            body.put("city_id", cityList.get(spinnerCity.getSelectedItemPosition()).id);
            body.put("tags", tagsArr);

            // String url = "http://192.168.0.110/~mathys.raspolini/travist/public/api/createKeypoint";
            String url = "http://10.0.2.2/~mathys.raspolini/travist/public/api/createKeypoint";
            JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, url, body,
                    resp -> {
                        Toast.makeText(this, "Keypoint créé !", Toast.LENGTH_SHORT).show();
                        finish();
                    },
                    err -> Toast.makeText(this, "Erreur création : " + err.getMessage(), Toast.LENGTH_LONG).show()
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

        } catch (JSONException ex) {
            ex.printStackTrace();
            Toast.makeText(this, "Erreur formation requête", Toast.LENGTH_SHORT).show();
        }
    }
}
