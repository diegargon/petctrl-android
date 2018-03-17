package net.envigo.petctrl;

import java.util.ArrayList;

/**
 * Created by diego on 19/02/18.
 *
 */

public interface iScanListener {
    void onFinishScan(ArrayList<PetClients> clients);
}
