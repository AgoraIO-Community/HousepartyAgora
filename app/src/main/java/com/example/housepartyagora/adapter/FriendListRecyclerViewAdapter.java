package com.example.housepartyagora.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.housepartyagora.R;
import com.example.housepartyagora.model.Friend;

import java.util.List;

public class FriendListRecyclerViewAdapter extends RecyclerView.Adapter<FriendListRecyclerViewAdapter.ViewHolder> {

    List<Friend> friendList;
    private static ClickListener clickListener;

    public FriendListRecyclerViewAdapter(List<Friend> list) {
        friendList = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.friend_item_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final Friend friend = friendList.get(position);
        holder.friendName.setText(friend.getUserName());
    }

    @Override
    public int getItemCount() {
        return friendList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        TextView friendName;
        Button addFriendButton;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            friendName = itemView.findViewById(R.id.text_friend_name);
            addFriendButton = itemView.findViewById(R.id.btn_add_friend);
            addFriendButton.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            clickListener.onItemClick(getAdapterPosition(), v);
        }
    }

    public void setOnItemClickListener(ClickListener clickListener) {
        FriendListRecyclerViewAdapter.clickListener = clickListener;
    }

    public interface ClickListener {
        void onItemClick(int position, View v);
    }
}
