#include "cam_NativeCamera.h"
#include "ThorlabsCamera.h"

static void
ThrowException(const char *msg, void *env_ptr)
{
    printf("%s\n", msg);
    JNIEnv *env = (JNIEnv *)env_ptr;
	jclass cl;
	cl = env->FindClass("java/lang/RuntimeException");
	env->ThrowNew(cl, msg);
}

static void setExceptionHandler(JNIEnv *env)
{
    camSetErrorCallback(ThrowException, env);
}

JNIEXPORT void JNICALL Java_cam_NativeCamera_camSetup
  (JNIEnv *env, jclass, jint camIdx)
{
    setExceptionHandler(env);
    camSetup(camIdx);
}

JNIEXPORT void JNICALL Java_cam_NativeCamera_camStartPreview
  (JNIEnv *, jclass, jint camIdx)
{
    camStartPreview(camIdx);
}

JNIEXPORT void JNICALL Java_cam_NativeCamera_camStopPreview
  (JNIEnv *, jclass, jint camIdx)
{
    camStopPreview(camIdx);
}

JNIEXPORT void JNICALL Java_cam_NativeCamera_camGetPreviewImage
  (JNIEnv *env, jclass, jint camIdx, jbyteArray ret)
{
    char *frame = (char *)env->GetPrimitiveArrayCritical(ret, 0);
    camGetLastPreviewImage(camIdx, frame);
    env->ReleasePrimitiveArrayCritical(ret, frame, 0);
}

JNIEXPORT void JNICALL Java_cam_NativeCamera_camStartSequence
  (JNIEnv *, jclass, jint camIdx)
{
    camStartSequence(camIdx);
}

JNIEXPORT void JNICALL Java_cam_NativeCamera_camGetNextSequenceImage
  (JNIEnv *env, jclass, jint camIdx, jbyteArray ret)
{
    char *frame = (char *)env->GetPrimitiveArrayCritical(ret, 0);
    camGetNextSequenceImage(camIdx, frame);
    env->ReleasePrimitiveArrayCritical(ret, frame, 0);
}

JNIEXPORT void JNICALL Java_cam_NativeCamera_camStopSequence
  (JNIEnv *, jclass, jint camIdx)
{
    camStopSequence(camIdx);
}

JNIEXPORT void JNICALL Java_cam_NativeCamera_camClose
  (JNIEnv *, jclass, jint camIdx)
{
    camClose(camIdx);
}

