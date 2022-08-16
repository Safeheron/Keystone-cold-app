package com.keystone.cold.mpc.fragment.imp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.view.View;
import com.keystone.coinlib.coins.MPC.kdf.Web3JCallback;
import com.keystone.coinlib.coins.MPC.kdf.Web3JUtils;
import com.keystone.coinlib.coins.MPC.uitls.GsonUtils;
import com.keystone.cold.AppExecutors;
import com.keystone.cold.MainApplication;
import com.keystone.cold.R;
import com.keystone.cold.Utilities;
import com.keystone.cold.databinding.MpcImportWalletBinding;
import com.keystone.cold.mpc.db.MPCDatabase;
import com.keystone.cold.mpc.db.dao.MPCWalletDao;
import com.keystone.cold.mpc.db.entity.MPCWalletEntity;
import com.keystone.cold.sdcard.OnSdcardStatusChange;
import com.keystone.cold.sdcard.SdCardStatusMonitor;
import com.keystone.cold.ui.MainActivity;
import com.keystone.cold.ui.fragment.BaseFragment;
import com.keystone.cold.ui.views.AuthenticateModal;
import com.keystone.cold.update.utils.Storage;
import com.keystone.cold.viewmodel.ElectrumViewModel;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.keystone.cold.ui.fragment.setting.MainPreferenceFragment.SETTING_CHOOSE_WATCH_WALLET;

/**
 * @author yudenghao
 * @date 2022/7/6
 */

public class ImportAndExportMPCWallet extends BaseFragment<MpcImportWalletBinding> {

  @Override
  protected int setView() {
    return R.layout.mpc_import_wallet;
  }

  private boolean isImport;

  private final OnSdcardStatusChange mOnSdcardStatusChange = new OnSdcardStatusChange() {
    @Override
    public String id() {
      return "ImportAndExportMPCWallet";
    }

    @Override
    public void onInsert() {
      mBinding.importMpcWallet.setVisibility(View.GONE);
      mBinding.createMpcWallet.setEnabled(true);
    }

    @Override
    public void onRemove() {
      mBinding.importMpcWallet.setVisibility(View.VISIBLE);
      mBinding.createMpcWallet.setEnabled(false);
    }
  };

  @Override
  public void onResume() {
    super.onResume();
    boolean sdCardExist = isHaveSDCard();
    mBinding.importMpcWallet.setVisibility(sdCardExist ? View.GONE : View.VISIBLE);
    mBinding.createMpcWallet.setEnabled(sdCardExist);
  }

  private boolean isHaveSDCard(){
    boolean result = false;
    StorageManager mStorageManager = (StorageManager) mActivity.getSystemService(Context.STORAGE_SERVICE);
    Class<?> storageVolumeClazz = null;
    try {
      storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");
      Method getVolumeList = mStorageManager.getClass().getMethod("getVolumeList");
      Method isRemovable = storageVolumeClazz.getMethod("isRemovable");
      Method getState = storageVolumeClazz.getMethod("getState");
      Object obj = null;
      try {
        obj = getVolumeList.invoke(mStorageManager);
      } catch (InvocationTargetException e) {
        e.printStackTrace();
      }
      final int length = Array.getLength(obj);
      for (int i = 0; i < length; i++) {
        Object storageVolumeElement = Array.get(obj, i);
        boolean removable = (Boolean) isRemovable.invoke(storageVolumeElement);
        String state = (String) getState.invoke(storageVolumeElement);
        if (removable && state.equals(Environment.MEDIA_MOUNTED)) {
          result = true;
          break;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return result;
  }

  @Override
  protected void init(View view) {
    mBinding.toolbar.setNavigationOnClickListener(v -> navigateUp());
    Bundle bundle = getArguments();
    int type = bundle.getInt("type", 0);
    isImport = type == 0;
    String hint = isImport
        ? "1. Save your existing MPC Wallet Keystore file on a microSD card (FAT32 format).<br/>2. Insert the microSD card into the Keystone."
        : "1. Insert a microSD card(FAT32 format) into the Keystone.<br/>2. Set a password to encrypt your Keystore file";
    mBinding.text1.setText(isImport ? "Import MPC Wallet" : "How to export MPC Wallet:");
    mBinding.text2.setText(hint);
    mBinding.toolbarTitle.setText(isImport ? "Import MPC Wallet" : "Export MPC Wallet");
    mBinding.createMpcWallet.setText(isImport ? "Import" : "Next");
    SdCardStatusMonitor.getInstance((MainApplication) mActivity.getApplication())
        .register(mOnSdcardStatusChange);
    mBinding.createMpcWallet.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        AuthenticateModal.show(mActivity, mActivity.getString(R.string.password_modal_title), "",
            password -> {
              AppExecutors.getInstance().diskIO().execute(new Runnable() {

                @Override
                public void run() {
                  Web3JUtils web3JUtils = new Web3JUtils(new Web3JCallback() {

                    @Override
                    public void onStart() {
                      AppExecutors.getInstance().mainThread().execute(new Runnable() {

                        @Override
                        public void run() {
                          showLoading("");
                        }
                      });
                    }

                    @Override
                    public void onError(String msg) {
                      AppExecutors.getInstance().mainThread().execute(new Runnable() {

                        @Override
                        public void run() {
                          dismissLoading();
                          alert(msg);
                        }
                      });
                    }

                    @Override
                    public void onSuccess(String source) {
                      AppExecutors.getInstance().mainThread().execute(new Runnable() {

                        @Override
                        public void run() {
                          dismissLoading();
                          if (!isImport) {
                            Utilities.isExportMPCWallet(mActivity, true);
                            ElectrumViewModel.exportSuccess(mActivity, new Runnable() {

                              @Override
                              public void run() {
                                navigateUp();
                              }
                            });
                          } else {
                            Utilities.getPrefs(mActivity)
                                .edit()
                                .putString(SETTING_CHOOSE_WATCH_WALLET, "mpcSnap")
                                .apply();
                            Intent intent = new Intent(mActivity, MainActivity.class);
                            mActivity.startActivity(intent);
                            mActivity.finish();
                          }
                        }
                      });
                    }
                  });
                  MPCDatabase database = MPCDatabase.getInstance(mActivity);
                  MPCWalletDao dao = database.getMPCWalletDao();
                  Storage storage = Storage.createByEnvironment();
                  String parent = storage.getExternalDir().getAbsolutePath();
                  String fileName = "export-mpc";
                  if (isImport) {
                    String json = web3JUtils.getWalletJson(password.password,
                        parent + "/" + fileName + ".json");
                    MPCWalletEntity wallet =
                        new GsonUtils<MPCWalletEntity>().toObj(MPCWalletEntity.class, json);
                    dao.inset(wallet);
                  } else {
                    MPCWalletEntity entity = dao.loadWallet();
                    web3JUtils.generateWalletFile(password.password, entity.address,
                        entity.toJson(), parent, fileName);
                  }
                }
              });
            }, () -> {

            });
      }
    });
  }

  @Override
  protected void initData(Bundle savedInstanceState) {

  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    SdCardStatusMonitor.getInstance((MainApplication) mActivity.getApplication())
        .unregister(mOnSdcardStatusChange);
  }
}
