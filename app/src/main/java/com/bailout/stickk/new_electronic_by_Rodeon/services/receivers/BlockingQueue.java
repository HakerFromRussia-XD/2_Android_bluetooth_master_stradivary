package com.bailout.stickk.new_electronic_by_Rodeon.services.receivers;

import java.util.ArrayList;

public class BlockingQueue {
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
        tasks.add (task);
        notify();
    }
}
