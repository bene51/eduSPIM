#include "stage_NativeMotor.h"

#include <stdlib.h>
#include "Stage.h"

static void
ThrowException(const char *msg, void *env_ptr)
{
    printf("%s\n", msg);
    JNIEnv *env = (JNIEnv *)env_ptr;
	jclass cl;
	cl = env->FindClass("stage/MotorException");
	env->ThrowNew(cl, msg);
}

static void
setExceptionHandler(JNIEnv *env)
{
    stageSetErrorCallback(ThrowException, env);
}

JNIEXPORT void JNICALL Java_stage_NativeMotor_stageConnect
  (JNIEnv *env, jclass, jint comport, jint baudrate, jobjectArray stages)
{
	setExceptionHandler(env);
	int nstages = env->GetArrayLength(stages);
	const char **c_stages = (const char **)malloc(nstages * sizeof(const char*));
	for(int i = 0; i < nstages; i++) {
		jobject obj = env->GetObjectArrayElement(stages, i);
		c_stages[i] = env->GetStringUTFChars((jstring)obj, NULL);
	}
	stageConnect(comport, baudrate, nstages, c_stages);
	for(int i = 0; i < nstages; i++) {
		jobject obj = env->GetObjectArrayElement(stages, i);
		env->ReleaseStringUTFChars((jstring)obj, c_stages[i]);
	}
	free(c_stages);
}

JNIEXPORT jboolean JNICALL Java_stage_NativeMotor_stageIsReferenceNeeded
  (JNIEnv *, jclass, jint axis)
{
	return stageIsReferenceNeeded(axis);
}

JNIEXPORT void JNICALL Java_stage_NativeMotor_stageReferenceIfNeeded
  (JNIEnv *, jclass, jint axis)
{
	stageReferenceIfNeeded(axis);
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

JNIEXPORT void JNICALL Java_stage_NativeMotor_stageStopMoving
  (JNIEnv *, jclass, jint axis)
{
	stageStopMoving(axis);
}

JNIEXPORT void JNICALL Java_stage_NativeMotor_stageClose
  (JNIEnv *, jclass)
{
	stageClose();
}

JNIEXPORT void JNICALL Java_stage_NativeMotor_stageSetAbsolutePosition
  (JNIEnv *, jclass, jint axis, jdouble val)
{
	stageSetAbsolutePosition(axis, val);
}

