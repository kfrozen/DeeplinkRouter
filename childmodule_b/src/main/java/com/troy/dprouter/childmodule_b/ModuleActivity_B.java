package com.troy.dprouter.childmodule_b;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.troy.dprouter.annotation.ActivityRouter;

@ActivityRouter(hosts = {"module_b_test"})
public class ModuleActivity_B extends AppCompatActivity
{
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_module_b);
    }
}
