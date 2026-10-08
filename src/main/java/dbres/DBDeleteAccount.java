package dbres;

public class DBDeleteAccount extends DBRes {
    public boolean is_self;

    public DBDeleteAccount(boolean ok, String error, boolean is_self) {
        super(ok, error);
        this.is_self = is_self;
    }
}
