package com.zhaidou;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Environment;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiscCache;
import com.nostra13.universalimageloader.cache.memory.impl.UsingFreqLimitedMemoryCache;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.utils.StorageUtils;
import com.tencent.bugly.crashreport.CrashReport;
import com.zhaidou.model.ZhaiDouRequest;
import com.zhaidou.utils.ToolUtils;

import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by wangclark on 15/7/2.
 */
public class ZDApplication extends Application{

    public static int localVersionCode;
    public static String localVersionName;

    private Typeface mTypeFace;
    private RequestQueue mRequestQueue;
    @Override
    public void onCreate() {

        super.onCreate();
        CrashReport.initCrashReport(this, "900008762", false);
        mRequestQueue= Volley.newRequestQueue(this);
        initTypeFace();
        try
        {
            PackageInfo packageInfo=getApplicationContext().getPackageManager().getPackageInfo(getPackageName(),0);
            localVersionCode=packageInfo.versionCode;
            localVersionName=packageInfo.versionName;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        creatFile();
        setImageLoad();

    }


    /**
     * universal_image_loader基本配置
     */
    private void setImageLoad()
    {
        File cacheDir = StorageUtils.getOwnCacheDirectory(getApplicationContext(), "zhaidou/image_cache/");
        ImageLoaderConfiguration configuration = new  ImageLoaderConfiguration.Builder(this)
                .threadPoolSize(3)//线程池加载的数量
                .diskCacheFileCount(50)//最大缓存数量
                .diskCacheSize(50 * 1024 * 1024) // 50 Mb sd卡(本地)缓存的最大值
                .diskCache(new UnlimitedDiscCache(cacheDir))//设置缓存路径
                .memoryCache(new UsingFreqLimitedMemoryCache(2* 1024 * 1024))
                .build();
        ImageLoader.getInstance().init(configuration);
    }

    private void creatFile()
    {
        String f;
        if (ToolUtils.hasSdcard())
        {
            f = Environment.getExternalStorageDirectory().getAbsolutePath() + "/zhaidou/";
        }
        else
        {
            f = getFilesDir().getAbsolutePath() + "/zhaidou/";
        }
        File file = new File(f);
        if (!file.exists())
        {
            file.mkdirs();
        }
    }


    private void initTypeFace(){
        if (mTypeFace==null){
            mTypeFace =Typeface.createFromAsset(getAssets(), "FZLTXHK.TTF");
        }
    }

    public Typeface getTypeFace() {
        return mTypeFace;
    }

    @Override
    public String toString() {
        return "ZDApplication{" +
                "mTypeFace=" + mTypeFace +
                '}';
    }
}
