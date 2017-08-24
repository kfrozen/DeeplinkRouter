## DeeplinkRouter Developers Guide ##

**Version 1.0.4**

- [Introduction](#introduction)
- [Usage](#usage)
- [Integration Guide](#integration-guide)
    - Route to Activity
    - Route to Fragment
    - Global Interceptor
    - Multiple modules
    - Parent Activity
    - Dispatch to child Fragments
    - Internal routing

<a name="#introduction"></a>
## Introduction

DeeplinkRouter makes it much easier to handle deeplink flow and navigation between activity/fragment by applying custom annotations. Now support navigation between different modules.

<a name="#usage"></a>
## Usage

In your app module's build.gradle file
```
    compile 'com.troy.deeplinkrouter:dprouter-api:1.0.4'

    annotationProcessor 'com.troy.deeplinkrouter:dprouter-compiler:1.0.4'
```

**Note:** You may try the attached deeplinkRouter_test_urls.html to test the demo app, several sample links have been included.

<a name="#integration-guide"></a>
## Integration Guide

Firstly, for both Activity and Fragment routing, choose a scheme for your app and set it to your launcher activity.

For example:
```
        <activity android:name=".activity.LaunchDispatcherActivity"
                  android:launchMode="singleTask">
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />

                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />

                <data android:scheme="dprouter" />
            </intent-filter>
        </activity>
```
So LaunchDispatcherActivity will be the entrance for all the deeplink uri, and inside the LaunchDispatcherActivity, add below code snippet
```
    public static final String s_DEFAULT_URI_TO_MAIN = "dprouter://main";
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Uri deeplinkUri = getIntent().getData();

        DPRouter.linkToActivity(this, deeplinkUri == null ? Uri.parse(s_DEFAULT_URI_TO_MAIN) : deeplinkUri);

        this.finish();
    }
```

- **Route to Activity**

Apply @ActivityRouter to your target activity, inside which **"hosts"** represents for a set of uri hosts this activity will response to, and **"params"**, which in format of "key=value", represents for the parameter filter. With the filter, a host can be shared by various targets as long as they have different param filter.

Note that:
1. If the **"params"** remained blank, then there's no parameter filter for this target.
2. If the **"hosts"** left as blank, the activity will be set to the default target, an application should only contain one default target.

Here's an example:
```
    //Here we set two key-value pairs to "params", which means DetailActivity is the target for uri that contains one of the three hosts AND with query id=123&teamname=NYK
    @ActivityRouter(hosts = {"game", "video", "team"}, params = {"id=123", "teamname=NYK"})
    public class DetailActivity extends BaseRouterActivity
    {
        ......
    }
    
    //Leave the values as blank, then MainActivity will be the default target, any unmatched hosts will be routed to here.
    @ActivityRouter 
    public class MainActivity extends BaseRouterActivity
    {
        ......
    }
```

Inside the target activity, you can fetch the query params by this way:
```
    Bundle extras = getIntent().getExtras();

    if(extras == null) return;

    String host = extras.getString("host");
    String path = extras.getString("path");
    String id = extras.getString("id");
    String teamName = extras.getString("teamname");
```

- **Route to Fragment**

Apply @FragmentRouter to your target Fragment, same definitions for **"hosts"** and **"params"** here
```
    @FragmentRouter(hosts = {"games"}, params = {"type=game"})
    public class GamesFragment extends BaseFragment
    {
        ......
    }
```

Meanwhile, @ActivityRouter, with a **"hosts"** set that contains your target fragment's host, also need to be applied to the parent Activity of your target Fragment
```
    @ActivityRouter(hosts = {"main", "home", "games", "videos", "teams"})
    public class MainActivity extends BaseRouterActivity
    {
        //MainActivity will contain the GamesFragment
        ......
    }
```

Then, based on the launch mode of your activity, dispatch the uri down to the fragments either in **onPostCreate(...)** or **onNewIntent(...)** method.
```
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
```

You may also hold the routing actions by setting **DPRouter.setRouterActive(boolean active)**, if set to false, then all the routing actions will be hold until a following **DPRouter.setRouterActive(true);**, and all the holding actions would be executed after then.

Lastly, in order to receive the matched fragment, your parent activity need to implement the INLFragmentRoutingCallback interface
```
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
```

Here's an implementation example:
```
    @Override
    public boolean onFragmentRouting(Fragment targetFragment, Bundle extras, boolean isTargetExisting)
    {
        if(targetFragment == null) return false;

        if(isTargetExisting)
        {
            Toast.makeText(this, "Target fragment:" + targetFragment.getClass().getSimpleName() + "has already been added", Toast.LENGTH_SHORT).show();

            return true;
        }

        if(extras != null)
        {
            targetFragment.setArguments(extras);
        }

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_holder, targetFragment);
        ft.commit();
        ft = null;

        return true;
    }
```

- **Interceptor**

A global interceptor can be applied by implementing the IDPRouterInterceptor in your own Application class.

Here you have a chance to modify the uri, or even drop the routing request by returning a **null** in onPreRouting method.

```
    public class RouterApplication extends Application implements IDPRouterInterceptor
    {
        @Override
        public Uri onPreRouting(Context context, Uri uri)
        {
            Toast.makeText(context, "Pre Routing", Toast.LENGTH_SHORT).show();

            return uri;
        }
    }
```

- **Multiple Modules support**

Two new annotations have been provided now: **Module** and **AllModules**

If your project had multiple modules, for each module, annotate one of the classes (just one will be enough) with annotation **Module** to indicate the module name, like this:

```
    @Module(name = "module_a")
    public class ModuleApplication extends Application
    {
    }
```

Then in your host module, annotate one of the classes (still, one is enough) with annotation **AllModules** to indicate all the support modules, note that the module names cannot be duplicated. Example:

```
    @AllModules(moduleNames = {"app", "module_a", "module_b"})
    @Module(name = "app")
    public class RouterApplication extends Application implements IDPRouterInterceptor
```

Finally, for each module including host and child modules, you need to add dependence for dprouter compiler in module's build.gradle file, like this:

```
    child module's build.gradle:

            compile project(':dprouter-api')
            annotationProcessor project(':dprouter-compiler')

    host module's build.gradle:

                compile project(':childmodule_a')
                compile project(':childmodule_b')
                annotationProcessor project(':dprouter-compiler')
```

You are all set for the multiple modules routing.

- **Parent Activity support**

For detail activities opened through deeplink, you may wish to navigate back a pre-defined activity (MainActivity for example) when user tap on back button instead of just exiting the app.

Now you can do this by defining your parent Activity in annotation **ActivityRouter**. Set **parentActivityHost()** to your parent Activity's host, for example:

```
    @ActivityRouter(hosts = {"user"}, parentActivityHost = "main")
    public class UserActivity extends BaseRouterActivity
```

And just leave this field as blank if the target Activity did not need a parent Activity.

Then inside your base Activity, BaseRouterActivity for instance, override the **finish()** method like this:

```
    @Override
    public void finish()
    {
        DPRouter.preFinish(this, RouterApplication.APP_SCHEME);

        super.finish();
    }
```

- **Dispatch to child Fragment**

There's a new field added to **@FragmentRouter** annotation called **isMasterFragment()**, which will return a boolean value (default to false).

This is to support dispatching deeplink action one step more to the child fragment that held by its master fragment(the one directly held by Activity). Thus for the fragments directly held by Activity, set **isMasterFragment()** to true inside the @FragmentRouter annotation.

To dispatch deeplink action from fragment, just follow the same way that activity does in your base fragment, please see detail usage for this in the demo: com.troy.deeplinkrouter.fragment.BaseFragment

**Note that there's a little change in DPRouter.linkToFragment() method, we updated the type of the first param from FragmentActivity to FragmentManager, which means for dispatching from Activity, getSupportFragmentManager() should be applied while for dispatching from Fragment, use getChildFragmentManager().
Also, we have added an extra param called isFromActivity, please see below:**

```
    /**
    *   @param fragmentManager the fragment manager held by the caller, should from getSupportFragmentManager() if the request was made by Activity
     *                         and getChildFragmentManager() if it was from a master Fragment.
     *  @param linkFromActivity true to indicate the request was made by Activity (now the fragmentManager should from getSupportFragmentManager()),
     *                          while false to indicate it was from a master Fragment (now the fragmentManager should from getChildFragmentManager())
    */
    public static void linkToFragment(final FragmentManager fragmentManager, @NonNull Uri uri, @NonNull INLFragmentRoutingCallback callback, final boolean linkFromActivity)
```

- **Internal routing support**

The router can actually replace the traditional startActivity(...) method to complete the internal navigation, which was quite useful for navigation between different modules.

We have added a helper method in DPRouter to help you get a formatted Uri for internal routing:

```
public static Uri composeNavigationUri(String schemeString, String host, String path, ArrayMap<String, String> extras)
{
    ......
}
```

After getting the Uri, you may invoke the below method to do internal routing, which can replace the traditional startActivity(...) method:

```
    /**
    *  @param isDeeplink true to indicate the request was made by deeplink action while false means it's a normal local navigation inside the app
    */
    public static boolean linkToActivity(Context context, @NonNull Uri uri, boolean isDeeplink)
    {
        ......
    }
```

For more integration details and usages, please refer to the demo.