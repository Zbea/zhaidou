package com.zhaidou.fragments;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.pulltorefresh.PullToRefreshBase;
import com.pulltorefresh.PullToRefreshListView;
import com.umeng.analytics.MobclickAgent;
import com.zhaidou.R;
import com.zhaidou.ZhaiDou;
import com.zhaidou.base.BaseActivity;
import com.zhaidou.base.BaseFragment;
import com.zhaidou.base.BaseListAdapter;
import com.zhaidou.base.CountManager;
import com.zhaidou.base.ViewHolder;
import com.zhaidou.model.Order1;
import com.zhaidou.model.Store;
import com.zhaidou.model.ZhaiDouRequest;
import com.zhaidou.utils.DialogUtils;
import com.zhaidou.utils.NetworkUtils;
import com.zhaidou.utils.SharedPreferencesUtil;
import com.zhaidou.utils.ToolUtils;
import com.zhaidou.view.TypeFaceTextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class OrderAllOrdersFragment extends BaseFragment implements View.OnClickListener, PullToRefreshBase.OnRefreshListener2<ListView> {
    private static final String ARG_TYPE = "type";
    private static final String ARG_PARAM2 = "param2";


    private String mParam2;

    private Dialog mDialog;
    private LinearLayout loadingView;
    private TextView titleTv;
    private RequestQueue mRequestQueue;
    private PullToRefreshListView mListView;
    AllOrderAdapter allOrderAdapter;
    private final int UPDATE_ORDER_LIST = 1;
    private final int UPDATE_COUNT_DOWN_TIME = 2;
    private final int UPDATE_UI_TIMER_FINISH = 3;
    private MyTimer timer;
    private View rootView;
    private String token;
    private long preTime = 0;
    private long timeStmp = 0;
    private Context mContext;
    private boolean isViewDestroy = false;
    private View mEmptyView, mNetErrorView;
    private Map<Integer, Boolean> timerMap = new HashMap<Integer, Boolean>();
    private boolean isTimerStart = false;
    private List<Order1> mOrderList;
    private DialogUtils mDialogUtils;
    private int mCurrentPage;
    private String mCurrentType;
    private boolean isDataLoaded = false;
    private String mUserId;
    private Map<Integer, Long> timerMapStamp = new HashMap<Integer, Long>();
    private boolean hasUnPayOrder = false;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            isViewDestroy = !isViewDestroy;
            switch (msg.what) {
                case UPDATE_ORDER_LIST:
                    loadingView.setVisibility(View.GONE);
                    mListView.setVisibility(View.VISIBLE);
                    allOrderAdapter.notifyDataSetChanged();
                    mListView.onRefreshComplete();
                    if (timer == null) {
                        timer = new MyTimer(15 * 60 * 1000, 1000);
                        timer.start();
                    }
                    break;
                case UPDATE_COUNT_DOWN_TIME:
                    if (mOrderList != null && mOrderList.size() > 0) {
                        loadingView.setVisibility(View.GONE);
                        allOrderAdapter.notifyDataSetChanged();
                    } else {
                        mListView.setVisibility(View.GONE);
                        loadingView.setVisibility(View.VISIBLE);
                    }
                    break;
            }
        }
    };

    public static OrderAllOrdersFragment newInstance(String type, String param2) {
        OrderAllOrdersFragment fragment = new OrderAllOrdersFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TYPE, type);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public OrderAllOrdersFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mCurrentType = getArguments().getString(ARG_TYPE);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (null != rootView) {
            ViewGroup parent = (ViewGroup) rootView.getParent();
            if (null != parent) {
                parent.removeView(rootView);
            }
        } else {
            rootView = inflater.inflate(R.layout.fragment_all_orders, container, false);

            mContext = getActivity();
            mOrderList = new ArrayList<Order1>();
            mDialogUtils = new DialogUtils(mContext);

            titleTv = (TypeFaceTextView) rootView.findViewById(R.id.title_tv);
            titleTv.setText(R.string.title_all_order);

            loadingView = (LinearLayout) rootView.findViewById(R.id.loadingView);
            mEmptyView = rootView.findViewById(R.id.nullline);
            mNetErrorView = rootView.findViewById(R.id.nullNetline);
            rootView.findViewById(R.id.netReload).setOnClickListener(this);
            mListView = (PullToRefreshListView) rootView.findViewById(R.id.lv_all_orderlist);
            mListView.setMode(PullToRefreshBase.Mode.BOTH);
            mListView.setOnRefreshListener(this);

            mRequestQueue = Volley.newRequestQueue(getActivity());
            allOrderAdapter = new AllOrderAdapter(getActivity(), mOrderList);
            mListView.setAdapter(allOrderAdapter);
            token = (String) SharedPreferencesUtil.getData(mContext, "token", "");
            mUserId = SharedPreferencesUtil.getData(mContext, "userId", -1) + "";

            initData();
            allOrderAdapter.setOnInViewClickListener(R.id.orderlayout, new BaseListAdapter.onInternalClickListener() {
                @Override
                public void OnClickListener(View parentV, View v, Integer position, Object values) {
                    final Order1 order = (Order1) values;
                    final TextView btn2 = (TextView) parentV.findViewById(R.id.bt_received);
                    if (btn2.getTag() != null)
                        preTime = Long.parseLong(btn2.getTag().toString());
                    OrderDetailFragment orderDetailFragment = OrderDetailFragment.newInstance(order.orderCode, 2);
                    ((BaseActivity) getActivity()).navigationToFragment(orderDetailFragment);
                }
            });
            allOrderAdapter.setOnInViewClickListener(R.id.bt_logistics, new BaseListAdapter.onInternalClickListener() {
                @Override
                public void OnClickListener(View parentV, View v, Integer position, Object values) {
                    final Order1 order = (Order1) values;
                    if (ZhaiDou.STATUS_UNDELIVERY == order.status) {
                        List<Store> childOrderPOList = order.childOrderPOList;
                        if (childOrderPOList.size() == 1 && childOrderPOList.get(0).orderItemPOList.size() == 1 && childOrderPOList.get(0).orderItemPOList.get(0).productType == 2) {
                            ShowToast("零元特卖商品不可以退哦！");
                            return;
                        }

                        mDialogUtils.showDialog(mContext.getResources().getString(R.string.order_apply_return_money), new DialogUtils.PositiveListener() {
                            @Override
                            public void onPositive() {
                                final Map<String, String> params = new HashMap<String, String>();
                                params.put("businessType", "01");
                                params.put("clientType", "ANDROID");
                                params.put("version", "1.0.1");
                                params.put("userId", mUserId);
                                params.put("clientVersion", "45");
                                params.put("orderCode", order.orderCode);
                                ZhaiDouRequest request = new ZhaiDouRequest(mContext,Request.Method.POST, ZhaiDou.URL_ORDER_APPLY_CANCEL,params, new Response.Listener<JSONObject>() {
                                    @Override
                                    public void onResponse(JSONObject jsonObject) {
                                        int status = jsonObject.optInt("status");
                                        String message = jsonObject.optString("message");
                                        if (200 == status) {
                                            order.status = ZhaiDou.STATUS_ORDER_APPLY_CANCEL;
                                            order.orderShowStatus = "退款申请中";
                                        }
                                        ShowToast(status == 200 ? "正在申请退款" : message);
                                    }
                                }, null);
                                mRequestQueue.add(request);
                            }
                        }, null);
                    } else if (ZhaiDou.STATUS_UNPAY == order.status) {
                        final Map<String, String> params = new HashMap<String, String>();
                        params.put("businessType", "01");
                        params.put("clientType", "ANDROID");
                        params.put("version", "1.0.1");
                        params.put("userId", mUserId);
                        params.put("clientVersion", "45");
                        params.put("orderCode", order.orderCode);
                        mDialogUtils.showDialog(mContext.getResources().getString(R.string.order_cancel_ok), new DialogUtils.PositiveListener() {
                            @Override
                            public void onPositive() {
                                ZhaiDouRequest request = new ZhaiDouRequest(mContext,Request.Method.POST, ZhaiDou.URL_ORDER_CANCEL,params, new Response.Listener<JSONObject>() {
                                    @Override
                                    public void onResponse(JSONObject jsonObject) {
                                        int status = jsonObject.optInt("status");
                                        String message = jsonObject.optString("message");
                                        if (200 == status) {
                                            order.status = ZhaiDou.STATUS_ORDER_CANCEL;
                                            order.orderShowStatus = "已取消";
                                            CountManager.getInstance().minus(CountManager.TYPE.TAG_PREPAY);
                                        }
                                        ShowToast(status == 200 ? "取消订单成功" : message);
                                    }
                                }, new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError volleyError) {

                                    }
                                });
                                mRequestQueue.add(request);
                            }
                        }, null);
                    }
                }
            });
            allOrderAdapter.setOnInViewClickListener(R.id.bt_received, new BaseListAdapter.onInternalClickListener() {
                @Override
                public void OnClickListener(View parentV, View v, Integer position, Object values) {
                    final Order1 order = (Order1) values;
                    if (ZhaiDou.STATUS_UNPAY == order.status) {
                        if (order.orderRemainingTime <= 0) {
                            ShowToast("订单已超时");
                            return;
                        }
                        ShopPaymentFragment shopPaymentFragment = ShopPaymentFragment.newInstance(order.orderId, order.orderCode, Double.parseDouble(order.orderPayAmount), Integer.parseInt(order.orderRemainingTime + ""), null);
                        ((BaseActivity) getActivity()).navigationToFragment(shopPaymentFragment);
                    } else if (ZhaiDou.STATUS__DELIVERYED == order.status) {
                        mDialogUtils.showDialog(mContext.getResources().getString(R.string.order_confirm), new DialogUtils.PositiveListener() {
                            @Override
                            public void onPositive() {
                                Map<String, String> params = new HashMap<String, String>();
                                params.put("businessType", "01");
                                params.put("clientType", "ANDROID");
                                params.put("userId", mUserId);
                                params.put("clientVersion", "45");
                                params.put("orderCode", order.orderCode);
                                ZhaiDouRequest request = new ZhaiDouRequest(mContext,Request.Method.POST, ZhaiDou.URL_ORDER_CONFIRM,params, new Response.Listener<JSONObject>() {
                                    @Override
                                    public void onResponse(JSONObject jsonObject) {
                                        int status = jsonObject.optInt("status");
                                        String message = jsonObject.optString("message");
                                        if (200 == status) {
                                            order.status = 50;
                                            order.orderShowStatus = "交易完成";
                                            allOrderAdapter.notifyDataSetChanged();
                                        } else {
                                            ShowToast(message);
                                        }
                                    }
                                }, null);
                                mRequestQueue.add(request);
                            }
                        }, null);
                    }
                }
            });
            allOrderAdapter.setOnInViewClickListener(R.id.iv_delete, new BaseListAdapter.onInternalClickListener() {
                @Override
                public void OnClickListener(View parentV, View v, final Integer position, Object values) {
                    final Order1 order = (Order1) values;
                    final Map<String, String> params = new HashMap<String, String>();
                    params.put("businessType", "01");
                    params.put("userId", mUserId);
                    params.put("orderCode", order.orderCode);
                    DialogUtils mDialogUtils = new DialogUtils(getActivity());
                    mDialogUtils.showDialog(mContext.getResources().getString(R.string.order_delete), new DialogUtils.PositiveListener() {
                        @Override
                        public void onPositive() {
                            ZhaiDouRequest request = new ZhaiDouRequest(mContext,Request.Method.POST, ZhaiDou.URL_ORDER_DELETE,params, new Response.Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject jsonObject) {
                                    int status = jsonObject.optInt("status");
                                    String message = jsonObject.optString("message");
                                    if (200 == status) {
                                        mOrderList.remove(order);
                                        allOrderAdapter.notifyDataSetChanged();
                                    } else {
                                        ShowToast(message);
                                    }
                                }
                            }, null);
                            mRequestQueue.add(request);
                        }
                    }, null);
                }
            });
        }

        titleTv.setText(ZhaiDou.TYPE_ORDER_ALL.equalsIgnoreCase(mCurrentType) ? "全部订单"
                : ZhaiDou.TYPE_ORDER_PREPAY.equalsIgnoreCase(mCurrentType) ? "待支付" : "待收货");
        return rootView;
    }

    private void initData() {
        if (NetworkUtils.isNetworkAvailable(mContext)) {
            isDataLoaded = true;
            mNetErrorView.setVisibility(View.GONE);
            loadingView.setVisibility(View.GONE);
            FetchOrderList(mCurrentPage = 1, mCurrentType);
        } else {
            mEmptyView.setVisibility(View.GONE);
            mNetErrorView.setVisibility(View.VISIBLE);
            loadingView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.netReload:
                initData();
                break;
        }
    }

    private void FetchOrderList(int page, final String type) {
        mDialog = mDialogUtils.showLoadingDialog();
        Map<String, String> params = new HashMap<String, String>();//28129
        params.put("userId", mUserId);//64410//16665//29650//mUserId
        params.put("clientType", "ANDROID");
        params.put("clientVersion", "45");
        params.put("businessType", "01");
        params.put("type",type);
        params.put("pageNo", page + "");
        params.put("pageSize", "10");// ZhaiDou.URL_ORDER_LIST,
        ZhaiDouRequest request = new ZhaiDouRequest(mContext,Request.Method.POST, ZhaiDou.URL_ORDER_LIST, params, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                if (mDialog != null) mDialog.dismiss();
                isDataLoaded = false;
                if (jsonObject==null)
                {
                    return;
                }
                ToolUtils.setLog(jsonObject.toString());
                int status = jsonObject.optInt("status");
                String message = jsonObject.optString("message");
                int totalCount = jsonObject.optInt("totalCount");
                if (type.equalsIgnoreCase(ZhaiDou.TYPE_ORDER_PREPAY))
                    CountManager.getInstance().init(CountManager.TYPE.TAG_PREPAY, totalCount);
                if (status == 200) {
                    JSONArray dataArray = jsonObject.optJSONArray("data");
                    int pageNo = jsonObject.optInt("pageNo");
                    if (dataArray != null && dataArray.length() < 10) {
//                        ShowToast("订单加载完毕");
                        mListView.onRefreshComplete();
                        mListView.setMode(PullToRefreshBase.Mode.PULL_FROM_START);
                    }
                    List<Order1> orderList = JSON.parseArray(dataArray.toString(), Order1.class);
                    if (pageNo == 0) {
                        mOrderList.clear();
                        timerMapStamp.clear();
                    }
                    mOrderList.addAll(orderList);
                    if (mOrderList.size() > 0) {
                        handler.sendEmptyMessage(UPDATE_ORDER_LIST);
                    } else {
                        mListView.setVisibility(View.GONE);
                        loadingView.setVisibility(View.VISIBLE);
                        mEmptyView.setVisibility(View.VISIBLE);
                    }
                } else {
                    ShowToast(message);
                }
                allOrderAdapter.notifyDataSetChanged();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                if (mDialog != null) mDialog.dismiss();
                if (allOrderAdapter.getCount() > 0) {
                    if (timer == null) {
                        timer = new MyTimer(15 * 60 * 1000, 1000);
                    }
                    timer.start();
                }
                Toast.makeText(mContext, "网络异常", Toast.LENGTH_SHORT).show();
            }
        });
        mRequestQueue.add(request);
    }

    @Override
    public void onPullDownToRefresh(PullToRefreshBase<ListView> refreshView) {
        mOrderList.clear();
        mListView.setMode(PullToRefreshBase.Mode.BOTH);
        FetchOrderList(mCurrentPage = 1, mCurrentType);
    }

    @Override
    public void onPullUpToRefresh(PullToRefreshBase<ListView> refreshView) {
        FetchOrderList(++mCurrentPage, mCurrentType);
    }

    public class AllOrderAdapter extends BaseListAdapter<Order1> {
        public AllOrderAdapter(Context context, List<Order1> list) {
            super(context, list);
        }

        @Override
        public View bindView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = mInflater.inflate(R.layout.item_order_list, null);
            TextView tv_order_time = ViewHolder.get(convertView, R.id.tv_order_time);
            TextView tv_order_number = ViewHolder.get(convertView, R.id.tv_order_number);
            TextView tv_order_amount = ViewHolder.get(convertView, R.id.tv_order_amount);
            TextView tv_order_status = ViewHolder.get(convertView, R.id.tv_order_status);
            ImageView iv_order_img = ViewHolder.get(convertView, R.id.iv_order_img);
            LinearLayout remarkLayout = ViewHolder.get(convertView, R.id.remarkLayout);
            TextView mRemarkView = ViewHolder.get(convertView, R.id.remark);
            TextView btn1 = ViewHolder.get(convertView, R.id.bt_logistics);
            TextView btn2 = ViewHolder.get(convertView, R.id.bt_received);
            LinearLayout iv_delete = ViewHolder.get(convertView, R.id.iv_delete);
            RelativeLayout ll_btn = ViewHolder.get(convertView, R.id.rl_btn);
            Order1 order = getList().get(position);
            tv_order_time.setText(order.creationTime);
            tv_order_number.setText(order.orderCode);
            tv_order_amount.setText("￥" + order.orderActualAmount);
            tv_order_status.setText(order.orderShowStatus);
            ToolUtils.setImageCacheUrl(order.childOrderPOList.get(0).orderItemPOList.get(0).pictureMiddleUrl, iv_order_img, R.drawable.icon_loading_defalut);
            if (TextUtils.isEmpty(order.remark)) {
                remarkLayout.setVisibility(View.GONE);
            } else {
                remarkLayout.setVisibility(View.VISIBLE);
                mRemarkView.setText("备注:" + order.remark);
            }
            hasUnPayOrder = false;
            switch (order.status) {
                case ZhaiDou.STATUS_UNPAY:
                    iv_delete.setVisibility(View.GONE);
                    tv_order_status.setText("未付款");
                    ll_btn.setVisibility(View.VISIBLE);
                    btn1.setVisibility(View.VISIBLE);
                    btn2.setVisibility(View.VISIBLE);
                    btn1.setText("取消订单");
                    if (timerMapStamp.get(position) == null) {
                        timerMapStamp.put(position, order.orderRemainingTime);// order.orderRemainingTime//(long)(new Random().nextInt(60)+20)
                    }
                    long l = Long.parseLong(timerMapStamp.get(position) + "");
                    if (l > 0) {
                        timerMapStamp.put(position, timerMapStamp.get(position) - 1);
                        order.orderRemainingTime = timerMapStamp.get(position) - 1;

                        btn2.setText("支付" + new SimpleDateFormat("mm:ss").format(new Date(l * 1000)));
                        btn2.setBackgroundResource(R.drawable.btn_red_click_selector);
                    } else {
                        if (!btn2.getText().toString().equalsIgnoreCase("超时过期"))
                            CountManager.getInstance().minus(CountManager.TYPE.TAG_PREPAY);
                        btn2.setText("超时过期");
                        ll_btn.setVisibility(View.GONE);
                        btn2.setBackgroundResource(R.drawable.btn_no_click_selector);
                    }
                    if (order.orderRemainingTime<=0)
                        tv_order_status.setText("超时过期");
                    hasUnPayOrder = true;
                    break;
                case ZhaiDou.STATUS_DELIVERY:
                    iv_delete.setVisibility(View.GONE);
                    ll_btn.setVisibility(View.GONE);
                    btn1.setVisibility(View.VISIBLE);
                    btn2.setVisibility(View.VISIBLE);
                    btn2.setText("确认收货");
                    btn1.setText("查看物流");
                    btn1.setBackgroundResource(R.drawable.btn_green_click_bg);
                    btn2.setBackgroundResource(R.drawable.btn_red_click_selector);
                    break;
                case ZhaiDou.STATUS_DEAL_SUCCESS:
                    iv_delete.setVisibility(View.VISIBLE);
                    tv_order_status.setText("交易完成");
                    ll_btn.setVisibility(View.GONE);
                    btn1.setVisibility(View.VISIBLE);
                    btn2.setText("申请退货");
                    btn2.setBackgroundResource(R.drawable.btn_green_click_bg);
                    btn1.setText("查看物流");
                    btn1.setBackgroundResource(R.drawable.btn_green_click_bg);
                    break;
                case ZhaiDou.STATUS_DEAL_CLOSE:
                    iv_delete.setVisibility(View.VISIBLE);
                    tv_order_status.setText("交易关闭");
                    ll_btn.setVisibility(View.GONE);
                    break;
                case ZhaiDou.STATUS_UNDELIVERY:
                    iv_delete.setVisibility(View.GONE);
                    ll_btn.setVisibility(View.VISIBLE);
                    btn2.setVisibility(View.GONE);
                    btn1.setText("申请退款");
                    if (order.childOrderPOList.size() == 1 && order.childOrderPOList.get(0).orderItemPOList.size() == 1 && order.childOrderPOList.get(0).orderItemPOList.get(0).productType == 2) {
                        ll_btn.setVisibility(View.GONE);
                    }
                    break;
                case ZhaiDou.STATUS_PART_DELIVERY:
                    iv_delete.setVisibility(View.GONE);
                    ll_btn.setVisibility(View.GONE);
                    break;
                case ZhaiDou.STATUS_ORDER_CANCEL:
                    iv_delete.setVisibility(View.VISIBLE);
                    ll_btn.setVisibility(View.GONE);
                    break;
                case ZhaiDou.STATUS_ORDER_APPLY_CANCEL://申请取消中
                    iv_delete.setVisibility(View.GONE);
                    ll_btn.setVisibility(View.GONE);
                    break;
                case ZhaiDou.STATUS_PICKINGUP:
                    iv_delete.setVisibility(View.GONE);
                    ll_btn.setVisibility(View.GONE);
                    break;
                case ZhaiDou.STATUS_RETURN_MONEY_SUCCESS:
                    ll_btn.setVisibility(View.GONE);
                    break;
                default:
                    ll_btn.setVisibility(View.GONE);
                    break;
            }
            return convertView;
        }
    }

    @Override
    public void onDestroyView() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        isViewDestroy = true;
        if (ZhaiDou.TYPE_ORDER_PREPAY.equalsIgnoreCase(mCurrentType))
            CountManager.getInstance().refreshData();
        super.onDestroyView();
    }


    @Override
    public void onResume() {
        if (!isDataLoaded) {//&&hasUnPayOrder
            mOrderList.clear();
            timerMapStamp.clear();
            FetchOrderList(mCurrentPage = 1, mCurrentType);
        }
        super.onResume();
        MobclickAgent.onPageStart(mContext.getResources().getString(R.string.title_all_order));
    }

    public void onPause() {
        super.onPause();
        MobclickAgent.onPageEnd(mContext.getResources().getString(R.string.title_all_order));
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        if (timer != null) {
            timer.cancel();
            isTimerStart = false;
            timer=null;
        }
        super.onStop();
    }

    private class MyTimer extends CountDownTimer {
        private MyTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long l) {
            handler.sendEmptyMessage(UPDATE_COUNT_DOWN_TIME);
        }

        @Override
        public void onFinish() {
            handler.sendEmptyMessage(UPDATE_UI_TIMER_FINISH);
        }
    }

}
