package com.RSD.pong.utils;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.RSD.pong.R;
import com.RSD.pong.models.ChatItem;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private final List<ChatItem> chatList;

    public ChatAdapter(List<ChatItem> chatList) {
        this.chatList = chatList;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatItem chat = chatList.get(position);
        holder.chatAvatar.setImageResource(chat.avatarResId);
        holder.chatName.setText(chat.name);
        holder.chatLastMessage.setText(chat.lastMessage);

        // making color alternation
        boolean isEven = position % 2 == 0;

        int attrToLoad = isEven ? R.attr.colorPrimaryVariant : R.attr.colorSecondaryVariant;

        int[] attrs = new int[]{attrToLoad};
        android.content.res.TypedArray ta =
                holder.itemView.getContext().obtainStyledAttributes(attrs);

        int color = ta.getColor(0, 0);
        ta.recycle();

        holder.itemView.setBackgroundColor(color);
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        ImageView chatAvatar;
        TextView chatName;
        TextView chatLastMessage;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            chatAvatar = itemView.findViewById(R.id.chatAvatar);
            chatName = itemView.findViewById(R.id.chatName);
            chatLastMessage = itemView.findViewById(R.id.chatLastMessage);
        }
    }
}