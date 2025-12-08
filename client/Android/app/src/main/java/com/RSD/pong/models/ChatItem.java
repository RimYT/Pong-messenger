package com.RSD.pong.models;

public class ChatItem {
    public int avatarResId;
    public String name;
    public String lastMessage;

    public ChatItem(int avatarResId, String name, String lastMessage) {
        this.avatarResId = avatarResId;
        this.name = name;
        this.lastMessage = lastMessage;
    }
}