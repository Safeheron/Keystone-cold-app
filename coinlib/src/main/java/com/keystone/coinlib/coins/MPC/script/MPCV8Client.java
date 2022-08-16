package com.keystone.coinlib.coins.MPC.script;

import com.eclipsesource.v8.JavaVoidCallback;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Object;
import com.keystone.coinlib.coins.MPC.script.callback.JSAwaitCallBack;

/**
 * @author yudenghao
 * @date 2022/7/11
 */

public class MPCV8Client {

  private final static String FUNCTION_NAME_RANDOM = "__randomHex__";
  public final static String FUNCTION_NAME_SUCCESS = "onCreateSuccess";
  public final static String FUNCTION_NAME_FAILED = "onCreateFailed";

  private final JSAwaitCallBack mOnSuccessCallback = new JSAwaitCallBack();

  private final JSAwaitCallBack mOnFailedCallback = new JSAwaitCallBack();


  public MPCV8Client(V8 v8) {
    v8.registerJavaMethod(new SecureRandomJ2JS(), FUNCTION_NAME_RANDOM);
    v8.registerJavaMethod(mOnSuccessCallback, FUNCTION_NAME_SUCCESS);
    v8.registerJavaMethod(mOnFailedCallback, FUNCTION_NAME_FAILED);
  }

  public void execAwait(V8Script v8Script) {
    JavaVoidCallback success = v8Script.getSuccessCallback();
    JavaVoidCallback failed = v8Script.getFailedCallback();
    if (success != null) {
      mOnSuccessCallback.registerCallback(success);
    }
    if (failed != null) {
      mOnFailedCallback.registerCallback(failed);
    }
    v8Script.execThen(FUNCTION_NAME_SUCCESS).execCatch(FUNCTION_NAME_FAILED);
  }


  public void executeScript(V8Object v8Object) {
    if (v8Object != null) {
      new V8Script(v8Object).execThen(FUNCTION_NAME_SUCCESS).execCatch(FUNCTION_NAME_FAILED);
    }
  }

  public final static class Builder {

    private final V8 mV8;

    private String mScript;

    public Builder(V8 v8) {
      this.mV8 = v8;
    }

    public Builder script(String script) {
      this.mScript = script;
      return this;
    }

    public MPCV8Client build() {
      return new MPCV8Client(mV8);
    }
  }
}
