package net.envigo.petctrl;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.text.method.HideReturnsTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import static java.lang.Math.round;


public class ClientFragment extends Fragment {

    private final static boolean DEBUG = true;
    public static final int GET_FROM_GALLERY = 3;
    private boolean saveEnable = false;
    protected SharedPreferences settings = null;

    public String PetClientID;
    Context context;
    View rootView;
    PetClients PetClient;
    ProgressBar RSSIBar;
    ProgressBar BatBar;
    TextView txtView ;
    TextView RSSIText;
    TextView BatText;
    EditText edtPetName;
    EditText edtChipID;
    EditText edtPhone;
    String admin_pass;
    int refreshTime;
    ImageButton btnLights;
    ImageButton btnVibration;
    ImageButton btnSound;
    ImageView photo;

    MainActivity activity;

    Handler mHandler = new Handler();

    public static ClientFragment newInstance(String PetClientID) {
        ClientFragment fragment = new ClientFragment();

        Bundle args = new Bundle();
        args.putString("PetClientID", PetClientID);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG) Log.d("Log", "Client onCreate " + this);

        if (getArguments() != null && PetClient == null ) {
            PetClientID =  getArguments().getString("PetClientID");
            if (DEBUG) Log.d("Log", "Client on create position: " + PetClientID);
        }
    }

    private Runnable runnableClientUpdate = new Runnable() {
        @SuppressWarnings("unchecked")
        @Override
        public void run() {

            int refreshPoint = settings.getInt("refreshTime", 5000);
            setRefreshTime(refreshPoint);

            HashMap<String, String> conn_details = new HashMap<>();
            HashMap<String, String> conn_data = new HashMap<>();
            conn_details.put("method", "POST");
            conn_details.put("url", "http://" + PetClient.getIpAddr() + "/client_info");
            conn_data.put("admin_password", admin_pass);

            //if (DEBUG) Log.d("Log", "ClientFag: Called runnableClientUpdate RefreshTime: " + refreshTime);

            ConnRest conn = new ConnRest(new iConnResult() {
                int lights, vibration, sound = 0;

                @Override
                public void onSuccess(JSONObject jsonObject) {
                    //if (DEBUG) Log.d("Log", "Clientfrag refresh success");

                    try {
                        if (jsonObject.getString("status").equals("ok")) {
                            String RSSI = jsonObject.getString("RSSI");
                            String Battery = jsonObject.getString("adc");

                            if ( (PetClient.getRSSI() != Integer.parseInt(RSSI))
                                    || (PetClient.getBattery() != Integer.parseInt(Battery) )
                                    ) {
                                PetClient.setRSSI(Integer.parseInt(RSSI));
                                PetClient.setBattery(Integer.parseInt(Battery));

                                lights = jsonObject.getInt("lights");
                                vibration = jsonObject.getInt("vibration");
                                sound= jsonObject.getInt("sound");
                                gpioButtonsState(lights, vibration, sound);

                                clientUpdate();
                            }

                            if (!PetClient.isReachable()) {
                                PetClient.setReachable(true);
                                activity.OverviewUpdate();
                            }

                        } else {
                            Log.e("Log", "Clientfrag refresh status notOK");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e("Log", "Clientfrag runnable Client update onFailed");
                    if (PetClient.isReachable()) {
                        PetClient.setRSSI(-100);
                        PetClient.setBattery(0);
                        PetClient.setReachable(false);
                        activity.OverviewUpdate();
                    }
                    clientUpdate();
                }
            });
            conn.execute(conn_details, conn_data);

           mHandler.postDelayed(runnableClientUpdate, refreshTime);
        }
    };

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (DEBUG) Log.d("Log", "Client frag onAttach " + this);
        this.context = context;
        activity = ((MainActivity) getActivity());
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (DEBUG) Log.d("Log", "Client frag onDetach " + this );
        mHandler.removeCallbacks(runnableClientUpdate);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        admin_pass = activity.getAdminPassword();
        settings = PreferenceManager.getDefaultSharedPreferences(context);
        setRefreshTime(settings.getInt("refreshTime", 1));

        if (DEBUG) Log.d("Log", "ClientFrag onCreateView " + this);

        if (activity.PetClientList.size() > 0) {
            if (DEBUG) Log.d("Log","Clientfrag PEtList" + activity.PetClientList.size() );
            PetClient = activity.getClientByID(PetClientID);
        } else {
            Log.e("Log","Clientfrag PetList on activity its 0");
        }

        rootView = inflater.inflate(R.layout.fragment_client, container, false);
        txtView = rootView.findViewById(R.id.txtStatus);
        RSSIText = rootView.findViewById(R.id.RSSIText);
        BatText = rootView.findViewById(R.id.BatText);
        edtPetName = rootView.findViewById(R.id.edtName);
        edtChipID = rootView.findViewById(R.id.edtChipID);
        edtPhone = rootView.findViewById(R.id.edtPhone);
        RSSIBar = rootView.findViewById(R.id.RSSIBar);
        BatBar = rootView.findViewById(R.id.BatStatus);

        ImageButton btnSave = rootView.findViewById(R.id.btnSave);
        ImageButton btnReboot = rootView.findViewById(R.id.btnReboot);
        btnLights = rootView.findViewById(R.id.btnLights);
        btnVibration = rootView.findViewById(R.id.btnVibration);
        btnSound = rootView.findViewById(R.id.btnSound);
        photo = rootView.findViewById(R.id.clientPhoto);

        if (PetClient != null) {
            edtPetName.setText(PetClient.getName());
            edtChipID.setText(PetClient.getChip());
            edtPhone.setText(PetClient.getPhoneNumber());
            txtView.setText(PetClient.getIpAddr());
            setRSSI(PetClient.getRSSI());
            setBattery(PetClient.getBattery());
            gpioButtonsState(PetClient.lightState, PetClient.vibrationState, PetClient.soundState);
        } else {
            Log.e("Log", "Petclient its null");
        }

        Switch switchSave = rootView.findViewById(R.id.swtSaveUnlock);

        switchSave.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                edtPetName.setEnabled(isChecked);
                edtChipID.setEnabled(isChecked);
                edtPhone.setEnabled(isChecked);
                saveEnable = isChecked;
            }
        });

        if (PetClient != null) {
            if (DEBUG) Log.d("Log", "Clientfrag Petclient not null getting image");
            getPetImage();
        } else {
            Log.e("Log","Clientfrag Petclient null not getting image " + this);
        }

        photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (activity.checkPermissions("READ_EXTERNAL")) {
                    startActivityForResult(new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI), GET_FROM_GALLERY);
                } else {
                    activity.requestPermissions("READ_EXTERNAL");
                }
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which){
                            case DialogInterface.BUTTON_POSITIVE:
                                if (DEBUG) Log.d("Log", "SAved button clicked");
                                activity.ShowProgressDialog();
                                saveClientData();
                            case DialogInterface.BUTTON_NEGATIVE:

                        }
                    }
                };

                if (saveEnable) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setMessage(R.string.areyousure).setPositiveButton(R.string.yes, dialogClickListener)
                            .setNegativeButton(R.string.no, dialogClickListener).show();
                } else {
                    Toast.makeText(getActivity(), "Habilite la edicion primero", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnReboot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {

                    @SuppressWarnings("unchecked")
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which){
                            case DialogInterface.BUTTON_POSITIVE:
                                if (DEBUG) Log.d("Log", "Reboot button clicked");
                                if (PetClient == null) {
                                    if (DEBUG) Log.e("Log", "reboot fail client null");
                                    return;
                                }

                                HashMap<String, String> conn_details = new HashMap<>();
                                HashMap<String, String> conn_data = new HashMap<>();

                                conn_details.put("method", "POST");
                                conn_details.put("url", "http://" + PetClient.getIpAddr() + "/settings");

                                if (admin_pass == null) return;

                                conn_data.put("admin_password", admin_pass);
                                conn_data.put("restart", "1");

                                ConnRest conn = new ConnRest(new iConnResult() {
                                    @Override
                                    public void onSuccess(JSONObject jsonObject) {
                                        if (DEBUG) Log.d("Log", "Reboot success");
                                        closeTab();
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        Log.e("Log", "Reboot no success");
                                    }
                                });
                                conn.execute(conn_details, conn_data);
                            case DialogInterface.BUTTON_NEGATIVE:

                        }
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(R.string.areyousure).setPositiveButton(R.string.yes, dialogClickListener)
                        .setNegativeButton(R.string.no, dialogClickListener).show();
            }
        });

        btnLights.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.ShowProgressDialog();
                gpio_toggle("lights");
            }
        });

        btnVibration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.ShowProgressDialog();
                gpio_toggle("vibration");
            }
        });

        btnSound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.ShowProgressDialog();
                gpio_toggle("sound");
            }
        });

        if (admin_pass != null && PetClient != null) {
            mHandler = new Handler();
            mHandler.post(runnableClientUpdate);
        } else {
            Toast.makeText(getActivity(), "Admin pass or Petcliente Null", Toast.LENGTH_SHORT).show();
            Log.e("Log", "Admin pass or Petclient NULL " + this);
        }

        return rootView;
    }

    public void setRSSI(int RSSI) {
        if (RSSI > 10) { // greater mean a error
            Log.e("Log", "Clientfrag: RSSI error " + RSSI);
            return;
        }

        int rssi = RSSI + 100;
        if (rssi > 70)  rssi = 70; // scale from 0 to 70 (-30 best rssi)
        if (rssi >= 50)  rssi = map_range(rssi, 50, 70, 45, 50);
        if(rssi < 50) rssi = map_range(rssi, 0, 49, 0,44);

        //int old_rssi = RSSIBar.getProgress();
        String rssi_text = "RSSI: "+ String.valueOf(RSSI);
        RSSIBar.setProgress( rssi );
        RSSIText.setText( rssi_text );

    }
    public void setBattery(int Bat) {
        /* No min feature in progress bar for the api level
        * battery must go from 1024(4.1) to 699(2.8) = 325 states (ProgressBar)
        * 699 = 2.8   674 = 2.7v
        * 4.1v typical 18650 max
        */
        int result_status = Bat - 699;
        if (result_status < 0) result_status = 0;

        float float_voltage = ( (float)Bat * (float) 4.1) / (float) 1024.0 ;
        double voltage = Math.round(float_voltage * 100.0) / 100.0;

        String battery_text = getString(R.string.battery)
                + String.valueOf(voltage)
                + "v";

        BatBar.setProgress(result_status);
        BatText.setText(battery_text);
    }

    int map_range (int value, int in_min, int in_max, int out_min, int out_max) {
        return (value - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
    }

    public void closeTab(){
        activity.closeCurrentTab(this);
    }

    public void clientUpdate () {
        //if (DEBUG) Log.d("Log", "Clientfrag update called");
        int RSSI = PetClient.getRSSI();

        setRSSI(RSSI);
        setBattery(PetClient.getBattery());
        int sens = activity.getSensProgress();
        int sensWarning = (sens * 5) - 100;
        //if (DEBUG) Log.d("Log", "Senswarning setup to " +sensWarning);
        if (sensWarning > -100 && RSSI < sensWarning) {
            activity.shakeIt();
        }

    }

    @SuppressWarnings("unchecked")
    private void saveClientData() {
        mHandler.removeCallbacks(runnableClientUpdate);

        HashMap<String, String> conn_details = new HashMap<>();
        HashMap<String, String> conn_data = new HashMap<>();

        conn_details.put("method", "POST");
        conn_details.put("url", "http://" + PetClient.getIpAddr() + "/settings_android");
        conn_data.put("admin_password", admin_pass);
        conn_data.put("petName", edtPetName.getText().toString());
        conn_data.put("petChip", edtChipID.getText().toString());
        conn_data.put("telefono", edtPhone.getText().toString());

        ConnRest conn = new ConnRest(new iConnResult() {
            @Override
            public void onSuccess(JSONObject jsonObject) {
                mHandler.post(runnableClientUpdate);
                try {
                    Toast.makeText(context, getString(R.string.needReboot), Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(context, "Excepcion", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
                activity.HideProgressDialog();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(context, "Failed", Toast.LENGTH_SHORT).show();
                activity.HideProgressDialog();
                mHandler.post(runnableClientUpdate);
            }

        });
        conn.execute(conn_details, conn_data);
    }

    void setRefreshTime(int point) {
        if (point == 0)
            refreshTime = 5 * 1000;
         else if (point == 1)
            refreshTime = 10 * 1000;
         else if (point == 2)
            refreshTime = 20 * 1000;
         else if (point == 3)
            refreshTime = 30 * 1000;
         else if (point == 4)
            refreshTime = 60 * 1000;
         else if (point == 5)
            refreshTime = 180 * 1000;
    }

    @SuppressLint("StaticFieldLeak")
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==GET_FROM_GALLERY && resultCode == Activity.RESULT_OK) {
            Uri selectedImage = data.getData();
            ByteArrayOutputStream bao = new ByteArrayOutputStream();
            Bitmap bitmap;

            try {
                bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), selectedImage);

                if (bitmap == null) return;

                activity.ShowProgressDialog();
                int maxWidth = 300;

                if (bitmap.getWidth() > maxWidth) {
                    float aspectRatio = bitmap.getWidth() / (float) bitmap.getHeight();
                    int ratioHeight = round(maxWidth / aspectRatio);
                    bitmap = Bitmap.createScaledBitmap(bitmap, maxWidth, ratioHeight, false);
                }

                int quality = 90;
                if (bitmap.getByteCount() > 100000) {
                    quality = 75;
                }

                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, bao);
                Log.d("Log", "Size compressed " + bao.size());
                byte[] image = bao.toByteArray();

                final File f = new File(context.getCacheDir(), "foto_tmp.jpg");
                //noinspection ResultOfMethodCallIgnored
                f.createNewFile(); // return if file exist true or false
                FileOutputStream fos = new FileOutputStream(f);
                fos.write(image);
                fos.flush();
                fos.close();

                new AsyncTask<Void, Void, Void>() {

                    @Override
                    protected Void doInBackground(Void... voids) {

                        String charset = "UTF-8";
                        String requestURL = "http://" + PetClient.getIpAddr() + "/photoupload";

                        try {
                            MultipartUtility multipart;
                            multipart = new MultipartUtility(requestURL, charset);
                            multipart.addFilePart("name", new File(f.getAbsolutePath()));
                            String response = multipart.finish(); // response from server.
                            Log.d("Log", "Response " + response);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        super.onPostExecute(aVoid);
                        try {
                            getPetImage();
                            //String response = multipart.finish(); // response from server.
                            //Log.d("Log", "Response " + response);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        activity.HideProgressDialog();
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            } catch (IOException e) {
                activity.HideProgressDialog();
                e.printStackTrace();
            }
        }
    }

    void getPetImage() {
        GetPetImageTask getPetImageTask = new GetPetImageTask();
        getPetImageTask.execute("http://" + PetClient.getIpAddr() + "/foto.jpg");
    }

    @SuppressLint("StaticFieldLeak")
    class GetPetImageTask extends DownloadImageTask {
         @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            photo.setImageBitmap(bitmap);
        }
    }

    @SuppressWarnings("unchecked")
    void gpio_toggle(String gpio_id) {

        if (admin_pass == null) return;

        HashMap<String, String> conn_details = new HashMap<>();
        HashMap<String, String> conn_data = new HashMap<>();

        conn_details.put("method", "POST");
        conn_details.put("url", "http://" + PetClient.getIpAddr() + "/gpio_toggle");
        conn_data.put("admin_password", admin_pass);
        conn_data.put("toggle", gpio_id);

        ConnRest conn = new ConnRest(new iConnResult() {
            String msg = getString(R.string.error);
            int lights = 0;
            int vibration = 0;
            int sound = 0;

            @Override
            public void onSuccess(JSONObject jsonObject) {
                activity.HideProgressDialog();
                try {
                    msg = jsonObject.getString("msg");
                    lights = jsonObject.getInt("lights");
                    vibration = jsonObject.getInt("vibration");
                    sound= jsonObject.getInt("sound");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Log.d("Log", jsonObject.toString());
                gpioButtonsState(lights, vibration, sound);

                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Exception e) {
                activity.HideProgressDialog();
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            }
        });
        conn.execute(conn_details, conn_data);
    }

    void gpioButtonsState(int lights, int vibration, int sound) {

        PetClient.lightState = lights;
        PetClient.vibrationState = vibration;
        PetClient.soundState = sound;

        if(lights == 1)
            btnLights.setColorFilter(Color.argb(255, 255, 51, 0));
        else
            btnLights.setColorFilter(Color.argb(255, 0, 0, 0));

        if(vibration == 1)
            btnVibration.setColorFilter(Color.argb(255, 255, 51, 0));
         else
            btnVibration.setColorFilter(Color.argb(255, 0, 0, 0));

        if(sound == 1)
            btnSound.setColorFilter(Color.argb(255, 255, 51, 0));
         else
            btnSound.setColorFilter(Color.argb(255, 0, 0, 0));
    }
/*
    @Override
    public void onResume() {
        super.onResume();
        if (DEBUG) Log.d("Log", "ClientFrag: onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        if (DEBUG) Log.d("Log", "ClientFrag: onPause");
    }
*/
}