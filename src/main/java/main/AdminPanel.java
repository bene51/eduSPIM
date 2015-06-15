package main;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import laser.LaserException;
import stage.IMotor;
import cam.CameraException;

@SuppressWarnings("serial")
public class AdminPanel extends JPanel {

	// TODO focus listeners

	private static DecimalFormat df = new DecimalFormat("0.000");

	private HashMap<String, String> oldPreferences;

	private MButton setStart, setEnd, ok, cancel;
	private NumberField mirror1Z, mirror2Z, mirror1M, mirror2M;
	private NumberField volumeStartY, volumeStartZ, volumeEndY, volumeEndZ;
	private NumberField fCameraGain, tCameraGain;
	private NumberField fCameraExp, fCameraFPS;
	private NumberField tCameraExp, tCameraFPS;
	private NumberField laserPower;
	private ArrayList<AdminPanelListener> listeners = new ArrayList<AdminPanelListener>();

	private double yPos, zPos;

	public AdminPanel(double y, double z) {
		super();
		this.yPos = y;
		this.zPos = z;

		volumeStartY = new NumberField(6);
		volumeStartY.setLimits(IMotor.POS_MIN_Y, IMotor.POS_MAX_Y);
		volumeStartY.setForeground(Color.black);
		volumeStartY.setText(df.format(Preferences.getStackYStart()));
		volumeStartY.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				int code = e.getKeyCode();
				if(code == KeyEvent.VK_ENTER ||
						code == KeyEvent.VK_UP ||
						code == KeyEvent.VK_DOWN) {
					updateFPS();
					fireMotorPositionChanged(
							Double.parseDouble(volumeStartY.getText()),
							Double.parseDouble(volumeStartZ.getText()));
				}
			}
		});

		volumeStartZ = new NumberField(6);
		volumeStartZ.setLimits(IMotor.POS_MIN_Z, IMotor.POS_MAX_Z);
		volumeStartZ.setForeground(Color.black);
		volumeStartZ.setText(df.format(Preferences.getStackZStart()));
		volumeStartZ.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				int code = e.getKeyCode();
				if(code == KeyEvent.VK_ENTER ||
						code == KeyEvent.VK_UP ||
						code == KeyEvent.VK_DOWN) {
					updateFPS();
					fireMotorPositionChanged(
							Double.parseDouble(volumeStartY.getText()),
							Double.parseDouble(volumeStartZ.getText()));
				}
			}
		});

		setStart = new MButton("Set");
		setStart.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				volumeStartY.setText(df.format(yPos));
				volumeStartZ.setText(df.format(zPos));
			}
		});

		volumeEndY = new NumberField(6);
		volumeEndY.setLimits(IMotor.POS_MIN_Y, IMotor.POS_MAX_Y);
		volumeEndY.setForeground(Color.black);
		volumeEndY.setText(df.format(Preferences.getStackYEnd()));
		volumeEndY.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				int code = e.getKeyCode();
				if(code == KeyEvent.VK_ENTER ||
						code == KeyEvent.VK_UP ||
						code == KeyEvent.VK_DOWN) {
					updateFPS();
					fireMotorPositionChanged(
							Double.parseDouble(volumeEndY.getText()),
							Double.parseDouble(volumeEndZ.getText()));
				}
			}
		});

		volumeEndZ = new NumberField(6);
		volumeEndZ.setLimits(IMotor.POS_MIN_Z, IMotor.POS_MAX_Z);
		volumeEndZ.setForeground(Color.black);
		volumeEndZ.setText(df.format(Preferences.getStackZEnd()));
		volumeEndZ.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				int code = e.getKeyCode();
				if(code == KeyEvent.VK_ENTER ||
						code == KeyEvent.VK_UP ||
						code == KeyEvent.VK_DOWN) {
					updateFPS();
					fireMotorPositionChanged(
							Double.parseDouble(volumeEndY.getText()),
							Double.parseDouble(volumeEndZ.getText()));
				}
			}
		});

		setEnd = new MButton("Set");
		setEnd.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				volumeEndY.setText(df.format(yPos));
				volumeEndZ.setText(df.format(zPos));
			}
		});
		mirror1Z = new NumberField(6);
		mirror1Z.setLimits(IMotor.POS_MIN_Z, IMotor.POS_MAX_Z);
		mirror1Z.setForeground(Color.black);
		mirror1Z.setText(df.format(Preferences.getMirrorZ1()));
		mirror1Z.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				int code = e.getKeyCode();
				if(code == KeyEvent.VK_ENTER ||
						code == KeyEvent.VK_UP ||
						code == KeyEvent.VK_DOWN) {
					updateFPS();
					fireMirrorPositionChanged(
							Double.parseDouble(mirror1Z.getText()),
							Double.parseDouble(mirror1M.getText()));
				}
			}
		});

		mirror1M = new NumberField(6);
		mirror1M.setLimits(IMotor.POS_MIN_M, IMotor.POS_MAX_M);
		mirror1M.setForeground(Color.black);
		mirror1M.setText(df.format(Preferences.getMirrorM1()));
		mirror1M.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				int code = e.getKeyCode();
				if(code == KeyEvent.VK_ENTER ||
						code == KeyEvent.VK_UP ||
						code == KeyEvent.VK_DOWN) {
					updateFPS();
					fireMirrorPositionChanged(
							Double.parseDouble(mirror1Z.getText()),
							Double.parseDouble(mirror1M.getText()));
				}
			}
		});
		mirror2Z = new NumberField(6);
		mirror2Z.setLimits(IMotor.POS_MIN_Z, IMotor.POS_MAX_Z);
		mirror2Z.setForeground(Color.black);
		mirror2Z.setText(df.format(Preferences.getMirrorZ2()));
		mirror2Z.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				int code = e.getKeyCode();
				if(code == KeyEvent.VK_ENTER ||
						code == KeyEvent.VK_UP ||
						code == KeyEvent.VK_DOWN) {
					updateFPS();
					fireMirrorPositionChanged(
							Double.parseDouble(mirror2Z.getText()),
							Double.parseDouble(mirror2M.getText()));
				}
			}
		});
		mirror2M = new NumberField(6);
		mirror2M.setLimits(IMotor.POS_MIN_M, IMotor.POS_MAX_M);
		mirror2M.setText(df.format(Preferences.getMirrorM2()));
		mirror2M.setForeground(Color.black);
		mirror2M.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				int code = e.getKeyCode();
				if(code == KeyEvent.VK_ENTER ||
						code == KeyEvent.VK_UP ||
						code == KeyEvent.VK_DOWN) {
					updateFPS();
					fireMirrorPositionChanged(
							Double.parseDouble(mirror2Z.getText()),
							Double.parseDouble(mirror2M.getText()));
				}
			}
		});

