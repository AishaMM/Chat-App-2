package com.koddev.chatapp;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.koddev.chatapp.Adapter.MessageAdapter;
import com.koddev.chatapp.Fragments.APIService;
import com.koddev.chatapp.Model.Chat;
import com.koddev.chatapp.Model.User;
import com.koddev.chatapp.Notifications.Client;
import com.koddev.chatapp.Notifications.Data;
import com.koddev.chatapp.Notifications.MyResponse;
import com.koddev.chatapp.Notifications.Sender;
import com.koddev.chatapp.Notifications.Token;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

public class MessageActivity extends AppCompatActivity {

    private CircleImageView profile_image;
    private TextView username;

    private FirebaseUser fUser;

    private StorageReference imageStorage;
    private StorageReference videoStorage;
    private DatabaseReference reference;

    private ImageButton btn_send, btn_add;
    private static EditText text_send;
    private ImageView imageView;
    private VideoView videoView;
    private MediaController mediacontroller;

    private MessageAdapter messageAdapter;
    private List<Chat> mChat;

    private RecyclerView recyclerView;
    private RelativeLayout imageLayoutBackground;

    private Intent intent;

    private ValueEventListener seenListener;

    private String userId;

    private APIService apiService;

    boolean notify = false;

    private static final int CAMERA = 1;
    private static final int GALLERY_PICK = 2;
    private static final int RECORD_VIDEO = 3;
    private static final int GALLERY_PICK_VIDEO = 4;
    public static final int READ_EXTERNAL_STORAGE = 0, WRITE_EXTERNAL_STORAGE = 1 , MULTIPLE_PERMISSIONS = 10;


    String[] permissions = new String[] {
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };

