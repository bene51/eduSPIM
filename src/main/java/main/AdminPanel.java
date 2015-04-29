package main;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import stage.IMotor;

@SuppressWarnings("serial")
public class AdminPanel extends JPanel {

	private final Logger logger = LoggerFactory.getLogger(AdminPanel.class);

	private static DecimalFormat df = new DecimalFormat("0.000");

	private HashMap<String, String> oldPreferences;

	private MTextField volumeStartY, volumeStartZ, volumeEndY, volumeEndZ;
	private JButton setStart, setEnd, ok, cancel;
	private MTextField mirror1Z, mirror2Z;
	private NumberField mirror1M, mirror2M;
	private ArrayList<AdminPanelListener> listeners = new ArrayList<AdminPanelListener>();

	private double yPos, zPos;

	public AdminPanel(double y, double z) {
		super();
		this.yPos = y;
		this.zPos = z;


		volumeStartY = new MTextField(df.format(Preferences.getStackYStart()));
		volumeStartZ = new MTextField(df.format(Preferences.getStackZStart()));
		setStart = new JButton("Set");
		setStart.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				volumeStartY.setText(df.format(yPos));
				volumeStartZ.setText(df.format(zPos));
			}
		});
		volumeEndY = new MTextField(df.format(Preferences.getStackYEnd()));
		volumeEndZ = new MTextField(df.format(Preferences.getStackZEnd()));
		setEnd = new JButton("Set");
		setEnd.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				volumeEndY.setText(df.format(yPos));
				volumeEndZ.setText(df.format(zPos));
			}
		});
		mirror1Z = new MTextField(df.format(Preferences.getMirrorZ1()));
		mirror1M = new NumberField(6);
		mirror1M.setForeground(Color.black);
		mirror1M.setText(df.format(Preferences.getMirrorM1()));
		mirror1M.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				int code = e.getKeyCode();
				if(code == KeyEvent.VK_ENTER ||
						code == KeyEvent.VK_UP ||
						code == KeyEvent.VK_DOWN) {
					mirror1Z.setText(df.format(zPos));
					fireMirrorPositionChanged(Double.parseDouble(mirror1M.getText()));
				}
			}
		});
		mirror2Z = new MTextField(df.format(Preferences.getMirrorZ2()));
		mirror2M = new NumberField(6);
		mirror2M.setText(df.format(Preferences.getMirrorM2()));
		mirror2M.setForeground(Color.black);
		mirror2M.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				int code = e.getKeyCode();
				if(code == KeyEvent.VK_ENTER ||
						code == KeyEvent.VK_UP ||
						code == KeyEvent.VK_DOWN) {
					mirror2Z.setText(df.format(zPos));
					fireMirrorPositionChanged(Double.parseDouble(mirror2M.getText()));
				}
			}
		});
		cancel = new JButton("Cancel");
		cancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				cancel();
			}
		});
		ok = new JButton("Apply & leave admin mode");
		ok.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				apply();
			}
		});






		Border lineBorder = BorderFactory.createLineBorder(Color.LIGHT_GRAY);

		setBackground(Color.BLACK);
		setForeground(Color.WHITE);

		setLayout(new GridBagLayout());
		GridBagConstraints cAll = new GridBagConstraints();
		cAll.insets = new Insets(5, 5, 5, 5);



		JPanel startVolPanel = new JPanel(new GridBagLayout());
		startVolPanel.setBackground(Color.BLACK);
		startVolPanel.setForeground(Color.WHITE);
		TitledBorder titledBorder = BorderFactory.createTitledBorder(lineBorder, "Volume start");
		titledBorder.setTitleColor(Color.LIGHT_GRAY);
		startVolPanel.setBorder(titledBorder);
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(3, 3, 3, 3);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;

		c.gridx = c.gridy = 0;
		startVolPanel.add(volumeStartY, c);

		c.gridy++;
		startVolPanel.add(volumeStartZ, c);

		c.gridy++;
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		startVolPanel.add(setStart, c);
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;

		cAll.gridx = cAll.gridy = 0;
		cAll.fill = GridBagConstraints.BOTH;
		cAll.weightx = 1;
		add(startVolPanel, cAll);




		JPanel endVolPanel = new JPanel(new GridBagLayout());
		endVolPanel.setBackground(Color.BLACK);
		endVolPanel.setForeground(Color.WHITE);
		titledBorder = BorderFactory.createTitledBorder(lineBorder, "Volume end");
		titledBorder.setTitleColor(Color.LIGHT_GRAY);
		endVolPanel.setBorder(titledBorder);

		c.gridx = c.gridy = 0;
		endVolPanel.add(volumeEndY, c);

		c.gridy++;
		endVolPanel.add(volumeEndZ, c);

		c.gridy++;
		c.fill = GridBagConstraints.NONE;
		c.weightx = 0;
		endVolPanel.add(setEnd, c);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;

		cAll.gridx = 1;
		cAll.fill = GridBagConstraints.BOTH;
		cAll.weightx = 1;
		add(endVolPanel, cAll);




		JPanel mirror1Panel = new JPanel(new GridBagLayout());
		mirror1Panel.setBackground(Color.BLACK);
		mirror1Panel.setForeground(Color.WHITE);
		titledBorder = BorderFactory.createTitledBorder(lineBorder, "Mirror pos. 1");
		titledBorder.setTitleColor(Color.LIGHT_GRAY);
		mirror1Panel.setBorder(titledBorder);

		c.gridx = c.gridy = 0;
		mirror1Panel.add(mirror1Z, c);

		c.gridy++;
		mirror1Panel.add(mirror1M, c);

		cAll.gridy++;
		cAll.gridx = 0;
		cAll.fill = GridBagConstraints.BOTH;
		cAll.weightx = 1;
		add(mirror1Panel, cAll);




		JPanel mirror2Panel = new JPanel(new GridBagLayout());
		mirror2Panel.setBackground(Color.BLACK);
		mirror2Panel.setForeground(Color.WHITE);
		titledBorder = BorderFactory.createTitledBorder(lineBorder, "Mirror pos. 2");
		titledBorder.setTitleColor(Color.LIGHT_GRAY);
		mirror2Panel.setBorder(titledBorder);

		c.gridx = c.gridy = 0;
		mirror2Panel.add(mirror2Z, c);

		c.gridy++;
		mirror2Panel.add(mirror2M, c);

		cAll.gridx++;
		cAll.fill = GridBagConstraints.BOTH;
		cAll.weightx = 1;
		add(mirror2Panel, cAll);

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
		buttons.setOpaque(true);
		buttons.setBackground(Color.BLACK);
		buttons.setForeground(Color.WHITE);
		buttons.add(cancel);
		buttons.add(ok);
		cAll.gridy++;
		cAll.gridx = 0;
		cAll.fill = GridBagConstraints.NONE;
		cAll.insets = new Insets(20, 3, 20, 3);
		cAll.gridwidth = GridBagConstraints.REMAINDER;
		cAll.anchor = GridBagConstraints.CENTER;
		add(buttons, cAll);
	}

	public void init() {
		volumeStartY.setText(df.format(Preferences.getStackYStart()));
		volumeStartZ.setText(df.format(Preferences.getStackZStart()));
		volumeEndY.setText(df.format(Preferences.getStackYEnd()));
		volumeEndZ.setText(df.format(Preferences.getStackZEnd()));
		mirror1Z.setText(df.format(Preferences.getMirrorZ1()));
		mirror2Z.setText(df.format(Preferences.getMirrorZ2()));
		mirror1M.setText(df.format(Preferences.getMirrorM1()));
		mirror2M.setText(df.format(Preferences.getMirrorM2()));

		oldPreferences = Preferences.backup();

		Preferences.setStackYStart(IMotor.POS_MIN_Y);
		Preferences.setStackZStart(IMotor.POS_MIN_Z);
		Preferences.setStackYEnd(IMotor.POS_MAX_Y);
		Preferences.setStackZEnd(IMotor.POS_MAX_Z);
	}

	public void cancel() {
		Preferences.restore(oldPreferences);
		fireDone();
	}

	public void apply() {
		double x1 = Double.parseDouble(mirror1Z.getText());
		double y1 = Double.parseDouble(mirror1M.getText());
		double x2 = Double.parseDouble(mirror2Z.getText());
		double y2 = Double.parseDouble(mirror2M.getText());

		double m = (y2 - y1) / (x2 - x1);
		double t = y1 - m * x1;

		oldPreferences.put(Preferences.STACK_Y_START,  volumeStartY.getText());
		oldPreferences.put(Preferences.STACK_Z_START,  volumeStartZ.getText());
		oldPreferences.put(Preferences.STACK_Y_END,    volumeEndY.getText());
		oldPreferences.put(Preferences.STACK_Z_END,    volumeEndY.getText());
		oldPreferences.put(Preferences.MIRROR_Z1,      Double.toString(x1));
		oldPreferences.put(Preferences.MIRROR_M1,      Double.toString(y1));
		oldPreferences.put(Preferences.MIRROR_Z2,      Double.toString(x2));
		oldPreferences.put(Preferences.MIRROR_M2,      Double.toString(y2));
		oldPreferences.put(Preferences.MIRROR_COEFF_M, Double.toString(m));
		oldPreferences.put(Preferences.MIRROR_COEFF_T, Double.toString(t));

		Preferences.restore(oldPreferences);
		logger.info("Successfully changed EduSPIM settings.");
		fireDone();
	}

	public void setPosition(double yPos, double zPos) {
		this.yPos = yPos;
		this.zPos = zPos;
	}

	public void addAdminPanelListener(AdminPanelListener l) {
		listeners.add(l);
	}

	public void removeAdminPanelListener(AdminPanelListener l) {
		listeners.remove(l);
	}

	private void fireMirrorPositionChanged(double pos) {
		for(AdminPanelListener l : listeners)
			l.mirrorPositionChanged(pos);
	}

	private void fireDone() {
		for(AdminPanelListener l : listeners)
			l.adminPanelDone();
	}

	private static class MTextField extends JTextField {
		public MTextField(String n) {
			super(6);
			setText(n);
			setBackground(Color.WHITE);
			setForeground(Color.BLACK);
			setEditable(false);
		}
	}

	public static void main(String[] args) {
		JFrame f = new JFrame();
		f.getContentPane().add(new AdminPanel(0, 0));
		f.pack();
		f.setVisible(true);
		System.out.println(f.getFont());
	}
}
