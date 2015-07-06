package com.zhaidou.fragments;



import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.Volley;
import com.pulltorefresh.PullToRefreshBase;
import com.pulltorefresh.PullToRefreshGridView;
import com.zhaidou.R;
import com.zhaidou.ZhaiDou;
import com.zhaidou.activities.ItemDetailActivity;
import com.zhaidou.base.BaseFragment;
import com.zhaidou.base.BaseListAdapter;
import com.zhaidou.base.ViewHolder;
import com.zhaidou.model.Product;
import com.zhaidou.utils.AsyncImageLoader1;
import com.zhaidou.utils.HtmlFetcher;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SingleFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class SingleFragment extends BaseFragment implements PullToRefreshBase.OnRefreshListener2<GridView>{
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_FROM = "from";
    private static final String ID = "id";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private int id=-1;

    private int currentpage=1;
    private int count=-1;
    private int sort=0;

    private ImageView iv_heart;
    private TextView tv_money,tv_count,tv_detail;
    private PullToRefreshGridView gv_single;

    private List<Product> products = new ArrayList<Product>();
    private RequestQueue mRequestQueue;
    private AsyncImageLoader1 imageLoader;
    private SingleAdapter singleAdapter;

    private WeakHashMap<Integer,View> mHashMap = new WeakHashMap<Integer, View>();
//    private SingleAdapter mSingleAdapter;

    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
          singleAdapter.setList(products);
          gv_single.onRefreshComplete();
        }
    };


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param from Parameter 2.
     * @return A new instance of fragment SingleFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SingleFragment newInstance(String param1, String from) {
        SingleFragment fragment = new SingleFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_FROM, from);
        fragment.setArguments(args);
        return fragment;
    }
    public SingleFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_FROM);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view=inflater.inflate(R.layout.fragment_single, container, false);
        initView(view);
