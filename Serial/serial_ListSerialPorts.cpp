#include "serial_ListSerialPorts.h"

#include "enumerate_serial.h"

JNIEXPORT jobjectArray JNICALL Java_serial_ListSerialPorts_enumeratePorts
  (JNIEnv *env, jclass)
{
	jobjectArray ret;

	char friendlyNames[1024];
	char portNames[1024];
	int len = sizeof(friendlyNames);
	char *delimiter = "\n";
	enumerate_serials(friendlyNames, portNames, len, delimiter);

	ret = (jobjectArray)env->NewObjectArray(2,
			env->FindClass("java/lang/String"),
			env->NewStringUTF(""));
	env->SetObjectArrayElement(ret, 0, env->NewStringUTF(friendlyNames));
	env->SetObjectArrayElement(ret, 1, env->NewStringUTF(portNames));

	return ret;
}

