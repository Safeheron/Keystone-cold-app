package com.keystone.cold.mpc.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.keystone.coinlib.coins.MPC.model.MPCKeyShard;
import com.keystone.coinlib.coins.MPC.uitls.GsonUtils;

/**
 * @author yudenghao
 * @date 2022/7/29
 */

@Entity(tableName = "mpcWallet" )
public class MPCWalletEntity {
  @PrimaryKey(autoGenerate = true)
  public long id = 1;

  public String pub1Key;

  public String pub2Key;

  public String pri2Key;

  public String x2;

  public String curve;

  public String x;

  public String y;

  public String n;

  public String g;

  public String cypher_x1;

  public String address;

  public  String getKeyShard() {
    MPCKeyShard keyShard = new MPCKeyShard();
    keyShard.x2 = this.x2;
    keyShard.cypher_x1 = this.cypher_x1;
    MPCKeyShard.PailPubKey key = new MPCKeyShard.PailPubKey();
    key.n = this.n;
    key.g = this.g;
    keyShard.pailPubKey = key;
    MPCKeyShard.Q q = new MPCKeyShard.Q();
    q.curve = this.curve;
    q.x = this.x;
    q.y = this.y;
    keyShard.Q = q;
    return keyShard.toJson();
  }

  public String toJson() {
    return GsonUtils.toJson(this);
  }


}

