package com.mafia.mafia;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class GameActivity extends AppCompatActivity {

    private boolean isDay;
    private int dayNumber;

    private CountDownTimer timer;
    private String roleName;
    private TextView voteDay;
    private TextView voteNight;
    private TextView timerField;
    private TextView timeLeft;
    private TextView DayNight;
    private Button roleButton;
    private Button accuseButton;
    private ArrayList<String> onlinePlayers;
    private ArrayAdapter<String> playersAdapter;
    private ArrayList<String> alivePlayers;
    private int rVoteSkip;
    private int rVoteProlong;
    private boolean voteReadyMode;
    private boolean prolongDayMode;
    private ArrayList<String> messages;
    private ArrayAdapter<String> messageAdapter;
    private GameClient client;
    private String roomName;
    private boolean isSkip;
    private int rid;
    private AlertDialog roleInfoDialog;
    private boolean roleActionDone;
    private String userName;
    private String lastMedicPick;
    private int accusesLeft;
    private ListView playerList;
    private String playerListSelected;
    private boolean isDead;
    private boolean hasStartedVote;
    private boolean timeOutDuringVote;
    private String instructText;

    private AlertDialog alertDialog;
    private String currentAccused;
    private String currentAccuser;
    private String currentConfirmer;
    private ArrayList<String> accuseList;
    private ArrayList<String> accusersList;


    private boolean buttonHideMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        voteDay = (TextView)findViewById(R.id.voteDay);
        voteNight = (TextView)findViewById(R.id.voteNight);
        timerField = (TextView)findViewById(R.id.timer);
        timeLeft = (TextView)findViewById(R.id.timeLeftText);
        DayNight = (TextView)findViewById(R.id.DayNight);
        roleButton = (Button)findViewById(R.id.roleButton);
        accuseButton = (Button)findViewById(R.id.accuse);

        defaultInit();
        client = new GameClient(Settings.ip, Settings.port, roomName, Settings.getUserName(), this); // Not guaranted its done
    }

    public void defaultInit() {
        userName = Settings.getUserName();
        lastMedicPick = "";
        isSkip = false;
        accusesLeft = 2;
        accuseButton.setText(getString(R.string.accuse) + "(" + (2-accusesLeft)+"/2)");
        playerListSelected = "";
        isDead = false;

        Intent intent = getIntent();
        roomName = intent.getStringExtra("ROOM_NAME");

        onlinePlayers = intent.getStringArrayListExtra("Players");

        ListView listPlayers = (ListView) findViewById(R.id.list_players);

        playersAdapter = new ArrayAdapter<String>(this, R.layout.message_item, onlinePlayers);
        listPlayers.setAdapter(playersAdapter);

        rVoteSkip = 0;
        rVoteProlong = 0;
        voteReadyMode = true;
        prolongDayMode = true;

        messageAdapterInit();
        HideShowButtonInit();
        startButtonListeners();
        (new roleInfoAsync()).execute();
    }

    public void initStartInfo(boolean isDay, int dayNumber, ArrayList<String> alivePlayers){
        this.alivePlayers = alivePlayers;
        changePlayerColorState();
        this.isDay = isDay;
        this.dayNumber = dayNumber;
    }

    public void initPlayerList(View promptsView, final AlertDialog alertDialog, final int num){
        playerList = (ListView) promptsView.findViewById(R.id.listPlayersRole);
        playerList.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        playerList.setSelector(android.R.color.holo_blue_light);
        playerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                playerListSelected = parent.getItemAtPosition(position).toString();
                System.out.println("USER CHOSEN: " + playerListSelected);
                switch(num){
                    case 1:
                        client.sendRoleAction(rid, playerListSelected);
                        lastMedicPick = playerListSelected; // Gör det för alla roller men används endast av medic
                        roleButton.setText(getString(R.string.roleButtonDone));
                        alertDialog.cancel();
                        roleActionDone = true;
                        break;
                    case 2:
                        client.accuse(playerListSelected);
                        alertDialog.cancel();
                        roleActionDone = true;
                        break;
                    case 4:
                        client.voteToKill(playerListSelected);
                        alertDialog.cancel();
                        roleActionDone = true;
                        break;
                }
            }
        });
    }

    public void startButtonListeners() {
        //VOTE TO NIGHT BUTTON
        final Button voteBtn = (Button) findViewById(R.id.voteNight);
        voteButtonChange(Integer.toString(rVoteSkip));
        voteBtn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isDead){
                    if(voteReadyMode) {
                        voteReadyMode = false;
                        client.emitter("readyForSkip");
                    } else {
                        voteReadyMode = true;
                        client.emitter("unreadyForSkip");
                    }
                }
            }
        });

        //TOGGLE CHAT BUTTON
        final Button toggleChat = (Button) findViewById(R.id.toggleChat);
        toggleChat.setOnClickListener(new Button.OnClickListener() {

            @Override
            public void onClick(View v) {
                if(toggleChat.getText().toString().equals(getString(R.string.hideChat))) {
                    toggleChat.setText(getString(R.string.showChat));
                    toggleChat(View.INVISIBLE);
                } else {
                    toggleChat.setText(getString(R.string.hideChat));
                    toggleChat(View.VISIBLE);
                }
            }
        });

        //PROLONG DAY VOTE BUTTON

        final Button prolongBtn = (Button) findViewById(R.id.voteDay);
        prolongedButtonChange(Integer.toString(rVoteProlong));
        prolongBtn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isDead) {
                    if (prolongDayMode) {
                        prolongDayMode = false;
                        System.out.println("@ButtonListener Prolong, prolong day mode = false");
                        client.emitter("prolongDay");
                    } else {
                        prolongDayMode = true;
                        client.emitter("unprolongDay");
                    }
                }
            }
        });
    } // End start button listeners

    public void voteButtonChange(String amount) {
        System.out.println("@voteButtonChange: amount = "+ amount);
        Button voteBtn = (Button) findViewById(R.id.voteNight);
        StringBuilder sb = new StringBuilder();
        String voteText = "";
        if(voteReadyMode){
            voteText = getString(R.string.vote_to_night);
        }else {
            voteText = getString(R.string.unvote_to_night);
        }
        sb.append(voteText);
        int num = 0;
        if(alivePlayers == null){
            num = onlinePlayers.size();
        }else{
            num = alivePlayers.size();
        }
        sb.append(" "+amount+"/"+num);
        final String buttonText = sb.toString();
        voteBtn.setText(buttonText);
    }

    public void prolongedButtonChange(String amount) {
        System.out.println("@prolongedButtonChange: amount = "+ amount);
        Button voteBtn = (Button) findViewById(R.id.voteDay);
        StringBuilder sb = new StringBuilder();
        String voteText = "";
        if(prolongDayMode){
            voteText = getString(R.string.vote_more_day);
        }else {
            voteText = getString(R.string.unvote_more_day);
        }
        sb.append(voteText);
        int num = 0;
        if(alivePlayers == null){
            num = onlinePlayers.size();
        }else{
            num = alivePlayers.size();
        }
        sb.append(" "+amount+"/"+num);
        final String buttonText = sb.toString();
        voteBtn.setText(buttonText);
    }

    public void prolongDayTime(){
        System.out.println("Time to prolong daytime");
        prolongDayMode = true;
        prolongedButtonChange("0");
        timer.cancel();
        startTimer((Integer.parseInt(timerField.getText().toString())*1000)+120000);
    }

    public void skipDay(){
        System.out.println("Time to skip day");
        timer.cancel();
        startNight();
    }

    public void changePlayerColorState() {
        ListView playersList = (ListView) findViewById(R.id.list_players);

        int count = playersList.getChildCount();
        for(int i = 0; i < count; i++) {
            TextView player = (TextView) playersList.getChildAt(i);
            if(player != null) {
                if(alivePlayers.contains(player.getText().toString())){
                    player.setBackgroundColor(Color.GREEN);
                } else{
                    player.setBackgroundColor(Color.RED);
                }
            }
        }
    }

    public void HideShowButtonInit() {
        final Button readyBtn = (Button) findViewById(R.id.hideShow);
        readyBtn.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                if(buttonHideMode) {
                    buttonHideMode = false;
                    readyBtn.setText(getString(R.string.show));
                    toggleRoleInfo(View.INVISIBLE);
                } else {
                    buttonHideMode = true;
                    readyBtn.setText(getString(R.string.hide));
                    toggleRoleInfo(View.VISIBLE);
                }
            }
        });
    }

    public void toggleChat(int visibility) {
        ListView messages = (ListView) findViewById(R.id.game_msg);
        EditText entermsg = (EditText) findViewById(R.id.enter_msg);
        Button sendmsg = (Button) findViewById(R.id.send_msg);

        messages.setVisibility(visibility);
        entermsg.setVisibility(visibility);
        sendmsg.setVisibility(visibility);
    }

    public void toggleRoleInfo(int visibility) {
        TextView roleName = (TextView) findViewById(R.id.role);
        TextView instructionsText = (TextView) findViewById(R.id.instructionsText);
        TextView instruct = (TextView) findViewById(R.id.instruct);
        if(visibility == View.INVISIBLE) {
            roleName.setText(getString(R.string.hidden));
        } else {
            roleName.setText(this.roleName);
        }

        instructionsText.setVisibility(visibility);
        instruct.setVisibility(visibility);
        roleButton.setVisibility(visibility);

    }

    public void messageAdapterInit() {
        messages = new ArrayList<String>();

        ListView listView = (ListView) findViewById(R.id.game_msg);
        //Adapter that AUTOMATICALLY fills the viewList with items in the array
        messageAdapter = new ArrayAdapter<String>(this, R.layout.message_item, messages);
        //bind adapter to listView object
        listView.setAdapter(messageAdapter);
        messageAdapter.add("Chat:");
    }

    public void startDay(){
        System.out.println("SETTING DAY");
        dayNumber++;
        voteDay.setVisibility(View.VISIBLE);
        voteNight.setVisibility(View.VISIBLE);
        timerField.setVisibility(View.VISIBLE);
        timeLeft.setVisibility(View.VISIBLE);
        accuseButton.setVisibility(View.VISIBLE);
        accuseButton.setText(getString(R.string.accuse) + " (" + (2-accusesLeft)+"/2)");
        isDay = true;
        roleButton.setText(getString(R.string.roleButton)); // CODE FOR CHAGE BACk
        final String dayText = "Day " + dayNumber;
        DayNight.setText(dayText);
        voteReadyMode = true;
        prolongDayMode = true;
        voteButtonChange("0");
        prolongedButtonChange("0");
        timeOutDuringVote = false;
        hasStartedVote = false;

        accuseList = new ArrayList<String>();
        accusersList = new ArrayList<String>();
        currentAccused = "";
        currentAccuser = "";
        currentConfirmer = "";
        accusesLeft = 2;

        startTimer(180000);
    }

    public void startNightString(String reasonForNight){
        messageAdapter.add(reasonForNight);
        startNight();
    }

    public void startNight(){
        if(alertDialog != null) {
            alertDialog.cancel();
        }
        System.out.println("SETTING NIGHT");
        voteDay.setVisibility(View.INVISIBLE);
        voteNight.setVisibility(View.INVISIBLE);
        timerField.setVisibility(View.INVISIBLE);
        timeLeft.setVisibility(View.INVISIBLE);
        accuseButton.setVisibility(View.INVISIBLE);
        isDay = false;
        final String dayText = "Night " + dayNumber;
        roleActionDone = false;
        DayNight.setText(dayText);
        roleActionDone = false;
        timer.cancel();
    }

    public void startGame(){
        if(!isDay){ // Default is night
            startNight();
        }else{
            startDay();
        }
    }

    public void startTimer(long time){
        timer = new CountDownTimer(time, 1000) {

            public void onTick(long millisUntilFinished) {
                timerField.setText( (millisUntilFinished / 1000)+"");
                //here you can have your logic to set text to edittext
            }

            public void onFinish() {
                // Toggle Night
                if(accusesLeft == 2){
                    startNightString("Time is out, going into night");
                }else if(hasStartedVote){
                    timeOutDuringVote = true;
                }else{
                    timeOutDuringVote = true;
                    startVote();
                }
            }
        }.start();
    }

    // Checks if role functionality is available at currentTime
    public void roleFunctionality(View view){

        System.out.println("@roleFunctionality button pressed. rid = " + this.rid +" , " + alivePlayers);

        if(isDead){
            System.out.println("User is dead");
            return;
        }
        System.out.println("roleActionDone = " + roleActionDone);
        if(roleActionDone){ // Gör inget om man redan gjort en action idag
            return;
        }
        ArrayList<String> tempList = new ArrayList<String>(alivePlayers);
        switch(this.rid){
            case 1:
                //Mafia
                // Get a list of players, vote on who to kill, when majority has selected the same one, hes chosen
                if(isDay){
                    roleHandlerNoOption();
                }else{
                    tempList.remove(userName);
                    roleHandlerList(tempList);
                }
                break;
            case 2:
                //Cop
                // Get a list of players, vote on who to check, get Yes or No
                if(isDay){
                    roleHandlerNoOption();
                }else{
                    tempList.remove(userName);
                    roleHandlerList(tempList);
                }
                break;
            case 3:
                //Medic
                if(isDay){
                    roleHandlerNoOption();
                }else{
                    if(!lastMedicPick.equals("")){
                        tempList.remove(lastMedicPick); // Sätt på rätt ställe
                    }
                    roleHandlerList(tempList);
                }
                break;
            case 4:
                roleHandlerNoOption();
                break;
            case 5:
                //Sheriff
                if(isDay){
                    tempList.remove(userName);
                    roleHandlerList(tempList);
                }else{
                    roleHandlerNoOption();
                }
                break;
            case 6:
                //Crazy Cop
                if(isDay){
                    roleHandlerNoOption();
                }else{
                    tempList.remove(userName);
                    roleHandlerList(tempList);
                }
                break;
        }
    }

    public void roleHandlerList(ArrayList<String> tempList){
        System.out.println("@roleHandlerList");
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.role_chooser, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        TextView instructField = (TextView)promptsView.findViewById(R.id.instruct);
        instructField.setText(instructText);
        // set prompts.xml to alertdialog builder
        alertDialogBuilder.setView(promptsView);

        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.cancel();
                    }
                });
        ArrayAdapter<String> listPlayerAdapter = new ArrayAdapter<String>(this, R.layout.player_list_item, tempList);
        alertDialog = alertDialogBuilder.create();
        initPlayerList(promptsView, alertDialog, 1);
        playerList.setAdapter(listPlayerAdapter);


        alertDialog.show();
    }

    public void roleHandlerNoOption(){
        System.out.println("@roleHandlerNoOption");
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.role_chooser_no_option, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set prompts.xml to alertdialog builder
        alertDialogBuilder.setView(promptsView);

        TextView text = (TextView) promptsView.findViewById(R.id.NoAction);
        text.setText("You have no possible actions at this time as " + roleName);
        TextView text2 = (TextView) promptsView.findViewById(R.id.dayNightCheck);
        if(isDay){
            text2.setText("It's currently Day");
        }
        else{
            text2.setText("It's currently Night");
        }

        // set dialog message
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.cancel();
                    }
                });
        alertDialogBuilder.setPositiveButton("Ok",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.cancel();
                    }
                });

        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    public void accuse(View view){
        System.out.println("@accuse");
        if(isDead){
            System.out.println("User is dead");
            return;
        }
        if(this.accusesLeft <= 0){ // Ska vara == 0
            System.out.println("No accuses left");
            client.emitter("startVote");
            return;
        }

        if(accusersList.contains(userName)){ // Prevents same user from participating in many accusers
            System.out.println("This user has already been active in a accuse"); // Might want to show to user
            return;
        }


        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.role_chooser, null);
        TextView instructField = (TextView)promptsView.findViewById(R.id.instruct);
        instructField.setText(instructText);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set prompts.xml to alertdialog builder
        alertDialogBuilder.setView(promptsView);

        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.cancel();
                    }
                });
        ArrayList<String> tempList = new ArrayList<String>(alivePlayers);
        tempList.remove(userName);
        ArrayAdapter<String> listPlayerAdapter = new ArrayAdapter<String>(this, R.layout.player_list_item, tempList);
        alertDialog = alertDialogBuilder.create();
        initPlayerList(promptsView, alertDialog, 2);
        playerList.setAdapter(listPlayerAdapter);


        alertDialog.show();
    }

    public void sendMessage(View view){
        if(!isDay){
            return;
        }
        EditText editText = (EditText) findViewById(R.id.enter_msg);
        String msg = editText.getText().toString();
        editText.setText("");
        System.out.println("TIME TO SEND A MESSAGE! " + msg);
        client.sendMessage(msg);
    }

    public void role(String rid, String roleNameInput, String instruct){
        System.out.println(instruct);
        this.instructText = instruct;
        roleName = getRoleName(roleNameInput);
        TextView role = (TextView)findViewById(R.id.role);
        role.setText(roleName);
        TextView instructField = (TextView)findViewById(R.id.instruct);
        instructField.setText(instruct);
        this.rid = Integer.parseInt(rid);
    }

    public String getRoleName(String roleName){
        if(roleName.equals("Crazy Cop")){
            return "Cop";
        }
        return roleName;
    }


    public void playerDied(String user, String killer) {
        alivePlayers.remove(user);
        System.out.println("@playerDied killer = " + killer);
        if(killer.equals("Mafia")){
            messageAdapter.add("During the night " + user+ " was killed");
        }else if(killer.startsWith("Sheriff")){
            messageAdapter.add(killer + " shot " + user + " in the face");
        }else if(killer.equals("Town")){
            messageAdapter.add(user + " was hanged");
        }
        if(user.equals(userName)){
            // You died
            isDead = true;
        }
        changePlayerColorState();
    }

    public void addMessage(String user, String message){
        messageAdapter.add(user + ": " + message);
    }

    public void addMessage(String message){
        messageAdapter.add(message);
    }


    public void gameEnding(String message, String resultArray){
        System.out.println("@GAME ENDING");
        client.leaveSocketAndRoom();
        Intent i = new Intent(this, EndScreen.class);
        i.putExtra("extra_message", message);
        i.putExtra("extra_array", resultArray);
        this.startActivity(i);
    }


    public void leaveUser(String user){
        messageAdapter.add(user + " left the game.");
    }

    public void createRoleInfoDialogView(ArrayList<String> roleInfo) {
        System.out.println("@roleinfoDialogView size = "+roleInfo.size());
        final CharSequence[] info = roleInfo.toArray(new String[roleInfo.size()]);
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Roles");
        dialogBuilder.setItems(info, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Nothing
            }
        });

        dialogBuilder.setPositiveButton("Ok",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.dismiss();
                    }
                });

        this.roleInfoDialog = dialogBuilder.create();
    }

    public void showRoleInfoDialog(View v) {
        if(this.roleInfoDialog != null) {
            roleInfoDialog.show();
        }
    }

    public void accuseStarted(String paramUserName, String paramUserChosen){
        // Start popup for accusation. This needs to be cancelled when any player has verified it
        currentAccused = paramUserChosen;
        currentAccuser = paramUserName;
        String message = paramUserName + " has accused " + paramUserChosen;
        if(userName.equals(currentAccused) || userName.equals(currentAccuser)){
            System.out.println("You do not get popup");
            return;
        }
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.accuse_confirm, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set prompts.xml to alertdialog builder
        alertDialogBuilder.setView(promptsView);

        TextView text = (TextView) promptsView.findViewById(R.id.accuseConfirmText);
        text.setText(message);
        TextView text2 = (TextView) promptsView.findViewById(R.id.accuseConfirmText2);
        text2.setText("The accusation needs to be confirmed to go through");


        // set dialog message
        alertDialogBuilder.setCancelable(false);
        // Set knapparna istället
        Button noConfirm = (Button) promptsView.findViewById(R.id.noConfirm);
        Button confirm = (Button) promptsView.findViewById(R.id.confirm);
        noConfirm.setText(getString(R.string.noConfirm));
        confirm.setText(getString(R.string.confirm));
        noConfirm.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                client.emitter("noConfirmAccuse");
            }
        });
        confirm.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                client.emitter("confirmAccuse");
            }
        });

        /*
        alertDialogBuilder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.cancel();
                    }
                });
        alertDialogBuilder.setPositiveButton("Confirm",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        client.emitter("confirmAccuse");
                    }
                });
        */
        alertDialog = alertDialogBuilder.create();
        alertDialog.show();

    }

    public void accuseConfirmed(String paramUserConfirmer){
        if(alertDialog != null) {
            alertDialog.cancel();
        }

        currentConfirmer = paramUserConfirmer;
        String message = currentAccuser + " successfully accused " + currentAccused + " with help of " + currentConfirmer;
        // Show the message to the user TODO
        messageAdapter.add(message);

        // Adding accuse
        accuseList.add(currentAccused);
        accusersList.add(currentAccuser);
        accusersList.add(currentConfirmer); // Prevents confirmer and accuser to accuse again this day
        accusesLeft--;
         // Update accuses left text on button
        if(accusesLeft == 0){
            accuseButton.setText(getString(R.string.vote_on_accuse) + " (" + (2-accusesLeft)+"/2)");
        }else{
            accuseButton.setText(getString(R.string.accuse) + " (" + (2-accusesLeft)+"/2)");
        }
    }

    public void accuseNotMade(){
        System.out.println("accuse not made");
        currentAccused = "";
        currentAccuser = "";
       if(alertDialog != null) {
           alertDialog.cancel(); // Borde nog bara cancela för accuseConfirm screenen
       }

    }

    public void secondVoting(){
        System.out.println("@secondVoting");
        if(timeOutDuringVote){
            startTimer(60000); // if timer runs out, give some extra time
        }
        accuseList = new ArrayList<String>();
        accusersList = new ArrayList<String>();
        currentAccused = "";
        currentAccuser = "";
        currentConfirmer = "";
        accusesLeft = 1;
        accuseButton.setText(getString(R.string.accuse) + " (" + (2-accusesLeft)+"/2)");
    }


    public void startVote(){
        System.out.println("@startVote");

        if(isDead){
            System.out.println("You cant vote, you are dead");
            return;
        }

        hasStartedVote = true;
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.role_chooser, null);

        TextView instruct = (TextView) findViewById(R.id.instruct);
        instruct.setText(getString(R.string.vote_to_kill));

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set prompts.xml to alertdialog builder
        alertDialogBuilder.setView(promptsView);

        alertDialogBuilder.setCancelable(false);
        ArrayList<String> tempList = new ArrayList<String>(accuseList);
        if(tempList.contains(userName)){
            tempList.remove(userName);
        }
        tempList.add("< Not Voting >");

        ArrayAdapter<String> listPlayerAdapter = new ArrayAdapter<String>(this, R.layout.player_list_item, tempList);
        alertDialog = alertDialogBuilder.create();
        initPlayerList(promptsView, alertDialog, 4);
        playerList.setAdapter(listPlayerAdapter);

        alertDialog.show();
    }

    public void copInfo(String message){
        System.out.println("@copInfo");
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.role_chooser_no_option, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set prompts.xml to alertdialog builder
        alertDialogBuilder.setView(promptsView);

        TextView text = (TextView) promptsView.findViewById(R.id.NoAction);
        text.setText("Your information as a cop were: ");
        TextView text2 = (TextView) promptsView.findViewById(R.id.dayNightCheck);
        text2.setText(message);

        // set dialog message
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.cancel();
                    }
                });
        alertDialogBuilder.setPositiveButton("Ok",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.cancel();
                    }
                });

        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private class roleInfoAsync extends AsyncTask<String, Void, ArrayList<String>> {

        @Override
        protected ArrayList<String> doInBackground(String... params) {
            URL url = null;
            String result = "";
            ArrayList<String> roleInfo = new ArrayList<String>();
            try {
                String path = Settings.ip+":"+Settings.port;//"192.168.1.72:8989";
                System.out.println(path);
                url = new URL("http://"+path+"/roleInfo");
            } catch(MalformedURLException e) {
                e.printStackTrace();
            }
            try {
                System.out.println("@roleInfoAsync trying to Get");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestProperty("User-Agent","Mafia");
                con.setRequestMethod("GET");
                int code = con.getResponseCode();
                if(code != 200){
                    System.out.println("Response code isn't 200: " + code);
                }
                else{
                    //response = con.getHeaderField("rooms");
                    StringBuilder sb = new StringBuilder();
                    BufferedReader response = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    String line = "";
                    System.out.println("Getting roleInfo Response");
                    while((line=response.readLine()) != null){
                        sb.append(line + "\n");
                    }
                    result = sb.toString();
                    System.out.println("@RESULTFROM ROLE INFO");
                    System.out.println(result);
                    try{
                        JSONObject obj =new JSONObject(result);
                        System.out.println("OBJ = " + obj.get("list"));
                        JSONArray jsonArray = (JSONArray)obj.get("list");
                        if (jsonArray != null) {
                            int len = jsonArray.length();
                            for (int i=0;i<len;i++){
                                System.out.println("@jasonArrayElement"+jsonArray.get(i).toString());
                                JSONObject temp = new JSONObject(jsonArray.get(i).toString());
                                roleInfo.add(temp.get("info").toString());
                            }
                        }
                        System.out.println("ROOMS");
                    }catch(JSONException e){
                        System.out.println("@JSONEXception");
                        e.printStackTrace();
                    }
                }
            } catch(IOException e) {
                System.out.println("@IOException");
                e.printStackTrace();
            }
            return roleInfo;
        }

        @Override
        protected void onPostExecute(ArrayList<String> result) {
            System.out.println("Creating roleinfodialogView");
            createRoleInfoDialogView(result);
        }

    }
}
