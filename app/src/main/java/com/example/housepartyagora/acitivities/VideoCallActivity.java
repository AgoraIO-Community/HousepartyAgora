package com.example.housepartyagora.acitivities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewStub;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.housepartyagora.R;
import com.example.housepartyagora.adapter.FriendListRecyclerViewAdapter;
import com.example.housepartyagora.adapter.MessageAdapter;
import com.example.housepartyagora.adapter.ShowFriendListRecyclerViewAdapter;
import com.example.housepartyagora.layout.GridVideoViewContainer;
import com.example.housepartyagora.layout.OnSwipeTouchListener;
import com.example.housepartyagora.layout.RecyclerItemClickListener;
import com.example.housepartyagora.layout.RtlLinearLayoutManager;
import com.example.housepartyagora.layout.SmallVideoViewAdapter;
import com.example.housepartyagora.layout.SmallVideoViewDecoration;
import com.example.housepartyagora.model.Friend;
import com.example.housepartyagora.model.MessageBean;
import com.example.housepartyagora.model.MessageListBean;
import com.example.housepartyagora.model.User;
import com.example.housepartyagora.model.UserStatusData;
import com.example.housepartyagora.rtm.AGApplication;
import com.example.housepartyagora.rtm.ChatManager;
import com.example.housepartyagora.utils.MessageUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;
import io.agora.rtm.ErrorInfo;
import io.agora.rtm.ResultCallback;
import io.agora.rtm.RtmChannel;
import io.agora.rtm.RtmClient;
import io.agora.rtm.RtmClientListener;
import io.agora.rtm.RtmMessage;
import io.agora.rtm.RtmStatusCode;

public class VideoCallActivity extends AppCompatActivity {public static final int LAYOUT_TYPE_DEFAULT = 0;
    public static final int LAYOUT_TYPE_SMALL = 1;

    private List<Friend> searchFriendList = new ArrayList<>();
    private List<Friend> friendList = new ArrayList<>();
    private String userName;
    private String channelName;
    private User user;
    private static final String TAG = VideoCallActivity.class.getName();
    public int mLayoutType = LAYOUT_TYPE_DEFAULT;
    private static final int PERMISSION_REQ_ID = 22;
    RtcEngine mRtcEngine;
    private ImageView mCallBtn, mMuteBtn, mSwitchCameraBtn;
    private GridVideoViewContainer mGridVideoViewContainer;
    private boolean isCalling = true;
    private boolean isMuted = false;
    private boolean isVoiceChanged = false;
    private boolean mIsLandscape = false;
    private RelativeLayout mSmallVideoViewDock;
    private SmallVideoViewAdapter mSmallVideoViewAdapter;
    private boolean mIsPeerToPeerMode = true;
    private String mActualTarget;
    private boolean isAddingFriend = false;
    private boolean isShowingFriend = false;
    private LinearLayout mAddFriendLinearLayout;
    private EditText mSearchFriendEditText;
    private RecyclerView mFriendListRecyclerView;
    private FriendListRecyclerViewAdapter mFriendListRecyclerViewAdapter;
    private final HashMap<Integer, SurfaceView> mUidsList = new HashMap<>();
    private LinearLayout mShowFriendLinearLayout;
    private RecyclerView mShowFriendListRecyclerView;
    private ShowFriendListRecyclerViewAdapter mShowFriendListRecyclerViewAdapter;
    private RelativeLayout mChatLayout;
    private String mPeerId = "";
    private RecyclerView mRecyclerView;
    private List<MessageBean> mMessageBeanList = new ArrayList<>();
    private MessageAdapter mMessageAdapter;

    private ChatManager mChatManager;
    private RtmClient mRtmClient;
    private RtmClientListener mClientListener;
    private RtmChannel mRtmChannel;
    private TextView mTitleTextView;
    private EditText mMsgEditText;

