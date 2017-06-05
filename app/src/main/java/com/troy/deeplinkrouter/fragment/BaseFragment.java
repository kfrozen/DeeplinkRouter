package com.troy.deeplinkrouter.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.troy.deeplinkrouter.R;

public abstract class BaseFragment extends Fragment
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
    }

    protected abstract String getPageName();
}
