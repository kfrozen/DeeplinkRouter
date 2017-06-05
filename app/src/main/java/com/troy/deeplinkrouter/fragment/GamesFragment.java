package com.troy.deeplinkrouter.fragment;


import com.troy.dprouter.annotation.FragmentRouter;

@FragmentRouter(hosts = {"games"}, params = {"type=game", "id=456"})
public class GamesFragment extends BaseFragment
{
    @Override
    protected String getPageName()
    {
        return "Games Page";
    }
}
