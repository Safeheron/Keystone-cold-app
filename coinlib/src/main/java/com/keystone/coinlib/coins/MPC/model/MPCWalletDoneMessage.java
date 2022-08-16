package com.keystone.coinlib.coins.MPC.model;

/**
 * @author yudenghao
 * @date 2022/7/25
 */

public class MPCWalletDoneMessage {
  public byte[] qrCode;
  public String keyShard;
  public String address;

  public MPCWalletDoneMessage(byte[] qrCode, String keyShard, String address) {
    this.qrCode = qrCode;
    this.keyShard = keyShard;
    this.address = address;
  }
}
