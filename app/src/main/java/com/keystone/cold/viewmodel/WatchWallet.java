/*
 *
 *  Copyright (c) 2021 Keystone
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 * in the file COPYING.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.keystone.cold.viewmodel;

import android.content.Context;

import com.keystone.coinlib.utils.Coins;
import com.keystone.cold.R;
import com.keystone.cold.Utilities;
import com.keystone.cold.scan.QREncoding;

import static com.keystone.cold.ui.fragment.setting.MainPreferenceFragment.SETTING_CHOOSE_WATCH_WALLET;

public enum WatchWallet {
    KEYSTONE("0"),
    POLKADOT_JS("1"),
    XRP_TOOLKIT("2"),
    METAMASK("3");

    public static final String XRP_TOOLKIT_SIGN_ID = "xrp_toolkit_sign_id";
    public static final String POLKADOT_JS_SIGN_ID = "polkadot_js_sign_id";
    public static final String METAMASK_SIGN_ID = "metamask_sign_id";

    private final String walletId;

    WatchWallet(String walletId) {
        this.walletId = walletId;
    }

    public static WatchWallet getWatchWallet(Context context) {
        String wallet = Utilities.getPrefs(context)
                .getString(SETTING_CHOOSE_WATCH_WALLET, KEYSTONE.getWalletId());
        return getWatchWalletById(wallet);
    }

    public static WatchWallet getWatchWalletById(String walletId) {
        WatchWallet selectWatchWallet = KEYSTONE;
        for (WatchWallet watchWallet : WatchWallet.values()) {
            if (watchWallet.getWalletId().equals(walletId)) {
                selectWatchWallet = watchWallet;
                break;
            }
        }
        return selectWatchWallet;
    }

    public String getWalletId() {
        return walletId;
    }

    public String getWalletName(Context context) {
        String[] wallets = context.getResources().getStringArray(R.array.watch_wallet_list);
        return wallets[Integer.parseInt(walletId)];
    }

    public QREncoding getQrEncoding() {
        if (this == WatchWallet.POLKADOT_JS) {
            return QREncoding.UOS;
        }
        return QREncoding.UR;
    }

    public Coins.Coin[] getSupportedCoins() {
        switch (this) {
            case KEYSTONE:
                return new Coins.Coin[]{Coins.BTC, Coins.BCH, Coins.ETH, Coins.XRP, Coins.TRON, Coins.LTC, Coins.DASH, Coins.DOT};
            case POLKADOT_JS:
                return new Coins.Coin[]{Coins.DOT, Coins.KSM};
            case XRP_TOOLKIT:
                return new Coins.Coin[]{Coins.XRP};
            case METAMASK:
                return new Coins.Coin[]{Coins.ETH};
        }
        return new Coins.Coin[]{};
    }

    public static boolean isSupported(Context context, String coinCode) {
        Coins.Coin[] list = getWatchWallet(context).getSupportedCoins();
        for (Coins.Coin coin : list) {
            if (coin.coinCode().equals(coinCode)) {
                return true;
            }
        }
        return false;
    }

    public String getSignId() {
        switch (this) {
            case POLKADOT_JS:
                return POLKADOT_JS_SIGN_ID;
            case XRP_TOOLKIT:
                return XRP_TOOLKIT_SIGN_ID;
            case METAMASK:
                return METAMASK_SIGN_ID;
        }
        return null;
    }
}