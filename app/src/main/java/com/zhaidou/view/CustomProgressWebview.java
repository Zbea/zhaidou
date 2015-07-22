package com.zhaidou.view;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebView;
import android.widget.ProgressBar;

import com.zhaidou.R;

/**
 * Created by roy on 15/7/22.
 */
public class CustomProgressWebview extends WebView
{
	ProgressBar progressBar;

	public CustomProgressWebview(Context context, AttributeSet attrs)
	{
		super(context, attrs);

		progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
		progressBar.setLayoutParams(new LayoutParams(android.view.ViewGroup.LayoutParams.FILL_PARENT, 5, 0, 0));
		progressBar.setMax(100);  
		addView(progressBar);
		setWebChromeClient(new WebChromeClient());

	}

	public class WebChromeClient extends android.webkit.WebChromeClient
	{
		@Override
		public void onProgressChanged(WebView view, int newProgress)
		{
			if (newProgress == 100)
			{
				progressBar.setVisibility(GONE);
			}
			else
			{
				if (progressBar.getVisibility() == GONE) progressBar.setVisibility(VISIBLE);
				progressBar.setProgress(newProgress);
			}
			super.onProgressChanged(view, newProgress);
		}
	}
}
