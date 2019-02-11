package promosys.com.testingnordicbluetooth5;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class FragmentConnected extends Fragment {

    private View rootView;
    private Context context;

    private EditText edtCommand;
    private Button btnSendCommand,btnSaveCommand;

    private MainActivity mainActivity;

    public StringBuffer strBuf;
    private TextView txtDisplay;

    private Gson gson;

    private RecyclerView recvwCommand;
    private RecyclerView.LayoutManager mLayoutManager;
    private ArrayList<SavedCommandObject> commandList,tempCommandList;
    public SavedCommandAdapter commandAdapter;

    private ScrollView scrollView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_connected,container,false);
        context = rootView.getContext();

        scrollView = (ScrollView)rootView.findViewById(R.id.scroll_vw);

        mainActivity = (MainActivity) context;

        gson = new Gson();

        txtDisplay = (TextView)rootView.findViewById(R.id.txt_display_command);
        strBuf = new StringBuffer();

        edtCommand = (EditText)rootView.findViewById(R.id.edt_ble_command);
        btnSendCommand = (Button) rootView.findViewById(R.id.btn_send_command);
        btnSendCommand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mainActivity.initTimer();

                String command = edtCommand.getText().toString();
                mainActivity.strBleMessage = command;
                mainActivity.sendMessageToBle();
            }
        });

        btnSaveCommand = (Button)rootView.findViewById(R.id.btn_save_command);
        btnSaveCommand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SavedCommandObject savedCommandObject = new SavedCommandObject(commandList.size()+1,edtCommand.getText().toString());
                commandList.add(savedCommandObject);
                commandAdapter.notifyDataSetChanged();
                saveListToStorage();
            }
        });

        initRecyclerView();

        return rootView;
    }

    public void displayOnScreen(String bleReply){
        String str = getCurrentTime() + " [Reply] : "+bleReply+"\n";
        strBuf.append(str);
        txtDisplay.setText(strBuf.toString());
        scrollView.fullScroll(View.FOCUS_DOWN);
        //mainActivity.writeToFile(str,context);
    }

    public void displaySendCommand(String sendCommand){
        String str = getCurrentTime() + " [Send]  : "+sendCommand+"\n";
        strBuf.append(str);
        txtDisplay.setText(strBuf.toString());
        scrollView.fullScroll(View.FOCUS_DOWN);

    }

    public void saveToLog(){
        mainActivity.writeToFile(strBuf.toString(),context);
    }

    private String getCurrentTime(){
        String strDate = "";
        try
        {
            long date = System.currentTimeMillis();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy  HH:mm:ss");
            strDate = sdf.format(date);

        }catch (Exception e) {}

        return strDate;
    }

    private void initRecyclerView(){
        recvwCommand = (RecyclerView) rootView.findViewById(R.id.recvw_command);
        recvwCommand.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(getActivity());
        recvwCommand.setLayoutManager(mLayoutManager);
        commandList = new ArrayList<SavedCommandObject>();
        tempCommandList = new ArrayList<SavedCommandObject>();
        commandAdapter = new SavedCommandAdapter(commandList);
        recvwCommand.setAdapter(commandAdapter);
        commandAdapter.setOnItemClickListener(new SavedCommandAdapter.MyClickListener() {
            @Override
            public void onItemClick(int position, View v) {
                mainActivity.initTimer();
                mainActivity.strBleMessage = commandList.get(position).getStrCommand();
                edtCommand.setText(commandList.get(position).getStrCommand());
                //displaySendCommand(mainActivity.strBleMessage);

                mainActivity.sendMessageToBle();
            }
        });

        commandAdapter.setOnItemLongClickListener(new SavedCommandAdapter.MyClickListener2() {
            @Override
            public void onItemLongClick(int position, View v) {
                displayCommandDialog(position);
            }
        });
        getListFromStorage();
    }

    private void getListFromStorage(){
        String strHistoryList = mainActivity.sharedPreferences.getString(getResources().getString(R.string.saved_command_key),"");
        if(!(strHistoryList.isEmpty())){
            tempCommandList = new ArrayList<SavedCommandObject>();
            tempCommandList = gson.fromJson(strHistoryList, new TypeToken<ArrayList<SavedCommandObject>>(){}.getType());

            for (int i = 0;i<tempCommandList.size();i++){
                String command = tempCommandList.get(i).getStrCommand();
                int number = tempCommandList.get(i).getIntCommand();
                SavedCommandObject nocObject = new SavedCommandObject(number,command);
                commandList.add(nocObject);
            }
            commandAdapter.notifyDataSetChanged();
        }

    }

    private void saveListToStorage(){
        String saveToJson = gson.toJson(commandList);
        mainActivity.editor.putString(getResources().getString(R.string.saved_command_key), saveToJson);
        mainActivity.editor.apply();
    }

    public void clearTerminal(){
        if(strBuf.length()>0){
            strBuf.delete(0,strBuf.length()-1);
        }
        txtDisplay.setText("");
        edtCommand.setText("");
    }

    private void displayCommandDialog(final int position){
        String command = commandList.get(position).getStrCommand();

        final Dialog openDialog = new Dialog(context);
        openDialog.setContentView(R.layout.edit_saved_command_dialog);

        final EditText edtCommand = (EditText)openDialog.findViewById(R.id.edtSavedCommand);
        edtCommand.setText(command);

        Button dialogEditCommand = (Button)openDialog.findViewById(R.id.btn_edit_command);
        dialogEditCommand.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                commandList.get(position).setStrCommand(edtCommand.getText().toString());
                commandAdapter.notifyDataSetChanged();

                saveListToStorage();

                openDialog.dismiss();
            }
        });

        Button dialogDeleteCommand = (Button)openDialog.findViewById(R.id.btn_delete_command);
        dialogDeleteCommand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                commandList.remove(position);
                commandAdapter.notifyDataSetChanged();

                saveListToStorage();
                openDialog.dismiss();
            }
        });

        openDialog.show();
    }

}
