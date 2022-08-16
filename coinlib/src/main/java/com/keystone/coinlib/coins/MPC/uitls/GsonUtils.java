package com.keystone.coinlib.coins.MPC.uitls;

import com.google.gson.Gson;

/**
 * @author yudenghao
 * @date 2022/8/9
 */

public class GsonUtils<T> {
  public static String toJson(Object obj) {
    return new Gson().toJson(obj);
  }

  public T toObj(Class<T> tClass, String json) {
    return new Gson().fromJson(json, tClass);
  }
}
