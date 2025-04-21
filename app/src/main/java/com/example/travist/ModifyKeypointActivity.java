package com.example.travist;

import android.annotation.SuppressLint;
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
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModifyKeypointActivity extends AppCompatActivity {
    private static final int PICK_IMAGE = 1001;

    private int kpId;
    private EditText etName, etPrice, etStart, etEnd, etX, etY;
    private Spinner spinnerCity;
    private LinearLayout tagsContainer;
    private Button btnAddTag, btnChooseImage, btnSave;
    private ImageView ivCover;
    private String base64Cover = "";

    private List<City> cityList = new ArrayList<>();
    private List<Tag> tagList   = new ArrayList<>();
    private List<Spinner> tagSpinners = new ArrayList<>();

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_modify_keypoint);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets b = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(b.left, b.top, b.right, b.bottom);
            return insets;
        });

        kpId = getIntent().getIntExtra("kpId", -1);
        if (kpId < 0) {
            Toast.makeText(this, "ID invalide", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        etName = findViewById(R.id.etKpName);
        etPrice = findViewById(R.id.etKpPrice);
        etStart = findViewById(R.id.etKpStartDate);
        etEnd = findViewById(R.id.etKpEndDate);
        etX = findViewById(R.id.etKpX);
        etY = findViewById(R.id.etKpY);
        spinnerCity = findViewById(R.id.spinnerCity);
        tagsContainer = findViewById(R.id.tagsContainer);
        btnAddTag = findViewById(R.id.btnAddTag);
        btnChooseImage = findViewById(R.id.btnChooseImage);
        ivCover = findViewById(R.id.ivCoverPreview);
        btnSave = findViewById(R.id.saveModifiedKeypoint);

        setupDatePicker(etStart);
        setupDatePicker(etEnd);

        btnAddTag.setOnClickListener(v -> addTagSpinner());
        btnChooseImage.setOnClickListener(v -> pickImage());
        btnSave.setOnClickListener(v -> sendUpdate());

        loadCities();
        loadTags();
        loadKeypoint();
    }

    private void setupDatePicker(EditText et) {
        et.setInputType(0);
        et.setFocusable(false);
        et.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this,
                    (DatePicker dp, int y, int m, int d) ->
                            et.setText(String.format("%04d-%02d-%02d", y, m+1, d)),
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)
            ).show();
        });
    }

    private void loadCities() {
        String url = "http://10.0.2.2/www/PPE_Travist/travist/public/api/getCities";
        StringRequest req = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        onCitiesLoaded(response);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Erreur parsing villes", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Erreur villes", Toast.LENGTH_SHORT).show()
        ) {
            @Override public Map<String, String> getHeaders() throws AuthFailureError {
                return new HashMap<>();
            }
        };
        Volley.newRequestQueue(this).add(req);
    }

    private void onCitiesLoaded(String resp) throws JSONException {
        JSONArray A = new JSONArray(resp);
        cityList.clear();
        List<String> names = new ArrayList<>();
        for(int i=0;i<A.length();i++){
            JSONObject o=A.getJSONObject(i);

            if (o.getInt("id") != 0) {
                cityList.add(new City(o.getInt("id"), o.getString("city_name"), o.getString("city_country")));
                names.add(o.getString("city_name"));
            }
        }
        spinnerCity.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names));
    }

    private void loadTags() {
        // String url = "http://192.168.0.110/~mathys.raspolini/travist/public/api/getTags";
        String url = "http://10.0.2.2/~mathys.raspolini/travist/public/api/getTags";
        StringRequest req = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        onTagsLoaded(response);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Erreur parsing tags", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Erreur tags", Toast.LENGTH_SHORT).show()
        ) {
            @Override public Map<String, String> getHeaders() throws AuthFailureError {
                return new HashMap<>();
            }
        };
        Volley.newRequestQueue(this).add(req);
    }

    private void onTagsLoaded(String resp) throws JSONException {
        JSONArray A = new JSONArray(resp);
        tagList.clear();
        for(int i=0;i<A.length();i++){
            JSONObject o=A.getJSONObject(i);
            if (o.getInt("id") != 0) {
                tagList.add(new Tag(o.getInt("id"), o.getString("tag_name")));
            }
        }
        for(Spinner sp: tagSpinners) populateTagSpinner(sp);
    }

    private void addTagSpinner() {
        Spinner sp = new Spinner(this);
        populateTagSpinner(sp);
        tagsContainer.addView(sp);
        tagSpinners.add(sp);
    }

    private void populateTagSpinner(Spinner sp){
        List<String> N = new ArrayList<>();
        for(Tag t: tagList) N.add(t.name);
        sp.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, N));
    }

    private void pickImage() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("image/*");
        startActivityForResult(i, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int req,int res,Intent data) {
        super.onActivityResult(req,res,data);
        if(req==PICK_IMAGE && res==Activity.RESULT_OK && data!=null){
            try(InputStream is=getContentResolver().openInputStream(data.getData())){
                Bitmap bmp=BitmapFactory.decodeStream(is);
                ivCover.setImageBitmap(bmp);
                ivCover.setVisibility(View.VISIBLE);
                ByteArrayOutputStream baos=new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.JPEG,80,baos);
                base64Cover=Base64.encodeToString(baos.toByteArray(),Base64.NO_WRAP);
            }catch(Exception e){e.printStackTrace();}
        }
    }

    private void loadKeypoint() {
        // String url = "http://192.168.0.110/~mathys.raspolini/travist/public/api/getKeypointById/" + kpId;
        String url = "http://10.0.2.2/~mathys.raspolini/travist/public/api/getKeypointById/" + kpId;
        StringRequest req = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        onKpDetail(response);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Erreur parsing détail", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this,"Erreur détail",Toast.LENGTH_SHORT).show()
        ){
            @Override public Map<String,String> getHeaders() throws AuthFailureError {
                return new HashMap<>();
            }
        };
        Volley.newRequestQueue(this).add(req);
    }

    private void onKpDetail(String resp) throws JSONException {
        JSONObject o = new JSONObject(resp);
        etName.setText(o.getString("key_point_name"));
        etPrice.setText(o.getString("key_point_price"));
        etStart.setText(o.getString("key_point_start_date"));
        etEnd.setText(o.getString("key_point_end_date"));
        etX.setText(o.getString("key_point_gps_x"));
        etY.setText(o.getString("key_point_gps_y"));
        int cId=o.getInt("city_id");
        for (int i = 0; i < cityList.size(); i++) {
            if (cityList.get(i).id == cId) {
                spinnerCity.setSelection(i);
                break;
            }
        }
        String b64 = o.optString("key_point_cover", "");
        if(!b64.isEmpty()){
            byte[] data = Base64.decode(b64, Base64.DEFAULT);
            ivCover.setImageBitmap(BitmapFactory.decodeByteArray(data,0,data.length));
            ivCover.setVisibility(View.VISIBLE);
            base64Cover = b64;
        }
        // String tagUrl = "http://192.168.0.110/~mathys.raspolini/travist/public/api/getTagsByKeypoint/" + kpId;
        String tagUrl = "http://10.0.2.2/~mathys.raspolini/travist/public/api/getTagsByKeypoint/" + kpId;
        StringRequest tagReq = new StringRequest(Request.Method.GET, tagUrl,
                response -> {
                    try {
                        onPivotTags(response);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {}
        ){
            @Override public Map<String,String> getHeaders() throws AuthFailureError {
                return new HashMap<>();
            }
        };
        Volley.newRequestQueue(this).add(tagReq);
    }

    private void onPivotTags(String resp) throws JSONException {
        JSONArray A = new JSONArray(resp);
        tagsContainer.removeAllViews();
        tagSpinners.clear();
        for (int i = 0; i < A.length(); i++) {
            JSONObject t = A.getJSONObject(i);
            addTagSpinner();
            int tagId = t.getInt("id");
            for (int j = 0; j < tagList.size(); j++) {
                if(tagList.get(j).id == tagId && tagId != 0) {
                    tagSpinners.get(i).setSelection(j);
                    break;
                }
            }
        }
    }

    private void sendUpdate() {
        try {
            SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");
            Date d1=sdf.parse(etStart.getText().toString()),
                    d2=sdf.parse(etEnd.getText().toString());
            if(d1.after(d2)){
                Toast.makeText(this,"Date début doit être avant date fin",Toast.LENGTH_SHORT).show();
                return;
            }
        } catch(Exception e) {}

        if(TextUtils.isEmpty(etName.getText())){ etName.setError("Obligatoire"); return; }
        if(TextUtils.isEmpty(etPrice.getText())){ etPrice.setError("Obligatoire"); return; }
        if(TextUtils.isEmpty(etStart.getText())){ etStart.performClick(); return; }
        if(TextUtils.isEmpty(etEnd.getText())){ etEnd.performClick(); return; }

        JSONArray tagArr=new JSONArray();
        for(Spinner sp: tagSpinners){
            int pos=sp.getSelectedItemPosition();
            if(pos<0) { Toast.makeText(this,"Un tag non sélectionné",Toast.LENGTH_SHORT).show(); return;}
            tagArr.put(tagList.get(pos).id);
        }

        JSONObject body=new JSONObject();
        try {
            body.put("key_point_name", etName.getText().toString().trim());
            body.put("key_point_price", Float.parseFloat(etPrice.getText().toString()));
            body.put("key_point_start_date", etStart.getText().toString());
            body.put("key_point_end_date", etEnd.getText().toString());
            body.put("key_point_cover", base64Cover);
            body.put("key_point_gps_x", Double.parseDouble(etX.getText().toString()));
            body.put("key_point_gps_y", Double.parseDouble(etY.getText().toString()));
            body.put("is_altered_keypoint", 0);
            body.put("city_id", cityList.get(spinnerCity.getSelectedItemPosition()).id);
            body.put("tags", tagArr);
        } catch(JSONException ex){ ex.printStackTrace(); }

        // String url = "http://192.168.0.110/~mathys.raspolini/travist/public/api/updateKeypoint/" + kpId;
        String url = "http://10.0.2.2/~mathys.raspolini/travist/public/api/updateKeypoint/" + kpId;
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, url, body,
                response -> {
                    Toast.makeText(this,"Lieu mis à jour",Toast.LENGTH_SHORT).show();
                    finish();
                },
                error -> Toast.makeText(this,"Erreur mise à jour",Toast.LENGTH_LONG).show()
        ){
            @Override public Map<String,String> getHeaders() throws AuthFailureError {
                Map<String,String> h=new HashMap<>();
                h.put("Content-Type","application/json; charset=UTF-8");
                return h;
            }
        };
        Volley.newRequestQueue(this).add(req);
    }
}
