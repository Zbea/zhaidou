package com.zhaidou.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.umeng.analytics.MobclickAgent;
import com.zhaidou.R;
import com.zhaidou.ZDApplication;
import com.zhaidou.ZhaiDou;
import com.zhaidou.base.BaseActivity;
import com.zhaidou.base.BaseFragment;
import com.zhaidou.model.ZhaiDouRequest;
import com.zhaidou.utils.DialogUtils;
import com.zhaidou.view.TypeFaceTextView;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 */
public class ModifyPswFragment extends BaseFragment {

    private TextView mCurrentPsw, mNewPsw, mConfirmPsw;

    private DialogUtils mDialogUtils;
    private TextView titleTv;

    public ModifyPswFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_modify_psw, container, false);

        titleTv = (TypeFaceTextView) view.findViewById(R.id.title_tv);
        titleTv.setText(R.string.title_change_psd);

        mConfirmPsw = (TextView) view.findViewById(R.id.confirm);
        mNewPsw = (TextView) view.findViewById(R.id.newPsw);
        mCurrentPsw = (TextView) view.findViewById(R.id.current);
        view.findViewById(R.id.commit).setOnClickListener(this);
        mDialogUtils = new DialogUtils(getActivity());
        return view;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.commit:
                String mCurrentText = mCurrentPsw.getText().toString();
                String mNewText = mNewPsw.getText().toString();
                String mConfirmText = mConfirmPsw.getText().toString();
                verifyInputMsg(mCurrentText, mNewText, mConfirmText);
                break;
        }
    }

    private void doModifyTask(String mCurrentText, String mNewText, String confirmText) {
        final Dialog dialog = mDialogUtils.showLoadingDialog();
        Map<String, String> params = new HashMap<String, String>();
        params.put("current_password", mCurrentText);
        params.put("password", mNewText);
        params.put("password_confirmation", confirmText);
        ZhaiDouRequest request = new ZhaiDouRequest(Request.Method.POST, ZhaiDou.USER_PSW_CHANGE_URL,params, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                if (dialog != null)
                    dialog.dismiss();
                int status = jsonObject.optInt("status");
                String message = jsonObject.optString("message");
                if (200 == status) {
                    JSONObject data = jsonObject.optJSONObject("data");
                    if (data != null) {
                        int pswStatus = data.optInt("status");
                        String msg = data.optString("message");
                        ShowToast(msg);
                        if (200 == pswStatus)
                            ((BaseActivity) getActivity()).popToStack(ModifyPswFragment.this);
                    }
                } else {
                    ShowToast(message);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                ShowToast("网络出现异常");
            }
        });
        ZDApplication.newRequestQueue().add(request);
    }

    private void verifyInputMsg(String mCurrentText, String mNewText, String mConfirmText) {
        if (TextUtils.isEmpty(mCurrentText)) {
            ShowToast("请填写当前登录密码");
            return;
        } else if (TextUtils.isEmpty(mNewText)) {
            ShowToast("新密码不能为空");
            return;
        } else if (TextUtils.isEmpty(mConfirmText)) {
            ShowToast("请再次填写密码");
            return;
        } else if (!mNewText.equals(mConfirmText)) {
            ShowToast("新密码与确认密码不一致");
            return;
        } else if (!TextUtils.isEmpty(mNewText) && mNewText.length() < 6) {
            ShowToast("密码最少6位");
            return;
        } else if (!TextUtils.isEmpty(mNewText) && mNewText.length() > 16) {
            ShowToast("密码最长16位");
            return;
        }
        doModifyTask(mCurrentText, mNewText, mConfirmText);
    }

    public void onResume() {
        super.onResume();
        MobclickAgent.onPageStart(mContext.getResources().getString(R.string.title_change_psd)); //统计页面
    }
    public void onPause() {
        super.onPause();
        MobclickAgent.onPageEnd(mContext.getResources().getString(R.string.title_change_psd));
    }

}
