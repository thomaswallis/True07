package com.auradevil.cachesuite.guis;

import com.auradevil.cachesuite.Main;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.jagex.cache.Archive;
import com.jagex.cache.ImageArchive;
import com.jagex.cache.ImageBean;
import com.jagex.cache.ImageGroup;
import com.jagex.cache.util.DataUtils;
import com.l2fprod.common.propertysheet.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * @author tom
 */
public class ImageEdit extends JInternalFrame {
	public JList files;
	public JList images;
	public JButton addGroupButton;
	public JButton removeGroupButton;
	public JButton addImageButton;
	public JButton removeImageButton;
	public JButton importButton;
	public JButton exportButton;
	public JButton repackButton;
	public JLabel imageIDLabel;
	public JLabel widthLabel;
	public JLabel heightLabel;
	public JPanel main;
	public JPanel imagePanel;
	private PropertySheetPanel properties;
	//private ArrayList<ImageGroup> imageGroups = new ArrayList<ImageGroup>();
	private ArrayList<String> knownImages;
	private int[] knownHashes;
	private Archive jagArchive;
	private ImageArchive imageArchive;
	private String title;
	private boolean hasEdited = false;

	public ImageEdit(final int cacheFile, final ArrayList<String> knownImages) {
		add(main);
		setSize(600, 550);
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
		this.knownImages = knownImages;
		try {
			this.jagArchive = new Archive(Main.logic.getCurrentCache().getIndice(0).getFile(cacheFile));
			this.imageArchive = new ImageArchive(jagArchive);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(Main.logic.getSwingComponent(), "An error occurred whilst loading archive:\n" + e);
			e.printStackTrace();
		}
		reloadKnownHashes();
		populateFilesList();
		files.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent listSelectionEvent) {
				if (files.getSelectedIndex() != -1) {
					String selected = (String) files.getSelectedValue();
					if (selected != null) {
						populateImagesList();
						if (imageArchive.getImage(files.getSelectedIndex()).countImages() != 0) {
							images.setSelectedIndex(0);
						}
						removeGroupButton.setEnabled(true);
						addImageButton.setEnabled(true);
					} else {
						updateDisplayedImage();
						populateImagesList();
					}
				} else {
					removeGroupButton.setEnabled(false);
					addImageButton.setEnabled(false);
				}
			}
		});
		images.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent listSelectionEvent) {
				if (images.getSelectedIndex() != -1) {
					ImageBean i = imageArchive.getImage(files.getSelectedIndex()).getImageBean(images.getSelectedIndex());
					properties.readFromObject(i);
					properties.setEnabled(true);
					updateDisplayedImage();
					exportButton.setEnabled(true);
					importButton.setEnabled(true);
					removeImageButton.setEnabled(true);
				} else {
					properties.setEnabled(false);
					exportButton.setEnabled(false);
					importButton.setEnabled(false);
					removeImageButton.setEnabled(false);
				}
			}
		});
		exportButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				ImageGroup arch = imageArchive.getImage(files.getSelectedIndex());
				Image thisSprite = arch.getImage(images.getSelectedIndex());
				try {
					if (Main.logic.saveImageToFile(getImageBytes(thisSprite))) {
						JOptionPane.showMessageDialog(Main.logic.getSwingComponent(), "Image dumped sucessfully");
					}
				} catch (IOException e) {
					JOptionPane.showMessageDialog(Main.logic.getSwingComponent(), "An unknown error occurred!\n" + e);
					e.printStackTrace();
				}
			}
		});
		importButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				try {
					byte[] newImage = Main.logic.loadImageFromFile();
					ImageGroup arch = imageArchive.getImage(files.getSelectedIndex());
					arch.replaceImage(images.getSelectedIndex(), newImage, Main.logic.getSwingComponent());
					updateDisplayedImage();
					setEdited();
					JOptionPane.showMessageDialog(Main.logic.getSwingComponent(), "Image replaced sucessfully");
				} catch (Exception e) {
					if (!(e instanceof NullPointerException)) {
						JOptionPane.showMessageDialog(Main.logic.getSwingComponent(), "An unknown error occurred:\n" + e);
						e.printStackTrace();
					}
				}
			}
		});
		repackButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				try {
					byte[] newData = imageArchive.repackArchive();
					Main.logic.addOrEditFile(0, cacheFile, newData);
					hasEdited = false;
					setTitle(title);
				} catch (IOException e) {
					JOptionPane.showMessageDialog(Main.logic.getSwingComponent(), "An error occurred whilst repacking:\n" + e);
					e.printStackTrace();
				}
			}
		});
		addGroupButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				ImageGroup a = new ImageGroup();
				String s = JOptionPane.showInputDialog("Enter a name for the new image group:");
				imageArchive.addImage(DataUtils.getHash(s + ".dat"), a);
				knownImages.add(s + ".dat");
				setEdited();
				reloadKnownHashes();
				populateFilesList();
			}
		});
		removeGroupButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				int i = files.getSelectedIndex();
				imageArchive.removeImage(i);
				setEdited();
				populateFilesList();
				files.setSelectedIndex((i == 0) ? 0 : i - 1);
			}
		});
		imagePanel.addComponentListener(new ComponentListener() {
			public void componentResized(ComponentEvent componentEvent) {
				if (images.getSelectedIndex() != -1) {
					updateDisplayedImage();
				}
			}

			public void componentMoved(ComponentEvent componentEvent) {
				if (images.getSelectedIndex() != -1) {
					updateDisplayedImage();
				}
			}

			public void componentShown(ComponentEvent componentEvent) {
				if (images.getSelectedIndex() != -1) {
					updateDisplayedImage();
				}
			}

			public void componentHidden(ComponentEvent componentEvent) {
				if (images.getSelectedIndex() != -1) {
					updateDisplayedImage();
				}
			}
		});
		addImageButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				try {
					byte[] newImage = Main.logic.loadImageFromFile();
					ImageGroup arch = imageArchive.getImage(files.getSelectedIndex());
					if (newImage != null) {
						arch.addSprite(newImage, Main.logic.getSwingComponent());
						populateImagesList();
						images.setSelectedIndex(arch.countImages() - 1);
						setEdited();
						updateDisplayedImage();
						JOptionPane.showMessageDialog(Main.logic.getSwingComponent(), "Image added sucessfully");
					} else {
						JOptionPane.showMessageDialog(Main.logic.getSwingComponent(), "No image selected");
					}
				} catch (Exception e) {
					JOptionPane.showMessageDialog(Main.logic.getSwingComponent(), "Error adding image: " + e);
					e.printStackTrace();
				}
			}
		});
		removeImageButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				ImageGroup arch = imageArchive.getImage(files.getSelectedIndex());
				arch.removeSprite(images.getSelectedIndex());
				setEdited();
				populateImagesList();
				if (arch.countImages() != 0) {
					images.setSelectedIndex(0);
					updateDisplayedImage();
				}
				JOptionPane.showMessageDialog(Main.logic.getSwingComponent(), "Image removed sucessfully");
			}
		});
	}

	private byte[] getImageBytes(Image i) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ImageIO.write((RenderedImage) i, "png", bos);
		bos.close();
		return bos.toByteArray();
	}

	private void updateDisplayedImage() {
		BufferedImage buffer = new BufferedImage(imagePanel.getWidth(), imagePanel.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics g = buffer.getGraphics();
		g.clearRect(0, 0, imagePanel.getWidth(), imagePanel.getHeight());

		int xCols = (imagePanel.getWidth() / 5) + 1;
		int yCols = (imagePanel.getHeight() / 5) + 1;

		for (int row = 0; row < yCols; row++) {
			for (int col = 0; col < xCols; col++) {
				if ((row + col) % 2 == 0)
					g.setColor(Color.white);
				else
					g.setColor(Color.gray);
				g.fillRect(col * 5, row * 5, 5, 5);
			}
		}
		String selected = (String) files.getSelectedValue();
		if (selected != null) {
			ImageGroup arch = imageArchive.getImage(files.getSelectedIndex());
			BufferedImage thisSprite = arch.getImage(images.getSelectedIndex());
			imageIDLabel.setText("Image ID: " + images.getSelectedIndex());
			widthLabel.setText("Image Width: " + thisSprite.getWidth(this));
			heightLabel.setText("Image Height: " + thisSprite.getHeight(this));
			int middleX = (imagePanel.getWidth() / 2) - (thisSprite.getWidth(this) / 2);
			int middleY = (imagePanel.getHeight() / 2) - (thisSprite.getHeight(this) / 2);
			BufferedImage imageBuffer = new BufferedImage(thisSprite.getWidth(this), thisSprite.getHeight(this), BufferedImage.TYPE_INT_ARGB);
			for (int x = 0; x < thisSprite.getWidth(); x++) {
				for (int y = 0; y < thisSprite.getHeight(); y++) {
					int rgb = thisSprite.getRGB(x, y);
					int[] components = unpackRGB(rgb);
					if (!(components[0] == 255 && components[1] == 0 && components[2] == 255)) {
						imageBuffer.setRGB(x, y, rgb);
					}
				}
			}
			g.drawImage(imageBuffer, middleX, middleY, this);
		}
		imagePanel.getGraphics().drawImage(buffer, 0, 0, this);
	}

	private void populateFilesList() {
		int numImages = imageArchive.countImages();
		String[] values = new String[numImages];
		int z = 0;
		for (int i = 0; i < numImages; i++) {
			int thisFile = jagArchive.getIdentifierAt(i);
			if (imageArchive.validImage(thisFile)) {
				String thisFileS = String.valueOf(thisFile);
				for (int x = 0; x < knownImages.size(); x++) {
					if (thisFile == knownHashes[x]) {
						thisFileS = knownImages.get(x);
						break;
					}
				}
				values[z++] = thisFileS;
			}
		}
		files.setListData(values);
	}

	private void populateImagesList() {
		String selected = (String) files.getSelectedValue();
		if (selected != null) {
			try {
				ImageGroup selectedArc = imageArchive.getImage(files.getSelectedIndex());
				int numImages = selectedArc.countImages();
				String[] values = new String[numImages];
				for (int i = 0; i < numImages; i++) {
					values[i] = String.valueOf(i);
				}
				images.setListData(values);
			} catch (Exception e) {
				JOptionPane.showMessageDialog(Main.logic.getSwingComponent(), "The file you selected wasn't a valid sprite");
			}
		} else {
			String[] values = new String[0];
			images.setListData(values);
		}
	}

	public void reloadKnownHashes() {
		knownHashes = new int[knownImages.size()];
		for (int i = 0; i < knownImages.size(); i++) {
			knownHashes[i] = DataUtils.getHash(knownImages.get(i));
		}
	}

	private void setEdited() {
		if (!hasEdited) {
			hasEdited = true;
			title = getTitle();
			setTitle(title + " (*)");
		}
	}

	private int[] unpackRGB(int rgb) {
		int[] val = new int[3];
		val[0] = (rgb >> 16) & 0xFF; // red
		val[1] = (rgb >> 8) & 0xFF; // green
		val[2] = (rgb) & 0xFF;	   // blue
		return val;
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
		main.setLayout(new GridLayoutManager(4, 3, new Insets(0, 0, 0, 0), -1, -1));
		final JScrollPane scrollPane1 = new JScrollPane();
		main.add(scrollPane1, new GridConstraints(0, 0, 3, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(100, 400), null, new Dimension(150, -1), 0, false));
		scrollPane1.setBorder(BorderFactory.createTitledBorder("Files"));
		files = new JList();
		scrollPane1.setViewportView(files);
		final JPanel panel1 = new JPanel();
		panel1.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
		main.add(panel1, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(300, 75), null, new Dimension(-1, 100), 0, false));
		panel1.setBorder(BorderFactory.createTitledBorder("Info"));
		imageIDLabel = new JLabel();
		imageIDLabel.setText("No image selected");
		panel1.add(imageIDLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		widthLabel = new JLabel();
		widthLabel.setText("");
		panel1.add(widthLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		heightLabel = new JLabel();
		heightLabel.setText("");
		panel1.add(heightLabel, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JPanel panel2 = new JPanel();
		panel2.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
		main.add(panel2, new GridConstraints(2, 2, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		panel2.setBorder(BorderFactory.createTitledBorder("Operations"));
		importButton = new JButton();
		importButton.setEnabled(false);
		importButton.setText("Import");
		importButton.setMnemonic('I');
		importButton.setDisplayedMnemonicIndex(0);
		panel2.add(importButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		exportButton = new JButton();
		exportButton.setEnabled(false);
		exportButton.setText("Export");
		exportButton.setMnemonic('E');
		exportButton.setDisplayedMnemonicIndex(0);
		panel2.add(exportButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		repackButton = new JButton();
		repackButton.setText("Save Archive");
		repackButton.setMnemonic('S');
		repackButton.setDisplayedMnemonicIndex(0);
		panel2.add(repackButton, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JPanel panel3 = new JPanel();
		panel3.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
		main.add(panel3, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
		panel3.setBorder(BorderFactory.createTitledBorder("Preview"));
		imagePanel = new JPanel();
		imagePanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
		imagePanel.setBackground(new Color(-1));
		panel3.add(imagePanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		final JScrollPane scrollPane2 = new JScrollPane();
		main.add(scrollPane2, new GridConstraints(0, 1, 3, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(100, 400), null, new Dimension(150, -1), 0, false));
		scrollPane2.setBorder(BorderFactory.createTitledBorder("Images"));
		images = new JList();
		scrollPane2.setViewportView(images);
		final JPanel panel4 = new JPanel();
		panel4.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
		main.add(panel4, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		addGroupButton = new JButton();
		addGroupButton.setText("+");
		panel4.add(addGroupButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		removeGroupButton = new JButton();
		removeGroupButton.setEnabled(false);
		removeGroupButton.setText("-");
		panel4.add(removeGroupButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JPanel panel5 = new JPanel();
		panel5.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
		main.add(panel5, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		addImageButton = new JButton();
		addImageButton.setEnabled(false);
		addImageButton.setText("+");
		panel5.add(addImageButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		removeImageButton = new JButton();
		removeImageButton.setEnabled(false);
		removeImageButton.setText("-");
		panel5.add(removeImageButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
	}

	/**
	 * @noinspection ALL
	 */
	public JComponent $$$getRootComponent$$$() {
		return main;
	}

	private void createUIComponents() {
		PropertySheetTableModel model = new PropertySheetTableModel();

		DefaultProperty offsetX = new DefaultProperty();
		offsetX.setName("drawOffsetX");
		offsetX.setDisplayName("Draw Offset X");
		offsetX.setType(Integer.class);

		DefaultProperty offsetY = new DefaultProperty();
		offsetY.setName("drawOffsetY");
		offsetY.setDisplayName("Draw Offset Y");
		offsetY.setType(Integer.class);

		model.addProperty(offsetX);
		model.addProperty(offsetY);

		model.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
				int selected = files.getSelectedIndex();
				if (selected != -1) {
					ImageBean i = imageArchive.getImage(selected).getImageBean(images.getSelectedIndex());
					Property prop = (Property) propertyChangeEvent.getSource();
					if (prop.getName().equals("drawOffsetX")) {
						i.setDrawOffsetX((Integer) prop.getValue());
					} else if (prop.getName().equals("drawOffsetY")) {
						i.setDrawOffsetY((Integer) prop.getValue());
					}
					System.out.println("Updated " + prop);
				}
			}
		});

		PropertySheetTable table = new PropertySheetTable(model);
		properties = new PropertySheetPanel(table);
		properties.setEnabled(false);
	}
}
