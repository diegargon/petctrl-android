package net.envigo.petctrl;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

public class OverviewFragment extends Fragment {

    private static final boolean DEBUG = true;

    protected SharedPreferences settings;

    private Context context;
    public Button btnScan;
    public TextView txtOverview;
    private View rootView;
    private int sensBarProgress;
    private SeekBar sensBar;


    public OverviewFragment() {
        // Required empty public constructor
    }

    public static OverviewFragment newInstance() {
       // if (DEBUG) Log.d("Log","OverviewFragment new instance");
        OverviewFragment fragment = new OverviewFragment();

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG) Log.d("Log", "OverviewFragment onCreate");
        //setRetainInstance(false);

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (DEBUG) Log.d("Log", "Overviewfrag onCreateView");

        rootView = inflater.inflate(R.layout.fragment_overview, container, false);
        txtOverview = rootView.findViewById(R.id.txtOverview);
        btnScan = rootView.findViewById(R.id.btnScan);
        sensBar = rootView.findViewById(R.id.sensBar);

        settings = PreferenceManager.getDefaultSharedPreferences(context);
        sensBar.setProgress(settings.getInt("sensWarning", 1));


        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity)getActivity()).getClientList();
                //String txt = ((MainActivity)getActivity()).overviewText();
                //txtOverview.setText(txt);
            }
        });

        if (((MainActivity)getActivity()).checkConfig(false) == false) {
            btnScan.setEnabled(false);
        } else {
            //if (DEBUG)  Log.d("Log", "Overviewfrag performClick onCreateView");
            btnScan.performClick();
        }

        return rootView;
    }

    public  void setText(String text) {
        //txtOverview = rootView.findViewById(R.id.txtOverview);
        if (txtOverview != null) { //TODO: Al rotar la pantalla es null y peta
            if (DEBUG) Log.d("Log", "Overviewfrag SetText txtOVerview  not null ");
            txtOverview.setText(text);
        } else {
            if (DEBUG) Log.d("Log", "Overviewfarg SetText  txtOverview null");
        }
    }

    public void update() {
        if (btnScan != null) {
            //if (DEBUG) Log.d("Log", "Overviewfrag UPDATE CLICK");
            btnScan.performClick();
        } else {
            //if (DEBUG) Log.d("Log", "Overviewfrag UPDATE NOCLICK");
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        //if (DEBUG) Log.d("Log", "Overviewfrag OnAttach");
        this.context = context;
    }

    public int getSensBarProgress () {
        sensBarProgress = sensBar.getProgress();
        if (sensBarProgress != settings.getInt("sensWarning", 1)) {
            SharedPreferences.Editor editPrefs = settings.edit();
            editPrefs.putInt("sensWarning", sensBarProgress);
            editPrefs.apply();
        }

        return sensBarProgress;
    }

}
