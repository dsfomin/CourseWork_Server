package com.gpsplus.geoserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerSocketTask {
    private static final ArrayList<ClientHandlerTask> clients = new ArrayList<>();
    private static final ExecutorService pool = Executors.newFixedThreadPool(4);


    public static void main(String[] args) throws IOException {
        ServerSocket refListener = new ServerSocket(9092);
        //ServerSocket userListener = new ServerSocket(9093);
        while (true) {
            System.out.println("Server waits for ref connection...");
            Socket client = refListener.accept();
            System.out.println("ref connected");
            ClientHandlerTask clientThread = new ClientHandlerTask(client);
            clients.add(clientThread);
            pool.execute(clientThread);
            clients.forEach(System.out::println);
        }
    }
}