    private static String MEDIA_DIRECTORY = "MEDIA";
    private String videoPath = "";
    private Uri filePath, videoUri;
    final CharSequence[] options = {"Take Photo", "Take Video", "Gallery", "Cancel"};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // and this
                startActivity(new Intent(MessageActivity.this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            }
        });

        apiService = Client.getClient("https://fcm.googleapis.com/").create(APIService.class);

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        imageLayoutBackground = findViewById(R.id.message_layout);

        profile_image = findViewById(R.id.profile_image);
        username = findViewById(R.id.username);
        btn_send = findViewById(R.id.btn_send);
        btn_add = findViewById(R.id.btn_add);
        text_send = findViewById(R.id.text_send);
        imageView = findViewById(R.id.imageView);
        videoView = findViewById(R.id.videoView);

        imageStorage = FirebaseStorage.getInstance().getReference("Images");
        videoStorage = FirebaseStorage.getInstance().getReference("Videos");

        mediacontroller = new MediaController(this);
        videoView.setMediaController(mediacontroller);
        mediacontroller.setAnchorView(videoView);
        // String uriPath = "android.resource://com.koddev.chatapp.videoviewdemonuts/"+R.raw.realshort;  //update package name
        // Uri uri = Uri.parse(uriPath);
        //videoView.setVideoURI(uri);
        filePath = Uri.parse(Environment.getExternalStorageDirectory().getPath() + "MEDIA.mp4");

        intent = getIntent();
        userId = intent.getStringExtra("userid");
        fUser = FirebaseAuth.getInstance().getCurrentUser();

        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                notify = true;
                String msg = text_send.getText().toString();
                if (!msg.equals("")){
                    sendMessage(fUser.getUid(), userId, msg, false);
                } else {
                    Toast.makeText(MessageActivity.this, "You can't send empty message", Toast.LENGTH_SHORT).show();
                }
                text_send.setText("");
            }
        });

        btn_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                notify = true;
                Choice();
            }
        });

        reference = FirebaseDatabase.getInstance().getReference("Users").child(userId);

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                username.setText(user.getUsername());
                if (user.getImageURL().equals("default")){
                    profile_image.setImageResource(R.mipmap.ic_launcher);
                } else {
                    Glide.with(getApplicationContext()).load(user.getImageURL()).into(profile_image);
                }

                readMessages(fUser.getUid(), userId, user.getImageURL());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        seenMessage(userId);

    }

    private void seenMessage(final String userId){
        reference = FirebaseDatabase.getInstance().getReference("Chats");
        seenListener = reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                    Chat chat = snapshot.getValue(Chat.class);
                    if (chat.getReceiver().equals(fUser.getUid()) && chat.getSender().equals(userId)){
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

    private void Choice() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MessageActivity.this);
        builder.setTitle("Choose Source");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (options[item].equals("Take Photo")) {
                    if (ContextCompat.checkSelfPermission(MessageActivity.this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MessageActivity.this, permissions, MULTIPLE_PERMISSIONS);
                    } else {
                        callCamera();
                    }
                }
                if (options[item].equals("Take Video")) {
                    if (ContextCompat.checkSelfPermission(MessageActivity.this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MessageActivity.this, permissions, MULTIPLE_PERMISSIONS);
                    } else {
                        recordVideo();
                    }
                }
                if (options[item].equals("Gallery")) {
                    if (ContextCompat.checkSelfPermission(MessageActivity.this,
                            Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MessageActivity.this,
                                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE);
                    } else {
                        callGallery();
                    }
                }
                if (options[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //switch (requestCode) {
            if (requestCode == READ_EXTERNAL_STORAGE) {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    callGallery();
                }
            }

            else if(requestCode == WRITE_EXTERNAL_STORAGE) {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    callCamera();
                    //recordVideo();
                }
            }

            else if(requestCode == WRITE_EXTERNAL_STORAGE) {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //callCamera();
                    recordVideo();
                }
            }

            else if(requestCode == MULTIPLE_PERMISSIONS) {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    callCamera();
                    //recordVideo();
                }
            }
            else if(requestCode == MULTIPLE_PERMISSIONS) {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //callCamera();
                    recordVideo();
                }
            }

    }

    private void callCamera() {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            // Ensure that there's a camera activity to handle the intent
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                super.startActivityForResult(takePictureIntent, CAMERA);
            }
    }

    private void callGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK_ACTIVITY);
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        super.startActivityForResult(Intent.createChooser(intent, "SELECT IMAGE"), GALLERY_PICK);
    }

    private void recordVideo() {
        Intent recordVideo = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        recordVideo.setType("video/*");
        if(recordVideo.resolveActivity(getPackageManager()) != null) {

            intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
            intent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, 500000); // set the size to no limit

            super.startActivityForResult(recordVideo, RECORD_VIDEO);
        }
    }


    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(((requestCode == GALLERY_PICK  && data != null && data.getData() != null ) || requestCode == CAMERA || requestCode == RECORD_VIDEO || requestCode == GALLERY_PICK_VIDEO) && resultCode == RESULT_OK && data != null)
        {

            switch (requestCode) {
                case CAMERA:
                    Bitmap photo = (Bitmap) data.getExtras().get("data");
                    filePath = getImageUri(getApplicationContext(), photo);
                    imageView.setImageBitmap(photo);
                    uploadImage();
                    break;

                case GALLERY_PICK:
                    filePath = data.getData();
                    if (filePath != null) {
                        imageView.setImageURI(filePath);
                    }
                    uploadImage();
                    break;

              case RECORD_VIDEO:
                    videoUri = data.getData();
                    videoView.setMediaController(mediacontroller);
                    videoView.setVideoURI(videoUri);
                    videoView.start();
                    uploadVideo();
                    break;

                case GALLERY_PICK_VIDEO:
                    videoUri = data.getData();
                    if (videoUri != null) {
                        videoView.setVideoURI(videoUri);
                    }
                    uploadVideo();
                    break;
            }
        }

    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item){
        switch (item.getItemId()) {
            case android.R.id.home:
                this.onBackPressed();//takes user back
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    public boolean onSupportNavigateUp () {
        onBackPressed();
        return true;
    }

    private void uploadImage() {
        if(filePath != null)
        {
            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Uploading...");
            progressDialog.show();

            reference = FirebaseDatabase.getInstance().getReference("Chats");
            DatabaseReference push_message = reference.child(fUser.getUid()).child(userId).push();
            String push_id = push_message.getKey();

            StorageReference ref = imageStorage.child("images/"+ push_id);
            ref.putFile(filePath)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            //Send message to the other person
                            Task<Uri> result  = taskSnapshot.getMetadata().getReference().getDownloadUrl();
                            result.addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    sendMessage(fUser.getUid(), userId, uri.toString(), true);

                                    Toast.makeText(MessageActivity.this, "Image uploaded", Toast.LENGTH_LONG).show();
                                    progressDialog.dismiss();
                                    Toast.makeText(MessageActivity.this, "Uploaded", Toast.LENGTH_SHORT).show();
                                    imageView.setImageDrawable(null);
                                }
                            });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.dismiss();
                            Toast.makeText(MessageActivity.this, "Failed "+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            double progress = (100.0*taskSnapshot.getBytesTransferred()/taskSnapshot
                                    .getTotalByteCount());
                            progressDialog.setMessage("Uploaded "+(int)progress+"%");
                        }
                    });
        }
    }

    private void uploadVideo() {
        if(videoPath != null)
        {
            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Uploading...");
            progressDialog.show();

            reference = FirebaseDatabase.getInstance().getReference("Chats");
            DatabaseReference push_message = reference.child(fUser.getUid()).child(userId).push();
            String push_id = push_message.getKey();

            StorageReference ref = videoStorage.child("videos/"+ push_id);
            ref.putFile(videoUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            //Send message to the other person
                            Task<Uri> result  = taskSnapshot.getMetadata().getReference().getDownloadUrl();
                            result.addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    sendMessage(fUser.getUid(), userId, uri.toString(), true);

                                    Toast.makeText(MessageActivity.this, "Video uploaded", Toast.LENGTH_LONG).show();
                                    progressDialog.dismiss();
                                    Toast.makeText(MessageActivity.this, "Uploaded", Toast.LENGTH_SHORT).show();
                                    videoView.setVideoURI(null);
                                }
                            });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.dismiss();
                            Toast.makeText(MessageActivity.this, "Failed "+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            double progress = (100.0*taskSnapshot.getBytesTransferred()/taskSnapshot
                                    .getTotalByteCount());
                            progressDialog.setMessage("Uploaded "+(int)progress+"%");
                        }
                    });
        }
    }

    private void sendMessage(String sender, final String receiver, String message, boolean media){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("sender", sender);
        hashMap.put("receiver", receiver);
        hashMap.put("message", message);
        hashMap.put("date", DateFormat.getDateTimeInstance().format(new Date()));
        hashMap.put("isseen", false);
        hashMap.put("media", media);

        reference.child("Chats").push().setValue(hashMap);


        // add user to chat fragment
        final DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference("Chatlist")
                .child(fUser.getUid())
                .child(userId);

        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()){
                    chatRef.child("id").setValue(userId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        
        final DatabaseReference chatRefReceiver = FirebaseDatabase.getInstance().getReference("Chatlist")
                .child(userId)
                .child(fUser.getUid());
        chatRefReceiver.child("id").setValue(fUser.getUid());

        final String msg = message;

        reference = FirebaseDatabase.getInstance().getReference("Users").child(fUser.getUid());
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                if (notify) {
                    sendNotification(receiver, user.getUsername(), msg);
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
                    Data data = new Data(fUser.getUid(), R.mipmap.ic_launcher, username+": "+message, "New Message",
                            userId);

                    Sender sender = new Sender(data, token.getToken());

                    apiService.sendNotification(sender)
                            .enqueue(new Callback<MyResponse>() {
                                @Override
                                public void onResponse(Call<MyResponse> call, Response<MyResponse> response) {
                                    if (response.code() == 200){
                                        if (response.body().success != 1){
                                            Toast.makeText(MessageActivity.this, "Failed!", Toast.LENGTH_SHORT).show();
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

    private void readMessages(final String myid, final String userId, final String imageurl){
        mChat = new ArrayList<>();

        reference = FirebaseDatabase.getInstance().getReference("Chats");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mChat.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                    Chat chat = snapshot.getValue(Chat.class);
                    if (chat.getReceiver().equals(myid) && chat.getSender().equals(userId) ||
                            chat.getReceiver().equals(userId) && chat.getSender().equals(myid)){
                        mChat.add(chat);
                    }

                    messageAdapter = new MessageAdapter(MessageActivity.this, mChat, imageurl);
                    recyclerView.setAdapter(messageAdapter);
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
        reference = FirebaseDatabase.getInstance().getReference("Users").child(fUser.getUid());

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("status", status);

        reference.updateChildren(hashMap);
    }

    @Override
    protected void onResume() {
        super.onResume();
        status("online");
        currentUser(userId);
    }

    @Override
    protected void onPause() {
        super.onPause();
        reference.removeEventListener(seenListener);
        status("offline");
        currentUser("none");
    }
}
