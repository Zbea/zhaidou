package com.zhaidou.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.pulltorefresh.PullToRefreshBase;
import com.pulltorefresh.PullToRefreshScrollView;
import com.zhaidou.MainActivity;
import com.zhaidou.R;
import com.zhaidou.ZhaiDou;
import com.zhaidou.activities.ItemDetailActivity;
import com.zhaidou.activities.SearchActivity;
import com.zhaidou.base.BaseActivity;
import com.zhaidou.base.BaseFragment;
import com.zhaidou.base.BaseListAdapter;
import com.zhaidou.base.ViewHolder;
import com.zhaidou.model.Article;
import com.zhaidou.model.Category;
import com.zhaidou.model.SwitchImage;
import com.zhaidou.utils.AsyncImageLoader1;
import com.zhaidou.utils.HtmlFetcher;
import com.zhaidou.utils.ImageDownloader;
import com.zhaidou.view.HeaderLayout;
import com.zhaidou.view.ImageSwitchWall;
import com.zhaidou.view.ListViewForScrollView;
import com.zhaidou.view.SwipeRefreshLayout;
import com.zhaidou.view.XListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;



/**
 * A simple {@link android.support.v4.app.Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link com.zhaidou.fragments.HomeFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link com.zhaidou.fragments.HomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class HomeFragment extends Fragment implements
                                         HeaderLayout.onLeftImageButtonClickListener,
                                         HeaderLayout.onRightImageButtonClickListener,
                                         ImageSwitchWall.OnItemClickListener,
                                         SwipeRefreshLayout.OnLoadListener,SwipeRefreshLayout.OnRefreshListener,
                                         HomeCategoryFragment.CategorySelectedListener,
                                         AdapterView.OnItemClickListener,View.OnClickListener,
                                         PullToRefreshBase.OnRefreshListener2<ScrollView>{
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String URL = "targetUrl";
    private static final String TYPE = "type";

    private ProgressDialog loading;
    private ListView listView;
    private ZhaiDou.ListType listType;

    /* pagination */
    private String targetUrl;
    private int currentPage=1;
    private int count=-1;

    private boolean loadedAll = false;
    private final int LOADED = 1;
    private AsyncImageLoader1 imageLoader;
    private WeakHashMap<Integer,View> mHashMap = new WeakHashMap<Integer, View>();
    /* Data Definition*/
    List<JSONObject> listItem;
    private static final int STATUS_REFRESH=0;
    private static final int STATUS_LOAD_MORE=1;
    private static final int UPDATE_CATEGORY=2;
    private static final int UPDATE_HOMELIST=3;
    private static final int UPDATE_BANNER=4;

    private ImageView mSearchView,mCategoryView;
    private TextView mTitleView;

    private PopupWindow mPopupWindow=null;
    private LinearLayout ll_poplayout;
    private FrameLayout fl_category_menu;
    private GridView gv_category;
    private CategoryAdapter mCategoryAdapter;
    private List<String> categoryList;
    private RequestQueue mRequestQueue;
    private List<Article> articleList = new ArrayList<Article>();
    private HomeAdapter mHomeAdapter;
    private ImageSwitchWall imageSwitchWall;
    private HomeCategoryFragment homeCategoryFragment;
    private Category mCategory;
    private LinearLayout mBackView;

    private LinearLayout mSwipeView;
