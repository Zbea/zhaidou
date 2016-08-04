package com.zhaidou.fragments;


import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.pulltorefresh.PullToRefreshBase;
import com.pulltorefresh.PullToRefreshListView;
import com.umeng.analytics.MobclickAgent;
import com.zhaidou.R;
import com.zhaidou.ZDApplication;
import com.zhaidou.ZhaiDou;
import com.zhaidou.activities.LoginActivity;
import com.zhaidou.adapter.CommentAdapter;
import com.zhaidou.base.BaseFragment;
import com.zhaidou.base.BaseListAdapter;
import com.zhaidou.dialog.CustomLoadingDialog;
import com.zhaidou.model.Comment;
import com.zhaidou.model.ZhaiDouRequest;
import com.zhaidou.utils.Api;
import com.zhaidou.utils.DialogUtils;
import com.zhaidou.utils.NetworkUtils;
import com.zhaidou.utils.SharedPreferencesUtil;
import com.zhaidou.utils.ToolUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

/**
 * Created by roy on 15/8/28.
 */
public class CommentListFragment extends BaseFragment
{
    private static final String DATA = "page";
    private static final String INDEX = "index";

    private String mPage;
    private String mIndex;

    private Dialog mDialog;
    private View mView;
    private TextView commentNumTv,nullCommentTv;
    private PullToRefreshListView listView;
    private FrameLayout frameLayout;
    private  LinearLayout commentLine;

    private int page = 1;
    private int pageSize;
    private int pageCount;
    private WeakHashMap<Integer, View> mHashMap = new WeakHashMap<Integer, View>();
    private List<Comment> comments=new ArrayList<Comment>();
    private CommentAdapter commentAdapter;
    private DialogUtils mDialogUtils;

