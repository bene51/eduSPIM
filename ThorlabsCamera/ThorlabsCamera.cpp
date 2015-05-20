#include <stdio.h>
#include "include/uc480.h"

#define SAVE_CALL(ans, cam) __save_call((ans), camIdx, __FILE__, __LINE__)

#define N_BUFFERS    100
#define WIDTH       1280
#define HEIGHT      1024


static HCAM *cameras = 0;
static char ***buffers = NULL;
static int nCameras;

void print_error(const char *msg, void *)
{
	printf("%s\n", msg);
}

static void (*error_callback)(const char *, void *param) = print_error;

static void *hparam = NULL;

static int
__save_call(int ans, int camIdx, const char *file, int line)
{
	if(ans != IS_SUCCESS) {
		IS_CHAR *pcErr = NULL;
		is_GetError(cameras[camIdx], &ans, &pcErr);
		char msg[1024];
		sprintf(msg, "Error %d (%s) in %s, line %d\n",
				ans, pcErr, file, line);
		error_callback(msg, hparam);
		return FALSE;
	}
	return TRUE;
}

void
camSetErrorCallback(void (*callback)(const char *, void *), void *param)
{
	error_callback = callback;
	hparam = param;
}

static void
print_cam_info(int camIdx)
{
	CAMINFO info;
	if(!SAVE_CALL(is_GetCameraInfo(cameras[camIdx], &info), camIdx))
		return;
	printf("ID: %s\n", info.ID);
	printf("Version: %s\n", info.Version);
	printf("Date: %s\n", info.Date);
	printf("Select: %d\n", info.Select);
}

static void
print_sensor_info(int camIdx)
{
	SENSORINFO info;
	if(!SAVE_CALL(is_GetSensorInfo(cameras[camIdx], &info), camIdx))
		return;
	int w = info.nMaxWidth;
	int h = info.nMaxHeight;
	printf("sensor size: %d x %d\n", w, h);
	printf("global shutter: %d\n", info.bGlobShutter);
	printf("pixel size: %d\n", info.wPixelSize);
}

static void
print_format_list(int camIdx)
{
	UINT count;
	UINT bytesNeeded = sizeof(IMAGE_FORMAT_LIST);
	HCAM cam = cameras[camIdx];
	if(!SAVE_CALL(is_ImageFormat(cam,
			IMGFRMT_CMD_GET_NUM_ENTRIES,
			&count, sizeof(count)), camIdx))
		return;
	bytesNeeded += (count - 1) * sizeof(IMAGE_FORMAT_INFO);
	void * ptr = malloc(bytesNeeded);

	IMAGE_FORMAT_LIST *pformatList = (IMAGE_FORMAT_LIST *) ptr;
	pformatList->nSizeOfListEntry = sizeof(IMAGE_FORMAT_INFO);
	pformatList->nNumListElements = count;
	if(!SAVE_CALL(is_ImageFormat(cam,
			IMGFRMT_CMD_GET_LIST,
			pformatList,
			bytesNeeded), camIdx)) {
		free(ptr);
		return;
	}
	for(unsigned int i = 0; i < count; i++)
		printf("format %d: %s\n",
			i, pformatList->FormatInfo[i].strFormatName);
	free(ptr);
}