//    SwipeRefreshLayout mSwipeLayout;

    private PullToRefreshScrollView mScrollView;
    private AdapterView.OnItemClickListener itemSelectListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            Log.i("onItemClick--->","onItemClick");
            try {
//                JSONObject item = listItem.get(i);
//                Intent detailIntent = new Intent(getActivity(), ItemDetailActivity.class);
//                detailIntent.putExtra("id", item.get("id").toString());
//                detailIntent.putExtra("title", item.get("title").toString());
//                detailIntent.putExtra("cover_url", item.get("thumbnail").toString());
//                detailIntent.putExtra("url", item.get("url").toString());
//                startActivity(detailIntent);
                HomeAdapter adapter = (HomeAdapter)adapterView.getAdapter();
                Article article =adapter.getItem(i);
                Intent detailIntent = new Intent(getActivity(), ItemDetailActivity.class);
                detailIntent.putExtra("id", article.getId());
                detailIntent.putExtra("title", article.getTitle());
                detailIntent.putExtra("cover_url",article.getImg_url());
//                detailIntent.putExtra("url", item.get("url").toString());
                startActivity(detailIntent);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    };
    /* Log cat */
    public static final String ERROR_CAT = "ERROR";
    public static final String DEBUG_CAT = "DEBUG";

    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {

            if (msg.what == LOADED) {

                if (loading.isShowing()) {
                    loading.dismiss();
                }
//                listView.setPullLoadEnable(true);
            }else if (msg.what==UPDATE_CATEGORY){
                mCategoryAdapter.setList(categoryList);
            }else if (msg.what==UPDATE_HOMELIST){
                Log.i("UPDATE_HOMELIST---------->",articleList.size()+"");
                mHomeAdapter.setList(articleList);
                mScrollView.onRefreshComplete();
//                mSwipeLayout.setLoading(false);
//                mSwipeLayout.setRefreshing(false);
//                loading.dismiss();
            }else if (msg.what==UPDATE_BANNER){
                List<SwitchImage> banners =(List<SwitchImage>) msg.obj;
                imageSwitchWall.setDatas(banners);
            }
            mHomeAdapter.notifyDataSetChanged();
        }
    };

    private OnFragmentInteractionListener mListener;

    // TODO: Rename and change types and number of parameters
    public static HomeFragment newInstance(String url, String type) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString(URL, url);
        args.putString(TYPE, type);
        fragment.setArguments(args);
        return fragment;
    }
    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            targetUrl = getArguments().getString(URL);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);
        listView = (ListViewForScrollView) view.findViewById(R.id.homeItemList);
        fl_category_menu=(FrameLayout)view.findViewById(R.id.fl_category_menu);
        mScrollView=(PullToRefreshScrollView)view.findViewById(R.id.scrollview);
        mSwipeView=(LinearLayout)view.findViewById(R.id.ll_adview);
        mBackView=(LinearLayout)view.findViewById(R.id.ll_back);
        mBackView.setOnClickListener(this);

        mScrollView.setMode(PullToRefreshBase.Mode.BOTH);
        mScrollView.setOnRefreshListener(this);

        view.findViewById(R.id.ll_lottery).setOnClickListener(this);
        view.findViewById(R.id.ll_competition).setOnClickListener(this);
        view.findViewById(R.id.ll_sale).setOnClickListener(this);
        view.findViewById(R.id.ll_forward).setOnClickListener(this);

//        mSwipeLayout=(SwipeRefreshLayout)view.findViewById(R.id.refresh_layout);


//        mSwipeLayout.setOnRefreshListener(this);
//        mSwipeLayout.setOnLoadListener(this);
//        mSwipeLayout.setColor(R.color.holo_blue_bright,
//                R.color.holo_green_light,
//                R.color.holo_orange_light,
//                R.color.holo_red_light);
//        mSwipeLayout.setMode(SwipeRefreshLayout.Mode.BOTH);

        mSearchView=(ImageView)view.findViewById(R.id.iv_search);
        mSearchView.setOnClickListener(this);
        mCategoryView=(ImageView)view.findViewById(R.id.iv_category);
        mCategoryView.setOnClickListener(this);
        mTitleView=(TextView)view.findViewById(R.id.tv_title);

        imageSwitchWall=(ImageSwitchWall)view.findViewById(R.id.isw_imagewall);

        imageSwitchWall.setOnItemClickListener(this);
        currentPage = 1;

        loadedAll = false;

        mRequestQueue = Volley.newRequestQueue(getActivity());
        listItem = new ArrayList<JSONObject>();
        mHomeAdapter = new HomeAdapter(getActivity(),articleList);
        listView.setAdapter(mHomeAdapter);

//        imageSwitchWall.setDatas(SwitchImage.getTestData());