//        new FetchDataTask().execute();
        FetchData(mParam1,sort,currentpage=1);
        return view;
    }

    private void initView(View view){
        tv_count=(TextView)view.findViewById(R.id.tv_count);
//        tv_detail=(TextView)view.findViewById(R.id.tv_detail);
        tv_money=(TextView)view.findViewById(R.id.tv_money);
        gv_single=(PullToRefreshGridView)view.findViewById(R.id.gv_single);
        singleAdapter = new SingleAdapter(getActivity(),products);
        gv_single.setAdapter(singleAdapter);
        mRequestQueue= Volley.newRequestQueue(getActivity());
        Log.i("mParam2----------->",mParam2);
        if ("category".equalsIgnoreCase(mParam2)){

            FetchCategoryData(mParam1, 0, currentpage);
        }else {
            FetchData(mParam1,0,currentpage);
        }
//        FetchData(mParam1,0,1);

        gv_single.setMode(PullToRefreshBase.Mode.BOTH);
        gv_single.setOnRefreshListener(this);
        singleAdapter.setOnInViewClickListener(R.id.ll_single_layout,new BaseListAdapter.onInternalClickListener() {
            @Override
            public void OnClickListener(View parentV, View v, Integer position, Object values) {
                Product product=(Product)values;
                Intent detailIntent = new Intent(getActivity(), ItemDetailActivity.class);
                detailIntent.putExtra("id", product.getId()+"");
                detailIntent.putExtra("title", product.getTitle());
//                detailIntent.putExtra("cover_url", product.getImg_url());
                detailIntent.putExtra("url",product.getUrl());
                startActivity(detailIntent);
            }
        });

    }

    public void FetchData(String msg,int sort,int page){
        this.sort=sort;
        currentpage=page;
        if (page==1) products.clear();
        Map<String, String> params = new HashMap<String, String>();
        params.put("price", "desc");
        params.put("hot_d", "desc");
        params.put("search", msg);
        params.put("page",page+"");
        if (sort==1){
            params.put("hot_d","desc");
        }else if (sort==2){
            params.put("price","asc");
        }else if (sort==3){
            params.put("price","desc");
        }

        JsonObjectRequest newMissRequest = new JsonObjectRequest(
                Request.Method.POST, ZhaiDou.SEARCH_PRODUCT_URL,
                new JSONObject(params), new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject json) {
                Log.i("SingleFragment---->",json.toString());
                JSONArray items = json.optJSONArray("article_items");
                JSONObject meta = json.optJSONObject("meta");

                count=meta==null?0:meta.optInt("count");
                if (items==null) return;
                for (int i=0;i<items.length();i++){

                    JSONObject item = items.optJSONObject(i);
                    int id=item.optInt("id");
                    String title =item.optString("title");
                    int price=item.optInt("price");
                    String url=item.optString("url");
                    int bean_like_count=item.optInt("bean_likes_count");
                    JSONArray array = item.optJSONArray("asset_imgs");
                    String image=null;
                    if (array.length()>0){
                        JSONArray array1 = array.optJSONArray(0);
                        JSONObject object =  array1.optJSONObject(1);
                        JSONObject picObj= object.optJSONObject("picture");
                        JSONObject thumbObj =picObj.optJSONObject("thumb");
                        image =thumbObj.optString("url");
                        Log.i("image",image);
                    }

                    Product product = new Product(id,title,price,url,bean_like_count,null,image);
                    products.add(product);
                }
                handler.sendEmptyMessage(0);
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i("onErrorResponse",error.toString());
            }
        });
        if (mRequestQueue==null) mRequestQueue=Volley.newRequestQueue(getActivity());
        mRequestQueue.add(newMissRequest);
    }
    public class SingleAdapter extends BaseListAdapter<Product> {
        public SingleAdapter(Context context, List<Product> list) {
            super(context, list);
            imageLoader = new AsyncImageLoader1(context);
        }

        @Override
        public View bindView(int position, View convertView, ViewGroup parent) {
            convertView=mHashMap.get(position);
            if (convertView==null)
                convertView=mInflater.inflate(R.layout.item_fragment_single,null);
            TextView tv_name = ViewHolder.get(convertView, R.id.tv_name);
            ImageView image =ViewHolder.get(convertView,R.id.iv_single_item);
            TextView tv_money=ViewHolder.get(convertView,R.id.tv_money);
            ImageView iv_heart=ViewHolder.get(convertView,R.id.iv_heart);
            TextView tv_count=ViewHolder.get(convertView,R.id.tv_count);

            Product product = getList().get(position);
            tv_name.setText(product.getTitle());
            tv_money.setText("￥"+product.getPrice()+"元");
            tv_count.setText(product.getBean_like_count()+"");
            imageLoader.LoadImage("http://"+product.getImage(),image);
            mHashMap.put(position,convertView);
            return convertView;
        }
    }
    public void FetchCategoryData(String id,int sort,int page){
        Log.i("FetchCategoryData--------->","FetchCategoryData");
//        if (page==1) products.clear();
//        Map<String, String> params = new HashMap<String, String>();
//        params.put("price", "desc");
//        params.put("hot_d", "desc");
//        params.put("search", msg);
//        params.put("page",page+"");
//        if (sort==1){
//            params.put("hot_d","desc");
//        }else if (sort==2){
//            params.put("price","asc");
//        }else if (sort==3){
//            params.put("price","desc");
//        }

        String url=ZhaiDou.ARTICLE_ITEM_WITH_CATEGORY+id;
        JsonObjectRequest fetchCategoryTask = new JsonObjectRequest(url,new Response.Listener<JSONObject>(){
            @Override
            public void onResponse(JSONObject jsonObject) {
                JSONArray items = jsonObject.optJSONArray("article_items");
                if (items==null) return;
                for (int i=0;i<items.length();i++){

                    JSONObject item = items.optJSONObject(i);
                    int id=item.optInt("id");
                    String title =item.optString("title");
                    int price=item.optInt("price");
                    String url=item.optString("url");
                    int bean_like_count=item.optInt("bean_likes_count");
                    JSONArray array = item.optJSONArray("asset_imgs");
                    JSONArray array1 = array.optJSONArray(0);
                    JSONObject object =  array1.optJSONObject(1);
                    JSONObject picObj= object.optJSONObject("picture");
                    JSONObject thumbObj =picObj.optJSONObject("thumb");
                    String image =thumbObj.optString("url");

                    Product product = new Product(id,title,price,url,bean_like_count,null,image);
                    products.add(product);
                }
                handler.sendEmptyMessage(0);
            }
        },new Response.ErrorListener(){
            @Override
            public void onErrorResponse(VolleyError volleyError) {
            }
        });
        mRequestQueue.add(fetchCategoryTask);
    }

    @Override
    public void onPullDownToRefresh(PullToRefreshBase<GridView> refreshView) {
        String label = DateUtils.formatDateTime(getActivity(), System.currentTimeMillis(),
                DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_ALL);
        refreshView.getLoadingLayoutProxy().setLastUpdatedLabel(label);
        Log.i("onPullDownToRefresh--->","onPullDownToRefresh");
        FetchData(mParam1,sort,currentpage=1);
        gv_single.setMode(PullToRefreshBase.Mode.BOTH);
    }

    @Override
    public void onPullUpToRefresh(PullToRefreshBase<GridView> refreshView) {
        Log.i("onPullUpToRefresh---->","onPullUpToRefresh");
        FetchData(mParam1,sort,++currentpage);
        if (count!=-1&&singleAdapter.getCount()==count){
            Toast.makeText(getActivity(),"已经加载完毕",Toast.LENGTH_SHORT).show();
            gv_single.onRefreshComplete();
            gv_single.setMode(PullToRefreshBase.Mode.PULL_FROM_START);
        }
    }
}
