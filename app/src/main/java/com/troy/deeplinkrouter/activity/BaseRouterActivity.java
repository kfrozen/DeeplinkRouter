package com.troy.deeplinkrouter.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import com.troy.dprouter.api.DPRouter;
import com.troy.dprouter.api.DPRouter.INLFragmentRoutingCallback;


public abstract class BaseRouterActivity extends AppCompatActivity implements INLFragmentRoutingCallback
{
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

//        DPRouter.setRouterActive(false);
//
//        (new Handler()).postDelayed(new Runnable()
//        {
//            @Override
//            public void run()
//            {
//                DPRouter.setRouterActive(true);
//            }
//        }, 5000);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState)
    {
        super.onPostCreate(savedInstanceState);

        dispatchFragmentRouting();
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);

        setIntent(intent);

        dispatchFragmentRouting();
    }

    private void dispatchFragmentRouting()
    {
        Uri deeplinkUri = getIntent().getData();

        if(deeplinkUri == null) return;

        DPRouter.linkToFragment(this, deeplinkUri, this);
    }

    @Override
    public boolean onFragmentRouting(Fragment targetFragment, Bundle extras, boolean isTargetExisting)
    {
        return false;
    }

    protected void innerAppend(String label, String content, StringBuilder builder)
    {
        if(!TextUtils.isEmpty(content))
        {
            builder.append(label).append("=").append(content).append("\n");
        }
    }
}
