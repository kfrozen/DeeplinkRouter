package com.troy.deeplinkrouter.activity;

import android.os.Bundle;
import android.widget.TextView;

import com.troy.deeplinkrouter.R;
import com.troy.dprouter.annotation.ActivityRouter;

@ActivityRouter(hosts = {"user"}, parentActivityHost = "signin")
public class UserActivity extends BaseRouterActivity
{
    private TextView mContent;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setTitle("Users");

        setContentView(R.layout.activity_detail);

        mContent = (TextView) findViewById(R.id.content);

        handleExtras();
    }

    private void handleExtras()
    {
        Bundle extras = getIntent().getExtras();

        if(extras == null) return;

        String host = extras.getString("host");
        String firstName = extras.getString("firstname");
        String lastName = extras.getString("lastname");

        StringBuilder builder = new StringBuilder();
        innerAppend("host", host, builder);
        innerAppend("firstName", firstName, builder);
        innerAppend("lastName", lastName, builder);

        mContent.setText(builder.toString());
    }
}
