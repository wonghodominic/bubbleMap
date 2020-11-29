package me.nathanp.bubbledrop;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static me.nathanp.bubbledrop.Util.getBitmapFromVectorDrawable;

public class MainActivity extends AppCompatActivity implements
        ActivityCompat.OnRequestPermissionsResultCallback,
        NavigationView.OnNavigationItemSelectedListener,
        OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;
    private static final int RECORDING_ACTIVITY_RESULT = 1;
    private static final int IMAGE_ACTIVITY_RESULT = 2;
    private static final int TEXT_ACTIVITY_RESULT = 3;


    private static final float MIN_DISPLACEMENT_DISTANCE = 5; //distance in meters before firing another location update
    private static final double QUERY_RADIUS = 0.5; //search radius in kilometers
    private static final float MIN_MAP_ZOOM = 13;
    private static final float MAX_MAP_ZOOM = 25;

    // Fragment manager
    FragmentManager mFragmentManager;

    // Audio Player
    MediaPlayer mPlayer;
    File mCurrentAudioFile;

    // Firebase stuff
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    GeoFire mGeoFire;
    GeoQuery mGeoQuery;
    StorageReference mStorageRef;
    FirebaseAuth mAuth;
    FirebaseUser mUser;

    // Map stuff
    private GoogleMap mMap;
    private Map<String, Marker> mMarkers = new HashMap<>();

    //Location stuff
    LocationRequest mLocationRequest;
    FusedLocationProviderClient mLocationProviderClient;
    Location mCurrentLocation;

    //Layout stuff
    CoordinatorLayout mLayout;
    FloatingActionButton mFabRecord;
    FloatingActionButton mFabText;
    FloatingActionButton mFabImage;
    boolean mFabsVisible;
    String mUserEmail;
    TextView mDisplayEmail;
    NavigationView navigationView;
    View header;

    PopupWindow mBubblePopup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(getString(R.string.app_name));

        mGeoFire = new GeoFire(database.getReference(getString(R.string.geofire_bubbles)));
        mStorageRef = FirebaseStorage.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        mAuth.addAuthStateListener(new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if (firebaseAuth.getCurrentUser() == null) {
                    gotoLoginActivity();
                } else {
                    mUser = firebaseAuth.getCurrentUser();
                    onUserReady();
                }
            }
        });

        mLayout = findViewById(R.id.main_coordinator_layout);
        mFabImage = findViewById(R.id.fab_image);
        mFabRecord = findViewById(R.id.fab_record);
        mFabText = findViewById(R.id.fab_text);
        navigationView = findViewById(R.id.nav_view);
        header = navigationView.getHeaderView(0);
        mDisplayEmail = header.findViewById(R.id.email);

        mFabImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent textIntent = new Intent(getBaseContext(), CameraActivity.class);
                startActivityForResult(textIntent, IMAGE_ACTIVITY_RESULT);
            }
        });

        mFabRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent audioIntent = new Intent(getBaseContext(), RecordActivity.class);
                startActivityForResult(audioIntent, RECORDING_ACTIVITY_RESULT);
            }
        });

        mFabText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent textIntent = new Intent(getBaseContext(), CreateTextBubbleActivity.class);
                startActivityForResult(textIntent, TEXT_ACTIVITY_RESULT);
            }
        });

        FloatingActionButton fab = findViewById(R.id.fab_plus);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFabs();
            }
        });

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10 * 1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
//        mLocationRequest.setSmallestDisplacement(MIN_DISPLACEMENT_DISTANCE);
        mLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        mFragmentManager = getSupportFragmentManager();

        SupportMapFragment mapFragment = (SupportMapFragment) mFragmentManager.findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    void gotoLoginActivity() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMinZoomPreference(MIN_MAP_ZOOM);
        mMap.setMaxZoomPreference(MAX_MAP_ZOOM);
        mMap.getUiSettings().setZoomControlsEnabled(false);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.getUiSettings().setIndoorLevelPickerEnabled(false);
        mMap.getUiSettings().setAllGesturesEnabled(false);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.setOnMarkerClickListener(this);

        if (checkLocationPermission()) {
            addLocationListeners();
        } else {
            requestLocationPermissions();
        }
    }

    void onUserReady() {
        // TODO: setup user specific views
        mUserEmail = mUser.getEmail();
        if (mDisplayEmail == null) {
            Log.e("Null", "Null");
        } else {
            Log.e("Not Null", "Not Null");
        }
        mDisplayEmail.setText(mUserEmail);
    }

    public void addLocationListeners() {
        try {
            mMap.setMyLocationEnabled(true);
            mLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) { // Got last known location. In some rare situations this can be null.
                        Log.d(TAG, "Got users initial location.");
                        setupGeoQuery(location);
                        updateLocation(location);
                    }
                }
            });
            mLocationProviderClient.requestLocationUpdates(mLocationRequest, new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult result) {
                    if (result == null) {
                        return;
                    }
                    updateLocation(result.getLastLocation());
                }
            }, null);


        } catch (SecurityException e) {
            Log.d(TAG, e.getMessage());

        }
    }

    public void setupGeoQuery(Location startingLocation) {
        GeoLocation geo = new GeoLocation(startingLocation.getLatitude(), startingLocation.getLongitude());
        mGeoQuery = mGeoFire.queryAtLocation(geo, 0.05);
        mGeoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(final String key, GeoLocation location) {
                final LatLng bubbleLoc = new LatLng(location.latitude, location.longitude);
                database.getReference(getString(R.string.bubbles)).child(key).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Bubble bubble = dataSnapshot.getValue(Bubble.class);
                        Bitmap bitmap = null;
                        if (bubble != null) {
                            if (bubble.bubbleType == Bubble.TEXT_BUBBLE) {
                                Log.e(TAG, "this is a text bubble");
                                bitmap = Util.getBitmapFromVectorDrawable(getApplicationContext(), R.drawable.ic_text_bubble, 2);
                            } else if (bubble.bubbleType == Bubble.PICTURE_BUBBLE) {
                                bitmap = Util.getBitmapFromVectorDrawable(getApplicationContext(), R.drawable.ic_image_bubble, 2);
                            } else if (bubble.bubbleType == Bubble.AUDIO_BUBBLE) {
                                bitmap = Util.getBitmapFromVectorDrawable(getApplicationContext(), R.drawable.ic_audio_bubble, 2);
                            }
//                        if (bitmap == null) {
//                            bitmap = Util.getBitmapFromVectorDrawable(getApplicationContext(), R.drawable.ic_audio_bubble, 2);
//                        }
                            Marker marker = mMap.addMarker(new MarkerOptions()
                                    .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                                    .anchor(0, 1)
                                    .position(bubbleLoc));
                            marker.setTag(key);
                            mMarkers.put(key, marker);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onKeyExited(String key) {
                mMarkers.get(key).remove();
                mMarkers.remove(key);
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
                LatLng bubbleLoc = new LatLng(location.latitude, location.longitude);
                mMarkers.get(key).setPosition(bubbleLoc);
            }

            @Override
            public void onGeoQueryReady() {}

            @Override
            public void onGeoQueryError(DatabaseError error) {}
        });
    }

    public boolean checkLocationPermission() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public void requestLocationPermissions() {
        requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_FINE_LOCATION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_FINE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                addLocationListeners();
            } else {
                Snackbar.make(mLayout, R.string.location_permission_rationale, Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.allow_permission, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                requestLocationPermissions();
                            }
                        }).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public void updateLocation(Location location) {
        Log.d(TAG, "Location updated...");
        mCurrentLocation = location;
        LatLng myLoc = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.setLatLngBoundsForCameraTarget(new LatLngBounds(myLoc, myLoc));
        mMap.animateCamera(CameraUpdateFactory.newLatLng(myLoc));
        mGeoQuery.setLocation(new GeoLocation(location.getLatitude(), location.getLongitude()), QUERY_RADIUS);
    }

    public void toggleFabs() {
        if (mFabsVisible) {
            mFabText.hide();
            mFabRecord.hide();
            mFabImage.hide();
        } else {
            mFabText.show();
            mFabRecord.show();
            mFabImage.show();
        }
        mFabsVisible = !mFabsVisible;
    }

    public void dropBubble(int type, String text) {
        GeoLocation bubbleLoc = new GeoLocation(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
        DatabaseReference bubbleRef = database.getReference(getString(R.string.bubbles)).push();
        Log.e(TAG, mUser.getUid());
        DatabaseReference userBubbleRef = database.getReference("users").child(mUser.getUid()).child("bubbles").child(bubbleRef.getKey());
        Bubble bubble = new Bubble(type, text, new Date(), bubbleRef.getKey());
        userBubbleRef.setValue(bubbleRef.getKey());
        bubbleRef.setValue(bubble);
        mGeoFire.setLocation(bubbleRef.getKey(), bubbleLoc, new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {
                Snackbar.make(mLayout, R.string.bubble_dropped, Snackbar.LENGTH_SHORT).show();
            }
        });
        if (type == Bubble.AUDIO_BUBBLE) {
            uploadBubble(text, bubbleRef.getKey(), getString(R.string.audio_file_format));
        } else if (type == Bubble.PICTURE_BUBBLE){
            uploadBubble(text, bubbleRef.getKey(), ".jpg");
        }
    }

    void uploadBubble(String filePathStr, String bubbleId, String format) {
        File filePath = new File(filePathStr);
        Log.d(TAG, filePathStr);
        Uri file = Uri.fromFile(filePath);
        StorageReference fileRef = mStorageRef.child(bubbleId + format);
        fileRef.putFile(file).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Snackbar.make(mLayout, R.string.upload_file_success, Snackbar.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Snackbar.make(mLayout, R.string.upload_file_failure, Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void showBubblePopup(final DatabaseReference bubbleRef, final Marker marker) {
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

        title.setText(R.string.bubble_loading);
        bubbleInfo.setVisibility(View.INVISIBLE);
        loading.setVisibility(View.VISIBLE);

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBubblePopup.dismiss();
            }
        });
        report.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBubblePopup.dismiss();
                marker.remove();
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
                        StorageReference imageFileRef = mStorageRef.child(bubbleRef.getKey() + ".jpg");
                        long ONE_MB = 1024*1024;
                        imageFileRef.getBytes(ONE_MB).addOnSuccessListener( new OnSuccessListener<byte[]>() {
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
            Log.e(TAG, Arrays.toString(audio));
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_logout) {
            mAuth.signOut();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        Log.d(TAG, "Tapped: " + id);

        // Handle navigation view item clicks here.
        switch (item.getItemId()) {

            case R.id.nav_item_bubbles: {
                Intent intent = new Intent(MainActivity.this, BubblesActivity.class);
                startActivity(intent);
                break;
            } case R.id.nav_item_logout: {
                mAuth.signOut();
                break;
            }
        }
        //close navigation drawer
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RECORDING_ACTIVITY_RESULT) {
            if (resultCode == RESULT_OK) {
                dropBubble(Bubble.AUDIO_BUBBLE, data.getStringExtra("recordingFilename"));
            } else if (resultCode == RESULT_CANCELED) {
                Log.d(TAG, "RESULT CANCELED");
                // Oops! User has canceled the recording
            }
        } else if (requestCode == TEXT_ACTIVITY_RESULT) {
            if (resultCode == RESULT_OK) {
                dropBubble(Bubble.TEXT_BUBBLE, data.getStringExtra("bubbleText"));
            } else if (resultCode == RESULT_CANCELED) {
                Log.d(TAG, "RESULT CANCELED");
                // Oops! User has canceled the text bubble
            }
        } else if (requestCode == IMAGE_ACTIVITY_RESULT) {
            if (resultCode == RESULT_OK) {
                dropBubble(Bubble.PICTURE_BUBBLE, data.getStringExtra("imageFile"));
            } else if (resultCode == RESULT_CANCELED) {
                Log.d(TAG, "RESULT CANCELED");
                // Oops! User has canceled the camera capture
            }
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        String key = (String) marker.getTag();
        if (key != null) {
            DatabaseReference bubbleRef = database.getReference(getString(R.string.bubbles)).child(key);
            showBubblePopup(bubbleRef, marker);
            return true;
        }
        return false;
    }
}
