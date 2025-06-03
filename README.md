# eduSPIM

## Purpose
This is a software framework for operating scientific devices with limited supervision, e.g. in an exhibition.
The particular implementation here controls [eduSPIM](http://www.eduspim.org), a light sheet microscope exhibited 
in the [TSD](http://www.tsd.de). The most important features of this software is

* **Simplicity:** 
Its UI was designed to be intuitive, illustrative and self-explanatory, so that users in the 
museum do not need to read a manual before starting to press buttons.

* **Applicability**
The software is currently used to control eduSPIM, but it runs also without any real hardware attached. In
this case, it automatically asks to download example data which is used to demonstrate the functionality
of a real fluorescent light sheet microscope.

* **Modularity**
The software defines an interface for each attached peripheral. Exchanging, e.g., a laser with a different 
brand is therefore simply a matter of implementing the laser interface for the new item.

* **Robustness:** 
It features an elaborate error handling design. This is crucial for controlling a device that is 
left unattended most of the time. eduSPIM is started from a batch script, from within a loop. If
it exits with an error code, it will automatically be restarted. If a certain hardware peripheral
fails, it will continue to run in simulating mode, i.e. use pre-acquired data to demonstrate normal
operation.

* **Maintainence:**
There is extensive logging, which can be configured to be written to a shared folder using a cloud
service. Additionally, automatic emails can be configured to be sent on startup, shutdown and on error.

* **Outreach:**
There is an accompanying [website](https://dl.dropboxusercontent.com/s/bhx0dt2q1h0hs0t/eduspim_site.html)
that shows a recent snapshot and some usage statistics for assessing the educational success of eduSPIM.


## Installation
* [Download](https://github.com/bene51/eduSPIM/releases/download/v1.1/eduSPIM.zip) the latest version from the [release](https://github.com/bene51/eduSPIM/releases) section
* Unpack it to a folder of your choice.
* On Windows: Double-click run.bat
* On Linux/Mac OS X: Open a terminal, change to the eduSPIM folder and start the run.sh script.

