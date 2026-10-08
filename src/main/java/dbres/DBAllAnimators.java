package dbres;

import models.Animator;

import java.util.List;

public class DBAllAnimators extends DBRes {
    public List<Animator> animators;

    public DBAllAnimators(boolean ok, String error, List<Animator> animators) {
        super(ok, error);
        this.animators = animators;
    }
}
