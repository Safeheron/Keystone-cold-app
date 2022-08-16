package com.keystone.coinlib.coins.MPC.script;

import com.eclipsesource.v8.JavaVoidCallback;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Function;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.V8TypedArray;
import java.util.Arrays;

/**
 * @author yudenghao
 * @date 2022/7/19
 */

public class V8Script {

  private V8Object mV8Object;

  private V8 mV8;

  private V8Function mFunction;

  private V8TypedArray mV8TypedArray;

  private V8Array mArgs;

  private JavaVoidCallback mSuccessCallback;

  private JavaVoidCallback mFailedCallback;

  public V8Script(V8 v8) {
    this.mV8 = v8;
  }


  public V8Script(V8Object v8Object) {
    this.mV8Object = v8Object;
  }

  public V8Object execNewObject(String script) {
    return mV8.executeObjectScript(script);
  }


  public V8Script obj(String objName) {
    mV8Object = (V8Object) mV8.get(objName);
    return this;
  }

  public V8Script get(String objName) {
    mV8Object = (V8Object) mV8Object.get(objName);
    return this;
  }

  public V8Script function(String name) {
    this.mFunction = (V8Function) mV8Object.get(name);
    this.mV8 = mFunction.getRuntime();
    mArgs = new V8Array(mV8);
    return this;
  }

  public void close() {
    if (mFunction != null) {
      mFunction.close();
    }
    if (mArgs != null) {
      mArgs.close();
    }
  }

  public JavaVoidCallback getSuccessCallback() {
    return mSuccessCallback;
  }

  public JavaVoidCallback getFailedCallback() {
    return mFailedCallback;
  }

  public String stringify(V8Object object) {
    V8 v8 = object.getRuntime();
    V8Array parameters = new V8Array(v8).push(object);
    V8Object json = v8.getObject("JSON");
    v8.registerResource(parameters);
    v8.registerResource(json);
    String res = json.executeStringFunction("stringify", parameters);
    parameters.close();
    json.close();
    return res;
  }

  public V8Script push(byte[] bytes) {
    String array = Arrays.toString(bytes);
    String script = "new Uint8Array(" + array + ")";
    mArgs.push(mV8.executeObjectScript(script));
    return this;
  }

  public V8Script push(String str) {
    mArgs.push(str);
    return this;
  }


  public V8Object execObjectFunction() {
    return (V8Object) mFunction.call(mV8Object, mArgs);
  }


  public String execStringFunction() {
    return (String) mFunction.call(mV8Object, mArgs);
  }


  public byte[] toBytes() {
    if (mV8TypedArray != null) {
      return mV8TypedArray.getBytes(0, mV8TypedArray.length());
    } else {
      return null;
    }
  }

  public V8Script execThen(String callback) {
    async("then", callback);
    return this;
  }

  public V8Script thenAwait(JavaVoidCallback callback) {
    this.mSuccessCallback = callback;
    return this;
  }

  public void catchAwait(JavaVoidCallback callback) {
    this.mFailedCallback = callback;
  }

  public V8Script execCatch(String callback) {
    async("catch", callback);
    return this;
  }

  private void async(String fun, String callback) {
    V8 v8 = mV8Object.getRuntime();
    V8Function then = (V8Function) mV8Object.get(fun);
    V8Object window = (V8Object) v8.get("window");
    Object onCreateSuccess = window.get(callback);
    V8Array thenCallBack = new V8Array(v8).push(onCreateSuccess);
    then.call(mV8Object, thenCallBack);
    thenCallBack.close();
    then.close();
  }
}
