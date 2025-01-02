package com.example.appcpp;

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
import android.widget.Spinner;  // Quan trọng: import Spinner
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
import java.util.prefs.Preferences; // Chỉ là ví dụ import

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

    // =============== Danh sách công ty ===============
    private final List<String> companyNameList = new ArrayList<>();

    static {
        if (!OpenCVLoader.initDebug()) {
            Log.e(TAG, "Cannot connect to OpenCV Manager");
        } else {
            System.loadLibrary("appcpp");
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

        // Mở fullscreen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

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

        mOpenCvCameraView = findViewById(R.id.opencv_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(cvCameraViewListener2);
        mOpenCvCameraView.setMaxFrameSize(640, 640);

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

        // Gọi hàm loadCompanies() để tải danh sách các công ty từ DB
        loadCompanies();
    }

    //==================================================
    //   LOAD DANH SÁCH CÔNG TY TỪ FIREBASE
    //==================================================
    private void loadCompanies() {
        DatabaseReference companiesRef = FirebaseDatabase.getInstance().getReference("companies");
        companiesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                synchronized (companyNameList) {
                    companyNameList.clear();
                    for (DataSnapshot child : snapshot.getChildren()) {
                        String companyName = child.child("companyName").getValue(String.class);
                        if (companyName != null) {
                            companyNameList.add(companyName);
                        }
                    }
                }
                Log.d(TAG, "Company list loaded: " + companyNameList.size() + " item(s).");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "loadCompanies() onCancelled: " + error.getMessage());
            }
        });
    }

    //==================================================
    //   TẠO DIALOG CHỌN TÊN & SPINNER CÔNG TY
    //==================================================
    private void showNameInputDialog(List<float[]> embeddings) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Nhập tên và chọn công ty");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText nameInput = new EditText(this);
        nameInput.setHint("Tên");
        layout.addView(nameInput);

        final Spinner companySpinner = new Spinner(this);
        // Lấy companyNameList (đã đồng bộ) để tạo adapter
        List<String> copyOfCompanies;
        synchronized (companyNameList) {
            copyOfCompanies = new ArrayList<>(companyNameList);
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                copyOfCompanies
        );
        companySpinner.setAdapter(spinnerAdapter);
        layout.addView(companySpinner);

        builder.setView(layout);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String name = nameInput.getText().toString().trim();
            String companyName = (String) companySpinner.getSelectedItem();

            if (!name.isEmpty() && companyName != null && !companyName.isEmpty()) {
                handleNameAndCompanyInput(name, companyName, embeddings);
            } else {
                Toast.makeText(this,
                        "Tên hoặc tên công ty không được để trống.",
                        Toast.LENGTH_SHORT).show();
                Log.e(TAG,
                        "User attempted to save face data without entering a name or choosing a company.");
            }
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void handleNameAndCompanyInput(String name, String companyName, List<float[]> embeddings) {
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
            createNewUserWithEmbeddingsAndCompany(name, companyName, embeddings);
        }
    }

    private void showNameAndCompanyExistsDialog(FaceData existingFaceData, List<float[]> embeddings) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Tên và Công ty đã tồn tại");
        dialog.setMessage("Đã có người dùng với tên và công ty này. Bạn có muốn thêm ảnh mới vào người dùng đó không?");
        dialog.setPositiveButton("Có", (dialog1, which1) -> {
            synchronized (faceDataList) {
                for (float[] emb : embeddings) {
                    existingFaceData.getEmbeddings().add(floatArrayToList(emb));
                }
            }
            saveFaceDataToFirebase(existingFaceData);
            Toast.makeText(this, "Đã thêm ảnh mới vào người dùng sẵn có.", Toast.LENGTH_SHORT).show();
        });
        dialog.setNegativeButton("Không", (dialog1, which1) -> dialog1.cancel());
        dialog.show();
    }

    private void createNewUserWithEmbeddingsAndCompany(String name, String companyName, List<float[]> embeddings) {
        String userId = UUID.randomUUID().toString();
        List<List<Float>> embeddingsList = new ArrayList<>();
        for (float[] embedding : embeddings) {
            embeddingsList.add(floatArrayToList(embedding));
        }

        FaceData faceData = new FaceData(userId, name, companyName, embeddingsList);
        saveFaceDataToFirebase(faceData);
        Toast.makeText(this, "Dữ liệu khuôn mặt đã được lưu.", Toast.LENGTH_SHORT).show();
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

                    int numFaces = DetectFaces(inputGray.getNativeObjAddr(), inputRgba.getNativeObjAddr(), faceRects);
                    Log.d(TAG, "Detected " + numFaces + " face(s).");

                    // =====================
                    // Tạo 1 snapshot an toàn
                    // =====================
                    List<FaceData> snapshot;
                    synchronized (faceDataList) {
                        snapshot = new ArrayList<>(faceDataList);
                    }

                    // Duyệt snapshot
                    for (int i = 0; i < numFaces; i++) {
                        float x = faceRects[i * 4];
                        float y = faceRects[i * 4 + 1];
                        float w = faceRects[i * 4 + 2];
                        float h = faceRects[i * 4 + 3];

                        Imgproc.rectangle(inputRgba, new Point(x, y), new Point(x + w, y + h),
                                new Scalar(0, 255, 0), 3);

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
                            Imgproc.putText(inputRgba, matchedName,
                                    new Point(x, y - 10),
                                    Imgproc.FONT_HERSHEY_SIMPLEX, 1.0, new Scalar(30, 30, 220), 2);

                            Log.d(TAG, "Matched user: " + matchedName + " with similarity: " + highestSimilarity);

                            if (!matchedUserId.equals(currentUserId)) {
                                currentUserId = matchedUserId;
                                Log.d(TAG, "Current user ID set to: " + currentUserId);
                            }

                            if (!canceledFaces.contains(matchedName)) {
                                if (isDialogVisible.compareAndSet(false, true)) {
                                    final String recognizedName = matchedName;
                                    runOnUiThread(() -> showRecognitionDialog(recognizedName));
                                }
                            }
                        } else {
                            Imgproc.putText(inputRgba, "Unknown",
                                    new Point(x, y - 10),
                                    Imgproc.FONT_HERSHEY_SIMPLEX, 1.0, new Scalar(255, 50, 50), 2);
                            Log.d(TAG, "No matching user found.");
                        }
                    }

                    // Vẽ mask
                    Mat mask = Mat.zeros(inputRgba.size(), inputRgba.type());
                    int cornerRadius = 40;

                    Imgproc.rectangle(
                            mask,
                            new Point(cornerRadius, 0),
                            new Point(mask.cols() - cornerRadius, mask.rows()),
                            new Scalar(255, 255, 255),
                            -1
                    );
                    Imgproc.rectangle(
                            mask,
                            new Point(0, cornerRadius),
                            new Point(mask.cols(), mask.rows() - cornerRadius),
                            new Scalar(255, 255, 255),
                            -1
                    );

