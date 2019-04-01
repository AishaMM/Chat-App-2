package com.koddev.chatapp.Adapter;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.koddev.chatapp.MainActivity;
import com.koddev.chatapp.Model.Group;
import com.koddev.chatapp.R;

import java.util.List;

public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.ViewHolder>{
    private Context mContext;
    private List<Group> mGroups;

    public GroupAdapter(Context mContext, List<Group> mGroups) {
        this.mContext = mContext;
        this.mGroups = mGroups;
    }

    @NonNull
    @Override
    public GroupAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.group_item, parent, false);
        return new GroupAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        final Group group = mGroups.get(position);
        holder.groupName.setText(group.getName());
        if (group.getImageURL().equals("default")){
            holder.profile_image.setImageResource(R.mipmap.ic_launcher);
        }
    }

    @Override
    public int getItemCount() {
        return mGroups.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView groupName;
        public ImageView profile_image;

        /*View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Group groupName = mGroups.get(getAdapterPosition());
                Intent intent = new Intent(mContext, MainActivity.class);
                mContext.startActivity(intent);
            }
        };*/

        public ViewHolder(View itemView) {
            super(itemView);

            groupName = itemView.findViewById(R.id.groupName);
            profile_image = itemView.findViewById(R.id.profile_image);
        }
    }
}
