package com.gpsplus.geoserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class RefListener implements Runnable{
    private final ArrayList<RefThread> refs;
    private final ServerSocket refListener;

    public RefListener(ArrayList<RefThread> refs, ServerSocket refListener) {
        this.refs = refs;
        this.refListener = refListener;
    }

    @Override
    public void run() {
        //noinspection InfiniteLoopStatement
        while (true) {
            while (!refsIsFull()) {
                System.out.println("Server waits for ref connection...");
                Socket ref = null;
                try {
                    ref = refListener.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("Ref connected");
                RefThread refThread = new RefThread(ref);
                refs.add(refThread);
                refs.forEach(System.out::println);
            }
        }
    }

    boolean refsIsFull() {
        return refs.size() == 4;
    }
}
