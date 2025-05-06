package com.example.timekeeping;

import static android.graphics.Color.GREEN;
import com.example.timekeeping.R;

import android.Manifest;
import androidx.appcompat.app.AlertDialog;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.DatePickerDialog;
import android.widget.DatePicker;
import java.util.Calendar;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class MainActivityCPP extends CameraActivity {

    private static final String TAG = "MainActivityCPP";
    private static final Object NATIVE_LOCK = new Object();
    private CameraBridgeViewBase mOpenCvCameraView;
    private Button galleryBtn;
    private Button showChecksBtn;
    private Button showFacesBtn;
    private Button resetRecognitionBtn;
    private Button signInBtn;
    private Button signOutBtn;
    private Button showMonthlyStatsBtn;
    private Button showCheckedListBtn;
    private ImageView imageView;
    private Button showAllCompanyStatsBtn;

    private static final int GALLERY_REQUEST_CODE = 1001;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 2001;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 2003;

    private final List<FaceData> faceDataList = new ArrayList<>();
    private DatabaseReference faceDataRef;
    private final Map<String, CheckData> checkDataMap = new HashMap<>();
    private DatabaseReference checkDataRef;
    private DatabaseReference attendanceRef;
    private final SimpleDateFormat dayFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    private FusedLocationProviderClient fusedLocationClient;
    private volatile Location currentLocation;
    private LocationCallback locationCallback;

    private FirebaseAuth mAuth;
    private String currentUserId = null;

    private AtomicBoolean isDialogVisible = new AtomicBoolean(false);
    private Set<String> canceledFaces = ConcurrentHashMap.newKeySet();
    private Map<String, Boolean> canceledMap = new HashMap<>();
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    private float recognitionThreshold = 0.5f;

    private static final String PREFS_NAME = "MyPrefs";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_REMEMBER_ME = "remember_me";

    private String managerCompanyName = null;

    String currentName = "Unknown";
    String currentId = null;

    private Map<String, List<Vote>> faceVotes = new HashMap<>();
    private Map<String, String> lastAnnotations = new HashMap<>();
    private Set<String> processedFaces = new HashSet<>();
    private Map<String, Long> voteStartTime = new HashMap<>();
    private static final int VOTE_THRESHOLD = 1;
    private static final long TIMEOUT = 5000;
    private int pendingExportMonth = -1;
    private int pendingExportYear = -1;

    private final AtomicBoolean autoDialogEnabled = new AtomicBoolean(true);

    private static class Vote {
        String name;
        String userId;
        Vote(String name, String userId) {
            this.name = name;
            this.userId = userId;
        }
    }
    private String lastRecognizedName = null;

    static {
        if (!OpenCVLoader.initLocal()) {
            Log.e(TAG, "Cannot connect to OpenCV Manager");
        } else {
            System.loadLibrary("app");
        }
    }

    public native void InitFaceDetector(String modelPath);
    public native int DetectFaces(long matAddrGray, long matAddrRgba, float[] largestFaceRect);
    public native void InitFaceRecognition(String modelPath);
    public native float[] ExtractFaceEmbedding(long matAddr);
    public native float CalculateSimilarity(float[] emb1, float[] emb2);

    @Override
    // =============== Lifecycle callback for create ===============
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        } catch (DatabaseException e) {  }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main_cpp);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_FULLSCREEN);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) currentUserId = currentUser.getUid();

        mOpenCvCameraView = findViewById(R.id.opencv_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.GONE);
        mOpenCvCameraView.setCameraIndex(0);
        mOpenCvCameraView.setCvCameraViewListener(cvCameraViewListener2);
        mOpenCvCameraView.setMaxFrameSize(640, 600);

        imageView = findViewById(R.id.imageView);
        galleryBtn = findViewById(R.id.galleryBtn);
        showFacesBtn = findViewById(R.id.showFacesBtn);
        resetRecognitionBtn = findViewById(R.id.resetRecognitionBtn);
        showChecksBtn = findViewById(R.id.showCheckedListBtn);
        signOutBtn = findViewById(R.id.signOutBtn);
        signInBtn = findViewById(R.id.signInBtn);
        showMonthlyStatsBtn = findViewById(R.id.showMonthlyStatsBtn);
        showAllCompanyStatsBtn = findViewById(R.id.showAllCompanyStatsBtn);
        showAllCompanyStatsBtn.setOnClickListener(v -> showAllCompanyMonthlyStatsModal());

        galleryBtn.setVisibility(View.GONE);
        showFacesBtn.setVisibility(View.GONE);
        showMonthlyStatsBtn.setOnClickListener(v -> showMonthlyAttendanceModal());
        galleryBtn.setOnClickListener(v -> openGallery());
        showFacesBtn.setOnClickListener(v -> displayRegisteredFaces());
        resetRecognitionBtn.setOnClickListener(v -> resetCanceledFaces());
        showChecksBtn.setOnClickListener(v -> showAttendanceModal());
        signOutBtn.setOnClickListener(v -> handleSignOut());
        signInBtn.setOnClickListener(v -> handleSignIn());

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationCallback = new LocationCallback() {
            @Override public void onLocationResult(LocationResult r) {
                if (r == null) return;
                for (Location l : r.getLocations()) currentLocation = l;
            }
        };

        showChecksBtn.setEnabled(true);

        loadRecognitionThreshold();
        requestPermissions();

        faceDataRef = FirebaseDatabase.getInstance().getReference("faceDataList");
        checkDataRef = FirebaseDatabase.getInstance().getReference("checkDataList");
        attendanceRef = FirebaseDatabase.getInstance().getReference("attendanceRecords");

        loadFaceDataList();
        loadCheckDataList();
        checkUserRoleAndAdjustUI();

        requestStoragePermission();
    }

    @Override
    // =============== Lifecycle callback for resume ===============
    protected void onResume() {
        super.onResume();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.enableView();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
            getLastKnownLocation();
        }
    }

    @Override
    // =============== Lifecycle callback for pause ===============
    protected void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    // =============== Lifecycle callback for destroy ===============
    protected void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        executorService.shutdown();
    }

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mOpenCvCameraView);
    }

    private CameraBridgeViewBase.CvCameraViewListener2 cvCameraViewListener2 =
            new CameraBridgeViewBase.CvCameraViewListener2() {
                @Override
                // =============== Lifecycle callback for camera view started ===============
                public void onCameraViewStarted(int width, int height) {
                    Log.d(TAG, "Camera view started with width: " + width + " and height: " + height);
                }
                @Override
                // =============== Lifecycle callback for camera view stopped ===============
                public void onCameraViewStopped() {
                    Log.d(TAG, "Camera view stopped.");
                }
                @Override
                // =============== Lifecycle callback for camera frame ===============
                public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
                    long startTime = System.nanoTime();
                    Mat inputRgba = inputFrame.rgba();
                    Mat inputGray = inputFrame.gray();
                    int MAX_FACES = 20;
                    float[] faceRects = new float[4 * MAX_FACES];
                    int numFaces = DetectFaces(inputGray.getNativeObjAddr(), inputRgba.getNativeObjAddr(), faceRects);
                    Log.d(TAG, "Detected " + numFaces + " face(s).");

                    Set<String> currentFrameKeys = new HashSet<>();
                    List<FaceData> snapshot;
                    synchronized (faceDataList) {
                        snapshot = new ArrayList<>(faceDataList);
                    }

                    for (int i = 0; i < numFaces; i++) {
                        float x = faceRects[i * 4];
                        float y = faceRects[i * 4 + 1];
                        float w = faceRects[i * 4 + 2];
                        float h = faceRects[i * 4 + 3];

                        String voteKey = getVoteKey(x, y, w, h);
                        currentFrameKeys.add(voteKey);

                        if (!voteStartTime.containsKey(voteKey)) {
                            voteStartTime.put(voteKey, System.currentTimeMillis());
                        }
                        if (!canceledMap.containsKey(voteKey)) {
                            canceledMap.put(voteKey, false);
                        }

                        Scalar boxColor = new Scalar(0, 205, 0);
                        int boxThickness = 3;
                        Imgproc.rectangle(inputRgba, new Point(x, y), new Point(x + w, y + h), boxColor, boxThickness);

                        float[] cameraFrameEmbedding;
                        synchronized (NATIVE_LOCK) {
                            cameraFrameEmbedding = ExtractFaceEmbedding(inputRgba.getNativeObjAddr());
                        }
                        String currentVoteName = "Unknown";
                        String currentVoteUserId = null;
                        float highestSimilarity = 0.0f;

                        if (cameraFrameEmbedding != null && !snapshot.isEmpty()) {
                            for (FaceData faceData : snapshot) {
                                if (faceData.getEmbeddings() == null || faceData.getEmbeddings().isEmpty())
                                    continue;
                                for (List<Float> embeddingList : faceData.getEmbeddings()) {
                                    float[] embeddingArray = convertFloatListToArray(embeddingList);
                                    float similarity = CalculateSimilarity(embeddingArray, cameraFrameEmbedding);
                                    if (similarity > recognitionThreshold && similarity > highestSimilarity) {
                                        highestSimilarity = similarity;
                                        currentVoteName = faceData.getName();
                                        currentVoteUserId = faceData.getUserId();
                                    }
                                }
                            }
                        }

                        List<Vote> votesList = faceVotes.containsKey(voteKey) ? faceVotes.get(voteKey) : new ArrayList<>();
                        votesList.add(new Vote(currentVoteName, currentVoteUserId));
                        faceVotes.put(voteKey, votesList);

                        String majorityName;
                        long elapsed = System.currentTimeMillis() - voteStartTime.get(voteKey);

                        if (votesList.size() < VOTE_THRESHOLD) {
                            if (canceledMap.get(voteKey)) {
                                majorityName = lastAnnotations.containsKey(voteKey) ? lastAnnotations.get(voteKey) : "Unknown";
                            } else {
                                if (elapsed < TIMEOUT) {
                                    majorityName = "Processing...";
                                } else {
                                    majorityName = "Unknown";
                                }
                            }
                        } else {
                            Vote majorityVote = getMajorityVote(votesList);
                            majorityName = majorityVote.name;
                            currentVoteUserId = majorityVote.userId;
                            if (canceledMap.get(voteKey)) {
                                majorityName = lastAnnotations.containsKey(voteKey) ? lastAnnotations.get(voteKey) : "Unknown";
                            } else {
                                if (!"Unknown".equalsIgnoreCase(majorityName)) {
                                    lastAnnotations.put(voteKey, majorityName);
                                }
                                if (!processedFaces.contains(voteKey)) {
                                    currentName = majorityName;
                                    currentId = currentVoteUserId;
                                    handleRecognizedFace(majorityName, currentVoteUserId, voteKey);
                                    processedFaces.add(voteKey);
                                }
                            }
                        }
                        drawNameLabel(inputRgba, x, y, majorityName, "Unknown".equalsIgnoreCase(majorityName));
                    }

                    faceVotes.keySet().retainAll(currentFrameKeys);
                    processedFaces.retainAll(currentFrameKeys);
                    lastAnnotations.keySet().retainAll(currentFrameKeys);
                    voteStartTime.keySet().retainAll(currentFrameKeys);
                    canceledMap.keySet().retainAll(currentFrameKeys);

                    long endTime = System.nanoTime();
                    long processingTimeMs = (endTime - startTime) / 1_000_000;
                    Log.d(TAG, "Frame processed in " + processingTimeMs + " ms");

                    return inputRgba;
                }
            };

    // =============== Core logic ===============
    private float[] convertFloatListToArray(List<Float> embeddingList) {
        float[] embeddingArray = new float[embeddingList.size()];
        for (int i = 0; i < embeddingList.size(); i++) {
            embeddingArray[i] = embeddingList.get(i);
        }
        return embeddingArray;
    }

    // =============== Gets vote key ===============
    private String getVoteKey(float x, float y, float w, float h) {
        int rx = Math.round(x / 20);
        int ry = Math.round(y / 20);
        int rw = Math.round(w / 20);
        int rh = Math.round(h / 20);
        return rx + "_" + ry + "_" + rw + "_" + rh;
    }

    // =============== Gets majority vote ===============
    private Vote getMajorityVote(List<Vote> votes) {
        Map<String, Integer> countMap = new HashMap<>();
        Map<String, String> userIdMap = new HashMap<>();
        for (Vote vote : votes) {
            String name = vote.name;
            countMap.put(name, countMap.getOrDefault(name, 0) + 1);
            if (vote.userId != null) {
                userIdMap.put(name, vote.userId);
            }
        }
        String majorityName = "Unknown";
        int maxCount = 0;
        for (Map.Entry<String, Integer> entry : countMap.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                majorityName = entry.getKey();
            }
        }
        return new Vote(majorityName, userIdMap.get(majorityName));
    }

    // =============== Handles recognized face ===============
    private void handleRecognizedFace(String matchedName, String matchedUserId, String voteKey) {
        if (canceledMap.get(voteKey)) return;
        if ("Unknown".equalsIgnoreCase(matchedName)) return;
        lastRecognizedName = matchedName;
        if (!autoDialogEnabled.get()) return;

        if (matchedUserId != null && !matchedUserId.equals(currentUserId)) {
            currentUserId = matchedUserId;
        }

        if (isDialogVisible.compareAndSet(false, true)) {
            runOnUiThread(() -> showRecognitionDialog(matchedName, voteKey));
        }
    }

    // =============== Draws name label ===============
    private void drawNameLabel(Mat frame, float x, float y, String name, boolean isUnknown) {
        int fontFace = Imgproc.FONT_HERSHEY_SIMPLEX;
        double fontScale = 0.6;
        int thickness = 2;
        int padding = 4;

        Scalar bgColor = isUnknown ? new Scalar(250, 0, 0) : new Scalar(0, 205, 0);
        Scalar textColor = new Scalar(255, 255, 255);

        int[] baseline = new int[1];
        Size textSize = Imgproc.getTextSize(name, fontFace, fontScale, thickness, baseline);

        int textWidth = (int) textSize.width;
        int textHeight = (int) textSize.height;
        int bgX1 = (int) x;
        int bgY2 = (int) y - padding;
        int bgX2 = bgX1 + textWidth + padding * 2;
        int bgY1 = bgY2 - textHeight - padding;

        if (bgX2 > frame.cols()) {
            bgX2 = frame.cols() - 10;
            bgX1 = bgX2 - textWidth - padding * 2;
        }
        if (bgY1 < 0) {
            bgY1 = 10;
            bgY2 = bgY1 + textHeight + padding;
        }

        Imgproc.rectangle(frame, new Point(bgX1, bgY1), new Point(bgX2, bgY2), bgColor, Imgproc.FILLED);
        Imgproc.putText(frame, name, new Point(bgX1 + padding, bgY2 - padding + 1), fontFace, fontScale, textColor, thickness);
    }

    // =============== Displays recognition dialog ===============
    private void showRecognitionDialog(String recognizedName, String voteKey) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivityCPP.this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_recognition, null);
        builder.setView(dialogView);

        TextView nameTextView = dialogView.findViewById(R.id.recognizedNameTextView);
        TextView locationTextView = dialogView.findViewById(R.id.locationTextView);
        TextView timeTextView = dialogView.findViewById(R.id.timeTextView);

        nameTextView.setText("Phát hiện: " + recognizedName);
        locationTextView.setText(getLocationText());
        updateTimeTextView(timeTextView);

        Handler timeHandler = new Handler();
        Runnable timeUpdater = new Runnable() {
            @Override
            // =============== Core logic ===============
            public void run() {
                updateTimeTextView(timeTextView);
                timeHandler.postDelayed(this, 1000);
            }
        };
        timeHandler.post(timeUpdater);

        builder.setPositiveButton("Chấm công", (dialog, which) -> {
            autoDialogEnabled.set(false);
            canceledMap.put(voteKey, false);
            if (!handleCheckIn(recognizedName, locationTextView.getText().toString())) {
                isDialogVisible.set(false);
            }
            timeHandler.removeCallbacks(timeUpdater);
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> {
            autoDialogEnabled.set(false);
            canceledMap.put(voteKey, true);
            isDialogVisible.set(false);
            timeHandler.removeCallbacks(timeUpdater);
        });
        builder.setOnCancelListener(dialog -> {
            autoDialogEnabled.set(false);
            isDialogVisible.set(false);
            timeHandler.removeCallbacks(timeUpdater);
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // =============== Gets location text ===============
    private String getLocationText() {
        if (currentLocation != null) {
            return String.format(Locale.getDefault(),
                    "Vị trí: Lat %.5f, Lon %.5f",
                    currentLocation.getLatitude(),
                    currentLocation.getLongitude());
        }
        return "Vị trí: Không có thông tin";
    }

    // =============== Handles check in ===============
    private boolean handleCheckIn(String recognizedName, String locationText) {
        FaceData faceData = getFaceDataByName(recognizedName);
        if (faceData == null) {
            Toast.makeText(this, "Không tìm thấy người dùng.", Toast.LENGTH_LONG).show();
            return false;
        }

        currentUserId = faceData.getUserId();
        if (currentLocation == null) {
            Toast.makeText(this, "Chưa có GPS.", Toast.LENGTH_LONG).show();
            return false;
        }

        long now = System.currentTimeMillis();
        String day = dayFmt.format(new Date(now));
        DatabaseReference todayRef = attendanceRef.child(currentUserId).child(day);

        todayRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            // =============== Lifecycle callback for data change ===============
            public void onDataChange(@NonNull DataSnapshot snap) {
                AttendanceRecord rec;

                long workStartTime = getWorkStartTime(day, faceData.getWorkStartHour());
                long workEndTime = getWorkEndTime(day, faceData.getWorkEndHour());

                if (!snap.exists()) {

                    rec = new AttendanceRecord(currentUserId, day);
                    rec.setInTime(now);
                    rec.setInLocation(locationText);
                    rec.setCompanyName(faceData.getCompanyName());
                    if (now > workStartTime) {
                        rec.setLate(true);
                    }
                    todayRef.setValue(rec);
                    Toast.makeText(MainActivityCPP.this, "Chấm công vào thành công " + recognizedName, Toast.LENGTH_SHORT).show();
                } else {
                    rec = snap.getValue(AttendanceRecord.class);
                    if (rec != null && rec.getOutTime() == 0) {

                        rec.setOutTime(now);
                        rec.setOutLocation(locationText);
                        rec.setDurationMillis(now - rec.getInTime());
                        if (now < workEndTime) {
                            rec.setEarlyLeave(true);
                        }
                        todayRef.setValue(rec);
                        Toast.makeText(MainActivityCPP.this, "Chấm công về thành công cho " + recognizedName, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivityCPP.this, "Hôm nay đã chấm đủ 2 lần.", Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            // =============== Lifecycle callback for cancelled ===============
            public void onCancelled(@NonNull DatabaseError e) {
                Toast.makeText(MainActivityCPP.this, "Lỗi chấm công: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
        return true;
    }

    // =============== Updates time text view ===============
    private void updateTimeTextView(TextView timeTextView) {
        String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        timeTextView.setText("Thời gian: " + currentTime);
    }

    // =============== Core logic ===============
    private boolean isWithinRange(double currentLat, double currentLon, double officeLat, double officeLon, float rangeInMeters) {
        float[] results = new float[1];
        Location.distanceBetween(currentLat, currentLon, officeLat, officeLon, results);
        float distance = results[0];
        Log.d(TAG, "Distance between current location and office: " + distance + " meters.");
        return distance <= rangeInMeters;
    }

    // =============== Opens gallery ===============
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Chọn ảnh"), GALLERY_REQUEST_CODE);
        Log.d(TAG, "Opening gallery for image selection.");
    }

    @Override
    // =============== Lifecycle callback for activity result ===============
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            List<Uri> imageUris = extractSelectedImages(data);
            List<float[]> newEmbeddings = processSelectedImages(imageUris);
            if (!newEmbeddings.isEmpty()) {
                if (!checkAllEmbeddingsSimilar(newEmbeddings, recognitionThreshold)) {
                    showMultipleFacesDetectedDialog();
                    return;
                }
                promptForName(newEmbeddings);
            } else {
                Toast.makeText(MainActivityCPP.this, "Không nhận diện được khuôn mặt từ ảnh đã chọn.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e(TAG, "No image selected or action canceled");
        }
    }

    // =============== Core logic ===============
    private List<Uri> extractSelectedImages(Intent data) {
        List<Uri> imageUris = new ArrayList<>();
        if (data.getClipData() != null) {
            int count = data.getClipData().getItemCount();
            for (int i = 0; i < count; i++) {
                imageUris.add(data.getClipData().getItemAt(i).getUri());
            }
        } else if (data.getData() != null) {
            imageUris.add(data.getData());
        }
        return imageUris;
    }

    // =============== Processes selected images ===============
    private List<float[]> processSelectedImages(List<Uri> imageUris) {
        List<float[]> embeddings = new ArrayList<>();
        for (Uri imageUri : imageUris) {
            try {
                getContentResolver().takePersistableUriPermission(imageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                Mat imgMat = uriToMat(imageUri);
                float[] embedding = ExtractFaceEmbedding(imgMat.getNativeObjAddr());
                if (embedding != null) {
                    embeddings.add(embedding);
                } else {
                    Toast.makeText(MainActivityCPP.this, "Không phát hiện khuôn mặt từ ảnh đã chọn.", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                Log.e(TAG, "Error processing selected image: " + e.getMessage());
                Toast.makeText(MainActivityCPP.this, "Lỗi xử lý ảnh: " + e.getMessage(), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        }
        return embeddings;
    }

    // =============== Core logic ===============
    private Mat uriToMat(Uri uri) throws IOException {
        InputStream in = getContentResolver().openInputStream(uri);
        if (in == null) {
            throw new IOException("Unable to open input stream from URI");
        }
        Bitmap bitmap = BitmapFactory.decodeStream(in);
        in.close();
        if (bitmap == null) {
            throw new IOException("Unable to decode bitmap from URI");
        }
        Mat mat = new Mat();
        Bitmap bmp32 = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp32, mat);
        return mat;
    }

    // =============== Checks all embeddings similar ===============
    private boolean checkAllEmbeddingsSimilar(List<float[]> embeddings, float threshold) {
        for (int i = 0; i < embeddings.size(); i++) {
            for (int j = i + 1; j < embeddings.size(); j++) {
                float similarity = CalculateSimilarity(embeddings.get(i), embeddings.get(j));
                if (similarity < threshold) {
                    return false;
                }
            }
        }
        return true;
    }

    // =============== Displays multiple faces detected dialog ===============
    private void showMultipleFacesDetectedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivityCPP.this);
        builder.setTitle("Phát hiện nhiều khuôn mặt khác nhau");
        builder.setMessage("Các ảnh đã chọn dường như thuộc về những người khác nhau. Vui lòng chọn ảnh của cùng một người.");
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    // =============== Core logic ===============
    private void promptForName(List<float[]> embeddings) {
        Map<String, FaceData> matchingUsers = findMatchingUsers(embeddings, recognitionThreshold);
        if (!matchingUsers.isEmpty()) {
            showMatchingUsersDialog(matchingUsers, embeddings);
        } else {
            showNameInputDialog(embeddings);
        }
    }

    private Map<String, FaceData> findMatchingUsers(List<float[]> embeddings, float threshold) {
        Map<String, FaceData> matchingUsers = new HashMap<>();
        List<FaceData> snapshot;
        synchronized (faceDataList) {
            snapshot = new ArrayList<>(faceDataList);
        }
        for (float[] newEmbedding : embeddings) {
            for (FaceData faceData : snapshot) {
                if (faceData.getEmbeddings() == null || faceData.getEmbeddings().isEmpty()) {
                    continue;
                }
                for (List<Float> embeddingList : faceData.getEmbeddings()) {
                    float[] existingEmbedding = new float[embeddingList.size()];
                    for (int i = 0; i < embeddingList.size(); i++) {
                        existingEmbedding[i] = embeddingList.get(i);
                    }
                    float similarity = CalculateSimilarity(existingEmbedding, newEmbedding);
                    if (similarity > threshold) {
                        matchingUsers.put(faceData.getName(), faceData);
                        break;
                    }
                }
            }
        }
        return matchingUsers;
    }

    // =============== Displays matching users dialog ===============
    private void showMatchingUsersDialog(Map<String, FaceData> matchingUsers, List<float[]> embeddings) {
        AlertDialog.Builder matchingDialog = new AlertDialog.Builder(MainActivityCPP.this);
        matchingDialog.setTitle("Phát hiện khuôn mặt tương tự");
        StringBuilder messageBuilder = new StringBuilder("Hình ảnh đã chọn trùng khớp với người dùng đã tồn tại:\n");
        for (String name : matchingUsers.keySet()) {
            messageBuilder.append("- ").append(name).append("\n");
        }
        matchingDialog.setMessage(messageBuilder.toString());
        matchingDialog.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        matchingDialog.show();
    }

    // =============== Displays user selection dialog ===============
    private void showUserSelectionDialog(Map<String, FaceData> matchingUsers, List<float[]> embeddings) {
        AlertDialog.Builder userSelectDialog = new AlertDialog.Builder(MainActivityCPP.this);
        userSelectDialog.setTitle("Chọn người dùng");
        List<String> userList = new ArrayList<>(matchingUsers.keySet());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivityCPP.this, android.R.layout.simple_list_item_1, userList);
        userSelectDialog.setAdapter(adapter, (dialog1, which1) -> {
            String selectedUserName = userList.get(which1);
            FaceData selectedUser = matchingUsers.get(selectedUserName);
            synchronized (faceDataList) {
                for (float[] embedding : embeddings) {
                    selectedUser.getEmbeddings().add(floatArrayToList(embedding));
                }
            }
            saveFaceDataToFirebase(selectedUser);
            Toast.makeText(MainActivityCPP.this, "Đã thêm ảnh vào người dùng sẵn có.", Toast.LENGTH_SHORT).show();
        });
        userSelectDialog.setNegativeButton("Hủy", (dialog1, which1) -> dialog1.cancel());
        userSelectDialog.show();
    }

    // =============== Core logic ===============
    private List<Float> floatArrayToList(float[] array) {
        List<Float> list = new ArrayList<>();
        for (float val : array) {
            list.add(val);
        }
        return list;
    }

    // =============== Core logic ===============
    private List<List<Float>> convertEmbeddingsToList(List<float[]> embeddings) {
        List<List<Float>> embeddingsList = new ArrayList<>();
        for (float[] embedding : embeddings) {
            embeddingsList.add(floatArrayToList(embedding));
        }
        return embeddingsList;
    }

    // =============== Displays name input dialog ===============
    private void showNameInputDialog(List<float[]> embeddings) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivityCPP.this);
        builder.setTitle("Nhập thông tin người dùng");
        LinearLayout layout = new LinearLayout(MainActivityCPP.this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText nameInput = new EditText(MainActivityCPP.this);
        nameInput.setHint("Tên");
        layout.addView(nameInput);

        final EditText latitudeInput = new EditText(MainActivityCPP.this);
        latitudeInput.setHint("Vĩ độ (ví dụ: 21.028511)");
        latitudeInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        layout.addView(latitudeInput);

        final EditText longitudeInput = new EditText(MainActivityCPP.this);
        longitudeInput.setHint("Kinh độ (ví dụ: 105.804817)");
        longitudeInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        layout.addView(longitudeInput);

        final EditText workStartHourInput = new EditText(MainActivityCPP.this);
        workStartHourInput.setHint("Giờ làm việc (ví dụ: 8)");
        workStartHourInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(workStartHourInput);

        final EditText workEndHourInput = new EditText(MainActivityCPP.this);
        workEndHourInput.setHint("Giờ tan ca (ví dụ: 17)");
        workEndHourInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(workEndHourInput);

        builder.setView(layout);
        builder.setPositiveButton("OK", (dialog, which) -> {
            String name = nameInput.getText().toString().trim();
            String latitudeStr = latitudeInput.getText().toString().trim();
            String longitudeStr = longitudeInput.getText().toString().trim();
            String workStartHourStr = workStartHourInput.getText().toString().trim();
            String workEndHourStr = workEndHourInput.getText().toString().trim();
            String companyName = managerCompanyName;

            if (!name.isEmpty() && !latitudeStr.isEmpty() && !longitudeStr.isEmpty() &&
                    !workStartHourStr.isEmpty() && !workEndHourStr.isEmpty() &&
                    companyName != null && !companyName.isEmpty()) {
                try {
                    double latitude = Double.parseDouble(latitudeStr);
                    double longitude = Double.parseDouble(longitudeStr);
                    int workStartHour = Integer.parseInt(workStartHourStr);
                    int workEndHour = Integer.parseInt(workEndHourStr);
                    handleNameAndCompanyInput(name, latitude, longitude, workStartHour, workEndHour, embeddings);
                } catch (NumberFormatException e) {
                    Toast.makeText(MainActivityCPP.this, "Vui lòng nhập các giá trị hợp lệ.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Invalid input for latitude, longitude, workStartHour, or workEndHour.");
                }
            } else {
                Toast.makeText(MainActivityCPP.this, "Tất cả các trường không được để trống.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "User attempted to save face data without entering required fields.");
            }
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // =============== Handles name and company input ===============
    private void handleNameAndCompanyInput(String name, double latitude, double longitude,
                                           int workStartHour, int workEndHour, List<float[]> embeddings) {
        String companyName = managerCompanyName;
        FaceData existingFaceData = null;
        synchronized (faceDataList) {
            for (FaceData f : faceDataList) {
                if (f.getName().equalsIgnoreCase(name) && f.getCompanyName().equalsIgnoreCase(companyName)) {
                    existingFaceData = f;
                    break;
                }
            }
        }
        if (existingFaceData != null) {
            showNameAndCompanyExistsDialog(existingFaceData, embeddings);
        } else {
            createNewUserWithEmbeddingsAndCompany(name, companyName, latitude, longitude, workStartHour, workEndHour, embeddings);
        }
    }

    // =============== Displays name and company exists dialog ===============
    private void showNameAndCompanyExistsDialog(FaceData existingFaceData, List<float[]> embeddings) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivityCPP.this);
        dialog.setTitle("Tên và Công ty đã tồn tại");
        dialog.setMessage("Đã có người dùng với tên và công ty này. Bạn có muốn thêm ảnh mới vào người dùng đó không?");
        dialog.setPositiveButton("Có", (dialog1, which1) -> {
            synchronized (faceDataList) {
                for (float[] emb : embeddings) {
                    existingFaceData.getEmbeddings().add(floatArrayToList(emb));
                }
            }
            saveFaceDataToFirebase(existingFaceData);
            Toast.makeText(MainActivityCPP.this, "Đã thêm ảnh mới vào người dùng sẵn có.", Toast.LENGTH_SHORT).show();
        });
        dialog.setNegativeButton("Không", (dialog1, which1) -> dialog1.cancel());
        dialog.show();
    }

    // =============== Core logic ===============
    private void createNewUserWithEmbeddingsAndCompany(String name, String companyName, double latitude,
                                                       double longitude, int workStartHour, int workEndHour,
                                                       List<float[]> embeddings) {
        String userId = UUID.randomUUID().toString();
        List<List<Float>> embeddingsList = convertEmbeddingsToList(embeddings);
        FaceData faceData = new FaceData(userId, name, companyName, embeddingsList, latitude, longitude, workStartHour, workEndHour);
        saveFaceDataToFirebase(faceData);
        Toast.makeText(MainActivityCPP.this, "Dữ liệu khuôn mặt đã được lưu.", Toast.LENGTH_SHORT).show();
    }

    // =============== Saves face data to firebase ===============
    private void saveFaceDataToFirebase(FaceData faceData) {
        faceDataRef.child(faceData.getUserId()).setValue(faceData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Face data saved for user: " + faceData.getName()))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save face data: " + e.getMessage());
                    Toast.makeText(MainActivityCPP.this, "Lưu dữ liệu thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // =============== Saves check data to firebase ===============
    private void saveCheckDataToFirebase(String checkId, CheckData checkData) {
        if (checkId == null) {
            Log.e(TAG, "Attempted to save CheckData with null checkId.");
            return;
        }
        checkDataRef.child(checkId).setValue(checkData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Check data saved for user: " + checkData.getName());
                    checkDataMap.put(checkId, checkData);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save check data: " + e.getMessage());
                    Toast.makeText(MainActivityCPP.this, "Lưu dữ liệu check thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // =============== Loads face data list ===============
    private void loadFaceDataList() {
        faceDataRef.addChildEventListener(new ChildEventListener() {
            @Override
            // =============== Lifecycle callback for child added ===============
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                try {
                    FaceData faceData = snapshot.getValue(FaceData.class);
                    if (faceData != null && faceData.getEmbeddings() != null) {
                        synchronized (faceDataList) {
                            faceDataList.add(faceData);
                        }
                        Log.d(TAG, "Loaded FaceData: " + faceData.getName());
                    }
                } catch (DatabaseException e) {
                    Log.e(TAG, "DatabaseException: " + e.getMessage());
                    Toast.makeText(MainActivityCPP.this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
            @Override
            // =============== Lifecycle callback for child changed ===============
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                try {
                    FaceData faceData = snapshot.getValue(FaceData.class);
                    if (faceData != null && faceData.getEmbeddings() != null) {
                        synchronized (faceDataList) {
                            for (int i = 0; i < faceDataList.size(); i++) {
                                if (faceDataList.get(i).getUserId().equals(faceData.getUserId())) {
                                    faceDataList.set(i, faceData);
                                    break;
                                }
                            }
                        }
                    }
                } catch (DatabaseException e) {
                    Log.e(TAG, "DatabaseException: " + e.getMessage());
                    Toast.makeText(MainActivityCPP.this, "Lỗi cập nhật dữ liệu: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
            @Override
            // =============== Lifecycle callback for child removed ===============
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                String userId = snapshot.getKey();
                if (userId != null) {
                    synchronized (faceDataList) {
                        faceDataList.removeIf(faceData -> faceData.getUserId().equals(userId));
                    }
                    Log.d(TAG, "Removed FaceData with userId: " + userId);
                }
            }
            @Override
            // =============== Lifecycle callback for child moved ===============
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override
            // =============== Lifecycle callback for cancelled ===============
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load face data: " + error.getMessage());
                Toast.makeText(MainActivityCPP.this, "Không tải được dữ liệu: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // =============== Loads check data list ===============
    private void loadCheckDataList() {
        checkDataRef.addChildEventListener(new ChildEventListener() {
            @Override
            // =============== Lifecycle callback for child added ===============
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                CheckData checkData = snapshot.getValue(CheckData.class);
                String key = snapshot.getKey();
                if (checkData != null && key != null) {
                    checkDataMap.put(key, checkData);
                    Log.d(TAG, "Loaded CheckData: " + checkData.getName());
                }
            }
            @Override
            // =============== Lifecycle callback for child changed ===============
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                CheckData checkData = snapshot.getValue(CheckData.class);
                String key = snapshot.getKey();
                if (checkData != null && key != null) {
                    checkDataMap.put(key, checkData);
                    Log.d(TAG, "Updated CheckData: " + checkData.getName());
                }
            }
            @Override
            // =============== Lifecycle callback for child removed ===============
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                String checkId = snapshot.getKey();
                if (checkId != null) {
                    checkDataMap.remove(checkId);
                    Log.d(TAG, "Removed CheckData with checkId: " + checkId);
                }
            }
            @Override
            // =============== Lifecycle callback for child moved ===============
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override
            // =============== Lifecycle callback for cancelled ===============
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load check data: " + error.getMessage());
                Toast.makeText(MainActivityCPP.this, "Không tải được dữ liệu check: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // =============== Starts location updates ===============
    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission();
        } else {
            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setInterval(10000);
            locationRequest.setFastestInterval(1000);
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
            Log.d(TAG, "Started location updates.");
        }
    }

    // =============== Gets last known location ===============
    private void getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permission not granted.");
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        currentLocation = location;
                        Log.d(TAG, "Last known location: " + location.getLatitude() + ", " + location.getLongitude());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get last known location: " + e.getMessage());
                    Toast.makeText(MainActivityCPP.this, "Không lấy được vị trí: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // =============== Requests permissions ===============
    private void requestPermissions() {
        boolean cameraPermissionGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
        boolean locationPermissionGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        if (!cameraPermissionGranted) {
            requestCameraPermission();
        }
        if (!locationPermissionGranted) {
            requestLocationPermission();
        }
        if (cameraPermissionGranted && locationPermissionGranted) {
            initFaceDetectionAndRecognition();
            mOpenCvCameraView.enableView();
            startLocationUpdates();
            getLastKnownLocation();
            Log.d(TAG, "All permissions granted.");
        }
    }

    // =============== Requests camera permission ===============
    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        Log.d(TAG, "Requesting camera permission.");
    }

    // =============== Requests location permission ===============
    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        Log.d(TAG, "Requesting location permission.");
    }

    // =============== Initializes face detection and recognition ===============
    private void initFaceDetectionAndRecognition() {
        executorService.submit(() -> {
            try {
                InputStream inputStream = getAssets().open("face_detection_yunet_2023mar.onnx");
                FileUtil fileUtil = new FileUtil();
                File detectionModelFile = fileUtil.createTempFile(this, inputStream, "face_detection_yunet_2023mar.onnx");
                InitFaceDetector(detectionModelFile.getAbsolutePath());
                Log.d(TAG, "Face Detector initialized.");
            } catch (IOException e) {
                Log.e(TAG, "Error initializing Face Detector: " + e.getMessage());
                Toast.makeText(this, "Lỗi khởi tạo bộ phát hiện: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
            try {
                InputStream inputStream = getAssets().open("sface_fp16.onnx");
                FileUtil fileUtil = new FileUtil();
                File recognitionModelFile = fileUtil.createTempFile(this, inputStream, "sface_fp16.onnx");
                InitFaceRecognition(recognitionModelFile.getAbsolutePath());
                Log.d(TAG, "Face Recognition initialized.");
            } catch (IOException e) {
                Log.e(TAG, "Error initializing Face Recognition: " + e.getMessage());
                Toast.makeText(this, "Lỗi khởi tạo nhận diện: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // =============== Displays registered faces ===============
    private void displayRegisteredFaces() {
        if (managerCompanyName == null || managerCompanyName.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy tên công ty.", Toast.LENGTH_SHORT).show();
            return;
        }
        List<FaceData> snapshot;
        synchronized (faceDataList) {
            snapshot = new ArrayList<>(faceDataList);
        }
        List<FaceData> filteredSnapshot = new ArrayList<>();
        for (FaceData face : snapshot) {
            if (managerCompanyName.equalsIgnoreCase(face.getCompanyName())) {
                filteredSnapshot.add(face);
            }
        }
        if (filteredSnapshot.isEmpty()) {
            Toast.makeText(this, "Không có người dùng trong công ty.", Toast.LENGTH_SHORT).show();
            return;
        }
        ListView listView = new ListView(MainActivityCPP.this);
        List<String> names = new ArrayList<>();
        for (FaceData faceData : filteredSnapshot) {
            names.add(faceData.getName() + " (ID: " + faceData.getUserId() + ")");
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivityCPP.this,
                android.R.layout.simple_list_item_1, names);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            FaceData selectedFace = filteredSnapshot.get(position);
            showFaceDataDialog(selectedFace);
        });
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivityCPP.this);
        builder.setTitle("Các khuôn mặt trong công ty: " + managerCompanyName);
        builder.setView(listView);
        builder.setNegativeButton("Đóng", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    // =============== Displays face data dialog ===============
    private void showFaceDataDialog(FaceData selectedFace) {
        AlertDialog.Builder embeddingDialog = new AlertDialog.Builder(MainActivityCPP.this);
        embeddingDialog.setTitle("Thông tin cho " + selectedFace.getName());
        StringBuilder sb = new StringBuilder();
        sb.append("Công ty: ").append(selectedFace.getCompanyName()).append("\n");
        sb.append("Vĩ độ làm việc: ").append(selectedFace.getOfficeLatitude()).append("\n");
        sb.append("Kinh độ làm việc: ").append(selectedFace.getOfficeLongitude()).append("\n");
        sb.append("Giờ làm việc: ").append(selectedFace.getWorkStartHour()).append("h\n");
        sb.append("Giờ tan ca: ").append(selectedFace.getWorkEndHour()).append("h\n");
        sb.append("Số lượng hình ảnh: ").append(selectedFace.getEmbeddings().size()).append("\n");
        embeddingDialog.setMessage(sb.toString());
        embeddingDialog.setPositiveButton("Xóa người dùng", (dialogInterface, i12) -> showDeleteUserDialog(selectedFace));
        embeddingDialog.setNegativeButton("Đóng", (dialog, which) -> dialog.dismiss());
        embeddingDialog.show();
    }

    // =============== Displays delete user dialog ===============
    private void showDeleteUserDialog(FaceData selectedFace) {
        AlertDialog.Builder confirmDialog = new AlertDialog.Builder(MainActivityCPP.this);
        confirmDialog.setTitle("Xóa người dùng");
        confirmDialog.setMessage("Bạn có chắc chắn muốn xóa \"" + selectedFace.getName() + "\" không?");
        confirmDialog.setPositiveButton("Có", (dialog, which) -> deleteUser(selectedFace));
        confirmDialog.setNegativeButton("Không", (dialog, which) -> dialog.cancel());
        confirmDialog.show();
    }

    // =============== Deletes user ===============
    private void deleteUser(FaceData faceData) {
        String userId = faceData.getUserId();
        if (userId != null && !userId.isEmpty()) {
            faceDataRef.child(userId).removeValue()
                    .addOnSuccessListener(aVoid -> Toast.makeText(MainActivityCPP.this, "Đã xóa người dùng.", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(MainActivityCPP.this, "Xóa thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(MainActivityCPP.this, "ID không hợp lệ.", Toast.LENGTH_SHORT).show();
        }
    }

    // =============== Displays saved checks ===============
    private void displaySavedChecks() {
        String activeUid = (currentUserId != null) ? currentUserId : (mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null);
        if (activeUid == null) {
            Toast.makeText(this, "Chưa xác định người dùng.", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference roleRef = FirebaseDatabase.getInstance().getReference("users").child(activeUid).child("role");
        roleRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            // =============== Lifecycle callback for data change ===============
            public void onDataChange(@NonNull DataSnapshot snap) {
                String role = snap.getValue(String.class);
                boolean isManager = "Manager".equalsIgnoreCase(role);
                List<CheckData> result = new ArrayList<>();

                if (isManager) {
                    snap.getRef().getParent().child("company").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        // =============== Lifecycle callback for data change ===============
                        public void onDataChange(@NonNull DataSnapshot cSnap) {
                            String companyName = cSnap.getValue(String.class);
                            if (companyName == null) companyName = "";
                            for (CheckData cd : checkDataMap.values()) {
                                if (companyName.equalsIgnoreCase(cd.getCompanyName())) {
                                    result.add(cd);
                                }
                            }
                            showChecksDialog(result, "Lịch sử Check công ty: " + companyName);
                        }
                        @Override
                        // =============== Lifecycle callback for cancelled ===============
                        public void onCancelled(@NonNull DatabaseError e) {}
                    });
                } else {
                    for (CheckData cd : checkDataMap.values()) {
                        if (lastRecognizedName != null && lastRecognizedName.equalsIgnoreCase(cd.getName())) {
                            result.add(cd);
                        }
                    }
                    showChecksDialog(result, "Lịch sử Check của bạn");
                }
            }
            @Override
            // =============== Lifecycle callback for cancelled ===============
            public void onCancelled(@NonNull DatabaseError err) {
                Toast.makeText(MainActivityCPP.this, "Lỗi khi đọc quyền: " + err.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // =============== Displays attendance modal ===============
    private void showAttendanceModal() {
        final Context ctx = MainActivityCPP.this;
        String uidLogin = (currentUserId != null) ? currentUserId : (mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null);
        if (uidLogin == null) {
            Toast.makeText(ctx, "Chưa đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uidLogin);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("PrivateResource")
            @Override
            // =============== Lifecycle callback for data change ===============
            public void onDataChange(@NonNull DataSnapshot snap) {
                final boolean isManager = "Manager".equalsIgnoreCase(snap.child("role").getValue(String.class));
                final String company = snap.child("company").getValue(String.class);

                LinearLayout root = new LinearLayout(ctx);
                root.setOrientation(LinearLayout.VERTICAL);
                int pad = (int) (16 * getResources().getDisplayMetrics().density);
                root.setPadding(pad, pad, pad, pad);

                final TextView tvDate = new TextView(ctx);
                tvDate.setTextSize(16);
                tvDate.setTypeface(Typeface.DEFAULT_BOLD);
                tvDate.setTextColor(Color.WHITE);
                tvDate.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#009688")));
                tvDate.setBackgroundResource(androidx.appcompat.R.drawable.abc_btn_default_mtrl_shape);
                tvDate.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_twotone_calendar_today_24, 0, 0, 0);
                tvDate.setCompoundDrawablePadding(12);
                tvDate.setPadding(48, 32, 48, 32);
                tvDate.setGravity(Gravity.CENTER);
                root.addView(tvDate);

                RadioGroup rgFilter = new RadioGroup(ctx);
                rgFilter.setOrientation(RadioGroup.HORIZONTAL);
                rgFilter.setGravity(Gravity.CENTER);
                String[] filterLabels = {"Tất cả", "Chưa", "Thiếu", "Đủ"};
                for (int i = 0; i < filterLabels.length; i++) {
                    RadioButton rb = new RadioButton(ctx);
                    rb.setText(filterLabels[i]);
                    rb.setId(i);
                    rgFilter.addView(rb);
                }
                rgFilter.check(0);
                root.addView(rgFilter);

                ListView lv = new ListView(ctx);
                lv.setDivider(null);
                root.addView(lv);

                AlertDialog dlg = new AlertDialog.Builder(MainActivityCPP.this)
                        .setTitle("Lịch sử chấm công")
                        .setView(root)
                        .setNegativeButton("Đóng", (d, w) -> d.dismiss())
                        .create();

                dlg.setOnShowListener(di -> {
                    Button close = dlg.getButton(AlertDialog.BUTTON_NEGATIVE);
                    if (close != null) close.setTextColor(Color.BLACK);
                });

                final Calendar calSel = Calendar.getInstance();

                Runnable reload = () -> {
                    String dayStr = dayFmt.format(calSel.getTime());
                    tvDate.setText(dayStr.equals(dayFmt.format(new Date())) ? "Ngày: " + dayStr + " (Hôm nay)" : "Ngày: " + dayStr);

                    int filter = rgFilter.getCheckedRadioButtonId();
                    List<AttendanceRecord> list = new ArrayList<>();

                    if (isManager) {
                        List<FaceData> snapshot;
                        synchronized (faceDataList) {
                            snapshot = new ArrayList<>(faceDataList);
                        }
                        for (FaceData faceData : snapshot) {
                            if (faceData.getCompanyName().equals(company)) {
                                DatabaseReference attendanceRef = FirebaseDatabase.getInstance().getReference("attendanceRecords")
                                        .child(faceData.getUserId()).child(dayStr);
                                attendanceRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    // =============== Lifecycle callback for data change ===============
                                    public void onDataChange(@NonNull DataSnapshot snap) {
                                        AttendanceRecord record = snap.getValue(AttendanceRecord.class);
                                        if (record == null) {
                                            record = new AttendanceRecord(faceData.getUserId(), dayStr);
                                            record.setInTime(0);
                                            record.setOutTime(0);
                                            record.setCompanyName(faceData.getCompanyName());
                                        }
                                        list.add(record);
                                        setAdapter(lv, list, dayStr, true, filter);
                                    }
                                    @Override
                                    // =============== Lifecycle callback for cancelled ===============
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Log.e(TAG, "Failed to load attendance: " + error.getMessage());
                                    }
                                });
                            }
                        }
                    } else {
                        String target = getUserIdByName(lastRecognizedName);
                        DatabaseReference attendanceRef = FirebaseDatabase.getInstance().getReference("attendanceRecords")
                                .child(target).child(dayStr);
                        attendanceRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            // =============== Lifecycle callback for data change ===============
                            public void onDataChange(@NonNull DataSnapshot snap) {
                                AttendanceRecord record = snap.getValue(AttendanceRecord.class);
                                if (record == null) {
                                    record = new AttendanceRecord(uidLogin, dayStr);
                                    record.setInTime(0);
                                    record.setOutTime(0);
                                }
                                list.add(record);
                                setAdapter(lv, list, dayStr, false, filter);
                            }
                            @Override
                            // =============== Lifecycle callback for cancelled ===============
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e(TAG, "Lỗi tải dữ liệu: " + error.getMessage());
                            }
                        });
                    }
                };

                tvDate.setOnClickListener(v -> new DatePickerDialog(
                        ctx, R.style.CustomDatePickerDialog,
                        (view, y, m, d) -> {
                            calSel.set(y, m, d, 0, 0, 0);
                            reload.run();
                        },
                        calSel.get(Calendar.YEAR),
                        calSel.get(Calendar.MONTH),
                        calSel.get(Calendar.DAY_OF_MONTH)
                ).show());

                rgFilter.setOnCheckedChangeListener((group, checkedId) -> reload.run());
                reload.run();
                dlg.show();
            }

            @Override
            // =============== Lifecycle callback for cancelled ===============
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ctx, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // =============== Displays monthly attendance dialog ===============
    private void showMonthlyAttendanceDialog() {
        final Context ctx = MainActivityCPP.this;
        String uidLogin = (currentUserId != null) ? currentUserId : (mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null);
        if (uidLogin == null) {
            Toast.makeText(ctx, "Chưa đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uidLogin);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            // =============== Lifecycle callback for data change ===============
            public void onDataChange(@NonNull DataSnapshot snap) {
                String userName = snap.child("name").getValue(String.class);

                AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
                View dialogView = getLayoutInflater().inflate(R.layout.dialog_monthly_attendance, null);
                builder.setView(dialogView);
                builder.setTitle("Thống kê cá nhân");

                NumberPicker monthPicker = dialogView.findViewById(R.id.monthPicker);
                NumberPicker yearPicker = dialogView.findViewById(R.id.yearPicker);
                TextView tvTotalTime = dialogView.findViewById(R.id.tvTotalTime);
                ListView lvAttendance = dialogView.findViewById(R.id.lvAttendance);
                PieChartView pieChart = dialogView.findViewById(R.id.pieChart);

                Calendar cal = Calendar.getInstance();
                int currYear = cal.get(Calendar.YEAR);
                int currMonth = cal.get(Calendar.MONTH) + 1;

                monthPicker.setMinValue(1);
                monthPicker.setMaxValue(12);
                monthPicker.setValue(currMonth);

                yearPicker.setMinValue(currYear - 10);
                yearPicker.setMaxValue(currYear + 10);
                yearPicker.setValue(currYear);

                ArrayAdapter<SpannableString> adapter = new ArrayAdapter<>(ctx, android.R.layout.simple_list_item_1);
                lvAttendance.setAdapter(adapter);

                Runnable loadData = () -> {
                    int selMonth = monthPicker.getValue();
                    int selYear = yearPicker.getValue();
                    String monthKey = String.format(Locale.getDefault(), "%04d-%02d", selYear, selMonth);
                    String target = getUserIdByName(lastRecognizedName);
                    if (target == null) {
                        target = uidLogin;
                    }
                    Log.d(TAG, "Đang tải dữ liệu cho user: " + target + ", tháng: " + monthKey);

                    DatabaseReference attRef = FirebaseDatabase.getInstance().getReference("attendanceRecords").child(target);

                    attRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        // =============== Lifecycle callback for data change ===============
                        public void onDataChange(@NonNull DataSnapshot ds) {
                            List<AttendanceRecord> monthRecords = new ArrayList<>();
                            for (DataSnapshot dayNode : ds.getChildren()) {
                                String dayKey = dayNode.getKey();
                                if (dayKey != null && dayKey.startsWith(monthKey)) {
                                    AttendanceRecord r = dayNode.getValue(AttendanceRecord.class);
                                    if (r != null) monthRecords.add(r);
                                }
                            }
                            Log.d(TAG, "Số bản ghi tìm thấy: " + monthRecords.size());

                            if (monthRecords.isEmpty()) {
                                tvTotalTime.setText("Tổng thời gian làm việc: 00:00:00\nTổng thời gian đi trễ: 00:00:00\nTổng thời gian về sớm: 00:00:00\nCông tháng: 0 ngày");
                                adapter.clear();
                                adapter.add(new SpannableString("Chưa có dữ liệu"));
                                try {
                                    pieChart.setWorkDays(0);
                                    pieChart.invalidate();
                                } catch (Exception e) {
                                    Log.e(TAG, "Lỗi khi đặt số ngày công: " + e.getMessage());
                                }
                                return;
                            }

                            Collections.sort(monthRecords, (a, b) -> b.getDate().compareTo(a.getDate()));

                            double totalWorkDays = 0.0;
                            long totalMillis = 0;
                            long totalLateMillis = 0;
                            long totalEarlyLeaveMillis = 0;
                            for (AttendanceRecord r : monthRecords) {
                                if (r.getInTime() != 0 && r.getOutTime() != 0) {
                                    FaceData faceData = getFaceDataByUserId(r.getUserId());
                                    if (faceData != null) {
                                        long workStart = getWorkStartTime(r.getDate(), faceData.getWorkStartHour());
                                        long workEnd = getWorkEndTime(r.getDate(), faceData.getWorkEndHour());
                                        long requiredWorkMillis = workEnd - workStart;
                                        long actualWorkMillis = r.getOutTime() - r.getInTime();

                                        double workRatio = (double) actualWorkMillis / requiredWorkMillis;
                                        if (workRatio > 1.0) workRatio = 1.0;
                                        totalWorkDays += workRatio;

                                        totalMillis += actualWorkMillis;


                                        if (r.getInTime() > workStart) {
                                            totalLateMillis += r.getInTime() - workStart;
                                        }

                                        if (r.getOutTime() < workEnd) {
                                            totalEarlyLeaveMillis += workEnd - r.getOutTime();
                                        }
                                    }
                                }
                            }
                            Log.d(TAG, "Tổng công tháng: " + totalWorkDays);

                            long totalSeconds = totalMillis / 1000;
                            String totalStr = String.format(Locale.getDefault(), "%02d:%02d:%02d",
                                    totalSeconds / 3600, (totalSeconds % 3600) / 60, totalSeconds % 60);


                            int totalLateMinutes = (int) Math.round(totalLateMillis / 60000.0);
                            int totalEarlyLeaveMinutes = (int) Math.round(totalEarlyLeaveMillis / 60000.0);

                            tvTotalTime.setText("Tổng thời gian làm việc: " + totalStr + "\n" +
                                    "Tổng thời gian đi trễ: " + totalLateMinutes + " phút\n" +
                                    "Tổng thời gian về sớm: " + totalEarlyLeaveMinutes + " phút\n" +
                                    "Công tháng: " + String.format("%.2f", totalWorkDays) + " công");

                            try {
                                pieChart.setWorkDays((int) Math.round(totalWorkDays));
                                pieChart.invalidate();
                            } catch (Exception e) {
                                Log.e(TAG, "Lỗi khi đặt số ngày công: " + e.getMessage());
                            }


                            adapter.clear();
                            for (AttendanceRecord r : monthRecords) {
                                String in = r.getInTime() == 0 ? "--" : timeFmt.format(new Date(r.getInTime()));
                                String out = r.getOutTime() == 0 ? "--" : timeFmt.format(new Date(r.getOutTime()));
                                long secs = Math.max(r.getDurationMillis(), 0) / 1000;
                                String dur = String.format(Locale.getDefault(), "%02d:%02d:%02d",
                                        secs / 3600, (secs % 3600) / 60, secs % 60);

                                FaceData faceData = getFaceDataByUserId(r.getUserId());
                                String lateInfo = "";
                                String earlyInfo = "";
                                String onTimeInfo = "";

                                if (faceData != null && r.getInTime() != 0 && r.getOutTime() != 0) {
                                    long workStart = getWorkStartTime(r.getDate(), faceData.getWorkStartHour());
                                    long workEnd = getWorkEndTime(r.getDate(), faceData.getWorkEndHour());


                                    if (r.getInTime() > workStart) {
                                        long lateMillis = r.getInTime() - workStart;
                                        int lateMinutes = (int) (lateMillis / 60000);
                                        lateInfo = " (Đi trễ " + lateMinutes + " phút)";
                                    }
                                    if (r.getOutTime() < workEnd) {
                                        long earlyMillis = workEnd - r.getOutTime();
                                        int earlyMinutes = (int) (earlyMillis / 60000);
                                        earlyInfo = " (Về sớm " + earlyMinutes + " phút)";
                                    }
                                }


                                StringBuilder displayTextBuilder = new StringBuilder();
                                displayTextBuilder.append("Ngày: ").append(r.getDate()).append("\n")
                                        .append("Thời gian vào: ").append(in);
                                if (!lateInfo.isEmpty()) {
                                    displayTextBuilder.append(lateInfo);
                                }
                                if (!onTimeInfo.isEmpty() && lateInfo.isEmpty()) {
                                    displayTextBuilder.append(onTimeInfo);
                                }
                                displayTextBuilder.append("\n")
                                        .append("Thời gian về: ").append(out);
                                if (!earlyInfo.isEmpty()) {
                                    displayTextBuilder.append(earlyInfo);
                                }
                                displayTextBuilder.append("\n")
                                        .append("Tổng thời gian làm việc: ").append(dur);

                                String displayText = displayTextBuilder.toString();
                                SpannableString spannable = new SpannableString(displayText);


                                spannable.setSpan(new StyleSpan(Typeface.BOLD), 0, 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                spannable.setSpan(new StyleSpan(Typeface.BOLD), displayText.indexOf("\nThời gian vào: "),
                                        displayText.indexOf("\nThời gian vào: ") + 13, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                spannable.setSpan(new StyleSpan(Typeface.BOLD), displayText.indexOf("\nThời gian về: "),
                                        displayText.indexOf("\nThời gian về: ") + 12, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                spannable.setSpan(new StyleSpan(Typeface.BOLD), displayText.indexOf("\nTổng thời gian làm việc: "),
                                        displayText.indexOf("\nTổng thời gian làm việc: ") + 22, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);


                                if (!lateInfo.isEmpty()) {
                                    int lateStart = displayText.indexOf(lateInfo);
                                    int lateEnd = lateStart + lateInfo.length();
                                    spannable.setSpan(new ForegroundColorSpan(Color.RED), lateStart, lateEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                }

                                if (!earlyInfo.isEmpty()) {
                                    int earlyStart = displayText.indexOf(earlyInfo);
                                    int earlyEnd = earlyStart + earlyInfo.length();
                                    spannable.setSpan(new ForegroundColorSpan(Color.RED), earlyStart, earlyEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                }

                                if (!onTimeInfo.isEmpty()) {
                                    int onTimeStart = displayText.indexOf(onTimeInfo);
                                    int onTimeEnd = onTimeStart + onTimeInfo.length();
                                    spannable.setSpan(new ForegroundColorSpan(Color.GREEN), onTimeStart, onTimeEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                }

                                adapter.add(spannable);
                            }
                        }

                        @Override
                        // =============== Lifecycle callback for cancelled ===============
                        public void onCancelled(@NonNull DatabaseError e) {
                            Toast.makeText(ctx, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                };

                monthPicker.setOnValueChangedListener((picker, oldVal, newVal) -> loadData.run());
                yearPicker.setOnValueChangedListener((picker, oldVal, newVal) -> loadData.run());
                loadData.run();

                builder.setNegativeButton("Đóng", null);
                builder.show();
            }

            @Override
            // =============== Lifecycle callback for cancelled ===============
            public void onCancelled(@NonNull DatabaseError e) {
                Toast.makeText(ctx, "Lỗi tải thông tin: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // =============== Core logic ===============
    private String formatDuration(long millis) {
        if (millis <= 0) return "00:00:00";
        long seconds = millis / 1000;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d",
                seconds / 3600, (seconds % 3600) / 60, seconds % 60);
    }

    // =============== Gets work start time ===============
    private long getWorkStartTime(String date, int workStartHour) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date d = sdf.parse(date);
            Calendar cal = Calendar.getInstance();
            cal.setTime(d);
            cal.set(Calendar.HOUR_OF_DAY, workStartHour);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return cal.getTimeInMillis();
        } catch (Exception e) {
            Log.e(TAG, "Error parsing date: " + e.getMessage());
            return 0;
        }
    }

    // =============== Gets work end time ===============
    private long getWorkEndTime(String date, int workEndHour) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date d = sdf.parse(date);
            Calendar cal = Calendar.getInstance();
            cal.setTime(d);
            cal.set(Calendar.HOUR_OF_DAY, workEndHour);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return cal.getTimeInMillis();
        } catch (Exception e) {
            Log.e(TAG, "Error parsing date: " + e.getMessage());
            return 0;
        }
    }

    // =============== Gets face data by user id ===============
    private FaceData getFaceDataByUserId(String userId) {
        for (FaceData faceData : faceDataList) {
            if (faceData.getUserId().equals(userId)) {
                return faceData;
            }
        }
        return null;
    }

    // =============== Displays monthly attendance modal ===============
    private void showMonthlyAttendanceModal() {
        showMonthlyAttendanceDialog();
    }

    // =============== Sets adapter ===============
    private void setAdapter(ListView lv, List<AttendanceRecord> list, String dayStr, boolean isManager, int filter) {
        if (list == null) list = new ArrayList<>();
        switch (filter) {
            case 0: break;
            case 1: list.removeIf(r -> r.getInTime() > 0 || r.getOutTime() > 0); break;
            case 2: list.removeIf(r -> r.getInTime() == 0 || r.getOutTime() > 0); break;
            case 3: list.removeIf(r -> r.getInTime() == 0 || r.getOutTime() == 0); break;
        }
        Collections.sort(list, (a, b) -> Long.compare(b.getInTime(), a.getInTime()));
        AttendanceRowAdapter ad = new AttendanceRowAdapter(this, list, isManager, dayStr, MainActivityCPP.this);
        lv.setAdapter(ad);
    }

    // =============== Displays checks dialog ===============
    private void showChecksDialog(List<CheckData> list, String title) {
        if (list.isEmpty()) {
            Toast.makeText(this, "Chưa có lượt check.", Toast.LENGTH_SHORT).show();
            return;
        }
        Collections.sort(list, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
        ListView lv = new ListView(this);
        List<String> lines = new ArrayList<>();
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        for (CheckData c : list) {
            lines.add("Tên: " + c.getName() + "\n" +
                    "Vị trí: " + c.getLocation() + "\n" +
                    "Thời gian: " + f.format(new Date(c.getTimestamp())));
        }
        lv.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, lines));
        lv.setOnItemLongClickListener((p, v, pos, id) -> {
            CheckData cd = list.get(pos);
            String key = null;
            for (Map.Entry<String, CheckData> e : checkDataMap.entrySet()) {
                if (e.getValue().equals(cd)) { key = e.getKey(); break; }
            }
            if (key != null) showDeleteCheckDialog(key, cd);
            return true;
        });
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(lv)
                .setNegativeButton("Đóng", (d, w) -> d.dismiss())
                .show();
    }

    // =============== Gets face data by name ===============
    private FaceData getFaceDataByName(String name) {
        synchronized (faceDataList) {
            for (FaceData faceData : faceDataList) {
                if (faceData.getName().equalsIgnoreCase(name)) {
                    return faceData;
                }
            }
        }
        return null;
    }

    // =============== Displays delete check dialog ===============
    private void showDeleteCheckDialog(String checkId, CheckData selectedCheck) {
        AlertDialog.Builder confirmDialog = new AlertDialog.Builder(MainActivityCPP.this);
        confirmDialog.setTitle("Xóa lượt check");
        confirmDialog.setMessage("Bạn có chắc muốn xóa lượt check này?");
        confirmDialog.setPositiveButton("Có", (dialog, which) -> deleteCheck(checkId));
        confirmDialog.setNegativeButton("Không", (dialog, which) -> dialog.cancel());
        confirmDialog.show();
    }

    // =============== Deletes check ===============
    private void deleteCheck(String checkId) {
        if (checkId != null && !checkId.isEmpty()) {
            checkDataRef.child(checkId).removeValue()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(MainActivityCPP.this, "Đã xóa lượt check.", Toast.LENGTH_SHORT).show();
                        checkDataMap.remove(checkId);
                    })
                    .addOnFailureListener(e -> Toast.makeText(MainActivityCPP.this, "Xóa thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show());
        } else {
            Toast.makeText(MainActivityCPP.this, "ID check không hợp lệ.", Toast.LENGTH_LONG).show();
        }
    }

    // =============== Resets canceled faces ===============
    private void resetCanceledFaces() {
        if (!Objects.equals(currentName, "Unknown")) {
            showRecognitionDialog(currentName, currentId);
        } else {
            Toast.makeText(MainActivityCPP.this, "Chưa nhận diện được khuôn mặt.", Toast.LENGTH_SHORT).show();
        }
        canceledFaces.clear();
    }

    // =============== Handles sign in ===============
    private void handleSignIn() {
        Intent intent = new Intent(MainActivityCPP.this, Login.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    // =============== Handles sign out ===============
    private void handleSignOut() {
        mAuth.signOut();
        clearPreferences();
        Toast.makeText(MainActivityCPP.this, "Đăng xuất thành công", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(MainActivityCPP.this, Login.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    // =============== Core logic ===============
    private void clearPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_EMAIL);
        editor.remove(KEY_REMEMBER_ME);
        editor.apply();
    }

    // =============== Checks user role and adjust u i ===============
    private void checkUserRoleAndAdjustUI() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DatabaseReference userRoleRef = FirebaseDatabase.getInstance().getReference("users").child(userId).child("role");
            userRoleRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                // =============== Lifecycle callback for data change ===============
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String role = snapshot.getValue(String.class);
                    if ("Manager".equalsIgnoreCase(role)) {
                        DatabaseReference userCompanyRef = FirebaseDatabase.getInstance().getReference("users").child(userId).child("company");
                        userCompanyRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            // =============== Lifecycle callback for data change ===============
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                managerCompanyName = snapshot.getValue(String.class);
                                if (managerCompanyName == null || managerCompanyName.isEmpty()) {
                                    Toast.makeText(MainActivityCPP.this, "Không tìm thấy tên công ty.", Toast.LENGTH_SHORT).show();
                                    galleryBtn.setVisibility(View.GONE);
                                    showFacesBtn.setVisibility(View.GONE);
                                    return;
                                }
                                showMonthlyStatsBtn.setVisibility(View.GONE);
                                resetRecognitionBtn.setVisibility(View.GONE);
                                signOutBtn.setVisibility(View.VISIBLE);
                                signInBtn.setVisibility(View.GONE);
                                galleryBtn.setVisibility(View.VISIBLE);
                                showFacesBtn.setVisibility(View.VISIBLE);
                            }
                            @Override
                            // =============== Lifecycle callback for cancelled ===============
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e(TAG, "Failed to read company name: " + error.getMessage());
                                Toast.makeText(MainActivityCPP.this, "Lỗi khi lấy tên công ty.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {

                        mOpenCvCameraView.setVisibility(View.VISIBLE);
                        signOutBtn.setVisibility(View.GONE);
                        signInBtn.setVisibility(View.VISIBLE);
                        resetRecognitionBtn.setVisibility(View.VISIBLE);
                        galleryBtn.setVisibility(View.GONE);
                        showFacesBtn.setVisibility(View.GONE);
                    }
                }
                @Override
                // =============== Lifecycle callback for cancelled ===============
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Failed to read role: " + error.getMessage());
                    mOpenCvCameraView.setVisibility(View.VISIBLE);
                    signOutBtn.setVisibility(View.GONE);
                    signInBtn.setVisibility(View.VISIBLE);
                    resetRecognitionBtn.setVisibility(View.VISIBLE);
                    galleryBtn.setVisibility(View.GONE);
                    showFacesBtn.setVisibility(View.GONE);
                }
            });
        } else {
            showAllCompanyStatsBtn.setVisibility(View.GONE);
            mOpenCvCameraView.setVisibility(View.VISIBLE);
            signOutBtn.setVisibility(View.GONE);
            signInBtn.setVisibility(View.VISIBLE);
            resetRecognitionBtn.setVisibility(View.VISIBLE);
            galleryBtn.setVisibility(View.GONE);
            showFacesBtn.setVisibility(View.GONE);
        }
    }

    // =============== Loads recognition threshold ===============
    private void loadRecognitionThreshold() {
        float defaultThreshold = 0.4f;
        recognitionThreshold = getSharedPreferences("app_prefs", MODE_PRIVATE)
                .getFloat("recognition_threshold", defaultThreshold);
        Log.d(TAG, "Loaded recognition threshold: " + recognitionThreshold);
    }

    private @Nullable String getUserIdByName(String name) {
        if (name == null) return null;
        synchronized (faceDataList) {
            for (FaceData fd : faceDataList) {
                if (name.equalsIgnoreCase(fd.getName()))
                    return fd.getUserId();
            }
        }
        return null;
    }

    public @Nullable String getNameByUid(String uid) {
        if (uid == null) return null;
        synchronized (faceDataList) {
            for (FaceData f : faceDataList) {
                if (uid.equals(f.getUserId())) return f.getName();
            }
        }
        return null;
    }

    public class AttendanceRowAdapter extends ArrayAdapter<AttendanceRecord> {
        private static final int GAP_DP = 12;
        private final LayoutInflater inflater;
        private final boolean isManager;
        private final String selectedDay;
        private final String todayStr;
        private final SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        private final MainActivityCPP host;

        public AttendanceRowAdapter(@NonNull Context ctx, @NonNull List<AttendanceRecord> data,
                                    boolean isMgr, String selDay, MainActivityCPP hostAct) {
            super(ctx, 0, data);
            this.inflater = LayoutInflater.from(ctx);
            this.isManager = isMgr;
            this.selectedDay = selDay;
            this.host = hostAct;
            todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        }

        @NonNull @Override
        // =============== Gets view ===============
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            CardView card;
            TextView tv;
            if (convertView == null) {
                card = new CardView(getContext());
                card.setUseCompatPadding(true);
                card.setRadius(dp(8));
                tv = new TextView(getContext());
                tv.setTextSize(16);
                tv.setLineSpacing(0, 1.1f);
                int pad = dp(16);
                tv.setPadding(pad, pad, pad, pad);
                card.addView(tv);
                int gap = dp(GAP_DP);
                CardView.LayoutParams lp = new CardView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, gap, 0, gap);
                card.setLayoutParams(lp);
            } else {
                card = (CardView) convertView;
                tv = (TextView) card.getChildAt(0);
            }

            AttendanceRecord r = getItem(position);
            int bgColor;

            if (selectedDay.equals(todayStr)) {
                bgColor = Color.parseColor("#424242");
                tv.setTextColor(Color.WHITE);
            } else if (isManager) {
                boolean hasIn = r != null && r.getInTime() > 0;
                boolean hasOut = r != null && r.getOutTime() > 0;
                if (!hasIn && !hasOut) bgColor = Color.parseColor("#FFEBEE");
                else if (hasIn && !hasOut) bgColor = Color.parseColor("#FFF9C4");
                else bgColor = Color.parseColor("#E8F5E9");
                tv.setTextColor(Color.parseColor("#263238"));
            } else {
                if (r != null && r.getOutTime() > 0) bgColor = Color.parseColor("#E8F5E9");
                else bgColor = Color.parseColor("#FFEBEE");
                tv.setTextColor(Color.parseColor("#263238"));
            }
            card.setCardBackgroundColor(bgColor);

            SpannableStringBuilder sb = new SpannableStringBuilder();
            if (r == null || r.getDate() == null) {
                sb.append("Không có dữ liệu cho ngày này");
                tv.setText(sb);
                return card;
            }

            if (isManager) {
                appendBold(sb, "Tên: ");
                String name = host.getNameByUidPublic(r.getUserId());
                sb.append(name != null ? name : "(Không rõ)");
                sb.append('\n');
            }

            appendBold(sb, "Thời gian vào: ");
            String inTime = r.getInTime() == 0 ? "--" : timeFmt.format(new Date(r.getInTime()));
            sb.append(inTime);
            int inTimeEnd = sb.length();
            if (r.isLate()) {
                sb.append(" (Đến trễ)");
                sb.setSpan(new ForegroundColorSpan(Color.RED), inTimeEnd, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            sb.append('\n');

            appendBold(sb, "Thời gian về: ");
            String outTime = r.getOutTime() == 0 ? "--" : timeFmt.format(new Date(r.getOutTime()));
            sb.append(outTime);
            int outTimeEnd = sb.length();
            if (r.isEarlyLeave()) {
                sb.append(" (Về sớm)");
                sb.setSpan(new ForegroundColorSpan(Color.RED), outTimeEnd, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            sb.append('\n');

            appendBold(sb, "Tổng thời gian làm việc: ");
            sb.append(formatDuration(r.getDurationMillis()));

            tv.setText(sb);
            return card;
        }

        // =============== Core logic ===============
        private void appendBold(SpannableStringBuilder sb, String text) {
            int start = sb.length();
            sb.append(text);
            sb.setSpan(new StyleSpan(Typeface.BOLD), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // =============== Core logic ===============
        private String formatDuration(long millis) {
            if (millis <= 0) return "--";
            long s = millis / 1000;
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60);
        }

        // =============== Core logic ===============
        private int dp(int v) {
            return Math.round(getContext().getResources().getDisplayMetrics().density * v);
        }
    }

    // =============== Gets name by uid public ===============
    public String getNameByUidPublic(String uid) {
        return getNameByUid(uid);
    }

    // =============== Core logic ===============
    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }

    // =============== Gets status ===============
    private int getStatus(@Nullable AttendanceRecord r) {
        if (r == null) return 0;
        boolean hasIn = r.getInTime() > 0;
        boolean hasOut = r.getOutTime() > 0;
        if (!hasIn && !hasOut) return 0;
        if (hasIn && !hasOut) return 1;
        return 2;
    }



    // =============== Gets working days up to today ===============
    private int getWorkingDaysUpToToday(int year, int month) {
        Calendar cal = Calendar.getInstance();
        int currentYear = cal.get(Calendar.YEAR);
        int currentMonth = cal.get(Calendar.MONTH) + 1;
        if (year < currentYear || (year == currentYear && month < currentMonth)) {

            cal.set(year, month - 1, 1);
            return cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        } else if (year == currentYear && month == currentMonth) {

            return cal.get(Calendar.DAY_OF_MONTH);
        } else {

            return 0;
        }
    }

    // =============== Displays all company monthly stats modal ===============
    private void showAllCompanyMonthlyStatsModal() {
        if (managerCompanyName == null || managerCompanyName.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy tên công ty.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_all_company_stats, null);
            builder.setView(dialogView);
            builder.setTitle("Thống kê chấm công công ty: " + managerCompanyName);

            NumberPicker monthPicker = dialogView.findViewById(R.id.monthPicker);
            NumberPicker yearPicker = dialogView.findViewById(R.id.yearPicker);
            AllCompanyPieChartView pieChart = dialogView.findViewById(R.id.allCompanyPieChart);
            LinearLayout legendLayout = dialogView.findViewById(R.id.legendLayout);
            CheckBox cbFull = dialogView.findViewById(R.id.cbFull);
            CheckBox cbLate = dialogView.findViewById(R.id.cbLate);
            CheckBox cbEarly = dialogView.findViewById(R.id.cbEarly);
            CheckBox cbBoth = dialogView.findViewById(R.id.cbBoth);
            RecyclerView employeeRecyclerView = dialogView.findViewById(R.id.employeeRecyclerView);
            Button exportButton = dialogView.findViewById(R.id.exportButton);
            exportButton.setOnClickListener(v -> {
                int selMonth = monthPicker.getValue();
                int selYear = yearPicker.getValue();
                prepareExportCSV(selMonth, selYear);
            });
            employeeRecyclerView.setLayoutManager(new LinearLayoutManager(this) {
                @Override
                // =============== Core logic ===============
                public boolean canScrollVertically() {
                    return false;
                }
            });

            Calendar cal = Calendar.getInstance();
            int currYear = cal.get(Calendar.YEAR);
            int currMonth = cal.get(Calendar.MONTH) + 1;

            monthPicker.setMinValue(1);
            monthPicker.setMaxValue(12);
            monthPicker.setValue(currMonth);

            yearPicker.setMinValue(currYear - 10);
            yearPicker.setMaxValue(currYear + 10);
            yearPicker.setValue(currYear);

            Map<String, Integer> absentMap = new HashMap<>();
            Map<String, Integer> lateOnlyMap = new HashMap<>();
            Map<String, Integer> earlyOnlyMap = new HashMap<>();
            Map<String, Integer> bothMap = new HashMap<>();
            Map<String, Long> lateMinutesMap = new HashMap<>();
            Map<String, Long> earlyMinutesMap = new HashMap<>();

            Runnable loadData = () -> {
                try {
                    int selMonth = monthPicker.getValue();
                    int selYear = yearPicker.getValue();
                    String monthKey = String.format(Locale.getDefault(), "%04d-%02d", selYear, selMonth);
                    int totalDays = getWorkingDaysUpToToday(selYear, selMonth);

                    List<FaceData> companyEmployees = new ArrayList<>();
                    synchronized (faceDataList) {
                        for (FaceData faceData : faceDataList) {
                            if (managerCompanyName.equalsIgnoreCase(faceData.getCompanyName())) {
                                companyEmployees.add(faceData);
                            }
                        }
                    }

                    if (companyEmployees.isEmpty()) {
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivityCPP.this, "Không có nhân viên trong công ty.", Toast.LENGTH_SHORT).show();
                            pieChart.setAttendanceData(0, 0, 0, 0, 0);
                            updateAllCompanyLegend(legendLayout, 0, 0, 0, 0);
                            pieChart.invalidate();
                            employeeRecyclerView.setAdapter(null);
                        });
                        return;
                    }

                    absentMap.clear();
                    lateOnlyMap.clear();
                    earlyOnlyMap.clear();
                    bothMap.clear();
                    lateMinutesMap.clear();
                    earlyMinutesMap.clear();

                    final int[] absent = {0};
                    final int[] lateOnly = {0};
                    final int[] earlyOnly = {0};
                    final int[] bothLateAndEarly = {0};
                    final int[] processedEmployees = {0};

                    for (FaceData employee : companyEmployees) {
                        DatabaseReference attRef = FirebaseDatabase.getInstance().getReference("attendanceRecords")
                                .child(employee.getUserId());
                        attRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            // =============== Lifecycle callback for data change ===============
                            public void onDataChange(@NonNull DataSnapshot ds) {
                                int attendedDays = 0;
                                int empLate = 0;
                                int empEarly = 0;
                                int empBoth = 0;
                                long empLateMinutes = 0;
                                long empEarlyMinutes = 0;

                                for (DataSnapshot dayNode : ds.getChildren()) {
                                    String dayKey = dayNode.getKey();
                                    if (dayKey != null && dayKey.startsWith(monthKey)) {
                                        attendedDays++;
                                        AttendanceRecord r = dayNode.getValue(AttendanceRecord.class);
                                        if (r != null && r.getInTime() != 0 && r.getOutTime() != 0) {
                                            FaceData faceData = getFaceDataByUserId(r.getUserId());
                                            if (faceData != null) {
                                                long workStart = getWorkStartTime(dayKey, faceData.getWorkStartHour());
                                                long workEnd = getWorkEndTime(dayKey, faceData.getWorkEndHour());

                                                boolean isLate = r.getInTime() > workStart;
                                                boolean isEarly = r.getOutTime() < workEnd;

                                                if (isLate && isEarly) {
                                                    empBoth++;
                                                    empLateMinutes += (r.getInTime() - workStart) / 60000;
                                                    empEarlyMinutes += (workEnd - r.getOutTime()) / 60000;
                                                } else if (isLate) {
                                                    empLate++;
                                                    empLateMinutes += (r.getInTime() - workStart) / 60000;
                                                } else if (isEarly) {
                                                    empEarly++;
                                                    empEarlyMinutes += (workEnd - r.getOutTime()) / 60000;
                                                }
                                            }
                                        }
                                    }
                                }

                                int empAbsent = totalDays - attendedDays;
                                String userId = employee.getUserId();
                                synchronized (this) {
                                    absent[0] += empAbsent;
                                    lateOnly[0] += empLate;
                                    earlyOnly[0] += empEarly;
                                    bothLateAndEarly[0] += empBoth;

                                    if (empAbsent > 0) absentMap.put(userId, empAbsent);
                                    if (empLate > 0) lateOnlyMap.put(userId, empLate);
                                    if (empEarly > 0) earlyOnlyMap.put(userId, empEarly);
                                    if (empBoth > 0) bothMap.put(userId, empBoth);
                                    if (empLateMinutes > 0) lateMinutesMap.put(userId, empLateMinutes);
                                    if (empEarlyMinutes > 0) earlyMinutesMap.put(userId, empEarlyMinutes);

                                    processedEmployees[0]++;

                                    if (processedEmployees[0] == companyEmployees.size()) {
                                        runOnUiThread(() -> {
                                            pieChart.setAttendanceData(absent[0], lateOnly[0], earlyOnly[0], bothLateAndEarly[0],
                                                    absent[0] + lateOnly[0] + earlyOnly[0] + bothLateAndEarly[0]);
                                            updateAllCompanyLegend(legendLayout, absent[0], lateOnly[0], earlyOnly[0], bothLateAndEarly[0]);
                                            pieChart.invalidate();
                                            showFilteredEmployeeList(employeeRecyclerView, cbFull.isChecked(), cbLate.isChecked(), cbEarly.isChecked(), cbBoth.isChecked(),
                                                    absentMap, lateOnlyMap, earlyOnlyMap, bothMap, lateMinutesMap, earlyMinutesMap);
                                        });
                                    }
                                }
                            }

                            @Override
                            // =============== Lifecycle callback for cancelled ===============
                            public void onCancelled(@NonNull DatabaseError e) {
                                runOnUiThread(() -> Toast.makeText(MainActivityCPP.this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e("ModalError", "Error loading data: " + e.getMessage(), e);
                    runOnUiThread(() -> Toast.makeText(MainActivityCPP.this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            };

            CompoundButton.OnCheckedChangeListener listener = (button, isChecked) -> {
                showFilteredEmployeeList(employeeRecyclerView, cbFull.isChecked(), cbLate.isChecked(), cbEarly.isChecked(), cbBoth.isChecked(),
                        absentMap, lateOnlyMap, earlyOnlyMap, bothMap, lateMinutesMap, earlyMinutesMap);
            };

            cbFull.setOnCheckedChangeListener(listener);
            cbLate.setOnCheckedChangeListener(listener);
            cbEarly.setOnCheckedChangeListener(listener);
            cbBoth.setOnCheckedChangeListener(listener);

            monthPicker.setOnValueChangedListener((picker, oldVal, newVal) -> loadData.run());
            yearPicker.setOnValueChangedListener((picker, oldVal, newVal) -> loadData.run());
            loadData.run();

            builder.setNegativeButton("Đóng", null);
            builder.show();
        } catch (Exception e) {
            Log.e("ModalError", "Failed to show modal: " + e.getMessage(), e);
            Toast.makeText(this, "Lỗi hiển thị thống kê: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // =============== Updates all company legend ===============
    private void updateAllCompanyLegend(LinearLayout layout, int absent, int late, int early, int both) {
        layout.removeAllViews();
        addAllCompanyLegendItem(layout, "Nghỉ", Color.parseColor("#FFB6C1"), absent);
        addAllCompanyLegendItem(layout, "Đi trễ", Color.parseColor("#DDA0DD"), late);
        addAllCompanyLegendItem(layout, "Về sớm", Color.parseColor("#ADD8E6"), early);
        addAllCompanyLegendItem(layout, "Đi trễ về sớm", Color.parseColor("#FFA500"), both);
    }

    // =============== Displays filtered employee list ===============
    private void showFilteredEmployeeList(RecyclerView recyclerView, boolean showFull, boolean showLate, boolean showEarly, boolean showBoth,
                                          Map<String, Integer> absentMap, Map<String, Integer> lateMap, Map<String, Integer> earlyMap, Map<String, Integer> bothMap,
                                          Map<String, Long> lateMinutesMap, Map<String, Long> earlyMinutesMap) {
        if (!showFull && !showLate && !showEarly && !showBoth) {
            recyclerView.setAdapter(null);
            return;
        }

        Set<String> userIds = new HashSet<>();
        if (showFull && absentMap != null) userIds.addAll(absentMap.keySet());
        if (showLate && lateMap != null) userIds.addAll(lateMap.keySet());
        if (showEarly && earlyMap != null) userIds.addAll(earlyMap.keySet());
        if (showBoth && bothMap != null) userIds.addAll(bothMap.keySet());

        String maxAbsentUserId = null;
        int maxAbsentCount = 0;
        String maxLateUserId = null;
        int maxLateCount = 0;
        String maxEarlyUserId = null;
        int maxEarlyCount = 0;
        String maxBothUserId = null;
        int maxBothCount = 0;

        List<Map<String, Object>> employeeDataList = new ArrayList<>();
        for (String userId : userIds) {
            StringBuilder details = new StringBuilder();
            String name = getNameByUid(userId);
            if (name == null) name = "Không rõ (ID: " + userId + ")";

            if (showFull && absentMap != null && absentMap.containsKey(userId)) {
                int count = absentMap.get(userId);
                details.append("Nghỉ: ").append(count).append(" ngày\n");
                if (count > maxAbsentCount) {
                    maxAbsentCount = count;
                    maxAbsentUserId = userId;
                }
            }
            if (showLate && lateMap != null && lateMap.containsKey(userId)) {
                int count = lateMap.get(userId);
                long lateMinutes = lateMinutesMap.getOrDefault(userId, 0L);
                details.append("Đi trễ: ").append(count).append(" lần (").append(lateMinutes).append(" phút)\n");
                if (count > maxLateCount) {
                    maxLateCount = count;
                    maxLateUserId = userId;
                }
            }
            if (showEarly && earlyMap != null && earlyMap.containsKey(userId)) {
                int count = earlyMap.get(userId);
                long earlyMinutes = earlyMinutesMap.getOrDefault(userId, 0L);
                details.append("Về sớm: ").append(count).append(" lần (").append(earlyMinutes).append(" phút)\n");
                if (count > maxEarlyCount) {
                    maxEarlyCount = count;
                    maxEarlyUserId = userId;
                }
            }
            if (showBoth && bothMap != null && bothMap.containsKey(userId)) {
                int count = bothMap.get(userId);
                long lateMinutes = lateMinutesMap.getOrDefault(userId, 0L);
                long earlyMinutes = earlyMinutesMap.getOrDefault(userId, 0L);
                details.append("Đi trễ về sớm: ").append(count).append(" lần (").append(lateMinutes).append(" phút trễ, ").append(earlyMinutes).append(" phút sớm)\n");
                if (count > maxBothCount) {
                    maxBothCount = count;
                    maxBothUserId = userId;
                }
            }

            if (details.length() > 0) {
                SpannableStringBuilder spannable = new SpannableStringBuilder();
                spannable.append(name + ":\n");
                spannable.setSpan(new StyleSpan(Typeface.BOLD), 0, name.length() + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                spannable.append(details.toString().trim());

                Map<String, Object> employeeData = new HashMap<>();
                employeeData.put("spannable", spannable);
                employeeData.put("userId", userId);
                employeeData.put("absentCount", absentMap != null && absentMap.containsKey(userId) ? absentMap.get(userId) : 0);
                employeeData.put("lateCount", lateMap != null && lateMap.containsKey(userId) ? lateMap.get(userId) : 0);
                employeeData.put("earlyCount", earlyMap != null && earlyMap.containsKey(userId) ? earlyMap.get(userId) : 0);
                employeeData.put("bothCount", bothMap != null && bothMap.containsKey(userId) ? bothMap.get(userId) : 0);
                employeeData.put("lateMinutes", lateMinutesMap.getOrDefault(userId, 0L));
                employeeData.put("earlyMinutes", earlyMinutesMap.getOrDefault(userId, 0L));
                employeeDataList.add(employeeData);
            }
        }

        if (employeeDataList.isEmpty()) {
            recyclerView.setAdapter(null);
            Toast.makeText(this, "Không có dữ liệu cho bộ lọc đã chọn.", Toast.LENGTH_SHORT).show();
            return;
        }

        Collections.sort(employeeDataList, (e1, e2) -> {
            int absent1 = (int) e1.get("absentCount");
            int absent2 = (int) e2.get("absentCount");
            int late1 = (int) e1.get("lateCount");
            int late2 = (int) e2.get("lateCount");
            int early1 = (int) e1.get("earlyCount");
            int early2 = (int) e2.get("earlyCount");
            int both1 = (int) e1.get("bothCount");
            int both2 = (int) e2.get("bothCount");

            if (absent1 != absent2) return Integer.compare(absent2, absent1);
            if (late1 != late2) return Integer.compare(late2, late1);
            if (early1 != early2) return Integer.compare(early2, early1);
            if (both1 != both2) return Integer.compare(both2, both1);

            SpannableStringBuilder s1 = (SpannableStringBuilder) e1.get("spannable");
            SpannableStringBuilder s2 = (SpannableStringBuilder) e2.get("spannable");
            return s1.toString().compareTo(s2.toString());
        });

        List<Map<String, Object>> displayList = new ArrayList<>();
        for (Map<String, Object> employeeData : employeeDataList) {
            String userId = (String) employeeData.get("userId");
            int backgroundColor = 0;

            if (showFull && maxAbsentUserId != null && userId.equals(maxAbsentUserId)) {
                backgroundColor = Color.parseColor("#FFB6C1");
            } else if (showLate && maxLateUserId != null && userId.equals(maxLateUserId)) {
                backgroundColor = Color.parseColor("#DDA0DD");
            } else if (showEarly && maxEarlyUserId != null && userId.equals(maxEarlyUserId)) {
                backgroundColor = Color.parseColor("#ADD8E6");
            } else if (showBoth && maxBothUserId != null && userId.equals(maxBothUserId)) {
                backgroundColor = Color.parseColor("#FFA500");
            }

            Map<String, Object> displayItem = new HashMap<>();
            displayItem.put("spannable", employeeData.get("spannable"));
            displayItem.put("backgroundColor", backgroundColor);
            displayList.add(displayItem);
        }

        EmployeeAdapter adapter = new EmployeeAdapter(displayList);
        recyclerView.setAdapter(adapter);
        recyclerView.requestLayout();
    }

    // =============== Exports attendance to c s v ===============
    private void exportAttendanceToCSV(List<FaceData> employees, String monthKey, String filter) {
        String companyName = managerCompanyName;
        String fileName = "Attendance_" + companyName + "_" + monthKey + ".csv";
        File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName);


        String[] parts = monthKey.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);


        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, 1);
        int maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        List<String> days = new ArrayList<>();
        for (int i = 1; i <= maxDay; i++) {
            days.add(String.format("%02d", i));
        }


        Calendar now = Calendar.getInstance();
        if (year == now.get(Calendar.YEAR) && month == now.get(Calendar.MONTH) + 1) {
            int currentDay = now.get(Calendar.DAY_OF_MONTH);
            days.clear();
            for (int i = 1; i <= currentDay; i++) {
                days.add(String.format("%02d", i));
            }
        }

        int totalDays = days.size();

        Map<String, Map<String, AttendanceRecord>> allRecords = new ConcurrentHashMap<>();
        processBatches(employees, 10, days, monthKey, allRecords, 0, () -> {
            try (OutputStreamWriter writer = new OutputStreamWriter(
                    new FileOutputStream(file), StandardCharsets.UTF_8)) {

                writer.write("\uFEFF");


                String title = "Thống kê chấm công tháng " + monthKey + " - Công ty: " + companyName;
                writer.append(title).append("\n");


                writer.append("STT,Tên,Số ngày nghỉ,Tổng số ca đi làm,Tổng phút đi trễ,Tổng phút về sớm");
                for (String day : days) {
                    writer.append(",").append(day);
                }
                writer.append("\n");


                for (int i = 0; i < employees.size(); i++) {
                    FaceData employee = employees.get(i);
                    String userId = employee.getUserId();
                    Map<String, AttendanceRecord> userRecords = allRecords.get(userId);

                    int workingShifts = 0;
                    long totalLateMinutes = 0;
                    long totalEarlyMinutes = 0;


                    for (String day : days) {
                        String date = monthKey + "-" + day;
                        AttendanceRecord record = userRecords.get(date);
                        if (record != null && record.getInTime() != 0 && record.getOutTime() != 0) {
                            workingShifts++;
                            long workStart = getWorkStartTime(date, employee.getWorkStartHour());
                            long workEnd = getWorkEndTime(date, employee.getWorkEndHour());
                            if (record.getInTime() > workStart) {
                                totalLateMinutes += (record.getInTime() - workStart) / 60000;
                            }
                            if (record.getOutTime() < workEnd) {
                                totalEarlyMinutes += (workEnd - record.getOutTime()) / 60000;
                            }
                        }
                    }

                    int absentDays = totalDays - workingShifts;


                    writer.append(String.valueOf(i + 1)).append(",")
                            .append(employee.getName()).append(",")
                            .append(String.valueOf(absentDays)).append(",")
                            .append(String.valueOf(workingShifts)).append(",")
                            .append(String.valueOf(totalLateMinutes)).append(",")
                            .append(String.valueOf(totalEarlyMinutes));


                    for (String day : days) {
                        String date = monthKey + "-" + day;
                        AttendanceRecord record = userRecords.get(date);
                        String status = getStatusForFilter(record, filter);
                        writer.append(",").append(status);
                    }
                    writer.append("\n");
                }

                Toast.makeText(this, "File CSV đã được lưu tại: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                Log.e(TAG, "Lỗi xuất file CSV: " + e.getMessage());
                Toast.makeText(this, "Lỗi xuất file: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
    // =============== Prepares export c s v ===============
    private void prepareExportCSV(int month, int year) {
        String monthKey = String.format("%d-%02d", year, month);
        String filter = "Tất cả";
        List<FaceData> employees = new ArrayList<>();
        synchronized (faceDataList) {
            for (FaceData faceData : faceDataList) {
                if (managerCompanyName != null && managerCompanyName.equalsIgnoreCase(faceData.getCompanyName())) {
                    employees.add(faceData);
                }
            }
        }
        if (employees.isEmpty()) {
            Toast.makeText(this, "Không có nhân viên để xuất dữ liệu.", Toast.LENGTH_SHORT).show();
            return;
        }
        exportAttendanceToCSV(employees, monthKey, filter);
    }

    // =============== Processes batches ===============
    private void processBatches(List<FaceData> employees, int batchSize, List<String> days, String monthKey,
                                Map<String, Map<String, AttendanceRecord>> allRecords, int batchIndex, Runnable onComplete) {
        int start = batchIndex * batchSize;
        if (start >= employees.size()) {

            onComplete.run();
            return;
        }

        List<FaceData> batch = employees.subList(start, Math.min(start + batchSize, employees.size()));
        List<Task<?>> tasks = new ArrayList<>();

        for (FaceData employee : batch) {
            String userId = employee.getUserId();
            Map<String, AttendanceRecord> userRecords = new ConcurrentHashMap<>();
            allRecords.put(userId, userRecords);

            for (String day : days) {
                String date = monthKey + "-" + day;
                DatabaseReference attRef = FirebaseDatabase.getInstance().getReference("attendanceRecords")
                        .child(userId).child(date);
                Task<DataSnapshot> task = attRef.get().addOnCompleteListener(taskSnapshot -> {
                    if (taskSnapshot.isSuccessful()) {
                        DataSnapshot snapshot = taskSnapshot.getResult();
                        AttendanceRecord record = snapshot.getValue(AttendanceRecord.class);
                        if (record != null) {
                            userRecords.put(date, record);
                        } else {
                            Log.d(TAG, "Không có dữ liệu chấm công cho userId: " + userId + ", date: " + date);
                        }
                    } else {
                        Exception e = taskSnapshot.getException();
                        Log.e(TAG, "Lỗi tải dữ liệu chấm công cho userId: " + userId + ", date: " + date + ", lỗi: " +
                                (e != null ? e.getMessage() : "Không có chi tiết lỗi"));

                    }
                });
                tasks.add(task);
            }
        }


        Tasks.whenAllComplete(tasks).addOnCompleteListener(task -> {
            processBatches(employees, batchSize, days, monthKey, allRecords, batchIndex + 1, onComplete);
        });
    }


    // =============== Requests storage permission ===============
    private void requestStoragePermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2002);
        }
    }

    // =============== Gets status for filter ===============
    private String getStatusForFilter(AttendanceRecord record, String filter) {
        if (record == null) {
            return "Nghỉ";
        }
        boolean isLate = record.isLate();
        boolean isEarly = record.isEarlyLeave();
        switch (filter) {
            case "Tất cả":
                if (isLate && isEarly) return "Đi trễ về sớm";
                if (isLate) return "Đi trễ";
                if (isEarly) return "Về sớm";
                return "Đúng giờ";
            case "Đi trễ":
                return isLate ? "Đi trễ" : "";
            case "Về sớm":
                return isEarly ? "Về sớm" : "";
            case "Đi trễ về sớm":
                return (isLate && isEarly) ? "Đi trễ về sớm" : "";
            default:
                return "";
        }
    }

    private static class EmployeeAdapter extends RecyclerView.Adapter<EmployeeAdapter.ViewHolder> {
        private final List<Map<String, Object>> data;

        EmployeeAdapter(List<Map<String, Object>> data) {
            this.data = data;
            Log.d("AdapterDebug", "Số lượng mục trong adapter: " + data.size());
        }

        @NonNull
        @Override
        // =============== Lifecycle callback for create view holder ===============
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView textView = new TextView(parent.getContext());
            textView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            textView.setPadding(16, 8, 16, 8);
            textView.setTextSize(16);
            return new ViewHolder(textView);
        }

        @Override
        // =============== Lifecycle callback for bind view holder ===============
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Map<String, Object> item = data.get(position);
            SpannableStringBuilder spannable = (SpannableStringBuilder) item.get("spannable");
            int backgroundColor = (int) item.get("backgroundColor");

            holder.textView.setText(spannable);
            if (backgroundColor != 0) {
                holder.textView.setBackgroundColor(backgroundColor);
            } else {
                holder.textView.setBackgroundColor(Color.TRANSPARENT);
            }

            Log.d("AdapterDebug", "Hiển thị mục tại vị trí " + position + ": " + spannable.toString());
        }

        @Override
        // =============== Gets item count ===============
        public int getItemCount() {
            return data.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final TextView textView;

            ViewHolder(@NonNull TextView textView) {
                super(textView);
                this.textView = textView;
            }
        }
    }


    // =============== Core logic ===============
    private void addAllCompanyLegendItem(LinearLayout layout, String label, int color, int count) {
        LinearLayout itemLayout = new LinearLayout(this);
        itemLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemLayout.setPadding(0, dp(4), 0, dp(4));

        View colorView = new View(this);
        LinearLayout.LayoutParams colorParams = new LinearLayout.LayoutParams(dp(16), dp(16));
        colorParams.setMargins(0, 0, dp(8), 0);
        colorView.setLayoutParams(colorParams);
        colorView.setBackgroundColor(color);

        TextView textView = new TextView(this);
        textView.setText(label + ": " + count);
        textView.setTextSize(14);

        itemLayout.addView(colorView);
        itemLayout.addView(textView);
        layout.addView(itemLayout);
    }
}