//		fCameraExp  = new MTextField(df.format(Preferences.getFCameraExposure()));
//		fCameraExp.setEditable(true);
		fCameraExp = new NumberField(6);
		fCameraExp.setText(df.format(Preferences.getFCameraExposure()));
		fCameraExp.setForeground(Color.black);
		fCameraExp.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				int code = e.getKeyCode();
				if(code == KeyEvent.VK_ENTER||
						code == KeyEvent.VK_UP ||
						code == KeyEvent.VK_DOWN) {
					updateFluorescenceExposure();
				}
			}
		});
		fCameraExp.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				updateFluorescenceExposure();
			}
		});

//		fCameraFPS  = new MTextField(df.format(Preferences.getFCameraFramerate()));
//		fCameraFPS.setEditable(true);
		fCameraFPS = new NumberField(6);
		fCameraFPS.setText(df.format(Preferences.getFCameraFramerate()));
		fCameraFPS.setForeground(Color.black);
		fCameraFPS.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				int code = e.getKeyCode();
				if(code == KeyEvent.VK_ENTER||
						code == KeyEvent.VK_UP ||
						code == KeyEvent.VK_DOWN) {
					updateFluorescenceFramerate();
				}
			}
		});
		fCameraFPS.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				updateFluorescenceFramerate();
			}
		});

		fCameraGain = new NumberField(6);
		fCameraGain.setLimits(1, 100);
		fCameraGain.setIntegersOnly(true);
		fCameraGain.setText(df.format(Preferences.getFCameraGain()));
		fCameraGain.setForeground(Color.black);
		fCameraGain.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				int code = e.getKeyCode();
				if(code == KeyEvent.VK_ENTER||
						code == KeyEvent.VK_UP ||
						code == KeyEvent.VK_DOWN) {
					updateFluorescenceGain();
				}
			}
		});
		fCameraGain.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				updateFluorescenceGain();
			}
		});

