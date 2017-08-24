package com.troy.deeplinkrouter.fragment;


import com.troy.dprouter.annotation.FragmentRouter;

@FragmentRouter(hosts = {"games"}, params = {"type=game"}, isMasterFragment = true)
public class GamesFragment extends BaseFragment
{
    @Override
    protected String getPageName()
    {
        return "Games Page";
    }
}
