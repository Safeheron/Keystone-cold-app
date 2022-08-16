package com.keystone.cold.mpc.fragment.sign;

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
import com.keystone.coinlib.coins.MPC.model.MPCCoSignMessageWhat;
import com.keystone.coinlib.coins.MPC.sign.MPCCoSign;
import com.keystone.cold.R;
import com.keystone.cold.databinding.MpcSignDisplayQrCodeBindingImpl;
import com.keystone.cold.ui.fragment.BaseFragment;
import com.keystone.cold.ui.fragment.main.scan.scanner.ScanResult;
import com.keystone.cold.ui.fragment.main.scan.scanner.ScanResultTypes;
import com.keystone.cold.ui.fragment.main.scan.scanner.ScannerState;
import com.keystone.cold.ui.fragment.main.scan.scanner.ScannerViewModel;
import com.sparrowwallet.hummingbird.UR;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author yudenghao
 * @date 2022/7/26
 */

public class SignDisplayQRCodeFragment extends BaseFragment<MpcSignDisplayQrCodeBindingImpl> {

  private final MPCCoSign mCoSignEcdsa = MPCCoSign.getInstance();
  private WeakReferenceHandler mHandler = new WeakReferenceHandler(this);

  @Override
  protected int setView() {
    return R.layout.mpc_sign_display_qr_code;
  }

  @Override
  protected void init(View view) {
    mBinding.toolbar.setNavigationIcon(null);
    mCoSignEcdsa.displayQrCode(mHandler);
    initListener();
    updateHint();
  }

  private void displayQRCode(byte[] bytes) {
    try {
      if (bytes != null && bytes.length > 0) {
        UR ur = UR.fromBytes(bytes);
        mBinding.dynamicQrcodeLayout.qrcode.displayUR(ur);
        mCoSignEcdsa.setCurrentDisplayRQCode(bytes);
        mBinding.next.setEnabled(true);
      }
    } catch (UR.InvalidTypeException | UR.InvalidCBORException e) {
      e.printStackTrace();
    }
  }

  private void alertError(@NonNull Message msg) {
    try {
      String res = (String) msg.obj;
      alert("Failed", res, this::restart);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void restart() {
    mCoSignEcdsa.reset();
    navigateUp();
  }

  private void updateHint() {
    mBinding.cancel.setVisibility(View.VISIBLE);
    if (mCoSignEcdsa.getCurrentDisplayRQCode() != null) {
      displayQRCode(mCoSignEcdsa.getCurrentDisplayRQCode());
    }
    int currentStep = mCoSignEcdsa.getCurrentStep();
    boolean done = currentStep == 2;
    mBinding.next.setText(done ? "Done" : "Next");
    switch (currentStep) {
      case 0:
      case 1:
        mBinding.text1.setText(setStepHint(1));
        break;
      case 2:
        mBinding.text1.setText(setStepHint(2));
        break;
    }
  }

  public SpannableString setStepHint(int step) {
    SpannableString span = new SpannableString("Step " + step + "/2 Scan the QR code with MPCSnap");
    StyleSpan styleSpan = new StyleSpan(Typeface.BOLD);
    span.setSpan(styleSpan,5, 6, Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
    span.setSpan(new AbsoluteSizeSpan(sp2px(17)),5, 6, Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
    span.setSpan(new ForegroundColorSpan(Color.parseColor("#00CDC3")), 5, 6, Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
    return span;
  }

  private  int sp2px( float spValue) {
    final float fontScale = mActivity.getResources().getDisplayMetrics().scaledDensity;
    return (int) (spValue * fontScale + 0.5f);
  }


  private void initListener() {
    mBinding.next.setOnClickListener(v -> {
      if (mCoSignEcdsa.getCurrentStep() != 2) {
        execScan();
      } else {
        restart();
      }
    });
    mBinding.cancel.setOnClickListener(v -> restart());
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

  private void execScan() {
    ScannerState scannerState = new ScannerState() {
      @Override
      public void handleScanResult(ScanResult result) {
        try {
          if (result.getType().equals(ScanResultTypes.UR_BYTES)) {
            byte[] bytes = (byte[]) result.resolve();
            if (bytes != null && bytes.length > 0) {
              String res = new String(bytes);
              switch (mCoSignEcdsa.getCurrentStep()) {
                case 1:
                  mCoSignEcdsa.updateStep();
                  mCoSignEcdsa.scanQrCodeResult(res);
                  break;
                case 2:
                  mCoSignEcdsa.scanQrCodeResult(res);
                  break;
              }
              navigate(R.id.scan_to_sign_qr_code);
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
    navigate(R.id.sign_display_to_scan);
  }

  @Override
  protected void initData(Bundle savedInstanceState) {

  }

  private static class WeakReferenceHandler extends Handler {

    private final WeakReference<SignDisplayQRCodeFragment> mWeakReference;

    public WeakReferenceHandler(SignDisplayQRCodeFragment target) {
      mWeakReference = new WeakReference<>(target);
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
      final SignDisplayQRCodeFragment target = mWeakReference.get();
      if (target != null) {
        if (msg.what == MPCCoSignMessageWhat.MPC_SUCCESS) {
          String res = (String) msg.obj;
          target.displayQRCode(res.getBytes());
          target.updateHint();
        } else {
          target.alertError(msg);
        }
      }
    }
  }
}
