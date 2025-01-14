package com.zhaidou.fragments;

import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.umeng.analytics.MobclickAgent;
import com.zhaidou.MainActivity;
import com.zhaidou.R;
import com.zhaidou.ZDApplication;
import com.zhaidou.ZhaiDou;
import com.zhaidou.base.AccountManage;
import com.zhaidou.base.BaseActivity;
import com.zhaidou.base.BaseFragment;
import com.zhaidou.base.CartCountManager;
import com.zhaidou.base.CountManager;
import com.zhaidou.dialog.CustomLoadingDialog;
import com.zhaidou.dialog.CustomVersionUpdateDialog;
import com.zhaidou.easeui.helpdesk.EaseHelper;
import com.zhaidou.model.ZhaiDouRequest;
import com.zhaidou.utils.Api;
import com.zhaidou.utils.DialogUtils;
import com.zhaidou.utils.NetworkUtils;
import com.zhaidou.utils.SharedPreferencesUtil;
import com.zhaidou.utils.ToolUtils;
import com.zhaidou.view.TypeFaceTextView;

import org.json.JSONObject;

import java.io.File;
import java.text.DecimalFormat;

public class SettingFragment extends BaseFragment implements View.OnClickListener{
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private static final int CLEAR_USER_DATA = 0;

    private String mParam1;
    private String mParam2;

    ProfileFragment mProfileFragment;
    private TextView tv_size;

