package main;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

@SuppressWarnings("serial")
public class AdminPanel extends JPanel {

	private static DecimalFormat df = new DecimalFormat("0.000");

	private MTextField volumeStartY, volumeStartZ, volumeEndY, volumeEndZ;
	private JButton setStart, setEnd, done;
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
			public void actionPerformed(ActionEvent e) {
				volumeStartY.setText(df.format(yPos));
				volumeStartZ.setText(df.format(zPos));
			}
		});
		volumeEndY = new MTextField(df.format(Preferences.getStackYEnd()));
		volumeEndZ = new MTextField(df.format(Preferences.getStackZEnd()));
		setEnd = new JButton("Set");
		setEnd.addActionListener(new ActionListener() {
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
		done = new JButton("Apply & leave admin mode");
		done.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				fireDone();
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


		cAll.gridy++;
		cAll.gridx = 0;
		cAll.fill = GridBagConstraints.NONE;
		cAll.insets = new Insets(20, 3, 20, 3);
		cAll.gridwidth = GridBagConstraints.REMAINDER;
		cAll.anchor = GridBagConstraints.CENTER;
		add(done, cAll);
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
			l.done();
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
