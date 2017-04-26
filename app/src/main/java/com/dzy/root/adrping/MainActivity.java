package com.dzy.root.adrping;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int msgKey1 = 1;
    private static final String pingRes = "pingRes";

    private static String startPingStr;
    private static String stopLastPingStr;
    private static String stopPingStr;
    private static String exitPingStr;
    private static String failPingStr;
    private static String cannotReachStr;
    private static String finishPingStr;
    private static String timeOutStr;

    private EditText ipaddr_etxt;
    private Button start_btn, cancel_btn;
    private TextView show_txt;
    private Toast tst;
    private PingThread pingThread;
    private KeeperThread keeperThread;
    volatile boolean pingIsAlive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        start_btn = (Button) findViewById(R.id.start_btn);
        cancel_btn = (Button) findViewById(R.id.cancel_btn);
        ipaddr_etxt =(EditText)findViewById(R.id.ipaddr_etxt);
        show_txt =(TextView)findViewById(R.id.show_txt);

        startPingStr = getResources().getString(R.string.start_ping);
        stopLastPingStr = getResources().getString(R.string.stop_last_ping);
        stopPingStr = getResources().getString(R.string.stop_ping);
        exitPingStr = getResources().getString(R.string.exit_ping);
        failPingStr = getResources().getString(R.string.fail_ping);
        cannotReachStr = getResources().getString(R.string.cannot_reach);
        finishPingStr = getResources().getString(R.string.finish_ping);
        timeOutStr = getResources().getString(R.string.time_out);

        start_btn.setOnClickListener(this);
        cancel_btn.setOnClickListener(this);
        pingThread=new PingThread();
        keeperThread=new KeeperThread();

    }

    @Override
    public void onClick(View v) {
        if(tst !=null)
            tst.cancel();
        switch (v.getId()) {
            case R.id.start_btn:
                if (pingThread.isAlive())
                {
                    setTimeToast(stopLastPingStr);
                    pingThread.interrupt();
                    keeperThread.interrupt();
                }
                setTimeToast(startPingStr);
                Thread.State pingThreadState= pingThread.getState();
                if(pingThreadState == Thread.State.TERMINATED) {
                    pingThread = new PingThread();
                    keeperThread =new KeeperThread();
                }
                pingIsAlive =true;
                pingThread.start();
                keeperThread.start();
                break;
            case R.id.cancel_btn:
                if (pingThread.isAlive())
                {
                    setTimeToast(stopPingStr);
                    pingThread.interrupt();
                    keeperThread.interrupt();
                }else
                {
                    setTimeToast(exitPingStr);
                    finish();
                }
                break;
            default:
                break;
        }
    }

    public class PingThread extends  Thread{
        private Process process;
        @Override
        public void run() {
            super.run();
            boolean isRun =true;
            do{
                String ipaddr_str=ipaddr_etxt.getText().toString();
                String line = null;
                BufferedReader successReader = null;
                String command = "ping " + ipaddr_str;
                Bundle bundle = new Bundle();

                try {
                    process = Runtime.getRuntime().exec(command);
                    if (process == null) {
                        bundle.putString(pingRes, failPingStr);
                        Message msg = new Message();
                        msg.what = msgKey1;
                        msg.setData(bundle);
                        mHandler.sendMessage(msg);
                    }else
                    {
                        successReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                        while ((line = successReader.readLine()) != null) {
                            pingIsAlive =true;
                            bundle.putString(pingRes, line);
                            Message msg = new Message();
                            msg.what = msgKey1;
                            msg.setData(bundle);
                            mHandler.sendMessage(msg);
                        }
                        bundle.putString(pingRes, cannotReachStr);
                        Message msg = new Message();
                        msg.what = msgKey1;
                        msg.setData(bundle);
                        mHandler.sendMessage(msg);
                    }
                }catch (IOException e) {
                }
                isRun =false;
            }while (isRun);
        }
        @Override
        public void interrupt() {
            super.interrupt();
            if(process != null)
            {
                process.destroy();
                Bundle bundle = new Bundle();
                if(pingIsAlive)
                    bundle.putString(pingRes, finishPingStr);
                else
                    bundle.putString(pingRes, timeOutStr);
                Message msg = new Message();
                msg.what = msgKey1;
                msg.setData(bundle);
                mHandler.sendMessage(msg);
            }
            pingIsAlive =false;
        }
    }

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Bundle bundle;
            switch (msg.what){
                case msgKey1:
                    bundle =msg.getData();
                    show_txt.setText(bundle.getString(pingRes));
                    break;
                default:
                    break;
            }
        }
    };
    private void setTimeToast(String showTxt) {
        tst = Toast.makeText(this, showTxt,
                Toast.LENGTH_SHORT);
        tst.show();
    }

    public class KeeperThread extends  Thread {
        @Override
        public void run() {
            super.run();
            while (true)
            {
                if(pingIsAlive)
                {
                    pingIsAlive =false;
                    try {
                        sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }else
                {
                    if (pingThread.isAlive())
                    {
                        pingThread.interrupt();
                    }
                    break;
                }
            }
        }
    }

}
