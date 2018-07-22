package com.mafia.mafia;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

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
import java.util.ArrayList;

public class HostActivity extends AppCompatActivity {

    private String roomName = "";
    private String password = "";
    private String cap = "";
    private boolean hidden = false;
    private int upperCap = 15;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host);


        initializeDropDown();
    }

    public String getRoomName() {
        return roomName;
    }

    public String getRoomPassword() {
        return password;
    }

    public String getRoomCap() {
        return cap;
    }

    public boolean getHidden() { return hidden; }

    private void initializeDropDown() {
        Spinner spinner = (Spinner) findViewById(R.id.playerCap);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,
                getSelectionAmount(upperCap));

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    public String[] getSelectionAmount(int amount) {
        String[] res = new String[amount-1];
        res[0] = "Unlimited cap";
        for (int i = 1; i < res.length; i++) {
            res[i] = Integer.toString(i+2);
        }
        return res;
    }

    public void joinRoom() {
        String userName = Settings.getUserName();
        (new JoinRoomAsync(this)).execute(userName, getRoomName());
    }

    public void joinRoomByPassword() {
        String userName = Settings.getUserName();
        (new JoinRoomByPasswordAsync(this)).execute(userName, getRoomName(), getRoomPassword());
    }

    //addRoom to server
    public void addRoom(View view) {
        EditText roomName = (EditText) findViewById(R.id.roomName);
        this.roomName = String.valueOf(roomName.getText());
        TextView roomError = (TextView) findViewById(R.id.room_error);
        StringValidator validRoom = new StringValidator();
        boolean valid = true;

        if(validRoom.validateRoomName(this.roomName)) {
            EditText roomPassword = (EditText) findViewById(R.id.roomPassword);
            this.password = String.valueOf(roomPassword.getText());

            Spinner playerCap = (Spinner) findViewById(R.id.playerCap);
            this.cap = playerCap.getSelectedItem().toString();
            if(this.cap.equals("Unlimited cap")) {this.cap = "0";}

            CheckBox privateRoom = (CheckBox) findViewById(R.id.private_room);
            this.hidden = privateRoom.isChecked();

            if(hidden) {
                System.out.println("Room is private");
                if(this.password.equals("")){
                    roomError.setText("Requires Password in private rooms");
                    valid = false;
                }
            } else {
                System.out.println("Room is NOT private");
            }

            if(valid) {
                (new CreateRoomSync()).execute();
            }
        } else {
            roomError.setText("Invalid room name, Only alphabetic characters and numbers allowed, no whitespace Kappa?");
        }

    }

    private class CreateRoomSync extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {

            URL url = null;
            try {
                String s = Settings.ip+":"+Settings.port;
                System.out.println(s);
                url = new URL("http://" + s + "/createRoom"); //matchLogin");
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            String response = "Default response value";
            try {
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestProperty("User-Agent", "Mafia");
                con.setRequestProperty("Content-Type", "application/json");
                System.out.println("Creating Room");
                con.setRequestMethod("POST");
                con.setDoOutput(true);
                String string = "";
                try {
                    JSONObject obj = new JSONObject();
                    obj.put("room", getRoomName());
                    obj.put("pw", getRoomPassword());
                    obj.put("cap", getRoomCap());
                    obj.put("private", getHidden());
                    string = obj.toString();
                    System.out.println("STRING TO JSON: " + string);
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
                        String temp = (String) obj.get("success");
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
            if(getRoomPassword().equals("")){ // Make sure it checks all possibilities
                joinRoom();
            }
            else{
                joinRoomByPassword();
            }
        }
    }

}
