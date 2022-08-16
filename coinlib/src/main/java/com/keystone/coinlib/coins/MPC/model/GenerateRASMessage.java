package com.keystone.coinlib.coins.MPC.model;

import java.io.Serializable;

/**
 * @author yudenghao
 * @date 2022/7/25
 */

public class GenerateRASMessage implements Serializable {

  public String pubKey;

  public String priKey;

  public GenerateRASMessage(String pubKey, String priKey) {
    this.pubKey = pubKey;
    this.priKey = priKey;
  }
}
