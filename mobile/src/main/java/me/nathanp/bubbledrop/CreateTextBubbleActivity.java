package me.nathanp.bubbledrop;

import android.content.Intent;
import android.media.Image;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

public class CreateTextBubbleActivity extends AppCompatActivity {
    private static final String TAG = "CreateTextBubbleActivity";
    EditText editText;
    ImageButton send;
    File file;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_typing_bubble);

        ImageButton closeButton = findViewById(R.id.closeButton);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        editText = findViewById(R.id.editText);
        send = findViewById(R.id.sendBubble);

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String text = editText.getText().toString();
                if (text.matches("")) {
                    Toast.makeText(getApplicationContext(), "input is empty", Toast.LENGTH_LONG).show();
                } else {
//                    file = new File(getCacheDir(), )
                    sendBubble(text);
                }
            }
        });


//        String input = editText.getText().toString();

    }

    public void sendBubble(String text) {
        Log.e(TAG, "Bubble should be sent!!!!");
        Intent data = new Intent();
        data.putExtra("bubbleText", text);
        setResult(RESULT_OK, data);
        finish();
    }

}
