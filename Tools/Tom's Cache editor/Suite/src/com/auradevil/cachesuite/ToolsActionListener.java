package com.auradevil.cachesuite;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author tom
 */
public class ToolsActionListener implements ActionListener {
	public void actionPerformed(ActionEvent actionEvent) {
		SuiteLogic logic = Main.logic;
		if (actionEvent.getActionCommand().equals("cacheedit")) {
			logic.showSelectCache();
		} else if (actionEvent.getActionCommand().equals("archiveedit")) {
			logic.showSelectArchive();
		} else if (actionEvent.getActionCommand().equals("imageedit")) {
			logic.showSelectImage();
		} else if (actionEvent.getActionCommand().equals("flooredit")) {
			logic.editFloors();
		} else if (actionEvent.getActionCommand().equals("itemedit")) {
			logic.editItems();
		} else if (actionEvent.getActionCommand().equals("modeledit")) {
            logic.editModels();
        }
	}
}
