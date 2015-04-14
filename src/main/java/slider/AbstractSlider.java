package slider;

import java.util.ArrayList;

public abstract class AbstractSlider {

	/**
	 * In the range [0; 100]
	 * @param pos
	 */
	public abstract void setPosition(double pos);

	public abstract void close();

	private ArrayList<SliderListener> listeners = new ArrayList<SliderListener>();

	public void addSliderListener(SliderListener l) {
		listeners.add(l);
	}

	public void removeSliderListener(SliderListener l) {
		listeners.remove(l);
	}

	protected void fireSliderPositionChanged(double pos) {
		for(SliderListener l : listeners)
			l.sliderPositionChanged(pos);
	}

	protected void fireSliderReleased(double startPos) {
		for(SliderListener l : listeners)
			l.sliderReleased(startPos);
	}
}
