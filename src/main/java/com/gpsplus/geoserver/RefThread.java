package com.gpsplus.geoserver;

import com.gpsplus.georef.LocationDTO;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.Callable;

public class RefThread implements Callable<MyLocation> {
    private ObjectInputStream in = null;
    private ObjectOutputStream out = null;
    private Socket socket;
    private MyLocation trueLocation = null;
    private static int idCounter = 0;
    private int refId;

    public RefThread(Socket clientSocket) {
        try {
            socket = clientSocket;
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            trueLocation = new MyLocation((LocationDTO) in.readObject(), refId);

            refId = idCounter++;
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public int getRefId() {
        return refId;
    }

    @Override
    public MyLocation call() {

        MyLocation loc = null;
        try {
            while (null == loc) {
                loc = new MyLocation((LocationDTO) in.readObject(), refId);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("RefThread call: " + e.getMessage());
        }
//        if (null == trueLocation) {
//            trueLocation = loc;
//        }
        return loc;
    }

    public MyLocation getTrueLocation() {
        return trueLocation;
    }

    @Override
    public String toString() {
        return super.toString();
    }


}
