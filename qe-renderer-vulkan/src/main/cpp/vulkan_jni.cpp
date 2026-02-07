#include <jni.h>
#include <android/log.h>
#include <android/native_window_jni.h>
#include "vulkan_renderer_native.h"

#define LOG_TAG "VulkanJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" {

// ========== Lifecycle ==========

JNIEXPORT jlong JNICALL
Java_com_quantum_engine_renderer_vulkan_VulkanRenderer_nativeCreate(
    JNIEnv* env, jobject obj) {
    LOGI("Creating VulkanRenderer native instance");
    
    auto* renderer = new VulkanRendererNative();
    return reinterpret_cast<jlong>(renderer);
}

JNIEXPORT jboolean JNICALL
Java_com_quantum_engine_renderer_vulkan_VulkanRenderer_nativeInitialize(
    JNIEnv* env, jobject obj, jlong handle) {
    LOGI("Initializing Vulkan");
    
    auto* renderer = reinterpret_cast<VulkanRendererNative*>(handle);
    bool success = renderer->initialize();
    
    if (!success) {
        LOGE("Failed to initialize Vulkan");
    }
    
    return success;
}

JNIEXPORT void JNICALL
Java_com_quantum_engine_renderer_vulkan_VulkanRenderer_nativeDestroy(
    JNIEnv* env, jobject obj, jlong handle) {
    LOGI("Destroying VulkanRenderer");
    
    auto* renderer = reinterpret_cast<VulkanRendererNative*>(handle);
    delete renderer;
}

// ========== Surface ==========

JNIEXPORT void JNICALL
Java_com_quantum_engine_renderer_vulkan_VulkanRenderer_nativeSetSurface(
    JNIEnv* env, jobject obj, jlong handle, jobject surface, jint width, jint height) {
    LOGI("Setting surface: %dx%d", width, height);
    
    auto* renderer = reinterpret_cast<VulkanRendererNative*>(handle);
    ANativeWindow* window = ANativeWindow_fromSurface(env, surface);
    
    if (window) {
        renderer->setSurface(window, width, height);
    } else {
        LOGE("Failed to get native window from surface");
    }
}

// ========== Frame ==========

JNIEXPORT void JNICALL
Java_com_quantum_engine_renderer_vulkan_VulkanRenderer_nativeBeginFrame(
    JNIEnv* env, jobject obj, jlong handle) {
    auto* renderer = reinterpret_cast<VulkanRendererNative*>(handle);
    renderer->beginFrame();
}

JNIEXPORT void JNICALL
Java_com_quantum_engine_renderer_vulkan_VulkanRenderer_nativeEndFrame(
    JNIEnv* env, jobject obj, jlong handle) {
    auto* renderer = reinterpret_cast<VulkanRendererNative*>(handle);
    renderer->endFrame();
}

// ========== Rendering ==========

JNIEXPORT void JNICALL
Java_com_quantum_engine_renderer_vulkan_VulkanRenderer_nativeSubmitMesh(
    JNIEnv* env, jobject obj, jlong handle, jlong meshHandle,
    jfloatArray transform, jfloatArray color) {
    
    auto* renderer = reinterpret_cast<VulkanRendererNative*>(handle);
    
    jfloat* transformData = env->GetFloatArrayElements(transform, nullptr);
    jfloat* colorData = env->GetFloatArrayElements(color, nullptr);
    
    renderer->submitMesh(meshHandle, transformData, colorData);
    
    env->ReleaseFloatArrayElements(transform, transformData, JNI_ABORT);
    env->ReleaseFloatArrayElements(color, colorData, JNI_ABORT);
}

JNIEXPORT void JNICALL
Java_com_quantum_engine_renderer_vulkan_VulkanRenderer_nativeSetViewProjection(
    JNIEnv* env, jobject obj, jlong handle,
    jfloatArray view, jfloatArray projection) {
    
    auto* renderer = reinterpret_cast<VulkanRendererNative*>(handle);
    
    jfloat* viewData = env->GetFloatArrayElements(view, nullptr);
    jfloat* projData = env->GetFloatArrayElements(projection, nullptr);
    
    renderer->setViewProjection(viewData, projData);
    
    env->ReleaseFloatArrayElements(view, viewData, JNI_ABORT);
    env->ReleaseFloatArrayElements(projection, projData, JNI_ABORT);
}

