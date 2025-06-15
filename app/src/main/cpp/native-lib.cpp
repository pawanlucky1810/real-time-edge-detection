#include <jni.h>
#include <string>
#include <opencv2/opencv.hpp>
#define LOG_TAG "NativeLib"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT jstring JNICALL
Java_com_pawan_android_ImageTransformer_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_pawan_android_ImageTransformer_transformImage(JNIEnv *env, jobject /* this */,
                                                                           jbyteArray image_data,
                                                                           jint width, jint height) {

    jbyte* data = env->GetByteArrayElements(image_data, nullptr);
    int length = env->GetArrayLength(image_data);

    cv::Mat input_image(height, width, CV_8UC4, reinterpret_cast<unsigned char*>(data));
    /**
     First convert the color image to grayscale, then apply Canny edge detection.
    **/
    cv::Mat gray_image;
    cv::Mat edges;
    cv::cvtColor(input_image, gray_image, cv::COLOR_RGBA2GRAY);
    cv::Canny(gray_image, edges, 100, 200);

    // convert gray to rgba
    cv::Mat output_image;
    cv::cvtColor(edges, output_image, cv::COLOR_GRAY2RGBA);

    jbyteArray result = env->NewByteArray(length);
    env->SetByteArrayRegion(result, 0, length, reinterpret_cast<jbyte *>(output_image.data));


    return result;

}