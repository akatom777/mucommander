
package com.mucommander.job;

import com.mucommander.ui.ProgressDialog;
import com.mucommander.ui.MainFrame;

import com.mucommander.file.AbstractFile;

import com.mucommander.text.Translator;

import java.io.*;


/**
 * ExtendedFileJob is a container for a file task : basically an operation that involves files and bytes.<br>
 * <p>What makes it different from FileJob is that the class implementing ExtendedFileJob has to be able to give
 * information about the file currently being processed.</p>
 * 
 * @author Maxence Bernard
 */
public abstract class ExtendedFileJob extends FileJob {

    /** Number of bytes of current file that have been processed, see {@link #getCurrentFileBytesProcessed() getCurrentFileBytesProcessed} */
    protected long currentFileProcessed;

	/** Read buffer */
	protected byte buffer[];
	
	/** Size that should be allocated to read buffer */
	protected final static int READ_BLOCK_SIZE = 8192;

	/** True if current file is at the top of the source/dest folder */
	protected boolean topLevelFile;
	
	
	/**
	 * Creates a new ExtendedFileJob.
	 */
    public ExtendedFileJob(ProgressDialog progressDialog, MainFrame mainFrame, java.util.Vector files) {
        super(progressDialog, mainFrame, files);
    }


	/**
	 * Copies the given InputStream's content to the given OutputStream, skipping the specified number
	 * of bytes (can be 0) from the InputStream.
	 */
	protected void copyStream(InputStream in, OutputStream out, long skipBytes) throws IOException {
		// Init read buffer the first time
		if(buffer==null)
			buffer = new byte[READ_BLOCK_SIZE];
		
		// Skip/do not read a number of bytes from the input stream
		if(skipBytes>0) {
			in.skip(skipBytes);
			currentFileProcessed += skipBytes;
//			nbBytesProcessed += skipBytes;
//			nbBytesSkipped += skipBytes;
		}
		
		// Copies the InputStream's content to the OutputStream
		int read;
		while ((read=in.read(buffer, 0, buffer.length))!=-1 && !isInterrupted()) {
//System.out.println("copyStream1: read="+read);
			out.write(buffer, 0, read);
//System.out.println("copyStream2: read="+read);
			nbBytesProcessed += read;
			currentFileProcessed += read;
		}
//System.out.println("copyStream: OVER");
	}
	
	
	/**
	 * Copies the given source file to the specified destination file, optionally resuming the operation.
	 */
	protected void copyFile(AbstractFile sourceFile, AbstractFile destFile, boolean append) throws FileJobException {
		OutputStream out = null;
		InputStream in = null;

		try {
			// Try to open InputStream
			try  { in = sourceFile.getInputStream(); }
			catch(IOException e1) {
				throw new FileJobException(FileJobException.CANNOT_OPEN_SOURCE);
			}
	
			// Try to open OutputStream
			try  { out = destFile.getOutputStream(append); }
			catch(IOException e2) {
				throw new FileJobException(FileJobException.CANNOT_OPEN_DESTINATION);
			}
	
			// Try to copy InputStream to OutputStream
			try  { copyStream(in, out, append?destFile.getSize():0); }
			catch(IOException e3) {
				throw new FileJobException(FileJobException.ERROR_WHILE_TRANSFERRING);
			}
		}
		catch(FileJobException e) {
			// Rethrow exception 
			throw e;
		}
		finally {
			// Tries to close the streams no matter what happened before
			// This block will always be executed, even if an exception
			// was thrown by the catch block
			// Finally found a use for the finally block!
			if(in!=null)
				try { in.close(); }
				catch(IOException e1) {}
			if(out!=null)
				try { out.close(); }
				catch(IOException e2) {}
		}
		
	}


	/**
	 * Tries to copy the given source file to the specified destination file (see {@link #copyFile(AbstractFile, AbstractFile, boolean} copyFile()}
	 * displaying a generic error dialog {@link #showErrorDialog(String, String) #showErrorDialog()} if something went wrong, 
	 * and giving the user to skip the file, retry or cancel.
	 */
	protected boolean tryCopyFile(AbstractFile sourceFile, AbstractFile destFile, boolean append, String errorDialogTitle) {
		// Copy file to destination
		do {				// Loop for retry
			try {
				copyFile(sourceFile, destFile, append);
				return true;
			}
			catch(FileJobException e) {
				// Copy failed
				if(com.mucommander.Debug.ON)
					System.out.println(""+e);
				
				int reason = e.getReason();
				String errorMsg;
				switch(reason) {
					// Could not open source file for read
					case FileJobException.CANNOT_OPEN_SOURCE:
						errorMsg = Translator.get("cannot_read_source", sourceFile.getName());
						break;
					// Could not open destination file for write
					case FileJobException.CANNOT_OPEN_DESTINATION:
						errorMsg = Translator.get("cannot_write_destination", sourceFile.getName());
						break;
					// An error occurred during file transfer
					case FileJobException.ERROR_WHILE_TRANSFERRING:
					default:
						errorMsg = Translator.get("error_while_transferring", sourceFile.getName());
						break;
				}
				
				// Ask the user what to do
				int ret = showErrorDialog(errorDialogTitle, errorMsg);
				// Retry action
				if(ret==RETRY_ACTION) {
					// Resume transfer
					if(reason==FileJobException.ERROR_WHILE_TRANSFERRING)
						append = true;
					continue;
				}
				// cancel action or close dialog
				else if(ret==-1 || ret==CANCEL_ACTION) {
					stop();
				}
				// skip, cancel or close return false
				return false;
			}
		} while(true);
	}
	
	
    /**
     * Computes and returns the percent done of current file. Returns 0 if current file's size is not available
     * (getNbCurrentFileBytesProcessed() returns -1).
     */
    public int getFilePercentDone() {
        long currentFileSize = getCurrentFileSize();
        if(currentFileSize<=0)
            return 0;
        else
            return (int)(100*getCurrentFileBytesProcessed()/(float)currentFileSize);

    }

    
    /**
     * Returns the number of bytes of the current file that have been processed.
     */
    public long getCurrentFileBytesProcessed() {
		return currentFileProcessed;
	}


    /**
     * Returns current file's size, -1 if is not available.
     */
    public long getCurrentFileSize() {
		return currentFile==null?-1:currentFile.getSize();
	}

	
	/**
	 * Advances file index and resets file bytes counter. This method should be called by subclasses whenever the job
	 * starts processing a new file.
	 */
	protected void nextFile(AbstractFile file) {
		super.nextFile(file);
		currentFileProcessed = 0;
		topLevelFile = file.getParent().equals(baseSourceFolder);
	}


    /**
     * Overrides this method to returns a more accurate percent value of the job processed so far, taking
     * into account current file's percent.
     */
    public int getTotalPercentDone() {
        float nbFilesProcessed = getCurrentFileIndex();
		
		if(currentFile!=null && topLevelFile && !currentFile.isDirectory()) {
			long currentFileSize = currentFile.getSize();
			if(currentFileSize>0)
				nbFilesProcessed += getCurrentFileBytesProcessed()/(float)currentFileSize;
		}
			
        return (int)(100*(nbFilesProcessed/(float)getNbFiles()));
    }
    
}