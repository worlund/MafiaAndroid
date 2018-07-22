package com.mafia.mafia;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public class EndScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_end_screen);
        Intent intent = getIntent();
        String message = intent.getStringExtra("extra_message");
        String tempArrayString = intent.getStringExtra("extra_array");

        String[] tempS = tempArrayString.split("\",\""); // Borde ge array?
        ArrayList<String> result = new ArrayList<String>();
        System.out.println("results: ");
        for(String s : tempS){
            System.out.println("String = " + s);

            result.add(s);
        }
        result.set(0, result.get(0).substring(2));
        result.set(result.size()-1, result.get(result.size()-1).substring(0,result.get(result.size()-1).length()-1));

        System.out.println("Printing it all");
        for(String s : result){
            System.out.println(s);
        }

        TextView instructText = (TextView) findViewById(R.id.instructText);
        TextView resultField = (TextView) findViewById(R.id.instruct);
        instructText.setText("Results: ");
        resultField.setText(message);


        ArrayAdapter<String> listPlayerAdapter = new ArrayAdapter<String>(this, R.layout.player_list_item, result);

        ListView list = (ListView) findViewById(R.id.listPlayersRole);
        list.setAdapter(listPlayerAdapter);
    }


    public void goToMenu(View view){
        Intent i = new Intent(this, MenuActivity.class);
        this.startActivity(i);
    }
}
