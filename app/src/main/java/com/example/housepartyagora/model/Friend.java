package com.example.housepartyagora.model;

public class Friend {
    private int agoraUid;
    private String userName;

    public Friend() {

    }

    public Friend(String userName) {
        this.userName = userName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public int getAgoraUid() {
        return agoraUid;
    }

    public void setAgoraUid(int agoraUid) {
        this.agoraUid = agoraUid;
    }
}
