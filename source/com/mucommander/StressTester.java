/*
 * This file is part of muCommander, http://www.mucommander.com
 * Copyright (c) 2002-2007 Maxence Bernard
 *
 * muCommander is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * muCommander is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with muCommander; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */


package com.mucommander;

import com.mucommander.file.AbstractFile;
import com.mucommander.ui.MainFrame;
import com.mucommander.ui.WindowManager;
import com.mucommander.ui.action.ActionManager;
import com.mucommander.ui.table.FileTable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;


public class StressTester implements Runnable, ActionListener {

    private static Thread stressThread;

    public StressTester() {
        (stressThread = new Thread(this)).start();
    }

    public static void stop() {
        stressThread = null;
    }

    public void run() {
        Random random = new Random();
        MainFrame mainFrame = WindowManager.getCurrentMainFrame();

        while(stressThread!=null) {
            if(random.nextInt(2)==0)
                ActionManager.performAction(com.mucommander.ui.action.SwitchActiveTableAction.class, mainFrame);    

            FileTable fileTable = mainFrame.getActiveTable();
            AbstractFile currentFolder = fileTable.getCurrentFolder();
            AbstractFile parentFolder = currentFolder.getParent();

            try {
                AbstractFile children[] = currentFolder.ls();
                // 1 in 3 chance to go up if folder has children
                if(children.length==0 || (random.nextInt(3)==0 && parentFolder!=null)) {
                    fileTable.selectRow(0);
                    ActionManager.performAction(com.mucommander.ui.action.OpenAction.class, mainFrame);
                }
                else {
                    AbstractFile randomChild = children[random.nextInt(children.length)];
                    if(!randomChild.isBrowsable())
                        continue;
                    // Try to ls() in RandomChild to trigger an IOException if folder is not readable
                    // so that no error dialog pops up when calling tryChangeCurrentFolder()
                    randomChild.ls();
                    fileTable.selectFile(randomChild);
                    ActionManager.performAction(com.mucommander.ui.action.OpenAction.class, mainFrame);
                    //					folderPanel.tryChangeCurrentFolder(randomChild, true);
                }
            }
            catch(Exception e) {
                if(com.mucommander.Debug.ON) com.mucommander.Debug.trace("Caught Exception: "+e);
            }

            if(com.mucommander.Debug.ON) com.mucommander.Debug.trace("Sleeping for a bit...");
            try { stressThread.sleep(100+random.nextInt(200)); }
            catch(InterruptedException e) { if(com.mucommander.Debug.ON) com.mucommander.Debug.trace("Caught InterruptedException "+e);}
        }
    }

    public void actionPerformed(ActionEvent e) {
        stop();
    }

    public static void main(String args[]) {
        Launcher.main(args);

        StressTester instance = new StressTester();
        JDialog stopDialog = new JDialog();
        JButton stopButton = new JButton("Stop");
        stopButton.addActionListener(instance);
        stopDialog.getContentPane().add(stopButton);
        stopDialog.setSize(new Dimension(80, 60));
        stopDialog.setVisible(true);
    }
}
