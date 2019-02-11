package promosys.com.testingnordicbluetooth5;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MyBleService extends Service {

    BluetoothManager btManager;
    BluetoothAdapter btAdapter;
    BluetoothLeScanner btScanner;

    public static final String mBroadcastBleOff = "promosys.com.unarvuterminal.bleoff";
    public static final String mBroadcastBleConnected = "promosys.com.unarvuterminal.bleconnected";
    public static final String mBroadcastBleDisconnected = "promosys.com.unarvuterminal.bledisconnected";
    public static final String mBroadcastBleDeviceFound = "promosys.com.unarvuterminal.blefound";
    public static final String mBroadcastBleDeviceNotFound = "promosys.com.unarvuterminal.blenotfound";
    public static final String mBroadcastBleGotReply = "promosys.com.unarvuterminal.blegotreply";
    public static final String mBroadcastConnectionEstablished = "promosys.com.unarvuterminal.bleestablished";
    public static final String mBroadcastFailedCharacteristics = "promosys.com.unarvuterminal.failedcharacteristics";
    public static final String mBroadcastCheckAlive = "promosys.com.unarvuterminal.checkalive";
    public static final String mBroadcastStopScanning = "promosys.com.unarvuterminal.stopscan";

    Boolean btScanning = false;
    int deviceIndex = 0;
    public ArrayList<BluetoothDevice> devicesDiscovered = new ArrayList<BluetoothDevice>();
    BluetoothGatt bluetoothGatt;
    BluetoothGattCharacteristic sendCharacteristic;
    private String strDevice = "";

    private long timeElapsed;
    private long startTime = 1000;
    private long interval = 1000;

    public MyRefreshTimer refreshTimer;

    char ETX = (char)0x03;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

    public Map<String, String> uuids = new HashMap<String, String>();

    private String CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID = "";
    private String CHARACTERISTIC_WRITE_UUID = "";
    private String SERVICE_WRITE_UUID = "";

    public String SCANNED_MAC_ADDRESS = "";

    // Stops scanning after 5 seconds.
    private Handler mHandler = new Handler();
    private static final long SCAN_PERIOD = 20000;

    // This is the object that receives interactions from clients.
    private final IBinder mBinder = new LocalBinder();
    private StringBuffer strBleBuffer = new StringBuffer();

    public boolean isWaitingReply = false;
    public boolean isSendingPartData = false;
    private boolean isWaitingBleReply = false;

    public boolean isFirstConnected = true;

    public boolean isActivityAlive = false;
    private int serviceId = 0;
    public boolean isBluetoothOnline = false;

    public boolean isScanForNearbyDevice = false;

    public boolean isDirectConnect = false;
    public String strDirectConnect = "";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("MyBleService","Start Service");
        serviceId = startId;
        initBle();

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder {
        MyBleService getService() {
            return MyBleService.this;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void initBle(){
        btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();

        if (btAdapter != null && !btAdapter.isEnabled()) {
            Log.i("BleService","Bluetooth is off");
            sendToMainActivity(mBroadcastBleOff,"","");
        }else{
            refreshTimer = new MyRefreshTimer(startTime, interval);
            refreshTimer.start();
        }

    }

    private void startConnecting(BluetoothDevice device){
        strDevice = device.getName();
        sendToMainActivity(mBroadcastBleDeviceFound,strDevice,"bleName");
        devicesDiscovered.add(device);
        deviceIndex++;

        btScanning = false;
        stopScanning();
        Log.i("MyBleService","devicesDiscovered: " + devicesDiscovered.size());

        if(devicesDiscovered.size() == 1){
            Log.i("MyBleService","Connecting");
            connectToDeviceSelected(0);
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            try {
                if(device.getName() != null){
                    Log.i("MyBleService","deviceName: " + device.getName());
                    Log.i("MyBleService","deviceName: " + device.getAddress());

                    if(isDirectConnect){
                        if(strDirectConnect.equals(device.getName())){
                            devicesDiscovered.add(device);
                            sendToMainActivity(mBroadcastBleDeviceFound,"","");
                            isDirectConnect = false;
                            stopScanning();

                            connectToDeviceSelected(0);
                        }
                    }else {
                        boolean isDuplicate = false;
                        for (int i = 0;i<devicesDiscovered.size();i++){
                            if(devicesDiscovered.get(i).getName().equals(device.getName()) && devicesDiscovered.get(i).getAddress().equals(device.getAddress())){
                                isDuplicate = true;
                            }
                        }

                        if(!isDuplicate){
                            devicesDiscovered.add(device);
                            sendToMainActivity(mBroadcastBleDeviceFound,"","");
                        }
                    }


                }

            }catch (NullPointerException error){ }
        }
    };


    public void startScanning() {
        if (btAdapter != null && !btAdapter.isEnabled()) {
            sendToMainActivity(mBroadcastBleOff,"","");
        }else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                btScanner = btAdapter.getBluetoothLeScanner();
            }

            Log.i("MyBleService","start scanning");
            Log.i("MyBleService","SCANNED_MAC_ADDRESS: " + SCANNED_MAC_ADDRESS);

            btScanning = true;
            deviceIndex = 0;
            devicesDiscovered.clear();
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        btAdapter.startLeScan(mLeScanCallback);
                    }

                }
            });

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                        //sendToMainActivity(mBroadcastBleDeviceFound,"","");
                        if(btScanning){
                            stopScanning();
                            Log.i("MyBleService","stop scanning");
                        }
                }
            }, SCAN_PERIOD);
        }

    }

    public void stopScanning() {
        isScanForNearbyDevice = false;
        btScanning = false;
        sendToMainActivity(mBroadcastStopScanning,"","");
        Log.i("MyBleService","devicesDiscovered.size: " + devicesDiscovered.size());
        if(devicesDiscovered.size() == 0){
            sendToMainActivity(mBroadcastBleDeviceNotFound,"","");
            //isDirectConnect = false;
        }

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    btAdapter.stopLeScan(mLeScanCallback);
                }
            }
        });
    }

    public void connectToDeviceSelected(int deviceIndex) {
        int deviceSelected = deviceIndex;
        bluetoothGatt = devicesDiscovered.get(deviceSelected).connectGatt(this, false, btleGattCallback);
    }

    public void disconnectDeviceSelected() {
        if(bluetoothGatt != null){
            bluetoothGatt.disconnect();
        }
    }


    // Device connect call back
    private final BluetoothGattCallback btleGattCallback = new BluetoothGattCallback() {

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            sendCharacteristic = characteristic;
            super.onCharacteristicChanged(gatt, characteristic);
            byte[] messageBytes = characteristic.getValue();
            String messageString = null;

            try {
                messageString = new String(messageBytes, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                Log.e("MainActivity", "Unable to convert message bytes to string");
            }

            isWaitingBleReply = false;
            isWaitingReply = false;

            sendToMainActivity(mBroadcastBleGotReply,messageString,"bleMessage");

        }

        @Override
        public void onMtuChanged(final BluetoothGatt gatt, final int mtu, final int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("MainActivity", "MTU changed to: " + mtu);
            } else {
                Log.i("MainActivity", "onMtuChanged error: " + status + ", mtu: " + mtu);
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            switch (newState) {
                case 0:
                    sendToMainActivity(mBroadcastBleDisconnected,"","");
                    bluetoothGatt.discoverServices();
                    isFirstConnected = true;
                    bluetoothGatt.close();
                    isBluetoothOnline = false;
                    devicesDiscovered.clear();

                    break;

                case 2:
                    isBluetoothOnline = true;
                    bluetoothGatt.discoverServices();

                    break;
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            displayGattServices(bluetoothGatt.getServices());
        }

        @Override
        // Result of a characteristic read operation
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            Log.i("MainActivity","onCharRead: " + characteristic);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if(isSendingPartData){
                    isWaitingBleReply = false;
                }
            }
            super.onCharacteristicWrite(gatt, characteristic, status);
        }
    };

    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            final String uuid = gattService.getUuid().toString();
            Log.i("MainActivity","Service discovered: " + uuid);

            new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                final String charUuid = gattCharacteristic.getUuid().toString();

                if(gattCharacteristic.getProperties() == 12){
                    CHARACTERISTIC_WRITE_UUID = charUuid;
                    SERVICE_WRITE_UUID = uuid;
                }

                if(gattCharacteristic.getProperties() == 16){
                    for (BluetoothGattDescriptor descriptor:gattCharacteristic.getDescriptors()){
                        CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID = descriptor.getUuid().toString();
                    }
                    setCharacteristicNotification(gattService.getUuid(),gattCharacteristic.getUuid(),true);
                }

            }
        }
    }


    public void setCharacteristicNotification(UUID serviceUuid, UUID characteristicUuid,
                                              boolean enable) {
        BluetoothGattCharacteristic characteristic = bluetoothGatt.getService(serviceUuid).getCharacteristic(characteristicUuid);
        bluetoothGatt.setCharacteristicNotification(characteristic, enable);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID));
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        bluetoothGatt.writeDescriptor(descriptor);

        sendToMainActivity(mBroadcastBleConnected,"","");
    }

    public void sendLongString(String sendString){
        if(!isWaitingReply){
            isWaitingReply = true;
            Log.i("MyBleService","sendString: " + sendString);
            isSendingPartData = true;
            int data_begin = 0;
            int data_end = 15;

            while (isSendingPartData){
                if(!isWaitingBleReply){
                    if(data_end == sendString.length()){
                        String sendData = sendString.substring(data_begin,data_end)+ "\r\n";
                        new LongOperation().execute(sendData);
                        isSendingPartData = false;
                    }else {
                        isWaitingBleReply = true;
                        new LongOperation().execute(sendString.substring(data_begin,data_end));
                    }
                    data_begin = data_end;
                    data_end = data_end + 15;
                    if (data_end > sendString.length()){
                        data_end = sendString.length();
                    }
                }
            }
        }
    }

    private class LongOperation extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

                try {
                    if(isBluetoothOnline){
                        writeCustomCharacteristic(params[0]);
                    }

                } catch (Exception e) {
                    Log.i("MyBleService","exception: " +e);
                }

            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {}
    }

    public void writeCustomCharacteristic(String message) {
        try {
            if (btAdapter == null || bluetoothGatt == null) {
                Log.i("MainActivity", "BluetoothAdapter not initialized");
                //return;
            }

            BluetoothGattService mCustomService = bluetoothGatt.getService(UUID.fromString(SERVICE_WRITE_UUID));
            if(mCustomService == null){
                Log.w("MainActivity", "Custom BLE Service not found");
                stopSelf(serviceId);

                return;
            }else {
                String originalString = message + "\r\n";
                byte[] b = new byte[message.length()];
                try {
                    b = originalString.getBytes("UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                BluetoothGattCharacteristic mWriteCharacteristic = mCustomService.getCharacteristic(UUID.fromString(CHARACTERISTIC_WRITE_UUID));
                mWriteCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                mWriteCharacteristic.setValue(b);
                try {
                    if(bluetoothGatt.writeCharacteristic(mWriteCharacteristic) == false){
                        Log.i("BleService", "Failed to write characteristic");
                        sendToMainActivity(mBroadcastFailedCharacteristics,"","");
                        this.stopSelf(serviceId);
                    }else {
                        if(isFirstConnected){
                            isFirstConnected = false;
                            sendToMainActivity(mBroadcastConnectionEstablished,"","");
                        }
                    }
                }catch (Exception error){
                    Log.i("MyBleService","error: " + error.toString());
                }

            }
        }catch (Exception e){
            Log.w("MyBleService","Exception: " + e);
        }
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        Log.i("MainActivity","got data available");
        try {
            if(characteristic.getValue()!= null){
                byte[] bytes = characteristic.getValue();
                String str = new String(bytes, "UTF-8");
                Log.i("MainActivity","getValue: " + str);
            }
        }catch (NullPointerException error){
            Log.i("MainActivity","Error: " + error);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private void sendToMainActivity(String whichBroadcast,String extra,String extraKey){
        Intent sendMainActivity = new Intent();
        sendMainActivity.setAction(whichBroadcast);
        if(!(extra.isEmpty())){
            sendMainActivity.putExtra(extraKey,extra);
        }
        sendBroadcast(sendMainActivity);

    }

    private void suicide(){
        refreshTimer.cancel();
        Log.i("MainActivity","Suicide");
        //this.stopSelf();
    }

    //auto-refresh timer class
    public class MyRefreshTimer extends CountDownTimer
    {
        public MyRefreshTimer(long startTime, long interval)
        {
            super(startTime, interval);
        }

        @Override
        public void onFinish()
        {
            refreshTimer.cancel();
            isActivityAlive = false;
            sendToMainActivity(mBroadcastCheckAlive,"","");
            Log.i("MainActivity","isAlive: " +  isActivityAlive);
            if(!isActivityAlive){
                suicide();
            }else {
                refreshTimer.start();
            }
        }

        @Override
        public void onTick(long millisUntilFinished)
        {
            timeElapsed = startTime - millisUntilFinished;
        }
    }


}
