package com.applovin.mediation.adapters;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.applovin.mediation.adapters.yandex.BuildConfig;

public class YandexIntegrityCheckProvider extends ContentProvider {

    static final String TAG = "YandexAdsIntegrityCheck";

    @Override
    public boolean onCreate() {
        final String version = BuildConfig.VERSION_NAME;
        Log.i(TAG, "Yandex Mobile Ads Adapter " + version + " for Applovin Mediation integrated successfully");
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull final Uri uri,
                        @Nullable final String[] projection,
                        @Nullable final String selection,
                        @Nullable final String[] selectionArgs,
                        @Nullable final String sortOrder) {
        return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull final Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull final Uri uri,
                      @Nullable final ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull final Uri uri,
                      @Nullable final String selection,
                      @Nullable final String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull final Uri uri,
                      @Nullable final ContentValues values,
                      @Nullable final String selection,
                      @Nullable final String[] selectionArgs) {
        return 0;
    }
}
