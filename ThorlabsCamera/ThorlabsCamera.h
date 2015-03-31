#ifndef __THORLABS_CAMERA_H__
#define __THORLABS_CAMERA_H__

void camSetErrorCallback(void (*callback)(const char *msg, void *), void *);

void
camSetup(int camIdx);

void
camStartPreview(int camIdx);

void
camStopPreview(int camIdx);

void
camGetLastPreviewImage(int camIdx, char *image);

void
camStartSequence(int camIdx);

void
camGetNextSequenceImage(int camIdx, char *image);

void
camStopSequence(int camIdx);

void
camAcquireSequence(int camIdx);

void
camClose(int camIdx);

#endif // __THORLABS_CAMERA_H__
