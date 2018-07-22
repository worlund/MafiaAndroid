package com.mafia.mafia;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;




public class LoginAsync extends AsyncTask<String, Void, String> {

    Context context;
    TextView txt_Error;
    String userName;

    public LoginAsync(Context context, TextView txt_view) {
        this.context = context;
        this.txt_Error = txt_view;
    }

    @Override
    protected void onPreExecute() {
        //super.onPreExecute();
       // mProgress.setProgress(0);
       // mProgress.setVisibility(View.Visible);
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        //super.onProgressUpdate(values);
       // mProgress.setProgress(values[0]);
    }

    @Override
    protected String doInBackground(String... params) {

        // All your networking logic
        // should be here

        //Test without server connection
        String test = params[2];
        System.out.println(test);
        /*
        if(test.equals("Robert")) {
            System.out.println("Match");
            return "True";
        } else {
            System.out.println("NoMatch");
            return "False";
        }
        */
        URL url = null;
        try {
            String s = params[0] + ":" + params[1];
            System.out.println(s);
            url = new URL("http://"+s+"/matchLogin"); //matchLogin");
        } catch(MalformedURLException e) {
            e.printStackTrace();
        }

        userName = params[2];
        String pw = params[3];
        String response = "Default response value";
        try {
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestProperty("User-Agent","Mafia");
            con.setRequestProperty("Content-Type", "application/json");
            System.out.println("Matching login");
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            String string = "";
            try{
                JSONObject obj = new JSONObject();
                obj.put("name", userName);
                obj.put("pw", pw);
                string = obj.toString();
                System.out.println("STRING TO JSON: " + string);
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
                    String temp = (String) obj.get("loginSuccess");
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
        System.out.println("@LoginAsync @onPost = "+response);
        if(response.equals("True")) {
            System.out.println("Response = True");
            Settings.setUserName(userName);
            Intent i = new Intent(context, MenuActivity.class);
            //i.putExtra("EXTRA_MESSAGE", "You made it!");
            context.startActivity(i);
        }else if(response.equals("False")) {
            System.out.println(" resp = False. Incorrect Username or Password");
            txt_Error.setText("Sorry!! Incorrect Username or Password");
        }else if(response.equals("LoggedInElsewhere")){
            System.out.println("User logged in elsewhere");
            txt_Error.setText("This account is currently logged in elsewhere");
        }

        /*
        Intent intent = new Intent(this, MenuActivity.class);
        EditText editText = (EditText) this.view.findViewById(R.id.editText);
        String message = editText.getText().toString();
        intent.putExtra("EXTRA_MESSAGE", message);
        startActivity(intent);
        */
    }


}