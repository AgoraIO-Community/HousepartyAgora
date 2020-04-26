package com.example.housepartyagora.model;

import java.util.List;

public class DBUser {
    String name;
    String state;
    List<String> friend;
    int uid;

    DBUser () {

    }

    public DBUser(String name, int uid, String state, List<String> friend) {
        this.name = name;
        this.uid = uid;
        this.state = state;
        this.friend = friend;
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public List<String> getFriend() {
        return friend;
    }

    public void setFriend(List<String> friend) {
        this.friend = friend;
    }
}
