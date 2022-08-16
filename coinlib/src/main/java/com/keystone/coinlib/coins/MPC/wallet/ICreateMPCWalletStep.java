package com.keystone.coinlib.coins.MPC.wallet;

import android.os.Handler;

/**
 * @author yudenghao
 * @date 2022/7/11
 */

public interface ICreateMPCWalletStep {

  void registerPublicKey(String publicKey);

  void displayQrCode(Handler handler);

  void scanQrCodeResult(String message);

  void reset();
}
