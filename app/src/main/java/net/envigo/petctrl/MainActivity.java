package net.envigo.petctrl;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentStatePagerAdapter;
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
    protected SharedPreferences settings = null;

    public List<Fragment> myFragments = new ArrayList<>();
    private ArrayList<String> categories = new ArrayList<>();
    protected OverviewFragment OvFrag;

    int OverViewPos;
    Context context;

    protected MyFragmentPageAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    private WifiUtils wifiUtils;
    protected TabLayout mTabLayout;
    protected String admin_password = null;
    protected String ap_name = null;
    private boolean ShowWelcome = true;
    protected ArrayList<PetClients> PetClientList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("Log", "MainActivity onCreate called" + this);
        setContentView(R.layout.activity_main);

        context = getApplicationContext();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        admin_password = settings.getString("admin_password", null);
        ap_name = settings.getString("ap_name", null);
        ShowWelcome = settings.getBoolean("checkWelcome", true);
        Log.d("Log", "Settings:" + admin_password + " " + ap_name);

        if (checkConfig(true)) {
            wifiUtils = new WifiUtils(context);
            wifiUtils.cfgAP(ap_name, admin_password);

            Log.d("Log", "Main, configuring user ap mode");
        }

        if (savedInstanceState != null) {
            //ashOn = savedInstanceState.getBoolean("isFlashOn");
        }
        //prefs = getSharedPreferences(My_Prefs, MODE_PRIVATE);
        //AutoOn = prefs.getBoolean("AutoOn", true);

        setupTabs();

        requestPermissions();
    }

    public boolean checkConfig(boolean msg) {
        if (admin_password != null && ap_name != null && admin_password.length() >= 6 && ap_name.length() >= 4) {
            return true;
        }
        if (msg) {
            Toast.makeText(context, R.string.configNeed, Toast.LENGTH_SHORT).show();
        }
        return false;
    }
    public void getClientList() {
        if(wifiUtils == null)
            return;

        wifiUtils.getClientList(true, 300, new iScanListener() {
            @Override
            public void onFinishScan(ArrayList<PetClients> clients) {

                /* Trabajamos sobre _tmp para poder asignar la nueva lista para que no de error
                al abrir addnewtab y detectar nuevas clientes
                 */
                ArrayList<PetClients> PetClientList_tmp = PetClientList;

                for (PetClients clientScanResult : clients) {

                    if(PetClientList_tmp.size() > 0) {

                        boolean coincidence = false;
                        for (PetClients result : PetClientList_tmp) {

                            if (result.getHWAddr().equals(clientScanResult.getHWAddr())) {

                                coincidence = true;

                            } else {
                                //Log.d("Log", "Petclientlist exists " );
                            }
                        }
                        if (!coincidence) {
                            //Log.d("Log", "Petclientlist not exists ");
                            PetClientList.add(clientScanResult);
                            Log.d("Log", "assgined slot client" + PetClientList.size());
                            get_client_info(clientScanResult);

                        }
                    } else {
                        //Log.d("Log", "Petclientlist first ");
                        PetClientList.add(clientScanResult);
                        Log.d("Log", "assgined slot client" + PetClientList.size());
                        get_client_info(clientScanResult);

                    }
                    newClient();
                }
            }

        });
    }

    public void get_client_info(final PetClients client) {
        HashMap<String, String> conn_details = new HashMap<>();
        HashMap<String, String> conn_data = new HashMap<>();

        conn_details.put("method", "POST");
        conn_details.put("url", "http://" + client.getIpAddr() + "/client_info");

        conn_data.put("admin_password", admin_password);

        ConnRest conn = new ConnRest(new iConnResult() {
            @Override
            public void onSuccess(JSONObject jsonObject) {
                if (jsonObject == null) {
                    Log.d("Log", "Success without response");

                } else {
                    Log.d("Log", "Success" +  jsonObject.toString() );
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
            }

            @Override
            public void onFailure(Exception e) {
                Log.d("Log", "Conn OnFail: " + e.getMessage());
                //TODO: Borrar PetclientList item failed que metimos en getclientlist si la conexion falla
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
            if (checkConfig(true) == true ) {
                showPairTab();
                Toast.makeText(context, R.string.closeToContinue, Toast.LENGTH_SHORT).show();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);

    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    public class MyFragmentPageAdapter extends FragmentStatePagerAdapter {

        //TODO: Ver para que funciona actualmente pos, por que no sirve para la posicion actual
        public  int pos = 0;

        private Context context;

        //public MyFragmentPageAdapter(Context context, FragmentManager fm, List<Fragment> myFrags, ArrayList<String> cats) {
        public MyFragmentPageAdapter(Context context, FragmentManager fm) {
            super(fm);
            this.context = context;
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
            //Log.d("Log", "SetprimartyItem called" + position + " " + container + " " + object);
        }


        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            //return super.instantiateItem(container, position);
            Log.d("Log", "instantiateItem called, position " + position);

            /*
            if(myInstantiateFragments.size() > position) {
                Fragment f = myInstantiateFragments.get(position);
                if (f != null) {
                    Log.d("Log", "instatiate item reuse");
                    return f;
                }
            }
            */
            Log.d("Log", "instatiate item new");

            Fragment createdFragment = (Fragment) super.instantiateItem(container, position);
            /*
            if (createdFragment instanceof OverviewFragment) {
                OverviewFragment Overfrag = (OverviewFragment) createdFragment;
                Overfrag.update();
            }
            if (createdFragment instanceof  ClientFragment) {
                ClientFragment Clientfrag = (ClientFragment) createdFragment;
                Clientfrag.update();
            }
            */
            //myFragments.set(position, new WeakReference<>(createdFragment));

            myFragments.set(position, createdFragment);

            return createdFragment;

            /*

            Fragment createdFragment = (Fragment) super.instantiateItem(container, position);
            myFragments.set(position, createdFragment);
            return createdFragment;
            */
        }

        @Override
        public Fragment getItem(int position) {
            Log.d("Log", "getItem adapter called " + position);

            return myFragments.get(position);
        }

        @Override
        public int getItemPosition(@NonNull Object object) {

            Log.d("Log","Main getItemPosition called" + object);
            //return super.getItemPosition(object);
            return mSectionsPagerAdapter.POSITION_NONE;
        }

        @Override
        public int getCount() {
            Log.d("Log", "getCount adapter called->" + myFragments.size());

            return myFragments.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            //return super.getPageTitle(position);
            Log.d("Log", "getPageTitle adapter called" + position);
            setPos(position);

            return categories.get(position);
        }

        public int getPos() {
            return pos;
        }

        public void addFragment(Fragment fragment, String title) {
            Log.d("Log", "addFragment: " +getCount() + " " + fragment);
            //myFragments.add(new WeakReference<>(fragment));

            myFragments.add(fragment);
            categories.add( title);
            mSectionsPagerAdapter.notifyDataSetChanged();
        }
        public void removeFragment(int position) {
            myFragments.remove(position);
            categories.remove(position);
            mSectionsPagerAdapter.notifyDataSetChanged();
        }


        @Override
        public void finishUpdate(ViewGroup container) {
            super.finishUpdate(container);
            Log.d("Log", "frag finish Update Called");


            /*
            ArrayList<Fragment> update = new ArrayList<>();
            for (int i=0, n=myFragments.size(); i < n; i++) {
                Fragment f = myFragments.get(i);
                if (f == null) continue;
                int pos = getItemPosition(f);
                while (update.size() <= pos) {
                    update.add(null);
                }
                update.set(pos, f);
            }
            myFragments = update;
            */

        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            super.destroyItem(container, position, object);
            Log.d("Log", "Destroy Item (Fragment) called");
            //myFragments.set(position, null);
        }

        public void setPos(int pos) {
            mSectionsPagerAdapter.pos = pos;
        }
    }

    void setupTabs() {
        mSectionsPagerAdapter = new MyFragmentPageAdapter(this ,getSupportFragmentManager());
        if (ShowWelcome) {
            showWelcomeTab();
        }

        showOverviewTab();

        mViewPager = findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mTabLayout = findViewById(R.id.tabLayout);
        mTabLayout.setupWithViewPager(mViewPager);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("Log", "Main onStop called");
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("Log", "Main activity on destroy called");
        /* TODO; Al salir de config tambien lo desabilita y no interesa, buscar otro modo de al salir desconectar
        if (wifiUtils != null) {
            wifiUtils.disableAP();
        }
        */
    }

    private void showWelcomeTab() {
        WelcomeFragment wf = new WelcomeFragment();
        mSectionsPagerAdapter.addFragment(wf, "Bienvenido");
    }

    private void showOverviewTab() {
        //Log.d("Log", "showOverviewTab called " + this);

        OverViewPos = myFragments.size();
        mSectionsPagerAdapter.addFragment(OverviewFragment.newInstance(), getString(R.string.overview));
    }

    public void addClientTab(String title, String PetClientID) {
        Log.d("Log", "addclienttab:" + PetClientID);
        //mSectionsPagerAdapter.addFragment(new ClientFragment(), title);
        ClientFragment PetClientFrag = ClientFragment.newInstance(PetClientID);
        mSectionsPagerAdapter.addFragment(PetClientFrag, title);
        PetClientFrag.setupClient();
        overviewText();
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
            mTabLayout.getTabAt(mSectionsPagerAdapter.getCount() -1 ).select();
        }
    }

    void requestPermissions() {
        //if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){

            //requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},  PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            // Cambiar ajustes
            if (!Settings.System.canWrite(getApplicationContext())) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 200);
            }
        }
    }

    public PetClients getClientByID(String ClientID) {
        if (PetClientList != null) {
            for (PetClients result : PetClientList) {
                if (result.getIpAddr() == ClientID) {
                    return result;
                }
            }
        }
        return null;
    }
    public void removeClientByID(String ClientID) {
        if(PetClientList != null) {
            for (PetClients result : PetClientList) {
                if (result.getIpAddr() == ClientID) {
                    Log.d("Log", "removeClientByID: "  + PetClientList.indexOf(result));
                    PetClientList.remove(result);
                }
            }
        }
    }
    public void newClient() {
        overviewText();
    }

    public void overviewText () {
        Log.d("Log","overviewText main");
        String text = "Clientes: " + PetClientList.size() + "\n";

        for (PetClients clientScanResult : PetClientList) {
            text += "Nombre: " + clientScanResult.getName();
            text += " Online: ";
            text += clientScanResult.isReachable() ? "Si" : "No";
            text += "\n";
        }

        OverviewFragment fr = (OverviewFragment) myFragments.get(OverViewPos);
        fr.setText(text);
    }

    protected String getAdminPassword() {
        Log.d("Log", "Get admin password");
        return admin_password;
    }
    protected String getApName() { return ap_name; };

    public void closeCurrentTab() {
        int CurrentPos = mSectionsPagerAdapter.getPos();
        //Log.d("Log", "Close tab called " + CurrentPos);
        mTabLayout.removeTabAt(CurrentPos);
        mSectionsPagerAdapter.removeFragment(CurrentPos);
        overviewText();
    }
}
