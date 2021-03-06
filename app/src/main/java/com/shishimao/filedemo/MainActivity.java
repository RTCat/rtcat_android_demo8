package com.shishimao.filedemo;

import android.Manifest;
import android.content.Intent;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.nbsp.materialfilepicker.MaterialFilePicker;
import com.nbsp.materialfilepicker.ui.FilePickerActivity;
import com.shishimao.sdk.Errors;
import com.shishimao.sdk.RTCat;
import com.shishimao.sdk.Receiver;
import com.shishimao.sdk.RemoteStream;
import com.shishimao.sdk.Sender;
import com.shishimao.sdk.Session;
import com.shishimao.sdk.http.RTCatRequests;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "FileTest";

    TextView chooseFileNameText;
    TextView sendFileNameText;
    TextView receiveFileNameText;
    TextView sendFileProgressText;

    String sendFilePath;
    File fileSend;
    File fileReceive;

    //RTcat
    RTCat cat;
    String token;
    Session session;
    ArrayList<Sender> senders = new ArrayList<>();
    ArrayList<Receiver> receivers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String[] permissions = {
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
        };
        ActivityCompat.requestPermissions(this,permissions, 111);


        chooseFileNameText = (TextView)findViewById(R.id.txt_choose_filename);
        sendFileNameText = (TextView)findViewById(R.id.txt_send_filename);
        receiveFileNameText = (TextView)findViewById(R.id.txt_receive_filename);
        sendFileProgressText = (TextView)findViewById(R.id.txt_send_progress);

        initCat();
    }

    public void onChooseFile(View view){
        new MaterialFilePicker()
                .withActivity(this)
                .withRequestCode(1)
                //.withFilter(Pattern.compile(".*\\.txt$")) // Filtering files and directories by file name using regexp
                .withFilterDirectories(false) // Set directories filterable (false by default)
                .withHiddenFiles(true) // Show hidden files and folders
                .start();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK) {
            sendFilePath = data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH);
            // Do anything with file
            chooseFileNameText.setText(sendFilePath);
            //sendFileProgressText.setText(0 + "%");
            Log.i("FileDemo",sendFilePath);
        }
    }

    public void initCat(){
        cat = new RTCat(this);
        cat.addObserver(new RTCat.RTCatObserver() {
            @Override
            public void error(Errors errors) {

            }

            @Override
            public void init() {
                createSession();
            }
        });
        cat.init();
    }

    public void createSession(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    RTCatRequests requests = new RTCatRequests(Config.APIKEY, Config.SECRET);
                    token = requests.getToken(Config.P2P_SESSION, "pub");
                    session = cat.createSession(token, Session.SessionType.P2P);
                    session.addObserver(new Session.SessionObserver() {
                        @Override
                        public void in(String token) {
                            JSONObject attr = new JSONObject();
                            try {
                                attr.put("type", "main");
                                attr.put("name", "old wang");
                            } catch (Exception e) {

                            }

                            session.sendTo(null,true,attr, token);
                        }

                        @Override
                        public void out(String token) {

                        }

                        @Override
                        public void connected(ArrayList wits) {
                            JSONObject attr = new JSONObject();
                            try {
                                attr.put("type", "main");
                                attr.put("name", "old wang");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            session.send(null,true,attr);
                        }

                        @Override
                        public void remote(final Receiver receiver) {
                            try {
                                receivers.add(receiver);

                                receiver.addObserver(new Receiver.ReceiverObserver() {
                                    @Override
                                    public void stream(RemoteStream remoteStream) {

                                    }

                                    @Override
                                    public void message(final String s) {
                                        Log.i(TAG,s);
                                    }

                                    @Override
                                    public void close() {

                                    }

                                    @Override
                                    public void error(Errors errors) {

                                    }

                                    @Override
                                    public void log(JSONObject jsonObject) {

                                    }


                                    @Override
                                    public void file(final File file) {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {

                                                receiveFileNameText.setText(file.getAbsolutePath());
                                            }
                                        });
                                        fileReceive = file;
                                        //TODO File
                                    }
                                });

                                receiver.response();
                            } catch (Exception e) {
                                Log.e(TAG,e.getMessage());
                            }
                        }

                        @Override
                        public void local(Sender sender) {
                            senders.add(sender);
                            sender.addObserver(new Sender.SenderObserver() {
                                @Override
                                public void close() {

                                }

                                @Override
                                public void error(Errors errors) {

                                }

                                @Override
                                public void log(JSONObject o) {

                                }

                                @Override
                                public void fileSending(final int x) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            sendFileProgressText.setText(x + "%");
                                        }
                                    });
                                }

                                @Override
                                public void fileFinished() {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            //TODO
                                            sendFileNameText.setText(sendFilePath);
                                        }
                                    });
                                }
                            });
                        }

                        @Override
                        public void message(String token, final String message) {

                        }

                        @Override
                        public void error(Errors errors) {

                        }


                        @Override
                        public void close() {
                            finish();
                        }
                    });

                    session.connect();

                } catch (Exception e) {
                    Log.e(TAG,e.getMessage());
                }
            }
        }).start();
    }

    public void onSendFile(View view){
        fileSend = new File(sendFilePath);

        Log.i(TAG,"file name: "+ fileSend.getName() + "file size: " + fileSend.length() + "file path: " + fileSend.getAbsolutePath());

        for (Sender sender:senders
                ) {
            sender.sendFile(fileSend);
        }
    }
}
