/*
    Copyright 2013-2014 appPlant UG

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
*/

package de.appplant.cordova.plugin.badge;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;


public class Badge extends CordovaPlugin {

    // Static ID for the badge notification
    private final int ID = -450793490;
    // Name for the shared preferences
    private final String KEY = "badge";

    @Override
    public boolean execute (String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        if (action.equalsIgnoreCase("clearBadge")) {
            clearBadge();

            return true;
        }

        if (action.equalsIgnoreCase("setBadge")) {
            int number       = args.optInt(0);
            String title     = args.optString(1, "%d new messages");
            String smallIcon = args.optString(2);

            clearBadge();
            setBadge(number, title, smallIcon);

            return true;
        }

        if (action.equalsIgnoreCase("getBadge")) {
            getBadge(callbackContext);

            return true;
        }

        // Returning false results in a "MethodNotFound" error.
        return false;
    }

    /**
     * Sets the badge of the app icon.
     *
     * @param badge
     *      The new badge number
     * @param title
     *      The notifications title
     * @param smallIcon
     *      The notifications small icon
     */
    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    private void setBadge (int badge, String title, String smallIcon) {
        Context context = cordova.getActivity().getApplicationContext();
        Resources res   = context.getResources();

        Bitmap appIcon  = BitmapFactory.decodeResource(res, getDrawableIcon());

        Intent intent = new Intent(context, LaunchActivity.class)
            .setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

        PendingIntent contentIntent = PendingIntent.getActivity(
            context, ID, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        title = String.format(title, badge);

        Builder notification = new Notification.Builder(context)
            .setContentTitle(title)
            .setNumber(badge)
            .setTicker(title)
            .setAutoCancel(true)
            .setSmallIcon(getResIdForSmallIcon(smallIcon))
            .setLargeIcon(appIcon)
            .setContentIntent(contentIntent);

        saveBadge(badge);

        if (Build.VERSION.SDK_INT<16) {
            // Build notification for HoneyComb to ICS
            getNotificationManager().notify(ID, notification.getNotification());
        } else if (Build.VERSION.SDK_INT>15) {
            // Notification for Jellybean and above
            getNotificationManager().notify(ID, notification.build());
        }
    }

    /**
     * Clears the badge of the app icon.
     */
    private void clearBadge () {
        saveBadge(0);
        getNotificationManager().cancel(ID);
    }

    /**
     * Retrieves the badge of the app icon.
     *
     * @param callback
     *      The function to be exec as the callback
     */
    private void getBadge (CallbackContext callbackContext) {
        SharedPreferences settings = getSharedPreferences();
        int badge = settings.getInt(KEY, 0);
        PluginResult result;

        result = new PluginResult(PluginResult.Status.OK, badge);

        callbackContext.sendPluginResult(result);
    }

    /**
     * Persist the badge of the app icon so that `getBadge` is able to return
     * the badge number back to the client.
     *
     * @param badge
     *      The badge of the app icon
     */
    private void saveBadge (int badge) {
        Editor editor = getSharedPreferences().edit();

        editor.putInt(KEY, badge);
        editor.apply();
    }

    /**
     * The Local storage for the application.
     */
    private SharedPreferences getSharedPreferences () {
        Context context = cordova.getActivity().getApplicationContext();

        return context.getSharedPreferences(KEY, Context.MODE_PRIVATE);
    }

    /**
     * @return
     *      The NotificationManager for the app
     */
    private NotificationManager getNotificationManager () {
        Context context = cordova.getActivity().getApplicationContext();

        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    /**
     * @return
     *      The resource ID of the app icon
     */
    private int getDrawableIcon () {
        Context context = cordova.getActivity().getApplicationContext();
        Resources res   = context.getResources();
        String pkgName  = context.getPackageName();

        int resId = res.getIdentifier("icon", "drawable", pkgName);

        return resId;
    }

    /**
     * @return
     *      The resource ID for the small icon
     */
    private int getResIdForSmallIcon (String smallIcon) {
        int resId      = 0;
        String pkgName = cordova.getActivity().getPackageName();

        resId = getResId(pkgName, smallIcon);

        if (resId == 0) {
            resId = getResId("android", smallIcon);
        }

        if (resId == 0) {
            resId = getResId("android", "ic_dialog_email");
        }

        return resId;
    }

    /**
     * Returns numerical icon Value
     *
     * @param {String} className
     * @param {String} iconName
     */
    private int getResId (String className, String iconName) {
        int icon = 0;

        try {
            Class<?> klass  = Class.forName(className + ".R$drawable");

            icon = (Integer) klass.getDeclaredField(iconName).get(Integer.class);
        } catch (Exception e) {}

        return icon;
    }
}
