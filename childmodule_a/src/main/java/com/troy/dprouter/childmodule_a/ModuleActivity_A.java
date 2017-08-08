package com.troy.dprouter.childmodule_a;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.troy.dprouter.annotation.ActivityRouter;

@ActivityRouter(hosts = {"module_a_test"})
public class ModuleActivity_A extends AppCompatActivity
{
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_module_a);
    }
}