    private Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case 1:
                    if (mDialog != null)
                    {
                        mDialog.dismiss();
                    }
                    commentNumTv.setText("("+pageCount+")");
                    commentNumTv.setVisibility(pageCount>0?View.VISIBLE:View.GONE);
                    nullCommentTv.setVisibility(pageCount>0?View.GONE:View.VISIBLE);
                    commentAdapter.notifyDataSetChanged();
                    listView.onRefreshComplete();
                    if (comments.size()< pageCount)
                    {
                        listView.setMode(PullToRefreshBase.Mode.BOTH);
                    } else
                    {
                        listView.setMode(PullToRefreshBase.Mode.PULL_FROM_START);
                    }
                    if (mDialog != null)
                        mDialog.dismiss();
                    break;
            }
        }
    };


    private PullToRefreshBase.OnRefreshListener2 onRefreshListener=new PullToRefreshBase.OnRefreshListener2()
    {
        @Override
        public void onPullDownToRefresh(PullToRefreshBase refreshView)
        {
            page=1;
            comments.clear();
            FetchData();
        }
        @Override
        public void onPullUpToRefresh(PullToRefreshBase refreshView)
        {
            page++;
            FetchData();
        }
    };



    private View.OnClickListener onClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            switch (v.getId())
            {
                case R.id.commentEditLine:
                    if (checkLogin())
                    {
                        frameLayout.setVisibility(View.VISIBLE);
                        CommentSendFragment commentSendFragment = CommentSendFragment.newInstance(mPage, mIndex,null);
                        commentSendFragment.setOnCommentListener(new CommentSendFragment.OnCommentListener()
                        {
                            @Override
                            public void onCommentResult(Comment comment)
                            {
                                if (comment!=null)
                                {
                                    pageCount++;
                                    commentNumTv.setText("("+pageCount+")");
                                    comments.add(0,comment);
                                    commentAdapter.notifyDataSetChanged();
                                }
                            }
                        });
                        getFragmentManager().beginTransaction().add(R.id.frameLayout, commentSendFragment).addToBackStack(null).commitAllowingStateLoss();
                    }
                    else
                    {
                        Intent intent = new Intent(getActivity(), LoginActivity.class);
                        intent.setFlags(1);
                        getActivity().startActivity(intent);
                    }
                    break;

            }
        }
    };

    public static CommentListFragment newInstance(String page, String index)
    {
        CommentListFragment fragment = new CommentListFragment();
        Bundle args = new Bundle();
        args.putSerializable(DATA, page);
        args.putSerializable(INDEX, index);
        fragment.setArguments(args);
        return fragment;
    }

    public CommentListFragment()
    {
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (getArguments() != null)
        {
            mPage = getArguments().getString(DATA);
            mIndex = getArguments().getString(INDEX);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {

        if (mView == null)
        {
            mView = inflater.inflate(R.layout.fragment_comment_list, container, false);
            mContext = getActivity();
            initView();
        }
        //缓存的rootView需要判断是否已经被加过parent， 如果有parent需要从parent删除，要不然会发生这个rootview已经有parent的错误。
        ViewGroup parent = (ViewGroup) mView.getParent();
        if (parent != null)
        {
            parent.removeView(mView);
        }

        return mView;
    }

    /**
     * 初始化
     */
    private void initView()
    {
        mDialogUtils = new DialogUtils(mContext);
        commentNumTv=(TextView)mView.findViewById(R.id.commentNumTv);
        nullCommentTv=(TextView)mView.findViewById(R.id.commentNullTv);

        listView=(PullToRefreshListView)mView.findViewById(R.id.lv_special_list);
        listView.setOnRefreshListener(onRefreshListener);
        commentAdapter=new CommentAdapter(mContext,comments);
        listView.setAdapter(commentAdapter);
        commentAdapter.setOnInViewClickListener(R.id.ll_click,new BaseListAdapter.onInternalClickListener()
        {
            @Override
            public void OnClickListener(View parentV, View v, Integer position, Object values)
            {
                sendComment(position);
            }
        });
        commentLine=(LinearLayout)mView.findViewById(R.id.commentEditLine);
        commentLine.setOnClickListener(onClickListener);

        frameLayout=(FrameLayout)mView.findViewById(R.id.frameLayout);

        if (NetworkUtils.isNetworkAvailable(mContext))
        {
            mDialog = CustomLoadingDialog.setLoadingDialog(mContext, "loading");
            FetchData();
        } else
        {
            Toast.makeText(mContext, "抱歉,网络链接失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendComment(final int position)
    {
        final Comment comment=comments.get(position);
        if (checkLogin())
        {
            int userId = (Integer) SharedPreferencesUtil.getData(mContext, "userId", -1);
            if (comment.userId==userId)
            {
                mDialogUtils.showListDialog(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                        mDialog=CustomLoadingDialog.setLoadingDialog(mContext,"");
                        Api.deleteComment(comment.id, new Api.SuccessListener()
                        {
                            @Override
                            public void onSuccess(Object object)
                            {
                                if (object != null)
                                {
                                    mDialog.dismiss();
                                    ToolUtils.setLog(object.toString());
                                    commentAdapter.remove(position);
                                    pageCount--;
                                    commentNumTv.setText("(" + pageCount + ")");
                                    commentNumTv.setVisibility(pageCount > 0 ? View.VISIBLE : View.GONE);
                                    ShowToast("删除成功");
                                }
                            }
                        }, null);
                    }
                });
            }
            else
            {
                frameLayout.setVisibility(View.VISIBLE);
                CommentSendFragment commentSendFragment = CommentSendFragment.newInstance(mPage, mIndex, comment);
                commentSendFragment.setOnCommentListener(new CommentSendFragment.OnCommentListener()
                {
                    @Override
                    public void onCommentResult(Comment comment)
                    {
                        if (comment != null)
                        {
                            pageCount++;
                            commentNumTv.setText("(" + pageCount + ")");
                            comments.add(0, comment);
                            commentAdapter.notifyDataSetChanged();
                        }
                    }
                });
                getFragmentManager().beginTransaction().add(R.id.frameLayout, commentSendFragment).addToBackStack(null).commitAllowingStateLoss();
            }
        } else
        {
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(1);
            getActivity().startActivity(intent);
        }
    }


    public void FetchData()
    {
        String url = ZhaiDou.HomeArticleCommentUrl + mIndex + "&pageNo=" + page + "&pageSize=15";
        ToolUtils.setLog(url);
        ZhaiDouRequest request = new ZhaiDouRequest(Request.Method.POST,url,
                new Response.Listener<JSONObject>()
                {
                    @Override
                    public void onResponse(JSONObject response)
                    {
                        listView.onRefreshComplete();
                        if (response == null)
                        {
                            if (mDialog != null)
                                mDialog.dismiss();
                            ToolUtils.setToast(mContext, R.string.loading_fail_txt);
                            return;
                        }
                        ToolUtils.setLog(response.toString());
                        JSONObject obj;
                        int status = response.optInt("status");
                        JSONObject jsonObject1 = response.optJSONObject("data");
                        if (jsonObject1 != null)
                        {
                            pageCount = jsonObject1.optInt("totalCount");
                            pageSize = jsonObject1.optInt("pageSize");
                            JSONArray jsonArray=jsonObject1.optJSONArray("items");
                            if (jsonArray!=null&jsonArray.length()>0)
                                for (int i = 0; i <jsonArray.length() ; i++)
                                {
                                    obj=jsonArray.optJSONObject(i);
                                    JSONObject jsonComment=obj.optJSONObject("comment");
                                    Comment comment=new Comment();
                                    if (jsonComment!=null)
                                    {
                                        int commentId=jsonComment.optInt("id");
                                        String commentTitle=jsonComment.optString("content");
                                        String commentUrl=jsonComment.optString("imgMd5");
                                        List<String> commentImgs=new ArrayList<String>();
                                        if (commentUrl.length()>0)
                                        {
                                            String[] commentUrls=commentUrl.split(",");
                                            for (int j = 0; j <commentUrls.length; j++)
                                            {
                                                commentImgs.add(commentUrls[j]);
                                            }
                                        }
                                        int commentUserId=jsonComment.optInt("commentUserId");
                                        String commentUserName=jsonComment.optString("commentUserName");
                                        String commentUserImg=jsonComment.optString("commentUserImg");
                                        String articleId=jsonComment.optString("articleId");
                                        String articleTitle=jsonComment.optString("articleTitle");
                                        String commentType=jsonComment.optString("commentType");
                                        String commentStatus=jsonComment.optString("status");
                                        String commentCreateTime= "";
                                        try
                                        {
                                            commentCreateTime = ToolUtils.getDateDiff(jsonComment.optString("createTime"));
                                        } catch (ParseException e)
                                        {
                                            e.printStackTrace();
                                        }

                                        comment.articleId=articleId;
                                        comment.articleTitle=articleTitle;
                                        comment.id=commentId;
                                        comment.time=commentCreateTime;
                                        comment.comment=commentTitle;
                                        comment.images=commentImgs;
                                        comment.type=commentType;
                                        comment.status=commentStatus;
                                        comment.userName=commentUserName;
                                        comment.userImage=commentUserImg;
                                        comment.userId=commentUserId;
                                    }

                                    JSONObject jsonReComment=obj.optJSONObject("reComment");
                                    if(jsonReComment!=null)
                                    {
                                        int reCommentId=jsonReComment.optInt("id");
                                        String reCommentTitle=jsonReComment.optString("content");
                                        String reCommentUrl=jsonReComment.optString("imgMd5");
                                        List<String> reCommentImgs=new ArrayList<String>();
                                        if (reCommentUrl.length()>0)
                                        {
                                            String[] reCommentUrls=reCommentUrl.split(",");
                                            for (int j = 0; j <reCommentUrls.length; j++)
                                            {
                                                reCommentImgs.add(reCommentUrls[j]);
                                            }
                                        }
                                        int reCommentUserId=jsonReComment.optInt("commentUserId");
                                        String reCommentUserName=jsonReComment.optString("commentUserName");
                                        String reCommentUserImg=jsonReComment.optString("commentUserImg");
                                        String reCommentArticleId=jsonReComment.optString("articleId");
                                        String reCommentArticleTitle=jsonReComment.optString("articleTitle");
                                        String reCommentType=jsonReComment.optString("commentType");
                                        String reCommentStatus=jsonReComment.optString("status");
                                        String reCommentCreateTime= "";
                                        try
                                        {
                                            reCommentCreateTime = ToolUtils.getDateDiff(jsonComment.optString("createTime"));
                                        } catch (ParseException e)
                                        {
                                            e.printStackTrace();
                                        }
                                        comment.idFormer =reCommentId;
                                        comment.timeFormer =reCommentCreateTime;
                                        comment.commentFormer =reCommentTitle;
                                        comment.imagesFormer =reCommentImgs;
                                        comment.typeFormer =reCommentType;
                                        comment.statusFormer =reCommentStatus;
                                        comment.userNameFormer =reCommentUserName;
                                        comment.userImageFormer =reCommentUserImg;
                                        comment.userIdFormer =reCommentUserId;
                                    }
                                    comments.add(comment);
                                }
                            mHandler.sendEmptyMessage(1);
                        } else
                        {
                            ToolUtils.setToast(mContext, R.string.loading_fail_txt);
                            return;
                        }
                    }
                }, new Response.ErrorListener()
        {
            @Override
            public void onErrorResponse(VolleyError volleyError)
            {
                mDialog.dismiss();
                listView.onRefreshComplete();
                if (page >1)
                {
                    page--;
                    ToolUtils.setToast(mContext, R.string.loading_fail_txt);
                }
            }
        });
        ZDApplication.mRequestQueue.add(request);
    }

    private OnCommentListener onCommentListener;

    public void setOnCommentListener(OnCommentListener onCommentListener)
    {
        this.onCommentListener=onCommentListener;
    }

    public interface OnCommentListener
    {
        void GetComments(List<Comment> comments1,int num);
    }


    public void onResume()
    {
        super.onResume();
        MobclickAgent.onPageStart("评论列表");
    }

    public void onPause()
    {
        super.onPause();
        MobclickAgent.onPageEnd("评论列表");
    }

    @Override
    public void onDestroy()
    {
        if (comments!=null)
        {
            if (comments.size()>4)
            {
                List<Comment> comments1=new ArrayList<Comment>();
                for (int i = 0; i <5 ; i++)
                {
                    comments1.add(comments.get(i));
                }
                onCommentListener.GetComments(comments1,pageCount);
            }
            else
            {
                onCommentListener.GetComments(comments,pageCount);
            }
        }
        super.onDestroy();
    }
}
