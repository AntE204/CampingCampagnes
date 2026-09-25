package models;

public abstract class TitleDesc {
    protected String title, desc;

    public TitleDesc(String title, String desc) {
        this.title = title;
        this.desc = desc;
    }

    public String get_title() {
        return this.title;
    }

    public String get_desc() {
        return this.desc;
    }
}
