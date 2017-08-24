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
import java.util.Map.Entry;

public class DPRouter
{
    private static ArrayList<Mapping> mActivityMappings = new ArrayList<>();
    private static ArrayList<Mapping> mMasterFragmentMappings = new ArrayList<>();
    private static ArrayList<Mapping> mChildFragmentMappings = new ArrayList<>();
    private static volatile ArrayList<Runnable> mPendingRoutingJobs = new ArrayList<>();
    private static boolean mIsRouterActive = true;
    private static final Object LOCK = new Object();
    private static ArrayMap<Class<? extends Activity>, ActivityRoutingStatus> mActivityRoutingStatusMap = new ArrayMap<>();

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
        if(mActivityMappings.isEmpty() || mMasterFragmentMappings.isEmpty() || mChildFragmentMappings.isEmpty())
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

    static void mapActivity(String host, Class<? extends Activity> targetActivity, ArrayMap<String, String> paramsFilter, String parentActivityHost)
    {
        Mapping mapping = new Mapping(host, targetActivity, null, paramsFilter, parentActivityHost);

        if(!mActivityMappings.contains(mapping)) mActivityMappings.add(mapping);
    }

    static void mapFragment(String host, Class<? extends Fragment> targetFragment, ArrayMap<String, String> paramsFilter, boolean isMaster)
    {
        Mapping mapping = new Mapping(host, null, targetFragment, paramsFilter);

        if(isMaster)
        {
            if(!mMasterFragmentMappings.contains(mapping)) mMasterFragmentMappings.add(mapping);
        }
        else
        {
            if(!mChildFragmentMappings.contains(mapping)) mChildFragmentMappings.add(mapping);
        }
    }

    public static void preFinish(@NonNull Activity caller, @NonNull String appScheme)
    {
        ActivityRoutingStatus activityRoutingStatus = mActivityRoutingStatusMap.get(caller.getClass());

        if(activityRoutingStatus != null && activityRoutingStatus.isActivityLinkedByDeeplink && !TextUtils.isEmpty(activityRoutingStatus.parentActivityHost))
        {
            activityRoutingStatus.isActivityLinkedByDeeplink = false;

            DPRouter.linkToActivity(caller, composeNavigationUri(appScheme, activityRoutingStatus.parentActivityHost, null, null), false);
        }
    }

    public static Uri composeNavigationUri(String schemeString, String host, String path, ArrayMap<String, String> extras)
    {
        StringBuilder builder = new StringBuilder(schemeString).append("://").append(host);

        if(!TextUtils.isEmpty(path))
        {
            builder.append("/").append(path);
        }

        if(extras != null && extras.size() > 0)
        {
            builder.append("?");

            for(Entry<String, String> entry : extras.entrySet())
            {
                if(entry == null) continue;

                builder.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
            }
        }

        String result = builder.toString();

        if(result.endsWith("?") || result.endsWith("&"))
        {
            result = result.substring(0, result.length() - 1);
        }

        return Uri.parse(result);
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
        return linkToActivity(context, uri, true);
    }

    /**
    *  @param isDeeplink true to indicate the request was made by deeplink action while false means it's a normal local navigation inside the app
    */
    public static boolean linkToActivity(Context context, @NonNull Uri uri, boolean isDeeplink)
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
                return innerStartActivity(context, mapping, uri, url, isDeeplink);
            }
            else if (defaultMapping == null && mapping.isDefault())
            {
                defaultMapping = mapping;
            }
        }

        return defaultMapping != null && innerStartActivity(context, defaultMapping, uri, url, isDeeplink);
    }

    private static boolean innerStartActivity(Context context, Mapping mapping, Uri uri, String url, boolean isDeeplink)
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

        ActivityRoutingStatus activityRoutingStatus = mActivityRoutingStatusMap.get(mapping.getTargetActivity());

        if(activityRoutingStatus == null)
        {
            mActivityRoutingStatusMap.put(mapping.getTargetActivity(), new ActivityRoutingStatus(isDeeplink, mapping.getParentActivityHost()));
        }
        else
        {
            activityRoutingStatus.isActivityLinkedByDeeplink = true;
        }

        return true;
    }

    /**
    *   @param fragmentManager the fragment manager held by the caller, should from getSupportFragmentManager() if the request was made by Activity
     *                         and getChildFragmentManager() if it was from a master Fragment.
     *  @param linkFromActivity true to indicate the request was made by Activity (now the fragmentManager should from getSupportFragmentManager()),
     *                          while false to indicate it was from a master Fragment (now the fragmentManager should from getChildFragmentManager())
    */
    public static void linkToFragment(final FragmentManager fragmentManager, @NonNull Uri uri, @NonNull INLFragmentRoutingCallback callback, final boolean linkFromActivity)
    {
        synchronized (LOCK)
        {
            if(mIsRouterActive)
            {
                innerLinkToFragment(fragmentManager, uri, callback, linkFromActivity);
            }
            else
            {
                mPendingRoutingJobs.add(new FragmentRoutingJob(fragmentManager, uri, callback, linkFromActivity));
            }
        }
    }

    private static void innerLinkToFragment(FragmentManager fragmentManager, @NonNull Uri uri, @NonNull INLFragmentRoutingCallback callback, boolean linkFromActivity)
    {
        if(fragmentManager == null) return;

        smartInit();

        List<Fragment> addedFragmentList = fragmentManager.getFragments();

        String url = uri.toString();

        ArrayList<Mapping> targetFragmentMappings = linkFromActivity ? mMasterFragmentMappings : mChildFragmentMappings;

        for(Mapping mapping : targetFragmentMappings)
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
        private FragmentManager fragmentManager;
        private Uri uri;
        private INLFragmentRoutingCallback callback;
        private boolean linkFromActivity;

        FragmentRoutingJob(FragmentManager fragmentManager, @NonNull Uri uri, @NonNull INLFragmentRoutingCallback callback, boolean linkFromActivity)
        {
            this.fragmentManager = fragmentManager;
            this.uri = uri;
            this.callback = callback;
            this.linkFromActivity = linkFromActivity;
        }

        @Override
        public void run()
        {
            innerLinkToFragment(fragmentManager, uri, callback, linkFromActivity);
        }
    }

    private static class ActivityRoutingStatus
    {
        ActivityRoutingStatus(boolean isActivityLinkedByDeeplink, String parentActivityHost)
        {
            this.isActivityLinkedByDeeplink = isActivityLinkedByDeeplink;

            this.parentActivityHost = parentActivityHost;
        }

        boolean isActivityLinkedByDeeplink = false;

        String parentActivityHost = null;
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
