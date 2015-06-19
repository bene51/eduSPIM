#include "windows_ToFront.h"
#include "windows.h"

void toFront(void)
{
	HWND hndl = NULL;
	printf("hi\n");
	hndl = FindWindowEx(NULL, NULL, NULL, "Sponsored session");
	if(hndl != NULL) {
		BOOL tmp = SetForegroundWindow(hndl);
		if(tmp == NULL) {
			printf("Could not close teamviewer window\n", hndl);
		} else {
			keybd_event(VK_RETURN, 0, KEYEVENTF_EXTENDEDKEY | 0, 0);
			keybd_event(VK_RETURN, 0, KEYEVENTF_EXTENDEDKEY | KEYEVENTF_KEYUP, 0);
			printf("Closed teamviewer window\n", hndl);
		}
	}

	hndl = FindWindowEx(NULL, NULL, NULL, "Display");
	if(hndl != NULL) {
		BOOL tmp = SetForegroundWindow(hndl);
	}
}

JNIEXPORT void JNICALL Java_windows_ToFront_toFront
  (JNIEnv *, jclass)
{
	toFront();
}

