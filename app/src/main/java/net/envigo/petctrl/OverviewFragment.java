package net.envigo.petctrl;


import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;


public class OverviewFragment extends Fragment {


    public Button btnScan;
    public TextView txtOverview;
    private View rootView;

    public OverviewFragment() {
        // Required empty public constructor
    }


    public static OverviewFragment newInstance() {
        //Log.d("Log","OverviewFragment new instance");
        OverviewFragment fragment = new OverviewFragment();

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Log.d("Log", "OverviewFragment onCreate");
        //setRetainInstance(false);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        //Log.d("Log", "Over frag onCreateView");
        rootView = inflater.inflate(R.layout.fragment_overview, container, false);
        txtOverview = rootView.findViewById(R.id.txtOverview);

        btnScan = rootView.findViewById(R.id.btnScan);

        if (((MainActivity)getActivity()).checkConfig(false) == false) {
            btnScan.setEnabled(false);
        } else {

            btnScan.performClick();
        }


        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity)getActivity()).getClientList();
                //String txt = ((MainActivity)getActivity()).overviewText();
                //txtOverview.setText(txt);
            }
        });



        return rootView;
    }



    public  void setText(String text) {
        //txtOverview = rootView.findViewById(R.id.txtOverview);
        if (txtOverview != null) { //TODO: Al rotar la pantalla es null y peta
            Log.d("Log", "SetText OVerview called " +this);
            txtOverview.setText(text);
        } else {
            Log.d("Log", "SetText OVerview NOT called " + this);
        }
    }

    public void update() {
        if (btnScan != null) {
            //Log.d("Log", "UPDATE CLICK");
            btnScan.performClick();
        } else {
            //Log.d("Log", "UPDATE NOCLICK");
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d("Log", "OnAttach OVerview Frag");
        ((MainActivity)getActivity()).getClientList();
    }
}
