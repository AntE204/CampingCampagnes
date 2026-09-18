package navigation;
import models.TitleDesc;

public class Choice extends TitleDesc {
    private boolean state;

    public Choice(String title, String desc, boolean state) {
        super(title, desc);
        this.state = state;
    }

    public boolean get_state() {
        return this.state;
    }

    public void set_state(boolean state) {
        this.state = state;
    }
}
