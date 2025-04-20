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
    private static final int PICK_IMAGE = 1001;

    private EditText etName, etPrice, etStartDate, etEndDate, etX, etY;
    private Spinner spinnerCity;
    private LinearLayout tagsContainer;
    private Button btnAddTag, btnChooseImage, btnSave;
    private ImageView ivCover;

    private List<City> cityList = new ArrayList<>();
    private List<Tag> tagList = new ArrayList<>();
    private List<Spinner> tagSpinners = new ArrayList<>();
    private String base64Cover = "";

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

        // Charger villes & tags
        loadCities();
        loadTags();

        // Premier spinner de tag
        addTagSpinner();

        btnAddTag.setOnClickListener(v -> addTagSpinner());
        btnChooseImage.setOnClickListener(v -> pickImage());
        btnSave.setOnClickListener(v -> createKeypoint());
    }

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

    // Chargement des villes
    private void loadCities() {
        String url = "http://10.0.2.2/www/PPE_Travist/travist/public/api/getCities";
        Volley.newRequestQueue(this).add(new StringRequest(Request.Method.GET, url,
                this::onCitiesLoaded,
                err -> Toast.makeText(this, "Erreur chargement villes", Toast.LENGTH_SHORT).show()
        ) {
            @Override public Map<String, String> getHeaders() throws AuthFailureError {
                return new HashMap<>();
            }
        });
    }

    private void onCitiesLoaded(String response) {
        try {
            JSONArray arr = new JSONArray(response);
            cityList.clear();
            List<String> names = new ArrayList<>();
            for (int i=0; i<arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
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

    // Chargement des tags
    private void loadTags() {
        String url = "http://10.0.2.2/www/PPE_Travist/travist/public/api/getTags";
        Volley.newRequestQueue(this).add(new StringRequest(Request.Method.GET, url,
                this::onTagsLoaded,
                err -> Toast.makeText(this, "Erreur chargement tags", Toast.LENGTH_SHORT).show()
        ) {
            @Override public Map<String, String> getHeaders() throws AuthFailureError {
                return new HashMap<>();
            }
        });
    }

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

    private void addTagSpinner() {
        Spinner sp = new Spinner(this);
        populateTagSpinner(sp);
        tagsContainer.addView(sp);
        tagSpinners.add(sp);
    }

    private void populateTagSpinner(Spinner spinner) {
        List<String> names = new ArrayList<>();
        for (Tag t : tagList) names.add(t.name);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    // Choix d'image et conversion Base64
    private void pickImage() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("image/*");
        startActivityForResult(i, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req==PICK_IMAGE && res==Activity.RESULT_OK && data!=null) {
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

    private void createKeypoint() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        Date d1;
        Date d2;
        try {
            d1 = sdf.parse(String.valueOf(etStartDate.getText()));
            d2 = sdf.parse(String.valueOf(etEndDate.getText()));

            if (d1.after(d2)) {
                Toast.makeText(this, "Veuillez choisir une date de début avant la date de fin", Toast.LENGTH_SHORT).show(); return;
            }
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        if (TextUtils.isEmpty(etName.getText())) {
            etName.setError("Obligatoire"); etName.requestFocus(); return;
        }

        if (TextUtils.isEmpty(etPrice.getText())) {
            etPrice.setError("Obligatoire"); etPrice.requestFocus(); return;
        }

        if (TextUtils.isEmpty(etStartDate.getText())) {
            Toast.makeText(this, "Veuillez choisir date de début", Toast.LENGTH_SHORT).show();
            etStartDate.performClick(); return;
        }

        if (TextUtils.isEmpty(etEndDate.getText())) {
            Toast.makeText(this, "Veuillez choisir date de fin", Toast.LENGTH_SHORT).show();
            etEndDate.performClick(); return;
        }

        if (Spinner.INVALID_POSITION == spinnerCity.getSelectedItemPosition()) {
            Toast.makeText(this, "Veuillez choisir une ville", Toast.LENGTH_SHORT).show(); return;
        }

        if (base64Cover.isEmpty()) {
            Toast.makeText(this, "Veuillez sélectionner une image", Toast.LENGTH_SHORT).show();
            btnChooseImage.performClick(); return;
        }

        // tags
        JSONArray tagsArr = new JSONArray();
        for (Spinner sp : tagSpinners) {
            int pos = sp.getSelectedItemPosition();
            if (pos<0) {
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

            String url = "http://10.0.2.2/www/PPE_Travist/travist/public/api/createKeypoint";
            JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, url, body,
                    resp -> {
                        Toast.makeText(this, "Keypoint créé !", Toast.LENGTH_SHORT).show();
                        finish();
                    },
                    err -> Toast.makeText(this, "Erreur création : " + err.getMessage(), Toast.LENGTH_LONG).show()
            ) {
                @Override public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String,String> h = new HashMap<>();
                    h.put("Content-Type","application/json; charset=UTF-8");
                    return h;
                }
            };
            Volley.newRequestQueue(this).add(req);

        } catch (JSONException ex) {
            ex.printStackTrace();
            Toast.makeText(this, "Erreur formation requête", Toast.LENGTH_SHORT).show();
        }
    }
}
