package com.zhaidou.fragments;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
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
import com.zhaidou.model.Order;
import com.zhaidou.model.OrderItem;
import com.zhaidou.model.Receiver;
import com.zhaidou.utils.PhotoUtil;
import com.zhaidou.utils.SharedPreferencesUtil;
import com.zhaidou.utils.ToolUtils;

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
import java.io.File;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link AfterSaleFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AfterSaleFragment extends BaseFragment implements View.OnClickListener {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_STATUS = "status";

    // TODO: Rename and change types of parameters
    private String mOrderId;
    private String mStatus;

    private View rootView;
    private TextView mOldPrice;
    private RequestQueue requestQueue;
    private ListView mListView;
    private GridView mImgGrid;
    private ImageAdapter imageAdapter;
    private final int UPDATE_RETURN_LIST = 1;
    private AfterSaleAdapter afterSaleAdapter;
    private TextView tv_return, tv_exchange, lastSelected,tv_commit;
    List<OrderItem> orderItems = new ArrayList<OrderItem>();
    private PhotoMenuFragment menuFragment;
    private FrameLayout mMenuContainer;
    boolean isFromCamera = false;// 区分拍照旋转
    int degree = 0;
    private ImageView iv_return_img;
    private final int MENU_CAMERA_SELECTED = 0;
    private final int MENU_PHOTO_SELECTED = 1;
    private final int UPDATE_UPLOAD_IMG_GRID=2;

    public String filePath = "";
    private String token;
    private List<String> imagePath = new ArrayList<String>();
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_RETURN_LIST:
                    afterSaleAdapter.notifyDataSetChanged();
                    break;
                case UPDATE_UPLOAD_IMG_GRID:
                    if (imagePath.size()>=4)
                        imagePath.remove("");
                    imageAdapter.notifyDataSetChanged();
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
     * @return A new instance of fragment AfterSaleFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static AfterSaleFragment newInstance(String orderId, String status) {
        AfterSaleFragment fragment = new AfterSaleFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, orderId);
        args.putString(ARG_STATUS, status);
        fragment.setArguments(args);
        return fragment;
    }

    public AfterSaleFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mOrderId = getArguments().getString(ARG_PARAM1);
            mStatus = getArguments().getString(ARG_STATUS);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        Log.i("AfterSaleFragment--------------->","onCreateView");
        if (null != rootView) {
            ViewGroup parent = (ViewGroup) rootView.getParent();
            if (null != parent) {
                parent.removeView(rootView);
            }
        } else {
            rootView = inflater.inflate(R.layout.fragment_after_sale, container, false);
            initView(rootView);// 控件初始化
        }
