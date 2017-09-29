package com.icegotcha.messagelist;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private AlertDialog dialog;

    private DatabaseReference frRootRef;
    private DatabaseReference frMessageRef;

    private ArrayAdapter<String> adapter;
    private List<String> messageSet = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        frRootRef = FirebaseDatabase.getInstance().getReference();
        frMessageRef = frRootRef.child("messages");

        queryMessages();

        ListView messageDisplaying = (ListView) findViewById(R.id.message_list);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, messageSet);
        messageDisplaying.setAdapter(adapter);

        dialog = createMessageDialog();

        Button newMessageBtn = (Button) findViewById(R.id.new_message_button);
        newMessageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.show();
            }
        });

    }

    private void queryMessages() {
        frMessageRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot == null || dataSnapshot.getValue() == null) return;

                messageSet.clear();

                // get map of message data
                Map<String, Object> messages = (Map<String, Object>) dataSnapshot.getValue();

                //iterate through each message, ignoring their UID
                for (Map.Entry<String, Object> entry : messages.entrySet()) {
                    // add single message to list
                    Map singleMessage = (Map) entry.getValue();

                    String messageInFormat = String.format(Locale.ROOT,
                            "%s\nเขียนโดย : %s\nemail: %s",
                            String.valueOf(singleMessage.get("message")),
                            String.valueOf(singleMessage.get("user")),
                            String.valueOf(singleMessage.get("email")));
                    messageSet.add(messageInFormat);
                    // Update ListView
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, "Oops! Can't load messages\n" +
                        "Error: " + databaseError.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

    }

    private int dpToPixel(int dp) {
        float scale = getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    private void addMessage(String username, String email, String message) {
        Map<String, String> values = new HashMap<>();
        values.put("user", username);
        values.put("email", email);
        values.put("message", message);
        frMessageRef.push().setValue(values, new DatabaseReference.CompletionListener() {
            public void onComplete(DatabaseError error, DatabaseReference ref) {
                String annouce;
                if (error != null) {
                    annouce = "Can't add message," +
                            "it has an error: " + error.getMessage();
                } else {
                    annouce = "Added successfully!";
                }
                Toast.makeText(MainActivity.this, annouce, Toast.LENGTH_LONG).show();
            }
        });
    }

    private AlertDialog createMessageDialog() {
        // ***** Create Dialog Layout *****

        int paddElementInDp = 10;
        int paddLayoutInDp = 5;

        int paddElement = dpToPixel(paddElementInDp);
        int paddLayout = dpToPixel(paddLayoutInDp);


        LinearLayout dialogLayout = new LinearLayout(MainActivity.this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(paddLayout, paddLayout, paddLayout, paddLayout);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        // Create Views
        final EditText usernameText = new EditText(MainActivity.this);
        usernameText.setHint("Username");
        usernameText.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        usernameText.setPadding(paddElement, paddElement, paddElement, paddElement);
        usernameText.setLayoutParams(layoutParams);

        final EditText emailText = new EditText(MainActivity.this);
        emailText.setHint("Email");
        emailText.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailText.setPadding(paddElement, paddElement, paddElement, paddElement);
        emailText.setLayoutParams(layoutParams);

        final EditText messageText = new EditText(MainActivity.this);
        messageText.setHint("Message");
        messageText.setInputType(InputType.TYPE_TEXT_VARIATION_LONG_MESSAGE);
        messageText.setPadding(paddElement, paddElement, paddElement, paddElement);
        messageText.setLayoutParams(layoutParams);
        messageText.setGravity(Gravity.TOP);
        messageText.setSingleLine(false);
        messageText.setImeOptions(EditorInfo.IME_FLAG_NO_ENTER_ACTION);
        messageText.setLines(5);

        // Add views to layout
        dialogLayout.addView(usernameText, 0);
        dialogLayout.addView(emailText, 1);
        dialogLayout.addView(messageText, 2);

        // ***** Create dialog *****
        return new AlertDialog.Builder(MainActivity.this)
                .setTitle("New Message")
                .setView(dialogLayout)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String username = usernameText.getText().toString(),
                                email = emailText.getText().toString(),
                                message = messageText.getText().toString();


                        if (username.equals("") || email.equals("") || message.equals("")) {
                            Toast.makeText(MainActivity.this, "Please fill the form completely",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        // add to database
                        addMessage(username, email, message);

                        // reset
                        usernameText.setText("");
                        emailText.setText("");
                        messageText.setText("");
                    }
                })
                .create();


    }

}