JNIEXPORT void JNICALL
Java_com_quantum_engine_renderer_vulkan_VulkanRenderer_nativeSetClearColor(
    JNIEnv* env, jobject obj, jlong handle,
    jfloat r, jfloat g, jfloat b, jfloat a) {
    
    auto* renderer = reinterpret_cast<VulkanRendererNative*>(handle);
    renderer->setClearColor(r, g, b, a);
}

JNIEXPORT void JNICALL
Java_com_quantum_engine_renderer_vulkan_VulkanRenderer_nativeSetViewport(
    JNIEnv* env, jobject obj, jlong handle,
    jint x, jint y, jint width, jint height) {
    
    auto* renderer = reinterpret_cast<VulkanRendererNative*>(handle);
    renderer->setViewport(x, y, width, height);
}

// ========== Resources ==========

JNIEXPORT jlong JNICALL
Java_com_quantum_engine_renderer_vulkan_VulkanRenderer_nativeLoadMesh(
    JNIEnv* env, jobject obj, jlong handle,
    jfloatArray vertices, jintArray indices,
    jfloatArray normals, jfloatArray uvs) {
    
    auto* renderer = reinterpret_cast<VulkanRendererNative*>(handle);
    
    jsize vertexCount = env->GetArrayLength(vertices);
    jsize indexCount = env->GetArrayLength(indices);
    jsize normalCount = env->GetArrayLength(normals);
    jsize uvCount = env->GetArrayLength(uvs);
    
    jfloat* vertexData = env->GetFloatArrayElements(vertices, nullptr);
    jint* indexData = env->GetIntArrayElements(indices, nullptr);
    jfloat* normalData = normalCount > 0 ? env->GetFloatArrayElements(normals, nullptr) : nullptr;
    jfloat* uvData = uvCount > 0 ? env->GetFloatArrayElements(uvs, nullptr) : nullptr;
    
    uint64_t meshHandle = renderer->loadMesh(
        vertexData, vertexCount,
        reinterpret_cast<uint32_t*>(indexData), indexCount,
        normalData, normalCount,
        uvData, uvCount
    );
    
    env->ReleaseFloatArrayElements(vertices, vertexData, JNI_ABORT);
    env->ReleaseIntArrayElements(indices, indexData, JNI_ABORT);
    if (normalData) env->ReleaseFloatArrayElements(normals, normalData, JNI_ABORT);
    if (uvData) env->ReleaseFloatArrayElements(uvs, uvData, JNI_ABORT);
    
    return static_cast<jlong>(meshHandle);
}

JNIEXPORT jlong JNICALL
Java_com_quantum_engine_renderer_vulkan_VulkanRenderer_nativeLoadTexture(
    JNIEnv* env, jobject obj, jlong handle,
    jbyteArray pixels, jint width, jint height, jint format) {
    
    auto* renderer = reinterpret_cast<VulkanRendererNative*>(handle);
    
    jsize size = env->GetArrayLength(pixels);
    jbyte* pixelData = env->GetByteArrayElements(pixels, nullptr);
    
    uint64_t texHandle = renderer->loadTexture(
        reinterpret_cast<uint8_t*>(pixelData),
        width, height, format
    );
    
    env->ReleaseByteArrayElements(pixels, pixelData, JNI_ABORT);
    
    return static_cast<jlong>(texHandle);
}

JNIEXPORT jlong JNICALL
Java_com_quantum_engine_renderer_vulkan_VulkanRenderer_nativeCompileShader(
    JNIEnv* env, jobject obj, jlong handle,
    jbyteArray spirvCode, jint stage) {
    
    auto* renderer = reinterpret_cast<VulkanRendererNative*>(handle);
    
    jsize codeSize = env->GetArrayLength(spirvCode);
    jbyte* codeData = env->GetByteArrayElements(spirvCode, nullptr);
    
    uint64_t shaderHandle = renderer->compileShader(
        reinterpret_cast<uint8_t*>(codeData),
        codeSize, stage
    );
    
    env->ReleaseByteArrayElements(spirvCode, codeData, JNI_ABORT);
    
    return static_cast<jlong>(shaderHandle);
}

