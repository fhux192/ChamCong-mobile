// MainActivityCPP.java

package com.example.timekeeping;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.android.gms.location.*;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivityCPP extends CameraActivity {

    private static final String TAG = "MainActivityCPP";
    private CameraBridgeViewBase mOpenCvCameraView;
    private static final int GALLERY_REQUEST_CODE = 1001;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 2001;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 2003;

    Button galleryBtn;
    Button showFacesBtn;
    Button resetRecognitionBtn;
    Button showChecksBtn;
    Button signInBtn;
    ImageView imageView;
    Button signOutBtn;

    // =============== LIST FACE & MAP CHECK ===============
    // Để tránh lỗi concurrent modification, ta cần đồng bộ
    // khi thêm/xoá/sửa faceDataList ở ChildEventListener
    private final List<FaceData> faceDataList = new ArrayList<>();
    private DatabaseReference faceDataRef;

    private final Map<String, CheckData> checkDataMap = new HashMap<>();
    private DatabaseReference checkDataRef;

    // =============== LOCATION ===============
    private FusedLocationProviderClient fusedLocationClient;
    private volatile Location currentLocation;
    private LocationCallback locationCallback;

    private AtomicBoolean isDialogVisible = new AtomicBoolean(false);
    private Set<String> canceledFaces = ConcurrentHashMap.newKeySet();

    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private float recognitionThreshold = 0.5f;

    // =============== FIREBASE AUTH ===============
    private FirebaseAuth mAuth;
    private String currentUserId = null;

    // =============== SHAREDPREF ===============
    private static final String PREFS_NAME = "MyPrefs";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_REMEMBER_ME = "remember_me";

    // =============== Biến lưu trữ companyName của Manager ===============
    private String managerCompanyName = null;

    static {
        if (!OpenCVLoader.initDebug()) {
            Log.e(TAG, "Cannot connect to OpenCV Manager");
        } else {
            System.loadLibrary("app");
        }
    }

    // =============== HÀM NATIVE ===============
    public native void InitFaceDetector(String modelPath);
    public native int DetectFaces(long matAddrGray, long matAddrRgba, float[] largestFaceRect);
    public native void InitFaceRecognition(String modelPath);
    public native float[] ExtractFaceEmbedding(long matAddr);
    public native float CalculateSimilarity(float[] emb1, float[] emb2);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Bật Persistence cho Firebase
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
            Log.d(TAG, "Firebase persistence enabled.");
        } catch (DatabaseException e) {
            Log.w(TAG, "Persistence already enabled.");
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main_cpp);

        // Khởi tạo Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        }
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_FULLSCREEN);
        mOpenCvCameraView = findViewById(R.id.opencv_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.GONE);
        mOpenCvCameraView.setCvCameraViewListener(cvCameraViewListener2);
        mOpenCvCameraView.setMaxFrameSize(640, 600);

        imageView = findViewById(R.id.imageView);

        galleryBtn = findViewById(R.id.galleryBtn);
        showFacesBtn = findViewById(R.id.showFacesBtn);
        resetRecognitionBtn = findViewById(R.id.resetRecognitionBtn);
        showChecksBtn = findViewById(R.id.showCheckedListBtn);
        signOutBtn = findViewById(R.id.signOutBtn);
        signInBtn = findViewById(R.id.signInBtn);

        galleryBtn.setVisibility(View.GONE);
        showFacesBtn.setVisibility(View.GONE);

        galleryBtn.setOnClickListener(view -> openGallery());
        showFacesBtn.setOnClickListener(view -> displayRegisteredFaces());
        resetRecognitionBtn.setOnClickListener(view -> resetCanceledFaces());
        showChecksBtn.setOnClickListener(view -> displaySavedChecks());

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                for (Location location : locationResult.getLocations()) {
                    currentLocation = location;
                    Log.d(TAG, "Location updated: " + location.getLatitude() + ", " + location.getLongitude());
                }
            }
        };

        // Tải ngưỡng so sánh tương tự
        loadRecognitionThreshold();

        // Yêu cầu permission camera & location
        requestPermissions();

        // Tham chiếu đến "faceDataList" trên Firebase
        faceDataRef = FirebaseDatabase.getInstance().getReference("faceDataList");
        Log.d(TAG, "Firebase Database reference initialized.");
        loadFaceDataList();

        // Tham chiếu đến "checkDataList"
        checkDataRef = FirebaseDatabase.getInstance().getReference("checkDataList");
        Log.d(TAG, "Firebase Database reference for check data initialized.");
        loadCheckDataList();

        // Kiểm tra role user, ẩn/hiện button
        checkUserRoleAndAdjustUI();

        signOutBtn.setOnClickListener(view -> handleSignOut());
        signInBtn.setOnClickListener(view -> handleSignIn());

        // Đã loại bỏ loadCompanies() vì không cần thiết khi không sử dụng Spinner
    }

    //==================================================
    //   TẠO DIALOG CHỌN TÊN & VỊ TRÍ
    //==================================================
    private void showNameInputDialog(List<float[]> embeddings) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivityCPP.this);
        builder.setTitle("Nhập tên và vị trí làm việc");

        LinearLayout layout = new LinearLayout(MainActivityCPP.this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        // Input cho tên
        final EditText nameInput = new EditText(MainActivityCPP.this);
        nameInput.setHint("Tên");
        layout.addView(nameInput);

        // Input cho Vĩ độ
        final EditText latitudeInput = new EditText(MainActivityCPP.this);
        latitudeInput.setHint("Vĩ độ (ví dụ: 21.028511)");
        latitudeInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        layout.addView(latitudeInput);

        // Input cho Kinh độ
        final EditText longitudeInput = new EditText(MainActivityCPP.this);
        longitudeInput.setHint("Kinh độ (ví dụ: 105.804817)");
        longitudeInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        layout.addView(longitudeInput);

        builder.setView(layout);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String name = nameInput.getText().toString().trim();
            String latitudeStr = latitudeInput.getText().toString().trim();
            String longitudeStr = longitudeInput.getText().toString().trim();
            String companyName = managerCompanyName; // Sử dụng tên công ty của Manager

            if (!name.isEmpty() && !latitudeStr.isEmpty() && !longitudeStr.isEmpty()
                    && companyName != null && !companyName.isEmpty()) {
                try {
                    double latitude = Double.parseDouble(latitudeStr);
                    double longitude = Double.parseDouble(longitudeStr);
                    handleNameAndCompanyInput(name, latitude, longitude, embeddings);
                } catch (NumberFormatException e) {
                    Toast.makeText(MainActivityCPP.this,
                            "Vui lòng nhập vĩ độ và kinh độ hợp lệ.",
                            Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Invalid latitude or longitude input.");
                }
            } else {
                Toast.makeText(MainActivityCPP.this,
                        "Tên, vĩ độ và kinh độ không được để trống.",
                        Toast.LENGTH_SHORT).show();
                Log.e(TAG,
                        "User attempted to save face data without entering required fields.");
            }
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void handleNameAndCompanyInput(String name, double latitude, double longitude, List<float[]> embeddings) {
        String companyName = managerCompanyName; // Lấy tên công ty từ Manager

        // Ta phải đồng bộ khi duyệt faceDataList
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
            createNewUserWithEmbeddingsAndCompany(name, companyName, latitude, longitude, embeddings);
        }
    }

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

    private void createNewUserWithEmbeddingsAndCompany(String name, String companyName, double latitude, double longitude, List<float[]> embeddings) {
        String userId = UUID.randomUUID().toString();
        List<List<Float>> embeddingsList = new ArrayList<>();
        for (float[] embedding : embeddings) {
            embeddingsList.add(floatArrayToList(embedding));
        }

        FaceData faceData = new FaceData(userId, name, companyName, embeddingsList, latitude, longitude);
        saveFaceDataToFirebase(faceData);
        Toast.makeText(MainActivityCPP.this, "Dữ liệu khuôn mặt đã được lưu.", Toast.LENGTH_SHORT).show();
    }

    private List<Float> floatArrayToList(float[] array) {
        List<Float> list = new ArrayList<>();
        for (float val : array) {
            list.add(val);
        }
        return list;
    }

    //===================================================
    //   onCreateView => onCameraFrame => DÙNG SNAPSHOT
    //===================================================
    private CameraBridgeViewBase.CvCameraViewListener2 cvCameraViewListener2 =
            new CameraBridgeViewBase.CvCameraViewListener2() {

                @Override
                public void onCameraViewStarted(int width, int height) {
                    Log.d(TAG, "Camera view started with width: " + width + " and height: " + height);
                }

                @Override
                public void onCameraViewStopped() {
                    Log.d(TAG, "Camera view stopped.");
                }

                @Override
                public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
                    long startTime = System.nanoTime();

                    Mat inputRgba = inputFrame.rgba();
                    Mat inputGray = inputFrame.gray();

                    int MAX_FACES = 10;
                    float[] faceRects = new float[4 * MAX_FACES];

                    // Phát hiện khuôn mặt
                    int numFaces = DetectFaces(inputGray.getNativeObjAddr(), inputRgba.getNativeObjAddr(), faceRects);
                    Log.d(TAG, "Detected " + numFaces + " face(s).");

                    // Tạo snapshot an toàn để tránh lỗi concurrent modification
                    List<FaceData> snapshot;
                    synchronized (faceDataList) {
                        snapshot = new ArrayList<>(faceDataList);
                    }

                    for (int i = 0; i < numFaces; i++) {
                        float x = faceRects[i * 4];
                        float y = faceRects[i * 4 + 1];
                        float w = faceRects[i * 4 + 2];
                        float h = faceRects[i * 4 + 3];

                        // Vẽ bounding box với độ dày đồng nhất
                        Scalar boxColor = new Scalar(0, 205, 0); // Màu xanh lá
                        int boxThickness = 3;
                        Imgproc.rectangle(inputRgba, new Point(x, y), new Point(x + w, y + h), boxColor, boxThickness);

                        // Trích xuất embedding và nhận diện
                        float[] cameraFrameEmbedding = ExtractFaceEmbedding(inputRgba.getNativeObjAddr());
                        String matchedName = "Unknown";
                        float highestSimilarity = 0.0f;
                        String matchedUserId = null;

                        if (cameraFrameEmbedding != null && !snapshot.isEmpty()) {
                            for (FaceData faceData : snapshot) {
                                if (faceData.getEmbeddings() == null || faceData.getEmbeddings().isEmpty())
                                    continue;

                                for (List<Float> embeddingList : faceData.getEmbeddings()) {
                                    float[] embeddingArray = new float[embeddingList.size()];
                                    for (int j = 0; j < embeddingList.size(); j++) {
                                        embeddingArray[j] = embeddingList.get(j);
                                    }

                                    float similarity = CalculateSimilarity(embeddingArray, cameraFrameEmbedding);
                                    if (similarity > recognitionThreshold && similarity > highestSimilarity) {
                                        highestSimilarity = similarity;
                                        matchedName = faceData.getName();
                                        matchedUserId = faceData.getUserId();
                                    }
                                }
                            }
                        }

                        if (!matchedName.equals("Unknown") && matchedUserId != null) {
                            // Cập nhật currentUserId nếu cần
                            if (!matchedUserId.equals(currentUserId)) {
                                currentUserId = matchedUserId;
                                Log.d(TAG, "Current user ID set to: " + currentUserId);
                            }

                            // Kiểm tra và hiển thị hộp thoại nhận diện
                            if (!canceledFaces.contains(matchedName)) {
                                if (isDialogVisible.compareAndSet(false, true)) {
                                    final String recognizedName = matchedName;
                                    runOnUiThread(() -> showRecognitionDialog(recognizedName));
                                }
                            }

                            // Chuẩn bị hiển thị tên người nhận diện
                            String displayName = matchedName;
                            int fontFace = Imgproc.FONT_HERSHEY_SIMPLEX;
                            double fontScale = 0.6;
                            int thickness = 2;

                            // Tính kích thước text
                            int[] baseline = new int[1];
                            Size textSize = Imgproc.getTextSize(displayName, fontFace, fontScale, thickness, baseline);

                            // Định nghĩa padding và kích thước nền
                            int padding = 4;
                            int textWidth = (int) textSize.width;
                            int textHeight = (int) textSize.height;

                            // Đảm bảo nền chữ không vượt quá khung hình
                            int bgX1 = (int) x;
                            int bgY2 = (int) y - padding; // Đỉnh của nền nằm ngay trên bounding box
                            int bgX2 = bgX1 + textWidth + padding * 2;
                            int bgY1 = bgY2 - textHeight - padding; // Đáy của nền nằm phía trên

                            if (bgX2 > inputRgba.cols()) {
                                bgX2 = inputRgba.cols() - 10;
                                bgX1 = bgX2 - textWidth - padding * 2;
                            }
                            if (bgY1 < 0) { // Đảm bảo nền không vượt ra ngoài phía trên khung hình
                                bgY1 = 10;
                                bgY2 = bgY1 + textHeight + padding;
                            }

                            // Vẽ nền cho tên (màu xanh lá với độ trong suốt)
                            Imgproc.rectangle(inputRgba, new Point(bgX1, bgY1), new Point(bgX2, bgY2),
                                    new Scalar(0, 205, 0), Imgproc.FILLED);

                            // Vẽ tên lên nền
                            Imgproc.putText(inputRgba, displayName, new Point(bgX1 + padding, bgY2 - padding + 1),
                                    fontFace, fontScale, new Scalar(255, 255, 255), thickness);
                        } else {
                            // Nếu không tìm thấy người dùng phù hợp, hiển thị "Unknown"
                            String displayName = "Unknown";
                            int fontFace = Imgproc.FONT_HERSHEY_SIMPLEX;
                            double fontScale = 0.6;
                            int thickness = 2;

                            // Tính kích thước text
                            int[] baseline = new int[1];
                            Size textSize = Imgproc.getTextSize(displayName, fontFace, fontScale, thickness, baseline);

                            // Định nghĩa padding và kích thước nền
                            int padding = 4;
                            int textWidth = (int) textSize.width;
                            int textHeight = (int) textSize.height;

                            // Đảm bảo nền chữ không vượt quá khung hình
                            int bgX1 = (int) x;
                            int bgY2 = (int) y - padding; // Đỉnh của nền nằm ngay trên bounding box
                            int bgX2 = bgX1 + textWidth + padding * 2;
                            int bgY1 = bgY2 - textHeight - (padding + 2); // Đáy của nền nằm phía trên

                            if (bgX2 > inputRgba.cols()) {
                                bgX2 = inputRgba.cols() - 10;
                                bgX1 = bgX2 - textWidth - padding * 2;
                            }
                            if (bgY1 < 0) { // Đảm bảo nền không vượt ra ngoài phía trên khung hình
                                bgY1 = 10;
                                bgY2 = bgY1 + textHeight + padding;
                            }

                            Imgproc.rectangle(inputRgba, new Point(bgX1, bgY1), new Point(bgX2, bgY2),
                                    new Scalar(250, 0, 0), Imgproc.FILLED);

                            // Vẽ "Unknown" lên nền
                            Imgproc.putText(inputRgba, displayName, new Point(bgX1 + padding, bgY2 - padding + 1),
                                    fontFace, fontScale, new Scalar(255, 255, 255), thickness);
                        }
                    }

                    // Vẽ mask (nếu cần thiết, giữ nguyên phần này nếu bạn muốn)
                    // ... (Phần mã vẽ mask không thay đổi)

                    long endTime = System.nanoTime();
                    long processingTimeMs = (endTime - startTime) / 1_000_000;
                    Log.d(TAG, "Frame processed in " + processingTimeMs + " ms");

                    return inputRgba;
                }
            };

    //=================================================
    //   DIALOG NHẬN DIỆN
    //=================================================
    private void showRecognitionDialog(String recognizedName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivityCPP.this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_recognition, null);
        builder.setView(dialogView);

        TextView nameTextView = dialogView.findViewById(R.id.recognizedNameTextView);
        TextView locationTextView = dialogView.findViewById(R.id.locationTextView);
        TextView timeTextView = dialogView.findViewById(R.id.timeTextView);

        nameTextView.setText("Phát hiện: " + recognizedName);

        String locationText;
        if (currentLocation != null) {
            locationText = String.format(Locale.getDefault(),
                    "Vị trí: Lat %.5f, Lon %.5f",
                    currentLocation.getLatitude(),
                    currentLocation.getLongitude());
        } else {
            locationText = "Vị trí: Không có thông tin";
        }
        locationTextView.setText(locationText);

        updateTimeTextView(timeTextView);

        Handler timeHandler = new Handler();
        Runnable timeUpdater = new Runnable() {
            @Override
            public void run() {
                updateTimeTextView(timeTextView);
                timeHandler.postDelayed(this, 1000);
            }
        };
        timeHandler.post(timeUpdater);

        builder.setPositiveButton("Chấm công", (dialog, which) -> {
            if (currentUserId == null) {
                Toast.makeText(MainActivityCPP.this, "Không xác định được ID người dùng.", Toast.LENGTH_LONG).show();
                isDialogVisible.set(false);
                timeHandler.removeCallbacks(timeUpdater);
                return;
            }

            FaceData faceData = getFaceDataByName(recognizedName);
            if (faceData == null) {
                Toast.makeText(MainActivityCPP.this, "Không tìm thấy thông tin người dùng.", Toast.LENGTH_LONG).show();
                isDialogVisible.set(false);
                timeHandler.removeCallbacks(timeUpdater);
                return;
            }

            // Kiểm tra vị trí
            if (currentLocation == null) {
                Toast.makeText(MainActivityCPP.this, "Không có thông tin vị trí hiện tại.", Toast.LENGTH_LONG).show();
                isDialogVisible.set(false);
                timeHandler.removeCallbacks(timeUpdater);
                return;
            }

            double officeLat = faceData.getOfficeLatitude();
            double officeLon = faceData.getOfficeLongitude();

            if (!isWithinRange(currentLocation.getLatitude(), currentLocation.getLongitude(), officeLat, officeLon, 20f)) {
                Toast.makeText(MainActivityCPP.this, "Vị trí của bạn không nằm trong khu vực phòng làm việc của bạn.", Toast.LENGTH_LONG).show();
                isDialogVisible.set(false);
                timeHandler.removeCallbacks(timeUpdater);
                return;
            }

            String checkId = checkDataRef.push().getKey();
            if (checkId == null) {
                Toast.makeText(MainActivityCPP.this, "Lỗi tạo ID check.", Toast.LENGTH_LONG).show();
                isDialogVisible.set(false);
                timeHandler.removeCallbacks(timeUpdater);
                return;
            }

            // Lấy companyName từ FaceData
            String companyName = faceData.getCompanyName();
            if (companyName == null || companyName.isEmpty()) {
                Toast.makeText(MainActivityCPP.this, "Không tìm thấy thông tin công ty.", Toast.LENGTH_LONG).show();
                isDialogVisible.set(false);
                timeHandler.removeCallbacks(timeUpdater);
                return;
            }

            long currentTimeMillis = System.currentTimeMillis();
            CheckData checkData = new CheckData(currentUserId, recognizedName, locationText, currentTimeMillis, companyName);
            saveCheckDataToFirebase(checkId, checkData);

            canceledFaces.add(recognizedName);

            Toast.makeText(MainActivityCPP.this, "Check thành công cho " + recognizedName, Toast.LENGTH_SHORT).show();
            isDialogVisible.set(false);
            timeHandler.removeCallbacks(timeUpdater);
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> {
            canceledFaces.add(recognizedName);
            //Toast.makeText(MainActivityCPP.this, "Nhận diện đã bị hủy.", Toast.LENGTH_SHORT).show();
            isDialogVisible.set(false);
            timeHandler.removeCallbacks(timeUpdater);
        });
        builder.setOnCancelListener(dialog -> {
            isDialogVisible.set(false);
            timeHandler.removeCallbacks(timeUpdater);
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void updateTimeTextView(TextView timeTextView) {
        String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        timeTextView.setText("Thời gian: " + currentTime);
    }

    private boolean isWithinRange(double currentLat, double currentLon, double officeLat, double officeLon, float rangeInMeters) {
        float[] results = new float[1];
        Location.distanceBetween(currentLat, currentLon, officeLat, officeLon, results);
        float distance = results[0];
        Log.d(TAG, "Distance between current location and office: " + distance + " meters.");
        return distance <= rangeInMeters;
    }

    //=================================================
    //   onResume, onPause, onDestroy
    //=================================================
    @Override
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
    protected void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        executorService.shutdown();
    }

    //=================================================
    //   GALLERY => onActivityResult
    //=================================================
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Chọn ảnh"), GALLERY_REQUEST_CODE);
        Log.d(TAG, "Opening gallery for image selection.");
    }

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mOpenCvCameraView);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                List<Uri> imageUris = new ArrayList<>();

                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    for (int i = 0; i < count; i++) {
                        Uri imageUri = data.getClipData().getItemAt(i).getUri();
                        imageUris.add(imageUri);
                    }
                } else if (data.getData() != null) {
                    imageUris.add(data.getData());
                }

                List<float[]> newEmbeddings = new ArrayList<>();
                for (Uri imageUri : imageUris) {
                    try {
                        getContentResolver().takePersistableUriPermission(imageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        Mat imgMat = uriToMat(imageUri);
                        float[] embedding = ExtractFaceEmbedding(imgMat.getNativeObjAddr());
                        if (embedding != null) {
                            newEmbeddings.add(embedding);
                        } else {
                            Toast.makeText(MainActivityCPP.this, "Không phát hiện khuôn mặt trong một hoặc nhiều ảnh đã chọn.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error processing selected image: " + e.getMessage());
                        Toast.makeText(MainActivityCPP.this, "Lỗi xử lý ảnh: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                }

                if (!newEmbeddings.isEmpty()) {
                    if (!checkAllEmbeddingsSimilar(newEmbeddings, recognitionThreshold)) {
                        showMultipleFacesDetectedDialog();
                        return;
                    }
                    promptForName(newEmbeddings);
                } else {
                    Toast.makeText(MainActivityCPP.this, "Không trích xuất được đặc trưng khuôn mặt từ ảnh đã chọn.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.e(TAG, "No image selected or action canceled");
                Toast.makeText(this, "Không chọn ảnh hoặc đã hủy bỏ thao tác.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private Mat uriToMat(Uri uri) throws IOException {
        InputStream in = getContentResolver().openInputStream(uri);
        if (in == null) {
            throw new IOException("Unable to open input stream from URI");
        }
        Bitmap bitmap = BitmapFactory.decodeStream(in);
        in.close();
        if (bitmap == null)  {
            throw new IOException("Unable to decode bitmap from URI");
        }
        Mat mat = new Mat();
        Bitmap bmp32 = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp32, mat);
        return mat;
    }

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

    private void showMultipleFacesDetectedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivityCPP.this);
        builder.setTitle("Phát hiện nhiều khuôn mặt khác nhau");
        builder.setMessage("Các ảnh đã chọn dường như thuộc về những người khác nhau. Vui lòng chọn ảnh của cùng một người.");
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

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
                if (faceData.getEmbeddings() == null || faceData.getEmbeddings().isEmpty())
                    continue;
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

    private void showMatchingUsersDialog(Map<String, FaceData> matchingUsers, List<float[]> embeddings) {
        AlertDialog.Builder matchingDialog = new AlertDialog.Builder(MainActivityCPP.this);
        matchingDialog.setTitle("Phát hiện khuôn mặt tương tự");

        StringBuilder messageBuilder = new StringBuilder("Các ảnh đã chọn tương tự với người dùng đã có:\n");
        for (String name : matchingUsers.keySet()) {
            messageBuilder.append("- ").append(name).append("\n");
        }
        messageBuilder.append("\nBạn muốn thêm ảnh này vào người dùng sẵn có hay tạo người dùng mới?");
        matchingDialog.setMessage(messageBuilder.toString());

        matchingDialog.setPositiveButton("Thêm vào người dùng sẵn có", (dialog, which) -> {
            showUserSelectionDialog(matchingUsers, embeddings);
        });

        matchingDialog.setNegativeButton("Tạo người dùng mới", (dialog, which) -> {
            showNameInputDialog(embeddings);
        });

        matchingDialog.show();
    }

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

    //=================================================
    //   LƯU DỮ LIỆU FACE & CHECK LÊN FIREBASE
    //=================================================
    private void saveFaceDataToFirebase(FaceData faceData) {
        faceDataRef.child(faceData.getUserId()).setValue(faceData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Face data saved for user: " + faceData.getName()
                            + " with userId: " + faceData.getUserId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save face data for user: " + faceData.getName()
                            + " Error: " + e.getMessage());
                    Toast.makeText(MainActivityCPP.this, "Lưu dữ liệu khuôn mặt không thành công: "
                            + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void saveCheckDataToFirebase(String checkId, CheckData checkData) {
        if (checkId == null) {
            Log.e(TAG, "Attempted to save CheckData with null checkId.");
            return;
        }
        checkDataRef.child(checkId).setValue(checkData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Check data saved for user: " + checkData.getName()
                            + " with checkId: " + checkId);
                    checkDataMap.put(checkId, checkData);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save check data for user: "
                            + checkData.getName() + " Error: " + e.getMessage());
                    Toast.makeText(MainActivityCPP.this, "Lưu dữ liệu check không thành công: "
                            + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    //=================================================
    //   LOAD FACE, CHECK TỪ FIREBASE (SYNC KHI GHI)
    //=================================================
    private void loadFaceDataList() {
        faceDataRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot,
                                     @Nullable String previousChildName) {
                try {
                    FaceData faceData = snapshot.getValue(FaceData.class);
                    if (faceData != null && faceData.getEmbeddings() != null) {
                        synchronized (faceDataList) {
                            faceDataList.add(faceData);
                        }
                        Log.d(TAG, "Loaded FaceData: " + faceData.getName()
                                + " with userId: " + faceData.getUserId());
                    } else {
                        Log.e(TAG, "Invalid data in Firebase. userId: "
                                + snapshot.getKey());
                    }
                } catch (DatabaseException e) {
                    Log.e(TAG, "DatabaseException: " + e.getMessage());
                    Toast.makeText(MainActivityCPP.this,
                            "Lỗi tải dữ liệu khuôn mặt: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot,
                                       @Nullable String previousChildName) {
                try {
                    FaceData faceData = snapshot.getValue(FaceData.class);
                    if (faceData != null && faceData.getEmbeddings() != null) {
                        synchronized (faceDataList) {
                            for (int i = 0; i < faceDataList.size(); i++) {
                                if (faceDataList.get(i).getUserId()
                                        .equals(faceData.getUserId())) {
                                    faceDataList.set(i, faceData);
                                    Log.d(TAG, "Updated FaceData: " + faceData.getName()
                                            + " with userId: " + faceData.getUserId());
                                    break;
                                }
                            }
                        }
                    } else {
                        Log.e(TAG, "Invalid data in Firebase onChildChanged. userId: "
                                + snapshot.getKey());
                    }
                } catch (DatabaseException e) {
                    Log.e(TAG, "DatabaseException onChildChanged: " + e.getMessage());
                    Toast.makeText(MainActivityCPP.this,
                            "Lỗi cập nhật dữ liệu khuôn mặt: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                String userId = snapshot.getKey();
                if (userId != null) {
                    synchronized (faceDataList) {
                        faceDataList.removeIf(faceData -> faceData.getUserId().equals(userId));
                    }
                    Log.d(TAG, "Removed FaceData with userId: " + userId);
                } else {
                    Log.e(TAG, "Snapshot key (userId) is null in onChildRemoved.");
                }
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot,
                                     @Nullable String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load face data from Firebase.", error.toException());
                Toast.makeText(MainActivityCPP.this,
                        "Không tải được dữ liệu khuôn mặt từ Firebase: "
                                + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadCheckDataList() {
        checkDataRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot,
                                     @Nullable String previousChildName) {
                CheckData checkData = snapshot.getValue(CheckData.class);
                String key = snapshot.getKey();
                if (checkData != null && key != null) {
                    checkDataMap.put(key, checkData);
                    Log.d(TAG, "Loaded CheckData: " + checkData.getName()
                            + " at " + checkData.getTimestamp()
                            + " with key: " + key);
                } else {
                    Log.e(TAG, "Invalid CheckData in Firebase or key is null.");
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot,
                                       @Nullable String previousChildName) {
                CheckData checkData = snapshot.getValue(CheckData.class);
                String key = snapshot.getKey();
                if (checkData != null && key != null) {
                    checkDataMap.put(key, checkData);
                    Log.d(TAG, "Updated CheckData: " + checkData.getName()
                            + " with key: " + key);
                } else {
                    Log.e(TAG, "Invalid CheckData in Firebase onChildChanged or key is null.");
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                String checkId = snapshot.getKey();
                if (checkId != null) {
                    checkDataMap.remove(checkId);
                    Log.d(TAG, "Removed CheckData with checkId: " + checkId);
                } else {
                    Log.e(TAG, "Snapshot key (checkId) is null in onChildRemoved for CheckData.");
                }
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot,
                                     @Nullable String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load check data from Firebase.", error.toException());
                Toast.makeText(MainActivityCPP.this,
                        "Không tải được dữ liệu check: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    //=================================================
    //   LOCATION UPDATES
    //=================================================
    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission();
        } else {
            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setInterval(10000);
            locationRequest.setFastestInterval(1000);
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            fusedLocationClient.requestLocationUpdates(locationRequest,
                    locationCallback,
                    Looper.getMainLooper());
            Log.d(TAG, "Started location updates.");
        }
    }

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
                        Log.d(TAG, "Last known location retrieved: "
                                + location.getLatitude() + ", "
                                + location.getLongitude());
                    } else {
                        Log.d(TAG, "No last known location available.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get last known location.", e);
                    Toast.makeText(MainActivityCPP.this, "Không lấy được vị trí cuối cùng: "
                            + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    //=================================================
    //   REQUEST PERMISSIONS
    //=================================================
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
            Log.d(TAG, "All permissions already granted.");
        }
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        Log.d(TAG, "Requesting camera permission.");
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        Log.d(TAG, "Requesting location permission.");
    }

    private void initFaceDetectionAndRecognition() {
        try {
            InputStream inputStream = getAssets().open("face_detection_yunet_2023mar.onnx");
            FileUtil fileUtil = new FileUtil();
            java.io.File detectionModelFile = fileUtil.createTempFile(this, inputStream,
                    "face_detection_yunet_2023mar.onnx");
            InitFaceDetector(detectionModelFile.getAbsolutePath());
            Log.d(TAG, "Face Detector initialized with model: "
                    + detectionModelFile.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Error initializing Face Detector: " + e.getMessage());
            Toast.makeText(this,
                    "Lỗi khởi tạo bộ phát hiện khuôn mặt: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }

        try {
            InputStream inputStream = getAssets().open("face_recognition_sface_2021dec.onnx");
            FileUtil fileUtil = new FileUtil();
            java.io.File recognitionModelFile = fileUtil.createTempFile(this, inputStream,
                    "face_recognition_sface_2021dec.onnx");
            InitFaceRecognition(recognitionModelFile.getAbsolutePath());
            Log.d(TAG, "Face Recognition initialized with model: "
                    + recognitionModelFile.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Error initializing Face Recognition: " + e.getMessage());
            Toast.makeText(this,
                    "Lỗi khởi tạo nhận diện khuôn mặt: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    //=================================================
    //   HIỂN THỊ / XÓA FACE, CHECK
    //=================================================
    private void displayRegisteredFaces() {
        // Kiểm tra nếu người dùng là Manager và đã có companyName
        if (managerCompanyName == null || managerCompanyName.isEmpty()) {
            Toast.makeText(this,
                    "Không tìm thấy tên công ty của bạn. Vui lòng thử lại sau.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Sao chép snapshot để tránh lỗi concurrent modification
        List<FaceData> snapshot;
        synchronized (faceDataList) {
            snapshot = new ArrayList<>(faceDataList);
        }

        // Lọc danh sách FaceData theo companyName
        List<FaceData> filteredSnapshot = new ArrayList<>();
        for (FaceData face : snapshot) {
            if (managerCompanyName.equalsIgnoreCase(face.getCompanyName())) {
                filteredSnapshot.add(face);
            }
        }

        if (filteredSnapshot.isEmpty()) {
            Toast.makeText(this, "Không có người dùng nào trong công ty của bạn.", Toast.LENGTH_SHORT).show();
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
        builder.setTitle("Các khuôn mặt đã đăng ký trong công ty: " + managerCompanyName);
        builder.setView(listView);
        builder.setNegativeButton("Đóng", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showFaceDataDialog(FaceData selectedFace) {
        AlertDialog.Builder embeddingDialog = new AlertDialog.Builder(MainActivityCPP.this);
        embeddingDialog.setTitle("Thông tin cho " + selectedFace.getName());

        StringBuilder sb = new StringBuilder();
        sb.append("Công ty: ").append(selectedFace.getCompanyName()).append("\n");
        sb.append("Vĩ độ làm việc: ").append(selectedFace.getOfficeLatitude()).append("\n");
        sb.append("Kinh độ làm việc: ").append(selectedFace.getOfficeLongitude()).append("\n");
        sb.append("Số lượng đặc trưng: ").append(selectedFace.getEmbeddings().size()).append("\n\n");

        int count = 1;
        for (List<Float> embeddingList : selectedFace.getEmbeddings()) {
            sb.append("Đặc trưng ").append(count).append(": [");
            for (int i = 0; i < embeddingList.size(); i++) {
                sb.append(String.format("%.4f", embeddingList.get(i)));
                if (i < embeddingList.size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append("]\n\n");
            count++;
        }

        String embeddingsString = sb.toString();
        embeddingDialog.setMessage(embeddingsString);

        embeddingDialog.setNeutralButton("Sao chép đặc trưng",
                (dialogInterface, i1) -> {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("", embeddingsString);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(MainActivityCPP.this, "Đã sao chép đặc trưng vào khay nhớ tạm.", Toast.LENGTH_SHORT).show();
                });

        embeddingDialog.setPositiveButton("Xóa người dùng", (dialogInterface, i12) -> {
            showDeleteUserDialog(selectedFace);
        });

        embeddingDialog.setNegativeButton("Đóng", (dialog, which) -> dialog.dismiss());

        embeddingDialog.show();
    }

    private void showDeleteUserDialog(FaceData selectedFace) {
        AlertDialog.Builder confirmDialog = new AlertDialog.Builder(MainActivityCPP.this);
        confirmDialog.setTitle("Xóa người dùng");
        confirmDialog.setMessage("Bạn có chắc chắn muốn xóa \"" + selectedFace.getName() + "\" không?");
        confirmDialog.setPositiveButton("Có", (dialog, which) -> {
            deleteUser(selectedFace);
        });
        confirmDialog.setNegativeButton("Không", (dialog, which) -> dialog.cancel());
        confirmDialog.show();
    }

    private void deleteUser(FaceData faceData) {
        String userId = faceData.getUserId();
        if (userId != null && !userId.isEmpty()) {
            faceDataRef.child(userId).removeValue()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(MainActivityCPP.this,
                                "Đã xóa người dùng \"" + faceData.getName() + "\".",
                                Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(MainActivityCPP.this,
                                "Xóa người dùng không thành công: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
        } else {
            Toast.makeText(MainActivityCPP.this,
                    "ID người dùng không hợp lệ. Không thể xóa.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void displaySavedChecks() {
        // Lấy user hiện tại
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DatabaseReference userRoleRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(userId)
                    .child("role");

            // Lấy role của user
            userRoleRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String role = snapshot.getValue(String.class);
                    Log.d(TAG, "User role: " + role);

                    // Nếu user là Manager
                    if ("Manager".equalsIgnoreCase(role)) {
                        // Lấy companyName của Manager
                        DatabaseReference userCompanyRef = FirebaseDatabase.getInstance()
                                .getReference("users")
                                .child(userId)
                                .child("company");
                        userCompanyRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                String companyName = snapshot.getValue(String.class);
                                if (companyName == null || companyName.isEmpty()) {
                                    Toast.makeText(MainActivityCPP.this,
                                            "Không tìm thấy tên công ty của bạn.",
                                            Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                // Lọc các CheckData theo companyName
                                List<CheckData> filteredChecks = new ArrayList<>();
                                for (CheckData cd : checkDataMap.values()) {
                                    if (cd.getCompanyName() != null && cd.getCompanyName().equalsIgnoreCase(companyName)) {
                                        filteredChecks.add(cd);
                                    }
                                }

                                if (filteredChecks.isEmpty()) {
                                    Toast.makeText(MainActivityCPP.this,
                                            "Chưa có lượt check nào được lưu cho công ty của bạn.",
                                            Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                // Sắp xếp các CheckData theo timestamp giảm dần
                                Collections.sort(filteredChecks, (cd1, cd2) -> Long.compare(cd2.getTimestamp(), cd1.getTimestamp()));

                                // Tạo ListView với các CheckData đã sắp xếp
                                ListView listView = new ListView(MainActivityCPP.this);
                                List<String> checkInfoList = new ArrayList<>();

                                for (CheckData cd : filteredChecks) {
                                    String formattedTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                                            Locale.getDefault()).format(new Date(cd.getTimestamp()));
                                    String info = "Tên: " + cd.getName() + "\n"
                                            + "Vị trí: " + cd.getLocation() + "\n"
                                            + "Thời gian: " + formattedTime;
                                    checkInfoList.add(info);
                                }

                                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                        MainActivityCPP.this,
                                        android.R.layout.simple_list_item_1,
                                        checkInfoList);
                                listView.setAdapter(adapter);

                                // Nhấn giữ để xóa
                                listView.setOnItemLongClickListener((parent, view, position, id) -> {
                                    CheckData selectedCheck = filteredChecks.get(position);
                                    String selectedCheckId = null;
                                    for (Map.Entry<String, CheckData> entry : checkDataMap.entrySet()) {
                                        if (entry.getValue().equals(selectedCheck)) {
                                            selectedCheckId = entry.getKey();
                                            break;
                                        }
                                    }
                                    if (selectedCheckId != null) {
                                        showDeleteCheckDialog(selectedCheckId, selectedCheck);
                                    } else {
                                        Toast.makeText(MainActivityCPP.this,
                                                "Không tìm thấy ID của lượt check này.",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                    return true;
                                });

                                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivityCPP.this);
                                builder.setTitle("Lịch sử Check cho công ty: " + companyName);
                                builder.setView(listView);
                                builder.setNegativeButton("Đóng",
                                        (dialog, which) -> dialog.dismiss());
                                builder.show();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e(TAG, "Failed to read manager's company name: " + error.getMessage());
                                Toast.makeText(MainActivityCPP.this,
                                        "Lỗi khi lấy tên công ty của bạn.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });

                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Failed to read user role: " + error.getMessage());
                    Toast.makeText(MainActivityCPP.this,
                            "Lỗi khi lấy thông tin vai trò người dùng.",
                            Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Người dùng không phải là Manager
            // Hiển thị lịch sử check cá nhân
            List<CheckData> userChecks = new ArrayList<>();
            for (CheckData cd : checkDataMap.values()) {
                if (cd.getUserId().equals(currentUserId)) {
                    userChecks.add(cd);
                }
            }
            if (userChecks.isEmpty()) {
                Toast.makeText(MainActivityCPP.this,
                        "Chưa có lượt check nào được lưu cho bạn.",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Sắp xếp các CheckData theo timestamp giảm dần
            Collections.sort(userChecks, (cd1, cd2) -> Long.compare(cd2.getTimestamp(), cd1.getTimestamp()));

            ListView listView = new ListView(MainActivityCPP.this);
            List<String> checkInfoList = new ArrayList<>();
            for (CheckData cd : userChecks) {
                String formattedTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                        Locale.getDefault()).format(new Date(cd.getTimestamp()));
                String info = "Tên: " + cd.getName() + "\n"
                        + "Vị trí: " + cd.getLocation() + "\n"
                        + "Thời gian: " + formattedTime;
                checkInfoList.add(info);
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivityCPP.this,
                    android.R.layout.simple_list_item_1, checkInfoList);
            listView.setAdapter(adapter);

            // Nhấn giữ để xóa
            listView.setOnItemLongClickListener((parent, view, position, id) -> {
                CheckData selectedCheck = userChecks.get(position);
                String selectedCheckId = null;
                for (Map.Entry<String, CheckData> entry : checkDataMap.entrySet()) {
                    if (entry.getValue().equals(selectedCheck)) {
                        selectedCheckId = entry.getKey();
                        break;
                    }
                }
                if (selectedCheckId != null) {
                    showDeleteCheckDialog(selectedCheckId, selectedCheck);
                } else {
                    Toast.makeText(MainActivityCPP.this,
                            "Không tìm thấy ID của lượt check này.",
                            Toast.LENGTH_SHORT).show();
                }
                return true;
            });

            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivityCPP.this);
            builder.setTitle("Lịch sử Check của bạn");
            builder.setView(listView);
            builder.setNegativeButton("Đóng", (dialog, which) -> dialog.dismiss());
            builder.show();
        }
    }

    // Helper method to get FaceData by userId
    private FaceData getFaceDataByUserId(String userId) {
        synchronized (faceDataList) {
            for (FaceData faceData : faceDataList) {
                if (faceData.getUserId().equals(userId)) {
                    return faceData;
                }
            }
        }
        return null;
    }

    // Helper method to get FaceData by name
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

    //=================================================
    //   DIALOG XÓA LƯỢT CHECK
    //=================================================
    private void showDeleteCheckDialog(String checkId, CheckData selectedCheck) {
        AlertDialog.Builder confirmDialog = new AlertDialog.Builder(MainActivityCPP.this);
        confirmDialog.setTitle("Xóa lượt check");
        confirmDialog.setMessage("Bạn có chắc muốn xóa lượt check này không?");
        confirmDialog.setPositiveButton("Có", (dialog, which) -> {
            deleteCheck(checkId);
        });
        confirmDialog.setNegativeButton("Không", (dialog, which) -> dialog.cancel());
        confirmDialog.show();
    }

    private void deleteCheck(String checkId) {
        if (checkId != null && !checkId.isEmpty()) {
            checkDataRef.child(checkId).removeValue()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(MainActivityCPP.this, "Đã xóa lượt check.", Toast.LENGTH_SHORT).show();
                        checkDataMap.remove(checkId);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(MainActivityCPP.this,
                                "Xóa lượt check không thành công: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
        } else {
            Toast.makeText(MainActivityCPP.this,
                    "ID check không hợp lệ. Không thể xóa.",
                    Toast.LENGTH_LONG).show();
        }
    }

    //=================================================
    //   RESET
    //=================================================
    private void resetCanceledFaces() {
        canceledFaces.clear();
    }

    //=================================================
    //   ĐĂNG NHẬP / ĐĂNG XUẤT
    //=================================================
    private void handleSignIn(){
        Intent intent = new Intent(MainActivityCPP.this, Login.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void handleSignOut() {
        mAuth.signOut();
        clearPreferences();
        Toast.makeText(MainActivityCPP.this,
                "Đăng xuất thành công", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(MainActivityCPP.this, Login.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void clearPreferences() {
        SharedPreferences sharedPreferences =
                getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_EMAIL);
        editor.remove(KEY_REMEMBER_ME);
        editor.apply();
    }

    //=================================================
    //   KIỂM TRA ROLE => ẨN / HIỆN NÚT
    //=================================================
    private void checkUserRoleAndAdjustUI() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DatabaseReference userRoleRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(userId)
                    .child("role");

            userRoleRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String role = snapshot.getValue(String.class);
                    Log.d(TAG, "User role: " + role);
                    if ("Manager".equalsIgnoreCase(role)) {
                        // Lấy companyName của Manager
                        DatabaseReference userCompanyRef = FirebaseDatabase.getInstance()
                                .getReference("users")
                                .child(userId)
                                .child("company");
                        userCompanyRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                managerCompanyName = snapshot.getValue(String.class);
                                Log.d(TAG, "Manager's company name: " + managerCompanyName);
                                if (managerCompanyName == null || managerCompanyName.isEmpty()) {
                                    Toast.makeText(MainActivityCPP.this,
                                            "Không tìm thấy tên công ty của bạn.",
                                            Toast.LENGTH_SHORT).show();
                                    // Ẩn các nút liên quan nếu không có companyName
                                    galleryBtn.setVisibility(View.GONE);
                                    showFacesBtn.setVisibility(View.GONE);
                                    return;
                                }
                                // Hiển thị các nút liên quan đến Manager

                                signOutBtn.setVisibility(View.VISIBLE);
                                signInBtn.setVisibility(View.GONE);
                                resetRecognitionBtn.setVisibility(View.GONE);
                                galleryBtn.setVisibility(View.VISIBLE);
                                showFacesBtn.setVisibility(View.VISIBLE);
                                Log.d(TAG, "Displayed galleryBtn and showFacesBtn for Manager.");
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e(TAG, "Failed to read manager's company name: " + error.getMessage());
                                Toast.makeText(MainActivityCPP.this,
                                        "Lỗi khi lấy tên công ty của bạn.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        // Người dùng không phải là Manager
                        mOpenCvCameraView.setVisibility(View.VISIBLE);
                        signOutBtn.setVisibility(View.GONE);
                        signInBtn.setVisibility(View.VISIBLE);
                        resetRecognitionBtn.setVisibility(View.VISIBLE);
                        galleryBtn.setVisibility(View.GONE);
                        showFacesBtn.setVisibility(View.GONE);
                        Log.d(TAG, "Hide galleryBtn and showFacesBtn for non-Manager role.");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Failed to read user role: " + error.getMessage());
                    mOpenCvCameraView.setVisibility(View.VISIBLE);
                    signOutBtn.setVisibility(View.GONE);
                    signInBtn.setVisibility(View.VISIBLE);
                    resetRecognitionBtn.setVisibility(View.VISIBLE);
                    galleryBtn.setVisibility(View.GONE);
                    showFacesBtn.setVisibility(View.GONE);
                }
            });
        } else {
            // Người dùng chưa đăng nhập
            mOpenCvCameraView.setVisibility(View.VISIBLE);
            signOutBtn.setVisibility(View.GONE);
            signInBtn.setVisibility(View.VISIBLE);
            resetRecognitionBtn.setVisibility(View.VISIBLE);
            galleryBtn.setVisibility(View.GONE);
            showFacesBtn.setVisibility(View.GONE);
        }
    }

    //=================================================
    //   LOAD RECOGNITION THRESHOLD
    //=================================================
    private void loadRecognitionThreshold() {
        float defaultThreshold = 0.5f;
        recognitionThreshold = getSharedPreferences("app_prefs", MODE_PRIVATE)
                .getFloat("recognition_threshold", defaultThreshold);
        Log.d(TAG, "Loaded recognition threshold: " + recognitionThreshold);
    }

}
