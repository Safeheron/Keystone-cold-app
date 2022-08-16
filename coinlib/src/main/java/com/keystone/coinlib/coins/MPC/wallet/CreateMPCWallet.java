package com.keystone.coinlib.coins.MPC.wallet;

import android.os.Handler;
import android.os.Message;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Object;
import com.keystone.coinlib.coins.MPC.model.CreateMPCWalletMessageWhat;
import com.keystone.coinlib.coins.MPC.model.GenerateRASMessage;
import com.keystone.coinlib.coins.MPC.model.MPCKeyShard;
import com.keystone.coinlib.coins.MPC.model.MPCWalletDoneMessage;
import com.keystone.coinlib.coins.MPC.script.MPCV8Client;
import com.keystone.coinlib.coins.MPC.script.V8Script;
import com.keystone.coinlib.v8.ScriptLoader;
import java.util.concurrent.CountDownLatch;

/**
 * @author yudenghao
 * @date 2022/7/11
 */

public class CreateMPCWallet implements ICreateMPCWalletStep, Runnable {


  private volatile static CreateMPCWallet instance;

  private static CountDownLatch mLock = new CountDownLatch(1);

  private boolean mStarted = false;

  private int mCurrentStep = 0;

  private byte[] currentDisplayRQCode = null;

  private String mMessage;

  private String nPublicKey;

  private Handler mCurrentHandler;

  private Message mCacheMessage = null;

  private CreateMPCWallet() {
  }

  public static CreateMPCWallet getInstance() {
    if (instance == null) {
      synchronized (CreateMPCWallet.class) {
        if (instance == null) {
          instance = new CreateMPCWallet();
        }
      }
    }
    return instance;
  }

  public boolean isStarted() {
    return mStarted;
  }

  public void updateStep() {
    mCurrentStep++;
  }

  public int getCurrentStep() {
    return mCurrentStep;
  }

  public byte[] getCurrentDisplayRQCode() {
    return currentDisplayRQCode;
  }



  public void setCurrentDisplayRQCode(byte[] currentDisplayRQCode) {
    this.currentDisplayRQCode = currentDisplayRQCode;
  }

  @Override
  public void registerPublicKey(String publicKey) {
    this.nPublicKey = publicKey;
  }

  @Override
  public void displayQrCode(Handler handler) {
    this.mCurrentHandler = handler;
    if (mCacheMessage != null) {
      mCurrentHandler.sendMessage(mCacheMessage);
      mCacheMessage = null;
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
      V8Object dkg = createDkG(v8);
      createContext(client, dkg);
      mLock.await();
      getMessage(client, dkg, 1);
      mLock = new CountDownLatch(1);
      mLock.await();
      getMessage(client, dkg, 2);
    } catch (Exception e) {
      mStarted = false;
      sendMessage(CreateMPCWalletMessageWhat.MPC_FAILED, e.getMessage());
    }
  }

  private void getMessage(MPCV8Client client, V8Object dkg, int step) {
    V8Script contextFun = new V8Script(dkg).function(step == 1 ? "step1" : "step2");
    contextFun.push(mMessage);
    if (step == 1) {
      contextFun.push(nPublicKey);
    }
    V8Object message = contextFun.execObjectFunction();
    V8Script await = new V8Script(message);
    await.thenAwait((object, array) -> {
      try {
        String res = (String) array.get(0);
        if (step == 2) {
          getKeyShard(dkg, res);
        } else {
          sendMessage(CreateMPCWalletMessageWhat.MPC_SUCCESS, res);
        }
      } catch (Exception e) {
        sendMessage(CreateMPCWalletMessageWhat.MPC_FAILED, e.getMessage());
      }
    }).catchAwait((object, array) -> {
      sendMessage(CreateMPCWalletMessageWhat.MPC_FAILED, array.get(0));
    });
    client.execAwait(await);
  }

  public void getKeyShard(V8Object dkg, String doneMessage) {
    V8Script exportKeyShare = new V8Script(dkg);
    V8Script toJsonObject = null;
    try {
      V8Object res = exportKeyShare.function("exportKeyShare2").execObjectFunction();
      toJsonObject = new V8Script(res);
      String mKeyShard = toJsonObject.stringify(res);
      String address = getFromAddress(dkg.getRuntime(), MPCKeyShard.toMPCKeyShard(mKeyShard));
      sendMessage(CreateMPCWalletMessageWhat.MPC_DONE,
          new MPCWalletDoneMessage(doneMessage.getBytes(), mKeyShard,address));
    } catch (Exception e) {
      sendMessage(CreateMPCWalletMessageWhat.MPC_FAILED, e.getMessage());
    } finally {
      exportKeyShare.close();
      if (toJsonObject != null) {
        toJsonObject.close();
      }
    }
  }

  private void createContext(MPCV8Client client, V8Object dkg) {
    V8Script contextFun = new V8Script(dkg).function("createContext");
    V8Object context = contextFun.execObjectFunction();
    V8Script await = new V8Script(context);
    await.thenAwait((object, array) -> {
          try {
            V8Object v8Object = (V8Object) array.get(0);
            String pubKey = (String) v8Object.get("pub");
            String priKey = (String) v8Object.get("priv");
            sendMessage(CreateMPCWalletMessageWhat.RSA_RESULT, new GenerateRASMessage(pubKey, priKey));
          } catch (Exception e) {
            sendMessage(CreateMPCWalletMessageWhat.MPC_FAILED, e.getMessage());
          }
        })
        .catchAwait(
            (object, array) -> sendMessage(CreateMPCWalletMessageWhat.MPC_FAILED, array.get(0)));
    client.execAwait(await);
  }

  private V8Object createDkG(V8 v8) {
    return new V8Script(v8).execNewObject("new POC.DKGP2()");
  }

  private String getFromAddress(V8 v8, MPCKeyShard keyShard) {
    V8Script v8Script = new V8Script((V8Object) v8.get("window"));
    String address = v8Script.get("POC")
        .function("deriveAddressFromCurvePoint")
        .push(keyShard.Q.x)
        .push(keyShard.Q.y)
        .execStringFunction();
    return address;
  }

  public void sendMessage(int what, Object obj) {
    Message message = Message.obtain();
    message.what = what;
    message.obj = obj;
    if (mCurrentHandler != null) {
      mCurrentHandler.sendMessage(message);
    } else {
      mCacheMessage = message;
    }
  }
}
