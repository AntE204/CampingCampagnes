package models;

public class Animation {
    public String name;
    public int length_minutes;

    public Animation(String name, int length_minutes) {
        this.name = name;
        this.length_minutes = length_minutes;
    }

    public String get_info() {
        return this.name + " (" + this.length_minutes + "min)";
    }
}
