package com.auradevil.cachesuite;

import com.auradevil.cachesuite.guis.*;
import com.auradevil.cachesuite.guis.dialogs.ChooseArchive;
import com.auradevil.cachesuite.guis.dialogs.ChooseCache;
import com.auradevil.cachesuite.guis.dialogs.ChooseImage;
import com.jagex.cache.Archive;
import com.jagex.cache.Cache;
import com.jagex.cache.CacheIndice;
import com.jagex.cache.util.DataUtils;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;

/**
 * @author tom
 */
public class SuiteLogic {
	private Cache currentCache;
	private Main swingComponent;
	private JFileChooser fc = new JFileChooser();
	private String[] archiveDefaults = {
			"title.jag", "config.jag", "interface.jag", "media.jag",
			"versionlist.jag", "textures.jag", "wordenc.jag", "sounds.jag"
	};
	private ArrayList<String>[] knownJagNames;

	public void loadKnownArchiveNames() {
		CacheIndice jagIndice = currentCache.getIndice(0);
		knownJagNames = new ArrayList[jagIndice.getNumFiles() - 1];
		for (int i = 0; i < jagIndice.getNumFiles() - 1; i++) {
			try {
				BufferedReader r = new BufferedReader(new FileReader("./archivenames/" + (i + 1) + ".txt"));
				String s;
				knownJagNames[i] = new ArrayList<String>();
				while ((s = r.readLine()) != null) {
					knownJagNames[i].add(s);
				}
				r.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}


	public SuiteLogic(Component swingComponent) {
		this.swingComponent = (Main) swingComponent;
	}

	public void loadNewCache() throws IOException {
		fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int response = fc.showOpenDialog(swingComponent);
		if (response == JFileChooser.APPROVE_OPTION) {
			File selectedDir = fc.getSelectedFile();
			if (selectedDir.isDirectory()) {
				loadCacheFromDir(selectedDir.getAbsolutePath());
			}
		}
	}

	public void loadCacheFromDir(String absolutePath) throws IOException {
		currentCache = new Cache(absolutePath);
		ToolsUI toolkit = swingComponent.getTools();
		toolkit.viewEditRawFileButton.setEnabled(true);
		toolkit.viewEditJaGeXArchivesButton.setEnabled(true);
		toolkit.viewEditImageArchivesButton.setEnabled(true);
		toolkit.viewEditFloorConfigurationButton.setEnabled(true);
		toolkit.viewEditItemConfigurationButton.setEnabled(true);
        toolkit.importExport3DModelsButton.setEnabled(true);
		loadKnownArchiveNames();
	}

	public void rebuildCache() throws IOException {
		currentCache.rebuildCache();
	}

	public void addOrEditFile(int cache, int file, byte[] data) {
		CacheIndice indice = Main.logic.getCurrentCache().getIndice(cache);
		try {
			indice.addOrEditFile(file, data.length, data, true);
			JOptionPane.showMessageDialog(Main.logic.getSwingComponent(), "Archive repacked sucessfully");
		} catch (IOException e) {
			if (e.toString().contains("Cache must be rebuilt")) {
				repackCache();
			} else {
				JOptionPane.showMessageDialog(Main.logic.getSwingComponent(), "An error occurred whilst repacking cache:\n" + e);
				e.printStackTrace();
			}
		}
	}

	public void repackCache() {
		int response = JOptionPane.showConfirmDialog(Main.logic.getSwingComponent(), "Cache must be rebuilt to avoid corruption.\n" +
						"Would you like to rebuild the cache?", "Select an option", JOptionPane.YES_NO_OPTION);
				switch (response) {
					case 0: // Yes
						try {
							Main.logic.rebuildCache();
							JOptionPane.showMessageDialog(Main.logic.getSwingComponent(), "Cache repacked sucessfully");
						} catch (IOException e1) {
							JOptionPane.showMessageDialog(Main.logic.getSwingComponent(), "An error occurred whilst rebuilding archive:\n" +
									e1);
							e1.printStackTrace();
						}
						break;
					case 1: // No
						JOptionPane.showMessageDialog(Main.logic.getSwingComponent(), "The cache will not be modified");
						break;
				}
	}

	public void showSelectCache() {
		ChooseCache c = new ChooseCache();
		for (String s : currentCache.getIndexFiles()) {
			c.cache.addItem(s);
		}
		c.pack();
		c.setLocationByPlatform(true);
		c.setVisible(true);
	}

	public Component getSwingComponent() {
		return swingComponent;
	}

	public Cache getCurrentCache() {
		return currentCache;
	}

	public void editCache(int cacheIndex) {
		CacheEdit e = new CacheEdit(cacheIndex);
		e.setTitle("Editing Cache: " + currentCache.getIndexFile(cacheIndex));
		swingComponent.addFrame(e);
	}

	public boolean saveToFile(byte[] data) throws IOException {
		fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		int response = fc.showSaveDialog(swingComponent);
		if (response == JFileChooser.APPROVE_OPTION) {
			File selected = fc.getSelectedFile();
			DataUtils.writeFile(selected, data);
			return true;
		}
		return false;
	}

	public boolean saveImageToFile(byte[] data) throws IOException {
		return saveToFile(data, "PNG Files (*.png)", new String[]{"png"});
	}


	public boolean saveToFile(byte[] data, final String description, String[] extensions) throws IOException {
		fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		fc.setFileFilter(new FileTypeFilter(description, extensions));
		int response = fc.showSaveDialog(swingComponent);
		if (response == JFileChooser.APPROVE_OPTION) {
			File selected = fc.getSelectedFile();
			DataUtils.writeFile(selected, data);
			return true;
		}
		return false;
	}

	public byte[] loadFromFile() throws IOException {
		fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		int response = fc.showSaveDialog(swingComponent);
		if (response == JFileChooser.APPROVE_OPTION) {
			File selected = fc.getSelectedFile();
			return DataUtils.readFile(selected);
		}
		return null;
	}

	public byte[] loadImageFromFile() throws IOException {
		return loadFromFile("Image Files (*.gif, *.jpg, *.png)", new String[]{"gif", "jpg", "png"});
	}

	public byte[] loadFromFile(final String description, String[] extensions) throws IOException {
		fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		fc.setFileFilter(new FileTypeFilter(description, extensions));
		int response = fc.showSaveDialog(swingComponent);
		if (response == JFileChooser.APPROVE_OPTION) {
			File selected = fc.getSelectedFile();
			return DataUtils.readFile(selected);
		}
		return null;
	}

	public void showSelectArchive() {
		ChooseArchive a = new ChooseArchive();
		CacheIndice jagIndice = currentCache.getIndice(0);
		for (int i = 1; i < jagIndice.getNumFiles(); i++) {
			a.archives.addItem(archiveDefaults[i - 1]);
		}
		a.pack();
		a.setLocationByPlatform(true);
		a.setVisible(true);
	}

	public void editArchive(int archive) {
		CacheIndice c = currentCache.getIndice(0);
		Archive a = null;
		try {
			a = new Archive(c.getFile(archive + 1));
		} catch (IOException e) {
			JOptionPane.showMessageDialog(Main.logic.getSwingComponent(), "An error occurred whilst loading archive:\n" + e);
			e.printStackTrace();
		}
		ArchiveEdit ae = new ArchiveEdit(a, archive + 1, knownJagNames[archive]);
		ae.setTitle("Editing Archive: " + archiveDefaults[archive]);
		swingComponent.addFrame(ae);
	}

	public void editImage(int jagFile) {
		ImageEdit ie = new ImageEdit(jagFile, knownJagNames[jagFile - 1]);
		ie.setTitle("Editing Images: " + archiveDefaults[jagFile - 1]);
		swingComponent.addFrame(ie);
	}

	public void showSelectImage() {
		ChooseImage ci = new ChooseImage();
		ci.pack();
		ci.setLocationByPlatform(true);
		ci.setVisible(true);
	}

	public void editFloors() {
		try {
			Archive textureJag = new Archive(getCurrentCache().getIndice(0).getFile(6));
			FloorEdit f = new FloorEdit(textureJag);
			f.setTitle("Editing Config: Floors");
			swingComponent.addFrame(f);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(getSwingComponent(), "Error locating textures:\n" + e);
			e.printStackTrace();
		}
	}

	public void editItems() {
		ItemEdit i = new ItemEdit();
		i.setTitle("Editing Config: Items");
		swingComponent.addFrame(i);
	}

    public void editModels() {
        ModelEdit m = new ModelEdit();
        m.setTitle("Editing model archive");
        swingComponent.addFrame(m);
    }

    class FileTypeFilter extends javax.swing.filechooser.FileFilter {
		String description;

		String extensions[];

		public FileTypeFilter(String description, String extension) {
			this(description, new String[]{extension});
		}

		public FileTypeFilter(String description, String extensions[]) {
			if (description == null) {
				this.description = extensions[0];
			} else {
				this.description = description;
			}
			this.extensions = (String[]) extensions.clone();
			toLower(this.extensions);
		}

		private void toLower(String array[]) {
			for (int i = 0, n = array.length; i < n; i++) {
				array[i] = array[i].toLowerCase();
			}
		}

		public String getDescription() {
			return description;
		}

		public boolean accept(File file) {
			if (file.isDirectory()) {
				return true;
			} else {
				String path = file.getAbsolutePath().toLowerCase();
				for (int i = 0, n = extensions.length; i < n; i++) {
					String extension = extensions[i];
					if ((path.endsWith(extension) && (path.charAt(path.length() - extension.length() - 1)) == '.')) {
						return true;
					}
				}
			}
			return false;
		}
	}
}
