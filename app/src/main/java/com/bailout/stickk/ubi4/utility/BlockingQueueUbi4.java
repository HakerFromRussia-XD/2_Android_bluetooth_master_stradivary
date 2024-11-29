package com.bailout.stickk.ubi4.utility;



import java.util.ArrayList;

public class BlockingQueueUbi4 {
    ArrayList<Runnable> tasks = new ArrayList<>();

    public synchronized Runnable get() {
        while (tasks.isEmpty()) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Runnable task = tasks.get(0);
        tasks.remove(task);
        return task;
    }

    public synchronized void put(Runnable task) {
        tasks.add(task);
        notify();
    }

    public synchronized int size() {
        return tasks.size();
    }
}

