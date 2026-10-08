package dbres;

public class DBRes {
    public boolean ok;
    public String error;

    public DBRes(boolean ok) {
        this.ok = ok;
        this.error = null;
    }

    public DBRes(boolean ok, String error) {
        this.ok = ok;
        this.error = error;
    }
}
