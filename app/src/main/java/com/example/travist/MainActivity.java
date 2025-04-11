package com.example.travist;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    RequestQueue rq;
    Button loginBtn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rq= Volley.newRequestQueue(this);

        loginBtn = findViewById(R.id.login);
        loginBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                doLogIn();
            }
        });
    }

    public void doLogIn(){
        EditText etLogin=findViewById(R.id.etLogin);
        EditText etPass=findViewById(R.id.etPassword);
        String mail=etLogin.getText().toString();
        String pw=etPass.getText().toString();
        if(pw.isEmpty() || mail.isEmpty()){
            Toast.makeText(this,"Mauvaise saisie",Toast.LENGTH_LONG).show();
            return;
        }
        // String url="http://192.168.0.110/~mathys.raspolini/travist/public/api/login";
        String url="http://10.0.2.2/www/PPE_Travist/travist/public/api/login";
        StringRequest req = new StringRequest(Request.Method.POST,url,this::processLoginRequest,this::handleErrors){
            @Nullable
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                HashMap hm=new HashMap();
                hm.put("user_email",mail);
                hm.put("user_password",pw);
                return hm;
            }
        };
        rq.add(req);
    }

    public void processLoginRequest(String response){
        try {
            JSONObject jo = new JSONObject(response);
            JSONObject joData=jo.getJSONObject("data");
            String token=joData.getString("token");
            Intent i=new Intent(this, Profile.class);
            i.putExtra("token",token);
            startActivity(i);
        }
        catch (JSONException x) {
            Toast.makeText(this,"JSON PARSE ERROR",Toast.LENGTH_LONG);
            Log.e("HELLOJWT",response);
        }

    }

    public void handleErrors(Throwable t){
        Toast.makeText(this,"SERVERSIDE PROBLEM",Toast.LENGTH_LONG);
        Log.e("HELLOJWT","SERVERSIDE BUG",t);
    }
}