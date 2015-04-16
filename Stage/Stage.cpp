#include <windows.h>
#include <stdio.h>
#include <stdint.h>

#include "PI_GCS2_DLL.h"
#include "Stage.h"

#define NSTAGES 2

#define STAGE_0 "M-111.1DG-NEW"
#define STAGE_1 "M-111.1DG-NEW"

#define AXIS "1"

#define SAVE_CALL(ans, id) __save_call((ans), id, __FILE__, __LINE__)

static int ID[NSTAGES];
static int daisyChain = -1;

static void print_error(const char *msg, void *)
{
	printf("%s\n", msg);
}

static void (*error_callback)(const char *, void *param) = print_error;
static void *hparam = NULL;

static int
__save_call(int ans, int id, const char *file, int line)
{
	if(!ans) {
		int errorCode = PI_GetError(id);
		char szError[1024];
		PI_TranslateError(errorCode, szError, 1024);
		char msg[1024];
		sprintf(msg, "Error %d (%s) in %s, line %d\n", ans, szError, file, line);
		error_callback(msg, hparam);
		return FALSE;
	}
	return TRUE;
}

void stageSetErrorCallback(void (*callback)(const char *, void *), void *param)
{
	error_callback = callback;
	hparam = param;
}

void stageConnect(int com_port, int baud)
{
	printf("Connecting with RS-232...\n");

	int nrDevices;
	char szDevices[16 * 128];
	printf("Trying to open daisy chain on port %d\n", com_port);
	while(daisyChain < 0) {
		daisyChain = PI_OpenRS232DaisyChain(com_port, baud, &nrDevices, szDevices, 16 * 128);
		printf("Trying again\n");
	}
	printf("daisyChain = %d: %d devices\n", daisyChain, nrDevices);

	for(int i = 0, j = 1; i < NSTAGES; i++) {
		ID[i] = PI_ConnectDaisyChainDevice(daisyChain, i + 1);
		if (ID[i] < 0) {
			char msg[1024];
			sprintf(msg, "Error %d in %s, line %d\n", ID[i], __FILE__, __LINE__);
			error_callback(msg, hparam);
			return;
		}
	}

	char buffer[255];
	const char *stages[NSTAGES] = {STAGE_0, STAGE_1};
	unsigned int PARAM_MAX_VEL = 0xA;
	unsigned int PARAM_MAX_ACC = 0x4A;

	for(int i = 0; i < NSTAGES; i++) {
		int id = ID[i];
		SAVE_CALL(PI_qIDN(id, buffer, 255), id);
		SAVE_CALL(PI_qSAI_ALL(id, buffer, 255), id);
		printf("Device %d, Init axis %s, stage type %s\n", id, AXIS, stages[i]);
		SAVE_CALL(PI_qCST(id, AXIS, buffer, 255), id);
		char* pStage = strchr(buffer, '=');
		pStage++;
		if (strnicmp(stages[i], pStage, strlen(stages[i])) == 0) {
			printf("stage type already defined\n");
			continue;
		}

		SAVE_CALL(PI_CST(id, AXIS, stages[i]), id);

		double max_vel, max_acc, min_pos, max_pos;
		SAVE_CALL(PI_qSPA(id, AXIS, &PARAM_MAX_VEL, &max_vel, 0, 0), id);
		SAVE_CALL(PI_qSPA(id, AXIS, &PARAM_MAX_ACC, &max_acc, 0, 0), id);

		SAVE_CALL(PI_qTMN(id, AXIS, &min_pos), id);
		SAVE_CALL(PI_qTMX(id, AXIS, &max_pos), id);
		printf("axis %d:", i);
		printf("    maximum vel: %f\n", max_vel);
		printf("    maximum acc: %f\n", max_acc);
		printf("    minimum pos: %f\n", min_pos);
		printf("    maximum pos: %f\n", max_pos);

		SAVE_CALL(PI_ACC(id, AXIS, &max_acc), id);
		SAVE_CALL(PI_DEC(id, AXIS, &max_acc), id);
		SAVE_CALL(PI_VEL(id, AXIS, &max_vel), id);
	}
}

