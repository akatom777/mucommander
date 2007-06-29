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


package com.mucommander.job;

import com.mucommander.conf.ConfigurationManager;
import com.mucommander.conf.ConfigurationVariables;
import com.mucommander.file.AbstractFile;
import com.mucommander.file.MimeTypes;
import com.mucommander.file.util.FileSet;
import com.mucommander.io.Base64OutputStream;
import com.mucommander.text.Translator;
import com.mucommander.ui.MainFrame;
import com.mucommander.ui.ProgressDialog;

import java.io.*;
import java.net.Socket;
import java.util.StringTokenizer;
import java.util.Vector;


/**
 * This job sends one or several files by email.
 *
 * @author Maxence Bernard
 */
public class SendMailJob extends TransferFileJob {

    /** True after connection to mail server has been established */
    private boolean connectedToMailServer;

    /** Error dialog title */
    private String errorDialogTitle;
	
	
    /////////////////////
    // Mail parameters //
    /////////////////////
    /** Email recipient(s) */
    private String recipientString;
    /** Email subject */
    private String mailSubject;
    /** Email body */
    private String mailBody;

    /** SMTP server */
    private String mailServer;
    /** From name */
    private String fromName;
    /** From address */
    private String fromAddress;
	
    /** Email boundary string, delimits the end of the body and attachments */
    private String boundary;

    /** Connection variable */
    private BufferedReader in;
    /** OuputStream to the SMTP server */
    private OutputStream out;
    /** Base64OuputStream to the SMTP server */
    private Base64OutputStream out64;
    /** Socket connection to the SMTP server */
    private Socket socket;

	
    private final static String CLOSE_TEXT = Translator.get("close");
    private final static int CLOSE_ACTION = 11;
	
	
    public SendMailJob(ProgressDialog progressDialog, MainFrame mainFrame, FileSet filesToSend, String recipientString, String mailSubject, String mailBody) {
        super(progressDialog, mainFrame, filesToSend);

        this.boundary = "mucommander"+System.currentTimeMillis();
        this.recipientString = recipientString;
        this.mailSubject = mailSubject;
        this.mailBody = mailBody+"\n\n"+"Sent by muCommander - http://www.mucommander.com\n";

        this.mailServer = ConfigurationManager.getVariable(ConfigurationVariables.SMTP_SERVER);
        this.fromName = ConfigurationManager.getVariable(ConfigurationVariables.MAIL_SENDER_NAME);
        this.fromAddress = ConfigurationManager.getVariable(ConfigurationVariables.MAIL_SENDER_ADDRESS);
    
        this.errorDialogTitle = Translator.get("email_dialog.error_title");
    }

    /**
     * Returns true if mail preferences have been set.
     */
    public static boolean mailPreferencesSet() {
        return ConfigurationManager.isVariableSet(ConfigurationVariables.SMTP_SERVER)
            && ConfigurationManager.isVariableSet(ConfigurationVariables.MAIL_SENDER_NAME)
            && ConfigurationManager.isVariableSet(ConfigurationVariables.MAIL_SENDER_ADDRESS);
    }


    /**
     * Shows an error dialog with a single action : close, and stops the job.
     */
    private void showErrorDialog(String message) {
        showErrorDialog(errorDialogTitle, message, new String[]{CLOSE_TEXT}, new int[]{CLOSE_ACTION});
        interrupt();
    }
	
	
    /***********************************************
     *** Methods taking care of sending the mail ***
     ***********************************************/
    
    private void openConnection() throws IOException {
        this.socket = new Socket(mailServer, 25);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        this.out = socket.getOutputStream();
        this.out64 = new Base64OutputStream(out, true);
		
        this.connectedToMailServer = true;
    }

    private void sendBody() throws IOException {
        // here you are supposed to send your username
        readWriteLine("HELO muCommander");
        // warning : some mail server validate the sender address and will fail is an invalid
        // address is provided
        readWriteLine("MAIL FROM: "+fromAddress);
		
        Vector recipients = new Vector();
        recipientString = splitRecipientString(recipientString, recipients);
        int nbRecipients = recipients.size();
        for(int i=0; i<nbRecipients; i++)
            readWriteLine("RCPT TO: <"+recipients.elementAt(i)+">" );
        readWriteLine("DATA");
        writeLine("MIME-Version: 1.0");
        writeLine("Subject: "+this.mailSubject);
        writeLine("From: "+this.fromName+" <"+this.fromAddress+">");
        writeLine("To: "+recipientString);
        writeLine("Content-Type: multipart/mixed; boundary=\"" + boundary +"\"");
        writeLine("\r\n--" + boundary);

        // Send the body
        //        writeLine( "Content-Type: text/plain; charset=\"us-ascii\"\r\n");
        writeLine("Content-Type: text/plain; charset=\"utf-8\"\r\n");
        writeLine(this.mailBody+"\r\n\r\n");
        writeLine("\r\n--" +  boundary );        
    }
    

