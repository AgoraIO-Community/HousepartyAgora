package com.example.housepartyagora.acitivities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.example.housepartyagora.layout.RecyclerItemClickListener;
import com.example.housepartyagora.model.DBUser;
import com.example.housepartyagora.model.MessageBean;
import com.example.housepartyagora.model.MessageListBean;
import com.example.housepartyagora.model.User;
import com.example.housepartyagora.model.UserStatusData;
import com.example.housepartyagora.rtm.AGApplication;
import com.example.housepartyagora.rtm.ChatManager;
import com.example.housepartyagora.utils.Constant;
import com.example.housepartyagora.utils.MessageUtil;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;
import io.agora.rtm.ErrorInfo;
import io.agora.rtm.ResultCallback;
import io.agora.rtm.RtmClient;
import io.agora.rtm.RtmClientListener;
import io.agora.rtm.RtmMessage;
import io.agora.rtm.RtmStatusCode;

public class VideoCallActivity extends AppCompatActivity {public static final int LAYOUT_TYPE_DEFAULT = 0;

    private List<DBUser> searchFriendList = new ArrayList<>();
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
    private boolean mIsLandscape = false;
    private boolean isAddingFriend = false;
    private boolean isShowingFriend = false;
    private boolean isLocalCall = true;
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
    private String localState = Constant.USER_STATE_OPEN;
    private List<String> DBFriend = new ArrayList<>();
    private TextView mTitleTextView;

    private ChatManager mChatManager;
    private RtmClient mRtmClient;
    private RtmClientListener mClientListener;
    private EditText mMsgEditText;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference mRef;
    private ChildEventListener childEventListener;
    private ChildEventListener joinFriendChildEventListener;
    private ChildEventListener chatSearchChildEventListener;

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
                    showToast("User: " + uid + " join!");
                    user.setAgoraUid(uid);
                    SurfaceView localView = mUidsList.remove(0);
                    mUidsList.put(uid, localView);
                    mRef.child(getUserName()).setValue(new DBUser(getUserName(), user.getAgoraUid(), localState, DBFriend));
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
                    showToast("User: " + uid + " left the room.");
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
        connectToFireDB(userName);

