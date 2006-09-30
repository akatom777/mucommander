package com.mucommander.ui.action;

import com.mucommander.conf.ConfigurationManager;
import com.mucommander.text.Translator;
import com.mucommander.ui.MainFrame;
import com.mucommander.ui.ToolBar;

/**
 * This action shows/hides the current MainFrame's {@link com.mucommander.ui.ToolBar} depending on its
 * current visible state: if it is visible, hides it, if not shows it.
 *
 * <p>This action's label will be updated to reflect the current visible state.
 *
 * <p>Each time this action is executed, the new current visible state is stored in the configuration so that
 * new MainFrame windows will use it to determine whether the ToolBar has to be made visible or not.
 *
 * @author Maxence Bernard
 */
public class ToggleToolBarAction extends MucoAction {

    public ToggleToolBarAction(MainFrame mainFrame) {
        super(mainFrame);
        setLabel(Translator.get(ConfigurationManager.getVariableBoolean("prefs.toolbar.visible", true)?"com.mucommander.ui.action.ToggleToolBarAction.hide":"com.mucommander.ui.action.ToggleToolBarAction.show"));
    }

    public void performAction() {
        ToolBar toolBar = mainFrame.getToolBar();
        boolean visible = !toolBar.isVisible();
        // Save the last toolbar visible state in the configuration, this will become the default for new MainFrame windows.
        ConfigurationManager.setVariableBoolean("prefs.toolbar.visible", visible);
        // Change the label to reflect the new toolbar state
        setLabel(Translator.get(visible?"com.mucommander.ui.action.ToggleToolBarAction.hide":"com.mucommander.ui.action.ToggleToolBarAction.show"));
        // Show/hide the toolbar
        toolBar.setVisible(visible);
        mainFrame.validate();
    }
}
