package main;


public interface AdminPanelListener {

	public void mirrorPositionChanged(double z, double pos);

	public void motorPositionChanged(double y, double z);

	public void cameraParametersChanged();

	public void adminPanelDone(boolean cancelled);
}
