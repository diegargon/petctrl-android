package net.envigo.petctrl;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Toast;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    //private static final int PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION = 1001;
    private final static boolean DEBUG = true;

    protected SharedPreferences settings = null;

    public List<Fragment> myFragments = new ArrayList<>();
    private ArrayList<String> categories = new ArrayList<>();

    int savedTabPos = 0;

    int OverViewPos;
    Context context;

    protected MyFragmentPageAdapter mSectionsPagerAdapter;
    private WifiUtils wifiUtils;
    protected TabLayout mTabLayout;
    protected String admin_password = null;
    protected String ap_name = null;

    private boolean ShowWelcome = true;
    protected ArrayList<PetClients> PetClientList = new ArrayList<>();

    private Handler mHandler;

    ProgressDialog waitDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG) Log.d("Log", "MainActivity onCreate called" + this);
        setContentView(R.layout.activity_main);

        context = getApplicationContext();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        waitDialog = new ProgressDialog(this);
        waitDialog.setMessage(getString(R.string.waitdialog));
        waitDialog.show();

        settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        admin_password = settings.getString("admin_password", null);
        ap_name = settings.getString("ap_name", null);
        ShowWelcome = settings.getBoolean("checkWelcome", true);


        if (DEBUG) Log.d("Log", "Settings:" + admin_password + " " + ap_name);

        if (checkConfig()) {
            wifiUtils = new WifiUtils(context);
            wifiUtils.cfgAP(ap_name, admin_password);

            if (DEBUG) Log.d("Log", "Main, configuring user ap mode");
        } else {
            Toast.makeText(context, R.string.configNeed, Toast.LENGTH_SHORT).show();
            Log.e("Log", "Main: Minor Error falta configuracion checkConfig return false");
        }

        /*
        if (savedInstanceState != null) {
            ashOn = savedInstanceState.getBoolean("isFlashOn");
        }
        prefs = getSharedPreferences(My_Prefs, MODE_PRIVATE);
        AutoOn = prefs.getBoolean("AutoOn", true);
        */
        setupTabs();

        StateSaver stateSaver = (StateSaver) getLastCustomNonConfigurationInstance();
        if (stateSaver != null) {
            //if(DEBUG) Log.d("Log","Main: StateSaver not null");
            PetClientList = stateSaver.getPetClientList();
            if (PetClientList.size() > 0) {
                openClientTabs();
            }
            savedTabPos = stateSaver.getTabPosition();
        }


        if (waitDialog.isShowing()) waitDialog.dismiss();

        requestPermissions("WRITE_SETTINGS");

        //mHandler = new Handler();
        //mHandler.post(runnableCode);
    }

    private Runnable runnableCode = new Runnable() {
        int i = 0;
        @Override
        public void run() {
            i++;
            if (DEBUG) Log.d("Log", "Mainactivity: Called runnableCode " + i);
            // Repeat this the same runnable code block again another 2 seconds
            mHandler.postDelayed(runnableCode, 2000);
        }
    };

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        //return super.onRetainCustomNonConfigurationInstance();
        if(DEBUG) Log.d("Log", "Main: OnRetain called");

        StateSaver stSaver = new StateSaver();
        stSaver.setPetClientList(PetClientList);
        stSaver.setTabPosition(mTabLayout.getSelectedTabPosition());
        return stSaver;
    }

    public boolean checkConfig() {
        return (admin_password != null && ap_name != null
                && admin_password.length() >= 6 && ap_name.length() >= 4);
        /*
        if (admin_password != null && ap_name != null && admin_password.length() >= 6 && ap_name.length() >= 4) {
            return true;
        }
        return false;
        */
    }

    public void getClientList() {

        if (DEBUG) Log.d("Log", "Main: getClientList called");
        if(wifiUtils == null)
            return;

        wifiUtils.getClientList(true, 300, new iScanListener() {
            @Override
            public void onFinishScan(ArrayList<PetClients> clients) {
                if (DEBUG) Log.d("Log", "MAIN getClientList onFinishScan " );
                /* Trabajamos sobre _tmp para poder asignar la nueva lista para que no de error
                al abrir addnewtab y detectar nuevas clientes
                 */
                ArrayList<PetClients> PetClientList_tmp = PetClientList;

                for (PetClients clientScanResult : clients) {

                    if(PetClientList_tmp.size() > 0) {

                        boolean coincidence = false;
                        for (PetClients result : PetClientList_tmp) {

                            if (result.getHWAddr().equals(clientScanResult.getHWAddr())) {
                                if (DEBUG) Log.d("Log", "MAIN Petclientlist exists ");
                                coincidence = true;

                            }
                        }
                        if (!coincidence) {
                            if (DEBUG) Log.d("Log", "Main Petclientlist not exists ");
                            PetClientList.add(clientScanResult);
                            if (DEBUG)
                                Log.d("Log", "Main assigned slot client" + PetClientList.size());
                            get_client_info(clientScanResult);
                        } else {
                            if (waitDialog.isShowing()) waitDialog.dismiss();
                        }
                    } else {
                        PetClientList.add(clientScanResult);
                        if (DEBUG) Log.d("Log", "Main assigned slot client" + PetClientList.size());
                        get_client_info(clientScanResult);

                    }
                    OverviewUpdate();
                }
            }

        });
    }

    public void openClientTabs ()  {
        for (PetClients client : PetClientList) {
            get_client_info(client);
        }
    }

    public void get_client_info(final PetClients client) {
        if (DEBUG) Log.d("Log", "Main: Get client info called");
        HashMap<String, String> conn_details = new HashMap<>();
        HashMap<String, String> conn_data = new HashMap<>();

        conn_details.put("method", "POST");
        conn_details.put("url", "http://" + client.getIpAddr() + "/client_info");

        conn_data.put("admin_password", admin_password);

        ConnRest conn = new ConnRest(new iConnResult() {
            @Override
            public void onSuccess(JSONObject jsonObject) {
                if (jsonObject == null) {
                    if (DEBUG) Log.d("Log", "MAIN Success with null");

                } else {
                    if (DEBUG) Log.d("Log", "MAIN Success" +  jsonObject.toString() );
                    try {
                        if (jsonObject.getString("status").equals("ok")) {
                            client.setName(jsonObject.getString("name"));
                            client.setChip(jsonObject.getString("chipid"));
                            client.setPhoneNumber(jsonObject.getString("phone"));
                            String RSSI = jsonObject.getString("RSSI");
                            client.setRSSI(Integer.parseInt(RSSI));

                            addClientTab(client.getName(), client.getIpAddr());

                        } else {
                            Toast.makeText(context, jsonObject.getString("msg"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (waitDialog.isShowing()) waitDialog.dismiss();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("Log", "MAIN Conn get_client_info OnFail: " + e.getMessage());
                removeClientByID(client.getIpAddr());
                if (waitDialog.isShowing()) waitDialog.dismiss();
            }
        });

        conn.execute(conn_details, conn_data);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent modifySettings=new Intent(MainActivity.this,SettingsActivity.class);
            startActivity(modifySettings);
            return true;
        }

        if (id == R.id.action_exit) {
            wifiUtils.disableAP();
            finish();
            return true;
        }
        if (id == R.id.action_pairdevices) {
            if (checkConfig()) {
                showPairTab();
                Toast.makeText(context, R.string.closeToContinue, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, R.string.configNeed, Toast.LENGTH_SHORT).show();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    public class MyFragmentPageAdapter extends FragmentStatePagerAdapter {

        private  int pos = 0;
        private Context context;

        private MyFragmentPageAdapter(Context context, FragmentManager fm) {
            super(fm);
            this.context = context;
        }

        @NonNull
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            //return super.instantiateItem(container, position);
            if(DEBUG) Log.d("Log", "instantiateItem called, position " + position);


            Fragment createdFragment = (Fragment) super.instantiateItem(container, position);

            //myFragments.set(position, new WeakReference<>(createdFragment));
            myFragments.set(position, createdFragment);

            return createdFragment;
        }

        @Override
        public Fragment getItem(int position) {
            return myFragments.get(position);
        }

        @Override
        public int getItemPosition(@NonNull Object object) {
            //return super.getItemPosition(object);
            //return mSectionsPagerAdapter.POSITION_NONE;
            return POSITION_NONE;
        }

        @Override
        public int getCount() {
            return myFragments.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            //return super.getPageTitle(position);
            setPos(position);

            return categories.get(position);
        }



        void addFragment(Fragment fragment, String title) {
            if (DEBUG) Log.d("Log", "MAIN addFragment: " +getCount() + " " + fragment);
            //myFragments.add(new WeakReference<>(fragment));

            myFragments.add(fragment);
            categories.add( title);
            mSectionsPagerAdapter.notifyDataSetChanged();
        }
        void removeFragment(int position) {
            myFragments.remove(position);
            categories.remove(position);
            mSectionsPagerAdapter.notifyDataSetChanged();
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            super.destroyItem(container, position, object);
            //if (DEBUG) Log.d("Log", "MAIN Destroy Item (Fragment) called");
            //myFragments.set(position, null);
        }

        int getPos() {
            return pos;
        }

        void setPos(int pos) {
            mSectionsPagerAdapter.pos = pos;
        }
    }

    void setupTabs() {
        ViewPager mViewPager;
        mSectionsPagerAdapter = new MyFragmentPageAdapter(this ,getSupportFragmentManager());
        mViewPager = findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mTabLayout = findViewById(R.id.tabLayout);
        mTabLayout.setupWithViewPager(mViewPager);

        if (ShowWelcome) showWelcomeTab();

        showOverviewTab();

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (DEBUG) Log.d("Log", "Main onStop called");
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (DEBUG) Log.d("Log", "Main activity on destroy called");

    }

    private void showWelcomeTab() {
        WelcomeFragment wf = new WelcomeFragment();
        mSectionsPagerAdapter.addFragment(wf, "Bienvenido");
    }

    private void showOverviewTab() {
        //if (DEBUG) Log.d("Log", "showOverviewTab called " + this);

        OverViewPos = myFragments.size();
        mSectionsPagerAdapter.addFragment(OverviewFragment.newInstance(), getString(R.string.overview));
    }

    public void addClientTab(String title, String PetClientID) {
        if (DEBUG) Log.d("Log", "MAIN addclienttab:" + PetClientID);
        //mSectionsPagerAdapter.addFragment(new ClientFragment(), title);
        ClientFragment PetClientFrag = ClientFragment.newInstance(PetClientID);
        mSectionsPagerAdapter.addFragment(PetClientFrag, title);
        //PetClientFrag.setupClient();
        //mTabLayout.getTabAt(mSectionsPagerAdapter.getCount() -1).select();
        if ((mSectionsPagerAdapter.getCount() -1) == savedTabPos) {
            mTabLayout.getTabAt(mSectionsPagerAdapter.getCount() -1).select();
        }
        OverviewUpdate();
    }

    private void showPairTab() {
        boolean frag_exists = false;

        for(Fragment f : myFragments) {
            if (f instanceof  PetSettings) {
                frag_exists = true;
            }
        }
        if(!frag_exists) {
            mSectionsPagerAdapter.addFragment(PetSettings.newInstance(), getString(R.string.pair));
            mTabLayout.getTabAt(mSectionsPagerAdapter.getCount() -1).select();
        }
    }

    boolean checkPermissions(String type) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if (type.equals("WRITE_SETTINGS")) {
                if (Settings.System.canWrite(getApplicationContext())) {
                    return true;
                }
            }
            if (type.equals("READ_EXTERNAL")) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED) {
                    return true;
                }
            }
        }
        return false;
    }
    void requestPermissions(String type) {
        //if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){

            //requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},  PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            // Cambiar ajustes
            if (type.equals("WRITE_SETTINGS")) {
                if (!Settings.System.canWrite(getApplicationContext())) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, 200);
                }
            }
            if (type.equals("READ_EXTERNAL")) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {

                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 201);

                }

            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch(requestCode) {
            case 200: { //WRITE/CHANGE SETTINGS (For wifi hotspot)
                if (! (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) ) {
                    Toast.makeText(context, "Esta aplicacion no puede funcionar sin esos permisos", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            case 201: { //READ_EXTERNAL for get/write from gallery
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(context, "Ahora puede intentarlo de nuevo", Toast.LENGTH_SHORT).show();
                } else {
                    //DENIED
                    Toast.makeText(context, "Sin estos permisos no podra subir foto", Toast.LENGTH_SHORT).show();
                }
            }

        }
    }

    public PetClients getClientByID(String ClientID) {
        if (PetClientList != null) {
            for (PetClients result : PetClientList) {
                if (result.getIpAddr().equals(ClientID)) {
                    return result;
                }
            }
        }
        return null;
    }
    public void removeClientByID(String ClientID) {
        if (DEBUG) Log.d("Log", "MAIN removeClientByID called");
        if(PetClientList != null) {
            for (PetClients result : PetClientList) {
                if (result.getIpAddr().equals(ClientID)) {
                    if (DEBUG) Log.d("Log", "MAIN removeClientByID: "  + PetClientList.indexOf(result) + " ip: " + ClientID);
                    PetClientList.remove(result);
                    OverviewUpdate();
                }
            }
        }

    }

    public void OverviewUpdate () {
        if (DEBUG) Log.d("Log","MAIN overview update");

        int num_clients = PetClientList.size();
        String client_details = "";

        //String text += "Clientes: " + PetClientList.size() + "\n";

        if (num_clients > 0) {
            for (PetClients client : PetClientList) {
                if (client.getName() != null) {
                    client_details += "Nombre: " + client.getName();
                    client_details += " Online: ";
                    client_details += client.isReachable() ? "Si" : "No";
                    client_details += "\n";
                } else {
                    num_clients -= 1;
                }
            }
        } else {
            return;
        }
        String text = "Clientes: " + num_clients + "\n";
        text += client_details;

        OverviewFragment fr = (OverviewFragment) myFragments.get(OverViewPos);
        fr.setText(text);
    }

    protected String getAdminPassword() {
        if (DEBUG) Log.d("Log", "MAIN Get admin password");
        return admin_password;
    }
    protected String getApName() { return ap_name; }

    public void closeCurrentTab(Fragment frag) {
        int CurrentPos = mSectionsPagerAdapter.getPos();
        //if (DEBUG) Log.d("Log", "MAIN Close tab called " + CurrentPos);

        if (frag instanceof ClientFragment) {
            ClientFragment clientfr = (ClientFragment) frag;
            removeClientByID(clientfr.PetClientID);
        }
        mTabLayout.removeTabAt(CurrentPos);
        mSectionsPagerAdapter.removeFragment(CurrentPos);

    }

    public int getSensProgress() {
        OverviewFragment fr = (OverviewFragment) myFragments.get(OverViewPos);
        return fr.getSensBarProgress();
    }
}
