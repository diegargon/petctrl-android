package net.envigo.petctrl;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v4.app.Fragment;
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
import java.util.HashMap;

public class ClientFragment extends Fragment {

    private final boolean DEBUG = true;
    private boolean saveEnable = false;
    Context context;
    View rootView;
    PetClients PetClient;
    ProgressBar RSSIBar;
    public String PetClientID;
    TextView txtView ;
    TextView RSSIText;
    EditText edtPetName;
    EditText edtChipID;
    EditText edtPhone;
    String admin_pass;

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
        setRetainInstance(true);

        if (getArguments() != null && PetClient == null ) {
            PetClientID =  getArguments().getString("PetClientID");
            if (DEBUG) Log.d("Log", "Client on create position: " + PetClientID);

            if (((MainActivity)getActivity()).PetClientList.size() > 0) {
                if (DEBUG) Log.d("Log","Clientfrag PEtList" + ((MainActivity)getActivity()).PetClientList.size() );
                PetClient = ((MainActivity) getActivity()).getClientByID(PetClientID);
            }
        }

        admin_pass = ((MainActivity) getActivity()).getAdminPassword();

        if (admin_pass != null) {
            mHandler = new Handler();
            mHandler.post(runableClientUpdate);
        }
    }

    private Runnable runableClientUpdate = new Runnable() {
        int i = 0;
        @Override
        public void run() {
            i++;
            HashMap<String, String> conn_details = new HashMap<>();
            HashMap<String, String> conn_data = new HashMap<>();
            conn_details.put("method", "POST");
            conn_details.put("url", "http://" + PetClient.getIpAddr() + "/client_info");
            conn_data.put("admin_password", admin_pass);

            //Log.d("Log", "ClientFag: Called runableClientUpdate " + i);

            ConnRest conn = new ConnRest(new iConnResult() {
                @Override
                public void onSuccess(JSONObject jsonObject) {
                    //if (DEBUG) Log.d("Log", "Clientfrag refresh success");

                    try {
                        if (jsonObject.getString("status").equals("ok")) {
                            String RSSI = jsonObject.getString("RSSI");
                            PetClient.setRSSI(Integer.parseInt(RSSI));
                            clientUpdate();
                        } else {
                            if (DEBUG) Log.d("Log", "Clientfrag refresh status notOK");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }

                @Override
                public void onFailure(Exception e) {
                    if (DEBUG) Log.d("Log", "Clientfrag NO refresh");
                    PetClient.setRSSI(-100);
                    clientUpdate();
                }
            });
            conn.execute(conn_details, conn_data);

           mHandler.postDelayed(runableClientUpdate, 5000);
        }
    };

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d("Log", "Client frag onDetach");
        mHandler.removeCallbacks(runableClientUpdate);
        context = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        if (DEBUG) Log.d("Log", "ClientFrag onCreateView");

        rootView = inflater.inflate(R.layout.fragment_client, container, false);
        txtView = rootView.findViewById(R.id.txtStatus);
        RSSIText = rootView.findViewById(R.id.RSSIText);
        edtPetName = rootView.findViewById(R.id.edtName);
        edtChipID = rootView.findViewById(R.id.edtChipID);
        edtPhone = rootView.findViewById(R.id.edtPhone);
        RSSIBar = rootView.findViewById(R.id.RSSIBar);

        if (PetClient != null) {
            if (DEBUG) Log.d("Log","Client Farg *client* not null");
            edtPetName.setText(PetClient.getName());
            edtChipID.setText(PetClient.getChip());
            edtPhone.setText(PetClient.getPhoneNumber());
            txtView.setText(PetClient.getIpAddr());
            setRSSI(PetClient.getRSSI());
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

        ImageButton btnSave = rootView.findViewById(R.id.btnSave);
        //ImageButton btnTurnOff = rootView.findViewById(R.id.btnTurnOff);
        ImageButton btnReboot = rootView.findViewById(R.id.btnReboot);
        ImageButton btnLights = rootView.findViewById(R.id.btnLights);
        ImageButton btnVibration = rootView.findViewById(R.id.btnVibration);
        ImageButton btnSound = rootView.findViewById(R.id.btnSound);
        ImageView photo = rootView.findViewById(R.id.clientPhoto);

        btnLights.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(context, "Lights", Toast.LENGTH_SHORT).show();
            }
        });

        photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(context, "Clicked image", Toast.LENGTH_SHORT).show();
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

        /*
        btnTurnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        */

        btnReboot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which){
                            case DialogInterface.BUTTON_POSITIVE:
                                if (DEBUG) Log.d("Log", "Reboot button clicked");
                                if (PetClient == null) {
                                    if (DEBUG) Log.d("Log", "reboot fail client null");
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
                                        if (DEBUG) Log.d("Log", "Reboot no success");
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

        btnVibration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(context, "Vibrator", Toast.LENGTH_SHORT).show();
            }
        });

        btnSound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(context, "Sound", Toast.LENGTH_SHORT).show();
            }
        });

        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    public void setupClient() {
        //Log.d("Log", "setupClient" + ((MainActivity)getActivity()).PetClientList.size());

        if (PetClient != null) {
            if (DEBUG) Log.d("Log", "Setup Client SUCCESS Petclient NOT NULL");
            edtPetName.setText(PetClient.getName());
            edtChipID.setText(PetClient.getChip());
            edtPhone.setText(PetClient.getPhoneNumber());
            txtView.setText(PetClient.getIpAddr());
            setRSSI(PetClient.getRSSI());
        } else {
            if (DEBUG) Log.d("Log","Setup Client FAILED Petclient NULL");
        }

    }
    @Override
    public void onResume() {
        super.onResume();
        if (DEBUG) Log.d("Log", "Client Frag on resume");

    }

    @Override
    public void onPause() {
        super.onPause();
        if (DEBUG) Log.d("Log", "Client Frag onPause");
    }

    public void setRSSI(int RSSI) {
        if (RSSI > 10) { // greater mean a error
            if (DEBUG) Log.d("Log", "Clientfrag RSSI error");
            return;
        }
        int rssi = RSSI + 100;
        if (rssi > 70)  rssi = 80; // scale from 0 to 70 (-30 best rssi)

        //int old_rssi = RSSIBar.getProgress();

        RSSIBar.setProgress( rssi );
        RSSIText.setText(String.valueOf(rssi -100) );
    }

    public void closeTab(){
        ((MainActivity)getActivity()).closeCurrentTab(this);
    }

    public void clientUpdate () {
        //if (DEBUG) Log.d("Log", "Clientfrag update called");
        int RSSI = PetClient.getRSSI();

        setRSSI(RSSI);

        int sens = ((MainActivity) getActivity()).getSensProgress();
        int sensWarning = (sens * 5) - 100;
        //if (DEBUG) Log.d("Log", "Senswarning setup to " +sensWarning);
        if (sensWarning > -100 && RSSI < sensWarning) {
            Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(500);
        }

    }

    private void saveClientData() {
        mHandler.removeCallbacks(runableClientUpdate);

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
                mHandler.post(runableClientUpdate);
                try {
                    Toast.makeText(context,jsonObject.getString("status"), Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(context, "Excepcion", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }

            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(context, "Failed", Toast.LENGTH_SHORT).show();
                mHandler.post(runableClientUpdate);
            }

        });
        conn.execute(conn_details, conn_data);
    }
}