        if (checkSelfPermission(REQUESTED_PERMISSIONS[0], PERMISSION_REQ_ID) &&
                checkSelfPermission(REQUESTED_PERMISSIONS[1], PERMISSION_REQ_ID) &&
                checkSelfPermission(REQUESTED_PERMISSIONS[2], PERMISSION_REQ_ID)) {
            initEngineAndJoinChannel();
        }
    }

    //request application permission
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
        user = new User();
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
        mMsgEditText = findViewById(R.id.message_edittiext);
        mTitleTextView = findViewById(R.id.message_title);

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
    }

    //connect to google fire database
    private void connectToFireDB(final String userName) {
        mRef = database.getReference("Users");

        //listen to the friend list in the database
        mRef.push();
        mRef.child(this.userName).child("friend").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                DBFriend = (List<String>) dataSnapshot.getValue();
                if (DBFriend == null) {
                    DBFriend = new ArrayList<>();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                showToast(databaseError.getMessage());
            }
        });

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mRef.child(userName).setValue(new DBUser(userName, user.getAgoraUid(), localState, DBFriend));
            }
        }, 1500);
    }

    private void initEngineAndJoinChannel() {
        initializeEngine();
        loginRTM();
        setupLocalVideo();
        joinChannel();
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

    //login into RTM for chat messaging
    private void loginRTM() {
        mRtmClient.login(null, userName, new io.agora.rtm.ResultCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {

            }

            @Override
            public void onFailure(ErrorInfo errorInfo) {
                showToast("RTM login failed");
            }
        });
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

        final UserStatusData user = mGridVideoViewContainer.getItem(position);

        if (user.mUid != this.user.getAgoraUid()) {

            chatSearchChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                    DBUser result = dataSnapshot.getValue(DBUser.class);
                    startMessaging(result.getName());

                    mRef.orderByChild("uid").startAt(user.mUid).endAt(user.mUid + "\uf8ff").removeEventListener(chatSearchChildEventListener);

                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            };

            mRef.orderByChild("uid").startAt(user.mUid).endAt(user.mUid + "\uf8ff").addChildEventListener(chatSearchChildEventListener);
        }
    }

    private void startMessaging(String userName) {
        mPeerId = userName;
        mMessageBeanList = new ArrayList<>();
        // load history chat records
        MessageListBean messageListBean = MessageUtil.getExistMessageListBean(mPeerId);

        mTitleTextView.setText(mPeerId);

        mChatLayout.setVisibility(View.VISIBLE);
        mSwitchCameraBtn.setVisibility(View.GONE);
        mCallBtn.setVisibility(View.GONE);
        mMuteBtn.setVisibility(View.GONE);
        mAddFriendLinearLayout.setVisibility(View.GONE);
        mShowFriendLinearLayout.setVisibility(View.GONE);

        if (messageListBean != null) {
            mMessageBeanList.addAll(messageListBean.getMessageBeanList());

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

        }else {
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

    public void onLockRoomClick(View view) {
        if (isLocalCall) {
            //when the user is in his own room
            if (localState.equals(Constant.USER_STATE_LOCK)) {
                //set the room to public
                localState = Constant.USER_STATE_OPEN;
                mRef.child(this.userName).setValue(new DBUser(this.userName, user.getAgoraUid(), localState, DBFriend));
                showToast("Room set to public");
            }else {
                //set the room to private so that no one can join the room
                localState = Constant.USER_STATE_LOCK;
                mRef.child(this.userName).setValue(new DBUser(this.userName, user.getAgoraUid(), localState, DBFriend));
                showToast("Room set to private");
            }
        }else {
            //when user is joining other people's room
            //leave that room and come back to user's own room
            isLocalCall = true;
            finishCalling();
            channelName = userName;
            startCalling();
            localState = Constant.USER_STATE_OPEN;
            //update user's room state
            mRef.child(userName).setValue(new DBUser(userName, user.getAgoraUid(), localState, DBFriend));
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

    private String getUserName() {
        return this.userName;
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
        if (searchFriendName == null || searchFriendName.equals("")) {
            showToast("Name can not be empty!");
        }else {
            searchFriends(searchFriendName);
        }
    }

    private void searchFriends(final String searchFriendName) {
        //search for a new friend in the database
        searchFriendList.clear();
        mFriendListRecyclerViewAdapter = new FriendListRecyclerViewAdapter(searchFriendList);
        mFriendListRecyclerViewAdapter.setOnItemClickListener(new FriendListRecyclerViewAdapter.ClickListener() {
            @Override
            public void onItemClick(int position, View v) {
                addFriend(searchFriendList.get(position).getName());
                mSearchFriendEditText.setText("");
                searchFriendList.clear();
                mFriendListRecyclerView.setAdapter(mFriendListRecyclerViewAdapter);
            }
        });
        RecyclerView.LayoutManager manager = new GridLayoutManager(getBaseContext(), 1);
        mFriendListRecyclerView.setLayoutManager(manager);

        mFriendListRecyclerView.setAdapter(mFriendListRecyclerViewAdapter);

        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                DBUser result = dataSnapshot.getValue(DBUser.class);

                searchFriendList.add(result);
                mRef.orderByChild("name").startAt(searchFriendName).endAt(searchFriendName + "\uf8ff").removeEventListener(childEventListener);

                mFriendListRecyclerViewAdapter = new FriendListRecyclerViewAdapter(searchFriendList);
                mFriendListRecyclerViewAdapter.setOnItemClickListener(new FriendListRecyclerViewAdapter.ClickListener() {
                    @Override
                    public void onItemClick(int position, View v) {
                        addFriend(searchFriendList.get(position).getName());
                        mSearchFriendEditText.setText("");
                        searchFriendList.clear();
                        mFriendListRecyclerView.setAdapter(mFriendListRecyclerViewAdapter);
                    }
                });
                RecyclerView.LayoutManager manager = new GridLayoutManager(getBaseContext(), 1);
                mFriendListRecyclerView.setLayoutManager(manager);

                mFriendListRecyclerView.setAdapter(mFriendListRecyclerViewAdapter);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };

        mRef.orderByChild("name").startAt(searchFriendName).endAt(searchFriendName + "\uf8ff").addChildEventListener(childEventListener);
    }

    public void addFriend(String userName) {
        DBFriend.add(userName);
        mRef.child(this.userName).setValue(new DBUser(this.userName, user.getAgoraUid(), localState, DBFriend));
    }

    public void onShowFriendListClick(View view) {
        if (isAddingFriend) {
            isAddingFriend = !isAddingFriend;
            mAddFriendLinearLayout.setVisibility(isAddingFriend ? View.VISIBLE : View.GONE);
        }

        isShowingFriend = !isShowingFriend;
        mShowFriendLinearLayout.setVisibility(isShowingFriend ? View.VISIBLE : View.GONE);

        mShowFriendListRecyclerViewAdapter = new ShowFriendListRecyclerViewAdapter(DBFriend);
        mShowFriendListRecyclerViewAdapter.setOnItemClickListener(new ShowFriendListRecyclerViewAdapter.ClickListener() {
            @Override
            public void onItemClick(final int position, View v) {

                if (v.getId() == R.id.btn_join_friend) {
                    joinFriendChildEventListener = new ChildEventListener() {
                        @Override
                        public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                            DBUser result = dataSnapshot.getValue(DBUser.class);
                            if (result.getState().equals(Constant.USER_STATE_OPEN)) {
                                joinFriend(DBFriend.get(position));
                                mShowFriendLinearLayout.setVisibility(View.GONE);
                            }else {
                                showToast(DBFriend.get(position) + "'s room is locked. You can message him to say hi!");
                            }

                            mRef.orderByChild("name").startAt(DBFriend.get(position)).endAt(DBFriend.get(position) + "\uf8ff").removeEventListener(joinFriendChildEventListener);
                        }

                        @Override
                        public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                        }

                        @Override
                        public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

                        }

                        @Override
                        public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    };
                    mRef.orderByChild("name").startAt(DBFriend.get(position)).endAt(DBFriend.get(position) + "\uf8ff").addChildEventListener(joinFriendChildEventListener);

                }else if (v.getId() == R.id.btn_chat_friend){
                    startMessaging(DBFriend.get(position));
                }
            }
        });
        RecyclerView.LayoutManager manager = new LinearLayoutManager(this);
        mShowFriendListRecyclerView.setLayoutManager(manager);
        mShowFriendListRecyclerView.setAdapter(mShowFriendListRecyclerViewAdapter);
    }

    public void joinFriend(String friendName){
        channelName = friendName;
        finishCalling();
        startCalling();
        //set the user's room state to private
        localState = Constant.USER_STATE_LOCK;
        mRef.child(userName).setValue(new DBUser(userName, user.getAgoraUid(), localState, DBFriend));
        isLocalCall = false;
    }

    public void onChatCloseClicked(View view) {
        mChatLayout.setVisibility(View.GONE);
        mSwitchCameraBtn.setVisibility(View.VISIBLE);
        mCallBtn.setVisibility(View.VISIBLE);
        mMuteBtn.setVisibility(View.VISIBLE);
    }

    public void onClickSend(View v) {
        //send chat messages
        String msg = mMsgEditText.getText().toString();
        if (!msg.equals("")) {
            MessageBean messageBean = new MessageBean(this.userName, msg, true);
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
