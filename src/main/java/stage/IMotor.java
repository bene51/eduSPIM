package stage;

public interface IMotor {

	public int getNDimensions();

	public void getPosition(double[] pos);

	public void getVelocity(double[] vel);

	public void getTarget(double[] target);

	public void getPosMin(double[] pos);

	public void getPosMax(double[] pos);

	public void getVelMin(double[] vel);

	public void getVelMax(double[] vel);

	public boolean isMoving();

	public void setVelocity(double[] vel);

	public void setTarget(double[] target);

	public void close();
}
