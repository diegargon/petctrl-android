package net.envigo.petctrl;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;
import java.util.HashMap;

public class ClientFragment extends Fragment {

    //Context context;
    View rootView;
    PetClients PetClient;
    ProgressBar RSSIBar;
    String PetClientID;
    TextView txtView ;
    EditText edtPetName;
    EditText edtChipID;
    EditText edtPhone;

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
            Log.d("Log", "Client on create position: " + PetClientID);

            if (((MainActivity)getActivity()).PetClientList.size() > 0) {
                Log.d("Log","Clientfrag PEtList" + ((MainActivity)getActivity()).PetClientList.size() );
                PetClient = ((MainActivity) getActivity()).getClientByID(PetClientID);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        Log.d("Log", "ClientFrag onCreateView");

        rootView = inflater.inflate(R.layout.fragment_client, container, false);
        txtView = rootView.findViewById(R.id.txtStatus);
        edtPetName = rootView.findViewById(R.id.edtName);
        edtChipID = rootView.findViewById(R.id.edtChipID);
        edtPhone = rootView.findViewById(R.id.edtPhone);
        RSSIBar = rootView.findViewById(R.id.RSSIBar);

        if (PetClient != null) {
            Log.d("Log","Client Farg *client* not null");
            edtPetName.setText(PetClient.getName());
            edtChipID.setText(PetClient.getChip());
            edtPhone.setText(PetClient.getPhoneNumber());
            txtView.setText(PetClient.getIpAddr());
            setRSSI(PetClient.getRSSI());
        }

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
                Log.d("Log", "Light button clicked");
            }
        });

        photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getActivity(), "Clicked image", Toast.LENGTH_SHORT).show();
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
                                Log.d("Log", "SAved button clicked");
                            case DialogInterface.BUTTON_NEGATIVE:

                        }
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(R.string.areyousure).setPositiveButton(R.string.yes, dialogClickListener)
                        .setNegativeButton(R.string.no, dialogClickListener).show();
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
                                Log.d("Log", "Reboot button clicked");
                                if (PetClient == null) {
                                    Log.d("Log", "reboot fail client null");
                                    return;
                                }
                                HashMap<String, String> conn_details = new HashMap<>();
                                HashMap<String, String> conn_data = new HashMap<>();

                                conn_details.put("method", "POST");
                                conn_details.put("url", "http://" + PetClient.getIpAddr() + "/settings");

                                String admin_pass = ((MainActivity) getActivity()).getAdminPassword();
                                if (admin_pass == null) return;


                                conn_data.put("admin_password", admin_pass);
                                conn_data.put("restart", "1");

                                ConnRest conn = new ConnRest(new iConnResult() {
                                    @Override
                                    public void onSuccess(JSONObject jsonObject) {
                                        Log.d("Log", "Reboot success");
                                        closeTab();
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        Log.d("Log", "Reboot no success");
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
                Log.d("Log", "Vibration button clicked");
            }
        });

        btnSound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("Log", "Sound button clicked");
                if (PetClient == null) {
                    Log.d("Log", "Petclient its NULL " + PetClientID);
                    return;
                }

            }
        });

        return rootView;
    }

    public void setupClient() {
        //Log.d("Log", "setupClient" + ((MainActivity)getActivity()).PetClientList.size());


        if (PetClient != null) {
            Log.d("Log", "Setup Client SUCCESS Petclient NOT NULL");
            edtPetName.setText(PetClient.getName());
            edtChipID.setText(PetClient.getChip());
            edtPhone.setText(PetClient.getPhoneNumber());
            txtView.setText(PetClient.getIpAddr());
            setRSSI(PetClient.getRSSI());
        } else {
            Log.d("Log","Setup Client FAILED Petclient NULL");
        }

    }
    @Override
    public void onResume() {
        super.onResume();
        Log.d("Log", "Client Frag on resume");

    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d("Log", "Client Frag onPause");
    }

    public void setRSSI(int RSSI) {
        RSSIBar.setProgress( (RSSI + 100) );
    }

    public void closeTab(){
        //((MainActivity)getActivity()).PetClientList.remove(PetClient_pos);
        ((MainActivity)getActivity()).removeClientByID(PetClientID);
        ((MainActivity)getActivity()).closeCurrentTab();
    }

    public void update () {
        Log.d("Log", "Clientfrag update called");
        //client = ((MainActivity)getActivity()).PetClientList.get(PetClient_pos);
    }
}