    RequestQueue requestQueue;
    private DialogUtils mDialogUtil;
    private Dialog mDialog;
    private String cachePath="/sdcard/zhaidou/";
    private Context mContext;
    private String serverName;
    private String serverInfo;
    private String serverUrl;
    private int serverCode;
    private TextView titleTv;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CLEAR_USER_DATA:
                    SharedPreferencesUtil.clearUser(getActivity());
                    CountManager.getInstance().clearCache();
                    AccountManage.getInstance().notifyLogOut();
                    EaseHelper.getInstance().logout(true,null);
                            ((MainActivity) mContext).logout(SettingFragment.this);
                    NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
                    notificationManager.cancel(0525);
                    CartCountManager.newInstance().notify(0);
                    break;
            }
        }
    };

    public static SettingFragment newInstance(String param1, String param2) {
        SettingFragment fragment = new SettingFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public SettingFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setting, container, false);

        mContext = getActivity();
        mDialogUtil = new DialogUtils(mContext);
        LinearLayout versionBtn = (LinearLayout) view.findViewById(R.id.ll_version);
        versionBtn.setOnClickListener(this);

        titleTv = (TypeFaceTextView) view.findViewById(R.id.title_tv);
        titleTv.setText(R.string.title_setting);

        tv_size = (TextView) view.findViewById(R.id.tv_size);

        view.findViewById(R.id.rl_back).setOnClickListener(this);
        view.findViewById(R.id.ll_recommend).setOnClickListener(this);
        view.findViewById(R.id.ll_profile).setOnClickListener(this);
        view.findViewById(R.id.ll_psw_change).setOnClickListener(this);
        view.findViewById(R.id.ll_collocation).setOnClickListener(this);
        view.findViewById(R.id.ll_add_v).setOnClickListener(this);
        view.findViewById(R.id.ll_version).setOnClickListener(this);
        view.findViewById(R.id.ll_clear).setOnClickListener(this);
        view.findViewById(R.id.ll_about).setOnClickListener(this);
        view.findViewById(R.id.bt_logout).setOnClickListener(this);
        requestQueue = ZDApplication.newRequestQueue();

        tv_size.setText(setCountSize());

        return view;
    }

    /**
     * 计算大小
     */
    private String setCountSize()
    {
        long size=0;
        File file=new File(cachePath);
        if (file.exists())
        {
            if (file.isDirectory())
            {
                size=getFileSizes(file);
            }
        }
        String sies=FormetFileSize(size);
        return sies;
    }

    /**
     * 获取指定文件夹
     * @param file
     * @return
     */
    private long getFileSizes(File file)
    {
        long size=0;
        if (file.exists())
        {
            if (file.isDirectory())
            {
                File[] files=file.listFiles();
                for (int i = 0; i < files.length; i++)
                {
                    if (files[i].isDirectory())
                    {
                        size=size+getFileSizes(files[i]);
                    }
                    else
                    {
                        size=size+file.length();
                    }
                }

            }
        }
        return size;
    }

    /**
     * 转换文件大小
     *
     * @param fileS
     * @return
     */
    private String FormetFileSize(long fileS) {
        DecimalFormat df = new DecimalFormat("#.00");
        String fileSizeString = "";
        String wrongSize = "0B";
        if (fileS == 0) {
            return wrongSize;
        }
        if (fileS < 1024) {
            fileSizeString = df.format((double) fileS) + "B";
        } else if (fileS < 1048576) {
            fileSizeString = df.format((double) fileS / 1024) + "KB";
        } else if (fileS < 1073741824) {
            fileSizeString = df.format((double) fileS / 1048576) + "MB";
        } else {
            fileSizeString = df.format((double) fileS / 1073741824) + "GB";
        }
        return fileSizeString;
    }


    private void deleteFile(File file)
    {
        if (file.exists())
        {
            if (file.isFile())
            {
                file.delete();
                return;
            }
            else
            {
                File[] childFile=file.listFiles();
                if (childFile==null&&childFile.length==0)
                {
                    file.delete();
                }
                else
                {
                    for (File f:childFile)
                    {
                        deleteFile(f);
                    }
                }
            }
        }
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.rl_back:
                ((BaseActivity) getActivity()).popToStack(SettingFragment.this);
                break;
            case R.id.ll_profile:
                mProfileFragment = ProfileFragment.newInstance("", "");
                ((BaseActivity) getActivity()).navigationToFragmentWithAnim(mProfileFragment);
                break;
            case R.id.ll_psw_change:
                ModifyPswFragment modifyPswFragment = new ModifyPswFragment();
                ((BaseActivity) getActivity()).navigationToFragment(modifyPswFragment);
                break;
            case R.id.ll_collocation:
                ImageBgFragment fragment = ImageBgFragment.newInstance("豆搭教程");
                ((BaseActivity) getActivity()).navigationToFragmentWithAnim(fragment);
                break;
            case R.id.ll_recommend:
                SettingRecommendFragment settingRecommendFragment = SettingRecommendFragment.newInstance("", "");
                ((BaseActivity) getActivity()).navigationToFragmentWithAnim(settingRecommendFragment);
                break;
            case R.id.ll_add_v:
                ImageBgFragment addVFragment = ImageBgFragment.newInstance("如何加V");
                ((BaseActivity) getActivity()).navigationToFragmentWithAnim(addVFragment);
                break;
            case R.id.ll_clear:

                deleteFile(new File(cachePath));
                new Handler().postDelayed(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        ToolUtils.setToast(mContext,"清除缓存成功");
                        tv_size.setText("0B");
                    }
                },1000);

                break;
            case R.id.ll_about:
                AboutFragment aboutFragment = AboutFragment.newInstance("", "");
                ((BaseActivity) getActivity()).navigationToFragmentWithAnim(aboutFragment);
                break;
            case R.id.bt_logout:
                mDialogUtil.showDialog("确定退出登录？", new DialogUtils.PositiveListener() {
                    @Override
                    public void onPositive() {
                        if (NetworkUtils.isNetworkAvailable(mContext)) {
                            logout();
                        } else {
                            ToolUtils.setToast(mContext, "抱歉,网络连接失败");
                        }
                    }
                }, null);
                break;
            case R.id.ll_version:
                if (NetworkUtils.isNetworkAvailable(mContext)) {
                    FetchAPK();
                } else {
                    ToolUtils.setToast(mContext, "抱歉,网络连接失败");
                }
                break;
            default:
                break;
        }
    }

    private void FetchAPK()
    {
        Api.getApkManage(new Api.SuccessListener()
        {
            @Override
            public void onSuccess(Object jsonObject)
            {
                if (jsonObject != null)
                {
                    JSONObject object = ((JSONObject) jsonObject).optJSONObject("data");
                    if (object != null)
                        serverName = object.optString("app_version");
                    serverCode = object.optInt("code_version");
                    serverUrl = object.optString("package_url");
                    if (serverCode > ZDApplication.localVersionCode) {
                        CustomVersionUpdateDialog customVersionUpdateDialog = new CustomVersionUpdateDialog(mContext, serverName, serverUrl);
                        customVersionUpdateDialog.checkUpdateInfo();
                    } else {
                        ToolUtils.setToast(mContext, "当前版本为最新版本");
                    }
                }
            }
        }, null);
    }

    public void logout() {
        mDialog= CustomLoadingDialog.setLoadingDialog(mContext,"");
        final String token = (String) SharedPreferencesUtil.getData(mContext, "token", "");
        ZhaiDouRequest request = new ZhaiDouRequest(ZhaiDou.USER_LOGOUT_URL + "?token=" + token
                , new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                if (mDialog != null) mDialog.dismiss();
                if (jsonObject==null)
                {
                    ShowToast("抱歉,退出失败");
                    return;
                }
                int status = jsonObject.optInt("status");
                String message = jsonObject.optString("message");
                if (status == 200) {
                    String msg = jsonObject.optJSONObject("data").optString("message");
                    ShowToast(msg);
                    mHandler.sendEmptyMessage(CLEAR_USER_DATA);
                } else {
                    ShowToast(message);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                if (mDialog != null) mDialog.dismiss();
                ShowToast("抱歉,退出失败");
            }
        });
        requestQueue.add(request);
    }

    public void onResume() {
        super.onResume();
        MobclickAgent.onPageStart(mContext.getResources().getString(R.string.title_setting));
    }

    public void onPause() {
        super.onPause();
        MobclickAgent.onPageEnd(mContext.getResources().getString(R.string.title_setting));
    }

}
