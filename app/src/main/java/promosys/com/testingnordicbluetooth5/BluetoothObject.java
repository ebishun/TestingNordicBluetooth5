package promosys.com.testingnordicbluetooth5;

public class BluetoothObject {

    private String bleName,bleAddress;

    public BluetoothObject(){}

    public BluetoothObject(String bleName,String bleAddress){
        this.bleName = bleName;
        this.bleAddress = bleAddress;
    }

    public String getBleName() {
        return bleName;
    }

    public void setBleName(String bleName) {
        this.bleName = bleName;
    }

    public String getBleAddress() {
        return bleAddress;
    }

    public void setBleAddress(String bleAddress) {
        this.bleAddress = bleAddress;
    }
}
