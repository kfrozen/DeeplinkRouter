package com.troy.deeplinkrouter.fragment;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.troy.deeplinkrouter.R;
import com.troy.dprouter.api.DPRouter;
import com.troy.dprouter.api.DPRouter.INLFragmentRoutingCallback;
import com.troy.dprouter.api.Mapping;

public abstract class BaseFragment extends Fragment implements INLFragmentRoutingCallback
{
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_detail, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        TextView content = (TextView) getView().findViewById(R.id.name);

        String pageName = getPageName();

        getActivity().setTitle(pageName);

        content.setText(pageName);

        dispatchFragmentRouting();
    }

    private void dispatchFragmentRouting()
    {
        Uri deeplinkUri = getArguments() == null ? null : (Uri) getArguments().getParcelable(Mapping.DPROUTER_BUNDLE_KEY_FULL_URI);

        if (deeplinkUri == null)
        {
            return;
        }

        DPRouter.linkToFragment(getChildFragmentManager(), deeplinkUri, this, false);
    }

    @Override
    public boolean onFragmentRouting(Fragment targetFragment, Bundle extras, boolean isTargetExisting)
    {
        return false;
    }

    protected abstract String getPageName();
}
