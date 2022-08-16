package com.keystone.coinlib.coins.MPC.sign;

import android.os.Handler;

/**
 * @author yudenghao
 * @date 2022/7/19
 */

public interface IMPCCoSignStep {

  void setTransactionDetails(String keyShard);

  void displayQrCode(Handler handler);

  void scanQrCodeResult(String handler);

  void reset();
}
