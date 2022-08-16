package com.keystone.cold.mpc.fragment.sign;

import android.os.Bundle;
import android.view.View;
import androidx.navigation.Navigation;
import com.keystone.coinlib.coins.MPC.model.TransactionDetails;
import com.keystone.coinlib.coins.MPC.sign.MPCCoSign;
import com.keystone.cold.R;
import com.keystone.cold.databinding.MpcTransactionConfirmBindingImpl;
import com.keystone.cold.ui.fragment.BaseFragment;
import com.keystone.cold.ui.views.AuthenticateModal;

import static com.keystone.cold.Utilities.IS_SETUP_VAULT;
import static com.keystone.cold.ui.fragment.setup.SetPasswordFragment.SHOULD_POP_BACK;

/**
 * @author yudenghao
 * @date 2022/7/26
 */

public class MPCTransactionConfirmFragment extends BaseFragment<MpcTransactionConfirmBindingImpl> {

  private final MPCCoSign mMPCCoSign = MPCCoSign.getInstance();


  public void setTransactionDetails(TransactionDetails details) {
    if (details != null) {
      mBinding.network.setText(details.networkName);
      mBinding.value.setText(details.txObj.value + "  ETH");
      mBinding.toAddress.setText(details.txObj.to);
      mBinding.data.setText(details.txObj.data);
      mBinding.free.setText(details.txObj.maxFeePerGas + "  GWei");
      mBinding.priority.setText(details.txObj.maxPriorityFeePerGas + "  GWei");
      mBinding.sign.setEnabled(true);
    }
  }

  private void initDao() {
    Bundle bundle = getArguments();
    if (bundle != null) {
      String address = bundle.getString("address","null");
      mBinding.from.setText(address);
    }
  }

  @Override
  protected int setView() {
    return R.layout.mpc_transaction_confirm;
  }

  @Override
  protected void init(View view) {
    initDao();
    if (getArguments() != null) {
      String json = getArguments().getString("objText",null);
      if (json != null) {
        setTransactionDetails(new TransactionDetails().jsonToTransactionDetails(json));
      }
    }
    mBinding.toolbar.setNavigationOnClickListener(v -> restart());
    mBinding.sign.setOnClickListener(v -> {
      AuthenticateModal.show(mActivity,
          mActivity.getString(R.string.password_modal_title),
          "",
          password -> {
            navigate(R.id.confirm_to_sign);
          },
          () -> {
            Bundle data = new Bundle();
            data.putBoolean(IS_SETUP_VAULT, true);
            data.putBoolean(SHOULD_POP_BACK, true);
            Navigation.findNavController(mActivity, R.id.nav_host_fragment)
                .navigate(R.id.global_action_to_setPasswordFragment, data);
          }
      );
    });
  }

  private void restart() {
    mMPCCoSign.reset();
    navigateUp();
  }

  @Override
  protected void initData(Bundle savedInstanceState) {
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
  }

}
