package dbres;

import models.Animation;

public class DBSingleAnimation extends DBRes {
    public Animation animation;

    public DBSingleAnimation(boolean ok, String error, Animation animation) {
        super(ok, error);
        this.animation = animation;
    }
}
