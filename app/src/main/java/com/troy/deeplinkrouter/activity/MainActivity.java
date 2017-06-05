package com.troy.deeplinkrouter.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.widget.Toast;

import com.troy.deeplinkrouter.R;
import com.troy.dprouter.annotation.ActivityRouter;


@ActivityRouter
public class MainActivity extends BaseRouterActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onFragmentRouting(Fragment targetFragment, Bundle extras, boolean isTargetExisting)
    {
        if(targetFragment == null)
        {
            Toast.makeText(this, "No fragment matched", Toast.LENGTH_SHORT).show();

            return false;
        }

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

        Toast.makeText(this, "New Target fragment:" + targetFragment.getClass().getSimpleName(), Toast.LENGTH_SHORT).show();

        return true;
    }
}
