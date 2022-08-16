package com.keystone.coinlib.coins.MPC.script.callback;

import com.eclipsesource.v8.JavaVoidCallback;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;

/**
 * @author yudenghao
 * @date 2022/7/25
 */

public class JSAwaitCallBack implements JavaVoidCallback {

  private JavaVoidCallback mCallback;

  public void registerCallback(JavaVoidCallback callback) {
    this.mCallback = callback;
  }

  @Override
  public void invoke(V8Object object, V8Array array) {
    if (mCallback != null) {
      mCallback.invoke(object, array);
    }
  }
}
