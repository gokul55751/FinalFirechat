package com.example.finalfirechat.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.finalfirechat.model.Message;
import com.example.finalfirechat.model.ReceiveMessage;
import com.example.finalfirechat.model.User;

import java.util.ArrayList;

public class DatabaseHelper extends SQLiteOpenHelper {
    public DatabaseHelper(Context context) {
        super(context, "firechat", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    public ArrayList<User> fetchAllUser(){
        ArrayList<User> userArrayList =  new ArrayList<>();
        return userArrayList;
    }

    public User getUser(String number) {
        return null;
    }

    public void addUser(User user) {

    }

    public void addPendingMessage(ReceiveMessage receiveMessage) {

    }

    public ArrayList<Message> fetchChat(String receiveId) {
        ArrayList<Message> messageArrayList = new ArrayList<>();
        return messageArrayList;
    }

    public void addChat(String receiveId, Message message) {

    }

    public void addValues(String name, ContentValues contentValues) {

    }

    public ArrayList<ReceiveMessage> fetchAllPendingMessage() {
        return null;
    }

    public void deleteAllPendingMessages() {

    }
}
