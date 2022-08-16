package com.keystone.cold.mpc.db.dao;

/**
 * @author yudenghao
 * @date 2022/7/29
 */

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.keystone.cold.mpc.db.entity.MPCWalletEntity;
import java.util.List;

@Dao
public interface MPCWalletDao {

  //todo Important reminder:
  // In order to ensure the security of data,
  // the production environment database needs to be encrypted,
  // and the specific encryption method can be carried out on demand

  @Query("SELECT * FROM mpcWallet")
  List<MPCWalletEntity> loadWallets();

  @Query("SELECT * FROM mpcWallet where id=1")
  MPCWalletEntity loadWallet();

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  void inset(MPCWalletEntity entity);

  @Query("UPDATE mpcWallet SET pub1Key = :pub1Key WHERE id=1")
  void updatePub1Key(String pub1Key);

  @Query("UPDATE mpcWallet SET pri2Key = :pri2Key,pub2Key = :pub2Key WHERE id=1")
  void updateRSA(String pri2Key, String pub2Key);

  @Query("UPDATE mpcWallet SET x2 = :x2,cypher_x1 = :cypher_x1,curve = :curve,x = :x,y = :y,n = :n,g= :g,address = :address WHERE id=1")
  void updateKeyShard(String x2, String cypher_x1, String curve, String x, String y, String n, String g, String address);


  @Query("DELETE from mpcWallet where id=1")
  int deleteWallet();
}
