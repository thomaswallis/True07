package com.auradevil.cachesuite;

import com.auradevil.cachesuite.guis.ToolsUI;
import com.auradevil.cachesuite.util.VersionChecker;
import com.jagex.cache.RSModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author tom
 */
public class Main extends JFrame {
	public static final float VERSION = 0.17f;
	private JDesktopPane mainPane;
	private static MenuActionListener menuListener = new MenuActionListener();
	private ToolsUI toolsUI;
	public static SuiteLogic logic;

	public static void main(String[] args) throws IOException {
		Main m = new Main();
		logic = new SuiteLogic(m);
		if (args.length > 1) {
			if (args[0].equals("-cache")) {
				logic.loadCacheFromDir(args[1]);
			}
		}
		RSModel.init();
	}

	public Main() throws HeadlessException {
		super("Runescape Cache Suite V" + VERSION + " BETA by Tom (www.Nuke-Net.com)");
		setLayout(new BorderLayout());
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		buildMenu();

		mainPane = new JDesktopPane();
		add(mainPane, BorderLayout.CENTER);

		setSize(900, 700);

		setLocationByPlatform(true);
		setVisible(true);

		toolsUI = new ToolsUI();
		createStaticFrame("Toolkit", toolsUI.mainPanel);
	}

	public void addFrame(JInternalFrame frame) {
		frame.setClosable(true);
		frame.setResizable(true);
		frame.setMaximizable(true);
		frame.setIconifiable(true);
		frame.setVisible(true);
		frame.setLocation(250, 120);
		mainPane.add(frame);
	}

	public void createFrame(String title, JPanel framePanel) {
		JInternalFrame frame = new JInternalFrame(title, true, true, true, true);
		frame.setVisible(true);
		frame.add(framePanel);
		frame.pack();
		frame.setLocation(250, 120);
		mainPane.add(frame);
	}

	public void createStaticFrame(String title, JPanel framePanel) {
		JInternalFrame frame = new JInternalFrame(title, false, false, false, false);
		frame.setVisible(true);
		frame.add(framePanel);
		frame.pack();
		mainPane.add(frame);
	}

	private void buildMenu() {
		JMenuBar menuBar = new JMenuBar();

		// Build File Menu
		JMenu fileMenu = new JMenu("File");
		JMenuItem openItem = new JMenuItem("Load Cache...");
		openItem.setActionCommand("loadcache");
		openItem.addActionListener(menuListener);
		fileMenu.add(openItem);

		menuBar.add(fileMenu);

		JMenu helpMenu = new JMenu("Help");

		JMenuItem visitForum = new JMenuItem("Visit help forum");
		visitForum.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				try {
					Desktop.getDesktop().browse(new URI("http://nuke-net.com/forum/index.php"));
				} catch (IOException e) {
					e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
				} catch (URISyntaxException e) {
					e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
				}
			}
		});

		helpMenu.add(visitForum);

		JMenuItem checkVerItem = new JMenuItem("Check for update..");
		checkVerItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				try {
					float newVer = VersionChecker.getLatestVersion();
					if (Main.VERSION < newVer) {
						int response = JOptionPane.showConfirmDialog(logic.getSwingComponent(), "A newer version of the Cache Suite is available!\n" +
								"Your version: " + Main.VERSION + " Latest Version: " + newVer + "\n" +
								"Would you like to download the latest version?", "Update?", JOptionPane.YES_NO_OPTION);
						if (response == JOptionPane.YES_OPTION) {
							try {
								Desktop.getDesktop().browse(new URI("http://nuke-net.com/?page_id=169"));
							} catch (IOException e) {
								e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
							} catch (URISyntaxException e) {
								e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
							}
						}
					} else {
						JOptionPane.showMessageDialog(logic.getSwingComponent(), "You currently have the latest version!");
					}
				} catch (IOException e) {
					JOptionPane.showMessageDialog(logic.getSwingComponent(), "An error occurred whilst checking for updates:\n"
							+ e);
					e.printStackTrace();
				}
			}
		});

		helpMenu.add(checkVerItem);

		menuBar.add(helpMenu);

		add(menuBar, BorderLayout.NORTH);
	}

	public ToolsUI getTools() {
		return toolsUI;
	}
}
