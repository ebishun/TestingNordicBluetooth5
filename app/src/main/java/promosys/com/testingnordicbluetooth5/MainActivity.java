package promosys.com.testingnordicbluetooth5;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static java.lang.reflect.Array.getLength;
import static promosys.com.testingnordicbluetooth5.MyBleService.mBroadcastBleConnected;
import static promosys.com.testingnordicbluetooth5.MyBleService.mBroadcastBleDeviceFound;
import static promosys.com.testingnordicbluetooth5.MyBleService.mBroadcastBleDeviceNotFound;
import static promosys.com.testingnordicbluetooth5.MyBleService.mBroadcastBleDisconnected;
import static promosys.com.testingnordicbluetooth5.MyBleService.mBroadcastBleGotReply;
import static promosys.com.testingnordicbluetooth5.MyBleService.mBroadcastBleOff;
import static promosys.com.testingnordicbluetooth5.MyBleService.mBroadcastCheckAlive;
import static promosys.com.testingnordicbluetooth5.MyBleService.mBroadcastConnectionEstablished;
import static promosys.com.testingnordicbluetooth5.MyBleService.mBroadcastFailedCharacteristics;
import static promosys.com.testingnordicbluetooth5.MyBleService.mBroadcastStopScanning;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.support.v7.widget.Toolbar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private IntentFilter mIntentFilter;

    private MyBleService mBleService;
    private boolean mIsBound = false;

    public MyRefreshTimer refreshTimer;
    char ETX = (char) 0x03;

    private boolean isRequestDisconnect = false;
    private boolean isBleConnected = false;

    private boolean isTimerBleReply = false;
    private int intTimerCounter = 0;

    private long timeElapsed;
    private long startTime = 30000;
    private long interval = 1000;

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 20;

    private GoogleApiClient mGoogleApiClient;

    private FragmentTransaction fragmentTransaction;
    private FragmentScanning fragmentScanning;
    private FragmentConnected fragmentConnected;
    private FragmentSettings fragmentSettings;

    private Toolbar toolbar;

    private boolean isFragmentScanning;
    private boolean isFragmentConnected;
    private boolean isFragmentSettings;

    public String strBleMessage = "";

    public boolean isTimerRunning = false;

    public String strBluetoothName = "";

    public SharedPreferences sharedPreferences;
    public SharedPreferences.Editor editor;

    public int timerDuration = 0;
    public boolean isIncludeCrcLength = false;
    public boolean isTimerEnable = false;

    Dialog initializingDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences(getResources().getString(R.string.shared_preference), Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitleTextColor(Color.WHITE);
        toolbar.setSubtitleTextColor(Color.WHITE);
        getSupportActionBar().hide();


        initIntentFilter();
        initPermission();
        initFragment();
    }

    public void initTimer(){
        if(refreshTimer != null){
            refreshTimer.cancel();
            isTimerRunning = false;
        }

        if(isTimerEnable){
            startTime = timerDuration;
            Log.i("MainActivity","startTime: "+ startTime);

            refreshTimer = new MyRefreshTimer(startTime, interval);
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i("Location", "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    @Override
    public void onConnected(Bundle arg0) {
        // Once connected with google api, get the location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.i("Location", "No permission given");
        } else {
            LocationRequest mLocationRequest = new LocationRequest();
            mLocationRequest.setInterval(10000);
            mLocationRequest.setFastestInterval(5000);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
            PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());

        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mIntentFilter);
        if(mBleService == null){
            Log.i("MainActivity","onResume: Service is dead");
        }
    }

    @Override
    public void onBackPressed() {
        if(isFragmentSettings){
            changeFragment("fragmentConnected");
        }
    }

    @Override
    protected void onDestroy() {
        Log.i("MainActivity","MainActivity Destroy");
        try {
            refreshTimer.cancel();
            mBleService.disconnectDeviceSelected();
        }catch (Exception e){
            Log.i("DisconnectDevice","Error: " + e.toString());
        }

        doUnbindService();
        if (isMyServiceRunning(MyBleService.class)){
            stopService(new Intent(this, MyBleService.class));
        }
        unregisterReceiver(mReceiver);

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id){

            case R.id.action_clear_terminal:
                fragmentConnected.clearTerminal();
                break;

            case R.id.action_save_log:
                fragmentConnected.saveToLog();
                break;

            case R.id.action_settings:
                changeFragment("fragmentSettings");
                break;

            case R.id.action_disconnect:
                if(isBleConnected){
                    mBleService.disconnectDeviceSelected();
                }
                break;

        }

        return super.onOptionsItemSelected(item);
    }

    private void initIntentFilter() {
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(mBroadcastBleOff);
        mIntentFilter.addAction(mBroadcastBleConnected);
        mIntentFilter.addAction(mBroadcastBleDisconnected);
        mIntentFilter.addAction(mBroadcastBleGotReply);
        mIntentFilter.addAction(mBroadcastBleDeviceNotFound);
        mIntentFilter.addAction(mBroadcastConnectionEstablished);
        mIntentFilter.addAction(mBroadcastFailedCharacteristics);
        mIntentFilter.addAction(mBroadcastCheckAlive);
        mIntentFilter.addAction(mBroadcastBleDeviceFound);
        mIntentFilter.addAction(mBroadcastStopScanning);
    }

    private void initPermission(){
        // Make sure we have access coarse location enabled, if not, prompt the user to enable it
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && this.checkSelfPermission(ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            {
                initLocation();

            } else {
                requestPermissions(new String[]{ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_FINE_LOCATION);
            }


            if (this.checkSelfPermission(ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect peripherals.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            requestPermissions(new String[]{ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                        }
                    }
                });
                builder.show();

            }else {
                startService();
            }

            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        }else{
            Log.i("MainActivity","isServiceRunning: " +isMyServiceRunning(MyBleService.class));
            startService();
        }

    }

    //Check whether the listener service is running or not in the background
    //If the service not running, the apps will restart the service on launch
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void startService(){
        Log.i("MainActivity","isMyServiceRunning: " + isMyServiceRunning(MyBleService.class));
        if(!(isMyServiceRunning(MyBleService.class))){
            startService(new Intent(this, MyBleService.class));
            doBindService();
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mBleService = ((MyBleService.LocalBinder)service).getService();
        }
        public void onServiceDisconnected(ComponentName className) {
            mBleService = null;
        }
    };

    void doBindService() {
        bindService(new Intent(this, MyBleService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
        Toast.makeText(getApplicationContext(),"Service is connected", Toast.LENGTH_SHORT).show();
    }

    void doUnbindService() {
        if (mIsBound) {
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    private void initLocation() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
        mGoogleApiClient.connect();
    }

    private void initFragment(){
        fragmentTransaction = getFragmentManager().beginTransaction();

        fragmentScanning = new FragmentScanning();
        fragmentConnected = new FragmentConnected();
        fragmentSettings = new FragmentSettings();

        fragmentTransaction.add(R.id.content,fragmentScanning,"fragmentScanning");
        fragmentTransaction.add(R.id.content,fragmentConnected,"fragmentConnected");
        fragmentTransaction.add(R.id.content,fragmentSettings,"fragmentSettings");
        fragmentTransaction.hide(fragmentConnected);
        fragmentTransaction.hide(fragmentSettings);

        isFragmentScanning = true;

        fragmentTransaction.commitAllowingStateLoss();

    }

    private void changeFragment(String whichFragment){
        fragmentTransaction = getFragmentManager().beginTransaction();
        switch (whichFragment){
            case "fragmentScanning":
                getSupportActionBar().hide();
                mBleService.SCANNED_MAC_ADDRESS = "";
                fragmentScanning.hideDevicesList();
                //fragmentTransaction.setCustomAnimations(R.animator.slide_in_right, R.animator.slide_out_left);
                isFragmentConnected = false;
                isFragmentSettings = false;
                isFragmentScanning = true;

                fragmentTransaction.hide(fragmentConnected);
                fragmentTransaction.hide(fragmentSettings);
                fragmentTransaction.show(fragmentScanning);

                fragmentConnected.clearTerminal();
                break;

            case "fragmentConnected":
                getSupportActionBar().show();
                toolbar.setTitle(strBluetoothName);
                //fragmentTransaction.setCustomAnimations(R.animator.slide_in_right, R.animator.slide_out_left);
                isFragmentConnected = true;
                isFragmentSettings = false;
                isFragmentScanning = false;

                fragmentTransaction.hide(fragmentScanning);
                fragmentTransaction.hide(fragmentSettings);
                fragmentTransaction.show(fragmentConnected);
                break;

            case "fragmentSettings":
                if(refreshTimer!=null){
                    refreshTimer.cancel();
                }

                getSupportActionBar().show();
                isFragmentConnected = false;
                isFragmentSettings = true;
                isFragmentScanning = false;

                fragmentTransaction.hide(fragmentConnected);
                fragmentTransaction.hide(fragmentScanning);
                fragmentTransaction.show(fragmentSettings);
                break;
        }

        fragmentTransaction.disallowAddToBackStack();
        fragmentTransaction.commitAllowingStateLoss();
    }


    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (Objects.requireNonNull(intent.getAction())){
                case mBroadcastBleConnected:
                    Log.i("MainActivity","connected");
                    changeFragment("fragmentConnected");
                    isBleConnected = true;

                    if(initializingDialog != null){
                        initializingDialog.dismiss();
                    }
                    break;

                case mBroadcastBleDisconnected:
                    isBleConnected = false;
                    changeFragment("fragmentScanning");
                    if(refreshTimer != null){
                        refreshTimer.cancel();
                    }
                    isTimerRunning = false;
                    Log.i("MainActivity","disconnect");
                    break;

                case mBroadcastBleGotReply:
                    Log.i("MainActivity","msgFromBluetooth: " + intent.getStringExtra("bleMessage"));
                    fragmentConnected.displayOnScreen(intent.getStringExtra("bleMessage"));
                    break;

                case mBroadcastBleDeviceNotFound:
                    mBleService.disconnectDeviceSelected();
                    if(initializingDialog != null){
                        initializingDialog.dismiss();
                    }
                    fragmentScanning.bleList.clear();
                    fragmentScanning.refreshList();
                    fragmentScanning.hideDevicesList();

                    break;

                case mBroadcastFailedCharacteristics:
                    Log.i("MainActivity","failed to write characteristics");
                    if(refreshTimer != null){
                        refreshTimer.cancel();
                    }
                    isTimerRunning = false;
                    break;

                case mBroadcastCheckAlive:
                    break;

                case mBroadcastBleOff:
                    break;

                case mBroadcastStopScanning:
                    fragmentScanning.btnScan.setEnabled(true);
                    fragmentScanning.btnScan.setBackgroundTintList(getResources().getColorStateList(R.color.colorAccent));
                    fragmentScanning.progressBar.setVisibility(View.INVISIBLE);
                    break;

                case mBroadcastBleDeviceFound:
                    fragmentScanning.bleList.clear();
                    for (int i = 0;i<mBleService.devicesDiscovered.size();i++){
                        String bleName = mBleService.devicesDiscovered.get(i).getName();
                        String bleAddress = mBleService.devicesDiscovered.get(i).getAddress();

                        BluetoothObject bleObject = new BluetoothObject(bleName,bleAddress);
                        fragmentScanning.bleList.add(bleObject);
                    }
                    fragmentScanning.refreshList();
                    break;
            }

        }
    };

    //call from btnScan from FragmentScanning
    public void startScanningBle(){
        mBleService.isDirectConnect = false;
        mBleService.startScanning();
    }

    public void checkIfTimerInitialized(){
        if(refreshTimer == null){

        }
    }

    public void connectToBle(int position){
        try{
            mBleService.stopScanning();

            if(!mBleService.btScanning){
                String bleAddress = fragmentScanning.bleList.get(position).getBleAddress();

                if(mBleService.devicesDiscovered.get(position).getAddress().equals(bleAddress)){
                    //mBleService.connectToDeviceSelected(position);
                    Log.i("MainActivity","bleName: " + mBleService.devicesDiscovered.get(position).getName());
                    Log.i("MainActivity","bleAddress: " + mBleService.devicesDiscovered.get(position).getAddress());
                    strBluetoothName = mBleService.devicesDiscovered.get(position).getName();
                    mBleService.connectToDeviceSelected(position);
                }
            }
            displayInitializingDialog();
        }catch (IndexOutOfBoundsException exception){
            Log.e("MainActivity","exception: " + exception);
        }
    }

    public void directConnect(String bleName){
        mBleService.isDirectConnect = true;
        mBleService.strDirectConnect = bleName;

        if(mBleService.btScanning){
            mBleService.stopScanning();
        }

        mBleService.startScanning();
        displayInitializingDialog();
    }

    public void sendMessageToBle(){
        if(isIncludeCrcLength){
            mBleService.writeCustomCharacteristic(buildStringToDevice(strBleMessage));
            fragmentConnected.displaySendCommand(buildStringToDevice(strBleMessage));
        }else {
            /*
            String hexLength = getLength2(strBleMessage);
            Log.i("MainActivity","hexLength: " + hexLength);
            String test = hexLength+strBleMessage;
            mBleService.writeCustomCharacteristic(test);
            */
            mBleService.writeCustomCharacteristic(strBleMessage);
            fragmentConnected.displaySendCommand(strBleMessage);
        }

        if(isTimerEnable){
            if(!isTimerRunning){
                refreshTimer.start();
                isTimerRunning = true;
            }
        }

    }

    private void displayInitializingDialog(){
        initializingDialog = new Dialog(this);
        initializingDialog.setContentView(R.layout.dialog_initializing);
        initializingDialog.setCanceledOnTouchOutside(false);
        initializingDialog.show();
    }

    public void writeToFile(String data,Context context) {
        File folder = new File(Environment.getExternalStorageDirectory() +
                File.separator + getResources().getString(R.string.app_name));

        boolean success = true;
        if (!folder.exists()) {
            success = folder.mkdirs();
        }
        String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        String filePath = baseDir + File.separator + getResources().getString(R.string.app_name) + File.separator + "ble_terminal_log.txt";
        File f = new File(filePath);

        try {
            FileOutputStream stream = new FileOutputStream(f);
            try {
                stream.write(data.getBytes());
            } finally {
                stream.close();
                Toast.makeText(getApplicationContext(),"Log saved",Toast.LENGTH_SHORT).show();
            }
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    private String buildStringToDevice(String strCommand){
        String finalString = "";
        String length = "$" + getLength(strCommand);
        String strGetCrc = length + strCommand;
        String strCrc = ModRTU_CRC(strGetCrc.getBytes());

        finalString = strGetCrc + strCrc + ETX;
        //Log.i("MainActivity","finalString: " + finalString);

        return finalString;
    }

    private String getLength(String strLength){
        String hexLength = "";
        hexLength = String.format("%04X", strLength.length());
        return hexLength;
    }

    private String getLength2(String strLength){
        String hexLength = "";
        hexLength = String.format("%02X", strLength.length());
        return hexLength;
    }

    private static String ModRTU_CRC(byte[] buf)
    {
        int crc = 0xFFFF;
        for (int pos = 0; pos < buf.length; pos++) {
            crc ^= (int)buf[pos] & 0xFF;   // XOR byte into least sig. byte of crc
            for (int i = 8; i != 0; i--) {    // Loop over each bit
                if ((crc & 0x0001) != 0) {      // If the LSB is set
                    crc >>= 1;                    // Shift right and XOR 0xA001
                    crc ^= 0xA001;
                }
                else                            // Else LSB is not set
                    crc >>= 1;                    // Just shift right
            }
        }
        return Integer.toHexString(crc);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case PERMISSION_REQUEST_COARSE_LOCATION:
                try {
                    if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                        Log.i("MainActivity", "Permission has been denied by user");
                    } else {
                        Log.i("MainActivity", "Permission has been granted by user");
                        Log.i("MainActivity","isServiceRunning: " +isMyServiceRunning(MyBleService.class));
                        startService();

                    }
                }catch (Exception error){
                    Log.i("MainActivity","permission error: " + error);
                }

                break;

            case PERMISSION_REQUEST_FINE_LOCATION:
                break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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
            sendMessageToBle();
            if(isTimerEnable){
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
