package com.troy.deeplinkrouter.fragment;


import com.troy.dprouter.annotation.FragmentRouter;

@FragmentRouter(hosts = {"teams"})
public class TeamsFragment extends BaseFragment
{
    @Override
    protected String getPageName()
    {
        return "Teams Page";
    }
}
