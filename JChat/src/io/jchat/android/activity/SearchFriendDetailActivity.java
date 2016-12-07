package io.jchat.android.activity;


import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.File;

import cn.jpush.im.android.api.JMessageClient;
import cn.jpush.im.android.api.callback.GetAvatarBitmapCallback;
import cn.jpush.im.android.api.callback.GetUserInfoCallback;
import cn.jpush.im.android.api.model.UserInfo;
import io.jchat.android.R;
import io.jchat.android.application.JChatDemoApplication;
import io.jchat.android.chatting.CircleImageView;
import io.jchat.android.chatting.utils.DialogCreator;
import io.jchat.android.chatting.utils.HandleResponseCode;
import io.jchat.android.tools.NativeImageLoader;

public class SearchFriendDetailActivity extends BaseActivity {

    private TextView title;
    private TextView mNickNameTv;
    private ImageButton mReturnBtn;
    private CircleImageView mAvatarIv;
    private ImageView mGenderIv;
    private TextView mGenderTv;
    private TextView mAreaTv;
    private TextView mSignatureTv;
    private TextView mAddFriendBtn;
    private Context mContext;
    private boolean mIsGetAvatar = false;
    private String mUsername;
    private String mAppKey;
    private String mAvatarPath;
    private String mDisplayName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_result_detail);
        mContext = this;
        title = (TextView) findViewById(R.id.title);
        title.setText(this.getString(R.string.search_friend_title_bar));
        mNickNameTv = (TextView) findViewById(R.id.nick_name_tv);
        mReturnBtn = (ImageButton) findViewById(R.id.friend_info_return_btn);
        mAvatarIv = (CircleImageView) findViewById(R.id.friend_detail_avatar);
        mGenderIv = (ImageView) findViewById(R.id.gender_iv);
        mGenderTv = (TextView) findViewById(R.id.gender_tv);
        mAreaTv = (TextView) findViewById(R.id.region_tv);
        mSignatureTv = (TextView) findViewById(R.id.signature_tv);
        mAddFriendBtn = (Button) findViewById(R.id.add_to_friend);

        inModule();


        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.friend_info_return_btn:
                        SearchFriendDetailActivity.this.finish();
                        break;
                    case R.id.add_to_friend:
                        Intent intent = new Intent();
                        intent.setClass(mContext, SendInvitationActivity.class);
                        intent.putExtra("targetUsername", mUsername);
                        intent.putExtra(JChatDemoApplication.AVATAR, mAvatarPath);
                        intent.putExtra(JChatDemoApplication.TARGET_APP_KEY, mAppKey);
                        intent.putExtra(JChatDemoApplication.NICKNAME, mDisplayName);
                        startActivity(intent);
                        break;
                    case R.id.friend_detail_avatar:
                        startBrowserAvatar();
                        break;
                }
            }
        };
        mReturnBtn.setOnClickListener(listener);
        mAddFriendBtn.setOnClickListener(listener);
        mAvatarIv.setOnClickListener(listener);
    }

    private void inModule() {
        Intent intent = getIntent();
        mUsername = intent.getStringExtra(JChatDemoApplication.NAME);
        mAppKey = intent.getStringExtra(JChatDemoApplication.TARGET_APP_KEY);
        mAvatarPath = intent.getStringExtra(JChatDemoApplication.AVATAR);
        mDisplayName = intent.getStringExtra(JChatDemoApplication.NICKNAME);
        Bitmap bitmap = NativeImageLoader.getInstance().getBitmapFromMemCache(mUsername);
        if (null != bitmap) {
            mAvatarIv.setImageBitmap(bitmap);
        } else if (null != mAvatarPath) {
            File file = new File(mAvatarPath);
            if (file.exists() && file.isFile()) {
                Picasso.with(mContext).load(file).into(mAvatarIv);
            }
        }

        mNickNameTv.setText(mDisplayName);
        String gender = intent.getStringExtra(JChatDemoApplication.GENDER);

        if (gender.equals("male")) {
            mGenderTv.setText(mContext.getString(R.string.man));
            mGenderIv.setImageResource(R.drawable.sex_man);
        } else if (gender.equals("female")) {
            mGenderTv.setText(mContext.getString(R.string.woman));
            mGenderIv.setImageResource(R.drawable.sex_woman);
        } else {
            mGenderTv.setText(mContext.getString(R.string.unknown));
        }
        mAreaTv.setText(intent.getStringExtra(JChatDemoApplication.REGION));
        mSignatureTv.setText(intent.getStringExtra(JChatDemoApplication.SIGNATURE));
    }

    private void startBrowserAvatar() {
        if (null != mAvatarPath) {
            if (mIsGetAvatar) {
                //如果缓存了图片，直接加载
                Bitmap bitmap = NativeImageLoader.getInstance().getBitmapFromMemCache(mUsername);
                if (bitmap != null) {
                    Intent intent = new Intent();
                    intent.putExtra("browserAvatar", true);
                    intent.putExtra("avatarPath", mUsername);
                    intent.setClass(this, BrowserViewPagerActivity.class);
                    startActivity(intent);
                }
            } else {
                final Dialog dialog = DialogCreator.createLoadingDialog(this, this.getString(R.string.jmui_loading));
                dialog.show();
                JMessageClient.getUserInfo(mUsername, new GetUserInfoCallback() {
                    @Override
                    public void gotResult(int status, String desc, UserInfo userInfo) {
                        if (status == 0) {
                            userInfo.getBigAvatarBitmap(new GetAvatarBitmapCallback() {
                                @Override
                                public void gotResult(int status, String desc, Bitmap bitmap) {
                                    if (status == 0) {
                                        mIsGetAvatar = true;
                                        //缓存头像
                                        NativeImageLoader.getInstance().updateBitmapFromCache(mUsername, bitmap);
                                        Intent intent = new Intent();
                                        intent.putExtra("browserAvatar", true);
                                        intent.putExtra("avatarPath", mUsername);
                                        intent.setClass(mContext, BrowserViewPagerActivity.class);
                                        startActivity(intent);
                                    } else {
                                        HandleResponseCode.onHandle(mContext, status, false);
                                    }
                                    dialog.dismiss();
                                }
                            });
                        } else {
                            dialog.dismiss();
                            HandleResponseCode.onHandle(mContext, status, false);
                        }
                    }
                });
            }
        }
    }
}
