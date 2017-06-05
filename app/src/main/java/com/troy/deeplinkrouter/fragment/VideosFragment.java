package com.troy.deeplinkrouter.fragment;


import com.troy.dprouter.annotation.FragmentRouter;

@FragmentRouter(hosts = {"games"}, params = {"type=videos"})
public class VideosFragment extends BaseFragment
{

    @Override
    protected String getPageName()
    {
        return "Videos Page";
    }
}
