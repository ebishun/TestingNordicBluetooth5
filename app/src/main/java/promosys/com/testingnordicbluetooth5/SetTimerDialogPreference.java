package promosys.com.testingnordicbluetooth5;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.app.AlertDialog;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;

public class SetTimerDialogPreference extends DialogPreference {

    Context mContext;
    private MainActivity mainActivity;
    SharedPreferences SP;
    SharedPreferences.Editor editor;
    private TextView txtTitle,txtSummary;

    AlertDialog alertDialog;

    private RadioButton rbtn1,rbtn5,rbtn10,rbtn30,rbtnNone;

    private String strTimerDuration;

    public SetTimerDialogPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public SetTimerDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init(context);
    }

    @Override
    public View getView(View convertView, ViewGroup parent)
    {
        View v = super.getView(convertView, parent);
        txtTitle = (TextView)v.findViewById(R.id.txt_pref_title);
        txtSummary = (TextView)v.findViewById(R.id.txt_pref_summary);

        txtTitle.setText("Timer Duration");
        txtSummary.setText("Set timer duration during sending");

        return v;
    }

    @Override
    protected void onBindDialogView(View view) {
        strTimerDuration = SP.getString("timerDuration","");

        rbtn1 = (RadioButton)view.findViewById(R.id.rbtn1);
        rbtn5 = (RadioButton)view.findViewById(R.id.rbtn5);
        rbtn10 = (RadioButton)view.findViewById(R.id.rbtn10);
        rbtn30 = (RadioButton)view.findViewById(R.id.rbtn30);
        rbtnNone = (RadioButton)view.findViewById(R.id.rbtn_none);

        switch (strTimerDuration){
            case "1":
                rbtn1.setChecked(true);
                break;

            case "5":
                rbtn5.setChecked(true);
                break;

            case "10":
                rbtn10.setChecked(true);
                break;

            case "30":
                rbtn30.setChecked(true);
                break;

            case "None":
                rbtnNone.setChecked(true);
                break;
        }
        super.onBindDialogView(view);
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);

        alertDialog = (AlertDialog) getDialog();
        alertDialog.setCanceledOnTouchOutside(false);
        strTimerDuration = SP.getString("timerDuration","");
        //mainActivity.timerDuration = Integer.parseInt(strTimerDuration)*1000;

        Button positiveButton = alertDialog
                .getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(rbtn1.isChecked()){
                    strTimerDuration = "1";
                    setTimerDuration();
                }else if(rbtn5.isChecked()){
                    strTimerDuration = "5";
                    setTimerDuration();
                }else if(rbtn10.isChecked()){
                    strTimerDuration = "10";
                    setTimerDuration();
                }else if(rbtn30.isChecked()){
                    strTimerDuration = "30";
                    setTimerDuration();
                }else if(rbtnNone.isChecked()){
                    strTimerDuration = "None";
                    mainActivity.isTimerRunning = false;
                    mainActivity.isTimerEnable = false;
                }

                editor.putString("timerDuration",strTimerDuration);
                editor.apply();

                alertDialog.dismiss();
            }
        });

    }

    private void setTimerDuration(){
        mainActivity.isTimerEnable = true;
        mainActivity.timerDuration = Integer.parseInt(strTimerDuration)*1000;
    }

    private void init(Context context) {
        setPersistent(false);
        setDialogLayoutResource(R.layout.timer_dialog_preference);
        SP = PreferenceManager.getDefaultSharedPreferences(context);
        editor = SP.edit();
        if(SP.getString("timerDuration","").isEmpty()){
            editor.putString("timerDuration","30");
            editor.apply();
        }
        strTimerDuration = SP.getString("timerDuration","");

        mainActivity = (MainActivity) mContext;

        if(strTimerDuration.equals("None")){
            mainActivity.isTimerRunning = false;
            mainActivity.isTimerEnable = false;
        }else {
            mainActivity.isTimerEnable = true;
            mainActivity.timerDuration = Integer.parseInt(strTimerDuration)*1000;
        }
    }

}


