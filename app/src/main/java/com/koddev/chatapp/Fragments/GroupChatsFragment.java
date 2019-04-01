package com.koddev.chatapp.Fragments;

import android.support.v4.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.koddev.chatapp.Adapter.GroupAdapter;
import com.koddev.chatapp.Adapter.GroupChatAdapter;
import com.koddev.chatapp.Adapter.UserAdapter;
import com.koddev.chatapp.GroupChatActivity;
import com.koddev.chatapp.Model.Chatlist;
import com.koddev.chatapp.Model.Group;
import com.koddev.chatapp.Model.Grouplist;
import com.koddev.chatapp.Model.User;
import com.koddev.chatapp.Notifications.Token;
import com.koddev.chatapp.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class GroupChatsFragment extends Fragment {

    private RecyclerView recyclerView;

    private GroupAdapter groupAdapter;
    private List<Group> mGroups;

    FirebaseUser fUser;
    DatabaseReference reference;

    private List<Grouplist> groupsList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_groupchats, container, false);

        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        fUser = FirebaseAuth.getInstance().getCurrentUser();

        groupsList = new ArrayList<>();

        reference = FirebaseDatabase.getInstance().getReference("Grouplist").child(fUser.getUid());
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                groupsList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                    Grouplist grouplist = snapshot.getValue(Grouplist.class);
                    groupsList.add(grouplist);
                }

                groupList();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        updateToken(FirebaseInstanceId.getInstance().getToken());


        return view;
    }

    private void updateToken(String token){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Tokens");
        Token token1 = new Token(token);
        reference.child(fUser.getUid()).setValue(token1);
    }

    private void groupList() {
        mGroups = new ArrayList<>();
        reference = FirebaseDatabase.getInstance().getReference("Groups");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mGroups.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                    Group group = snapshot.getValue(Group.class);
                    for (Grouplist grouplist : groupsList){
                        if (grouplist.getId().equals(grouplist.getId())){
                            mGroups.add(group);
                        }
                    }
                }
                groupAdapter = new GroupAdapter(getContext(), mGroups);
                recyclerView.setAdapter(groupAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }






























    /*private View groupFragmentView;
    private ListView listView;
    ArrayAdapter<String> arrayAdapter;
    private ArrayList<String> list_of_groups = new ArrayList<>();

    private DatabaseReference GroupRef;

    public GroupChatsFragment() {

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        groupFragmentView = inflater.inflate(R.layout.fragment_groupchats, container, false);

        GroupRef = FirebaseDatabase.getInstance().getReference().child("Groups");

        listView = groupFragmentView.findViewById(R.id.list_view);
        arrayAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, list_of_groups);
        listView.setAdapter(arrayAdapter);

        retrieveAndDisplayGroups();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {

                String currentGroupName = adapterView.getItemAtPosition(position).toString();

                Intent intent = new Intent(getContext(), GroupChatActivity.class);
                intent.putExtra("groupName", currentGroupName);
                startActivity(intent);
            }
        });

        return groupFragmentView;
    }

    private void retrieveAndDisplayGroups() {
        GroupRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                Set<String> set = new HashSet<>(); //to prevent duplication
                Iterator iterator = dataSnapshot.getChildren().iterator();
                while(iterator.hasNext()) {
                    set.add(((DataSnapshot)iterator.next()).getKey());
                }

                list_of_groups.clear();
                list_of_groups.addAll(set);
                arrayAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }*/
}
