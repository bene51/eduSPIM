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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import laser.LaserException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import stage.IMotor;
import cam.CameraException;

@SuppressWarnings("serial")
public class AdminPanel extends JPanel {

	private final Logger logger = LoggerFactory.getLogger(AdminPanel.class);

	private static DecimalFormat df = new DecimalFormat("0.000");

	private HashMap<String, String> oldPreferences;

	private MTextField volumeStartY, volumeStartZ, volumeEndY, volumeEndZ;
	private MButton setStart, setEnd, ok, cancel;
	private MTextField mirror1Z, mirror2Z;
	private NumberField mirror1M, mirror2M;
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


		volumeStartY = new MTextField(df.format(Preferences.getStackYStart()));
		volumeStartZ = new MTextField(df.format(Preferences.getStackZStart()));
		setStart = new MButton("Set");
		setStart.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				volumeStartY.setText(df.format(yPos));
				volumeStartZ.setText(df.format(zPos));
			}
		});
		volumeEndY = new MTextField(df.format(Preferences.getStackYEnd()));
		volumeEndZ = new MTextField(df.format(Preferences.getStackZEnd()));
		setEnd = new MButton("Set");
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

	public void init() {
		volumeStartY.setText(df.format(Preferences.getStackYStart()));
		volumeStartZ.setText(df.format(Preferences.getStackZStart()));
		volumeEndY.setText(df.format(Preferences.getStackYEnd()));
		volumeEndZ.setText(df.format(Preferences.getStackZEnd()));
		mirror1Z.setText(df.format(Preferences.getMirrorZ1()));
		mirror2Z.setText(df.format(Preferences.getMirrorZ2()));
		mirror1M.setText(df.format(Preferences.getMirrorM1()));
		mirror2M.setText(df.format(Preferences.getMirrorM2()));
		fCameraExp.setText(df.format(Preferences.getFCameraExposure()));
		fCameraFPS.setText(df.format(Preferences.getFCameraFramerate()));
		fCameraGain.setText(Integer.toString(Preferences.getFCameraGain()));
		tCameraExp.setText(df.format(Preferences.getTCameraExposure()));
		tCameraFPS.setText(df.format(Preferences.getTCameraFramerate()));
		tCameraGain.setText(Integer.toString(Preferences.getTCameraGain()));
		laserPower.setText(df.format(Preferences.getLaserPower()));

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
		logger.info("Successfully changed EduSPIM settings.");
		fireDone(false);
	}

	private void updateTransmissionGain() {
		try {
			int gain = (int)Math.round(Double.parseDouble(tCameraGain.getText()));
			Microscope.getInstance().getTransmissionCamera().setGain(gain);
			Microscope.getInstance().singlePreview(true, true);
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
			Microscope.getInstance().singlePreview(true, true);
		} catch (CameraException e1) {
			ExceptionHandler.showException("Error changing the exposure time of the transmission camera", e1);
		}
	}

	private void updateTransmissionFramerate() {
		try {
			double fps = Double.parseDouble(tCameraFPS.getText());
			fps = Microscope.getInstance().getTransmissionCamera().setFramerate(fps);
			tCameraFPS.setText(df.format(fps));
			Microscope.getInstance().singlePreview(true, true);
		} catch (CameraException e1) {
			ExceptionHandler.showException("Error changing the frame rate of the transmission camera", e1);
		}
	}

	private void updateFluorescenceGain() {
		try {
			int gain = (int)Math.round(Double.parseDouble(fCameraGain.getText()));
			Microscope.getInstance().getFluorescenceCamera().setGain(gain);
			Microscope.getInstance().singlePreview(true, true);
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
			Microscope.getInstance().singlePreview(true, true);
		} catch (CameraException e1) {
			ExceptionHandler.showException("Error changing the exposure time of the fluorescence camera", e1);
		}
	}

	private void updateFluorescenceFramerate() {
		try {
			double fps = Double.parseDouble(fCameraFPS.getText());
			fps = Microscope.getInstance().getFluorescenceCamera().setFramerate(fps);
			fCameraFPS.setText(df.format(fps));
			Microscope.getInstance().singlePreview(true, true);
		} catch (CameraException e1) {
			ExceptionHandler.showException("Error changing the frame rate of the fluorescence camera", e1);
		}
	}

	private void updateLaserpower() {
		try {
			double power = Double.parseDouble(laserPower.getText());
			Microscope.getInstance().getLaser().setPower(power);
			Microscope.getInstance().singlePreview(true, true);
		} catch (LaserException e1) {
			ExceptionHandler.showException("Error changing the laser power", e1);
		} catch(CameraException e2) {
			ExceptionHandler.showException("Error showing preview image after changing laser power", e2);
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

	private void fireMirrorPositionChanged(double pos) {
		for(AdminPanelListener l : listeners)
			l.mirrorPositionChanged(pos);
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