void
camSetup(int camIdx)
{
	if(cameras == NULL) {
		printf("initializing cameras\n");
		nCameras = 0;
		SAVE_CALL(is_GetNumberOfCameras(&nCameras), camIdx);
		printf("%d camera(s) connected\n", nCameras);
		if(nCameras == 0) {
			error_callback("No camera connected", hparam);
			return;
		}
		cameras = (HCAM *)malloc(nCameras * sizeof(HCAM));
		cameras[0] = 1;
		cameras[1] = 2;
		buffers = (char ***)malloc(nCameras * sizeof(char *));
	}
	if(!SAVE_CALL(is_InitCamera(&cameras[camIdx], NULL), camIdx))
		return;
	HCAM cam = cameras[camIdx];

	print_sensor_info(camIdx);
	print_cam_info(camIdx);
	print_format_list(camIdx);

	printf("Setting shutter mode to rolling shutter\n");
	INT shutterMode = IS_DEVICE_FEATURE_CAP_SHUTTER_MODE_ROLLING;
	if(!SAVE_CALL(is_DeviceFeature(cam,
			IS_DEVICE_FEATURE_CMD_SET_SHUTTER_MODE,
			(void *)&shutterMode,
			sizeof(shutterMode)), camIdx))
		return;


	int cm = is_SetColorMode(cam, IS_GET_COLOR_MODE);
	printf("Found previous color mode: %d\n", cm);

	int bpp = 8;
	cm = IS_CM_MONO8;
	if(!SAVE_CALL(is_SetColorMode(cam, cm), camIdx))
		return;
	printf("Setting color mode to MONO8\n");

	IS_RECT aoi;
	aoi.s32X = 0;
	aoi.s32Y = 0;
	aoi.s32Width = WIDTH;
	aoi.s32Height = HEIGHT;
	if(!SAVE_CALL(is_AOI(cam,
			IS_AOI_IMAGE_SET_AOI,
			(void *)&aoi,
			sizeof(aoi)), camIdx))
		return;
	printf("Setting AOI to fullframe\n");

	printf("Setting flash mode to 'freerun high active'\n");
	UINT nMode = IO_FLASH_MODE_FREERUN_HI_ACTIVE;
	if(!SAVE_CALL(is_IO(cam,
			IS_IO_CMD_FLASH_SET_MODE,
			(void *)&nMode,
			sizeof(nMode)), camIdx))
		return;

	printf("Setting automatic flash parameters\n");
	IO_FLASH_PARAMS flashParams;
	if(!SAVE_CALL(is_IO(cam,
			IS_IO_CMD_FLASH_APPLY_GLOBAL_PARAMS,
			(void *)&flashParams, sizeof(flashParams)), camIdx))
		return;
	if(!SAVE_CALL(is_IO(cam,
			IS_IO_CMD_FLASH_GET_PARAMS,
			(void *)&flashParams, sizeof(flashParams)), camIdx))
		return;
	printf("    flash delay: %d\n", flashParams.s32Delay);
	printf("    flash duration: %d\n", flashParams.u32Duration);

	UINT nRange[3];
	ZeroMemory(nRange, sizeof(nRange));
	if(!SAVE_CALL(is_PixelClock(cam,
			IS_PIXELCLOCK_CMD_GET_RANGE,
			(void *)nRange,
			sizeof(nRange)), camIdx))
		return;

	printf("Setting maximum pixel range: %d\n", nRange[1]);
	if(!SAVE_CALL(is_PixelClock(cam,
			IS_PIXELCLOCK_CMD_SET,
			&nRange[1],
			sizeof(nRange[1])), camIdx))
		return;

	double min, max, interval;
	if(!SAVE_CALL(is_GetFrameTimeRange(cam,
			&min, &max, &interval), camIdx))
		return;
	printf("Possible frame times: %f (min), %f (max), %f (interval)\n",
			min, max, interval);

	double tfps = 30, rfps; // 1 / min;
	if(!SAVE_CALL(is_SetFrameRate(cam, tfps, &rfps), camIdx))
		return;
	printf("Minimum framerate = %f\n", 1.0 / max);
	printf("Maximum framerate = %f\n", 1.0 / min);
	printf("Setting frame rate to %f, real framerate is %f\n", tfps, rfps);

	double exposure = 0;
	if(!SAVE_CALL(is_Exposure(cam,
			IS_EXPOSURE_CMD_SET_EXPOSURE,
			&exposure,
			sizeof(exposure)), camIdx))
		return;
	printf("Setting maximum exposure time: %f\n", exposure);

	int bid;
	buffers[camIdx] = (char **)malloc(N_BUFFERS * sizeof(char *));

	for(int i = 0; i < N_BUFFERS; i++) {
		if(!SAVE_CALL(is_AllocImageMem(cam,
				WIDTH,
				HEIGHT,
				bpp,
				&buffers[camIdx][i],
				&bid), camIdx))
			return;
		if(!SAVE_CALL(is_AddToSequence(cam,
				buffers[camIdx][i],
				bid), camIdx))
			return;
	}
}

double
camGetExposuretime(int camIdx)
{
	double exposure;
	SAVE_CALL(is_Exposure(cameras[camIdx],
			IS_EXPOSURE_CMD_GET_EXPOSURE,
			&exposure,
			sizeof(exposure)), camIdx);
	return exposure;
}

void
camSetExposuretime(int camIdx, double *exposure)
{
	SAVE_CALL(is_Exposure(cameras[camIdx],
			IS_EXPOSURE_CMD_SET_EXPOSURE,
			exposure,
			8), camIdx);
}

void
camStartPreview(int camIdx)
{
	SAVE_CALL(is_CaptureVideo(cameras[camIdx], IS_DONT_WAIT), camIdx);
}

void
camStopPreview(int camIdx)
{
	SAVE_CALL(is_StopLiveVideo(cameras[camIdx], IS_WAIT), camIdx);
}

