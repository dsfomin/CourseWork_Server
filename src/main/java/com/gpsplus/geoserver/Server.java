package com.gpsplus.geoserver;

import javax.management.InstanceNotFoundException;
import java.io.IOException;
import java.net.ServerSocket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.InputMismatchException;
import java.util.List;
import java.util.concurrent.*;

public class Server {
    private static final ArrayList<RefThread> refs = new ArrayList<>();
    private static final ArrayList<UserThread> users = new ArrayList<>();
    private static final int REF_PORT = 9092;
    private static final int USER_PORT = 9093;
    private static final ExecutorService refExecutorService = Executors.newFixedThreadPool(4);
    private static final ExecutorService userExecutorService = Executors.newFixedThreadPool(4);

    public static void main(String[] args) throws IOException, InterruptedException, InstanceNotFoundException {
        ServerSocket refServerSocket = new ServerSocket(REF_PORT);
        ServerSocket userServerSocket = new ServerSocket(USER_PORT);

        RefListener refListener = new RefListener(refs, refServerSocket);

        new Thread(refListener).start();
        new Thread(new UserListener(users, userServerSocket)).start();

        while (!refListener.refsIsFull()) {
            System.out.println("Not enough refs yet");
            refs.stream().map(RefThread::getTrueLocation).forEach(System.out::println);
            Thread.sleep(5000);
            // TODO: send users  'not enough refs'
        }

        System.out.println("Got 4 refs: " + refs.size());

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        BlockingQueue<MyLocation> queue = new ArrayBlockingQueue<>(4);

        scheduler.scheduleWithFixedDelay(() -> refs.forEach(e -> {
            try {
                queue.put(refExecutorService.submit(e).get());
            } catch (InterruptedException | ExecutionException interruptedException) {
                interruptedException.printStackTrace();
            }
        }), 0, 5, TimeUnit.SECONDS);

        //noinspection InfiniteLoopStatement
        while (true) {
            if (queue.size() == 4) {
                queue.forEach(System.out::println);
                System.out.println(LocalDateTime.now());

                Correction correction = computeCorrection(List.copyOf(queue));
                users.forEach(u -> u.setCorrection(correction));
                System.out.println("Try to send data " + correction);
                users.forEach(userExecutorService::execute);

                queue.clear();
            }
        }
    }

    private static Correction computeCorrection(List<MyLocation> locationList) throws InstanceNotFoundException {
        List<Correction> correctionList = new ArrayList<>();

        for (MyLocation loc : locationList) {
            MyLocation trueLocation = refs.stream()
                    .filter(e -> e.getRefId() == loc.getRefId())
                    .findFirst()
                    .orElseThrow(InstanceNotFoundException::new)
                    .getTrueLocation();

            System.out.println(trueLocation.distanceTo(loc));

            correctionList.add(new Correction(trueLocation.getLongitude() - loc.getLongitude(),
                    trueLocation.getLatitude() - loc.getLatitude(),
                    trueLocation.getAltitude() - loc.getAltitude()));
        }
        Correction average = findAverage(correctionList);
        System.out.println(average);
        return average;
    }

    private static Correction findAverage(List<Correction> corrections) {
        double longitude = corrections.stream().mapToDouble(Correction::getLongitude).average().orElseThrow(EmptyStackException::new);
        double latitude = corrections.stream().mapToDouble(Correction::getLatitude).average().orElseThrow(EmptyStackException::new);
        double altitude = corrections.stream().mapToDouble(Correction::getAltitude).average().orElseThrow(EmptyStackException::new);

        return new Correction(longitude, latitude, altitude);
    }
}