JNIEXPORT jlong JNICALL
Java_com_quantum_engine_renderer_vulkan_VulkanRenderer_nativeCreateGraphicsPipeline(
    JNIEnv* env, jobject obj, jlong handle,
    jlong vertexShader, jlong fragmentShader, jintArray config) {
    
    auto* renderer = reinterpret_cast<VulkanRendererNative*>(handle);
    
    jint* configData = env->GetIntArrayElements(config, nullptr);
    
    uint64_t pipelineHandle = renderer->createGraphicsPipeline(
        vertexShader, fragmentShader, configData
    );
    
    env->ReleaseIntArrayElements(config, configData, JNI_ABORT);
    
    return static_cast<jlong>(pipelineHandle);
}

// ========== Compute ==========

JNIEXPORT void JNICALL
Java_com_quantum_engine_renderer_vulkan_VulkanRenderer_nativeDispatchCompute(
    JNIEnv* env, jobject obj, jlong handle,
    jlong computeShader, jint groupsX, jint groupsY, jint groupsZ) {
    
    auto* renderer = reinterpret_cast<VulkanRendererNative*>(handle);
    renderer->dispatchCompute(computeShader, groupsX, groupsY, groupsZ);
}

// ========== Ray Tracing ==========

JNIEXPORT jboolean JNICALL
Java_com_quantum_engine_renderer_vulkan_VulkanRenderer_nativeSupportsRayTracing(
    JNIEnv* env, jobject obj, jlong handle) {
    
    auto* renderer = reinterpret_cast<VulkanRendererNative*>(handle);
    return renderer->supportsRayTracing();
}

JNIEXPORT void JNICALL
Java_com_quantum_engine_renderer_vulkan_VulkanRenderer_nativeTraceRays(
    JNIEnv* env, jobject obj, jlong handle,
    jlong raygenShader, jlong missShader, jlong hitShader,
    jint width, jint height) {
    
    auto* renderer = reinterpret_cast<VulkanRendererNative*>(handle);
    renderer->traceRays(raygenShader, missShader, hitShader, width, height);
}

// ========== Info ==========

JNIEXPORT jobjectArray JNICALL
Java_com_quantum_engine_renderer_vulkan_VulkanRenderer_nativeGetVulkanInfo(
    JNIEnv* env, jobject obj, jlong handle) {
    
    auto* renderer = reinterpret_cast<VulkanRendererNative*>(handle);
    
    VulkanInfo info = renderer->getVulkanInfo();
    
    // Crear array de Object[8]
    jclass objectClass = env->FindClass("java/lang/Object");
    jobjectArray result = env->NewObjectArray(8, objectClass, nullptr);
    
    // Strings
    env->SetObjectArrayElement(result, 0, env->NewStringUTF(info.deviceName.c_str()));
    env->SetObjectArrayElement(result, 1, env->NewStringUTF(info.apiVersion.c_str()));
    env->SetObjectArrayElement(result, 2, env->NewStringUTF(info.driverVersion.c_str()));
    
    // Integers
    jclass intClass = env->FindClass("java/lang/Integer");
    jmethodID intConstructor = env->GetMethodID(intClass, "<init>", "(I)V");
    env->SetObjectArrayElement(result, 3, env->NewObject(intClass, intConstructor, info.vendorId));
    
    env->SetObjectArrayElement(result, 4, env->NewStringUTF(info.deviceType.c_str()));
    env->SetObjectArrayElement(result, 5, env->NewObject(intClass, intConstructor, info.maxTextureSize));
    
    // Booleans
    jclass boolClass = env->FindClass("java/lang/Boolean");
    jmethodID boolConstructor = env->GetMethodID(boolClass, "<init>", "(Z)V");
    env->SetObjectArrayElement(result, 6, env->NewObject(boolClass, boolConstructor, info.supportsRayTracing));
    env->SetObjectArrayElement(result, 7, env->NewObject(boolClass, boolConstructor, info.supportsMeshShaders));
    
    return result;
}

} // extern "C"
