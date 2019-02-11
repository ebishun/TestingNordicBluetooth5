package promosys.com.testingnordicbluetooth5;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class BluetoothScanningAdapter extends RecyclerView.Adapter<BluetoothScanningAdapter.MyViewHolder> {

    private ArrayList<BluetoothObject> bleList;
    private static MyClickListener myClickListener;
    private boolean isItemClicked = false;
    private int itemClickedPosition = 0;

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        private TextView txtBluetoothName,txtBluetoothAddress;

        public MyViewHolder(View view) {
            super(view);
            txtBluetoothName = (TextView)view.findViewById(R.id.txt_settings_title);
            txtBluetoothAddress = (TextView)view.findViewById(R.id.txt_ble_address);

            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            myClickListener.onItemClick(getAdapterPosition(),view);
        }
    }

    public void setOnItemClickListener(MyClickListener myClickListener) {
        this.myClickListener = myClickListener;
    }

    public BluetoothScanningAdapter(ArrayList<BluetoothObject> bleList) {
        this.bleList = bleList;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.settings_card_layout, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, final int position) {
        final BluetoothObject bluetoothObject = bleList.get(position);
        holder.txtBluetoothName.setText(bluetoothObject.getBleName());
        holder.txtBluetoothAddress.setText(bluetoothObject.getBleAddress());

    }

    public void itemClicked(int position){
        isItemClicked = true;
        itemClickedPosition = position;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return bleList.size();
    }

    public void removeItem(int position){
        bleList.remove(position);
        notifyDataSetChanged();
    }

    public interface MyClickListener {
        public void onItemClick(int position, View v);
    }

}
