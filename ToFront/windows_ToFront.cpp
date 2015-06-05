#include "windows_ToFront.h"
#include "windows.h"

void toFront(void)
{
	HWND hndl = NULL;
	printf("hi\n");
	hndl = FindWindowEx(NULL, NULL, NULL, "Sponsored session");
	printf("Sponsored session window = %p\n", hndl);
	if(hndl != NULL) {
		BOOL tmp = SetForegroundWindow(hndl);
		if(tmp == NULL) {
			printf("SetForegroundWindow failed\n");
		} else {
			keybd_event(VK_RETURN, 0, KEYEVENTF_EXTENDEDKEY | 0, 0);
			keybd_event(VK_RETURN, 0, KEYEVENTF_EXTENDEDKEY | KEYEVENTF_KEYUP, 0);
		}
	}

	hndl = FindWindowEx(NULL, NULL, NULL, "Display");
	printf("eduSPIM window = %p\n", hndl);
	if(hndl != NULL) {
		BOOL tmp = SetForegroundWindow(hndl);
		printf("SetForegroundWindow failed\n");
	}
}

JNIEXPORT void JNICALL Java_windows_ToFront_toFront
  (JNIEnv *, jclass)
{
	toFront();
}

