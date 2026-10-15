package navigation;
import models.TitleDesc;

public class Choice extends TitleDesc {
    private boolean state;

    public Choice(String title, String desc) {
        super(title, desc);
        this.state = true;
    }

    public boolean get_state() {
        return this.state;
    }
}
