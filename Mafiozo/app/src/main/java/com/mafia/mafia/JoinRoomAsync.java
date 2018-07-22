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
public class JoinRoomAsync extends AsyncTask<String, Void, String> {

    //MenuActivity menu;
    Context context;
    String roomName;

    public JoinRoomAsync(Context context) {
        this.context = context; // = (MenuActivity) context;
    }

    @Override
    protected String doInBackground(String... params) {

        String userName = params[0];
        roomName = params[1];

        URL url = null;
        try {
            String s = Settings.ip+":"+Settings.port;
            System.out.println(s);
            url = new URL("http://" + s + "/joinRoom"); //matchLogin");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        String response = "Default response value";
        try {
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestProperty("User-Agent", "Mafia");
            con.setRequestProperty("Content-Type", "application/json");
            System.out.println("Joining room (without pw, might give prompt)");
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            String string = "";
            try {
                JSONObject obj = new JSONObject();
                obj.put("name", userName);
                obj.put("room", roomName);
                string = obj.toString();
                System.out.println("STRING @JoinRoomAsync: " + string);
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
        System.out.println("returning response");
        return response;
    }

    @Override
    protected void onPostExecute(String result) {
        System.out.println("@joinRoomAsync");
        if (result.equals("Success")) {
            //change to LobbyVIew
            System.out.println("HALLO");
            Intent intent = new Intent(context, LobbyActivity.class);
            System.out.println("EXTRAING");
            intent.putExtra("roomName", roomName);
            System.out.println("What");
            intent.putExtra("hasPW", "false");
            System.out.println("time to start");
            context.startActivity(intent);
        } else if(result.equals("RequiresPW")){ //Room has password
            System.out.println("@joinRoomAsync RequiresPW");
            ((MenuActivity) context).showPasswordPrompt();
        } else if(result.equals("RoomFull")){
            // Cant connect room full, msg or something
            System.out.println("roomFull");
        }

    }
}
