package download;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.URL;
import java.util.Observable;
import java.util.Observer;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

// The Download Manager.
@SuppressWarnings("serial")
public class DownloadManager extends JDialog implements Observer {

	// Download table's data model.
	private DownloadsTableModel tableModel;

	// Table listing downloads.
	private JTable table;

	// Constructor for Download Manager.
	public DownloadManager() {
		setModal(true);
		// Set application title.
		setTitle("Download Manager");

		// Set window size.
		setSize(640, 480);

		// Handle window closing events.
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				actionExit();
			}
		});

		// Set up Downloads table.
		tableModel = new DownloadsTableModel();
		table = new JTable(tableModel);

		// Allow only one row at a time to be selected.
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		// Set up ProgressBar as renderer for progress column.
		ProgressRenderer renderer = new ProgressRenderer(0, 100);
		renderer.setStringPainted(true); // show progress text
		table.setDefaultRenderer(JProgressBar.class, renderer);

		// Set table's row height large enough to fit JProgressBar.
		table.setRowHeight((int) renderer.getPreferredSize().getHeight());

		// Set up downloads panel.
		JPanel downloadsPanel = new JPanel();
		downloadsPanel.setBorder(BorderFactory.createTitledBorder("Downloads"));
		downloadsPanel.setLayout(new BorderLayout());
		downloadsPanel.add(new JScrollPane(table), BorderLayout.CENTER);

		// Add panels to display.
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(downloadsPanel, BorderLayout.CENTER);
	}

	// Exit this program.
	private void actionExit() {
		this.dispose();
	}

	public Download addURL(String url, String downloadPath, String convertPath) {
		URL verifiedUrl = verifyUrl(url);
		if (verifiedUrl != null) {
			Download dl = new Download(verifiedUrl, downloadPath, convertPath);
			dl.addObserver(DownloadManager.this);
			tableModel.addDownload(dl);
			return dl;
		} else {
			throw new RuntimeException("Invalid Download URL");
		}
	}

	// Verify download URL.
	private URL verifyUrl(String url) {
		// Only allow HTTP URLs.
		if (!(url.toLowerCase().startsWith("https://") || url.toLowerCase()
				.startsWith("http://")))
			return null;

		// Verify format of URL.
		URL verifiedUrl = null;
		try {
			verifiedUrl = new URL(url);
		} catch (Exception e) {
			return null;
		}

		// Make sure URL specifies a file.
		if (verifiedUrl.getFile().length() < 2)
			return null;

		return verifiedUrl;
	}

	@Override
	public void update(Observable o, Object arg) {
		for (int i = 0; i < tableModel.getRowCount(); i++) {
			if (tableModel.getDownload(i).getStatus() != Download.COMPLETE)
				return;
		}
		actionExit();
	}

	public static void main(String[] args) {
		new Thread() {
			@Override
			public void run() {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						DownloadManager manager = new DownloadManager();
						manager.setModal(true);
						File outdir = new File(System.getProperty("user.home") + File.separator
								+ ".eduSPIM" + File.separator + "pre-acquired");
						if (!outdir.exists())
							outdir.mkdirs();
						Download fl = manager.addURL(
								"https://idisk-srv1.mpi-cbg.de/~bschmid/eduSPIM/fluorescence.avi",
								new File(outdir, "fluorescence.avi").getAbsolutePath(),
								new File(outdir, "fluorescence.tif").getAbsolutePath());
						Download tr = manager.addURL(
								"https://idisk-srv1.mpi-cbg.de/~bschmid/eduSPIM/transmission.avi",
								new File(outdir, "transmission.avi").getAbsolutePath(),
								new File(outdir, "transmission.tif").getAbsolutePath());
						manager.setVisible(true);

						System.out.println("bla");
						fl.getImage().show();
						tr.getImage().show();
					}
				});
			}
		}.start();

	}
}