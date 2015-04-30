#include "enumerate_serial.h"

#include "windows.h"
#include "setupapi.h"
#include "devguid.h"
#include "stdio.h"

void main(void)
{
	char friendlyNames[1024];
	char portNames[1024];
	int len = sizeof(friendlyNames);
	char *delimiter = "\n";
	enumerate_serials(friendlyNames, portNames, len, delimiter);
	printf("friendly names:\n%s\n", friendlyNames);
	printf("port name:\n%s\n", portNames);
}

void
enumerate_serials(
		char *friendlyNames,
		char *portNames,
		int len,
		char *delimiter)
{
	DWORD DeviceIndex = 0;
	HDEVINFO hDeviceInfo = SetupDiGetClassDevs(
			&GUID_DEVCLASS_PORTS,
			NULL,
			NULL,
			DIGCF_PRESENT);
	SP_DEVINFO_DATA DeviceInfoData;
	ZeroMemory(&DeviceInfoData, sizeof(SP_DEVINFO_DATA));
	DeviceInfoData.cbSize = sizeof(SP_DEVINFO_DATA);

	friendlyNames[0] = '\0';
	portNames[0] = '\0';


	while(SetupDiEnumDeviceInfo(
			hDeviceInfo,
			DeviceIndex++,
			&DeviceInfoData)) {
		DWORD regDataType;
		BYTE hardwareId[300];
		if(SetupDiGetDeviceRegistryProperty(
				hDeviceInfo, 
				&DeviceInfoData, 
				SPDRP_FRIENDLYNAME,
				&regDataType,
				hardwareId,
				sizeof(hardwareId), 
				NULL)) {
			HKEY regKey;
			long ret;
			BYTE value[300];
			LONG lpcValue = sizeof(value);
			_snprintf_s(friendlyNames, len, _TRUNCATE, "%s%s%s", friendlyNames, hardwareId, delimiter);
			regKey = SetupDiOpenDevRegKey(
					hDeviceInfo,
					&DeviceInfoData,
					DICS_FLAG_GLOBAL,
					0,
					DIREG_DEV,
					KEY_QUERY_VALUE);
			ret = GetLastError();
			if(ret == ERROR_SUCCESS) {
				ret = RegQueryValueEx(regKey,
						"PortName",
						NULL,
						NULL,
						value,
						(LPDWORD)&lpcValue);
				if(ret != ERROR_SUCCESS)
					printf("RegQueryValueEx() returned %d\n", ret);
			} else {
				printf("SetupDiOpenDevRegKey returned %d\n", ret);
			}

			_snprintf_s(portNames, len, _TRUNCATE, "%s%s%s", portNames, value, delimiter);
		}
	}
}

