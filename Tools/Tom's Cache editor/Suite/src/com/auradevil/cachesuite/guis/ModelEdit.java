package com.auradevil.cachesuite.guis;

import com.auradevil.t3d.T3DPanel;
import com.auradevil.t3d.data.Camera;
import com.auradevil.t3d.data.Model;
import com.auradevil.cachesuite.Main;
import com.auradevil.cachesuite.util.Model3DS;
import com.jagex.cache.CacheIndice;
import com.jagex.cache.RSModel;
import com.jagex.cache.configloaders.ItemConfig;
import com.jagex.cache.util.DataUtils;
import com.jagex.cache.util.exceptions.CacheException;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.File;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * @author tom
 */
public class ModelEdit extends JInternalFrame {
    private JList modelList;
    private JPanel viewPanel;
    private JPanel main;
	private JButton importButton;
	private JButton exportButton;
	private T3DPanel viewPort;
    CacheIndice modelIndice;

	public ModelEdit() {
        add(main);
		pack();
		setVisible(true);
        modelIndice = Main.logic.getCurrentCache().getIndice(1);
        rebuildModelsList();
		modelList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (modelList.getSelectedIndex() != -1) {
                    updatePreview();
                }
            }
        });
		importButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				try {
					byte[] data3DS = Main.logic.loadFromFile("3DS Models (*.3ds)", new String[]{"3ds", "3DS"});
					if (data3DS != null) {
						Model3DS model = new Model3DS(data3DS);
						RSModel m = model.toRSModel();
						byte[] uncompressed = m.recompile();
						DataUtils.writeFile(new File("/home/tom/out.rsm"), uncompressed);
						byte[] newModelData = DataUtils.gzCompress(uncompressed);
						Main.logic.addOrEditFile(1, modelList.getSelectedIndex(), newModelData);
						updatePreview();
					}
				} catch (IOException e) {
					JOptionPane.showMessageDialog(Main.logic.getSwingComponent(), "An unknown error occurred:\n" + e);
					e.printStackTrace();
				}
			}
		});
	}

    public void updatePreview() {
        try {
            byte[] modelData = modelIndice.getFile(modelList.getSelectedIndex());
            if (modelData[0] == 31 && (modelData[1]&0xff) == 139) {
                modelData = DataUtils.gzDecompress(modelData);
            }
            RSModel model = new RSModel(modelData);
			Model m = model.toModel();
            viewPort.setModel(m);
        } catch (IOException e) {
			if (e instanceof CacheException) {
				viewPort.setModel(null);
			} else {
            	e.printStackTrace();
			}
        }
    }

    private void createUIComponents() {
        viewPort = new T3DPanel();
        viewPort.setDrawFrame(false);
		viewPort.setDrawVerts(true);
        viewPanel = viewPort;
		// Set the camera up
		Camera camera = viewPort.getOurCamera();
		camera.setZ(-20);
		camera.setAngleX((float) Math.toRadians(25));
    }

    private void rebuildModelsList() {
		String[] values = new String[modelIndice.getNumFiles()];
		for (int i = 0; i < values.length; i++) {
			values[i] = String.valueOf(i);
		}
		modelList.setListData(values);
	}
}