//        loading = ProgressDialog.show(getActivity(), "", "正在努力加载中...", true);
        getBannerData();
        FetchData(currentPage,null);
        setUpPopView();

        if (homeCategoryFragment==null)
            homeCategoryFragment =HomeCategoryFragment.newInstance("","");
        getChildFragmentManager().beginTransaction().add(R.id.fl_category_menu,homeCategoryFragment
                        ,HomeCategoryFragment.TAG).hide(homeCategoryFragment).commit();
        homeCategoryFragment.setCategorySelectedListener(this);
        listView.setOnItemClickListener(this);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {

//        initTopBarForBoth("每日精选功能美物",R.drawable.icon_category,R.drawable.icon_search,this,this);
        super.onActivityCreated(savedInstanceState);

    }

    private void setUpPopView(){
        mPopupWindow = new PopupWindow(getActivity());
        View view = getActivity().getLayoutInflater().inflate(R.layout.item_popupwindows, null);

        ll_poplayout = (LinearLayout) view.findViewById(R.id.ll_popup);

        gv_category = (GridView)view.findViewById(R.id.gv_category);
        categoryList=new ArrayList<String>();
        mCategoryAdapter=new CategoryAdapter(getActivity(),categoryList);
        gv_category.setAdapter(mCategoryAdapter);
        mPopupWindow.setWidth(LinearLayout.LayoutParams.MATCH_PARENT);
        mPopupWindow.setHeight(LinearLayout.LayoutParams.WRAP_CONTENT);
        mPopupWindow.setBackgroundDrawable(new BitmapDrawable());

        mPopupWindow.setOutsideTouchable(true);
        mPopupWindow.setContentView(view);
        gv_category.setOnItemClickListener(new GridView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

                mPopupWindow.dismiss();
            }
        });

        mCategoryAdapter.setOnInViewClickListener(R.id.tv_category_item,new BaseListAdapter.onInternalClickListener() {
            @Override
            public void OnClickListener(View parentV, View v, Integer position, Object values) {
            }
        });
    }
    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.iv_search:
                startActivity(new Intent(getActivity(),SearchActivity.class));
                break;
            case R.id.iv_category:
                toggleMenu();
//                mPopupWindow.showAtLocation(getView(), Gravity.TOP, 0, 220);
//                mPopupWindow.setFocusable(true);
//                gv_category.setFocusable(true);
//                runOnWorkThread(new Runnable() {
//                    @Override
//                    public void run() {
//
//                        try {
//                            java.net.URL url = new URL(ZhaiDou.INDEX_CATEGORY_FILTER);
//                            String json = HtmlFetcher.fetch(url);
//                            Log.d(DEBUG_CAT, "-------> INDEX_CATEGORY_FILTER: " + json);
//                            JSONObject root = new JSONObject(json);
//                            JSONArray categories = root.optJSONArray("article_categories");
//                            categoryList.clear();
//                            for (int i = 0; i < categories.length(); i++) {
//                                JSONObject item = categories.optJSONObject(i);
//                                ShowLog(item.optString("name"));
//                                categoryList.add(item.optString("name"));
//                            }
//                            handler.sendEmptyMessage(UPDATE_CATEGORY);
//                        } catch (Exception ex) {
//                            Log.e("Debug Info", ex.getMessage());
//                        }
//                    }
//                });
                break;
            case R.id.ll_lottery:
