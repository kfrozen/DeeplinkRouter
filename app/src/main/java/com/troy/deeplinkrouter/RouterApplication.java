package com.troy.deeplinkrouter;

import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.widget.Toast;

import com.troy.dprouter.api.IDPRouterInterceptor;


public class RouterApplication extends Application implements IDPRouterInterceptor
{
    @Override
    public Uri onPreRouting(Context context, Uri uri)
    {
        Toast.makeText(context, "Pre Routing", Toast.LENGTH_SHORT).show();

        return uri;
    }
}
