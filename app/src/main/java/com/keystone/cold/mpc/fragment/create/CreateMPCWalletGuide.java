package com.keystone.cold.mpc.fragment.create;

import android.os.Bundle;
import android.view.View;
import com.keystone.cold.R;
import com.keystone.cold.databinding.MpcCreateWalletGuideBinding;
import com.keystone.cold.ui.fragment.BaseFragment;

/**
 * @author yudenghao
 * @date 2022/7/6
 */

public class CreateMPCWalletGuide extends BaseFragment<MpcCreateWalletGuideBinding> {

  private final static String GUIDE_HINT =
      "1. Open MPCSnap example website<br />2. Click Create MPC Wallet<br />3. Touch Create MPC Wallet below and then scan the QR code with MPCSnap";

  @Override
  protected int setView() {
    return R.layout.mpc_create_wallet_guide;
  }

  @Override
  protected void init(View view) {
    initViews();
  }

  private void initViews() {
    mBinding.text2.setText(GUIDE_HINT);
    mBinding.toolbar.setNavigationOnClickListener(v -> navigateUp());
    mBinding.createMpcWallet.setOnClickListener(v -> navigate(R.id.action_create_wallet));
    mBinding.importMpcWallet.setOnClickListener(v -> {
      Bundle bundle = new Bundle();
      bundle.putInt("type",0);
      navigate(R.id.action_create_to_import_mpc_wallet,bundle);
    });
  }

  @Override
  protected void initData(Bundle savedInstanceState) {

  }
}