    /**
     * Parses the specified string, replaces delimiter characters if needed and adds recipients  (String instances) to the given Vector.
     *
     * @param recipientsStr String containing one or several recipients that need to be separated by ',' and/or ';' characters.
     */
    private String splitRecipientString(String recipientsStr, Vector recipients) {

        // /!\ this piece of code is far from being bullet proof but I'm too lazy now to rewrite it
        StringBuffer newRecipientsSb = new StringBuffer();
        StringTokenizer st = new StringTokenizer(recipientsStr, ",;");
        String rec;
        int pos1, pos2;
        while(st.hasMoreTokens()) {
            rec = st.nextToken().trim();
            if((pos1=rec.indexOf('<'))!=-1 && (pos2=rec.indexOf('>', pos1+1))!=-1)
                recipients.add(rec.substring(pos1+1, pos2));
            else
                recipients.add(rec);
            newRecipientsSb.append(rec+(st.hasMoreTokens()?", ":""));
        }
		
        return newRecipientsSb.toString();
    }
	
	
    /**
     * Send file as attachment encoded in Base64, and returns true if file was successfully
     * and completely transferred.
     */ 
    private void sendAttachment(AbstractFile file) throws IOException {
        InputStream fileIn = null;
        try {
            // Send MIME type of attachment file
            String mimeType = MimeTypes.getMimeType(file);
            // Default mime type
            if(mimeType==null)
                mimeType = "application/octet-stream";
            writeLine("Content-Type:"+mimeType+"; name="+file.getName());
            writeLine("Content-Disposition: attachment;filename=\""+file.getName()+"\"");
            writeLine("Content-transfer-encoding: base64\r\n");
            fileIn = setCurrentInputStream(file.getInputStream());
            
            // Write file to socket
            AbstractFile.copyStream(fileIn, out64);
	
            // Writes padding bytes without closing the stream.
            out64.writePadding();
	
            writeLine("\r\n--" + boundary);
        }
        catch(IOException e) {
            throw e;
        }
        finally {
            if(fileIn!=null)
                fileIn.close();
        }
    }
	
    private void sayGoodBye() throws IOException {
        writeLine("\r\n\r\n--" + boundary + "--\r\n");
        readWriteLine(".");
        readWriteLine("QUIT");
    }


    private void closeConnection() {
        try {
            socket.close();
            in.close();
            out64.close();
        }
        catch(Exception e){
        }
    }
    
    private void readWriteLine(String s) throws IOException {
        out.write((s + "\r\n").getBytes("UTF-8"));
        in.readLine();
    }

    private void writeLine(String s) throws IOException {
        out.write((s + "\r\n").getBytes("UTF-8"));
    }


    ////////////////////////////////////
    // TransferFileJob implementation //
    ////////////////////////////////////

    protected boolean processFile(AbstractFile file, Object recurseParams) {
        if(getState()==INTERRUPTED)
            return false;

        // Send file attachment
        try {
            sendAttachment(file);
        }
        catch(IOException e) {
            showErrorDialog(Translator.get("email.send_file_error", file.getName()));
            return false;
        }

        // If this was the last file, notify the mail server that the mail is over
        if(getCurrentFileIndex()==getNbFiles()-1) {
            try {
                // Say goodbye to the server
                sayGoodBye();
            }
            catch(IOException e) {
                showErrorDialog(Translator.get("email.goodbye_failed"));
                return false;
            }
        }

        return true;
    }


    public String getStatusString() {
        if(connectedToMailServer)
            return Translator.get("email.sending_file", getCurrentFileInfo());
        else
            return Translator.get("email.connecting_to_server", mailServer);
    }

    
    protected boolean hasFolderChanged(AbstractFile folder) {
        // This job does not modify anything
        return false;
    }


    ///////////////////////
    // Overridden method //
    ///////////////////////

    /**
     * This method is called when this job starts, before the first call to {@link #processFile(AbstractFile,Object) processFile()} is made.
     * This method here does nothing but it can be overriden by subclasses to perform some first-time initializations.
     */
    protected void jobStarted() {
        super.jobStarted();

        // Open socket connection to the mail server, and say hello
        try {
            openConnection();
        }
        catch(IOException e) {
            showErrorDialog(Translator.get("email.server_unavailable", mailServer));
        }

        if(getState()==INTERRUPTED)
            return;

        // Send mail body
        try {
            sendBody();
        }
        catch(IOException e) {
            showErrorDialog(Translator.get("email.connection_closed"));
        }
    }


    /**
     * Method overridden to close connection to the mail server.
     */
    protected void jobStopped() {
        super.jobStopped();

        // Close the connection
        closeConnection();
    }
}
