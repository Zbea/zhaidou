package com.zhaidou.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

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
import com.zhaidou.model.CountTime;
import com.zhaidou.model.Coupon;
import com.zhaidou.model.Product;
import com.zhaidou.utils.AsyncImageLoader1;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SpecialSaleFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class SpecialSaleFragment extends BaseFragment implements View.OnClickListener{
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;


    private GridView mGridView;
    private TextView mTimerView;
    private ProductAdapter mAdapter;
    private Map<Integer,View> mHashMap = new HashMap<Integer, View>();
    private AsyncImageLoader1 imageLoader;
    private MyTimer mTimer;
    private RequestQueue requestQueue;
    private List<Product> products = new ArrayList<Product>();

    private final int UPDATE_ADAPTER=0;
    private final int UPDATE_COUNT_DOWN_TIME=1;
    private final int UPDATE_UI_TIMER_FINISH=2;
    private final int UPDATE_TIMER_START=3;

    private Coupon mCoupon;
    private Handler mHandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case UPDATE_ADAPTER:
                    mAdapter.notifyDataSetChanged();
                    break;
                case UPDATE_COUNT_DOWN_TIME:
                    CountTime time = (CountTime)msg.obj;
                    String timerFormat = getResources().getString(R.string.timer);
                    String hourStr=String.format("%02d", time.getHour());
                    String minStr=String.format("%02d", time.getMinute());
                    String secondStr=String.format("%02d", time.getSecond());
                    String timer = String.format(timerFormat,time.getDay(),hourStr,minStr,secondStr);
                    Log.i("timer---->",timer);
                    mTimerView.setText(timer);
                    break;
                case UPDATE_UI_TIMER_FINISH:
                    mTimerView.setText("已结束");
                    break;
                case UPDATE_TIMER_START:
                    String date = (String)msg.obj;
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

                    try{
                        long millionSeconds = sdf.parse(date).getTime();//毫秒
//                        Log.i("millionSeconds",millionSeconds+"");
//                        Log.i("current---->",System.currentTimeMillis()+"");
                        long temp = millionSeconds-System.currentTimeMillis();
                        mTimer=new MyTimer(temp,1000);
                        mTimer.start();
                    }catch (Exception e){
                        Log.i("Exception e",e.getMessage());
                    }
                    break;
            }
        }
    };
    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment SpecialSaleFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SpecialSaleFragment newInstance(String param1, String param2) {
        SpecialSaleFragment fragment = new SpecialSaleFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }
    public SpecialSaleFragment() {
        // Required empty public constructor
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
        // Inflate the layout for this fragment
        View view=inflater.inflate(R.layout.fragment_special_sale, container, false);
        mGridView=(GridView)view.findViewById(R.id.gv_sale);
        mGridView.setEmptyView(mEmptyView);
        mTimerView=(TextView)view.findViewById(R.id.tv_count_time);
        mAdapter=new ProductAdapter(getActivity(),products);
        mGridView.setAdapter(mAdapter);
        view.findViewById(R.id.ll_back).setOnClickListener(this);
        view.findViewById(R.id.iv_coupon).setOnClickListener(this);
        requestQueue= Volley.newRequestQueue(getActivity());
        Log.i("onCreateView------------->",products.size()+"");
        FetchCouponData();
        if (products!=null&&products.size()==0)
            FetchData();


        mAdapter.setOnInViewClickListener(R.id.ll_single_layout,new BaseListAdapter.onInternalClickListener() {
            @Override
            public void OnClickListener(View parentV, View v, Integer position, Object values) {
                Log.i("value--->",values.toString());
                WebViewFragment webViewFragment = WebViewFragment.newInstance(((Product)values).getUrl(),true);
                ((MainActivity)getActivity()).navigationToFragment(webViewFragment);
            }
        });


        return view;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.ll_back:
                ((MainActivity)getActivity()).popToStack(SpecialSaleFragment.this);
                break;
            case R.id.iv_coupon:
                Log.i("iv_coupo------------->","iv_coupon");
                if (mCoupon!=null){
                    WebViewFragment webViewFragment =WebViewFragment.newInstance(mCoupon.getUrl(),true);
                    ((MainActivity)getActivity()).navigationToFragment(webViewFragment);
                }
                break;
        }
    }

    public void FetchData(){
        JsonObjectRequest request=new JsonObjectRequest(ZhaiDou.SPECIAL_SALE_URL,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject jsonObject) {

                        String end_date=jsonObject.optString("end_date");
                        Message timerMsg = new Message();
                        timerMsg.what=UPDATE_TIMER_START;
                        timerMsg.obj=end_date;
                        mHandler.sendMessage(timerMsg);

                        JSONArray items = jsonObject.optJSONArray("event_items");
                        if (items!=null&&items.length()>0){
                            for (int i=0;i<items.length();i++){
                                JSONObject item = items.optJSONObject(i);
                                int id = item.optInt("id");
                                int event_id=item.optInt("event_id");
                                String url=item.optString("url");
                                int price=item.optInt("price");
                                String title=item.optString("title");
                                String image=item.optJSONObject("image").optString("url");
                                int remaining=item.optInt("remaining");
                                Product product=new Product();
                                product.setId(id);
                                product.setPrice(price);
                                product.setUrl(url);
                                product.setTitle(title);
                                product.setImage(image);
                                products.add(product);
                            }
                            mHandler.sendEmptyMessage(UPDATE_ADAPTER);
                        }

                    }
                },new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {

            }
        });
        requestQueue.add(request);
    }
    public class ProductAdapter extends BaseListAdapter<Product> {
        public ProductAdapter(Context context, List<Product> list) {
            super(context, list);
            imageLoader = new AsyncImageLoader1(context);
        }

        @Override
        public View bindView(int position, View convertView, ViewGroup parent) {
            convertView=mHashMap.get(position);
            if (convertView==null)
                convertView=mInflater.inflate(R.layout.item_fragment_sale,null);
            TextView tv_name = ViewHolder.get(convertView, R.id.tv_name);
            ImageView image =ViewHolder.get(convertView,R.id.iv_single_item);
            TextView tv_money=ViewHolder.get(convertView,R.id.tv_money);
            TextView tv_price=ViewHolder.get(convertView,R.id.tv_price);
            TextView tv_count=ViewHolder.get(convertView,R.id.tv_count);

            Product product = getList().get(position);
            tv_name.setText(product.getTitle());
            imageLoader.LoadImage("http://"+product.getImage(),image);
            tv_price.setText("￥"+product.getPrice()+"元");
            tv_count.setText("剩余"+product.getRemaining()+"%");
            mHashMap.put(position,convertView);

            return convertView;
        }
    }


    public void FetchCouponData(){
        JsonObjectRequest request = new JsonObjectRequest(ZhaiDou.COUPON_DATA_URL,new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                Log.i("FetchCouponData----->",jsonObject.toString());
                int id=jsonObject.optInt("id");
                String created_at=jsonObject.optString("created_at");
                String updated_at=jsonObject.optString("updated_at");
                String for_date=jsonObject.optString("for_date");
                String url=jsonObject.optString("url");
                mCoupon=new Coupon(id,created_at,updated_at,for_date,url);
            }
        },new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {

            }
        });
        requestQueue.add(request);
    }
    @Override
    public void onDestroyView() {
        Log.i("onDestroyView---->","onDestroyView");
        if (mTimer!=null){
            mTimer.cancel();
            mTimer=null;
        }
        super.onDestroyView();
    }

    private class MyTimer extends CountDownTimer{
        private MyTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long l) {
//            Log.i("onTick------------>",l+"");
            long day=24*3600*1000;
            long hour=3600*1000;
            long minute=60*1000;
            //两个日期想减得到天数
            long dayCount= l/day;
            long hourCount= (l-(dayCount*day))/hour;
            long minCount=(l-(dayCount*day)-(hour*hourCount))/minute;
            long secondCount=(l-(dayCount*day)-(hour*hourCount)-(minCount*minute))/1000;
            CountTime time = new CountTime(dayCount,hourCount,minCount,secondCount);
            Message message =new Message();
            message.what=UPDATE_COUNT_DOWN_TIME;
            message.obj=time;
            mHandler.sendMessage(message);
        }

        @Override
        public void onFinish() {
            Log.i("onFinish---------->","onFinish");
            mHandler.sendEmptyMessage(UPDATE_UI_TIMER_FINISH);
        }
    }
}