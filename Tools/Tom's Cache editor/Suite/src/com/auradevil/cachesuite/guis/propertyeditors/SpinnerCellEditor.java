package com.auradevil.cachesuite.guis.propertyeditors;

import com.l2fprod.common.beans.editor.AbstractPropertyEditor;

import javax.swing.*;

/**
 * @author tom
 */
public class SpinnerCellEditor extends AbstractPropertyEditor {

	public SpinnerCellEditor() {
		this(null);
	}


	public SpinnerCellEditor(SpinnerModel spinnerModel) {
		editor = new JSpinner(spinnerModel) {
			public void setValue(Object o) {
				if (o != null) {
					super.setValue(o);
				}
			}
		};

	}

	public Object getValue() {
		return ((JSpinner) editor).getValue();
	}

	public void setValue(Object value) {
		((JSpinner) editor).setValue(value);
	}
}
