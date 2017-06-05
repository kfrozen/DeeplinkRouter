package com.troy.deeplinkrouter.activity;

import android.os.Bundle;
import android.widget.TextView;

import com.troy.deeplinkrouter.R;
import com.troy.dprouter.annotation.ActivityRouter;

@ActivityRouter(hosts = {"game", "video", "team"})
public class DetailActivity extends BaseRouterActivity
{
    private TextView mContent;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setTitle("Details");

        setContentView(R.layout.activity_detail);

        mContent = (TextView) findViewById(R.id.content);

        handleExtras();
    }

    private void handleExtras()
    {
        Bundle extras = getIntent().getExtras();

        if(extras == null) return;

        String host = extras.getString("host");
        String path = extras.getString("path");
        String id = extras.getString("id");
        String teamName = extras.getString("teamname");

        StringBuilder builder = new StringBuilder();
        innerAppend("host", host, builder);
        innerAppend("path", path, builder);
        innerAppend("id", id, builder);
        innerAppend("teamName", teamName, builder);

        mContent.setText(builder.toString());
    }
}
