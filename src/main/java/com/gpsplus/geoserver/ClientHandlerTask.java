package com.gpsplus.geoserver;

import com.gpsplus.georef.LocationDTO;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandlerTask implements Runnable {
    private Socket client = null;
    private ObjectInputStream in = null;
    private ObjectOutputStream out = null;

    public ClientHandlerTask(Socket clientSocket) {
        try {
            client = clientSocket;
            out = new ObjectOutputStream(client.getOutputStream());
            in = new ObjectInputStream(client.getInputStream());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void run() {
        try {

            while (!Thread.currentThread().isInterrupted()) {
                LocationDTO loc = (LocationDTO) in.readObject();
                MyLocation myLocation = new MyLocation(loc);
                System.out.println("Client : " + loc);
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