//        View view = inflater.inflate(R.layout.fragment_after_sale, container, false);

        return rootView;
    }

    private void initView(View view) {
        token=(String)SharedPreferencesUtil.getData(getActivity(),"token","");
        tv_commit=(TextView)view.findViewById(R.id.tv_commit);
        tv_commit.setOnClickListener(this);
        tv_return = (TextView) view.findViewById(R.id.tv_return);
        tv_exchange = (TextView) view.findViewById(R.id.tv_exchange);
        mOldPrice = (TextView) view.findViewById(R.id.tv_outdated);
        mImgGrid = (GridView) view.findViewById(R.id.gv_img);
        mMenuContainer = (FrameLayout) view.findViewById(R.id.rl_header_menu);
        TextPaint textPaint = mOldPrice.getPaint();
        textPaint.setAntiAlias(true);
        textPaint.setFlags(Paint.STRIKE_THRU_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
        requestQueue = Volley.newRequestQueue(getActivity());
        mListView = (ListView) view.findViewById(R.id.lv_aftersale);
        afterSaleAdapter = new AfterSaleAdapter(getActivity(), orderItems);
        mListView.setAdapter(afterSaleAdapter);
        FetchOrderDetail(mOrderId);

        tv_exchange.setText("return_money".equalsIgnoreCase(mStatus) ? "退款" : "退货");
        tv_exchange.setOnClickListener(this);
        tv_return.setOnClickListener(this);

        iv_return_img = (ImageView) view.findViewById(R.id.iv_return_img);
        iv_return_img.setOnClickListener(this);
        menuFragment = PhotoMenuFragment.newInstance("", "");
        if (menuFragment != null)
            getChildFragmentManager().beginTransaction().replace(R.id.rl_header_menu, menuFragment).addToBackStack("").hide(menuFragment).commit();
        imagePath.add("");
        imageAdapter = new ImageAdapter(getActivity(), imagePath);
        mImgGrid.setAdapter(imageAdapter);

        menuFragment.setMenuSelectListener(new PhotoMenuFragment.MenuSelectListener() {
            @Override
            public void onMenuSelect(int position, String tag) {
                switch (position) {
                    case MENU_CAMERA_SELECTED:
                        Toast.makeText(getActivity(), position + "--->" + tag, Toast.LENGTH_LONG).show();
                        File dir = new File(ZhaiDou.MyAvatarDir);
                        if (!dir.exists()) {
                            dir.mkdirs();
                        }
                        // 原图
                        File file = new File(dir, new SimpleDateFormat("yyMMddHHmmss")
                                .format(new Date()));
                        filePath = file.getAbsolutePath();// 获取相片的保存路径
                        Uri imageUri = Uri.fromFile(file);

                        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                        startActivityForResult(intent,
                                MENU_CAMERA_SELECTED);
                        break;
                    case MENU_PHOTO_SELECTED:
                        Toast.makeText(getActivity(), position + "--->" + tag, Toast.LENGTH_LONG).show();
                        Intent intent1 = new Intent(Intent.ACTION_PICK, null);
                        intent1.setDataAndType(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                        startActivityForResult(intent1,
                                MENU_PHOTO_SELECTED);
                        break;
                }
                toggleMenu();
            }
        });


        imageAdapter.setOnInViewClickListener(R.id.iv_img, new BaseListAdapter.onInternalClickListener() {
            @Override
            public void OnClickListener(View parentV, View v, Integer position, Object values) {
                final String imgPath = (String) values;
                if (TextUtils.isEmpty(imgPath)) {
                    imagePath.remove("");
                    mMenuContainer.setVisibility(View.VISIBLE);
                    toggleMenu();
                } else {
                    PhotoViewFragment photoViewFragment=PhotoViewFragment.newInstance(position,imgPath);
                    ((MainActivity)getActivity()).navigationToFragment(photoViewFragment);
                    photoViewFragment.setPhotoListener(new PhotoViewFragment.PhotoListener() {
                        @Override
                        public void onPhotoDelete(int position, String url) {
                            if (!TextUtils.isEmpty(url)){
                                imagePath.remove(position);
                                if (imagePath.size()<3&&!imagePath.contains(""))
                                    imagePath.add("");
                                imageAdapter.notifyDataSetChanged();
                            }
                        }
                    });
                }
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_exchange:
                if (lastSelected != null)
                    lastSelected.setSelected(false);
                tv_exchange.setSelected(true);
                lastSelected = tv_exchange;
                break;
            case R.id.tv_return:
                if (lastSelected != null)
                    lastSelected.setSelected(false);
                tv_return.setSelected(true);
                lastSelected = tv_return;
                break;
            case R.id.iv_return_img:
                mMenuContainer.setVisibility(View.VISIBLE);
                toggleMenu();
                break;
            case R.id.tv_commit:
                new CommitTask().execute();
//                for (int i = 0; i < imagePath.size(); i++) {
//                    String path=imagePath.get(i);
//                    if (!TextUtils.isEmpty(path)){
//                        File file=new File(path);
//                        Log.i("file----->",file.length()+"");
//                        Bitmap bitmap=BitmapFactory.decodeFile(path);
//                        String base64Str =PhotoUtil.bitmapToBase64(bitmap);
//                    }
//                }
                break;
        }
    }

    private void FetchOrderDetail(String id) {
        JsonObjectRequest request = new JsonObjectRequest(ZhaiDou.URL_ORDER_LIST+"/" + id, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                Log.i("FetchOrderDetail-------------->", jsonObject.toString());
                if (jsonObject != null) {
                    JSONObject orderObj = jsonObject.optJSONObject("order");
                    int amount = orderObj.optInt("amount");
                    int id = orderObj.optInt("id");
                    String status = orderObj.optString("status");
                    String created_at_for = orderObj.optString("created_at_for");
                    String receiver_address = orderObj.optString("receiver_address");
                    String created_at = orderObj.optString("created_at");
                    String status_ch = orderObj.optString("status_ch");
                    String number = orderObj.optString("number");
                    String receiver_phone = orderObj.optString("receiver_phone");
                    String deliver_number = orderObj.optString("deliver_number");
                    String receiver_name = orderObj.optString("receiver_name");

                    JSONObject receiverObj = orderObj.optJSONObject("receiver");
                    int receiverId = receiverObj.optInt("id");
                    String address = receiverObj.optString("address");
                    String phone = receiverObj.optString("phone");
                    String name = receiverObj.optString("name");
                    Receiver receiver = new Receiver(receiverId, address, phone, name);


                    JSONArray order_items = orderObj.optJSONArray("order_items");
//                    List<OrderItem> orderItems=new ArrayList<OrderItem>();
                    if (order_items != null && order_items.length() > 0) {
                        for (int i = 0; i < order_items.length(); i++) {
                            JSONObject item = order_items.optJSONObject(i);
                            int itemId = item.optInt("id");
                            int itemPrice = item.optInt("price");
                            int count = item.optInt("count");
                            int cost_price = item.optInt("cost_price");
                            String merchandise = item.optString("merchandise");
                            String specification = item.optString("specification");
                            int merchandise_id = item.optInt("merchandise_id");
                            String merch_img = item.optString("merch_img");
                            OrderItem orderItem = new OrderItem(itemId, itemPrice, count, cost_price, merchandise, specification, merchandise_id, merch_img);
                            orderItems.add(orderItem);
                        }
                    }
                    Order order = new Order("", id, number, amount, status, status_ch, created_at_for, created_at, receiver, orderItems, receiver_address, receiver_phone, deliver_number, receiver_name);
                    Message message = new Message();
                    message.obj = order;
                    message.what = UPDATE_RETURN_LIST;
                    handler.sendMessage(message);
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
                headers.put("SECAuthorization",token);
                return headers;
            }
        };
        requestQueue.add(request);
    }

    public class AfterSaleAdapter extends BaseListAdapter<OrderItem> {
        public AfterSaleAdapter(Context context, List<OrderItem> list) {
            super(context, list);
        }

        @Override
        public View bindView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = mInflater.inflate(R.layout.item_order_return1, null);
            TextView tv_name = ViewHolder.get(convertView, R.id.tv_name);
            TextView tv_specification = ViewHolder.get(convertView, R.id.tv_specification);
            TextView tv_count = ViewHolder.get(convertView, R.id.tv_count);
            ImageView iv_order_img = ViewHolder.get(convertView, R.id.iv_order_img);
            TextView tv_old_price = ViewHolder.get(convertView, R.id.tv_old_price);
            TextView tv_price = ViewHolder.get(convertView, R.id.tv_price);

            OrderItem item = getList().get(position);
            tv_name.setText(item.getMerchandise());
            tv_specification.setText(item.getSpecification());
            tv_count.setText(item.getCount() + "");
            tv_price.setText("￥" + item.getPrice());
            tv_old_price.setText("￥" + item.getCost_price());
            TextPaint textPaint = tv_old_price.getPaint();
            textPaint.setAntiAlias(true);
            textPaint.setFlags(Paint.STRIKE_THRU_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
            ToolUtils.setImageCacheUrl(item.getMerch_img(), iv_order_img);
            return convertView;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case MENU_CAMERA_SELECTED:// 拍照修改头像
                if (resultCode == getActivity().RESULT_OK) {
                    if (!Environment.getExternalStorageState().equals(
                            Environment.MEDIA_MOUNTED)) {
                        Toast.makeText(getActivity(), "SD不可用", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Log.i("拍照修改头像------------>", "拍照修改头像");
                    isFromCamera = true;
                    File file = new File(filePath);
                    Log.i("MENU_CAMERA_SELECTED-------------->", filePath+"------->"+imagePath.size());
                    degree = PhotoUtil.readPictureDegree(file.getAbsolutePath());
//                    ToolUtils.setImageCacheUrl("file://" + filePath, iv_return_img);
                    if (imagePath!=null&&imagePath.size()<3){
                        imagePath.add(filePath);
                        imagePath.add("");
                        handler.sendEmptyMessage(UPDATE_UPLOAD_IMG_GRID);
                    }

                }
                break;
            case MENU_PHOTO_SELECTED:// 本地修改头像
                Log.i("requestCode", "本地修改头像");
                Uri uri = null;
                if (data == null) {
                    return;
                }
                if (resultCode == getActivity().RESULT_OK) {
                    if (!Environment.getExternalStorageState().equals(
                            Environment.MEDIA_MOUNTED)) {
                        Toast.makeText(getActivity(), "SD不可用", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    isFromCamera = false;
                    uri = data.getData();
                    Log.i("MENU_PHOTO_SELECTED------------>", uri.getPath());

                    try {
                        Bitmap bm = null;
                        ContentResolver resolver = getActivity().getContentResolver();
                        bm = MediaStore.Images.Media.getBitmap(resolver, uri);
                        String[] proj = {MediaStore.Images.Media.DATA};
                        Cursor cursor = getActivity().managedQuery(uri, proj, null, null, null);
                        //按我个人理解 这个是获得用户选择的图片的索引值
                        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                        //将光标移至开头 ，这个很重要，不小心很容易引起越界
                        cursor.moveToFirst();
                        //最后根据索引值获取图片路径
                        String path = cursor.getString(column_index);
                        Log.i("MENU_PHOTO_SELECTED------------>path", path+"---------------->"+imagePath.size());
                        ToolUtils.setImageCacheUrl("file://" + path, iv_return_img);
                        if (imagePath != null && imagePath.size() < 3){
                            imagePath.add(path);
                            imagePath.add("");
                            handler.sendEmptyMessage(UPDATE_UPLOAD_IMG_GRID);
                        }
                    } catch (Exception e) {

                    }
                } else {
                    Toast.makeText(getActivity(), "照片获取失败", Toast.LENGTH_SHORT).show();
                }

                break;
            case 2:// 裁剪头像返回
                // TODO sent to crop
                Log.i("裁剪头像返回----->", "裁剪头像返回");
                if (data == null) {
                    Toast.makeText(getActivity(), "取消选择", Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    saveCropAvator(data);
                }

                // 初始化文件路径
                filePath = "";
                break;
            default:
                break;
        }
    }

    /**
     * 保存裁剪的头像
     *
     * @param data
     */
    private void saveCropAvator(Intent data) {
        Log.i("saveCropAvator--------->", "saveCropAvator");
        Bundle extras = data.getExtras();
        if (extras != null) {
            Bitmap bitmap = extras.getParcelable("data");
            Log.i("life", "avatar - bitmap = " + bitmap);

            String base64str = PhotoUtil.bitmapToBase64(bitmap);
            Log.i("base64str0---------->", base64str);
//            new UpLoadAvatar().execute(base64str);
        }
    }

    public void toggleMenu() {
        if (menuFragment != null) {
            if (menuFragment.isHidden()) {
                getChildFragmentManager().beginTransaction().show(menuFragment).commit();
            } else {
                getChildFragmentManager().beginTransaction().hide(menuFragment).commit();
            }
        }
    }

    public class ImageAdapter extends BaseListAdapter<String> {
        private WeakHashMap<Integer,View> mHashMap = new WeakHashMap<Integer, View>();
        public ImageAdapter(Context context, List<String> list) {
            super(context, list);
        }

        @Override
        public View bindView(int position, View convertView, ViewGroup parent) {
            convertView=mHashMap.get(position);
            if (convertView == null)
                convertView = mInflater.inflate(R.layout.item_img_grid, null);
            ImageView iv_img = ViewHolder.get(convertView, R.id.iv_img);
            String img = getList().get(position);
            if (TextUtils.isEmpty(img)){
                iv_img.setBackgroundDrawable(getResources().getDrawable(R.drawable.icon_add));
            }else {
                ToolUtils.setImageCacheUrl("file://" + img, iv_img);
            }
            mHashMap.put(position,convertView);
            return convertView;
        }
    }

    private class CommitTask extends AsyncTask<Void,Void,String>{
        @Override
        protected String doInBackground(Void... voids) {
            String result=applyReturn();
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            Log.i("result------------->",s.toString());
        }
    }
    private String applyReturn() {

        String result=null;
        BufferedReader in = null;
        try {
            // 定义HttpClient
            HttpClient client = new DefaultHttpClient();


            // 实例化HTTP方法
            HttpPost request = new HttpPost(ZhaiDou.URL_ORDER_LIST+"/"+mOrderId+"/return_items");
            request.addHeader("SECAuthorization",token);

            // 创建名/值组列表
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("sale_return_item[return_category_id]", "" + mOrderId));
            params.add(new BasicNameValuePair("sale_return_item[node]", "海底世界噶"));
            for (int i = 0; i < imagePath.size(); i++) {
                Log.i("imagePath---------->",imagePath.get(i).toString());
                String path=imagePath.get(i);
                if (!TextUtils.isEmpty(path)){
                    Bitmap bitmap=BitmapFactory.decodeFile(path);
                    String base64Str =PhotoUtil.bitmapToBase64(bitmap);
                    Log.i("base64Str---------->",base64Str);
                    params.add(new BasicNameValuePair("sale_return_item[attachments_attributes][][picture]","data:image/png;base64,"+base64Str));
                }
            }

            String arr="[";
            for (int k=0;k<orderItems.size();k++){
                arr=arr+orderItems.get(k).getId()+",";
            }
            arr=arr.substring(0,arr.length()-2)+"]";
            params.add(new BasicNameValuePair("sale_return_item[order_item_ids][]",orderItems.get(0).getId()+""));

            // 创建UrlEncodedFormEntity对象
            UrlEncodedFormEntity formEntiry = new UrlEncodedFormEntity(
                    params, HTTP.UTF_8);
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
            result = sb.toString();
            Log.i("result------------>",result.toString());
            return result;

        } catch (Exception e) {

        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }
}