    // Ask for Android device permissions at runtime.
    private static final String[] REQUESTED_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        // Listen for the onJoinChannelSuccess callback.
        // This callback occurs when the local user successfully joins the channel.
        public void onJoinChannelSuccess(String channel, final int uid, int elapsed) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(VideoCallActivity.this, "User: " + uid + " join!", Toast.LENGTH_LONG).show();
                    Log.i("agora", "Join channel success, uid: " + (uid & 0xFFFFFFFFL));
                    user.setAgoraUid(uid);
                    SurfaceView localView = mUidsList.remove(0);
                    mUidsList.put(uid, localView);
                }
            });
        }

        @Override
        // Listen for the onFirstRemoteVideoDecoded callback.
        // This callback occurs when the first video frame of a remote user is received and decoded after the remote user successfully joins the channel.
        // You can call the setupRemoteVideo method in this callback to set up the remote video view.
        public void onFirstRemoteVideoDecoded(final int uid, int width, int height, int elapsed) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.i("agora", "First remote video decoded, uid: " + (uid & 0xFFFFFFFFL));
                    setupRemoteVideo(uid);
                }
            });
        }

        @Override
        // Listen for the onUserOffline callback.
        // This callback occurs when the remote user leaves the channel or drops offline.
        public void onUserOffline(final int uid, int reason) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(VideoCallActivity.this, "User: " + uid + " left the room.", Toast.LENGTH_LONG).show();
                    Log.i("agora", "User offline, uid: " + (uid & 0xFFFFFFFFL));
                    onRemoteUserLeft(uid);
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.hide();
        }
        setContentView(R.layout.activity_video_call);
        getExtras();
        initUI();

        if (checkSelfPermission(REQUESTED_PERMISSIONS[0], PERMISSION_REQ_ID) &&
                checkSelfPermission(REQUESTED_PERMISSIONS[1], PERMISSION_REQ_ID) &&
                checkSelfPermission(REQUESTED_PERMISSIONS[2], PERMISSION_REQ_ID)) {
            initEngineAndJoinChannel();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQ_ID: {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED || grantResults[1] != PackageManager.PERMISSION_GRANTED) {
                    break;
                }
                initEngineAndJoinChannel();
                break;
            }
        }
    }

    private void getExtras() {
        userName = getIntent().getExtras().getString("userName");
        channelName = userName;

        //todo: create a user
        user = new User("122");
        mIsPeerToPeerMode = false;
        mActualTarget = "test";
    }

    private void initUI() {
        mCallBtn = findViewById(R.id.start_call_end_call_btn);
        mMuteBtn = findViewById(R.id.audio_mute_audio_unmute_btn);
        mSwitchCameraBtn = findViewById(R.id.switch_camera_btn);
        mAddFriendLinearLayout = findViewById(R.id.layout_add_friends);
        mGridVideoViewContainer = findViewById(R.id.grid_video_view_container);
        mSearchFriendEditText = findViewById(R.id.et_search_friends);
        mFriendListRecyclerView = findViewById(R.id.rv_friendList);
        mShowFriendLinearLayout = findViewById(R.id.layout_show_friends);
        mShowFriendListRecyclerView = findViewById(R.id.rv_show_friendList);
        mChatLayout = findViewById(R.id.layout_chat);
        mRecyclerView = findViewById(R.id.message_list);
        mTitleTextView = findViewById(R.id.message_title);
        mMsgEditText = findViewById(R.id.message_edittiext);

        mGridVideoViewContainer.setItemEventHandler(new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                //can add single click listener logic
            }

            @Override
            public void onItemLongClick(View view, int position) {
                //can add long click listener logic
            }

            @Override
            public void onItemDoubleClick(View view, int position) {
                onBigVideoViewDoubleClicked(view, position);
            }

        });


        mGridVideoViewContainer.setOnTouchListener(new OnSwipeTouchListener(this) {
            public void onSwipeRight() {
                //todo: iterate friend list and find next frind online



                //     leave channel and join a friend's channel
                finishCalling();

                channelName = "newfriend";

                SurfaceView surfaceView = RtcEngine.CreateRendererView(getBaseContext());
                mRtcEngine.setupLocalVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0));
                surfaceView.setZOrderOnTop(false);
                surfaceView.setZOrderMediaOverlay(false);

                mUidsList.put(0, surfaceView);

                joinChannel();
            }
        });
    }

    private void initEngineAndJoinChannel() {
        initializeEngine();
        loginRTM();
        setupLocalVideo();
        joinChannel();
    }

    private void loginRTM() {
        mRtmClient.login(null, userName, new io.agora.rtm.ResultCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {

            }

            @Override
            public void onFailure(ErrorInfo errorInfo) {
                Toast.makeText(VideoCallActivity.this, "RTM login failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initializeEngine() {
        try {
            mRtcEngine = RtcEngine.create(getBaseContext(), getString(R.string.agora_app_id), mRtcEventHandler);
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
            throw new RuntimeException("NEED TO check rtc sdk init fatal error\n" + Log.getStackTraceString(e));
        }

        mChatManager = AGApplication.the().getChatManager();
        mRtmClient = mChatManager.getRtmClient();
        mClientListener = new MyRtmClientListener();
        mChatManager.registerListener(mClientListener);
    }

    private void setupLocalVideo() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRtcEngine.enableVideo();
                mRtcEngine.enableInEarMonitoring(true);
                mRtcEngine.setInEarMonitoringVolume(80);

                SurfaceView surfaceView = RtcEngine.CreateRendererView(getBaseContext());
                mRtcEngine.setupLocalVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0));
                surfaceView.setZOrderOnTop(false);
                surfaceView.setZOrderMediaOverlay(false);

                mUidsList.put(0, surfaceView);

                mGridVideoViewContainer.initViewContainer(VideoCallActivity.this, 0, mUidsList, mIsLandscape);
            }
        });
    }

    private void joinChannel() {
        // Join a channel with a token, token can be null.
        mRtcEngine.joinChannel(null, channelName, "Extra Optional Data", 0);
    }

    private boolean checkSelfPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, requestCode);
            return false;
        }
        return true;
    }

    private void onBigVideoViewDoubleClicked(View view, int position) {
        if (mUidsList.size() < 2) {
            return;
        }

        UserStatusData user = mGridVideoViewContainer.getItem(position);
        //int uid = (user.mUid == 0) ? this.user.getAgoraUid() : user.mUid;

        if (user.mUid != this.user.getAgoraUid()) {

            //todo: get user name from firebase database
            startMessaging("test");
        }



//        if (mLayoutType == LAYOUT_TYPE_DEFAULT && mUidsList.size() != 1) {
//            switchToSmallVideoView(uid);
//        } else {
//            switchToDefaultVideoView();
//        }
    }

    private void startMessaging(String userName) {
        mChatLayout.setVisibility(View.VISIBLE);
        mSwitchCameraBtn.setVisibility(View.GONE);
        mCallBtn.setVisibility(View.GONE);
        mMuteBtn.setVisibility(View.GONE);
        mAddFriendLinearLayout.setVisibility(View.GONE);
        mShowFriendLinearLayout.setVisibility(View.GONE);

        mPeerId = userName;

        // load history chat records
        MessageListBean messageListBean = MessageUtil.getExistMessageListBean(mPeerId);
        if (messageListBean != null) {
            mMessageBeanList.addAll(messageListBean.getMessageBeanList());
        }

        // load offline messages since last chat with this peer.
        // Then clear cached offline messages from message pool
        // since they are already consumed.
        MessageListBean offlineMessageBean = new MessageListBean(mPeerId, mChatManager);
        mMessageBeanList.addAll(offlineMessageBean.getMessageBeanList());
        mChatManager.removeAllOfflineMessages(mPeerId);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        mMessageAdapter = new MessageAdapter(this, mMessageBeanList);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setAdapter(mMessageAdapter);

    }

    private void switchToSmallVideoView(int bigBgUid) {
        HashMap<Integer, SurfaceView> slice = new HashMap<>(1);
        slice.put(bigBgUid, mUidsList.get(bigBgUid));
        Iterator<SurfaceView> iterator = mUidsList.values().iterator();
        while (iterator.hasNext()) {
            SurfaceView s = iterator.next();
            s.setZOrderOnTop(true);
            s.setZOrderMediaOverlay(true);
        }

        mUidsList.get(bigBgUid).setZOrderOnTop(false);
        mUidsList.get(bigBgUid).setZOrderMediaOverlay(false);

        mGridVideoViewContainer.initViewContainer(this, bigBgUid, slice, mIsLandscape);

        bindToSmallVideoView(bigBgUid);

        mLayoutType = LAYOUT_TYPE_SMALL;
    }

    private void bindToSmallVideoView(int exceptUid) {
        if (mSmallVideoViewDock == null) {
            ViewStub stub = (ViewStub) findViewById(R.id.small_video_view_dock);
            mSmallVideoViewDock = (RelativeLayout) stub.inflate();
        }

        boolean twoWayVideoCall = mUidsList.size() == 2;

        RecyclerView recycler = (RecyclerView) findViewById(R.id.small_video_view_container);

        boolean create = false;

        if (mSmallVideoViewAdapter == null) {
            create = true;
            mSmallVideoViewAdapter = new SmallVideoViewAdapter(this, this.user.getAgoraUid(), exceptUid, mUidsList);
            mSmallVideoViewAdapter.setHasStableIds(true);
        }
        recycler.setHasFixedSize(true);

        if (twoWayVideoCall) {
            recycler.setLayoutManager(new RtlLinearLayoutManager(getApplicationContext(), RtlLinearLayoutManager.HORIZONTAL, false));
        } else {
            recycler.setLayoutManager(new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.HORIZONTAL, false));
        }
        recycler.addItemDecoration(new SmallVideoViewDecoration());
        recycler.setAdapter(mSmallVideoViewAdapter);
        recycler.addOnItemTouchListener(new RecyclerItemClickListener(getBaseContext(), new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {

            }

            @Override
            public void onItemLongClick(View view, int position) {

            }

            @Override
            public void onItemDoubleClick(View view, int position) {
                onSmallVideoViewDoubleClicked(view, position);
            }
        }));

        recycler.setDrawingCacheEnabled(true);
        recycler.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_AUTO);

        if (!create) {
            mSmallVideoViewAdapter.setLocalUid(this.user.getAgoraUid());
            mSmallVideoViewAdapter.notifyUiChanged(mUidsList, exceptUid, null, null);
        }
        for (Integer tempUid : mUidsList.keySet()) {
            if (this.user.getAgoraUid() != tempUid) {
                if (tempUid == exceptUid) {
                    mRtcEngine.setRemoteUserPriority(tempUid, Constants.USER_PRIORITY_HIGH);
                } else {
                    mRtcEngine.setRemoteUserPriority(tempUid, Constants.USER_PRIORITY_NORANL);
                }
            }
        }
        recycler.setVisibility(View.VISIBLE);
        mSmallVideoViewDock.setVisibility(View.VISIBLE);
    }

    private void onSmallVideoViewDoubleClicked(View view, int position) {
        switchToDefaultVideoView();
    }

    private void onRemoteUserLeft(int uid) {
        removeRemoteVideo(uid);
    }

    private void removeRemoteVideo(final int uid) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Object target = mUidsList.remove(uid);
                if (target == null) {
                    return;
                }
                switchToDefaultVideoView();
            }
        });

    }

    private void setupRemoteVideo(final int uid) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SurfaceView mRemoteView = RtcEngine.CreateRendererView(getApplicationContext());

                mUidsList.put(uid, mRemoteView);
                mRemoteView.setZOrderOnTop(true);
                mRemoteView.setZOrderMediaOverlay(true);
                mRtcEngine.setupRemoteVideo(new VideoCanvas(mRemoteView, VideoCanvas.RENDER_MODE_HIDDEN, uid));

                switchToDefaultVideoView();
            }
        });
    }

    private void switchToDefaultVideoView() {

        mGridVideoViewContainer.initViewContainer(VideoCallActivity.this, user.getAgoraUid(), mUidsList, mIsLandscape);

        boolean setRemoteUserPriorityFlag = false;

        mLayoutType = LAYOUT_TYPE_DEFAULT;

        int sizeLimit = mUidsList.size();
        if (sizeLimit > 5) {
            sizeLimit = 5;
        }

        for (int i = 0; i < sizeLimit; i++) {
            int uid = mGridVideoViewContainer.getItem(i).mUid;
            if (user.getAgoraUid() != uid) {
                if (!setRemoteUserPriorityFlag) {
                    setRemoteUserPriorityFlag = true;
                    mRtcEngine.setRemoteUserPriority(uid, Constants.USER_PRIORITY_HIGH);
                } else {
                    mRtcEngine.setRemoteUserPriority(uid, Constants.USER_PRIORITY_NORANL);
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isCalling) {
            leaveChannel();
        }
        RtcEngine.destroy();
        MessageUtil.addMessageListBeanList(new MessageListBean(mPeerId, mMessageBeanList));
        mChatManager.unregisterListener(mClientListener);
    }

    private void leaveChannel() {
        // Leave the current channel.
        mRtcEngine.leaveChannel();
    }

    public void onCallClicked(View view) {
        if (isCalling) {
            //finish current call
            finishCalling();
            isCalling = false;
            mCallBtn.setImageResource(R.drawable.btn_startcall);
            finish();
        } else {
            //start the call
            startCalling();
            isCalling = true;
            mCallBtn.setImageResource(R.drawable.btn_endcall);
        }
    }

    private void finishCalling() {
        leaveChannel();
        mUidsList.clear();
    }

    private void startCalling() {
        setupLocalVideo();
        joinChannel();
    }

    public void onSwitchCameraClicked(View view) {
        mRtcEngine.switchCamera();
    }

    public void onLocalAudioMuteClicked(View view) {
        isMuted = !isMuted;
        mRtcEngine.muteLocalAudioStream(isMuted);
        int res = isMuted ? R.drawable.btn_mute : R.drawable.btn_unmute;
        mMuteBtn.setImageResource(res);
    }

    public void onAddFriendClick(View view) {
        if (isShowingFriend) {
            isShowingFriend = !isShowingFriend;
            mShowFriendLinearLayout.setVisibility(isShowingFriend ? View.VISIBLE : View.GONE);
        }
        isAddingFriend = !isAddingFriend;
        mAddFriendLinearLayout.setVisibility(isAddingFriend ? View.VISIBLE : View.GONE);

    }

    public void onSearchButtonClick(View view) {
        String searchFriendName = mSearchFriendEditText.getText().toString();
        if (searchFriendName == null || searchFriendName == "") {
            Toast.makeText(this, "Name can not be empty!", Toast.LENGTH_SHORT).show();
        }else {
            searchFriends(searchFriendName);
        }
    }

    private void searchFriends(String searchFriendName) {
        //todo: implement search friend logic
        searchFriendList.clear();
        Friend friend = new Friend();
        friend.setUserName(searchFriendName);
        searchFriendList.add(friend);

        mFriendListRecyclerViewAdapter = new FriendListRecyclerViewAdapter(searchFriendList);
        mFriendListRecyclerViewAdapter.setOnItemClickListener(new FriendListRecyclerViewAdapter.ClickListener() {
            @Override
            public void onItemClick(int position, View v) {
                addFriend(searchFriendList.get(position).getUserName());
                mSearchFriendEditText.setText("");
            }
        });
        RecyclerView.LayoutManager manager = new GridLayoutManager(this, 1);
        mFriendListRecyclerView.setLayoutManager(manager);

        mFriendListRecyclerView.setAdapter(mFriendListRecyclerViewAdapter);

    }

    public void addFriend(String userName) {
        friendList.add(new Friend(userName));
    }

    public void onShowFriendClick(View view) {
        if (isAddingFriend) {
            isAddingFriend = !isAddingFriend;
            mAddFriendLinearLayout.setVisibility(isAddingFriend ? View.VISIBLE : View.GONE);
        }

        isShowingFriend = !isShowingFriend;
        mShowFriendLinearLayout.setVisibility(isShowingFriend ? View.VISIBLE : View.GONE);

        mShowFriendListRecyclerViewAdapter = new ShowFriendListRecyclerViewAdapter(friendList);
        mShowFriendListRecyclerViewAdapter.setOnItemClickListener(new ShowFriendListRecyclerViewAdapter.ClickListener() {
            @Override
            public void onItemClick(int position, View v) {

                if (v.getId() == R.id.btn_join_friend) {
                    //todo: check if friend is locked
                    joinFriend(friendList.get(position).getUserName());
                }else if (v.getId() == R.id.btn_chat_friend){
                    startMessaging(friendList.get(position).getUserName());
                }
                //Toast.makeText(VideoCallActivity.this, "" + friendList.get(position).getUserName(), Toast.LENGTH_SHORT).show();
            }
        });
        RecyclerView.LayoutManager manager = new LinearLayoutManager(this);
        mShowFriendListRecyclerView.setLayoutManager(manager);
        mShowFriendListRecyclerView.setAdapter(mShowFriendListRecyclerViewAdapter);

    }

    public void joinFriend(String friendName){

    }

    public void onChatCloseClicked(View view) {
        mChatLayout.setVisibility(View.GONE);
        mSwitchCameraBtn.setVisibility(View.VISIBLE);
        mCallBtn.setVisibility(View.VISIBLE);
        mMuteBtn.setVisibility(View.VISIBLE);
    }

    public void onClickSend(View v) {
        String msg = mMsgEditText.getText().toString();
        if (!msg.equals("")) {
            MessageBean messageBean = new MessageBean(user.getFireDisplayName(), msg, true);
            mMessageBeanList.add(messageBean);
            mMessageAdapter.notifyItemRangeChanged(mMessageBeanList.size(), 1);
            mRecyclerView.scrollToPosition(mMessageBeanList.size() - 1);
            sendPeerMessage(msg);
        }
        mMsgEditText.setText("");
    }

    /**
     * API CALL: send message to peer
     */
    private void sendPeerMessage(String content) {
        // step 1: create a message
        RtmMessage message = mRtmClient.createMessage();
        message.setText(content);

        // step 2: send message to peer
        mRtmClient.sendMessageToPeer(mPeerId, message, mChatManager.getSendMessageOptions(), new ResultCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {

            }

            @Override
            public void onFailure(ErrorInfo errorInfo) {
                // refer to RtmStatusCode.PeerMessageState for the message state
                final int errorCode = errorInfo.getErrorCode();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        switch (errorCode) {
                            case RtmStatusCode.PeerMessageError.PEER_MESSAGE_ERR_TIMEOUT:
                            case RtmStatusCode.PeerMessageError.PEER_MESSAGE_ERR_FAILURE:
                                showToast(getString(R.string.send_msg_failed));
                                break;
                            case RtmStatusCode.PeerMessageError.PEER_MESSAGE_ERR_PEER_UNREACHABLE:
                                showToast(getString(R.string.peer_offline));
                                break;
                            case RtmStatusCode.PeerMessageError.PEER_MESSAGE_ERR_CACHED_BY_SERVER:
                                showToast(getString(R.string.message_cached));
                                break;
                        }
                    }
                });
            }
        });
    }

    class MyRtmClientListener implements RtmClientListener {

        @Override
        public void onConnectionStateChanged(final int state, final int reason) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    switch (state) {
                        case RtmStatusCode.ConnectionState.CONNECTION_STATE_RECONNECTING:
                            showToast(getString(R.string.reconnecting));
                            break;
                        case RtmStatusCode.ConnectionState.CONNECTION_STATE_ABORTED:
                            showToast(getString(R.string.account_offline));
                            setResult(MessageUtil.ACTIVITY_RESULT_CONN_ABORTED);
                            finish();
                            break;
                    }
                }
            });
        }

        @Override
        public void onMessageReceived(final RtmMessage message, final String peerId) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String content = message.getText();
                    if (peerId.equals(mPeerId)) {
                        MessageBean messageBean = new MessageBean(peerId, content,false);
                        messageBean.setBackground(getMessageColor(peerId));
                        mMessageBeanList.add(messageBean);
                        mMessageAdapter.notifyItemRangeChanged(mMessageBeanList.size(), 1);
                        mRecyclerView.scrollToPosition(mMessageBeanList.size() - 1);
                    } else {
                        MessageUtil.addMessageBean(peerId, content);
                    }
                }
            });
        }

        @Override
        public void onTokenExpired() {

        }

        @Override
        public void onPeersOnlineStatusChanged(Map<String, Integer> map) {

        }
    }

    private void showToast(final String text) {
        Toast.makeText(VideoCallActivity.this, text, Toast.LENGTH_SHORT).show();
    }

    private int getMessageColor(String account) {
        for (int i = 0; i < mMessageBeanList.size(); i++) {
            if (account.equals(mMessageBeanList.get(i).getAccount())) {
                return mMessageBeanList.get(i).getBackground();
            }
        }
        return MessageUtil.COLOR_ARRAY[MessageUtil.RANDOM.nextInt(MessageUtil.COLOR_ARRAY.length)];
    }
}
