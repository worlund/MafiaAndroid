package com.mafia.mafia;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


public class MainActivity extends AppCompatActivity {

    TextView txt_Error;
    LoginValidator validator;

    //initialize main login screen
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        System.out.println("ON CREATE LALLALALAL");
        removeUserIfValid();
        //Settings.setUserName(""); // REMOVE

        txt_Error =(TextView)findViewById(R.id.errorText);
        validator = new LoginValidator();
    }

    //Login with given username and password
    public void login(View view) {
        EditText editText = (EditText) findViewById(R.id.editText);
        EditText passwordField = (EditText) findViewById(R.id.password_login);

        String username = editText.getText().toString();
        String password = passwordField.getText().toString();

        boolean userSuccess = validator.validate(username);
        boolean pwSuccess = validator.validatePassword(password);
        if(userSuccess && pwSuccess) {
            LoginAsync login = new LoginAsync(this, txt_Error);
            login.execute(Settings.ip, Settings.port, username, password);
        } else {
            txt_Error.setText("Invalid format of username: a-zA-Z0-9\nOr Password: min 8 chars 1 Alph and 1 number");
        }

    }

    public void ipLoginListener(View v) {
        EditText ip_field = (EditText) findViewById(R.id.ip_field);
        EditText port_field = (EditText) findViewById(R.id.port_field);
        final String ipString = ip_field.getText().toString();
        final String portString = port_field.getText().toString();
        txt_Error.setText("IP:PORT = "+ipString+":"+portString);
        Settings.setConnectionInfo(ipString,portString);
    }

    //Toggle register fields
    public void toggleRegister(View view) {
        toggleFields(View.VISIBLE, View.INVISIBLE);
    }

    //Handle registration of new user
    public void register(View view) {
        //ASYNC CALL TO SERVER WITH NAME AND PW
        EditText editText = (EditText) findViewById(R.id.registerField);
        String username = editText.getText().toString();

        EditText passwordField = (EditText) findViewById(R.id.password_register);
        String password = passwordField.getText().toString();

        boolean userSuccess = validator.validate(username);
        boolean pwSuccess = validator.validatePassword(password);
        if(userSuccess && pwSuccess) {
            RegisterAsync reg = new RegisterAsync(this, txt_Error);
            reg.execute(Settings.ip, Settings.port, username, password);
        } else {
            txt_Error.setText("Invalid format of username: a-zA-Z0-9\nOr Password: min 8 chars 1 Alph and 1 number");
        }

    }

    //Go back to default login screen
    public void goBack(View view) {
        toggleFields(View.INVISIBLE, View.VISIBLE);
    }

    //toggle between Login and Registration field
    private void toggleFields(int visibility, int visibility2) {
        txt_Error.setText("");
        EditText registerField = (EditText) findViewById(R.id.registerField);
        registerField.setVisibility(visibility);
        Button registerBtn = (Button) findViewById(R.id.registerBtn);
        registerBtn.setVisibility(visibility);
        Button backBtn = (Button) findViewById(R.id.back);
        backBtn.setVisibility(visibility);
        EditText pwRegisterField = (EditText) findViewById(R.id.password_register);
        pwRegisterField.setVisibility(visibility);

        EditText enterName = (EditText) findViewById(R.id.editText);
        enterName.setVisibility(visibility2);
        Button enterBtn = (Button) findViewById(R.id.button);
        enterBtn.setVisibility(visibility2);
        Button register = (Button) findViewById(R.id.register);
        register.setVisibility(visibility2);
        EditText passwordField = (EditText) findViewById(R.id.password_login);
        passwordField.setVisibility(visibility2);

    }

    public void removeUserIfValid(){
        String name = Settings.getUserName();
        if(name != null){
            new leaveAsync().execute(name);
        }
    }
    private class leaveAsync extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            URL url = null;
            try {
                String s = Settings.ip + ":" + Settings.port;
                System.out.println(s);
                url = new URL("http://"+s+"/removeUser");
            } catch(MalformedURLException e) {
                e.printStackTrace();
            }

            String name = params[0];
            String response = "Default response value";
            try {
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestProperty("User-Agent","Mafia");
                con.setRequestProperty("Content-Type", "application/json");
                System.out.println("Removing user from online users");
                con.setRequestMethod("POST");
                con.setDoOutput(true);
                String string = "";
                try{
                    JSONObject obj = new JSONObject();
                    obj.put("name", name);
                    string = obj.toString();
                }catch(JSONException e){
                    e.printStackTrace();
                }
                con.getOutputStream().write(string.getBytes("UTF-8"));

                int code = con.getResponseCode();
                if(code != 200){
                    System.out.println("Response code isn't 200: " + code);
                }
                else{
                    BufferedReader infil = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line = "";
                    while((line=infil.readLine()) != null){
                        sb.append(line + "\n");
                    }
                    System.out.println("REAL MSG = "+sb.toString());
                    try{
                        JSONObject obj =new JSONObject(sb.toString());
                        String temp = (String) obj.get("success");
                        System.out.println("Msg  = " + temp);
                        response = temp;
                    }catch(JSONException e){
                        e.printStackTrace();
                    }
                    //System.out.println("MSG: "+ con.getResponseMessage());
                    //response = con.getHeaderField("loginSuccess");
                    System.out.println("200!! " + response);
                }

            } catch(IOException e) {
                e.printStackTrace();
            }
            return response;
        }
        @Override
        protected void onPostExecute(String response) {
            System.out.println("@RemoveUser @onPost = "+response);
            if(response.equals("True")) {
                System.out.println("User Succesfully removed");
            }else{
                System.out.println("USER NOT REMOVED?");
            }
        }
    }

}




