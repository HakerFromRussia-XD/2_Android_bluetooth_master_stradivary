package me.aflak.libraries.data;

public class Gesture_my {
    private int number, image;
    private String name, info, title;
    private double rating, prise;

    public Gesture_my(int number, int image, String name, String info, String title, double rating, double prise) {
        this.number = number;
        this.image = image;
        this.name = name;
        this.info = info;
        this.title = title;
        this.rating = rating;
        this.prise = prise;
    }

    public int getNumber() {
        return number;
    }

    public int getImage() {
        return image;
    }

    public String getName() {
        return name;
    }

    public String getInfo() {
        return info;
    }

    public String getTitle() {
        return title;
    }

    public double getRating() {
        return rating;
    }

    public double getPrise() {
        return prise;
    }
}
