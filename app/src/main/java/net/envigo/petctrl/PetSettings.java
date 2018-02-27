package net.envigo.petctrl;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
    private Conn conn;

    private ArrayList<PetClients> PetClientList;

    public static PetSettings newInstance() {
        PetSettings fragment = new PetSettings();
        //Bundle args = new Bundle();
        //args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        //fragment.setArguments(args);
        Log.d("Log", "newInstace called");

        return fragment;

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
                TabLayout tabLayout = ((MainActivity)getActivity()).mTabLayout;
                MainActivity.MyFragmentPageAdapter mAdapter = ((MainActivity)getActivity()).mSectionsPagerAdapter;
                if(tabLayout != null && mAdapter != null) {
                    tabLayout.removeTabAt(mAdapter.getPos());
                    mAdapter.removeFragment(mAdapter.getPos());
                    mAdapter.notifyDataSetChanged();
                }

                String admin_password = ((MainActivity)getActivity()).settings.getString("admin_password", null);
                String ap_name = ((MainActivity)getActivity()).settings.getString("ap_name", null);


                if (admin_password != null && ap_name != null && admin_password.length() >= 6 && ap_name.length() >= 4) {
                    WifiUtils wifi = new WifiUtils(context);
                    wifi.cfgAP(ap_name, admin_password);
                    Log.d("Log", "Main, configuring user ap mode");
                } else {
                    Toast.makeText(context, R.string.configNeed, Toast.LENGTH_SHORT).show();
                }
            }
        });

        petListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                Log.d("Log", "Item selected position " + position);
                Log.d("Log", "Item: " + listAdapter.getItem(position));
                PetClients client = PetClientList.get(position);
                Log.d("Log", "" + client.getIpAddr());

                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which){
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

                scanText.setText("WifiApState: " + wifiUtils.getState() + "\n\n");
                scanText.append("Clients: \n");

                //PetClients clientScanResult
                PetClientList = clients;

                //for (PetClients clientScanResult : clients) {
                for (PetClients clientScanResult : clients) {
                    listAdapter.add(clientScanResult.getIpAddr());


                            /*
                            scanText.append("####################\n");
                            scanText.append("IpAddr: " + clientScanResult.getIpAddr() + "\n");
                            scanText.append("Device: " + clientScanResult.getDevice() + "\n");
                            scanText.append("HWAddr: " + clientScanResult.getHWAddr() + "\n");
                            scanText.append("isReachable: " + clientScanResult.isReachable() + "\n");
                            */
                }
                //((MainActivity)getActivity()).addNewTab();
                petListView.setAdapter(listAdapter);

            }
        });

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d("Log", "onAttach PetSettings called");

        this.context = context;
    }

    public void assocClient(final int position) {
        final PetClients client = PetClientList.get(position);
        Log.d("Log", "assocClient");

        /*
        PetClientList.remove(position);
        listAdapter.remove(listAdapter.getItem(position));
        listAdapter.notifyDataSetChanged();
        */

        conn = new Conn(context);

        conn.setFileQuery("associate");
        conn.setMethod("POST");
        conn.setServerURL("http://" + client.getIpAddr() + "/");
        //conn.setServerURL("http://192.168.4.22/");
        final String admin_password = ((MainActivity)getActivity()).settings.getString("admin_password", "false");
        final String ap_name = ((MainActivity)getActivity()).settings.getString("ap_name", "false");

        if (admin_password.equals("false") || ap_name.equals("false")) { return; }

        HashMap<String, String> conn_data = new HashMap<>();
        conn_data.put("admin_password", admin_password);
        conn_data.put("ap_name", ap_name);

        conn.execute(conn_data);
    }

    public class Conn extends ConnRest {

        Context context;

        public Conn(Context context) {
            super(context);
            this.context = context;
        }

        @Override
        protected void onPostExecute(JSONObject jsonObject) {
            super.onPostExecute(jsonObject);
            Log.d("Log", "Conn onPostExecute PetSettings called");

            if (jsonObject != null) {

                try {
                    if (jsonObject.getString("status").equals("sucess")) {
                        scanBtn.performClick();
                    }
                    Toast.makeText(context, jsonObject.getString("status"), Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
