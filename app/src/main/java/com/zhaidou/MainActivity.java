package com.zhaidou;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.sdk.android.AlibabaSDK;
import com.alibaba.sdk.android.callback.InitResultCallback;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpClientStack;
import com.android.volley.toolbox.Volley;
import com.umeng.analytics.MobclickAgent;
import com.zhaidou.activities.LoginActivity;
import com.zhaidou.base.AccountManage;
import com.zhaidou.base.BaseActivity;
import com.zhaidou.base.CartCountManager;
import com.zhaidou.base.CountManager;
import com.zhaidou.dialog.CustomVersionUpdateDialog;
import com.zhaidou.fragments.DiyFragment;
import com.zhaidou.fragments.MainCategoryFragment;
import com.zhaidou.fragments.MainGoodsFragment;
import com.zhaidou.fragments.MainHomeFragment;
import com.zhaidou.fragments.MainMagicFragment;
import com.zhaidou.fragments.MainPersonalFragment;
import com.zhaidou.fragments.OrderDetailFragment;
import com.zhaidou.fragments.RegisterFragment;
import com.zhaidou.fragments.ShopCartFragment;
import com.zhaidou.fragments.ShopPaymentFailFragment;
import com.zhaidou.fragments.ShopPaymentFragment;
import com.zhaidou.fragments.ShopPaymentSuccessFragment;
import com.zhaidou.model.Area;
import com.zhaidou.model.City;
import com.zhaidou.model.Province;
import com.zhaidou.model.User;
import com.zhaidou.model.ZhaiDouRequest;
import com.zhaidou.utils.Api;
import com.zhaidou.utils.DeviceUtils;
import com.zhaidou.utils.DialogUtils;
import com.zhaidou.utils.SharedPreferencesUtil;
import com.zhaidou.utils.ToolUtils;

