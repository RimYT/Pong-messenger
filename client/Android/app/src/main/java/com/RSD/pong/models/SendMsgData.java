package com.RSD.pong.models;

public class SendMsgData {
    public String recipient_username;
    public String ciphertext;
    public String access_token;

    public SendMsgData(String recipient_username, String ciphertext, String access_token) {
        this.recipient_username = recipient_username;
        this.ciphertext = ciphertext;
        this.access_token = access_token;
    }
}