package me.nathanp.bubbledrop;

import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

public class BubblesActivity extends AppCompatActivity {
    private static final String TAG = "BubblesActivity";

    //Firebase things
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    StorageReference mStorageRef;

    //Layout stuff
    CoordinatorLayout mLayout;
    RecyclerView recyclerView;

    // Bubbles
    ArrayList<Bubble> mBubbles = new ArrayList<>();
    BubbleAdapter mBubbleAdapter = new BubbleAdapter(this, mBubbles, new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            expandBubbleItem(v);
        }
    });

    PopupWindow mBubblePopup;
    MediaPlayer mPlayer;

    FirebaseUser mUser = FirebaseAuth.getInstance().getCurrentUser();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mStorageRef = FirebaseStorage.getInstance().getReference();

        setContentView(R.layout.activity_bubbles);
        recyclerView = findViewById(R.id.bubbles_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(mBubbleAdapter);

        Toolbar toolbar = findViewById(R.id.bubbles_toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        setupQuery();
    }

    public void setupQuery() {
        DatabaseReference ref = database.getReference("users").child(mUser.getUid()).child("bubbles");
        ref.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                DatabaseReference bubbleRef = database.getReference("bubbles").child(dataSnapshot.getKey());
                bubbleRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        mBubbles.add(dataSnapshot.getValue(Bubble.class));
                        mBubbleAdapter.notifyItemInserted(mBubbles.size() - 1);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void expandBubbleItem(View bubbleView) {
        Log.d(TAG, "expandBubbleItem");
        String key = (String) bubbleView.getTag();
        Log.d(TAG, "expandBubbleItem key: " + key);
        if (key != null) {
            DatabaseReference bubbleRef = database.getReference(getString(R.string.bubbles)).child(key);
            showBubblePopup(bubbleRef);
        }
    }

    public void showBubblePopup(final DatabaseReference bubbleRef) {
        Log.d(TAG, "showBubblePopup");
        // Inflate the popup_layout.xml
        LinearLayout mapLayout = findViewById(R.id.map_layout);
        View popupView = getLayoutInflater().inflate(R.layout.popup_bubble, mapLayout);

        // Creating the PopupWindow
        mBubblePopup = new PopupWindow(popupView, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, true);

        mBubblePopup.showAtLocation(popupView, Gravity.CENTER|Gravity.TOP, 0,0);

        // Getting references to buttons, hook up popup UI.
        final TextView title = popupView.findViewById(R.id.title);
        final ConstraintLayout bubbleInfo = popupView.findViewById(R.id.bubble_data);
        final ProgressBar loading = popupView.findViewById(R.id.loading);
        final TextView info = popupView.findViewById(R.id.info);
        final ImageButton playButton = popupView.findViewById(R.id.playpause_btn);
        final ImageView imageView = popupView.findViewById(R.id.image);
        ImageButton close = popupView.findViewById(R.id.close);
        ImageButton report = popupView.findViewById(R.id.report);
        report.setVisibility(View.INVISIBLE);
        ImageButton delete = popupView.findViewById(R.id.delete);
        delete.setVisibility(View.VISIBLE);


        title.setText(R.string.bubble_loading);
        bubbleInfo.setVisibility(View.INVISIBLE);
        loading.setVisibility(View.VISIBLE);

        bubbleInfo.setTag(bubbleRef.getKey());

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBubblePopup.dismiss();
            }
        });

        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String key = (String) bubbleInfo.getTag();
                Log.d(TAG, "onClick: key: " + key);
                if (key != null) {
                    DatabaseReference databaseRef = database.getReference().getRoot();
                    databaseRef.child("bubbles").child(key).removeValue();
                    databaseRef.child("geofire").child("bubbles").child(key).removeValue();
                    databaseRef.child("users").child(mUser.getUid()).child("bubbles").child(key).removeValue();
                    // Restarting activity to reflect database changes
                    Intent intent = getIntent();
                    finish();
                    startActivity(intent);
                }
                mBubblePopup.dismiss();
            }
        });

        final ValueEventListener bubbleDataListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Bubble bubble = dataSnapshot.getValue(Bubble.class);

                title.setText(R.string.bubble_loaded);
                if (bubble != null) {
                    imageView.setVisibility(View.INVISIBLE);
                    playButton.setVisibility(View.INVISIBLE);
                    info.setVisibility(View.INVISIBLE);
                    if (bubble.bubbleType == Bubble.AUDIO_BUBBLE) {
                        StorageReference audioFileRef = mStorageRef.child(bubbleRef.getKey() + getString(R.string.audio_file_format));
                        long ONE_MB = 1024*1024;
                        audioFileRef.getBytes(ONE_MB).addOnSuccessListener( new OnSuccessListener<byte[]>() {
                            @Override
                            public void onSuccess(byte[] bytes) {
//                                Log.e(TAG, Arrays.toString(bytes));
                                File tempFile = saveTempFile(bytes);
                                if (tempFile != null) {
                                    if (startPlayback(tempFile)) {
                                        playButton.setImageDrawable(getDrawable(R.drawable.ic_pause_circle));
                                    }
                                }
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {}
                        });
                        playButton.setVisibility(View.VISIBLE);
                        playButton.setOnClickListener(new View.OnClickListener() {
                            boolean playing = false;
                            @Override
                            public void onClick(View v) {
                                if (!playing) {
                                    playButton.setImageDrawable(getDrawable(R.drawable.ic_pause_circle));
                                } else {
                                    stopPlayback();
                                    playButton.setImageDrawable(getDrawable(R.drawable.ic_play_circle));
                                }
                                playing = !playing;
                            }
                        });
                    } else if (bubble.bubbleType == Bubble.PICTURE_BUBBLE) {
                        StorageReference audioFileRef = mStorageRef.child(bubbleRef.getKey() + ".jpg");
                        long ONE_MB = 1024*1024;
                        audioFileRef.getBytes(ONE_MB).addOnSuccessListener( new OnSuccessListener<byte[]>() {
                            @Override
                            public void onSuccess(byte[] bytes) {
//                                Log.e(TAG, Arrays.toString(bytes));
                                File tempFile = saveTempFile(bytes);
                                if (tempFile != null) {
                                    imageView.setImageURI(Uri.fromFile(tempFile));
                                }
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {}
                        });
                        imageView.setVisibility(View.VISIBLE);
                    } else if (bubble.bubbleType == Bubble.TEXT_BUBBLE) {
                        info.setText(bubble.filenameOrText);
                        info.setVisibility(View.VISIBLE);
                    } else {
                        Log.d(TAG, "This shouldn't happen...");
                    }
                } else {
                    info.setText(R.string.bad_bubble);
                }
                loading.setVisibility(View.GONE);
                bubbleInfo.setVisibility(View.VISIBLE);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        };

        bubbleRef.addListenerForSingleValueEvent(bubbleDataListener);

        mBubblePopup.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                if (mPlayer != null && mPlayer.isPlaying()) {
                    mPlayer.stop();
                    mPlayer.release();
                    mPlayer = null;
                }
                bubbleRef.removeEventListener(bubbleDataListener);
            }
        });
    }

    File saveTempFile(byte[] audio) {
        try {
            File tempFile = File.createTempFile(getString(R.string.saved_filename), null, getCacheDir());
            FileOutputStream fos = new FileOutputStream(tempFile);
            fos.write(audio);
            fos.close();
            return tempFile;
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }

    boolean startPlayback(File tempFile) {
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(tempFile.getPath());
            mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mPlayer.setLooping(true);
            mPlayer.prepare();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            return false;
        }
        mPlayer.start();
        return true;
    }

    void stopPlayback() {
        if (mPlayer != null) {
            mPlayer.stop();
            mPlayer = null;
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}


