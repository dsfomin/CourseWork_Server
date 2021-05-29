package com.gpsplus.geoserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class UserListener implements Runnable{
    private final ArrayList<UserThread> users;
    private final ServerSocket userListener;

    public UserListener(ArrayList<UserThread> users, ServerSocket userListener) {
        this.users = users;
        this.userListener = userListener;
    }

    @Override
    public void run() {
        //noinspection InfiniteLoopStatement
        while (true) {
            System.out.println("Server waits for user connection...");
            Socket user = null;
            try {
                user = userListener.accept();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("User connected");
            UserThread userThread = new UserThread(user);
            users.add(userThread);
            users.forEach(System.out::println);
        }
    }
}
