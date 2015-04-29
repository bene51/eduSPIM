package main;

import static stage.IMotor.Y_AXIS;
import static stage.IMotor.Z_AXIS;
import stage.IMotor;
import stage.MotorException;
import buttons.AbstractButtons;
import buttons.ButtonsListener;
import cam.CameraException;

public class SPIMButtonsListener implements ButtonsListener {

	private Microscope microscope;

	public SPIMButtonsListener(Microscope m) {
		this.microscope = m;
	}

	@Override
	public void buttonPressed(int button) {
		System.out.println("mic: button pressed " + button);
		if(microscope.isBusy()) {
			microscope.requestFocus();
			return;
		}

		switch(button) {
		case AbstractButtons.BUTTON_LASER:
			// TODO move mirror away and switch laser on
			break;
		case AbstractButtons.BUTTON_STACK:
			acquireStack();
			break;
		case AbstractButtons.BUTTON_Y_DOWN:
			startPreview(button, Y_AXIS, false, Preferences.getStackYStart());
			break;
		case AbstractButtons.BUTTON_Y_UP:
			startPreview(button, Y_AXIS, true,  Preferences.getStackYEnd());
			break;
		case AbstractButtons.BUTTON_Z_DOWN:
			startPreview(button, Z_AXIS, false, Preferences.getStackZStart());
			break;
		case AbstractButtons.BUTTON_Z_UP:
			startPreview(button, Z_AXIS, true,  Preferences.getStackZEnd());
			break;
		}
		microscope.requestFocus();
	}

	@Override
	public void buttonReleased(int button) {
		switch(button) {
		case AbstractButtons.BUTTON_LASER:
			// TODO move mirror back and switch laser to triggered
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
		} catch (MotorException e) {
			ExceptionHandler.handleException("Motor error during stack acquisition, trying to close and re-opening motors", e);

			// close and re-open motor and try again
			// in case it fails, restart the software
			microscope.resetBusy();
			try {
				microscope.initMotor(false);
				microscope.acquireStack();
			} catch(Exception ex) {
				ExceptionHandler.handleException("Error during stack acquisition after re-opening motor, restarting the software", ex);
				microscope.shutdown(Microscope.EXIT_STACK_ERROR);
			}

		} catch (CameraException e) {
			ExceptionHandler.handleException("Camera error during stack acquisition, trying to close and re-opening the camera", e);

			// close and re-open the camera and try again
			// in case it fails, restart the software
			microscope.resetBusy();
			try {
				microscope.initCamera();
				microscope.acquireStack();
			} catch(Exception ex) {
				ExceptionHandler.handleException("Error during stack acquisition after re-opening the camera, restarting the software", ex);
				microscope.shutdown(Microscope.EXIT_STACK_ERROR);
			}
		}
	}

	private void startPreview(int button, int axis, boolean positive, double target) {
		double y = Preferences.getStackYStart();
		double z = Preferences.getStackZStart();
		IMotor motor = microscope.getMotor();
		try {
			y = motor.getPosition(Y_AXIS);
			z = motor.getPosition(Z_AXIS);
			microscope.startPreview(button, axis, positive,  target);
		} catch (MotorException e) {
			ExceptionHandler.handleException("Motor error during preview, trying to close and re-opening motors", e);

			// close and re-open motor and try again
			// in case it fails, restart the software
			microscope.resetBusy();
			try {
				microscope.initMotor(false);
				microscope.getMotor().setTarget(Y_AXIS, y);
				microscope.getMotor().setTarget(Z_AXIS, z);
				while(motor.isMoving())
					Microscope.sleep(50);
				microscope.startPreview(button, axis, positive, target);
			} catch(Exception ex) {
				ExceptionHandler.handleException("Error during preview after re-opening motor, restarting the software", e);
				microscope.shutdown(Microscope.EXIT_PREVIEW_ERROR);
			}
		} catch (CameraException e) {
			ExceptionHandler.handleException("Camera error during preview, trying to close and re-opening the camera", e);

			// close and re-open the camera and try again
			// in case it fails, restart the software
			microscope.resetBusy();
			try {
				microscope.initCamera();
				microscope.startPreview(button, axis, positive, target);
			} catch(Exception ex) {
				ExceptionHandler.handleException("Error during preview after re-opening the camera, restarting the software", e);
				microscope.shutdown(Microscope.EXIT_PREVIEW_ERROR);
			}
		}
	}
}
