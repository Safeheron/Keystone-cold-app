package com.keystone.coinlib.coins.MPC.script;

import com.eclipsesource.v8.JavaCallback;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * @author yudenghao
 * @date 2022/7/11
 */

public class SecureRandomJ2JS implements JavaCallback {

  private static final String TAG = "SecureRandomJ2JS";

  @Override
  public Object invoke(V8Object object, V8Array array) {
    int arg = array.getInteger(0);
    boolean isPrime = array.getBoolean(1);
    V8Object res = new V8Object(array.getRuntime());
    byte[] v = getRandom(arg, isPrime);
    String value = bytesToHex(v);
    res.add("value", value);
    return res;
  }

  private byte[] getRandom(int len, boolean isPrime) {
    SecureRandom seedGenerator = new SecureRandom();
    if (isPrime) {
    return BigInteger.probablePrime(len,seedGenerator).toByteArray();
    } else {
    return seedGenerator.generateSeed(len);
    }
  }



  public String bytesToHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for (byte aByte : bytes) {
      String hex = Integer.toHexString(aByte & 0xFF);
      if (hex.length() < 2) {
        sb.append(0);
      }
      sb.append(hex);
    }
    return sb.toString();
  }
}
