package com.troy.deeplinkrouter.fragment;


import com.troy.dprouter.annotation.FragmentRouter;

@FragmentRouter(hosts = {"games"}, params = {"type=videos"})
public class GameVideosFragment extends BaseFragment
{

    @Override
    protected String getPageName()
    {
        return "Game-Videos Page";
    }
}
