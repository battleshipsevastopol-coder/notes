package com.example.notes;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final String PREFS_NAME = "NotesPrefs";
    private static final String KEY_NOTE = "note_content";

    private EditText noteEditText;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        noteEditText = (EditText) findViewById(R.id.noteEditText);
        Button saveButton = (Button) findViewById(R.id.saveButton);
        Button clearButton = (Button) findViewById(R.id.clearButton);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Load saved note
        String savedNote = prefs.getString(KEY_NOTE, "");
        noteEditText.setText(savedNote);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = noteEditText.getText().toString();
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(KEY_NOTE, text);
                editor.apply();
                Toast.makeText(MainActivity.this, R.string.saved, Toast.LENGTH_SHORT).show();
            }
        });

        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                noteEditText.setText("");
                SharedPreferences.Editor editor = prefs.edit();
                editor.remove(KEY_NOTE);
                editor.apply();
                Toast.makeText(MainActivity.this, R.string.cleared, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Auto-save on pause
        String text = noteEditText.getText().toString();
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_NOTE, text);
        editor.apply();
    }
}
