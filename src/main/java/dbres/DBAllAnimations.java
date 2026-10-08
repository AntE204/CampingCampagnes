package dbres;

import models.Animation;

import java.util.List;

public class DBAllAnimations extends DBRes {
    public List<Animation> animations;

    public DBAllAnimations(boolean ok, String error, List<Animation> animations) {
        super(ok, error);
        this.animations = animations;
    }
}
