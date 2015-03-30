package slider;

import java.awt.Scrollbar;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class AWTSlider extends AbstractSlider implements AdjustmentListener {

	private Scrollbar scroll;
	private long timeReleased = 0;
	private final int MAX = 1000;

	public AWTSlider() {
		scroll = new Scrollbar(Scrollbar.HORIZONTAL, 0, 10, 0, MAX);
		scroll.addAdjustmentListener(this);
		scroll.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				double v = scroll.getValue() / (double)MAX;
				timeReleased = System.currentTimeMillis();
				fireSliderReleased(v);
			}
		});
	}

	public Scrollbar getScrollbar() {
		return scroll;
	}

	@Override
	public void setPosition(double pos) {
		int v = (int)Math.round(pos * MAX);
		scroll.setValue(v);
	}

	public void adjustmentValueChanged(AdjustmentEvent arg0) {
		long t = System.currentTimeMillis();
		if(t - timeReleased > 100) {
			double v = scroll.getValue() / (double)MAX;
			fireSliderPositionChanged(v);
		}
	}
}
