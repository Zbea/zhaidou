package com.zhaidou.fragments;


import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
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
import com.zhaidou.model.Order;
import com.zhaidou.utils.SharedPreferencesUtil;
import com.zhaidou.utils.ToolUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class UnReceiveFragment extends BaseFragment {
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    private Dialog mDialog;
    private LinearLayout loadingView;

    private RequestQueue mRequestQueue;
    private UnReceiveAdapter unReceiveAdapter;
    private ListView mListView;
    private List<Order> orders;
    private final int STATUS_UNRECEIVE_LIST = 14;
    private String token;
    private View rootView;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case STATUS_UNRECEIVE_LIST:
                    loadingView.setVisibility(View.GONE);
                    unReceiveAdapter.notifyDataSetChanged();
                    break;
            }
        }
    };

    public static UnReceiveFragment newInstance(String param1, String param2) {
        UnReceiveFragment fragment = new UnReceiveFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public UnReceiveFragment() {
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
        if (null != rootView) {
            ViewGroup parent = (ViewGroup) rootView.getParent();
            if (null != parent) {
                parent.removeView(rootView);
            }
        } else {
            rootView = inflater.inflate(R.layout.fragment_un_receive, container, false);
            initView(rootView);
        }
        return rootView;
    }

    private void initView(View view) {
        mDialog = CustomLoadingDialog.setLoadingDialog(getActivity(), "loading");
        loadingView = (LinearLayout) view.findViewById(R.id.loadingView);
        mListView = (ListView) view.findViewById(R.id.lv_unreceivelist);
        token = (String) SharedPreferencesUtil.getData(getActivity(), "token", "");
        orders = new ArrayList<Order>();
        unReceiveAdapter = new UnReceiveAdapter(getActivity(), orders);
        mListView.setAdapter(unReceiveAdapter);
        mRequestQueue = Volley.newRequestQueue(getActivity());
        FetchReceiveData();
        unReceiveAdapter.setOnInViewClickListener(R.id.bt_logistics, new BaseListAdapter.onInternalClickListener() {
            @Override
            public void OnClickListener(View parentV, View v, Integer position, Object values) {
                final Order order = (Order) values;
                if ("4".equalsIgnoreCase(order.getStatus())) {
                    LogisticsMsgFragment logisticsMsgFragment = LogisticsMsgFragment.newInstance("", "");
                    ((MainActivity) getActivity()).navigationToFragment(logisticsMsgFragment);
                } else if ("1".equalsIgnoreCase(order.getStatus())) {
//                    AfterSaleFragment afterSaleFragment = AfterSaleFragment.newInstance(order.getOrderId() + "", order.getStatus());
//                    ((MainActivity) getActivity()).navigationToFragment(afterSaleFragment);
                    final Dialog dialog = new Dialog(getActivity(), R.style.custom_dialog);

                    View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_custom_collect_hint, null);
                    TextView textView = (TextView) view.findViewById(R.id.tv_msg);
                    textView.setText("是否申请退款?");
                    TextView cancelTv = (TextView) view.findViewById(R.id.cancelTv);
                    cancelTv.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            dialog.dismiss();
                        }
                    });

                    TextView okTv = (TextView) view.findViewById(R.id.okTv);
                    okTv.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, ZhaiDou.URL_ORDER_LIST + "/" + order.getOrderId() + "/update_status?status=3", new Response.Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject jsonObject) {
                                    JSONObject orderObj = jsonObject.optJSONObject("order");
                                    if (orderObj != null) {
                                        String status = orderObj.optString("status");
                                        if ("3".equalsIgnoreCase(status)) {
                                            orders.remove(order);
                                        }
                                        handler.sendEmptyMessage(STATUS_UNRECEIVE_LIST);
                                    }
                                }
                            }, new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError volleyError) {

                                }
                            }) {
                                @Override
                                public Map<String, String> getHeaders() throws AuthFailureError {
                                    Map<String, String> headers = new HashMap<String, String>();
                                    headers.put("SECAuthorization", token);
                                    return headers;
                                }
                            };
                            mRequestQueue.add(request);
                            dialog.dismiss();
                        }
                    });
                    dialog.setCanceledOnTouchOutside(true);
                    dialog.setCancelable(true);
                    dialog.addContentView(view, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    dialog.show();
                }
            }
        });
        unReceiveAdapter.setOnInViewClickListener(R.id.bt_received, new BaseListAdapter.onInternalClickListener() {
            @Override
            public void OnClickListener(View parentV, View v, final Integer position, Object values) {
                final Order order = (Order) values;
                final Dialog dialog = new Dialog(getActivity(), R.style.custom_dialog);

                View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_custom_collect_hint, null);
                TextView textView = (TextView) view.findViewById(R.id.tv_msg);
                textView.setText("是否确认收货?");
                TextView cancelTv = (TextView) view.findViewById(R.id.cancelTv);
                cancelTv.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();
                    }
                });

                TextView okTv = (TextView) view.findViewById(R.id.okTv);
                okTv.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, ZhaiDou.URL_ORDER_LIST + "/" + order.getOrderId() + "/update_status?status=5", new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject jsonObject) {
                                JSONObject orderObj = jsonObject.optJSONObject("order");
                                if (orderObj != null) {
                                    String status = orderObj.optString("status");
                                    if ("5".equalsIgnoreCase(status)) {
                                        orders.remove(order);
                                    }
                                    handler.sendEmptyMessage(STATUS_UNRECEIVE_LIST);
                                }
                            }
                        }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError volleyError) {

                            }
                        }) {
                            @Override
                            public Map<String, String> getHeaders() throws AuthFailureError {
                                Map<String, String> headers = new HashMap<String, String>();
                                headers.put("SECAuthorization", token);
                                return headers;
                            }
                        };
                        mRequestQueue.add(request);
                        dialog.dismiss();
                    }
                });
                dialog.setCanceledOnTouchOutside(true);
                dialog.setCancelable(true);
                dialog.addContentView(view, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                dialog.show();
            }
        });
        unReceiveAdapter.setOnInViewClickListener(R.id.ll_unreceive, new BaseListAdapter.onInternalClickListener() {
            @Override
            public void OnClickListener(View parentV, View v, Integer position, Object values) {
                final Order order = (Order) values;
                OrderDetailFragment orderDetailFragment = OrderDetailFragment.newInstance(order.getOrderId() + "", order.getOver_at(), order);
                ((MainActivity) getActivity()).navigationToFragment(orderDetailFragment);
                orderDetailFragment.setOrderListener(new OrderDetailFragment.OrderListener() {
                    @Override
                    public void onOrderStatusChange(Order o) {
                        Log.i("o-------------->", o.getStatus());
                        Log.i("order.getStatus()---------->", order.getStatus());
                        if ("5".equalsIgnoreCase(o.getStatus())) {
                            orders.remove(order);
                            handler.sendEmptyMessage(STATUS_UNRECEIVE_LIST);
                        }
                    }
                });
            }
        });
    }

    private void FetchReceiveData() {
        JsonObjectRequest request = new JsonObjectRequest(ZhaiDou.URL_ORDER_LIST + "?status=1,4", new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                if (mDialog != null)
                    mDialog.dismiss();
                Log.i("FetchReceiveData----------->", jsonObject.toString());
                if (jsonObject != null) {
                    JSONArray orderArr = jsonObject.optJSONArray("orders");
                    if (orderArr != null && orderArr.length() > 0) {
                        orders.clear();
                        for (int i = 0; i < orderArr.length(); i++) {
                            JSONObject orderObj = orderArr.optJSONObject(i);
                            int id = orderObj.optInt("id");
                            String number = orderObj.optString("number");
                            int amount = orderObj.optInt("amount");
                            String status = orderObj.optString("status");
                            String status_ch = orderObj.optString("status_ch");
                            String created_at = orderObj.optString("created_at");
                            String created_at_for = orderObj.optString("created_at_for");
                            String img = orderObj.optString("merch_img");
                            long over_at = orderObj.optLong("over_at");
                            Order order = new Order(id, number, amount, status, status_ch, created_at_for, created_at, "", 0);
                            order.setImg(img);
                            order.setOver_at(over_at);
                            if ((STATUS_UNRECEIVE_LIST + "").contains(status))
                                orders.add(order);
                        }
                        handler.sendEmptyMessage(STATUS_UNRECEIVE_LIST);
                    } else {
                        mListView.setVisibility(View.GONE);
                        loadingView.setVisibility(View.VISIBLE);
                    }
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Toast.makeText(getActivity(), "网络异常", Toast.LENGTH_SHORT).show();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<String, String>();
                headers.put("SECAuthorization", token);
                return headers;
            }
        };
        mRequestQueue.add(request);
    }

    public class UnReceiveAdapter extends BaseListAdapter<Order> {
        public UnReceiveAdapter(Context context, List<Order> list) {
            super(context, list);
        }

        @Override
        public View bindView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = mInflater.inflate(R.layout.item_order_unreceive, null);
            TextView tv_order_time = ViewHolder.get(convertView, R.id.tv_unreceive_order_time);
            TextView tv_order_number = ViewHolder.get(convertView, R.id.tv_order_number);
            TextView tv_order_amount = ViewHolder.get(convertView, R.id.tv_order_amount);
            TextView tv_order_status = ViewHolder.get(convertView, R.id.tv_order_status);
            TextView bt_received = ViewHolder.get(convertView, R.id.bt_received);
            TextView bt_logistics = ViewHolder.get(convertView, R.id.bt_logistics);
            ImageView iv_order_img = ViewHolder.get(convertView, R.id.iv_order_img);
            Order item = getList().get(position);
            tv_order_time.setText(item.getCreated_at_for());
            tv_order_number.setText(item.getNumber());
            tv_order_amount.setText("￥" + item.getAmount() + "");
            tv_order_status.setText(item.getStatus_ch());
            if ("1".equalsIgnoreCase(item.getStatus())) {
                bt_received.setVisibility(View.GONE);
                bt_logistics.setText("申请退款");
            } else if ("4".equalsIgnoreCase(item.getStatus())) {
                bt_received.setVisibility(View.VISIBLE);
                bt_logistics.setText("查看物流");
            }
            ToolUtils.setImageCacheUrl(item.getImg(), iv_order_img);
            return convertView;
        }
    }
}
