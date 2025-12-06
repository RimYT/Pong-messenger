package com.RSD.pong.utils;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.util.Log;

public class AccountHandler {
    static Prefs prefs;
    public static void createAccount(Activity activity, String username, String accessToken, String refreshToken) {
        prefs = new Prefs(activity);
        AccountManager accountManager = AccountManager.get(activity);
        Account account = new Account(username, "com.RSD.pong.account");

        Account existing = getSavedAccount(activity);
        if (existing != null) return;

        accountManager.addAccountExplicitly(account, null, null);
        accountManager.setAuthToken(account, "access", accessToken);
        accountManager.setUserData(account, "refresh", refreshToken);
        accountManager.setUserData(account, "server_ip", prefs.getServerIp());
        // prefs.setToken(token);
    }

    public static Account getSavedAccount(Activity context) {
        AccountManager accountManager = AccountManager.get(context);
        Account[] accounts = accountManager.getAccountsByType("com.RSD.pong.account");
        return accounts.length > 0 ? accounts[0] : null;
    }

    public static void deleteAccount(Activity activity, Account account) {
        AccountManager am = AccountManager.get(activity);

        if (account == null) {
            return;
        }

        boolean removed = am.removeAccountExplicitly(account);
    }
}
