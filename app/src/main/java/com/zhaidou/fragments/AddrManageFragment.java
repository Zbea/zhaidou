package com.zhaidou.fragments;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.zhaidou.MainActivity;
import com.zhaidou.R;
import com.zhaidou.ZhaiDou;
import com.zhaidou.base.BaseFragment;
import com.zhaidou.base.BaseListAdapter;
import com.zhaidou.base.ViewHolder;
import com.zhaidou.dialog.CustomLoadingDialog;
import com.zhaidou.model.Address;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link AddrManageFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class AddrManageFragment extends BaseFragment implements View.OnClickListener{
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_NickName = "param1";
    private static final String ARG_MOBILE = "param2";
    private static final String ARG_ADDRESS = "param3";
    private static final String ARG_PROFILE_ID = "param4";
    private static final String ARG_STATUS = "param5";

    // TODO: Rename and change types of parameters
    private String mNickName;
    private String mMobile;
    private String mAddress;
    private String mProfileId;
    private int mStatus;

    private LinearLayout ll_edit_addr;
    private LinearLayout ll_manage_address;
    private EditText et_mobile,et_addr,et_name;
    private TextView tv_save,tv_edit,tv_addr_username,tv_addr_mobile,tv_addr,tv_delete;
    private String token;
    private SharedPreferences mSharedPreferences;

    private AddressListener addressListener;

    private Dialog mDialog;
    private RequestQueue mRequestQueue;
    private ListView mListview;
    private AddressAdapter addressAdapter;
    private List<Address> addressList=new ArrayList<Address>();
    private final int UPDATE_ADDRESS_LIST=0;
    private int mCheckedPosition=0;
    private View rootView;
    private Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case UPDATE_ADDRESS_LIST:
                    addressAdapter.notifyDataSetChanged();
                    break;
            }
        }
    };
    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment AddrManageFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static AddrManageFragment newInstance(String nickname,String mobile,String address, String profileId,int status) {
        AddrManageFragment fragment = new AddrManageFragment();
        Bundle args = new Bundle();
        args.putString(ARG_NickName, nickname);
        args.putString(ARG_MOBILE, mobile);
        args.putString(ARG_ADDRESS, address);
        args.putString(ARG_PROFILE_ID, profileId);
        args.putInt(ARG_STATUS,status);
        fragment.setArguments(args);
        return fragment;
    }
    public AddrManageFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mNickName = getArguments().getString(ARG_NickName);
            mMobile = getArguments().getString(ARG_MOBILE);
            mAddress = getArguments().getString(ARG_ADDRESS);
            mProfileId = getArguments().getString(ARG_PROFILE_ID);
            mStatus=getArguments().getInt(ARG_STATUS);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i("onCreateView----------------->","onCreateView");
        if (null != rootView) {
            ViewGroup parent = (ViewGroup) rootView.getParent();
            if (null != parent) {
                parent.removeView(rootView);
            }
        } else {
            rootView = inflater.inflate(R.layout.fragment_addr_manage,container, false);
            initView(rootView);
        }
        return rootView;
    }

    private void initView(View view){
        mListview=(ListView)view.findViewById(R.id.lv_addresses);
        addressAdapter=new AddressAdapter(getActivity(),addressList);
        mListview.setAdapter(addressAdapter);
        view.findViewById(R.id.bt_new_address).setOnClickListener(this);
        mRequestQueue= Volley.newRequestQueue(getActivity());
        mSharedPreferences=getActivity().getSharedPreferences("zhaidou", Context.MODE_PRIVATE);
        token=mSharedPreferences.getString("token", null);
        FetchData();
        addressAdapter.setOnInViewClickListener(R.id.tv_defalue_addr,new BaseListAdapter.onInternalClickListener() {
            @Override
            public void OnClickListener(View parentV, View v, Integer position, Object values) {
                mCheckedPosition=position;
                addressAdapter.notifyDataSetChanged();
            }
        });
        addressAdapter.setOnInViewClickListener(R.id.tv_delete,new BaseListAdapter.onInternalClickListener() {
            @Override
            public void OnClickListener(View parentV, View v, Integer position, Object values) {
                ShowToast("删除");
            }
        });
        addressAdapter.setOnInViewClickListener(R.id.tv_edit,new BaseListAdapter.onInternalClickListener() {
            @Override
            public void OnClickListener(View parentV, View v, Integer position, Object values) {
                ShowToast("编辑");
            }
        });
    }
    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.tv_save:
                hideInputMethod();
                String name=et_name.getText().toString();
                String mobile=et_mobile.getText().toString();
                String address=et_addr.getText().toString();
                if (TextUtils.isEmpty(name)){
                    Toast.makeText(getActivity(),"收货人信息不能为空",Toast.LENGTH_SHORT).show();
                    return;
                }else if (TextUtils.isEmpty(mobile)){
                    Toast.makeText(getActivity(),"联系方式不能为空",Toast.LENGTH_SHORT).show();
                    return;
                }else if (TextUtils.isEmpty(address)){
                    Toast.makeText(getActivity(),"收货地址不能为空",Toast.LENGTH_SHORT).show();
                    return;
                }
                mNickName=name;
                mMobile=mobile;
                mAddress=address;
                Log.i("hhh","dada");
                new MyTask().execute(name,mobile,address,mProfileId);
                break;
            case R.id.tv_edit:
                ll_edit_addr.setVisibility(View.VISIBLE);
                ll_manage_address.setVisibility(View.GONE);
                et_addr.setHint(mAddress);
                et_mobile.setHint(mMobile);
                et_name.setHint(mNickName);
                break;
            case R.id.tv_delete:
                Log.i("tv_addr_username.getText().toString()--->",tv_addr_username.getText().toString());
                Log.i("tv_addr_mobile.getText().toString()--->",tv_addr_mobile.getText().toString());
                new MyTask().execute(tv_addr_username.getText().toString(),tv_addr_mobile.getText().toString(),"",mProfileId);
                break;
            case R.id.bt_new_address:
                NewAddrFragment newAddrFragment=NewAddrFragment.newInstance("","","","",0);
                ((MainActivity)getActivity()).navigationToFragment(newAddrFragment);
                break;
        }
    }


    private class MyTask extends AsyncTask<String,Void,String> {

        @Override
        protected void onPreExecute()
        {
            mDialog= CustomLoadingDialog.setLoadingDialog(getActivity(), "loading");
        }

        @Override
        protected String doInBackground(String... strings) {

            String s = null;
            try {
                s =executeHttpPost(strings[0],strings[1],strings[2],strings[3]);
            }catch (Exception e){

            }
            return s;
        }

        @Override
        protected void onPostExecute(String s) {
            Log.i("AddrManageFragment--onPostExecute-->",s);
            try {
                mDialog.dismiss();
                JSONObject json =new JSONObject(s);
                JSONObject profile =json.optJSONObject("profile");
                String mobile=profile.optString("mobile");
                String address=profile.optString("address2");
                addressListener.onAddressDataChange(mNickName,mMobile,address);
            }catch (Exception e){

            }
        }
    }
    public String executeHttpPost(String name,String mobile,String addr,String id) throws Exception {
        Log.i("name--->",name==null?"":name);
        Log.i("mobile--->",mobile==null?"":mobile);
        Log.i("addr--->",addr==null?"":addr);
        Log.i("id--->",id==null?"":id);
        BufferedReader in = null;
        try {
            // 定义HttpClient
            HttpClient client = new DefaultHttpClient();


            // 实例化HTTP方法
            HttpPost request = new HttpPost(ZhaiDou.USER_EDIT_PROFILE_URL+id);
            request.addHeader("SECAuthorization", token);


            // 创建名/值组列表
            List<NameValuePair> parameters = new ArrayList<NameValuePair>();

            parameters.add(new BasicNameValuePair("_method","PUT"));
//            String newStr = new String(old.getBytes("UTF-8"));
            parameters.add(new BasicNameValuePair("profile[first_name]",name));
            parameters.add(new BasicNameValuePair("profile[mobile]",mobile));
            parameters.add(new BasicNameValuePair("profile[address2]",addr));
            parameters.add(new BasicNameValuePair("profile[id]",id));

            // 创建UrlEncodedFormEntity对象
            UrlEncodedFormEntity formEntiry = new UrlEncodedFormEntity(
                    parameters, HTTP.UTF_8);//这里要设置，不然回来乱码
            request.setEntity(formEntiry);
            // 执行请求
            HttpResponse response = client.execute(request);

            in = new BufferedReader(new InputStreamReader(response.getEntity()
                    .getContent()));
            StringBuffer sb = new StringBuffer("");
            String line = "";
            String NL = System.getProperty("line.separator");
            while ((line = in.readLine()) != null) {
                sb.append(line + NL);
            }
            in.close();
            String result = sb.toString();
            Log.i("EditProfileFragment--------->",result);
            return result;

        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void setAddressListener(AddressListener addressListener) {
        this.addressListener = addressListener;
    }

    public interface AddressListener{
        public void onAddressDataChange(String name,String mobile,String address);
    }


    private void FetchData(){
        JsonObjectRequest request=new JsonObjectRequest("http://192.168.199.173/special_mall/api/receivers",new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                Log.i("AddrManageFragment------------>",jsonObject.toString());
                JSONArray receiversArr=jsonObject.optJSONArray("receivers");
                if (receiversArr!=null&&receiversArr.length()>0){
                    for (int i=0;i<receiversArr.length();i++){
                        JSONObject receiverObj = receiversArr.optJSONObject(i);
                        String phone=receiverObj.optString("phone");
                        int user_id=receiverObj.optInt("user_id");
                        String addr=receiverObj.optString("address");
                        String name=receiverObj.optString("name");
                        Address address=new Address();
                        address.setAddress(addr);
                        address.setName(name);
                        address.setUser_id(user_id);
                        address.setPhone(phone);
                        addressList.add(address);
                    }
                    handler.sendEmptyMessage(UPDATE_ADDRESS_LIST);
                }
            }
        },new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Toast.makeText(getActivity(),"网络异常",Toast.LENGTH_SHORT).show();
            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String,String> headers=new HashMap<String, String>();
                if (!TextUtils.isEmpty(token))
                    headers.put("SECAuthorization","Yk77mfWaq_xYyeEibAxx");
                return headers;
            }
        };
        mRequestQueue.add(request);
    }

    public class AddressAdapter extends BaseListAdapter<Address> {
        public AddressAdapter(Context context, List<Address> list) {
            super(context, list);
        }

        @Override
        public View bindView(int position, View convertView, ViewGroup parent) {
            if (convertView==null)
                convertView=mInflater.inflate(R.layout.item_addresses_list,null);
            TextView tv_name = ViewHolder.get(convertView, R.id.tv_addr_username);
            TextView tv_mobile = ViewHolder.get(convertView, R.id.tv_addr_mobile);
            TextView tv_addr= ViewHolder.get(convertView, R.id.tv_addr);
            TextView tv_defalue=ViewHolder.get(convertView,R.id.tv_defalue_addr);
            Address address=getList().get(position);
            tv_name.setText(address.getName());
            tv_mobile.setText(address.getPhone());
            tv_addr.setText(address.getAddress());
            mCheckedPosition=address.isIs_default()?position:0;
            tv_defalue.setSelected(mCheckedPosition==position?true:false);
            return convertView;
        }
    }
}
