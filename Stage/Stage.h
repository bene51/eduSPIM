#ifndef _STAGE_H_
#define _STAGE_H_

void stageSetErrorCallback(void (*callback)(const char *, void *), void *param);

void stageConnect(int com_port, int baud, int n_stages, const char *stages[]);

bool stageIsReferenceNeeded(int axis);

void stageClose();

void stageReferenceIfNeeded(int axis);

void stageSetTarget(int axis, double pos);

double stageGetPosition(int axis);

void stageSetVelocity(int axis, double vel);

double stageGetVelocity(int axis);

bool stageIsMoving(int axis);

void stageStopMoving(int axis);

void stageSetAbsolutePosition(int axis, double val);

#endif // _STAGE_H_

