package net.envigo.petctrl;

import org.json.JSONObject;

/**
 * Created by diego on 8/03/18.
 */

public interface iConnResult<T> {
    //void onSuccess(T object);
    void onSuccess(JSONObject jsonObject);
    void onFailure(Exception e);
}
