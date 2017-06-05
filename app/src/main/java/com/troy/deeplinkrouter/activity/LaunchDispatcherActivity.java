package com.troy.deeplinkrouter.activity;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.troy.dprouter.api.DPRouter;


public class LaunchDispatcherActivity extends AppCompatActivity
{
    public static final String s_DEFAULT_URI_TO_MAIN = "dprouter://main";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Uri deeplinkUri = getIntent().getData();

        DPRouter.linkToActivity(this, deeplinkUri == null ? Uri.parse(s_DEFAULT_URI_TO_MAIN) : deeplinkUri);

        this.finish();
    }
}