// 2) Vẽ 4 ellipse bo tròn 4 góc
// Góc trên-trái
                    Imgproc.ellipse(
                            mask,
                            new Point(cornerRadius, cornerRadius),
                            new Size(cornerRadius, cornerRadius),
                            180,    // angle
                            0, 90,  // startAngle, endAngle
                            new Scalar(255, 255, 255),
                            -1
                    );
// Góc trên-phải
                    Imgproc.ellipse(
                            mask,
                            new Point(mask.cols() - cornerRadius, cornerRadius),
                            new Size(cornerRadius, cornerRadius),
                            270,
                            0, 90,
                            new Scalar(255, 255, 255),
                            -1
                    );
// Góc dưới-trái
                    Imgproc.ellipse(
                            mask,
                            new Point(cornerRadius, mask.rows() - cornerRadius),
                            new Size(cornerRadius, cornerRadius),
                            90,
                            0, 90,
                            new Scalar(255, 255, 255),
                            -1
                    );
// Góc dưới-phải
                    Imgproc.ellipse(
                            mask,
                            new Point(mask.cols() - cornerRadius, mask.rows() - cornerRadius),
                            new Size(cornerRadius, cornerRadius),
                            0,
                            0, 90,
                            new Scalar(255, 255, 255),
                            -1
                    );

                    Mat output = new Mat();
                    Core.bitwise_and(inputRgba, mask, output);

                    long endTime = System.nanoTime();
                    long processingTimeMs = (endTime - startTime) / 1_000_000;

                    return output;
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

        TextView nameTextView     = dialogView.findViewById(R.id.recognizedNameTextView);
        TextView locationTextView = dialogView.findViewById(R.id.locationTextView);
        TextView timeTextView     = dialogView.findViewById(R.id.timeTextView);

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

        builder.setPositiveButton("Check", (dialog, which) -> {
            if (currentUserId == null) {
                Toast.makeText(MainActivityCPP.this, "Không xác định được ID người dùng.", Toast.LENGTH_LONG).show();
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

            long currentTimeMillis = System.currentTimeMillis();
            CheckData checkData = new CheckData(currentUserId, recognizedName, locationText, currentTimeMillis);
            saveCheckDataToFirebase(checkId, checkData);

            canceledFaces.add(recognizedName);

            Toast.makeText(MainActivityCPP.this, "Check thành công cho " + recognizedName, Toast.LENGTH_SHORT).show();
            isDialogVisible.set(false);
            timeHandler.removeCallbacks(timeUpdater);
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> {
            canceledFaces.add(recognizedName);
            Toast.makeText(MainActivityCPP.this, "Nhận diện đã bị hủy.", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(this, "Không phát hiện khuôn mặt trong một hoặc nhiều ảnh đã chọn.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error processing selected image: " + e.getMessage());
                        Toast.makeText(this, "Lỗi xử lý ảnh: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
                    Toast.makeText(this, "Không trích xuất được đặc trưng khuôn mặt từ ảnh đã chọn.", Toast.LENGTH_SHORT).show();
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
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
        AlertDialog.Builder matchingDialog = new AlertDialog.Builder(this);
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
        AlertDialog.Builder userSelectDialog = new AlertDialog.Builder(this);
        userSelectDialog.setTitle("Chọn người dùng");

        List<String> userList = new ArrayList<>(matchingUsers.keySet());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, userList);

        userSelectDialog.setAdapter(adapter, (dialog1, which1) -> {
            String selectedUserName = userList.get(which1);
            FaceData selectedUser = matchingUsers.get(selectedUserName);

            synchronized (faceDataList) {
                for (float[] embedding : embeddings) {
                    selectedUser.getEmbeddings().add(floatArrayToList(embedding));
                }
            }
            saveFaceDataToFirebase(selectedUser);
            Toast.makeText(this, "Đã thêm ảnh vào người dùng sẵn có.", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(this, "Lưu dữ liệu khuôn mặt không thành công: "
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
                    Toast.makeText(this, "Lưu dữ liệu check không thành công: "
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
                    Toast.makeText(this, "Không lấy được vị trí cuối cùng: "
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
        // Copy snapshot
        List<FaceData> snapshot;
        synchronized (faceDataList) {
            snapshot = new ArrayList<>(faceDataList);
        }

        if (snapshot.isEmpty()) {
            Toast.makeText(this, "Chưa có khuôn mặt nào được đăng ký.", Toast.LENGTH_SHORT).show();
            return;
        }

        ListView listView = new ListView(this);
        List<String> names = new ArrayList<>();
        for (FaceData faceData : snapshot) {
            names.add(faceData.getName() + " (ID: " + faceData.getUserId() + ")");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, names);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            FaceData selectedFace = snapshot.get(position);
            showFaceDataDialog(selectedFace);
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Các khuôn mặt đã đăng ký");
        builder.setView(listView);
        builder.setNegativeButton("Đóng", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showFaceDataDialog(FaceData selectedFace) {
        AlertDialog.Builder embeddingDialog = new AlertDialog.Builder(this);
        embeddingDialog.setTitle("Thông tin cho " + selectedFace.getName());

        StringBuilder sb = new StringBuilder();
        sb.append("Công ty: ").append(selectedFace.getCompanyName()).append("\n");
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
                    Toast.makeText(this, "Đã sao chép đặc trưng vào khay nhớ tạm.", Toast.LENGTH_SHORT).show();
                });

        embeddingDialog.setPositiveButton("Xóa người dùng", (dialogInterface, i12) -> {
            showDeleteUserDialog(selectedFace);
        });

        embeddingDialog.setNegativeButton("Đóng", (dialog, which) -> dialog.dismiss());

        embeddingDialog.show();
    }

    private void showDeleteUserDialog(FaceData selectedFace) {
        AlertDialog.Builder confirmDialog = new AlertDialog.Builder(this);
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
                        Toast.makeText(this,
                                "Đã xóa người dùng \"" + faceData.getName() + "\".",
                                Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this,
                                "Xóa người dùng không thành công: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
        } else {
            Toast.makeText(this,
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
                        List<Map.Entry<String, CheckData>> userCheckEntries
                                = new ArrayList<>(checkDataMap.entrySet());
                        if (userCheckEntries.isEmpty()) {
                            Toast.makeText(MainActivityCPP.this,
                                    "Chưa có lượt check nào được lưu.",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        ListView listView = new ListView(MainActivityCPP.this);
                        List<String> checkInfoList = new ArrayList<>();

                        for (Map.Entry<String, CheckData> entry : userCheckEntries) {
                            CheckData cd = entry.getValue();
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
                            String firebaseKey = userCheckEntries.get(position).getKey();
                            CheckData selectedCheck = userCheckEntries.get(position).getValue();
                            showDeleteCheckDialog(firebaseKey, selectedCheck);
                            return true;
                        });

                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivityCPP.this);
                        builder.setTitle("Lịch sử Check");
                        builder.setView(listView);
                        builder.setNegativeButton("Đóng",
                                (dialog, which) -> dialog.dismiss());
                        builder.show();

                    } else {
                        // Không phải Manager
                        Log.d(TAG, "User is not a Manager. Access to this feature is restricted.");
                        Toast.makeText(MainActivityCPP.this,
                                "Bạn không có quyền truy cập tính năng này.",
                                Toast.LENGTH_SHORT).show();
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
            // Nếu chưa login
            Toast.makeText(this,
                    "Bạn chưa đăng nhập. Không thể xem lịch sử check.",
                    Toast.LENGTH_SHORT).show();
        }

        // Nếu user == null nhưng currentUserId != null
        if (currentUserId != null && currentUser == null) {
            List<Map.Entry<String, CheckData>> userCheckEntries = new ArrayList<>();
            for (Map.Entry<String, CheckData> entry : checkDataMap.entrySet()) {
                CheckData cd = entry.getValue();
                if (cd.getUserId().equals(currentUserId)) {
                    userCheckEntries.add(entry);
                }
            }
            if (userCheckEntries.isEmpty()) {
                Toast.makeText(this,
                        "Chưa có lượt check nào được lưu cho bạn.",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            ListView listView = new ListView(this);
            List<String> checkInfoList = new ArrayList<>();
            for (Map.Entry<String, CheckData> entry : userCheckEntries) {
                CheckData cd = entry.getValue();
                String formattedTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                        Locale.getDefault()).format(new Date(cd.getTimestamp()));
                String info = "Tên: " + cd.getName() + "\n"
                        + "Vị trí: " + cd.getLocation() + "\n"
                        + "Thời gian: " + formattedTime;
                checkInfoList.add(info);
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, checkInfoList);
            listView.setAdapter(adapter);

            listView.setOnItemLongClickListener((parent, view, position, id) -> {
                String firebaseKey = userCheckEntries.get(position).getKey();
                CheckData selectedCheck = userCheckEntries.get(position).getValue();
                showDeleteCheckDialog(firebaseKey, selectedCheck);
                return true;
            });

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Lịch sử Check của bạn");
            builder.setView(listView);
            builder.setNegativeButton("Đóng", (dialog, which) -> dialog.dismiss());
            builder.show();
        }
    }

    private void showDeleteCheckDialog(String checkId, CheckData selectedCheck) {
        AlertDialog.Builder confirmDialog = new AlertDialog.Builder(this);
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
                        Toast.makeText(this, "Đã xóa lượt check.", Toast.LENGTH_SHORT).show();
                        checkDataMap.remove(checkId);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this,
                                "Xóa lượt check không thành công: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
        } else {
            Toast.makeText(this,
                    "ID check không hợp lệ. Không thể xóa.",
                    Toast.LENGTH_LONG).show();
        }
    }

    //=================================================
    //   RESET
    //=================================================
    private void resetCanceledFaces() {
        canceledFaces.clear();
        Toast.makeText(this,
                "Đã đặt lại trạng thái hiển thị hộp thoại nhận diện.",
                Toast.LENGTH_SHORT).show();
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
                        signOutBtn.setVisibility(View.VISIBLE);
                        signInBtn.setVisibility(View.GONE);
                        resetRecognitionBtn.setVisibility(View.GONE);
                        galleryBtn.setVisibility(View.VISIBLE);
                        showFacesBtn.setVisibility(View.VISIBLE);
                        Log.d(TAG, "Displayed galleryBtn and showFacesBtn for Manager.");
                    } else {
                        signOutBtn.setVisibility(View.GONE);
                        signInBtn.setVisibility(View.VISIBLE);
                        resetRecognitionBtn.setVisibility(View.VISIBLE);
                        galleryBtn.setVisibility(View.GONE);
                        showFacesBtn.setVisibility(View.GONE);
                        Log.d(TAG, "Hid galleryBtn and showFacesBtn for non-Manager role.");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Failed to read user role: " + error.getMessage());
                    signOutBtn.setVisibility(View.GONE);
                    signInBtn.setVisibility(View.VISIBLE);
                    resetRecognitionBtn.setVisibility(View.VISIBLE);
                    galleryBtn.setVisibility(View.GONE);
                    showFacesBtn.setVisibility(View.GONE);
                }
            });
        } else {
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
