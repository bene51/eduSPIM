package buttons;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.JFrame;

public class NiceButton extends JComponent implements MouseListener {
	private static final long serialVersionUID = 1L;

	private Dimension size = new Dimension(60, 60);

	private ArrayList<ActionListener> listeners = new ArrayList<ActionListener>();

	private boolean mousePressed = false;

	private int type = 0;

	public NiceButton(int type) {
		super();
		this.type = type;

		enableInputMethods(true);
		addMouseListener(this);
		setFocusable(true);
	}

	@Override
	public void paintComponent(Graphics gr) {
		super.paintComponent(gr);

		// turn on anti-alias mode
		Graphics2D g = (Graphics2D) gr;
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);

		int h = getHeight();
		int w = getWidth();

		g.setColor(Color.black);
		g.fillRect(0, 0, w, h);

		AffineTransform at = g.getTransform();
		g.translate(w/2.0, h/2.0);

		if(w > h)
			w = h;
		else
			h = w;

		g.setColor(Color.LIGHT_GRAY);
		g.fillOval(-w/2, -h/2, w, h);

		g.scale(0.95, 0.95);
		GradientPaint grad = new GradientPaint(
				0, -h/2, Color.BLACK,  // new Color(50, 50, 50),
				0, +h/2, Color.WHITE); // new Color(200, 200, 200));
		g.setPaint(grad);
		g.fillOval(-w/2, -h/2, w, h);

		g.scale(0.65, 0.65);

		grad = new GradientPaint(
				0, -h/2, Color.WHITE,  // new Color(150, 150, 150),
				0, +h/2, Color.BLACK); // new Color(100, 100, 100));
		g.setPaint(grad);
		g.fillOval(-w/2, -h/2, w, h);
		g.setColor(mousePressed ? new Color(0, 150, 255) : Color.GRAY);
		g.setStroke(new BasicStroke(w/10.0f));
		g.drawOval(-w/2, -h/2, w, h);
		g.setColor(Color.BLACK);

		switch(type) {
		case AbstractButtons.BUTTON_STACK:
			double ra = w/4.0;
			int[] x = new int[] {
				(int)(ra * Math.cos(2 * Math.PI / 3)),
				(int)(ra * Math.cos(2 * Math.PI / 3)),
				(int)Math.round(ra)
			};
			int[] y = new int[] {
				(int)(ra * Math.sin(2 * Math.PI / 3)),
				(int)(ra * Math.sin(4 * Math.PI / 3)),
				0
			};
			Polygon p = new Polygon(x, y, 3);
			g.fillPolygon(p);
			g.setStroke(new BasicStroke(w/12, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.draw(p);
			break;

		case AbstractButtons.BUTTON_LASER:
			int x1 = Math.round(-w / 12);
			int y1 = 0;
			int r = w * 7 / 60;
			g.fillOval(x1-r, y1-r, 2 * r, 2 * r);
			g.setStroke(new BasicStroke(w/30, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

			r = w/6;
			for(int i = 0; i < 12; i++) {
				double a = i * 30 * Math.PI / 180;
				double d = r;
				if(i == 4)
					d *= 2;
				if(i % 2 == 0)
					d = 4 * d /  3;
				float stroke = i % 2 == 0 ? w/30f : w/40f;
				int xs = (int)Math.round(x1 + d * Math.sin(a));
				int ys = (int)Math.round(y1 + d * Math.cos(a));
				g.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				g.drawLine(x1, y1, xs, ys);
			}
			break;
		case AbstractButtons.BUTTON_INFO:
			g.translate(0, -h/16);
			g.fillRoundRect(-w/12, -h/12, w/6, h/2, w/15, w/15);
			// 25,44,16,7
			g.fillRoundRect(-w/12 - w/15, h / 4, 2 * (w/12 + w/15), h-h/12-3*h/4, w/12, w/12);
			g.fillOval(-w/12, -h/12 - 4 * w/18, w/6, w/6);
			float stroke = w / 18f;
			g.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.drawLine(-w/15, -h/12 + Math.round(stroke/2), -w/10, -h/18 + h/6);
			break;
		case AbstractButtons.BUTTON_Y_DOWN:
		case AbstractButtons.BUTTON_Y_UP:
		case AbstractButtons.BUTTON_Z_DOWN:
		case AbstractButtons.BUTTON_Z_UP:
			double angle = 0;
			switch(type) {
			case AbstractButtons.BUTTON_Y_DOWN: angle = Math.PI; break;
			case AbstractButtons.BUTTON_Z_UP:   angle = Math.PI/4; break;
			case AbstractButtons.BUTTON_Z_DOWN: angle = 5 * Math.PI / 4; break;
			}
			g.rotate(angle);
			g.translate(0, -h/16);

			int s = w/6;
			x = new int[] {-s, 0, s};
			y = new int[] {0, -3 * s / 2, 0};
			g.fillPolygon(x, y, 3);
			g.setStroke(new BasicStroke(w/12, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.drawPolygon(x, y, 3);
			g.fillRect(-w/12, -1, w/6, h/4);
			g.fillRoundRect(-w/6, h/4-2, w/3, h/8, h/10, h/10);
		}
		g.setTransform(at);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
//		setCursor(new Cursor(Cursor.HAND_CURSOR));
//		repaint();
	}

	@Override
	public void mouseExited(MouseEvent e) {
//		setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
//		repaint();
	}

	@Override
	public void mousePressed(MouseEvent e) {
		mousePressed = true;
		notifyListeners(e);
		repaint();
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		mousePressed = false;
		setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		repaint();
	}

	public void addActionListener(ActionListener listener) {
		listeners.add(listener);
	}

	private void notifyListeners(MouseEvent e) {
		ActionEvent evt = new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
				new String(), e.getWhen(), e.getModifiers());
		synchronized (listeners) {
			for (int i = 0; i < listeners.size(); i++) {
				ActionListener tmp = listeners.get(i);
				tmp.actionPerformed(evt);
			}
		}
	}

	@Override
	public Dimension getPreferredSize() {
		return size;
	}

//	@Override
//	public Dimension getMinimumSize() {
//		return getPreferredSize();
//	}
//
//	@Override
//	public Dimension getMaximumSize() {
//		return getPreferredSize();
//	}

	public static void main(String[] args) {
		for(int i = 0; i < 7; i++) {
			JFrame frame = new JFrame();
			NiceButton button = new NiceButton(i);
			frame.getContentPane().add(button);
			frame.pack();
			frame.setVisible(true);
		}
	}
}