//		tCameraExp  = new MTextField(df.format(Preferences.getTCameraExposure()));
//		tCameraExp.setEditable(true);
		tCameraExp = new NumberField(6);
		tCameraExp.setText(df.format(Preferences.getTCameraExposure()));
		tCameraExp.setForeground(Color.black);
		tCameraExp.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				int code = e.getKeyCode();
				if(code == KeyEvent.VK_ENTER||
						code == KeyEvent.VK_UP ||
						code == KeyEvent.VK_DOWN) {
					updateTransmissionExposure();
				}
			}
		});
		tCameraExp.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				updateTransmissionExposure();
			}
		});

//		tCameraFPS  = new MTextField(df.format(Preferences.getTCameraFramerate()));
//		tCameraFPS.setEditable(true);
		tCameraFPS = new NumberField(6);
		tCameraFPS.setText(df.format(Preferences.getTCameraFramerate()));
		tCameraFPS.setForeground(Color.black);
		tCameraFPS.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				int code = e.getKeyCode();
				if(code == KeyEvent.VK_ENTER||
						code == KeyEvent.VK_UP ||
						code == KeyEvent.VK_DOWN) {
					updateTransmissionFramerate();
				}
			}
		});
		tCameraFPS.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				updateTransmissionFramerate();
			}
		});

		tCameraGain = new NumberField(6);
		tCameraGain.setLimits(1, 100);
		tCameraGain.setIntegersOnly(true);
		tCameraGain.setText(df.format(Preferences.getTCameraGain()));
		tCameraGain.setForeground(Color.black);
		tCameraGain.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				int code = e.getKeyCode();
				if(code == KeyEvent.VK_ENTER||
						code == KeyEvent.VK_UP ||
						code == KeyEvent.VK_DOWN) {
					updateTransmissionGain();
				}
			}
		});
		tCameraGain.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				updateTransmissionGain();
			}
		});

//		laserPower = new MTextField(Double.toString(Preferences.getLaserPower()));
//		laserPower.setEditable(true);
		laserPower = new NumberField(6);
		laserPower.setLimits(1, Microscope.getInstance().getLaser().getMaxPower());
		laserPower.setIntegersOnly(true);
		laserPower.setText(Integer.toString((int)Math.round(Preferences.getLaserPower())));
		laserPower.setForeground(Color.black);
		laserPower.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				int code = e.getKeyCode();
				if(code == KeyEvent.VK_ENTER||
						code == KeyEvent.VK_UP ||
						code == KeyEvent.VK_DOWN) {
					updateLaserpower();
				}
			}
		});
		laserPower.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				updateLaserpower();
			}
		});


		cancel = new MButton("Cancel");
		cancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				cancel();
			}
		});
		ok = new MButton("Apply & leave admin mode");
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
		cAll.insets = new Insets(15, 5, 15, 5);



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




		// TODO mirror
//		JPanel mirror2Panel = new JPanel(new GridBagLayout());
//		mirror2Panel.setBackground(Color.BLACK);
//		mirror2Panel.setForeground(Color.WHITE);
//		titledBorder = BorderFactory.createTitledBorder(lineBorder, "Mirror pos. 2");
//		titledBorder.setTitleColor(Color.LIGHT_GRAY);
//		mirror2Panel.setBorder(titledBorder);
//
//		c.gridx = c.gridy = 0;
//		mirror2Panel.add(mirror2Z, c);
//
//		c.gridy++;
//		mirror2Panel.add(mirror2M, c);
//
//		cAll.gridx++;
//		cAll.fill = GridBagConstraints.BOTH;
//		cAll.weightx = 1;
//		add(mirror2Panel, cAll);



		JPanel fCameraPanel = new JPanel(new GridBagLayout());
		fCameraPanel.setBackground(Color.BLACK);
		fCameraPanel.setForeground(Color.WHITE);
		titledBorder = BorderFactory.createTitledBorder(lineBorder, "Fluor. camera");
		titledBorder.setTitleColor(Color.LIGHT_GRAY);
		fCameraPanel.setBorder(titledBorder);

		c.gridx = c.gridy = 0;
		fCameraPanel.add(new MLabel("Exp."), c);
		c.gridx++;
		fCameraPanel.add(fCameraExp, c);

		c.gridy++;
		c.gridx = 0;
		fCameraPanel.add(new MLabel("FPS"), c);
		c.gridx++;
		fCameraPanel.add(fCameraFPS, c);

