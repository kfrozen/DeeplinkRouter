package com.troy.dprouter.api;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

public class DPRouter
{
    private static ArrayList<Mapping> mActivityMappings = new ArrayList<>();
    private static ArrayList<Mapping> mFragmentMappings = new ArrayList<>();
    private static volatile ArrayList<Runnable> mPendingRoutingJobs = new ArrayList<>();
    private static boolean mIsRouterActive = true;
    private static final Object LOCK = new Object();

    public static void setRouterActive(boolean active)
    {
        if(active && !mIsRouterActive)
        {
            processPendingRoutingJobs();
        }

        mIsRouterActive = active;
    }

    private static void smartInit()
    {
        if(mActivityMappings.isEmpty() || mFragmentMappings.isEmpty())
        {
            try
            {
                Class<?> finder = Class.forName("com.troy.dprouter.api.RouterInit");

                IDPRouterInit routerInit = (IDPRouterInit) finder.newInstance();

                routerInit.init();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    static void mapActivity(String host, Class<? extends Activity> targetActivity, ArrayMap<String, String> paramsFilter)
    {
        Mapping mapping = new Mapping(host, targetActivity, null, paramsFilter);

        if(!mActivityMappings.contains(mapping)) mActivityMappings.add(mapping);
    }

    static void mapFragment(String host, Class<? extends Fragment> targetFragment, ArrayMap<String, String> paramsFilter)
    {
        Mapping mapping = new Mapping(host, null, targetFragment, paramsFilter);

        if(!mFragmentMappings.contains(mapping)) mFragmentMappings.add(mapping);
    }

    public static Intent resolve(Context context, @NonNull Uri uri)
    {
        smartInit();

        String url = uri.toString();

        for (Mapping mapping : mActivityMappings)
        {
            if (mapping.isMatched(url))
            {
                Intent intent = new Intent();

                intent.setClass(context, mapping.getTargetActivity());

                intent.setData(uri);

                intent.putExtras(mapping.parseExtras(url));

                if(!(context instanceof Activity))
                {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }

                return intent;
            }
        }

        return null;
    }

    public static boolean linkToActivity(Context context, @NonNull Uri uri)
    {
        smartInit();

        IDPRouterInterceptor interceptor = getGlobalInterceptor(context);

        if(interceptor != null)
        {
            uri = interceptor.onPreRouting(context, uri);

            if(uri == null) return false;
        }

        String url = uri.toString();

        Mapping defaultMapping = null;

        for (Mapping mapping : mActivityMappings)
        {
            if (mapping.isMatched(url))
            {
                return innerStartActivity(context, mapping, uri, url);
            }
            else if (defaultMapping == null && mapping.isDefault())
            {
                defaultMapping = mapping;
            }
        }

        return defaultMapping != null && innerStartActivity(context, defaultMapping, uri, url);
    }

    private static boolean innerStartActivity(Context context, Mapping mapping, Uri uri, String url)
    {
        if(mapping.getTargetActivity() == null) return false;

        Intent intent = new Intent();

        intent.setClass(context, mapping.getTargetActivity());

        intent.setData(uri);

        intent.putExtras(mapping.parseExtras(url));

        if(!(context instanceof Activity))
        {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }

        context.startActivity(intent);

        return true;
    }

    public static void linkToFragment(FragmentActivity activity, @NonNull Uri uri, @NonNull INLFragmentRoutingCallback callback)
    {
        synchronized (LOCK)
        {
            if(mIsRouterActive)
            {
                innerLinkToFragment(activity, uri, callback);
            }
            else
            {
                mPendingRoutingJobs.add(new FragmentRoutingJob(activity, uri, callback));
            }
        }
    }

    private static void innerLinkToFragment(FragmentActivity activity, @NonNull Uri uri, @NonNull INLFragmentRoutingCallback callback)
    {
        if(activity == null) return;

        smartInit();

        FragmentManager fm = activity.getSupportFragmentManager();

        List<Fragment> addedFragmentList = fm.getFragments();

        String url = uri.toString();

        for(Mapping mapping : mFragmentMappings)
        {
            if(mapping.isMatched(url))
            {
                boolean isTargetHasBeenAdded = false;
                Fragment existingTargetFragment = null;

                Class<? extends Fragment> target = mapping.getTargetFragment();

                if(target == null) break;

                if(addedFragmentList != null && !addedFragmentList.isEmpty())
                {
                    for(Fragment f : addedFragmentList)
                    {
                        if(f == null) continue;

                        Class<? extends Fragment> fClass = f.getClass();

                        if(target.equals(fClass))
                        {
                            isTargetHasBeenAdded = true;

                            existingTargetFragment = f;

                            break;
                        }
                    }
                }

                Bundle extras = mapping.parseExtras(url);

                if(isTargetHasBeenAdded)
                {
                    callback.onFragmentRouting(existingTargetFragment, extras, true);

                    return;
                }

                try
                {
                    callback.onFragmentRouting(target.newInstance(), extras, false);

                    return;
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

                break;
            }
        }

        callback.onFragmentRouting(null, null, false);
    }

    private static void processPendingRoutingJobs()
    {
        if(mPendingRoutingJobs.isEmpty()) return;

        synchronized (LOCK)
        {
            if(mPendingRoutingJobs.isEmpty()) return;

            for(Runnable job : mPendingRoutingJobs)
            {
                job.run();
            }

            mPendingRoutingJobs.clear();
        }
    }

    private static IDPRouterInterceptor getGlobalInterceptor(Context context)
    {
        if(context.getApplicationContext() instanceof IDPRouterInterceptor)
        {
            return (IDPRouterInterceptor) context.getApplicationContext();
        }

        return null;
    }

    private static class FragmentRoutingJob implements Runnable
    {
        private FragmentActivity activity;
        private Uri uri;
        private INLFragmentRoutingCallback callback;

        FragmentRoutingJob(FragmentActivity activity, @NonNull Uri uri, @NonNull INLFragmentRoutingCallback callback)
        {
            this.activity = activity;
            this.uri = uri;
            this.callback = callback;
        }

        @Override
        public void run()
        {
            innerLinkToFragment(activity, uri, callback);
        }
    }

    public interface INLFragmentRoutingCallback
    {
        /**
         * @param targetFragment The fragment that matched to the input uri
         * @param extras This bundle contains the key-value pairs generated based on the uri query items
         * @param isTargetExisting {@code true} means there's already an instance of the targetFragment's class has been added to
         *                                     the parent activity's {@code FragmentManager}, and the returned targetFragment was that
         *                                     previously added instance.
         *                         {@code false} means the returned targetFragment was newly created.
         */
        boolean onFragmentRouting(Fragment targetFragment, Bundle extras, boolean isTargetExisting);
    }
}