import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 */
public class MainActivity extends BaseActivity implements DiyFragment.OnFragmentInteractionListener,
        MainHomeFragment.OnFragmentInteractionListener, MainCategoryFragment.OnFragmentInteractionListener, RegisterFragment.RegisterOrLoginListener
        , CountManager.onCountChangeListener, AccountManage.AccountListener, CountManager.onCommentChangeListener, CartCountManager.OnCartCountListener
{

    private FragmentManager manager;
    private Fragment utilityFragment;
    private Fragment magicHomeFragment;
    private Fragment goodsFragment;
    private ShopCartFragment shopCartFragment;

    private TextView homeButton;
    private TextView magicButton;
    private TextView categoryButton;
    private TextView personalButton;
    private TextView cartButton;
    private TextView lastButton;


    private TextView titleView;
    private LinearLayout mTabContainer;
    private ImageView iv_dot, mMsgView;
    public TextView cart_dot;
    private LinearLayout viewLayout;

    private String token;
    private int id;

    boolean isFromCamera = false;// 区分拍照旋转
    int degree = 0;
    public String filePath = "";
    private int type = 1; //1跳转我的账户2跳转购物车

    private long mTime;
    private Activity mContext;
    private String serverName;
    private String serverInfo;
    private String serverUrl;
    private int serverCode;
    private RequestQueue mRequestQueue;
    private final int WX_PAY_SUCCESS = 0;
    private final int WX_PAY_FAILED = -1;
    private final int WX_PAY_CANCEL = -2;
    private  static List<Province> provinceList=new ArrayList<Province>();

    public int num = 0;
    public int cartCount = 0;
    public static int screenWidth;
    public static int screenHeight;


    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if (action.equals(ZhaiDou.IntentRefreshLoginTag))
            {
                checkLogin();
                FetchCountData();
            }
            if (action.equalsIgnoreCase(ZhaiDou.BROADCAST_WXAPI_FILTER))
            {
                List<Fragment> fragments = getSupportFragmentManager().getFragments();
                int result = intent.getIntExtra("code", -2);
                if (fragments.size() > 1)
                {
                    Fragment fragment = fragments.get(fragments.size() - 1);
                    Fragment shopPaymentFragment = getSupportFragmentManager().findFragmentByTag(ShopPaymentFragment.class.getSimpleName());
                    Fragment shopPaymentFailFragment = getSupportFragmentManager().findFragmentByTag(ShopPaymentFailFragment.class.getSimpleName());

                    if (shopPaymentFragment != null)
                    {
                        ((ShopPaymentFragment) shopPaymentFragment).setPayment();
                    }
                    if (shopPaymentFailFragment != null && shopPaymentFailFragment instanceof ShopPaymentFailFragment)
                    {
                        ((ShopPaymentFailFragment) shopPaymentFailFragment).handleWXPayResult(result);
                    } else if (shopPaymentFragment != null && shopPaymentFragment instanceof ShopPaymentFragment)
                    {
                        ((ShopPaymentFragment) shopPaymentFragment).setPayment();
                        ((ShopPaymentFragment) shopPaymentFragment).handleWXPayResult(result);
                    } else
                    {
                    }
                }
            }
        }
    };

    private Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case 0:
                    if (type == 1)
                    {
                        if (persoanlFragment == null)
                        {
                            persoanlFragment = MainPersonalFragment.newInstance("", "");
                        } else
                        {
                            persoanlFragment.refreshData(MainActivity.this);
                        }
                        selectFragment(currentFragment, persoanlFragment);
                        setButton(personalButton);
                    } else
                    {
                        if (shopCartFragment == null)
                        {
                            shopCartFragment = ShopCartFragment.newInstance("", 1);
                        } else
                        {
                            shopCartFragment.refreshData();
                        }
                        selectFragment(currentFragment, shopCartFragment);
                        setButton(cartButton);
                    }
                    FetchUnPayData();
                    break;
                case 1000:
                    Integer unreadMsgCount = (Integer) msg.obj;
                    mMsgView.setVisibility(unreadMsgCount > 0 ? View.VISIBLE : View.GONE);
                    break;
            }
        }
    };

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main_layout);
        manager = getSupportFragmentManager();
        iv_dot = (ImageView) findViewById(R.id.iv_dot);
        mMsgView = (ImageView) findViewById(R.id.iv_msg);
        cart_dot = (TextView) findViewById(R.id.cartTipsTv);
        CartTip(0);
        viewLayout = (LinearLayout) findViewById(R.id.content);
        mContext = this;
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        screenWidth = dm.widthPixels;
        screenHeight = dm.heightPixels;
        init();
        initBroadcastReceiver();

        FetchAPK();
        initComponents();
        commitActiveData();
        AlibabaSDK.asyncInit(this, new InitResultCallback()
        {
            @Override
            public void onSuccess()
            {
            }

            @Override
            public void onFailure(int i, String s)
            {
            }
        });
        CountManager.getInstance().setOnCountChangeListener(this);
        CountManager.getInstance().setOnCommentChangeListener(this);
        AccountManage.getInstance().register(this);
        CartCountManager.newInstance().setOnCartCountListener(this);
        if (checkLogin())
        {
            FetchUnPayData();
            FetchCountData();
        }
    }


    /**
     * 注册广播
     */
    private void initBroadcastReceiver()
    {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ZhaiDou.IntentRefreshLoginExitTag);
        intentFilter.addAction(ZhaiDou.IntentRefreshLoginTag);
        intentFilter.addAction(ZhaiDou.BROADCAST_WXAPI_FILTER);
        mContext.registerReceiver(broadcastReceiver, intentFilter);
    }

    public void init()
    {
        mRequestQueue = Volley.newRequestQueue(this, new HttpClientStack(new DefaultHttpClient()));
        FetchCityData();
    }

    public void initComponents()
    {

        mTabContainer = (LinearLayout) findViewById(R.id.tab_container);
        mChildContainer = (FrameLayout) findViewById(R.id.fl_child_container);
        homeButton = (TextView) findViewById(R.id.tab_home);

        lastButton = homeButton;

        if (utilityFragment == null)
        {
            utilityFragment = MainHomeFragment.newInstance(ZhaiDou.HOME_PAGE_URL, ZhaiDou.ListType.HOME.toString());
        }

        currentFragment = utilityFragment;
        setButton(lastButton);
        setDefaultFragment(utilityFragment);

        homeButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                selectFragment(currentFragment, utilityFragment);
                setButton(view);
                MobclickAgent.onEvent(mContext,"main_home");
            }
        });

        magicButton = (TextView) findViewById(R.id.tab_beauty);
        magicButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (magicHomeFragment == null)
                {
                    magicHomeFragment = new MainMagicFragment();
                }
                selectFragment(currentFragment, magicHomeFragment);
                setButton(view);
                MobclickAgent.onEvent(mContext,"main_magic");

            }
        });

        categoryButton = (TextView) findViewById(R.id.tab_category);
        categoryButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                MobclickAgent.onEvent(mContext,"main_good");
                gotoCategory();

            }
        });

        cartButton = (TextView) findViewById(R.id.tab_diy);
        cartButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                type = 2;
                if (!checkLogin())
                {
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    intent.setFlags(2);
                    MainActivity.this.startActivityForResult(intent, 10000);
                } else
                {
                    if (shopCartFragment == null)
                    {
                        shopCartFragment = ShopCartFragment.newInstance("haha", 1);
                    }
                    selectFragment(currentFragment, shopCartFragment);
                    setButton(view);
                }
            }
        });

        personalButton = (TextView) findViewById(R.id.tab_personal);
        personalButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                type = 1;
                if (!checkLogin())//||!EMChatManager.getInstance().isConnected()
                {
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    intent.setFlags(2);
                    MainActivity.this.startActivityForResult(intent, 10000);
                } else
                {
                    if (persoanlFragment == null)
                    {
                        persoanlFragment = MainPersonalFragment.newInstance("", "");
                    }
                    selectFragment(currentFragment, persoanlFragment);
                    setButton(view);
                }

            }
        });

    }

    public void setButton(View view)
    {
        TextView button = (TextView) view;

        lastButton.setSelected(false);

        lastButton = button;

        lastButton.setSelected(true);

    }

    /**
     * 跳转分类
     */
    public void gotoCategory()
    {
        if (goodsFragment == null)
        {
            goodsFragment = MainGoodsFragment.newInstance("", "");
        }
        selectFragment(currentFragment, goodsFragment);
        setButton(categoryButton);

    }


    public void setDefaultFragment(Fragment defaultFragment)
    {
        getSupportFragmentManager().beginTransaction().add(R.id.content, defaultFragment).commit();
    }

    public void selectFragment(Fragment from, Fragment to)
    {
        if (currentFragment != to)
        {
            currentFragment = to;
            FragmentTransaction transaction = manager.beginTransaction();
            if (!to.isAdded())
            {
                transaction.hide(from).add(R.id.content, to).commit();
            } else
            {
                transaction.hide(from).show(to).commit();
            }
        }
    }

    public void toHomeFragment()
    {
        if (currentFragment instanceof MainHomeFragment)
        {
            FragmentTransaction transaction = manager.beginTransaction();
            transaction.show(currentFragment).commit();
        } else
        {
            if (utilityFragment != null)
                selectFragment(currentFragment, utilityFragment);
            setButton(homeButton);
        }
    }

    public void onFragmentInteraction(Uri uri)
    {

    }


    /**
     * 处理重复点击子 fragment时候出现触发底层 fragment 的问题
     *
     * @param ev
     * @return
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev)
    {
        int num = manager.getBackStackEntryCount();
        if (num < 1)
        {
            mChildContainer.setClickable(false);
        } else
        {
            mChildContainer.setClickable(true);
        }
        return super.dispatchTouchEvent(ev);
    }

    public boolean checkLogin()
    {
        token = (String) SharedPreferencesUtil.getData(this, "token", "");
        id = (Integer) SharedPreferencesUtil.getData(this, "userId", -1);
        boolean isLogin = !TextUtils.isEmpty(token) && id > -1;
        return isLogin;
    }



    public void logout(Fragment fragment)
    {
        popToStack(fragment);
        if (utilityFragment == null)
        {
            utilityFragment = MainHomeFragment.newInstance(ZhaiDou.HOME_PAGE_URL, ZhaiDou.ListType.HOME.toString());
        }
        selectFragment(currentFragment, utilityFragment);
        setButton(homeButton);
    }

    /**
     * 清除除开首页的全部fragment
     */
    public void allfragment()
    {
        List<Fragment> fragments = manager.getFragments();
        for (Fragment fragment : fragments)
        {
            if (fragment instanceof MainHomeFragment || fragment instanceof MainPersonalFragment || fragment instanceof MainMagicFragment || fragment instanceof MainCategoryFragment || fragment instanceof ShopCartFragment)
            {
            } else
            {
                popToStack(fragment);
            }
        }
    }

    public void hideTip(int v)
    {
        iv_dot.setVisibility(v);
    }

    /**
     * 显示购物车数量
     * @param ty
     */
    public void CartTip(int ty)
    {
        if (ty == 0)
        {
            cart_dot.setVisibility(View.GONE);
        } else
        {
            cart_dot.setText("" + ty);
            cart_dot.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 得到预加载的城市
     * @return
     */
    public static List<Province> getAddressCity()
    {
        return provinceList;
    }

    /**
     * 设置加载城市
     * @param provinces
     */
    public static void setAddressCity(List<Province> provinces)
    {
        provinceList=provinces;
    }


    private void FetchUnPayData()
    {
        String mUserId = SharedPreferencesUtil.getData(this, "userId", -1) + "";
        Map<String, String> params = new HashMap();
        params.put("userId", mUserId);
        params.put("clientType", "ANDROID");
        params.put("clientVersion", "45");
        params.put("businessType", "01");
        params.put("type", ZhaiDou.TYPE_ORDER_PREPAY);
        params.put("pageNo", "0");
        params.put("pageSize", "0");
        ZhaiDouRequest request = new ZhaiDouRequest(MainActivity.this, Request.Method.POST, ZhaiDou.URL_ORDER_LIST, params, new Response.Listener<JSONObject>()
        {
            @Override
            public void onResponse(JSONObject jsonObject)
            {
                int status = jsonObject.optInt("status");
                int totalCount = jsonObject.optInt("totalCount");
                if (status == 200)
                {
                    CountManager.getInstance().init(CountManager.TYPE.TAG_PREPAY, totalCount);
                }
            }
        }, new Response.ErrorListener()
        {
            @Override
            public void onErrorResponse(VolleyError volleyError)
            {
            }
        });
        ((ZDApplication) getApplicationContext()).mRequestQueue.add(request);
    }

    private void commitActiveData()
    {
        ApplicationInfo appInfo = null;
        try
        {
            appInfo = this.getPackageManager()
                    .getApplicationInfo(getPackageName(),
                            PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e)
        {
            e.printStackTrace();
        }
        final String channel = appInfo.metaData.getString("UMENG_CHANNEL");
        Map<String, String> map = new HashMap<String, String>();
        map.put("device_token[device_token]", DeviceUtils.getImei(this));
        ZhaiDouRequest request = new ZhaiDouRequest(MainActivity.this, Request.Method.POST, ZhaiDou.URL_STATISTICS, map, null, null)
        {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError
            {
                Map<String, String> header = new HashMap<String, String>();
                header.put("zd-client", channel);
                header.put("ZhaidouVesion", mContext.getResources().getString(R.string.app_versionName));
                return header;
            }
        };
        mRequestQueue.add(request);
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
                    ToolUtils.setLog(jsonObject.toString());
                    JSONObject object = ((JSONObject) jsonObject).optJSONObject("data");
                    if (object != null)
                        serverName = object.optString("app_version");
                    serverCode = object.optInt("code_version");
                    serverUrl = object.optString("package_url");
                    if (serverCode > ZDApplication.localVersionCode)
                    {
                        CustomVersionUpdateDialog customVersionUpdateDialog = new CustomVersionUpdateDialog(mContext, serverName, serverUrl);
                        customVersionUpdateDialog.checkUpdateInfo();
                    }
                }
            }
        }, null);
    }

    private void FetchCityData()
    {
        Api.getAddressCity(new Api.SuccessListener()
        {
            @Override
            public void onSuccess(Object object)
            {
                if (object != null)
                {
                    JSONObject jsonObject=(JSONObject)object;
                    JSONArray providerArr = jsonObject.optJSONArray("providers");
                    for (int i = 0; i < providerArr.length(); i++)
                    {
                        JSONObject provinceObj = providerArr.optJSONObject(i);
                        int provinceId = provinceObj.optInt("id");
                        String provinceName = provinceObj.optString("name");
                        Province province = new Province();
                        province.setId(provinceId);
                        province.setName(provinceName);
                        List<City> cityList = new ArrayList<City>();
                        JSONArray cityArr = provinceObj.optJSONArray("cities");
                        if (cityArr != null && cityArr.length() > 0)
                        {
                            for (int k = 0; k < cityArr.length(); k++)
                            {
                                JSONObject cityObj = cityArr.optJSONObject(k);
                                int cityId = cityObj.optInt("id");
                                String cityName = cityObj.optString("name");
                                JSONArray areaArr = cityObj.optJSONArray("children");
                                City city = new City();
                                city.setId(cityId);
                                city.setName(cityName);
                                List<Area> areaList = new ArrayList<Area>();
                                if (areaArr != null && areaArr.length() > 0)
                                {
                                    for (int j = 0; j < areaArr.length(); j++)
                                    {
                                        JSONObject areaObj = areaArr.optJSONObject(j);
                                        int areaId = areaObj.optInt("id");
                                        String areaName = areaObj.optString("name");
                                        int areaPrice = areaObj.optInt("price");
                                        Area area = new Area();
                                        area.setId(areaId);
                                        area.setName(areaName);
                                        area.setPrice(areaPrice);
                                        areaList.add(area);
                                    }
                                    city.setAreas(areaList);
                                    cityList.add(city);
                                }

                            }
                        }
                        province.setCityList(cityList);
                        provinceList.add(province);
                    }
                }
            }
        },null);
    }

    /**
     * 请求购物车列表数据
     */
    public void FetchCountData()
    {
        Api.getCartCount(id,new Api.SuccessListener()
        {
            @Override
            public void onSuccess(Object jsonObject)
            {
                if (jsonObject != null)
                {
                    JSONObject object =((JSONObject)jsonObject).optJSONObject("data");
                    cartCount = object.optInt("totalQuantity");
                    CartCountManager.newInstance().notify(cartCount);
                    CartTip(cartCount);
                }
            }
        },null);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        MobclickAgent.onResume(this);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        MobclickAgent.onPause(this);
    }

    int currentPrePayCount = 0;

    @Override
    public void onCount(int count)
    {
        iv_dot.setVisibility((count > 0 && count > currentPrePayCount) ? View.VISIBLE : View.GONE);
        currentPrePayCount = count;
    }

    @Override
    public void onLogOut()
    {
        iv_dot.setVisibility(View.GONE);
        mMsgView.setVisibility(View.GONE);
        SharedPreferencesUtil.removeUnReadComment();
        CountManager.getInstance().notifyCommentChange();
    }

    @Override
    public void onChange()
    {
        Integer unReadDesigner = (Integer) SharedPreferencesUtil.getData(ZDApplication.getInstance(), "UnReadDesigner", 0);
        mMsgView.setVisibility(unReadDesigner > 0 ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
        {
            Configuration o = newConfig;
            o.orientation = Configuration.ORIENTATION_PORTRAIT;
            newConfig.setTo(o);
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT)
        {
        }
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onChange(int count)
    {
        CartTip(count);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
//        CallbackContext.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
        switch (resultCode)
        {
            case 2000:
                int id = data.getIntExtra("id", -1);
                String email = data.getStringExtra("email");
                String token = data.getStringExtra("token");
                String nick = data.getStringExtra("nick");
                User user = new User(id, email, token, nick, null);
                Message message = new Message();
                message.obj = user;
                message.what = 0;
                mHandler.sendMessage(message);
                break;
            case 1000:
                break;
        }
    }

    @Override
    protected void onDestroy()
    {
        if (broadcastReceiver!=null)
            unregisterReceiver(broadcastReceiver);
        RequestQueue mRequestQueue=ZDApplication.newRequestQueue();
        if (mRequestQueue!=null)
            mRequestQueue.cancelAll(null);
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        int num = manager.getBackStackEntryCount();
        List<Fragment> fragments = manager.getFragments();
        if (num == 0)
        {
            if (keyCode == KeyEvent.KEYCODE_BACK)
            {
                if ((System.currentTimeMillis() - mTime) > 2000)
                {
                    Toast.makeText(this, "再按一次退出", Toast.LENGTH_SHORT).show();
                    mTime = System.currentTimeMillis();
                } else
                {
                    finish();
                }
                return true;
            }
        } else
        {
            if (fragments.size() > 0 && keyCode == KeyEvent.KEYCODE_BACK)
            {
                Fragment orderDetailFragment = manager.findFragmentByTag(OrderDetailFragment.class.getSimpleName());
                Fragment shopPaymentSuccessFragmen = manager.findFragmentByTag(ShopPaymentSuccessFragment.class.getSimpleName());
                Fragment shopPaymentFailFragment = manager.findFragmentByTag(ShopPaymentFailFragment.class.getSimpleName());
                Fragment shopPaymentFragment = manager.findFragmentByTag(ShopPaymentFragment.class.getSimpleName());
//                Fragment fragment = fragments.get(fragments.size() - 1);
//                if (fragment instanceof OrderDetailFragment || fragment instanceof ShopPaymentFailFragment ||
//                        fragment instanceof ShopPaymentSuccessFragment || fragment instanceof ShopPaymentFragment)
//                {
                    if ((orderDetailFragment != null && orderDetailFragment instanceof OrderDetailFragment))
                    {
                        popToStack(orderDetailFragment);
                        return true;
                    } else if ((shopPaymentSuccessFragmen != null && shopPaymentSuccessFragmen instanceof ShopPaymentSuccessFragment))
                    {
                        popToStack(shopPaymentSuccessFragmen);
                        return true;
                    } else if ((shopPaymentFailFragment != null && shopPaymentFailFragment instanceof ShopPaymentFailFragment))
                    {
                        popToStack(shopPaymentFailFragment);
                        return true;
                    } else if (shopPaymentFragment != null && shopPaymentFragment instanceof ShopPaymentFragment)
                    {
                        ToolUtils.setLog("关闭shopPaymentFragment");
                        //ShopPaymentFragment返回弹出提示
                        BackPaymentDialog(shopPaymentFragment);
                        return true;
                    }
                else
                    {
                        return super.onKeyDown(keyCode, event);
                    }
//                } else
//                {
//                    return super.onKeyDown(keyCode, event);
//                }
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 收银台返回弹窗处理
     *
     * @param shopPaymentFragment
     */
    private void BackPaymentDialog(final Fragment shopPaymentFragment)
    {
        DialogUtils mDialogUtils = new DialogUtils(this);
        mDialogUtils.showDialog("确认要放弃支付?", new DialogUtils.PositiveListener()
        {
            @Override
            public void onPositive()
            {
                popToStack(shopPaymentFragment);
            }
        }, null);
    }

}
