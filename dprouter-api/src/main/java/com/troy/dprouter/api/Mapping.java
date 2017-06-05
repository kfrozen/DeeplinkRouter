package com.troy.dprouter.api;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;

import java.util.Set;

public class Mapping
{
    public static final String DPROUTER_BUNDLE_KEY_HOST = "host";
    public static final String DPROUTER_BUNDLE_KEY_PATH = "path";
    public static final String DPROUTER_DEFAULT_HOST = "default";

    private String host;
    private ArrayMap<String, String> paramsFilter = new ArrayMap<>();
    private Class<? extends Activity> targetActivity;
    private Class<? extends Fragment> targetFragment;

    public Mapping(String host, Class<? extends Activity> activity, Class<? extends Fragment> fragment, ArrayMap<String, String> paramsFilter)
    {
        this.host = host;
        this.targetActivity = activity;
        this.targetFragment = fragment;
        if(paramsFilter != null) this.paramsFilter = paramsFilter;
    }

    public final boolean isDefault()
    {
        return TextUtils.equals(host, DPROUTER_DEFAULT_HOST);
    }

    public boolean isMatched(String url)
    {
        if(TextUtils.isEmpty(url)) return false;

        Uri uri = Uri.parse(url);

        String host = uri.getHost();

        if(!TextUtils.isEmpty(host) && TextUtils.equals(this.host, host))
        {
            if(paramsFilter.isEmpty())
            {
                return true;
            }
            else
            {
                Set<String> queryKeySet = uri.getQueryParameterNames();

                if(queryKeySet.isEmpty()) return false;

                boolean isAllParamsMatched = true;

                for(String paramKey : paramsFilter.keySet())
                {
                    isAllParamsMatched = isAllParamsMatched
                            && queryKeySet.contains(paramKey)
                            && TextUtils.equals(paramsFilter.get(paramKey), uri.getQueryParameter(paramKey));
                }

                return isAllParamsMatched;
            }
        }

        return false;
    }

    public Bundle parseExtras(String url)
    {
        if(TextUtils.isEmpty(url)) return null;

        Bundle extras = new Bundle();

        Uri uri = Uri.parse(url);

        extras.putString(DPROUTER_BUNDLE_KEY_HOST, uri.getHost());

        String path = uri.getPath();

        if(!TextUtils.isEmpty(path))
        {
            if(path.startsWith("/"))
            {
                path = path.substring(1);
            }

            extras.putString(DPROUTER_BUNDLE_KEY_PATH, path);
        }

        //Parse all the query items in the url

        Set<String> queryKeySet = uri.getQueryParameterNames();

        if(!queryKeySet.isEmpty())
        {
            for(String key : queryKeySet)
            {
                extras.putString(key, uri.getQueryParameter(key));
            }
        }

        return extras;
    }

    public String getHost()
    {
        return host;
    }

    public Class<? extends Activity> getTargetActivity()
    {
        return targetActivity;
    }

    public Class<? extends Fragment> getTargetFragment()
    {
        return targetFragment;
    }
}
