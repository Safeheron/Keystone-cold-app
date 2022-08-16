package com.keystone.cold.mpc.fragment.create;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProviders;
import com.keystone.coinlib.coins.MPC.model.CreateMPCWalletMessageWhat;
import com.keystone.coinlib.coins.MPC.model.GenerateRASMessage;
import com.keystone.coinlib.coins.MPC.model.MPCKeyShard;
import com.keystone.coinlib.coins.MPC.model.MPCWalletDoneMessage;
import com.keystone.coinlib.coins.MPC.sign.MPCCoSign;
import com.keystone.coinlib.coins.MPC.wallet.CreateMPCWallet;
import com.keystone.cold.AppExecutors;
import com.keystone.cold.R;
import com.keystone.cold.Utilities;
import com.keystone.cold.databinding.DisplayQrCodeBindingImpl;
import com.keystone.cold.mpc.db.MPCDatabase;
import com.keystone.cold.mpc.db.dao.MPCWalletDao;
import com.keystone.cold.mpc.db.entity.MPCWalletEntity;
import com.keystone.cold.ui.MainActivity;
import com.keystone.cold.ui.fragment.BaseFragment;
import com.keystone.cold.ui.fragment.main.scan.scanner.ScanResult;
import com.keystone.cold.ui.fragment.main.scan.scanner.ScanResultTypes;
import com.keystone.cold.ui.fragment.main.scan.scanner.ScannerState;
import com.keystone.cold.ui.fragment.main.scan.scanner.ScannerViewModel;
import com.sparrowwallet.hummingbird.UR;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import org.json.JSONObject;

import static com.keystone.cold.ui.fragment.setting.MainPreferenceFragment.SETTING_CHOOSE_WATCH_WALLET;

/**
 * @author yudenghao
 * @date 2022/7/12
 */

public class DisPlayQrCodeFragment extends BaseFragment<DisplayQrCodeBindingImpl> {


  private CreateMPCWallet mCreateMPCWallet;

  private WeakReferenceHandler mHandler = new WeakReferenceHandler(this);

  @Override
  protected int setView() {
    return R.layout.display_qr_code;
  }

  @Override
  protected void init(View view) {
    mCreateMPCWallet = CreateMPCWallet.getInstance();
    mBinding.toolbar.setNavigationIcon(null);
    initDao();
    execStep1();
    initListener();
    updateHint();
  }

  private MPCWalletDao initDao() {
    MPCDatabase database = MPCDatabase.getInstance(mActivity);
    return database.getMPCWalletDao();
  }

  @Override
  protected void initData(Bundle savedInstanceState) {

  }

  public void updateHint() {
    int currentStep = mCreateMPCWallet.getCurrentStep();
    boolean done = currentStep == 3;
    mBinding.next.setText(done ? "Done" : "Next");
    mBinding.cancel.setVisibility(View.VISIBLE);
    switch (currentStep) {
      case 0:
      case 1:
        mBinding.text1.setText(setStepHint(1));
        break;
      case 2:
        mBinding.text1.setText(setStepHint(2));
        break;
      case 3:
        mBinding.text1.setText(setStepHint(3));
        break;
    }
  }

