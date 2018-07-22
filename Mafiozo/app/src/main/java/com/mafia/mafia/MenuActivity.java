package com.mafia.mafia;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MenuActivity extends AppCompatActivity {
    String roomName = "";
    ListView listView;
    ArrayAdapter<String> roomsAdapter;
    List<String> rooms;
    String password = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        //String message = intent.getStringExtra("EXTRA_MESSAGE");

        // Capture the layout's TextView and set the string as its text
        //TextView textView = (TextView) findViewById(R.id.textView);
        //textView.setText(message);

        initializeListView();

    }

    private void initializeListView() {

        password = "";
        this.rooms = new ArrayList<String>();
        //get listview of rooms
        listView = (ListView) findViewById(R.id.rooms_list);
        //set adapter
        roomsAdapter = new ArrayAdapter<String>(this, R.layout.list_room_item, this.rooms);
        listView.setAdapter(roomsAdapter);

        //start background thread to get rooms
        (new RoomsAsync()).execute();

        //set listener on rooms to join them if clicked
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(password.equals("")) {
                    joinRoom(view);
                } else {
                    joinRoomByPasswordInit(view, password);
                }

            }
        });
    }

    //join a room -> change activity and join the socketCLuBofDoom?
    public void joinRoom(View view) {
        String userName = Settings.getUserName();
        TextView room = (TextView) view;
        this.roomName = room.getText().toString().split(" ")[0];
        (new JoinRoomAsync(this)).execute(userName, this.roomName);
    }

    //create a room
    public void createRoom(View view) {
        Intent intent = new Intent(this, HostActivity.class);
        System.out.println(view);
        startActivity(intent);
    }

    public void showPasswordPrompt() {
        //showPassword Prompt (create new password field and button)
        // get prompts.xml view
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.passowrd_prompt, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                this);

        // set prompts.xml to alertdialog builder
        alertDialogBuilder.setView(promptsView);

        final EditText userInput = (EditText) promptsView
                .findViewById(R.id.password_dialog);

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                // get user input and set it to result
                                // edit text
                                String password = userInput.getText().toString();
                                joinRoomByPassword(password);
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                dialog.cancel();
                            }
                        });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

    public void connectPrompt() {
        //showPassword Prompt (create new password field and button)
        // get prompts.xml view
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.passowrd_prompt, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                this);

        // set prompts.xml to alertdialog builder
        alertDialogBuilder.setView(promptsView);

        final EditText userInput = (EditText) promptsView
                .findViewById(R.id.password_dialog);

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                // get user input and set it to result
                                // edit text
                                String password = userInput.getText().toString();
                                connectRoomByPassword(password);
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                dialog.cancel();
                            }
                        });
        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

    public void joinRoomByPasswordInit(View view, String password) {
        TextView room = (TextView) view;
        this.roomName = room.getText().toString().split(" ")[0];
        joinRoomByPassword(password);
    }

    public void connectRoomByPassword(String password) {
        this.password = password;
        (new ConnectByPasswordAsync()).execute(password);
    }

    public void joinRoomByPassword(String password) {
        String userName = Settings.getUserName(); //getUserName
        (new JoinRoomByPasswordAsync(this)).execute(userName, this.roomName, password);
    }

    public void insertRooms(String[] rooms) {
        //ListView listView = (ListView) findViewById(R.id.rooms_list);

        //Adapter that AUTOMATICALLY fills the viewList with items in the array
        //bind adapter to listView object
        roomsAdapter.clear();
        if(rooms.length > 0) {
            this.rooms = new ArrayList(Arrays.asList(rooms));
            roomsAdapter.addAll(this.rooms);
        }

    }

    public void connectByPassword(View view) {
        connectPrompt();
    }

    public void notFound() {
        TextView errorMsg = (TextView) findViewById(R.id.err_text); //add in xml
        errorMsg.setText("Room not found");
    }

    public void roomFull() {
        TextView errormsg = (TextView) findViewById(R.id.err_text); //add in xml
        errormsg.setText("room is full!");
    }
    public void refreshList(View view) {
        this.password = "";
        (new RoomsAsync()).execute();
    }

    public void changeToLobby(String roomName) {
        Intent intent = new Intent(this, LobbyActivity.class);
        intent.putExtra("roomName", roomName);
        intent.putExtra("hasPW", "true");
        this.startActivity(intent);
    }

    //CLASS FOR GETTING ROOMS FROM SERVER
    private class RoomsAsync extends AsyncTask<String, Void, String[]> {

        @Override
        protected String[] doInBackground(String... params) {

        URL url = null;
        String[] rooms = {};
        try {
            String path = Settings.ip+":"+Settings.port;//"192.168.1.72:8989";
            System.out.println(path);
            url = new URL("http://"+path+"/rooms");
        } catch(MalformedURLException e) {
            e.printStackTrace();
        }

        String testRes = "Default response value";
        try {
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestProperty("User-Agent","Mafia");
            con.setRequestMethod("GET");
            int code = con.getResponseCode();
            if(code != 200){
                System.out.println("Response code isn't 200: " + code);
            }
            else{
                StringBuilder sb = new StringBuilder();
                BufferedReader response = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String line = "";
                while((line=response.readLine()) != null){
                    sb.append(line + "\n");
                }
                System.out.println("REAL MSG = "+sb.toString());
                try{
                    JSONObject obj =new JSONObject(sb.toString());
                    System.out.println("OBJ = " + obj.get("list"));
                    JSONArray jsonArray = (JSONArray)obj.get("list");
                    if (jsonArray != null) {
                        int len = jsonArray.length();
                        rooms = new String[len];
                        for (int i=0;i<len;i++){
                            rooms[i] = (jsonArray.get(i).toString());
                        }
                    }
                    System.out.println("ROOMS");
                }catch(JSONException e){
                    System.out.println("@JSONEXception");
                    e.printStackTrace();
                }
                System.out.println("END OF ELSE");
            }
            System.out.println("END OF TRY");
        } catch(IOException e) {
            System.out.println("@IOException");
            e.printStackTrace();
        }
            System.out.println("READY TO RETURN @doInBackground");
            return rooms;
            //return testRes;
        }

        @Override
        protected void onPostExecute(String[] result) {
           insertRooms(result);
        }
    }

    private class ConnectByPasswordAsync extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            String password = params[0];
            URL url = null;
            String result = "";
            try {
                String path = Settings.ip+":"+Settings.port;//"192.168.1.72:8989";
                System.out.println(path);
                url = new URL("http://"+path+"/joinRoomByPW");
            } catch(MalformedURLException e) {
                e.printStackTrace();
            }

            String testRes = "Default response value";
            try {
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestProperty("User-Agent","Mafia");
                con.setRequestProperty("Content-Type", "application/json");
                con.setRequestMethod("POST");
                con.setDoOutput(true);
                String string = "";
                System.out.println("password = " + password);
                try {
                    JSONObject obj = new JSONObject();
                    obj.put("name", Settings.getUserName());
                    obj.put("pw", password);
                    string = obj.toString();
                    System.out.println("STRING TO JSON: " + string);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                con.getOutputStream().write(string.getBytes("UTF-8"));

                int code = con.getResponseCode();
                if(code != 200){
                    System.out.println("Response code isn't 200: " + code);
                }
                else{
                    //response = con.getHeaderField("rooms");
                    StringBuilder sb = new StringBuilder();
                    BufferedReader response = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    String line = "";
                    while((line=response.readLine()) != null){
                        sb.append(line + "\n");
                    }
                    result = sb.toString();
                }
                System.out.println("END OF TRY");
            } catch(IOException e) {
                System.out.println("@IOException");
                e.printStackTrace();
            }
            System.out.println("READY TO RETURN @doInBackground");
            return result;
            //return testRes;
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                JSONObject obj = new JSONObject(result);
                String res = obj.get("result").toString();

                if(res.equals("NoRoomFound")) {
                    System.out.println("NoRoomFound");
                    //show text for not Found?
                    notFound();
                } else if(res.equals("OneFound")) {
                    System.out.println("OneFound");
                    if(obj.get("innerRes").toString().equals("True")) {
                        System.out.println("Joining");
                        changeToLobby(obj.get("room").toString());
                        /*
                        String room = obj.get("room").toString();
                        String[] rooms = new String[1];
                        rooms[0] = room;
                        insertRooms(rooms);
                        */
                    } else {
                        System.out.println("RoomFull?");
                        roomFull();
                    }

                }else if(res.equals("ManyRoomsFound")) {
                    System.out.println("ManyRoomsFound");
                    String[] rooms = {};
                    JSONArray jsonArray = (JSONArray)obj.get("rooms");
                    if (jsonArray != null) {
                        int len = jsonArray.length();
                        rooms = new String[len];
                        for (int i=0;i<len;i++){
                            rooms[i] = (jsonArray.get(i).toString());
                        }
                    }
                    insertRooms(rooms);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