//		c.gridy++;
//		c.gridx = 0;
//		fCameraPanel.add(new MLabel("Offs."), c);
//		c.gridx++;
//		fCameraPanel.add(fCameraOffs, c);

		c.gridy++;
		c.gridx = 0;
		fCameraPanel.add(new MLabel("Gain"), c);
		c.gridx++;
		fCameraPanel.add(fCameraGain, c);

		cAll.gridx = 0;
		cAll.gridy++;
		cAll.fill = GridBagConstraints.BOTH;
		cAll.weightx = 1;
		add(fCameraPanel, cAll);



		JPanel tCameraPanel = new JPanel(new GridBagLayout());
		tCameraPanel.setBackground(Color.BLACK);
		tCameraPanel.setForeground(Color.WHITE);
		titledBorder = BorderFactory.createTitledBorder(lineBorder, "Transm. camera");
		titledBorder.setTitleColor(Color.LIGHT_GRAY);
		tCameraPanel.setBorder(titledBorder);

		c.gridx = c.gridy = 0;
		tCameraPanel.add(new MLabel("Exp."), c);
		c.gridx++;
		tCameraPanel.add(tCameraExp, c);

		c.gridy++;
		c.gridx = 0;
		tCameraPanel.add(new MLabel("FPS"), c);
		c.gridx++;
		tCameraPanel.add(tCameraFPS, c);

