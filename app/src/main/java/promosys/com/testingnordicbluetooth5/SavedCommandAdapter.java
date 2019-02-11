package promosys.com.testingnordicbluetooth5;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

public class SavedCommandAdapter extends RecyclerView.Adapter<SavedCommandAdapter.MyViewHolder> {

    private ArrayList<SavedCommandObject> commandList;
    private static MyClickListener siteClickListener;
    private static MyClickListener2 myClickListener2;


    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,View.OnLongClickListener{
        private Button txtCommand;

        public MyViewHolder(View view) {
            super(view);

            txtCommand = (Button) view.findViewById(R.id.btn_command);

            view.setOnClickListener(this);
            view.setOnLongClickListener(this);
        }


        @Override
        public void onClick(View view) {
            siteClickListener.onItemClick(getAdapterPosition(), view);
        }


        @Override
        public boolean onLongClick(View v) {
            myClickListener2.onItemLongClick(getAdapterPosition(), v);
            return false;
        }
    }

    public void setOnItemClickListener(MyClickListener siteClickListener) {
        this.siteClickListener = siteClickListener;
    }

    public void setOnItemLongClickListener(MyClickListener2 myClickListener2) {
        this.myClickListener2 = myClickListener2;
    }

    public SavedCommandAdapter(ArrayList<SavedCommandObject> commandList) {
        this.commandList = commandList;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycler_layout, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, final int position) {
        final SavedCommandObject siteObject = commandList.get(position);

        holder.txtCommand.setText(siteObject.getStrCommand());
    }

    @Override
    public int getItemCount() {
        return commandList.size();
    }


    public void removeItem(int position){
        commandList.remove(position);
        notifyDataSetChanged();

    }

    public interface MyClickListener {
        public void onItemClick(int position, View v);
    }

    public interface MyClickListener2 {
        public void onItemLongClick(int position, View v);
    }

}
