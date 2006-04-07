
package com.mucommander.file;

import java.util.zip.GZIPInputStream;
import java.io.*;


/**
 * 
 *
 * @author Maxence Bernard
 */
public class GzipEntryFile extends AbstractEntryFile {

	/**
	 * Creates a GzipEntryFile around the given file.
	 */
	public GzipEntryFile(GzipArchiveFile archiveFile) {
		super(archiveFile);
		this.parent = archiveFile;
	}


	/////////////////////////////////////////
	// AbstractFile methods implementation //
	/////////////////////////////////////////
	
	public long getDate() {
		return archiveFile.getDate();
	}
	
	public boolean changeDate(long lastModified) {
		// Entries are read-only
		return false;
	}

	public long getSize() {
		return -1;
	}
	
	public boolean isDirectory() {
		return false;
	}

	public AbstractFile[] ls() throws IOException {
		return new AbstractFile[0];
	}

	public String getName() {
		String extension = archiveFile.getExtension();
		String name = archiveFile.getName();
		
		if(extension==null)
			return name;
		
		extension = extension.toLowerCase();
		
		if(extension.equals("tgz"))
			return name.substring(0, name.length()-3)+"tar";

		if(extension.equals("gz"))
			return name.substring(0, name.length()-3);
			
		return name;
	}
	
	public InputStream getInputStream() throws IOException {
		return new GZIPInputStream(archiveFile.getInputStream());
	}
}