package com.troy.deeplinkrouter.activity;

import android.os.Bundle;
import android.widget.TextView;

import com.troy.deeplinkrouter.R;
import com.troy.dprouter.annotation.ActivityRouter;


@ActivityRouter(hosts = {"authenticate", "signin"})
public class SignInActivity extends BaseRouterActivity
{
    private TextView mContent;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setTitle("Sign In");

        setContentView(R.layout.activity_detail);

        mContent = (TextView) findViewById(R.id.content);

        handleExtras();
    }

    private void handleExtras()
    {
        Bundle extras = getIntent().getExtras();

        if(extras == null) return;

        String host = extras.getString("host");
        String username = extras.getString("username");
        String password = extras.getString("password");

        StringBuilder builder = new StringBuilder();
        innerAppend("host", host, builder);
        innerAppend("userName", username, builder);
        innerAppend("password", password, builder);

        mContent.setText(builder.toString());
    }
}
