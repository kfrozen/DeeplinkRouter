package com.troy.dprouter.api;

import android.content.Context;
import android.net.Uri;

public interface IDPRouterInterceptor
{
    Uri onPreRouting(Context context, Uri uri);
}
