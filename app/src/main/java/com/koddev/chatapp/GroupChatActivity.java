package com.koddev.chatapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Display;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.koddev.chatapp.Adapter.GroupChatAdapter;
import com.koddev.chatapp.Adapter.MessageAdapter;
import com.koddev.chatapp.Fragments.APIService;
import com.koddev.chatapp.Model.Chat;
import com.koddev.chatapp.Model.User;
import com.koddev.chatapp.Notifications.Client;
import com.koddev.chatapp.Notifications.Data;
import com.koddev.chatapp.Notifications.MyResponse;
import com.koddev.chatapp.Notifications.Sender;
import com.koddev.chatapp.Notifications.Token;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GroupChatActivity extends AppCompatActivity {

    private ImageButton btn_send, btn_add;
    private static EditText text_send;
    private CircleImageView profile_image;
    private TextView groupTextDisplay;
    private String currentGroupName, currentUserID, currentUserName, groupID;

    private FirebaseAuth firebaseAuth;
    private DatabaseReference userRef, groupRef, messageKeyRef;

    private RecyclerView recyclerView;
    private APIService apiService;
    private GroupChatAdapter groupChatAdapter;

    private ValueEventListener seenListener;

    private List<Chat> mChat = new ArrayList<>();

    Intent intent;
    boolean notify = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(currentGroupName);

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        profile_image = findViewById(R.id.profile_image);
        groupTextDisplay = findViewById(R.id.group_text);
        btn_send = findViewById(R.id.btn_send);
        btn_add = findViewById(R.id.btn_add);
        text_send = findViewById(R.id.text_send);

        apiService = Client.getClient("https://fcm.googleapis.com/").create(APIService.class);

        currentGroupName = getIntent().getExtras().get("groupName").toString();
        Toast.makeText(GroupChatActivity.this, currentGroupName, Toast.LENGTH_SHORT).show();

        firebaseAuth = FirebaseAuth.getInstance();
        currentUserID = firebaseAuth.getCurrentUser().getUid();
        groupID = intent.getStringExtra("groupID");
        userRef = FirebaseDatabase.getInstance().getReference().child("Users");
        groupRef = FirebaseDatabase.getInstance().getReference().child("Groups").child(currentGroupName);


        getUserInfo();

        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                sendMessage(false);
                text_send.setText("");
            }
        });

        groupRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                readMessages(currentUserID, groupID);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        seenMessage(groupID);
    }


    private void getUserInfo() {
        userRef.child(currentUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {
                    currentUserName = dataSnapshot.child("username").getValue().toString();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void seenMessage(final String userId){
        seenListener = groupRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                    Chat chat = snapshot.getValue(Chat.class);
                    if (chat.getReceiver().equals(currentUserID) && chat.getSender().equals(userId)){
                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("isseen", true);
                        snapshot.getRef().updateChildren(hashMap);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void sendMessage(boolean media){
        //DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

        String message = text_send.getText().toString();
        String messageKey = groupRef.push().getKey();

        HashMap<String, Object> hash_Map = new HashMap<>();
        groupRef.updateChildren(hash_Map);

        messageKeyRef = groupRef.child(messageKey);

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("sender", currentUserName);
        hashMap.put("receiver", groupID);
        hashMap.put("message", message);
        hashMap.put("date", DateFormat.getDateTimeInstance().format(new Date()));
        hashMap.put("isseen", false);
        hashMap.put("media", media);

        groupRef.push().setValue(hashMap);


        // add user to chat fragment
        final DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference("Groups").child(currentGroupName)
                .child(currentUserID)
                .child(groupID);

        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()){
                    groupRef.child("id").setValue(groupID);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        final DatabaseReference chatRefReceiver = FirebaseDatabase.getInstance().getReference("Groups").child(currentGroupName)
                .child(groupID)
                .child(currentUserID);
        chatRefReceiver.child("id").setValue(currentUserID);

        final String msg = message;

        userRef.child(currentUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                if (notify) {
                    sendNotification(groupID, user.getUsername(), msg);
                }
                notify = false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void sendNotification(String receiver, final String username, final String message){
        DatabaseReference tokens = FirebaseDatabase.getInstance().getReference("Tokens");
        Query query = tokens.orderByKey().equalTo(receiver);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                    Token token = snapshot.getValue(Token.class);
                    Data data = new Data(currentUserID, R.mipmap.ic_launcher, username+": "+message, "New Message",
                            groupID);

                    Sender sender = new Sender(data, token.getToken());

                    apiService.sendNotification(sender)
                            .enqueue(new Callback<MyResponse>() {
                                @Override
                                public void onResponse(Call<MyResponse> call, Response<MyResponse> response) {
                                    if (response.code() == 200){
                                        if (response.body().success != 1){
                                            Toast.makeText(GroupChatActivity.this, "Failed!", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }

                                @Override
                                public void onFailure(Call<MyResponse> call, Throwable t) {

                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void readMessages(final String myid, final String groupID){

        groupRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mChat.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                    Chat chat = snapshot.getValue(Chat.class);
                    if (chat.getReceiver().equals(myid) && chat.getSender().equals(groupID) ||
                            chat.getReceiver().equals(groupID) && chat.getSender().equals(myid)){
                        mChat.add(chat);
                    }

                    groupChatAdapter = new GroupChatAdapter(GroupChatActivity.this, mChat);
                    recyclerView.setAdapter(groupChatAdapter);
                    recyclerView.smoothScrollToPosition(recyclerView.getAdapter().getItemCount());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void currentUser(String userId){
        SharedPreferences.Editor editor = getSharedPreferences("PREFS", MODE_PRIVATE).edit();
        editor.putString("currentuser", userId);
        editor.apply();
    }

    private void status(String status){
        userRef.child(currentUserID);

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("status", status);

        userRef.child(currentUserID).updateChildren(hashMap);
    }

    @Override
    protected void onResume() {
        super.onResume();
        status("online");
        currentUser(groupID);
    }

    @Override
    protected void onPause() {
        super.onPause();
        userRef.child(currentUserID).removeEventListener(seenListener);
        status("offline");
        currentUser("none");
    }
}
