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

package com.mucommander.res;

import com.mucommander.xml.writer.*;

import java.io.*;

/**
 * Class used to write resource list files.
 * @author Nicolas Rinaudo
 */
public class ResourceListWriter implements XmlConstants {
    // - Instance fields -------------------------------------------------
    // -------------------------------------------------------------------
    /** Where to write the content of the resource list to. */
    private XmlWriter out;



    // - Initialisation --------------------------------------------------
    // -------------------------------------------------------------------
    /**
     * Opens a new resource list writer on the specified output stream.
     * @param out where to write the resource list.
     */
    public ResourceListWriter(OutputStream out) {this.out = new XmlWriter(out);}

    /**
     * Opens a new resource list writer on the specified file.
     * @param file where to write the resource list.
     */
    public ResourceListWriter(File file) throws IOException {out = new XmlWriter(file);}



    // - XML output ------------------------------------------------------
    // -------------------------------------------------------------------
    /**
     * Starts the list.
     */
    public void startList() {
        out.startElement(ROOT_ELEMENT);
        out.println();
    }

    /**
     * Adds the specified path to the list.
     * @param path path to add to the list.
     */
    public void addFile(String path) {
        XmlAttributes attributes;

        attributes = new XmlAttributes();
        attributes.add(PATH_ATTRIBUTE, path);
        out.writeStandAloneElement(FILE_ELEMENT, attributes);
    }

    /**
     * Ends the list.
     */
    public void endList() {
        out.endElement(ROOT_ELEMENT);
    }

    /**
     * Closes the stream used to write the list.
     */
    public void close() throws IOException {out.close();}
}
