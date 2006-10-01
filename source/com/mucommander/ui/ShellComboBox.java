package com.mucommander.ui;

import com.mucommander.ShellHistoryManager;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import java.awt.*;
import java.awt.event.*;
import java.util.Iterator;

/**
 * Widget used for shell command input.
 * <p>
 * In addition to providing basic shell command input features, this widget interfaces with
 * the {@link com.mucommander.ShellHistoryManager} to offer a history of shell commands
 * for the user to browse through.
 * </p>
 * @author Maxence Bernard, Nicolas Rinaudo
 */
public class ShellComboBox extends JComboBox implements ActionListener, KeyListener {
    // - Instance fields -----------------------------------------------------
    // -----------------------------------------------------------------------
    /** Input field used to type in commands. */
    private JTextField input;
    /** Where to run commands. */
    private RunDialog  parent;
    /** Whether to ignore UI events or not. */
    private boolean    ignoreEvents = true;



    // - Initialisation ------------------------------------------------------
    // -----------------------------------------------------------------------
    /**
     * Creates a new shell combo box.
     * @param parent where to execute commands.
     */
    public ShellComboBox(RunDialog parent) {
        this.parent = parent;

        // Sets the combo box's editor.
        this.input = new JTextField();
        setEditor(new BasicComboBoxEditor() {
                public Component getEditorComponent() {
                    return ShellComboBox.this.input;
                }
            });
        setEditable(true);

        // Listen to action events generated by the combo box (popup menu selection)
        addActionListener(this);
        // Listen to key events generated by the text field (enter and escape)
        input.addKeyListener(this);

        // Prevent up/down keys from firing ActionEvents (default behavior is plain stupid)
        // Java 1.3
        putClientProperty("JComboBox.lightweightKeyboardNavigation","Lightweight");
        // Java 1.4 and up
        putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE);

        // Fills the combo box with the current history.
        populateHistory();
    }

    /**
     * Fills the combo box with the current shell history.
     */
    private void populateHistory() {
        Iterator iterator;

        // Empties the content of the combo box
        removeAllItems();

        // Iteratores through all shell history elements.
        iterator = ShellHistoryManager.getHistoryIterator();
        while(iterator.hasNext())
            insertItemAt(iterator.next(), 0);
    }



    // - Misc. ----------------------------------------------------------------------
    // ------------------------------------------------------------------------------

    /**
     * Overrides this method to ignore events received when this component is disabled.
     */
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        this.ignoreEvents = !enabled;
        if(enabled) {
            input.setSelectionStart(0);
            input.setSelectionEnd(input.getText().length());
        }
    }

    /**
     * Reacts to selections in the combo box.
     */
    public void actionPerformed(ActionEvent e) {
        // Return if events should be ignored
        if(ignoreEvents)
            return;

        Object selectedItem = getSelectedItem();
        // If a folder was selected in the combo popup menu, change current folder to the selected one
        if(selectedItem!=null) {
            // Explicitely hide popup, seems to be necessary under Windows/Java 1.5
            hidePopup();

            // Runs the requested command.
            setEnabled(false);
            runCommand((String)selectedItem);            
        }
    }

    /**
     * Reacts to validation in the input field.
     */
    public void keyPressed(KeyEvent e) {
        // Return if events should be ignored or if popup is visible (events would pertain to combo popup, not text field)
        if(ignoreEvents || isPopupVisible())
            return;

        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
        }
        else if(e.getKeyCode()==KeyEvent.VK_ENTER) {
            setEnabled(false);
            runCommand(input.getText());
        }
    }

    /**
     * Not used.
     */
    public void keyTyped(KeyEvent e) {}

    /**
     * Not used.
     */
    public void keyReleased(KeyEvent e) {}



    // - Command handling -----------------------------------------------------------
    // ------------------------------------------------------------------------------
    /**
     * Returns the current shell command.
     * @param add whether or not that command should be added to the history.
     * @return the current shell command.
     */
    public String getCommand(boolean add) {
        String command;

        command = input.getText();
        if(add) {
            ShellHistoryManager.add(command);
            populateHistory();
        }

        return command;
    }

    /**
     * Runs the specified command and adds it to history.
     * @param command command to run.
     */
    private void runCommand(String command) {
        ShellHistoryManager.add(command);
        populateHistory();
        input.setText(command);
        parent.runCommand(command);
    }
}