//                WebViewFragment prizeFragment=WebViewFragment.newInstance(ZhaiDou.PRIZE_SCRAPING_URL,true);
//                ((BaseActivity)getActivity()).navigationToFragment(prizeFragment);
                Intent detailIntent = new Intent(getActivity(), ItemDetailActivity.class);
                detailIntent.putExtra("url",ZhaiDou.PRIZE_SCRAPING_URL);
                startActivity(detailIntent);
                break;
            case R.id.ll_competition:
                WebViewFragment webViewFragment=WebViewFragment.newInstance(ZhaiDou.COMPETITION_URL,true);
                ((BaseActivity)getActivity()).navigationToFragment(webViewFragment);
                break;
            case R.id.ll_sale:
                SpecialSaleFragment specialSaleFragment =SpecialSaleFragment.newInstance("","");
                ((MainActivity)getActivity()).navigationToFragment(specialSaleFragment);
                break;
            case R.id.ll_forward:
                WebViewFragment forwardFragment=WebViewFragment.newInstance(ZhaiDou.FORWARD_URL,true);
                ((BaseActivity)getActivity()).navigationToFragment(forwardFragment);
                break;
            case R.id.ll_back:
                mCategoryView.setVisibility(View.VISIBLE);
                mBackView.setVisibility(View.GONE);
                mSwipeView.setVisibility(View.VISIBLE);
                break;
        }
    }

    public class CategoryAdapter extends BaseListAdapter<String> {
        public CategoryAdapter(Context context, List<String> list) {
            super(context, list);
        }



        @Override
        public View bindView(int position, View convertView, ViewGroup parent) {

            if (convertView==null)
                convertView=mInflater.inflate(R.layout.category_item_gv,null);
            TextView tv_item = ViewHolder.get(convertView, R.id.tv_category_item);
            String item = getList().get(position);
            tv_item.setText(item);

            return convertView;
        }
    }

    private void FetchData(int page,Category category){
        Log.i("FetchData------------------>","FetchData--->"+page);
        currentPage=page;

        if (page==1) {
            articleList.clear();
            loadedAll = false;
        }

        if (loadedAll){
            Toast.makeText(getActivity(),"已经加载完毕了哦！！！",Toast.LENGTH_SHORT).show();
//            mSwipeLayout.setLoading(false);
            mScrollView.onRefreshComplete();
            return;
        }

        String categoryId=(category==null?"":category.getId()+"");
        String url;
//        if (category==null){
//            url=ZhaiDou.HOME_BASE_URL+"article/api/articles?page="+page+"&catetory_id";
//        }else {
//            url="http://192.168.199.171/article/api/articles?page="+page+"&catetory_id="+categoryId;
//        }
        url=ZhaiDou.HOME_CATEGORY_URL+page+((category==null)?"&catetory_id":"&catetory_id="+categoryId);
        Log.i("categoryId------------>",categoryId);

        Log.i("url---->",url);

        JsonObjectRequest jr = new JsonObjectRequest(url,new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.i("FetchData--->data---->",response.toString());
                JSONArray articles = response.optJSONArray("articles");
                JSONObject meta = response.optJSONObject("meta");
                count=meta==null?0:meta.optInt("count");
                if (articles==null||articles.length()<=0){
//                    articleList.clear();
//                    handler.sendEmptyMessage(UPDATE_HOMELIST);
                    return;
                }
                for (int i=0;i<articles.length();i++){
                    JSONObject article =articles.optJSONObject(i);
                    int id =article.optInt("id");
                    String title=article.optString("title");
                    String img_url=article.optString("img_url");
                    String is_new=article.optString("is_new");
                    int reviews = article.optInt("reviews");
                    Article item =new Article(id,title,img_url,is_new,reviews);
                    articleList.add(item);
                }

//                JSONObject meta = response.optJSONObject("meta");
//                int count = meta.optInt("count");
//                int page = meta.optInt("page");
//                int size = meta.optInt("size");
//                loadedAll = count<size;

                Message message = new Message();
                message.what=UPDATE_HOMELIST;

                handler.sendMessage(message);
            }
        },new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
