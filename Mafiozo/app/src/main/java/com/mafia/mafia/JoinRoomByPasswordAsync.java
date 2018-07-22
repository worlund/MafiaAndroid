package com.mafia.mafia;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

/**
 * Created by Robert on 2017-03-10.
 */

public class JoinRoomByPasswordAsync extends AsyncTask<String, Void, String> {

    Context context;
    String roomName;

    public JoinRoomByPasswordAsync(Context context) {
        this.context = context; // = (MenuActivity) context;
    }

    @Override
    protected String doInBackground(String... params) {
        String userName = params[0];
        roomName = params[1];
        String password = params[2];

        URL url = null;
        try {
            String s = Settings.ip+":"+Settings.port;
            System.out.println(s);
            url = new URL("http://" + s + "/joinRoomWPassword"); //matchLogin");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        String response = "Default response value";
        try {
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestProperty("User-Agent", "Mafia");
            con.setRequestProperty("Content-Type", "application/json");
            System.out.println("Joining Room by PW");
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            String string = "";
            try {
                JSONObject obj = new JSONObject();
                obj.put("name", userName);
                obj.put("room", roomName);
                obj.put("pw", password);
                string = obj.toString();
                System.out.println("STRING @JoinRoomBYPWAsync: " + string);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            con.getOutputStream().write(string.getBytes("UTF-8"));

            int code = con.getResponseCode();
            if (code != 200) {
                System.out.println("Response code isn't 200: " + code);
            } else {
                BufferedReader infil = new BufferedReader(new InputStreamReader(con.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line = "";
                while ((line = infil.readLine()) != null) {
                    sb.append(line + "\n");
                }
                System.out.println("REAL MSG = " + sb.toString());
                try {
                    JSONObject obj = new JSONObject(sb.toString());
                    String temp = (String) obj.get("result");
                    System.out.println("Msg  = " + temp);
                    response = temp;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                //System.out.println("MSG: "+ con.getResponseMessage());
                //response = con.getHeaderField("loginSuccess");
                System.out.println("200!! " + response);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    @Override
    protected void onPostExecute(String result) {
        if (result.equals("True")) {
            //change to LobbyVIew
            Intent intent = new Intent(context, LobbyActivity.class);
            intent.putExtra("roomName", roomName);
            intent.putExtra("hasPW", "true");
            context.startActivity(intent);
        }else if(result.equals("False")){
            // Wrong password, give some message with wrong pw or something
        }

    }
}