//		c.gridy++;
//		c.gridx = 0;
//		tCameraPanel.add(new MLabel("Offs."), c);
//		c.gridx++;
//		tCameraPanel.add(tCameraOffs, c);

		c.gridy++;
		c.gridx = 0;
		tCameraPanel.add(new MLabel("Gain"), c);
		c.gridx++;
		tCameraPanel.add(tCameraGain, c);

		cAll.gridx++;
		cAll.fill = GridBagConstraints.BOTH;
		cAll.weightx = 1;
		add(tCameraPanel, cAll);




		JPanel laserPanel = new JPanel(new GridBagLayout());
		laserPanel.setBackground(Color.BLACK);
		laserPanel.setForeground(Color.WHITE);
		titledBorder = BorderFactory.createTitledBorder(lineBorder, "Illumination");
		titledBorder.setTitleColor(Color.LIGHT_GRAY);
		laserPanel.setBorder(titledBorder);

		c.gridx = c.gridy = 0;
		laserPanel.add(new MLabel("Laser power"), c);
		c.gridx++;
		laserPanel.add(laserPower, c);

		cAll.gridx = 0;
		cAll.gridy++;
		cAll.fill = GridBagConstraints.BOTH;
		cAll.weightx = 1;
		cAll.gridwidth = GridBagConstraints.REMAINDER;
		add(laserPanel, cAll);




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

	/**
	 * Since this stupid mirror motor doesn't allow to adjust its speed,
	 * the framerate needs to be adjusted for the fixed speed of the mirror motor.
	 *
	 * Let the mirror distance over the entire z range be dm, and the fixed mirror
	 * speed be vm, which is roughly 20um / 1500ms = 0.01333333. Then the framerate
	 * dt = dm / vm, and fps = ICamera.DEPTH / dt.
	 */
	private void updateFPS() {
		// final double vm = 0.02 / 1.5; // for 2000 Hz
		// final double vm = 0.02 / 3.0; // for 500 Hz
		// final double vm = 0.02 / 7.0; // for 200 Hz
//		final double vm = 0.02 / 8.000;  // for 150 Hz

		// calculate mirror coefficients
//		double x1 = Double.parseDouble(mirror1Z.getText());
//		double y1 = Double.parseDouble(mirror1M.getText());
//		double x2 = Double.parseDouble(mirror2Z.getText());
//		double y2 = Double.parseDouble(mirror2M.getText());
//
//		if(Math.abs(y1 - y2) < 1e-6)
//			return;

//		double m = (y2 - y1) / (x2 - x1);
//		double t = y1 - m * x1;
//
//		double z1 = Double.parseDouble(volumeStartZ.getText());
//		double z2 = Double.parseDouble(volumeEndZ.getText());
//
//		double m1 = m * z1 + t;
//		double m2 = m * z2 + t;
//
//		double dm = Math.abs(m1 - m2);
//		double dt = dm / vm;
//
//		double fps = ICamera.DEPTH / dt;
//
//
//
//
//		try {
//			double tmp = Microscope.getInstance().getTransmissionCamera().setFramerate(fps);
//			tCameraFPS.setText(df.format(tmp));
//			tmp = Microscope.getInstance().getTransmissionCamera().setExposuretime(
//					Double.parseDouble(tCameraExp.getText()));
//			tCameraExp.setText(df.format(tmp));
//		} catch(Exception e) {
//			ExceptionHandler.showException("Error updating transmission camera parameters", e);
//		}
//
//		try {
//			double tmp = Microscope.getInstance().getFluorescenceCamera().setFramerate(fps);
//			fCameraFPS.setText(df.format(tmp));
//			tmp = Microscope.getInstance().getFluorescenceCamera().setExposuretime(
//					Double.parseDouble(fCameraExp.getText()));
//			fCameraExp.setText(df.format(tmp));
//		} catch(Exception e) {
//			ExceptionHandler.showException("Error updating transmission camera parameters", e);
//		}
	}

	public void init() {
		volumeStartY.setText(df.format(Preferences.getStackYStart()));
		volumeStartZ.setText(df.format(Preferences.getStackZStart()));
		volumeEndY.setText(df.format(Preferences.getStackYEnd()));
		volumeEndZ.setText(df.format(Preferences.getStackZEnd()));
		mirror1Z.setText(df.format(Preferences.getMirrorZ1()));
		mirror2Z.setText(df.format(Preferences.getMirrorZ2()));
		mirror1M.setText(df.format(Preferences.getMirrorM1()));
		// TODO mirror
		mirror1M.setText(df.format(2));
		mirror2M.setText(df.format(Preferences.getMirrorM2()));
		fCameraExp.setText(df.format(Preferences.getFCameraExposure()));
		fCameraFPS.setText(df.format(Preferences.getFCameraFramerate()));
		fCameraGain.setText(Integer.toString(Preferences.getFCameraGain()));
		tCameraExp.setText(df.format(Preferences.getTCameraExposure()));
		tCameraFPS.setText(df.format(Preferences.getTCameraFramerate()));
		tCameraGain.setText(Integer.toString(Preferences.getTCameraGain()));
		laserPower.setText(Integer.toString((int)Math.round(Preferences.getLaserPower())));

		oldPreferences = Preferences.backup();

		Preferences.setStackYStart(IMotor.POS_MIN_Y);
		Preferences.setStackZStart(IMotor.POS_MIN_Z);
		Preferences.setStackYEnd(IMotor.POS_MAX_Y);
		Preferences.setStackZEnd(IMotor.POS_MAX_Z);
	}

	public void cancel() {
		Preferences.restore(oldPreferences);
		fireDone(true);
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
		oldPreferences.put(Preferences.STACK_Z_END,    volumeEndZ.getText());
		oldPreferences.put(Preferences.MIRROR_Z1,      Double.toString(x1));
		oldPreferences.put(Preferences.MIRROR_M1,      Double.toString(y1));
		oldPreferences.put(Preferences.MIRROR_Z2,      Double.toString(x2));
		oldPreferences.put(Preferences.MIRROR_M2,      Double.toString(y2));
		oldPreferences.put(Preferences.MIRROR_COEFF_M, Double.toString(m));
		oldPreferences.put(Preferences.MIRROR_COEFF_T, Double.toString(t));
		oldPreferences.put(Preferences.CAMERA_F_EXPOSURE,  fCameraExp.getText());
		oldPreferences.put(Preferences.CAMERA_F_FRAMERATE, fCameraFPS.getText());
		oldPreferences.put(Preferences.CAMERA_F_GAIN,      fCameraGain.getText());
		oldPreferences.put(Preferences.CAMERA_T_EXPOSURE,  tCameraExp.getText());
		oldPreferences.put(Preferences.CAMERA_T_FRAMERATE, tCameraFPS.getText());
		oldPreferences.put(Preferences.CAMERA_T_GAIN,      tCameraGain.getText());
		oldPreferences.put(Preferences.LASER_POWER,    laserPower.getText());

		Preferences.restore(oldPreferences);
		fireDone(false);
	}

	private void updateTransmissionGain() {
		try {
			int gain = (int)Math.round(Double.parseDouble(tCameraGain.getText()));
			Microscope.getInstance().getTransmissionCamera().setGain(gain);
			fireCameraParametersChanged();
		} catch (CameraException e1) {
			try {
				int gain = Microscope.getInstance().getTransmissionCamera().getGain();
				tCameraGain.setText(Integer.toString(gain));
			} catch(Throwable t) {
				ExceptionHandler.showException("Error changing the gain of the transmission camera", e1);
			}
		}
	}

	private void updateTransmissionExposure() {
		try {
			double exp = Double.parseDouble(tCameraExp.getText());
			exp = Microscope.getInstance().getTransmissionCamera().setExposuretime(exp);
			tCameraExp.setText(df.format(exp));
			fireCameraParametersChanged();
		} catch (CameraException e1) {
			ExceptionHandler.showException("Error changing the exposure time of the transmission camera", e1);
		}
	}

	private void updateTransmissionFramerate() {
		try {
			double fps = Double.parseDouble(tCameraFPS.getText());
			fps = Microscope.getInstance().getTransmissionCamera().setFramerate(fps);
			tCameraFPS.setText(df.format(fps));
			fireCameraParametersChanged();
		} catch (CameraException e1) {
			ExceptionHandler.showException("Error changing the frame rate of the transmission camera", e1);
		}
	}

	private void updateFluorescenceGain() {
		try {
			int gain = (int)Math.round(Double.parseDouble(fCameraGain.getText()));
			Microscope.getInstance().getFluorescenceCamera().setGain(gain);
			fireCameraParametersChanged();
		} catch (CameraException e1) {
			try {
				int gain = Microscope.getInstance().getFluorescenceCamera().getGain();
				fCameraGain.setText(Integer.toString(gain));
			} catch(Throwable t) {
				ExceptionHandler.showException("Error changing the gain of the fluorescence camera", e1);
			}
		}
	}

	private void updateFluorescenceExposure() {
		try {
			double exp = Double.parseDouble(fCameraExp.getText());
			exp = Microscope.getInstance().getFluorescenceCamera().setExposuretime(exp);
			fCameraExp.setText(df.format(exp));
			fireCameraParametersChanged();
		} catch (CameraException e1) {
			ExceptionHandler.showException("Error changing the exposure time of the fluorescence camera", e1);
		}
	}

	private void updateFluorescenceFramerate() {
		try {
			double fps = Double.parseDouble(fCameraFPS.getText());
			fps = Microscope.getInstance().getFluorescenceCamera().setFramerate(fps);
			fCameraFPS.setText(df.format(fps));
			fireCameraParametersChanged();
		} catch (CameraException e1) {
			ExceptionHandler.showException("Error changing the frame rate of the fluorescence camera", e1);
		}
	}

	private void updateLaserpower() {
		try {
			double power = Double.parseDouble(laserPower.getText());
			Microscope.getInstance().getLaser().setPower(power);
			fireCameraParametersChanged();
		} catch (LaserException e1) {
			ExceptionHandler.showException("Error changing the laser power", e1);
		}
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

	private void fireMirrorPositionChanged(double z, double m) {
		for(AdminPanelListener l : listeners)
			l.mirrorPositionChanged(z, m);
	}

	private void fireMotorPositionChanged(double y, double z) {
		for(AdminPanelListener l : listeners)
			l.motorPositionChanged(y, z);
	}

	private void fireCameraParametersChanged() {
		for(AdminPanelListener l : listeners)
			l.cameraParametersChanged();
	}

	private void fireDone(boolean cancelled) {
		for(AdminPanelListener l : listeners)
			l.adminPanelDone(cancelled);
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

	private static class MButton extends JButton {
		public MButton(String text) {
			super(text);
			setBackground(Color.black);
			setForeground(Color.black);
		}
	}

	private static class MLabel extends JLabel {
		public MLabel(String text) {
			super(text);
			setBackground(Color.BLACK);
			setForeground(Color.WHITE);
		}
	}

	public static void main(String[] args) {
		try {
			// Set System L&F
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch(Throwable e) {
			e.printStackTrace();
		}
		JFrame f = new JFrame();
		f.getContentPane().add(new AdminPanel(0, 0));
		f.pack();
		f.setVisible(true);
		System.out.println(f.getFont());
	}
}
