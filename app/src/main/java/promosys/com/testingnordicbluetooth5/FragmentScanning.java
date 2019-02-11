package promosys.com.testingnordicbluetooth5;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import java.util.ArrayList;

public class FragmentScanning extends Fragment {

    private View rootView;
    private Context context;
    private MainActivity mainActivity;

    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager mLayoutManager;
    private BluetoothScanningAdapter bleAdapter;
    public ArrayList<BluetoothObject> bleList;

    private LinearLayout layoutScanList;

    public Button btnScan,btnConnect;
    private EditText edtBleName;

    public ProgressBar progressBar;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_scanning,container,false);
        context = rootView.getContext();
        mainActivity = (MainActivity) context;
        initRecyclerView();

        edtBleName = (EditText)rootView.findViewById(R.id.edt_ble_name);
        progressBar = (ProgressBar)rootView.findViewById(R.id.progress_scanning);

        layoutScanList = (LinearLayout)rootView.findViewById(R.id.layout_scan_list);
        layoutScanList.setVisibility(View.INVISIBLE);

        btnScan = (Button)rootView.findViewById(R.id.btn_ble_scan);
        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                btnScan.setEnabled(false);
                btnScan.setBackgroundTintList(getResources().getColorStateList(R.color.colorDisabled));

                mainActivity.startScanningBle();
                bleList.clear();
                bleAdapter.notifyDataSetChanged();
                layoutScanList.setVisibility(View.VISIBLE);
                recyclerView.setEnabled(true);

                progressBar.setVisibility(View.VISIBLE);
            }
        });

        btnConnect = (Button)rootView.findViewById(R.id.btn_ble_connect);
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mainActivity.strBluetoothName = edtBleName.getText().toString();
                mainActivity.directConnect(edtBleName.getText().toString());
            }
        });

        return rootView;
    }

    private void initRecyclerView(){
        recyclerView = (RecyclerView)rootView.findViewById(R.id.ble_recvw);
        recyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(mLayoutManager);
        bleList = new ArrayList<BluetoothObject>();

        bleAdapter = new BluetoothScanningAdapter(bleList);
        recyclerView.setAdapter(bleAdapter);

        bleAdapter.setOnItemClickListener(new BluetoothScanningAdapter.MyClickListener() {
            @Override
            public void onItemClick(int position, View v) {
                mainActivity.connectToBle(position);
            }
        });
    }

    public void refreshList(){
        bleAdapter.notifyDataSetChanged();
    }

    public void hideDevicesList(){
        layoutScanList.setVisibility(View.INVISIBLE);
        recyclerView.setEnabled(false);
    }

}
