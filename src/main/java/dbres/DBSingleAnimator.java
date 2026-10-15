package dbres;

import models.Animator;

public class DBSingleAnimator extends DBRes{
    public Animator animator;
    public DBSingleAnimator(boolean ok, String error, Animator anim){
        super(ok, error);
        this.animator=anim;
    }
}
