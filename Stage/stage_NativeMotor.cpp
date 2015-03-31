#include "stage_NativeMotor.h"
#include "Stage.h"

static void
ThrowException(const char *msg, void *env_ptr)
{
    printf("%s\n", msg);
    JNIEnv *env = (JNIEnv *)env_ptr;
	jclass cl;
	cl = env->FindClass("java/lang/RuntimeException");
	env->ThrowNew(cl, msg);
}

static void
setExceptionHandler(JNIEnv *env)
{
    stageSetErrorCallback(ThrowException, env);
}

JNIEXPORT void JNICALL Java_stage_NativeMotor_stageConnect
  (JNIEnv *env, jclass, jint comport, jint baudrate)
{
	setExceptionHandler(env);
	stageConnect(comport, baudrate);
}

JNIEXPORT jboolean JNICALL Java_stage_NativeMotor_stageIsReferenceNeeded
  (JNIEnv *, jclass)
{
	return stageIsReferenceNeeded();
}

JNIEXPORT void JNICALL Java_stage_NativeMotor_stageReferenceIfNeeded
  (JNIEnv *, jclass)
{
	stageReferenceIfNeeded();
}

JNIEXPORT jdouble JNICALL Java_stage_NativeMotor_stageGetPosition
  (JNIEnv *, jclass, jint axis)
{
	return stageGetPosition(axis);
}

JNIEXPORT jdouble JNICALL Java_stage_NativeMotor_stageGetVelocity
  (JNIEnv *, jclass, jint axis)
{
	return stageGetVelocity(axis);
}

JNIEXPORT jboolean JNICALL Java_stage_NativeMotor_stageIsMoving
  (JNIEnv *, jclass, jint axis)
{
	return stageIsMoving(axis);
}

JNIEXPORT void JNICALL Java_stage_NativeMotor_stageSetVelocity
  (JNIEnv *, jclass, jint axis, jdouble vel)
{
	stageSetVelocity(axis, vel);
}

JNIEXPORT void JNICALL Java_stage_NativeMotor_stageSetTarget
  (JNIEnv *, jclass, jint axis, jdouble pos)
{
	stageSetTarget(axis, pos);
}

JNIEXPORT void JNICALL Java_stage_NativeMotor_stageClose
  (JNIEnv *, jclass)
{
	stageClose();
}

