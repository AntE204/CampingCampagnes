package models;

public class Animation {
    public int id;
    public String name;
    public int length_minutes;

    public Animation(int id, String name, int length_minutes) {
        this.id = id;
        this.name = name;
        this.length_minutes = length_minutes;
    }

    public String get_info() {
        return this.name + " (" + this.length_minutes + "min)";
    }
}
