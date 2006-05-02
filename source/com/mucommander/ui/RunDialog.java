
package com.mucommander.ui;

import com.mucommander.PlatformManager;
import com.mucommander.ProcessMonitor;
import com.mucommander.ProcessListener;
import com.mucommander.ui.comp.dialog.*;
import com.mucommander.text.Translator;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;



/**
 * Dialog used to execute a user-defined command.

 * Creates and displays a new dialog allowing the user to input a command which will be executed once the action is confirmed.
 * The command output of the user command is displayed in a text area
 *
 * @author Maxence Bernard
 */
public class RunDialog extends FocusDialog implements ActionListener, ProcessListener {
    private MainFrame mainFrame;
	
    private JTextField commandField;
	
    private JButton runStopButton;
    private JButton cancelButton;

    private JTextArea outputTextArea;
	
    private Process currentProcess;
    private ProcessMonitor processMonitor;
	
    // Dialog size constraints
    private final static Dimension MINIMUM_DIALOG_DIMENSION = new Dimension(480, 400);	
    //    // Dialog width should not exceed 360
    //   private final static Dimension MAXIMUM_DIALOG_DIMENSION = new Dimension(360,10000);	

    private static String lastCommand = "";

    private int caretPos;
	
    /**
     * Creates and displays a new RunDialog.
     * 
     * @param mainFrame the main frame this dialog is attached to.
     */
    public RunDialog(MainFrame mainFrame) {
        super(mainFrame, Translator.get("run_dialog.run_command"), mainFrame);
        this.mainFrame = mainFrame;
		
        Container contentPane = getContentPane();
        YBoxPanel mainPanel = new YBoxPanel();
		
        JLabel label = new JLabel(Translator.get("run_dialog.run_command_description")+":");
        mainPanel.add(label);

        commandField = new JTextField(lastCommand);
        // Text is selected so that user can directly type and replace path
        commandField.setSelectionStart(0);
        commandField.setSelectionEnd(lastCommand.length());
        commandField.addActionListener(this);

        mainPanel.add(commandField);
        mainPanel.addSpace(10);

        contentPane.add(mainPanel, BorderLayout.NORTH);

        mainPanel.add(new JLabel(Translator.get("run_dialog.command_output")+":"));
        outputTextArea = new JTextArea();
        outputTextArea.setLineWrap(true);
        outputTextArea.setRows(10);
        outputTextArea.setEditable(false);

        // Use a monospaced font in the command field and process output text area, as most terminals do.
        // The logical "Monospaced" font name is always available in Java.
        // The font size is the one of the default JTextArea, style is plain.
        Font monospacedFont = new Font("Monospaced", Font.PLAIN, outputTextArea.getFont().getSize());
        commandField.setFont(monospacedFont);
        outputTextArea.setFont(monospacedFont);

        JScrollPane scrollPane = new JScrollPane(outputTextArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        contentPane.add(scrollPane, BorderLayout.CENTER);
		
        // Run / Cancel buttons panel
        runStopButton = new JButton(Translator.get("run_dialog.run"));
        cancelButton = new JButton(Translator.get("cancel"));
        contentPane.add(DialogToolkit.createOKCancelPanel(runStopButton, cancelButton, this), BorderLayout.SOUTH);

        // Selects OK when enter is pressed
        getRootPane().setDefaultButton(runStopButton);
        
        // Path field will receive initial focus
        setInitialFocusComponent(commandField);		
			
        setMinimumSize(MINIMUM_DIALOG_DIMENSION);
        //		setMaximumSize(MAXIMUM_DIALOG_DIMENSION);

        // Closing this dialog kills the process
        addWindowListener(new WindowAdapter() {
                public void windowClosed(WindowEvent e) {
                    if(currentProcess!=null) {
                        currentProcess.destroy();
                        processMonitor.stopMonitoring();
                    }
                }
            });
		
        showDialog();
    }

	
    public void processDied(Process process, int retValue) {
        if(com.mucommander.Debug.ON) com.mucommander.Debug.trace("process "+process+" exit, return value= "+retValue);
        this.currentProcess = null;
        switchToRunState();
    }	

	
    public void processOutput(Process process, byte buffer[], int offset, int length) {
        if(com.mucommander.Debug.ON) com.mucommander.Debug.trace("processOutput "+process+" output= "+new String(buffer, 0, length));
        addToTextArea(buffer, offset, length);
    }

	
    public void processError(Process process, byte buffer[], int offset, int length) {
        if(com.mucommander.Debug.ON) com.mucommander.Debug.trace("processError "+process+" output= "+new String(buffer, 0, length));
        addToTextArea(buffer, offset, length);
    }


    private void addToTextArea(byte buffer[], int offset, int length) {
        outputTextArea.append(new String(buffer, 0, length));
        caretPos += length;
        outputTextArea.setCaretPosition(caretPos);
        outputTextArea.repaint();
    }
	
	
    private void switchToStopState() {
        // Change 'Run' button to 'Stop'
        this.runStopButton.setText(Translator.get("run_dialog.stop"));
        // Clear text area
        this.outputTextArea.setText("");
        //				// Make text area active
        //				this.outputTextArea.setEnabled(true);
        // Make command field disabled
        this.commandField.setEnabled(false);
        // Repaint the dialog
        repaint();
    }
	
    private void switchToRunState() {
        // Change 'Stop' button to 'Run'
        this.runStopButton.setText(Translator.get("run_dialog.run"));
        //		// Make text area not active anymore
        //		this.outputTextArea.setEnabled(false);
        // Make command field active again
        this.commandField.setEnabled(true);
        commandField.requestFocus();
        commandField.setSelectionStart(0);
        commandField.setSelectionEnd(lastCommand.length());
        // Repaint this dialog
        repaint();
    }	


    ////////////////////////////
    // ActionListener methods //
    ////////////////////////////
	
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
		
        // Run button starts a new command
        if(this.currentProcess==null && (source == runStopButton || source == commandField)) {
            String command = commandField.getText();
            this.lastCommand = command;
            this.currentProcess = PlatformManager.execute(command, mainFrame.getLastActiveTable().getCurrentFolder());
            // If command could be executed
            if(currentProcess!=null) {
                // Reset caret position
                caretPos = 0;
                switchToStopState();
                // And start monitoring the process and outputting to the text area
                processMonitor = new ProcessMonitor(this.currentProcess, this);
            }
            // Probably should notify the user if the command could not be executed
        }
        // Stop button stops current process
        else if(this.currentProcess!=null && source==runStopButton) {
            processMonitor.stopMonitoring();
            currentProcess.destroy();
            this.currentProcess = null;
            switchToRunState();
        }
        // Cancel button disposes the dialog and kills the process
        else if(source == cancelButton) {
            dispose();			
        }
    }	
}
