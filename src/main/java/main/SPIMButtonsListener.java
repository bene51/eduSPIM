package main;

import static stage.IMotor.Y_AXIS;
import static stage.IMotor.Z_AXIS;
import stage.IMotor;
import buttons.AbstractButtons;
import buttons.ButtonsListener;

public class SPIMButtonsListener implements ButtonsListener {

	private Microscope microscope;

	public SPIMButtonsListener(Microscope m) {
		this.microscope = m;
	}

	@Override
	public void buttonPressed(int button) {
		System.out.println("mic: button pressed " + button);
		// ignore buttons if timelapse is running
		if(microscope.isTimelapseRunning())
			return;

		while(microscope.isBusy()) {
			Microscope.sleep(30);
		}
		if(microscope.getButtonDown() != button)
			return;

		switch(button) {
		case AbstractButtons.BUTTON_INFO:
			infoOn();
			break;
		case AbstractButtons.BUTTON_LASER:
			manualLaserOn();
			break;
		case AbstractButtons.BUTTON_STACK:
			acquireStack();
			break;
		case AbstractButtons.BUTTON_Y_DOWN:
			startPreview(button, Y_AXIS, Math.max(Preferences.getStackYStart(), Preferences.getStackYEnd()));
			break;
		case AbstractButtons.BUTTON_Y_UP:
			startPreview(button, Y_AXIS, Math.min(Preferences.getStackYStart(), Preferences.getStackYEnd()));
			break;
		case AbstractButtons.BUTTON_Z_DOWN:
			startPreview(button, Z_AXIS, Math.min(Preferences.getStackZStart(), Preferences.getStackZEnd()));
			break;
		case AbstractButtons.BUTTON_Z_UP:
			startPreview(button, Z_AXIS, Math.max(Preferences.getStackZStart(), Preferences.getStackZEnd()));
			break;
		}
		microscope.requestFocus();
	}

	@Override
	public void buttonReleased(int button) {
		switch(button) {
		case AbstractButtons.BUTTON_INFO:
			infoOff();
			break;
		case AbstractButtons.BUTTON_LASER:
			manualLaserOff();
			break;
		case AbstractButtons.BUTTON_STACK:
			break;
		case AbstractButtons.BUTTON_Y_DOWN:
			break;
		case AbstractButtons.BUTTON_Y_UP:
			break;
		case AbstractButtons.BUTTON_Z_DOWN:
			break;
		case AbstractButtons.BUTTON_Z_UP:
			break;
		}
		microscope.requestFocus();
	}

	private void acquireStack() {
		try {
			microscope.acquireStack();
		} catch (Throwable e) {
			ExceptionHandler.handleException("Error during stack acquisition, trying to close and re-initialize the hardware", e);

			microscope.resetBusy();
			try {
				microscope.closeHardware();
				microscope.initHardware();
				microscope.acquireStack();
			} catch(Throwable ex) {
				ExceptionHandler.handleException("Error during stack acquisition after re-initializing the hardware, restarting the software", ex);
				microscope.shutdown(Microscope.EXIT_STACK_ERROR);
			}
		}
	}

	private void startPreview(int button, int axis, double target) {
		double y = Preferences.getStackYStart();
		double z = Preferences.getStackZStart();
		IMotor motor = microscope.getMotor();
		try {
			y = motor.getPosition(Y_AXIS);
			z = motor.getPosition(Z_AXIS);
			microscope.startPreview(button, axis, target);
		} catch (Throwable e) {
			ExceptionHandler.handleException("Error during preview, trying to close and re-initialize the hardware", e);

			microscope.resetBusy();
			try {
				microscope.closeHardware();
				microscope.initHardware();
				microscope.getMotor().setTarget(Y_AXIS, y);
				microscope.getMotor().setTarget(Z_AXIS, z);
				while(motor.isMoving())
					Microscope.sleep(50);
				microscope.startPreview(button, axis, target);
			} catch(Throwable ex) {
				ExceptionHandler.handleException("Error during preview after re-initializing the hardware, restarting the software", e);
				microscope.shutdown(Microscope.EXIT_PREVIEW_ERROR);
			}
		}
	}

	private void infoOn() {
		microscope.showInfo();
	}

	private void infoOff() {
		microscope.closeInfo();
	}

	private void manualLaserOn() {
		try {
			microscope.manualLaserOn();
		} catch (Throwable e) {
			ExceptionHandler.handleException("Laser error during manual laser on, trying to close and re-initialize the hardware", e);

			try {
				microscope.closeHardware();
				microscope.initHardware();
				microscope.manualLaserOn();
			} catch(Throwable ex) {
				ExceptionHandler.handleException("Error during manual laser on after re-initializing the hardware, restarting the software", e);
				microscope.shutdown(Microscope.EXIT_MANUAL_LASER_ERROR);
			}
		}
	}

	private void manualLaserOff() {
		try {
			microscope.manualLaserOff();
		} catch (Throwable e) {
			ExceptionHandler.handleException("Laser error during manual laser off, trying to close and re-initialize the hardware", e);

			try {
				microscope.closeHardware();
				microscope.initHardware();
			} catch(Throwable ex) {
				ExceptionHandler.handleException("Error during manual laser off after re-opening the laser, restarting the software", e);
				microscope.shutdown(Microscope.EXIT_MANUAL_LASER_ERROR);
			}
		}
	}
}