  public SpannableString setStepHint(int step) {
    SpannableString span = new SpannableString("Step " + step + "/3 Scan the QR code with MPCSnap");
    StyleSpan styleSpan = new StyleSpan(Typeface.BOLD);
    span.setSpan(styleSpan,5, 6, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    span.setSpan(new AbsoluteSizeSpan(sp2px(17)),5, 6, Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
    span.setSpan(new ForegroundColorSpan(Color.parseColor("#00CDC3")), 5, 6, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    return span;
  }

  private  int sp2px( float spValue) {
    final float fontScale = mActivity.getResources().getDisplayMetrics().scaledDensity;
    return (int) (spValue * fontScale + 0.5f);
  }


  private void initListener() {
    mBinding.next.setOnClickListener(v -> {
      if (mCreateMPCWallet.getCurrentStep() != 3) {
        execScan();
      } else {
        stepIntoMainActivity();
      }
    });
    mBinding.cancel.setOnClickListener(v -> restart());
  }


  private void stepIntoMainActivity() {
    mCreateMPCWallet.reset();
    Utilities.getPrefs(mActivity).edit().putString(SETTING_CHOOSE_WATCH_WALLET, "mpcSnap").apply();
    Intent intent = new Intent(mActivity, MainActivity.class);
    mActivity.startActivity(intent);
    mActivity.finish();
  }

  private void restart() {
    mCreateMPCWallet.reset();
    navigateUp();
  }

  private void execStep1() {
    mCreateMPCWallet.displayQrCode(mHandler);
    if (!mCreateMPCWallet.isStarted()) {
      mCreateMPCWallet.updateStep();
      new Thread(mCreateMPCWallet).start();
    } else {
      if (mCreateMPCWallet.getCurrentDisplayRQCode() != null) {
        displayQRCode(mCreateMPCWallet.getCurrentDisplayRQCode());
      }
    }
  }

  private void execScan() {
    ScannerState scannerState = new ScannerState() {
      @Override
      public void handleScanResult(ScanResult result) {
        try {
          if (result.getType().equals(ScanResultTypes.UR_BYTES)) {
            byte[] bytes = (byte[]) result.resolve();
            if (bytes != null && bytes.length > 0) {
              MPCWalletEntity entity = new MPCWalletEntity();
              switch (mCreateMPCWallet.getCurrentStep()) {
                case 1:
                  String res = new String(bytes);
                  JSONObject jsonObject = new JSONObject(res);
                  String pubKey = jsonObject.getString("pubkey1");
                  entity.id = 1;
                  entity.pub1Key = pubKey;
                  AppExecutors.getInstance().diskIO().execute(() -> {
                    MPCWalletDao dao = initDao();
                    dao.updatePub1Key(pubKey);
                  });
                  mCreateMPCWallet.registerPublicKey(pubKey);
                  mCreateMPCWallet.updateStep();
                  mCreateMPCWallet.scanQrCodeResult(jsonObject.getString("message1"));
                  break;
                case 2:
                  mCreateMPCWallet.updateStep();
                  mCreateMPCWallet.scanQrCodeResult(Base64.getEncoder().encodeToString(bytes));
                  break;
              }
              navigate(R.id.scan_to_display_qr_code);
            }
          }
        } catch (Exception e) {
          Message message = Message.obtain();
          message.what = CreateMPCWalletMessageWhat.SCAN_FAILED;
          message.obj = e.getMessage();
          mHandler.sendMessage(message);
          e.printStackTrace();
        }
      }
    };
    List<ScanResultTypes> desiredResults =
        new ArrayList<>(Collections.singletonList(ScanResultTypes.UR_BYTES));
    scannerState.setDesiredResults(desiredResults);
    ScannerViewModel scannerViewModel =
        ViewModelProviders.of(mActivity).get(ScannerViewModel.class);
    scannerViewModel.setState(scannerState);
    navigate(R.id.display_to_scan_qr_code);
  }

  private void displayQRCode(byte[] bytes) {
    try {
      if (bytes != null && bytes.length > 0) {
        UR ur = UR.fromBytes(bytes);
        mBinding.dynamicQrcodeLayout.qrcode.displayUR(ur);
        mCreateMPCWallet.setCurrentDisplayRQCode(bytes);
        mBinding.next.setEnabled(true);
      }
    } catch (UR.InvalidTypeException | UR.InvalidCBORException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    mHandler.removeCallbacksAndMessages(null);
    mHandler = null;
    if (MPCCoSign.getInstance() != null) {
      MPCCoSign.getInstance().reset();
    }
    Log.i(TAG, "onDestroy");
  }

  private void alertError(@NonNull Message msg) {
    try {
      String res = (String) msg.obj;
      alert("Failed", res, this::restart);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static class WeakReferenceHandler extends Handler {

    private final WeakReference<DisPlayQrCodeFragment> mWeakReference;

    public WeakReferenceHandler(DisPlayQrCodeFragment target) {
      mWeakReference = new WeakReference<>(target);
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
      final DisPlayQrCodeFragment target = mWeakReference.get();
      if (target != null) {
        switch (msg.what) {
          case CreateMPCWalletMessageWhat.RSA_RESULT:
            GenerateRASMessage rsa = (GenerateRASMessage) msg.obj;
            AppExecutors.getInstance().diskIO().execute(() -> {
              MPCWalletDao dao = target.initDao();
              MPCWalletEntity entity = new MPCWalletEntity();
              entity.pri2Key = rsa.priKey;
              entity.pub2Key = rsa.pubKey;
              dao.inset(entity);
            });
            byte[] bytes = rsa.pubKey.getBytes();
            target.mCreateMPCWallet.registerPublicKey(rsa.pubKey);
            target.displayQRCode(bytes);
            break;
          case CreateMPCWalletMessageWhat.MPC_SUCCESS:
            String res = (String) msg.obj;
            target.displayQRCode(res.getBytes());
            target.updateHint();
            break;
          case CreateMPCWalletMessageWhat.MPC_DONE:
            MPCWalletDoneMessage messageDone = (MPCWalletDoneMessage) msg.obj;
            MPCKeyShard keyShard = MPCKeyShard.toMPCKeyShard(messageDone.keyShard);
            AppExecutors.getInstance()
                .diskIO()
                .execute(() -> target.initDao()
                    .updateKeyShard(keyShard.x2, keyShard.cypher_x1, keyShard.Q.curve, keyShard.Q.x,
                        keyShard.Q.y, keyShard.pailPubKey.n, keyShard.pailPubKey.g,
                        messageDone.address));
            target.displayQRCode(messageDone.qrCode);
            target.updateHint();
            break;
          case CreateMPCWalletMessageWhat.MPC_FAILED:
          default:
            target.alertError(msg);
        }
      }
    }
  }
}