//                Log.i("onErrorResponse------->",error.getMessage());
            }
        });
        mRequestQueue.add(jr);
    }

    public class HomeAdapter extends BaseListAdapter<Article> {
        public HomeAdapter(Context context, List<Article> list) {
            super(context, list);
            imageLoader=new AsyncImageLoader1(context);
        }

        @Override
        public View bindView(int position, View convertView, ViewGroup parent) {
            convertView = mHashMap.get(position);

            if (convertView==null)
                convertView=mInflater.inflate(R.layout.home_item_list,null);

            TextView title = ViewHolder.get(convertView, R.id.title);
            TextView articleViews = ViewHolder.get(convertView,R.id.views);
            ImageView cover = ViewHolder.get(convertView,R.id.cover);

            Article article = getList().get(position);

            title.setText(article.getTitle());
            articleViews.setText(article.getReviews()+"");
            imageLoader.LoadImage(article.getImg_url(),cover);

            mHashMap.put(position,convertView);
            return convertView;
        }
    }

    @Override
    public void onItemClick(ViewGroup vg, View v, int position) {
        Log.i("position--->",position+"");
        mCategoryView.setVisibility(View.GONE);
        mBackView.setVisibility(View.VISIBLE);
        mSwipeView.setVisibility(View.GONE);
        List<SwitchImage> switchImages = imageSwitchWall.getData();
        SwitchImage switchImage = switchImages.get(position);
        Log.i("switchImage--------------->",switchImage.toString());
        Category category = new Category();
        category.setId(switchImage.getId());
        FetchData(currentPage=0,category);
    }

    public void toggleMenu(){
        if (homeCategoryFragment.isHidden()){
            fl_category_menu.setVisibility(View.VISIBLE);
            getChildFragmentManager().beginTransaction().show(homeCategoryFragment).commit();
        }else {
            fl_category_menu.setVisibility(View.GONE);
            getChildFragmentManager().beginTransaction().hide(homeCategoryFragment).commit();
        }
        homeCategoryFragment.notifyDataSetChanged();
    }

    private String getTime() {
        return new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(new Date());
    }

    @Override
    public void onRefresh() {
        Log.i("OnRefreshListener---->","OnRefreshListener");
        FetchData(currentPage=1,mCategory);
    }

    @Override
    public void onLoad() {
        Log.i("onLoad------>","onLoad");
        FetchData(++currentPage,mCategory);
    }

    @Override
    public void onCategorySelected(Category category) {
        Log.i("HomeFragment-------------->",category==null?"全部":category.getName());
        FetchData(currentPage = 1, mCategory = category);
        toggleMenu();
    }

    private void getBannerData(){
        String url=ZhaiDou.HOME_BANNER_URL;
        Log.i("HOME_BANNER_URL------------------->",ZhaiDou.HOME_BANNER_URL);
        final List<SwitchImage> banners = new ArrayList<SwitchImage>();
        JsonObjectRequest bannerRequest = new JsonObjectRequest(url,new Response.Listener<JSONObject>(){
            @Override
            public void onResponse(JSONObject jsonObject) {
//                Log.i("jsonObject------>",jsonObject.toString());
                JSONArray article_categories=jsonObject.optJSONArray("article_categories");
                if (article_categories!=null&&article_categories.length()>0){
                    for(int i=0;i<article_categories.length();i++){
                        JSONObject categoryobj =article_categories.optJSONObject(i);
                        JSONArray childrenObj =  categoryobj.optJSONArray("children");
//                    SwitchImage switchImage = new SwitchImage();
                        for (int k=0;k<childrenObj.length();k++){
                            JSONObject banner = childrenObj.optJSONObject(i);
                            int id = banner.optInt("id");
                            String name = banner.optString("name");
                            String url = banner.optJSONObject("avatar").optString("url");

                            SwitchImage switchImage = new SwitchImage("http://"+url,id,name);
                            banners.add(switchImage);
                        }
                    }
                    Message message = new Message();
                    message.what=UPDATE_BANNER;
                    message.obj = banners;
                    handler.sendMessage(message);
                }

            }
        },new Response.ErrorListener(){
            @Override
            public void onErrorResponse(VolleyError volleyError) {

            }
        });
        mRequestQueue.add(bannerRequest);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

        Article article = articleList.get(position);
        Log.i("id----->",article.getId()+"");
        Intent detailIntent = new Intent(getActivity(), ItemDetailActivity.class);
        detailIntent.putExtra("id", article.getId()+"");
        detailIntent.putExtra("title", article.getTitle());
        detailIntent.putExtra("cover_url", article.getImg_url());
        detailIntent.putExtra("url",ZhaiDou.ARTICLE_DETAIL_URL+article.getId());
        startActivity(detailIntent);
    }

    @Override
    public void onPullDownToRefresh(PullToRefreshBase<ScrollView> refreshView) {
        String label = DateUtils.formatDateTime(getActivity(), System.currentTimeMillis(),
                DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_ALL);
        refreshView.getLoadingLayoutProxy().setLastUpdatedLabel(label);
        Log.i("onPullDownToRefresh--->","onPullDownToRefresh");
        getBannerData();
        FetchData(currentPage = 1, mCategory);
        mScrollView.setMode(PullToRefreshBase.Mode.BOTH);
    }

    @Override
    public void onPullUpToRefresh(PullToRefreshBase<ScrollView> refreshView) {
        Log.i("onPullUpToRefresh---->","onPullUpToRefresh");
        if (count!=-1&&mHomeAdapter.getCount()==count){
            Toast.makeText(getActivity(),"已经加载完毕",Toast.LENGTH_SHORT).show();
            mScrollView.onRefreshComplete();
            mScrollView.setMode(PullToRefreshBase.Mode.PULL_FROM_START);
            return;
        }
        FetchData(++currentPage, mCategory);
    }
}
