package com.keystone.coinlib.coins.MPC.sign;

import android.os.Handler;
import android.os.Message;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Object;
import com.keystone.coinlib.coins.MPC.model.MPCCoSignMessageWhat;
import com.keystone.coinlib.coins.MPC.model.TransactionDetails;
import com.keystone.coinlib.coins.MPC.script.MPCV8Client;
import com.keystone.coinlib.coins.MPC.script.V8Script;
import com.keystone.coinlib.v8.ScriptLoader;
import java.util.concurrent.CountDownLatch;

/**
 * @author yudenghao
 * @date 2022/7/19
 */

public class MPCCoSign implements IMPCCoSignStep, Runnable {

  private static final String TAG = "CoSignEcdsa";

  private volatile static MPCCoSign instance;

  private boolean mStarted = false;

  private static CountDownLatch mLock = new CountDownLatch(1);

  private Handler mCurrentHandler;

  private Message cacheMessage = null;

  private byte[] currentDisplayRQCode = null;

  private int mCurrentStep = 0;

  private String mMessage;

  private String mPriv2;

  private String mPub1;

  private String address;

  public boolean isStarted() {
    return mStarted;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  private TransactionDetails mTransactionDetails = new TransactionDetails();

  private static String mKeyShardString;

  private MPCCoSign() {
  }

  public void updateStep() {
    mCurrentStep++;
  }

  public byte[] getCurrentDisplayRQCode() {
    return currentDisplayRQCode;
  }

  public void setCurrentDisplayRQCode(byte[] currentDisplayRQCode) {
    this.currentDisplayRQCode = currentDisplayRQCode;
  }

  public void setRSA(String pri, String pub) {
    this.mPriv2 = pri;
    this.mPub1 = pub;
    updateStep();
  }

  public int getCurrentStep() {
    return mCurrentStep;
  }

  public void bindKeyShard(String keyShard) {
    mKeyShardString = keyShard;
  }

  public static MPCCoSign getInstance() {
    if (instance == null) {
      synchronized (MPCCoSign.class) {
        if (instance == null) {
          instance = new MPCCoSign();
        }
      }
    }
    return instance;
  }

  @Override
  public void setTransactionDetails(String keyShard) {
    mTransactionDetails = mTransactionDetails.jsonToTransactionDetails(keyShard);
    scanQrCodeResult(mTransactionDetails.message1);
  }

  @Override
  public void displayQrCode(Handler handler) {
    this.mCurrentHandler = handler;
    if (cacheMessage != null) {
      mCurrentHandler.sendMessage(cacheMessage);
      cacheMessage = null;
    }
  }

  @Override
  public void scanQrCodeResult(String message) {
    currentDisplayRQCode = null;
    this.mMessage = message;
    mLock.countDown();
  }

  @Override
  public void reset() {
    instance = null;
  }

  @Override
  public void run() {
    mStarted = true;
    V8 v8;
    try {
      //初始化V8
      v8 = ScriptLoader.sInstance.loadByCoinCode("MPC");
      MPCV8Client client = new MPCV8Client.Builder(v8).build();
      V8Object createSignerP2 = createSignerP2(v8);
      mLock.await();
      getMessage(client, createSignerP2, 1);
      mLock = new CountDownLatch(1);
      mLock.await();
      getMessage(client, createSignerP2, 2);
    } catch (Exception e) {
      mStarted = false;
      sendMessage(MPCCoSignMessageWhat.MPC_FAILED, e.getMessage());
    }
  }

  private void getMessage(MPCV8Client client, V8Object signP2, int step) {
    V8Script contextFun = new V8Script(signP2).function(step == 1 ? "step1" : "step2");
    if (step == 1) {
      contextFun.push(mTransactionDetails.toJson());
    }
    contextFun.push(mMessage);
    V8Object message = contextFun.execObjectFunction();
    V8Script await = new V8Script(message);
    await.thenAwait((object, array) -> {
      try {
        String res = (String) array.get(0);
        sendMessage(MPCCoSignMessageWhat.MPC_SUCCESS, res);
      } catch (Exception e) {
        sendMessage(MPCCoSignMessageWhat.MPC_FAILED, e.getMessage());
      }
    }).catchAwait((object, array) -> {
      sendMessage(MPCCoSignMessageWhat.MPC_FAILED, array.get(0));
    });
    client.execAwait(await);
  }


  public void sendMessage(int what, Object obj) {
    Message message = Message.obtain();
    message.what = what;
    message.obj = obj;
    if (mCurrentHandler != null) {
      mCurrentHandler.sendMessage(message);
    } else {
      cacheMessage = message;
    }
  }

  private V8Object createSignerP2(V8 v8) {
    String script = "new POC.SignerP2(" + mKeyShardString + ", '" + mPriv2 + "', '" + mPub1 + "')";
    return v8.executeObjectScript(script);
  }
}
