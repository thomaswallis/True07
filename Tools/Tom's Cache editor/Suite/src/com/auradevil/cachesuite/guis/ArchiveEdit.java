package com.auradevil.cachesuite.guis;

import com.auradevil.cachesuite.Main;
import com.auradevil.cachesuite.guis.dialogs.RenameDialog;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.jagex.cache.Archive;
import com.jagex.cache.util.DataUtils;

import javax.swing.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;

/**
 * @author tom
 */
public class ArchiveEdit extends JInternalFrame {
	public JList files;
	public JLabel nameHashLabel;
	public JLabel sizeLabel;
	public JButton dumpFileButton;
	public JButton removeFileButton;
	public JButton replaceFileButton;
	public JButton renameFileButton;
	public JButton addFileButton;
	public JButton repackArchiveButton;
	public JPanel main;
	private Archive editingArchive;
	private boolean hasEdited = false;
	private String title;
	private ArrayList<String> knownNames;
	private int[] knownHashes;

	public ArchiveEdit(final Archive archive, final int cacheFile, ArrayList<String> knownNames) {
		this.knownNames = knownNames;
		reloadKnownHashes();
		add(main);
		pack();
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addInternalFrameListener(new InternalFrameAdapter() {
			@Override
			public void internalFrameClosing(InternalFrameEvent e) {
				if (hasEdited) {
					int response = JOptionPane.showConfirmDialog(Main.logic.getSwingComponent(), "This archive has been modified.\n" +
							"Are you sure you wish to exit without saving changes?", "Exit?", JOptionPane.YES_NO_OPTION);
					if (response == JOptionPane.YES_OPTION) {
						dispose();
					}
				} else {
					dispose();
				}
			}
		});
		editingArchive = archive;
		buildFilesList();
		files.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (files.getSelectedIndex() == -1) {
					nameHashLabel.setText("No file selected");
					sizeLabel.setText("");
					dumpFileButton.setEnabled(false);
					removeFileButton.setEnabled(false);
					replaceFileButton.setEnabled(false);
					renameFileButton.setEnabled(false);
					return;
				}
				nameHashLabel.setText("Name hash: " + archive.getIdentifierAt(files.getSelectedIndex()));
				sizeLabel.setText("Size: " + archive.getDecompressedSize(files.getSelectedIndex()));
				dumpFileButton.setEnabled(true);
				removeFileButton.setEnabled(true);
				replaceFileButton.setEnabled(true);
				renameFileButton.setEnabled(true);
			}
		});
		dumpFileButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					Main.logic.saveToFile(archive.getFileAt(files.getSelectedIndex()));
					JOptionPane.showMessageDialog(Main.logic.getSwingComponent(), "File dump sucessful");
				} catch (IOException e1) {
					JOptionPane.showMessageDialog(Main.logic.getSwingComponent(), "An error occurred dumping file:\n" + e1);
					e1.printStackTrace();
				}
			}
		});
		removeFileButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				archive.removeFile(files.getSelectedIndex());
				JOptionPane.showMessageDialog(Main.logic.getSwingComponent(), "Removed file sucessfully");
				buildFilesList();
				setEdited();
			}
		});
		replaceFileButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					byte[] newData = Main.logic.loadFromFile();
					archive.updateFile(files.getSelectedIndex(), newData);
					setEdited();
					JOptionPane.showMessageDialog(Main.logic.getSwingComponent(), "File replacement sucessful");
				} catch (IOException e1) {
					JOptionPane.showMessageDialog(Main.logic.getSwingComponent(), "An error occurred replacing file:\n" + e1.getMessage());
				}
			}
		});
		renameFileButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				RenameDialog rd = new RenameDialog(archive, files.getSelectedIndex());
				rd.setVisible(true);
				rd.pack();
				buildFilesList();
				setEdited();
				JOptionPane.showMessageDialog(Main.logic.getSwingComponent(), "File renamed sucessfuly");
			}
		});
		addFileButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					byte[] newData = Main.logic.loadFromFile();
					if (newData != null) {
						archive.addFile(-1, newData);
						RenameDialog rd = new RenameDialog(archive, archive.getTotalFiles() - 1);
						rd.setVisible(true);
						rd.pack();
						buildFilesList();
						setEdited();
						JOptionPane.showMessageDialog(Main.logic.getSwingComponent(), "File added sucessfuly");
					}
				} catch (IOException e1) {
					JOptionPane.showMessageDialog(Main.logic.getSwingComponent(), "An error occurred whilst adding file: " + e1);
					e1.printStackTrace();
				}
			}
		});
		repackArchiveButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					byte[] newData = archive.recompile();
					Main.logic.addOrEditFile(0, cacheFile, newData);
					hasEdited = false;
					setTitle(title);
				} catch (IOException e1) {
					JOptionPane.showMessageDialog(Main.logic.getSwingComponent(), "An error occurred whilst repacking archive: " + e1);
					e1.printStackTrace();
				}
			}
		});
	}

	public void buildFilesList2() {
		String[] values = new String[editingArchive.getTotalFiles()];
		for (int i = 0; i < editingArchive.getTotalFiles(); i++) {
			values[i] = String.valueOf(editingArchive.getIdentifierAt(i));
		}
		files.setListData(values);
	}

	private void buildFilesList() {
		int numImages = editingArchive.getTotalFiles();
		String[] values = new String[numImages];
		for (int i = 0; i < numImages; i++) {
			int thisFile = editingArchive.getIdentifierAt(i);
			String thisFileS = String.valueOf(thisFile);
			for (int x = 0; x < knownNames.size(); x++) {
				if (thisFile == knownHashes[x]) {
					thisFileS = knownNames.get(x);
					break;
				}
			}
			values[i] = thisFileS;
		}
		files.setListData(values);
	}

	public void reloadKnownHashes() {
		knownHashes = new int[knownNames.size()];
		for (int i = 0; i < knownNames.size(); i++) {
			knownHashes[i] = DataUtils.getHash(knownNames.get(i));
		}
	}

	private void setEdited() {
		if (!hasEdited) {
			hasEdited = true;
			title = getTitle();
			setTitle(title + " (*)");
		}
	}

	{
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
		$$$setupUI$$$();
	}

	/**
	 * Method generated by IntelliJ IDEA GUI Designer
	 * >>> IMPORTANT!! <<<
	 * DO NOT edit this method OR call it in your code!
	 *
	 * @noinspection ALL
	 */
	private void $$$setupUI$$$() {
		main = new JPanel();
		main.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
		final JScrollPane scrollPane1 = new JScrollPane();
		main.add(scrollPane1, new GridConstraints(0, 0, 2, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(100, 400), null, new Dimension(150, -1), 0, false));
		scrollPane1.setBorder(BorderFactory.createTitledBorder("Files"));
		files = new JList();
		scrollPane1.setViewportView(files);
		final JPanel panel1 = new JPanel();
		panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
		main.add(panel1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(300, 75), new Dimension(217, 14), new Dimension(-1, 100), 0, false));
		panel1.setBorder(BorderFactory.createTitledBorder("Info"));
		nameHashLabel = new JLabel();
		nameHashLabel.setText("No file selected");
		panel1.add(nameHashLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		sizeLabel = new JLabel();
		sizeLabel.setText("");
		panel1.add(sizeLabel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JPanel panel2 = new JPanel();
		panel2.setLayout(new GridLayoutManager(5, 2, new Insets(0, 0, 0, 0), -1, -1));
		main.add(panel2, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
		panel2.setBorder(BorderFactory.createTitledBorder("Operations"));
		dumpFileButton = new JButton();
		dumpFileButton.setEnabled(false);
		dumpFileButton.setText("Dump File");
		dumpFileButton.setMnemonic('D');
		dumpFileButton.setDisplayedMnemonicIndex(0);
		panel2.add(dumpFileButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		removeFileButton = new JButton();
		removeFileButton.setEnabled(false);
		removeFileButton.setText("Remove File");
		removeFileButton.setMnemonic('F');
		removeFileButton.setDisplayedMnemonicIndex(7);
		panel2.add(removeFileButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		replaceFileButton = new JButton();
		replaceFileButton.setEnabled(false);
		replaceFileButton.setText("Replace File");
		replaceFileButton.setMnemonic('R');
		replaceFileButton.setDisplayedMnemonicIndex(0);
		panel2.add(replaceFileButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		renameFileButton = new JButton();
		renameFileButton.setEnabled(false);
		renameFileButton.setText("Rename File");
		renameFileButton.setMnemonic('E');
		renameFileButton.setDisplayedMnemonicIndex(1);
		panel2.add(renameFileButton, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		addFileButton = new JButton();
		addFileButton.setText("Add File");
		addFileButton.setMnemonic('A');
		addFileButton.setDisplayedMnemonicIndex(0);
		panel2.add(addFileButton, new GridConstraints(3, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final Spacer spacer1 = new Spacer();
		panel2.add(spacer1, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
		final Spacer spacer2 = new Spacer();
		panel2.add(spacer2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
		repackArchiveButton = new JButton();
		repackArchiveButton.setText("Repack Archive");
		repackArchiveButton.setMnemonic('P');
		repackArchiveButton.setDisplayedMnemonicIndex(2);
		panel2.add(repackArchiveButton, new GridConstraints(4, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
	}

	/**
	 * @noinspection ALL
	 */
	public JComponent $$$getRootComponent$$$() {
		return main;
	}
}
