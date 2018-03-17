package net.envigo.petctrl;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class OverviewFragment extends Fragment {

    private static final boolean DEBUG = false;
    protected SharedPreferences settings;
    private Context context;
    public Button btnScan;
    public TextView txtOverview;
    private TextView txtRefreshTime;

    private SeekBar sensBar;
    private SeekBar refreshTime;

    MainActivity activity;

    public OverviewFragment() {}

    public static OverviewFragment newInstance() {
        //OverviewFragment fragment = new OverviewFragment();
        //return fragment;
        return new OverviewFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG) Log.d("Log", "OverviewFragment onCreate");
    }

    @Override
    public View onCreateView(@NonNull  LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (DEBUG) Log.d("Log", "Overviewfrag onCreateView");
        View rootView;

        rootView = inflater.inflate(R.layout.fragment_overview, container, false);
        txtOverview = rootView.findViewById(R.id.txtOverview);
        txtRefreshTime = rootView.findViewById(R.id.txtRefreshTime);

        btnScan = rootView.findViewById(R.id.btnScan);

        sensBar = rootView.findViewById(R.id.sensBar);
        refreshTime = rootView.findViewById(R.id.refreshTime);

        settings = PreferenceManager.getDefaultSharedPreferences(context);
        sensBar.setProgress(settings.getInt("sensWarning", 1));
        refreshTime.setProgress(settings.getInt("refreshTime", 1));

        setRefreshTxt();

        refreshTime.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                setRefreshTxt();
                SharedPreferences.Editor editPrefs = settings.edit();
                editPrefs.putInt("refreshTime", progress );
                editPrefs.apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("Log", "Overviewfrag: btnScan click called");
                if (activity != null) {
                    activity.ShowProgressDialog();
                    activity.getClientList();
                }
                //String txt = activity.overviewText();
                //txtOverview.setText(txt);
            }
        });

        if(!activity.checkConfig()) {
            Toast.makeText(context, R.string.configNeed, Toast.LENGTH_SHORT).show();
            btnScan.setEnabled(false);
        } else {
            btnScan.performClick();
        }

        return rootView;
    }

    public  void setText(String text) {
        if (txtOverview != null) {
            txtOverview.setText(text);
        } else {
            Log.e("Log", "Overviewfarg SetText  txtOverview null");
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (DEBUG) Log.d("Log", "Overviewfrag OnAttach");
        this.context = context;
        activity = ((MainActivity)getActivity());
    }

    public int getSensBarProgress () {
        int sensBarProgress;
        sensBarProgress = sensBar.getProgress();
        if (sensBarProgress != settings.getInt("sensWarning", 1)) {
            SharedPreferences.Editor editPrefs = settings.edit();
            editPrefs.putInt("sensWarning", sensBarProgress);
            editPrefs.apply();
        }

        return sensBarProgress;
    }

    public void setRefreshTxt() {
        String label = getString(R.string.refreshTime) + ": ";

        int point = refreshTime.getProgress();

        if (point == 0) {
            label = label + "5s";
        } else if (point == 1) {
            label = label + "10s";
        } else if (point == 2) {
            label = label + "20s";
        } else if (point == 3) {
            label = label + "30s";
        } else if (point == 4) {
            label = label + "1m";
        } else if (point == 5) {
            label = label + "3m";
        }
        txtRefreshTime.setText(label);
    }
}
