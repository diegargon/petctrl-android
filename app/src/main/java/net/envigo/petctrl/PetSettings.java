package net.envigo.petctrl;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *  Created by diego on 18/02/18.
 */

public class PetSettings  extends Fragment {

    private WifiUtils wifiUtils;
    private Context context;
    private Button scanBtn;
    private TextView scanText;
    private ArrayAdapter<String> listAdapter ;
    private ListView petListView;
    MainActivity activity;



    private ArrayList<PetClients> PetClientAssocList;

    public static PetSettings newInstance() {
        PetSettings fragment = new PetSettings();
        Log.d("Log", "Petsettings newInstance called");

        return fragment;

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setRetainInstance(false);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //return super.onCreateView(inflater, container, savedInstanceState);
        View rootView = inflater.inflate(R.layout.fragment_petsettings, container, false);

        Log.d("Log", "onCreateView Petlist called");

        wifiUtils = new WifiUtils(context);

        scanBtn = rootView.findViewById(R.id.scanBtn);
        scanText = rootView.findViewById(R.id.scanText);
        petListView = rootView.findViewById(R.id.petListView);
        Button closeBtn = rootView.findViewById(R.id.btnClose);

        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TabLayout tabLayout = activity.mTabLayout;
                MainActivity.MyFragmentPageAdapter mAdapter = activity.mSectionsPagerAdapter;

                String admin_password = activity.getAdminPassword();
                String ap_name = activity.getApName();

                if (admin_password != null && ap_name != null && admin_password.length() >= 6 && ap_name.length() >= 4) {
                    WifiUtils wifi = new WifiUtils(context);
                    wifi.cfgAP(ap_name, admin_password);
                    Log.d("Log", "Main, configuring user ap mode");
                } else {
                    Toast.makeText(context, R.string.configNeed, Toast.LENGTH_SHORT).show();
                }

                if (tabLayout != null && mAdapter != null) {
                    tabLayout.removeTabAt(mAdapter.getPos());
                    mAdapter.removeFragment(mAdapter.getPos());
                    mAdapter.notifyDataSetChanged();
                }
            }

        });

        petListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                Log.d("Log", "Item selected position " + position);
                Log.d("Log", "Item: " + listAdapter.getItem(position));
                PetClients client = PetClientAssocList.get(position);
                Log.d("Log", "" + client.getIpAddr());

                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                assocClient(position);
                                break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                //No button clicked
                                break;
                        }

                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage(R.string.action_pairdevices_ask).setPositiveButton(R.string.yes, dialogClickListener)
                        .setNegativeButton(R.string.no, dialogClickListener).show();
            }
        });

        //wifiUtils.cfgDiscoveryAP();
        scanBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Log.d("Log:", "Button pressed");
                wifiUtils.cfgDiscoveryAP();
                getClientList();

            }
        });

        return rootView;
    }

    private void getClientList() {

        wifiUtils.getClientList(true, 300, new iScanListener() {
            @Override
            public void onFinishScan(ArrayList<PetClients> clients) {
                ArrayList<String> petList = new ArrayList<>();
                listAdapter = new ArrayAdapter<>(context, R.layout.petlistrow, petList);

                String text = "WifiApState: " + wifiUtils.getState() + "\n\n";
                scanText.setText(text);
                scanText.append("Clients: \n");

                PetClientAssocList = clients;

                for (PetClients clientScanResult : clients) {
                    listAdapter.add(clientScanResult.getIpAddr());
                }
                petListView.setAdapter(listAdapter);
            }
        });

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d("Log", "onAttach PetSettings called");
        this.context = context;
        activity = ((MainActivity) getActivity());

    }

    @SuppressWarnings("unchecked")
    public void assocClient(final int position) {
        final PetClients client = PetClientAssocList.get(position);
        Log.d("Log", "assocClient");

        final String admin_password = activity.getAdminPassword();
        final String ap_name = activity.getApName();

        if (admin_password.equals("false") || ap_name.equals("false")) { return; }

        HashMap<String, String> conn_details = new HashMap<>();
        HashMap<String, String> conn_data = new HashMap<>();

        conn_details.put("method", "POST");
        conn_details.put("url", "http://" + client.getIpAddr() + "/associate");

        conn_data.put("admin_password", admin_password);
        conn_data.put("ap_name", ap_name);

        ConnRest conn = new ConnRest(new iConnResult() {

            @Override
            public void onSuccess(JSONObject jsonObject) {
                if (jsonObject != null) {

                    try {
                        if (jsonObject.getString("status").equals("sucess")) {
                            scanBtn.performClick();
                        }
                        Toast.makeText(context, jsonObject.getString("status"), Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.d("Log", "Success with null");
                }
            }

            @Override
            public void onFailure(Exception e) {

            }
        });
        conn.execute(conn_details, conn_data);

    }

}
