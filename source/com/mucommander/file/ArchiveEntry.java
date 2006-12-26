
package com.mucommander.file;

/**
 * This abstract class encapsulates 3rd-party archive entries.
 *
 * @author Maxence Bernard 
 */
abstract class ArchiveEntry {
	
    /** Underlying entry object */
    protected Object entry;

    /** Depth of this entry based on the number of '/' character occurrences */
    private int depth = -1;
	
	
    /**
     * Creates a new ArchiveEntry that encapsulates the given 3rd party entry.
     */
    ArchiveEntry(Object entry) {
        this.entry = entry;
    }
		
    /**
     * Returns the encapsulated entry.
     */
    Object getEntry() {
        return entry;
    }


    /**
     * Returns the depth of this entry based on the number of slash character ('/') occurrences its path contains.
     * Minimum depth is 0.
     */
    int getDepth() {
        // Depth is only calculated once as it never changes (this class is immutable)
        if(depth == -1) {
            depth = 0;
            int pos=0;
            String path = getPath();

            while ((pos=path.indexOf('/', pos+1))!=-1)
                depth++;
			
            // Directories in archives end with a '/'
            if(path.charAt(path.length()-1)=='/')
                depth--;
        }

        return depth;	
    }


    /**
     * Extracts this entry's filename from its path and returns it.
     *
     * @return this entry's filename
     */
    public String getName() {
        String path = getPath();
        int len = path.length();
        // Remove trailing '/' if any
        if(path.charAt(len-1)=='/')
            path = path.substring(0, --len);

        int lastSlash = path.lastIndexOf('/');
        return lastSlash==-1?
          path:
          path.substring(lastSlash+1, len);
    }


    //////////////////////
    // Abstract methods //
    //////////////////////
		
    /**
     * Returns the encapsulated entry's path.
     */
    abstract String getPath();
	
    /**
     * Returns the encapsulated entry's date.
     */
    abstract long getDate();

    /**
     * Returns the encapsulated entry's size.
     */
    abstract long getSize();

    /**
     * Returns <code>true</code> if the encapsulated entry is a directory.
     */
    abstract boolean isDirectory();

    /**
     * Returns read/write/execute permissions as an int, UNIX octal style.
     * The value can be compared against {@link AbstractFile#READ_MASK}, {@link AbstractFile#WRITE_MASK} and
     * {@link AbstractFile#EXECUTE_MASK} bit masks to determine if the entry is readable/writable/executable.
     */
    abstract int getPermissions();
}
