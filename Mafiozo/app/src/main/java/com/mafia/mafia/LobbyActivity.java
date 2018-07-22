package com.mafia.mafia;

import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public class LobbyActivity extends AppCompatActivity {

    private String roomName;
    private Client client;

    private String playersMax;

    private ArrayList<String> messages;
    private ArrayAdapter<String> messageAdapter;
    private ArrayList<String> onlinePlayers;
    private ArrayAdapter<String> playersAdapter;
    private ArrayList<String> readyArray;

    private boolean buttonReadyMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println("@LOBBY");
        setContentView(R.layout.activity_lobby);
        Intent intent = getIntent();
        roomName = intent.getStringExtra("roomName");
        String hasPW = intent.getStringExtra("hasPW");
        TextView lobby = (TextView) findViewById(R.id.lobby);
        lobby.setText(roomName);
        TextView pw = (TextView) findViewById(R.id.pw);
        pw.setText(hasPW);

        messages = new ArrayList<String>();
        onlinePlayers = new ArrayList<String>();
        readyArray = new ArrayList<String>();

        ListView listView = (ListView) findViewById(R.id.game_msg);
        //Adapter that AUTOMATICALLY fills the viewList with items in the array
        messageAdapter = new ArrayAdapter<String>(this, R.layout.message_item, messages);
        //bind adapter to listView object
        listView.setAdapter(messageAdapter);
        messageAdapter.add("Chat:");

        ListView listPlayers = (ListView) findViewById(R.id.list_players);

        playersAdapter = new ArrayAdapter<String>(this, R.layout.message_item, onlinePlayers);

        /*{
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = convertView;
                if(view == null){
                    LayoutInflater vi = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = vi.inflate(R.layout.message_item, null);
                }
                TextView text = (TextView) super.getView(position, convertView, parent);
                String name = text.getText().toString();
                if(readyArray.contains(name)) {
                    text.setBackgroundColor(Color.GREEN);
                }
                System.out.println(view);
                return view;
            }
        };*/

        listPlayers.setAdapter(playersAdapter);
        // ROOM NAME
        client = new Client(Settings.ip, Settings.port, roomName, Settings.getUserName(), this);
        // Connect socket
        // Get data of how many users, and which users

        final Button readyBtn = (Button) findViewById(R.id.voteToStart);
        readyBtn.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                if(buttonReadyMode) {
                    buttonReadyMode = false;
                    readyBtn.setText("Unready");
                    client.ready();
                } else {
                    buttonReadyMode = true;
                    readyBtn.setText("Ready");
                    client.unready();
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Server: Check if room is empty after deletion, delete room
        System.out.println("ONPAUSE");
        // Leave
        //  System.out.println("Not starting, should remove socket connection to room");
        client.leaveSocketAndRoom(); // Remove userName socket aswell???
        //client.removeFromRoom();
    }

    public void sendMessage(View view){
        EditText editText = (EditText) findViewById(R.id.message);
        String msg = editText.getText().toString();
        editText.setText("");
        System.out.println("TIME TO SEND A MESSAGE! " + msg);
        client.sendMessage(msg);
    }

    public void addMessage(String user, String message){
        messageAdapter.add(user + ": " + message);
    }

    public void joinUser(String user){
        System.out.println("@JoinUser lobbyAct user = " + user);
        playersAdapter.add(user);
        messageAdapter.add(user + " joined the lobby");
    }

    public void leaveUser(String user){
        playersAdapter.remove(user);
        messageAdapter.add(user + " left the lobby");
    }

    public void updateFields(String connPlayers, String readyPlayers, String readyUser, boolean ready){
        String cToChange;
        String rToChange;

        if(this.playersMax.equals("?")) {
            cToChange = connPlayers;
            rToChange = readyPlayers;
        } else {
            cToChange = connPlayers +"/"+this.playersMax;
            rToChange = readyPlayers +"/"+this.playersMax;
        }
        TextView c = (TextView) findViewById(R.id.connP);
        c.setText(cToChange);
        TextView r = (TextView) findViewById(R.id.pReady);
        r.setText(rToChange);


        if(!readyPlayers.equals("")) {
            if(ready){
                readyArray.add(readyUser);
            }else {
                readyArray.remove(readyUser);
            }
            setPlayersReadyColor();
        }
    }

    public void setPlayersReadyColor() {
        ListView playersList = (ListView) findViewById(R.id.list_players);

        int count = playersList.getChildCount();
        System.out.println("#items = "+count);
        for(int i = 0; i < count; i++) {
            TextView player = (TextView) playersList.getChildAt(i);
            System.out.println("checkViewChild");
            if(player != null) {
                System.out.println("viewPlayerText = "+player.getText().toString());
                if(readyArray.contains(player.getText().toString())){
                    System.out.println("found match in ready array");
                    player.setBackgroundColor(Color.GREEN);
                }else {
                    player.setBackgroundColor(0x00000000);
                }
            }

        }
    }

    public void setConnInfo(String connectedPlayers, String readyPlayers, String playersMax, String cPlayersNum, String rPlayersNum){
        System.out.println("@SETCONNINFO " + connectedPlayers);
        //System.out.println(readyPlayers);
        //System.out.println(playersMax);
        String[] split = connectedPlayers.split(",");
        for(int i = 0; i < split.length-1; i++){
            playersAdapter.add(split[i]);
            System.out.println("ADDING TO PLAYERLIST: " + split[i]);
        }
        String[] split2 = readyPlayers.split(",");
        for(int i = 0; i < split2.length-1; i++){
            readyArray.add(split2[i]);
        }
        this.playersMax = playersMax;
        updateFields(cPlayersNum, rPlayersNum, "", true);
        setPlayersReadyColor();
        // Set arrays to correct fields
        // Behöver veta hur json->string ser ut
    }



    public void startGame(){
        System.out.println("STARTING GAME");
        Intent i = new Intent(this, GameActivity.class);
        i.putExtra("ROOM_NAME", roomName);
        i.putStringArrayListExtra("Players", onlinePlayers);
        this.startActivity(i);
    }
}
