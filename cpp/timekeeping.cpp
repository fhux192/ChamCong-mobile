
#include <jni.h>
#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/dnn.hpp>
#include <opencv2/objdetect/face.hpp>
#include <vector>
#include <string>
#include <fstream>
#include <sstream>
#include <mutex>
#include <android/log.h>

#define LOG_TAG "native-lib"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

using namespace cv;
using namespace std;

static Ptr<FaceDetectorYN> face_detector;
static Ptr<FaceRecognizerSF> face_recognition;
static std::mutex gMutex;

struct FaceData {
    std::string name;
    std::vector<float> embedding;
};
static std::vector<FaceData> faceDataList;

static inline Mat rgbaToSafeBgr(const Mat& srcRgba) {
    Mat bgr; cvtColor(srcRgba, bgr, COLOR_RGBA2BGR);
    return bgr.clone();
}

extern "C" {

JNIEXPORT void JNICALL
Java_com_example_timekeeping_MainActivityCPP_InitFaceDetector(JNIEnv* env, jobject,
                                                              jstring jModelPath) {
    const char* cstr = env->GetStringUTFChars(jModelPath, nullptr);
    {
        std::lock_guard<std::mutex> lk(gMutex);
        face_detector = FaceDetectorYN::create(cstr, "", Size(112, 112), 0.5f, 0.3f, 1,
                                               dnn::DNN_BACKEND_DEFAULT, dnn::DNN_TARGET_CPU);
    }
    env->ReleaseStringUTFChars(jModelPath, cstr);
}

JNIEXPORT void JNICALL
Java_com_example_timekeeping_MainActivityCPP_InitFaceRecognition(JNIEnv* env, jobject,
                                                                 jstring jModelPath) {
    const char* cstr = env->GetStringUTFChars(jModelPath, nullptr);
    {
        std::lock_guard<std::mutex> lk(gMutex);
        face_recognition = FaceRecognizerSF::create(cstr, "", /*norm*/0, /*input_size*/0);
    }
    env->ReleaseStringUTFChars(jModelPath, cstr);
}

JNIEXPORT jint JNICALL
Java_com_example_timekeeping_MainActivityCPP_DetectFaces(JNIEnv* env, jobject,
                                                         jlong addrGray, jlong addrRgba,
                                                         jfloatArray largestFaceRectOut) {
    Mat& rgba = *(Mat*)addrRgba;

    std::lock_guard<std::mutex> lk(gMutex);
    if (face_detector.empty()) return 0;

    Mat img = rgbaToSafeBgr(rgba);
    face_detector->setInputSize(img.size());

    Mat faces; face_detector->detect(img, faces);
    if (faces.empty()) return 0;

    // tìm khuôn lớn nhất
    int biggestIdx = 0; float maxArea = 0.f;
    for (int i = 0; i < faces.rows; ++i) {
        float* d = faces.ptr<float>(i);
        float area = d[2] * d[3];
        if (area > maxArea) { maxArea = area; biggestIdx = i; }
    }

    jfloat rect[4] = { faces.at<float>(biggestIdx,0), faces.at<float>(biggestIdx,1),
                       faces.at<float>(biggestIdx,2), faces.at<float>(biggestIdx,3) };
    env->SetFloatArrayRegion(largestFaceRectOut, 0, 4, rect);

    return faces.rows;
}

JNIEXPORT jfloatArray JNICALL
Java_com_example_timekeeping_MainActivityCPP_ExtractFaceEmbedding(JNIEnv* env, jobject,
                                                                  jlong addrMat) {
    std::lock_guard<std::mutex> lk(gMutex);
    if (face_detector.empty() || face_recognition.empty()) return nullptr;

    Mat& rgba = *(Mat*)addrMat;
    Mat img = rgbaToSafeBgr(rgba);
    face_detector->setInputSize(img.size());

    Mat faces; face_detector->detect(img, faces);
    if (faces.empty()) return nullptr;

    int biggestIdx = 0; float maxArea = 0.f;
    for (int i = 0; i < faces.rows; ++i) {
        float area = faces.at<float>(i,2) * faces.at<float>(i,3);
        if (area > maxArea) { maxArea = area; biggestIdx = i; }
    }

    Mat aligned, embedding;
    face_recognition->alignCrop(img, faces.row(biggestIdx), aligned);
    if (aligned.empty()) return nullptr;

    face_recognition->feature(aligned, embedding);
    if (embedding.empty()) return nullptr;

    jsize len = embedding.cols;
    jfloatArray jEmb = env->NewFloatArray(len);
    env->SetFloatArrayRegion(jEmb, 0, len, (const jfloat*)embedding.ptr<float>());
    return jEmb;
}

JNIEXPORT jfloat JNICALL
Java_com_example_timekeeping_MainActivityCPP_CalculateSimilarity(JNIEnv* env, jobject,
                                                                 jfloatArray jEmb1, jfloatArray jEmb2) {
    std::lock_guard<std::mutex> lk(gMutex);
    if (face_recognition.empty()) return -1.f;

    jsize len1 = env->GetArrayLength(jEmb1);
    jsize len2 = env->GetArrayLength(jEmb2);
    if (len1 != len2) return -1.f;

    jfloat* e1 = env->GetFloatArrayElements(jEmb1, nullptr);
    jfloat* e2 = env->GetFloatArrayElements(jEmb2, nullptr);

    Mat m1(1, len1, CV_32F, e1);
    Mat m2(1, len2, CV_32F, e2);
    float sim = face_recognition->match(m1, m2, FaceRecognizerSF::DisType::FR_COSINE);

    env->ReleaseFloatArrayElements(jEmb1, e1, 0);
    env->ReleaseFloatArrayElements(jEmb2, e2, 0);
    return sim;
}

JNIEXPORT void JNICALL
Java_com_example_timekeeping_MainActivityCPP_LoadFaceDataList(JNIEnv* env, jobject,
                                                              jstring jPath) {
    const char* cpath = env->GetStringUTFChars(jPath, nullptr);
    {
        std::lock_guard<std::mutex> lk(gMutex);
        faceDataList.clear();
        ifstream in(cpath);
        if (!in.is_open()) { LOGD("File not found: %s", cpath); }
        else {
            string line; while (getline(in, line)) {
                stringstream ss(line); FaceData fd; ss >> fd.name; float v;
                while (ss >> v) fd.embedding.push_back(v);
                faceDataList.push_back(std::move(fd));
            }
        }
    }
    env->ReleaseStringUTFChars(jPath, cpath);
}

JNIEXPORT void JNICALL
Java_com_example_timekeeping_MainActivityCPP_SaveFaceDataList(JNIEnv* env, jobject,
                                                              jstring jPath) {
    const char* cpath = env->GetStringUTFChars(jPath, nullptr);
    {
        std::lock_guard<std::mutex> lk(gMutex);
        ofstream out(cpath);
        if (!out.is_open()) { LOGD("Cannot open file: %s", cpath); }
        else {
            for (const auto& fd : faceDataList) {
                out << fd.name;
                for (float v : fd.embedding) out << ' ' << v;
                out << '\n';
            }
        }
    }
    env->ReleaseStringUTFChars(jPath, cpath);
}

JNIEXPORT jstring JNICALL
Java_com_example_timekeeping_MainActivityCPP_CheckDuplicateFace(JNIEnv* env, jobject,
                                                                jfloatArray jEmb, jfloat thresh) {
    jsize len = env->GetArrayLength(jEmb);
    jfloat* data = env->GetFloatArrayElements(jEmb, nullptr);
    Mat newEmb(1, len, CV_32F, data);

    string bestName; float bestSim = 0.f;
    {
        std::lock_guard<std::mutex> lk(gMutex);
        if (face_recognition.empty()) goto END;
        for (const auto& fd : faceDataList) {
            Mat oldEmb(1, (int)fd.embedding.size(), CV_32F, (void*)fd.embedding.data());
            float s = face_recognition->match(newEmb, oldEmb, FaceRecognizerSF::DisType::FR_COSINE);
            if (s > thresh && s > bestSim) { bestSim = s; bestName = fd.name; }
        }
    }
    END:
    env->ReleaseFloatArrayElements(jEmb, data, 0);
    return bestName.empty() ? nullptr : env->NewStringUTF(bestName.c_str());
}

JNIEXPORT void JNICALL
Java_com_example_timekeeping_MainActivityCPP_UpdateFaceData(JNIEnv* env, jobject,
                                                            jstring jName, jfloatArray jEmb) {
    const char* cname = env->GetStringUTFChars(jName, nullptr);
    jsize len = env->GetArrayLength(jEmb);
    jfloat* data = env->GetFloatArrayElements(jEmb, nullptr);

    std::lock_guard<std::mutex> lk(gMutex);
    bool found = false;
    for (auto& fd : faceDataList) {
        if (fd.name == cname) { fd.embedding.assign(data, data + len); found = true; break; }
    }
    if (!found) {
        FaceData fd; fd.name = cname; fd.embedding.assign(data, data + len); faceDataList.push_back(std::move(fd));
    }

    env->ReleaseFloatArrayElements(jEmb, data, 0);
    env->ReleaseStringUTFChars(jName, cname);
}

}
