package com.keystone.coinlib.coins.MPC.kdf;

/**
 * @author yudenghao
 * @date 2022/8/8
 */

public interface Web3JCallback {

    void onStart();

    void onError(String msg);

    void onSuccess(String source);

}
