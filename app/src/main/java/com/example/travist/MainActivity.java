package com.example.travist;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

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
    // Initialisation des variables
    private RequestQueue rq;
    Button loginBtn;

    EditText etLogin;
    EditText etPass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialisation de Volley
        rq = Volley.newRequestQueue(this);

        // Attribution et appel du bouton de login (listener)
        loginBtn = findViewById(R.id.login);
        loginBtn.setOnClickListener(view -> {
            doLogIn();
        });
    }

    // Méthode WebService pour effectuer le login de l'utilisateur
    private void doLogIn() {
        // Attribution des valeurs aux EditTexts et récupération / attribution des valeurs inputs dans des variables
        etLogin = findViewById(R.id.etLogin);
        etPass = findViewById(R.id.etPassword);
        String mail = etLogin.getText().toString();
        String pw = etPass.getText().toString();

        // Si une des valeurs est vide, on interrompt le code, et on renvoie un message d'erreur
        if(pw.isEmpty() || mail.isEmpty()) {
            Toast.makeText(this,"Mauvaise saisie",Toast.LENGTH_LONG).show();
            return;
        }

        // Initialisation de l'URL pour le login en WebService

        // String url = "http://192.168.0.110/~mathys.raspolini/travist/public/api/login";
        String url = "http://10.0.2.2/~mathys.raspolini/travist/public/api/login";

        // Vérification du compte admin pour l'interface administrateur sinon user normal (simplifiée pour le jeu d'essai)
        if (mail.equals("admin@gmail.com")) {

            // Initialisation de la requête
            StringRequest req = new StringRequest(Request.Method.POST, url, this::processLoginRequestAdmin,this::handleErrors) {

                // Initialisation du HashMap pour la comparaison des données
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    HashMap hm = new HashMap();
                    hm.put("user_email", mail);
                    hm.put("user_password", pw);
                    return hm;
                }
            };
            rq.add(req); // Ajout de la requête à la RequestQueue
        } else {

            // Même principe
            StringRequest req = new StringRequest(Request.Method.POST, url, this::processLoginRequestUser,this::handleErrors) {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    HashMap hm = new HashMap();
                    hm.put("user_email",mail);
                    hm.put("user_password",pw);
                    return hm;
                }
            };
            rq.add(req);
        }
    }

    // Méthode WebService pour procéder à la requête de login en user
    private void processLoginRequestUser(String response) {
        try {
            // On initialise le JSONObject et sa data, et on y récupère le token
            JSONObject jo = new JSONObject(response);
            JSONObject joData = jo.getJSONObject("data");
            String token = joData.getString("token");

            // On définit le token utilisateur dans le singleton dédié à la session de l'utilisateur
            UserSession.setToken(token);

            // On initialise l'intent et on y passe le token, puis on passe à l'autre activité
            Intent i = new Intent(this, Profile.class);
            startActivity(i);
        }
        catch (JSONException x) {
            // Sinon on interrompt le code et on renvoie une erreur de parsing
            Toast.makeText(this,"JSON PARSE ERROR", Toast.LENGTH_LONG);
            Log.e("HELLOJWT",response);
        }
    }

    // Même principe pour le login administrateur
    private void processLoginRequestAdmin(String response) {
        try {
            JSONObject jo = new JSONObject(response);
            JSONObject joData = jo.getJSONObject("data");
            String token = joData.getString("token");

            UserSession.setToken(token);

            Intent i = new Intent(this, AdminPanelActivity.class);
            startActivity(i);
        }
        catch (JSONException x) {
            Toast.makeText(this,"JSON PARSE ERROR",Toast.LENGTH_LONG);
            Log.e("HELLOJWT",response);
        }
    }

    // Méthode destinée à la gestion des erreurs
    public void handleErrors(Throwable t){
        Toast.makeText(this,"SERVERSIDE PROBLEM",Toast.LENGTH_LONG);
        Log.e("HELLOJWT","SERVERSIDE BUG",t);
    }
}