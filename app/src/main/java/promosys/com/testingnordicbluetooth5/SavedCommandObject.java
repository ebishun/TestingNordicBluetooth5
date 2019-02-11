package promosys.com.testingnordicbluetooth5;

public class SavedCommandObject {

    private int intCommand;
    private String strCommand;

    public SavedCommandObject(){}

    public SavedCommandObject(int intCommand, String strCommand){
        this.intCommand = intCommand;
        this.strCommand = strCommand;
    }

    public int getIntCommand() {
        return intCommand;
    }

    public void setIntCommand(int intCommand) {
        this.intCommand = intCommand;
    }

    public String getStrCommand() {
        return strCommand;
    }

    public void setStrCommand(String strCommand) {
        this.strCommand = strCommand;
    }
}
