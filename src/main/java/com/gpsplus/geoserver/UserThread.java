package com.gpsplus.geoserver;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class UserThread implements Runnable {
    private ObjectInputStream in = null;
    private ObjectOutputStream out = null;
    private volatile Correction correction = null;

    public UserThread(Socket clientSocket) {
        try {
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            in = new ObjectInputStream(clientSocket.getInputStream());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void run() {
        try {

            while (correction == null) { Thread.onSpinWait(); }
            out.writeObject(correction);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Correction getCorrection() {
        return correction;
    }

    public void setCorrection(Correction correction) {
        this.correction = correction;
    }

}
