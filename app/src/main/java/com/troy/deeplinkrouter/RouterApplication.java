package com.troy.deeplinkrouter;

import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.widget.Toast;

import com.troy.dprouter.annotation.AllModules;
import com.troy.dprouter.annotation.Module;
import com.troy.dprouter.api.IDPRouterInterceptor;

@AllModules(moduleNames = {"app", "module_a", "module_b"})
@Module(name = "app")
public class RouterApplication extends Application implements IDPRouterInterceptor
{
    public static final String APP_SCHEME = "dprouter";

    @Override
    public Uri onPreRouting(Context context, Uri uri)
    {
        Toast.makeText(context, "Pre Routing", Toast.LENGTH_SHORT).show();

        return uri;
    }
}
