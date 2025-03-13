package com.bailout.stickk.ubi4.utility;

import java .util.ArrayList;

public class BlockingQueueUbi4 {
    ArrayList<Runnable> tasks = new ArrayList<>();
    private boolean canTake = true; // Флаг, разрешающий извлечение задачи
    private long lastAllowTime = 0; // Время последнего события dataReceive

    public synchronized Runnable get() {
        while (tasks.isEmpty() || !canTake) {
            try {
                if (!tasks.isEmpty() && !canTake) {
                    // Проверяем, прошла ли секунда с последнего dataReceive
                    long elapsed = System.currentTimeMillis() - lastAllowTime;
                    if (elapsed >= 1000) {
                        canTake = true; // Автоматическая разблокировка
                    } else {
                        // Ждём оставшееся время до секунды
                        wait(1000 - elapsed);
                    }
                } else {
                    wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Runnable task = tasks.get(0);
        canTake = false;
        tasks.remove(task);
        return task;
    }

    public synchronized void put(Runnable task, byte[] byteArray) {
        tasks.add(task);
        StringBuilder byteArrayS = new StringBuilder();
        for (byte b : byteArray) {
            byteArrayS.append(" "+b);
        }
        notify();
    }

    public synchronized int size() {
        return tasks.size();
    }

    public synchronized void allowNext() {
        canTake = true;
        lastAllowTime = System.currentTimeMillis(); // Фиксируем время события
        notify(); // Разрешаем извлечение следующей задачи
    }
}

