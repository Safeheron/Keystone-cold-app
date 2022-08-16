package com.keystone.coinlib.coins.MPC.model;

import com.google.gson.Gson;
import java.io.Serializable;

/**
 * @author yudenghao
 * @date 2022/7/26
 */

public class MPCKeyShard implements Serializable {

  public String x2;

  public Q Q;

  public PailPubKey pailPubKey;

  public String cypher_x1;

  public static class Q implements Serializable {
    public String curve;

    public String x;

    public String y;
  }

  public static class PailPubKey implements Serializable {

    public String n;

    public String g;
  }

  public String toJson() {
    return new Gson().toJson(this);
  }

  public static MPCKeyShard toMPCKeyShard(String json) {
    return new Gson().fromJson(json,MPCKeyShard.class);
  }
}
