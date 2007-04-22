package com.mucommander.ant.util;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;

import java.io.File;

/**
 * Task used to compute the size of a fileset.
 * @author Nicolas Rinaudo
 */
public class SizeTask extends Task {
    private String  propertyName;
    private FileSet files;

    public void SizeTask() {}

    public void init() {
        propertyName = null;
        files        = null;
    }

    public void addConfiguredFileSet(FileSet f) {files = f;}
    public void setName(String s) {propertyName = s;}

    public void execute() throws BuildException {
        DirectoryScanner scanner;
        String[] selectedFiles;
        int      size;

        // Makes sure the task has been properly initialised.
        if(propertyName == null)
            throw new BuildException("Unspecified name - please fill in the name attribute.");
        if(files == null)
            throw new BuildException("Unspecified fileset.");

        scanner = files.getDirectoryScanner(getProject());
        scanner.scan();
        selectedFiles = scanner.getIncludedFiles();
        size          = 0 ;

        for(int i = 0; i < selectedFiles.length; i++)
            size += new File(files.getDir(getProject()), selectedFiles[i]).length();

        getProject().setProperty(propertyName, Integer.toString(size / 1024));
    }
}