bool stageIsReferenceNeeded()
{
	int axis;
	for (axis = 0; axis < NSTAGES; axis++) {
		BOOL bFlag = TRUE;
		int id = ID[axis];
		BOOL bRefOK = FALSE;

		SAVE_CALL(PI_SVO(id, AXIS, &bFlag), id);
		SAVE_CALL(PI_qFRF(id, AXIS, &bRefOK), id);
		if (!bRefOK)
			return TRUE;
	}
	return FALSE;
}

void stageStopMoving()
{
	for (int axis = 0; axis < NSTAGES; axis++)
		SAVE_CALL(PI_HLT(ID[axis], AXIS), ID[axis]);
}

void stageClose()
{
	for (int axis = 0; axis < NSTAGES; axis++)
		PI_CloseConnection(ID[axis]);
	PI_CloseDaisyChain(daisyChain);
}

void stageReferenceIfNeeded()
{
	BOOL bRefOK[NSTAGES];
	BOOL allOK = TRUE;
	int axis;
	for (axis = 0; axis < NSTAGES; axis++) {
		BOOL bFlag = TRUE;
		int id = ID[axis];
		SAVE_CALL(PI_SVO(id, AXIS, &bFlag), id);
		SAVE_CALL(PI_qFRF(id, AXIS, &bRefOK[axis]), id);
		if (bRefOK[axis])
			printf("device %d, Axis %s already referenced\n", id, AXIS);
		else
			allOK = FALSE;
	}

	if (allOK)
		return;

	for(axis = 0; axis < NSTAGES; axis++) {
		if(bRefOK[axis])
			continue;

		BOOL bFlag = FALSE;
		int id = ID[axis];
		SAVE_CALL(PI_qTRS(id, AXIS, &bFlag), id);
		if(bFlag) { // stage has reference switch
			SAVE_CALL(PI_FRF(id, AXIS), id);
			printf("device %d, Reference stage for axis %s by reference switch ", id, AXIS);
		} else {
			SAVE_CALL(PI_qLIM(id, AXIS, &bFlag), id);
			if(bFlag) {
				SAVE_CALL(!PI_FNL(id, AXIS), id);
				printf("device %d, Reference stage for axis %s by negative limit switch ", id, AXIS);
			} else {
				char msg[1024];
				sprintf(msg, "Error (Stage has no reference or limit switch) in %s, line %d\n", __FILE__, __LINE__);
				error_callback(msg, hparam);
				return;
			}
		}
	}

	do {
		Sleep(500);
		allOK = TRUE;
		for(axis = 0; axis < NSTAGES; axis++) {
			SAVE_CALL(PI_IsControllerReady(ID[axis], &bRefOK[axis]), ID[axis]);
			if(!bRefOK[axis])
				allOK = FALSE;
		}
		printf(".");
	} while (! allOK);
	printf("\n");

	for(axis = 0; axis < NSTAGES; axis++) {
		SAVE_CALL(PI_qFRF(ID[axis], AXIS, &bRefOK[axis]), ID[axis]);
		if (!bRefOK[axis]) {
			char msg[1024];
			sprintf(msg, "Error (Stage not referenced) in %s, line %d\n", __FILE__, __LINE__);
			error_callback(msg, hparam);
			return;
		}
	}
}

void stageSetTarget(int axis, double pos)
{
	SAVE_CALL(PI_MOV(ID[axis], AXIS, &pos), ID[axis]); 
}

double stageGetPosition(int axis)
{
	double pos;
	SAVE_CALL(PI_qPOS(ID[axis], AXIS, &pos), ID[axis]);
	return pos;
}

void stageSetVelocity(int axis, double vel)
{
	SAVE_CALL(PI_VEL(ID[axis], AXIS, &vel), ID[axis]);
}

double stageGetVelocity(int axis)
{
	double vel;
	SAVE_CALL(PI_qVEL(ID[axis], AXIS, &vel), ID[axis]);
	return vel;
}

bool stageIsMoving(int axis)
{
	int moving;
	SAVE_CALL(PI_IsMoving(ID[axis], AXIS, &moving), ID[axis]);
	return moving;
}

void main(void)
{
	stageConnect(7, 38400);
	stageClose();
}

