package com.keystone.cold.mpc.fragment.address;

import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import androidx.fragment.app.Fragment;
import com.keystone.cold.AppExecutors;
import com.keystone.cold.R;
import com.keystone.cold.databinding.MpcWalletAddressBinding;
import com.keystone.cold.mpc.db.MPCDatabase;
import com.keystone.cold.mpc.db.dao.MPCWalletDao;
import com.keystone.cold.mpc.db.entity.MPCWalletEntity;
import com.keystone.cold.ui.fragment.BaseFragment;
import com.keystone.cold.Utilities;

/**
 * @author yudenghao
 * @date 2022/8/4
 */

public class MPCWalletAddressFragment extends BaseFragment<MpcWalletAddressBinding> {

  @Override
  protected int setView() {
    return R.layout.mpc_wallet_address;
  }

  @Override
  protected void init(View view) {
    initDao();
    if (!Utilities.isExportMPCWallet(mActivity)) {
      String text = "Important! MPC Wallet not backed up! Click <font color = 'blue'>here</font> to back up.";
      mBinding.backUpHint.setVisibility(View.VISIBLE);
      mBinding.backUpHint.setText(Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY));
      mBinding.backUpHint.setOnClickListener(new View.OnClickListener() {

        @Override
        public void onClick(View v) {
          Bundle bundle = new Bundle();
          bundle.putInt("type",1);
          navigate(R.id.action_address_to_export_mpc_wallet,bundle);
        }
      });
    } else  {
      mBinding.backUpHint.setVisibility(View.GONE);
    }
  }

  @Override
  protected void initData(Bundle savedInstanceState) {

  }

  public static Fragment newInstance() {
    return new MPCWalletAddressFragment();
  }

  public String getAddress() {
    return mBinding.addr.getText().toString();
  }

  private void initDao() {
    AppExecutors.getInstance().diskIO().execute(new Runnable()  {

      @Override
      public void run() {
        Context context = getActivity();
        MPCDatabase database = MPCDatabase.getInstance(context);
        MPCWalletDao mDao = database.getMPCWalletDao();
        MPCWalletEntity wallets = mDao.loadWallet();
        AppExecutors.getInstance().mainThread().execute(new Runnable() {

          @Override
          public void run() {
            if (wallets != null) {
              mBinding.addr.setText(wallets.address);
            }
          }
        });
      }
    });
  }
}
