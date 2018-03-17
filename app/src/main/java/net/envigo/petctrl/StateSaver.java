package net.envigo.petctrl;

import java.util.ArrayList;

/**
 * Created by diego on 16/03/18.
 *
 */

class StateSaver {

    private ArrayList<PetClients> PetClientList = new ArrayList<>();
    private int TabPosition = 0;

    StateSaver() {}

    void setPetClientList(ArrayList<PetClients> clientListArray) {
        PetClientList = clientListArray;
    }

    ArrayList<PetClients> getPetClientList() {
        return PetClientList;
    }

    void setTabPosition(int tabPosition) {
        this.TabPosition = tabPosition;
    }
    int getTabPosition() {
        return TabPosition;
    }
}