void
camGetLastPreviewImage(int camIdx, char *image)
{
	char *next, *last;
	int id, lastId;
	HCAM cam = cameras[camIdx];
	SAVE_CALL(is_GetActSeqBuf(cam, &id, &next, &last), camIdx);
	lastId = (N_BUFFERS + id - 2) % N_BUFFERS + 1;
	printf("id = %d lastId = %d\n", id, lastId);
//	SAVE_CALL(is_LockSeqBuf(cam, lastId, last));
	SAVE_CALL(is_CopyImageMem(cam, last, lastId, image), camIdx);
//	SAVE_CALL(is_UnlockSeqBuf(cam, lastId, last));
//	SAVE_CALL(is_LockSeqBuf(cam, id, next));
//	SAVE_CALL(is_CopyImageMem(cam, next, id, image));
//	SAVE_CALL(is_UnlockSeqBuf(cam, id, next));
}

void
camStartSequence(int camIdx)
{
	int start = GetTickCount();
	HCAM cam = cameras[camIdx];
	if(!SAVE_CALL(is_InitImageQueue(cam, 0), camIdx))
		return;
	if(!SAVE_CALL(is_CaptureVideo(cam, IS_DONT_WAIT), camIdx))
		return;
	int end = GetTickCount();
	printf("Starting acquisition took %d ms\n", (end - start));
}

void
camGetNextSequenceImage(int camIdx, char *image)
{
	int nMemID = 0;
	char *pBuffer = NULL;
	HCAM cam = cameras[camIdx];
	SAVE_CALL(is_WaitForNextImage(cam, 20000, &pBuffer, &nMemID), camIdx);
	printf("got image %d\n", nMemID);
	SAVE_CALL(is_CopyImageMem(cam, pBuffer, nMemID, image), camIdx);
	SAVE_CALL(is_UnlockSeqBuf(cam, nMemID, pBuffer), camIdx);
}

void
camStopSequence(int camIdx)
{
	HCAM cam = cameras[camIdx];
	SAVE_CALL(is_StopLiveVideo(cam, IS_WAIT), camIdx);
	SAVE_CALL(is_ExitImageQueue(cam), camIdx);
}

double
camGetFramerate(int camIdx)
{
	double fps;
	SAVE_CALL(is_SetFrameRate(cameras[camIdx], IS_GET_FRAMERATE, &fps), camIdx);
	return fps;
}

void
camSetFramerate(int camIdx, double *fps)
{
	double rfps;
	if(!SAVE_CALL(is_SetFrameRate(cameras[camIdx], *fps, &rfps), camIdx))
		return;
	*fps = rfps;
}

int
camGetGain(int camIdx)
{
	return is_SetHardwareGain(
			cameras[camIdx],
			IS_GET_MASTER_GAIN,
			IS_IGNORE_PARAMETER,
			IS_IGNORE_PARAMETER,
			IS_IGNORE_PARAMETER);
}

void
camSetGain(int camIdx, int gain)
{
	SAVE_CALL(is_SetHardwareGain(
			cameras[camIdx],
			gain,
			IS_IGNORE_PARAMETER,
			IS_IGNORE_PARAMETER,
			IS_IGNORE_PARAMETER), camIdx);
}

void
camClose(int camIdx)
{
	HCAM cam = cameras[camIdx];
	SAVE_CALL(is_ClearSequence(cam), camIdx);

	for(int i = 0; i < N_BUFFERS; i++)
		SAVE_CALL(is_FreeImageMem(cam, buffers[camIdx][i], i + 1), camIdx);

	free(buffers[camIdx]);
	buffers[camIdx] = NULL;

	SAVE_CALL(is_ExitCamera(cam), camIdx);
	cameras[camIdx] = 0;

	// check whether this was the last open camera
	int connectedCameras = 0;
	for(int i = 0; i < nCameras; i++) {
		if(cameras[i] != 0)
			connectedCameras++;
	}

	if(connectedCameras == 0) {
		free(cameras);
		cameras = NULL;
		free(buffers);
		buffers = NULL;
	}
}

void
main(void) {
	camSetup(0);
	camStartPreview(0);

	char input[1000];
	char *frame = (char *)malloc(WIDTH * HEIGHT);
	while(true) {
		printf("Press <Enter> to capture next preview image, ");
		printf("type 'q' to exit.");
		gets(input);
		if(input[0] == 'q')
			break;
		camGetLastPreviewImage(0, frame);
		printf("%d\n", frame[0]);
	}
	camStopPreview(0);
	free(frame);

	camClose(0);
}

