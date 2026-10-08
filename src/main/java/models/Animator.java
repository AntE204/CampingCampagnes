package models;

public class Animator {
    public int id;
    public String email, first_name, last_name, street, town, zip_code, phone;
    public boolean is_director;

    public Animator(int id, String email, String first_name, String last_name, String street, String town, String zip_code, String phone, boolean is_director) {
        this.id = id;
        this.email = email;
        this.first_name = first_name;
        this.last_name = last_name;
        this.street = street;
        this.town = town;
        this.zip_code = zip_code;
        this.phone = phone;
        this.is_director = is_director;
    }

    public String get_info() {
        return this.email + " - " + this.first_name + " " + this.last_name;
    }
}
