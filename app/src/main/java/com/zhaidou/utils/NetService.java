package com.zhaidou.utils;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.util.Log;

public class NetService
{
	private static String result;
	
	/**
	 * http get请求
	 * @param url
	 * @return
	 */
	public static String getHttpService(String url)
	{
		try
		{
			HttpGet httpGet=new HttpGet(url);
			HttpClient httpClient=new DefaultHttpClient();
			HttpResponse httpResponse=httpClient.execute(httpGet);
			if (httpResponse.getStatusLine().getStatusCode()==HttpStatus.SC_OK)
			{
				result=EntityUtils.toString(httpResponse.getEntity(), "utf-8");
				Log.i("zhaidou", "result:"+result);
			}
			else
			{
				result=null;
			}
		}
		catch (ClientProtocolException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		return result;
		
	}
}