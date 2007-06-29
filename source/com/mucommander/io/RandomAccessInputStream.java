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

package com.mucommander.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * <code>RandomAccessInputStream</code> is an <code>InputStream</code> with random access.
 *
 * <b>Important:</b> <code>BufferedInputStream</code> or any wrapper <code>InputStream</code> class that uses a read buffer
 * CANNOT be used with a <code>RandomAccessInputStream</code> if the {@link #seek(long)} method is to be used. Doing so
 * would corrupt the read buffer and yield to data inconsistencies.
 *
 * @author Maxence Bernard
 */
public abstract class RandomAccessInputStream extends InputStream implements RandomAccess {

    /** The last offset set by {@link #mark(int)} */
    private int markOffset;


    /**
     * Creates a new RandomAccessInputStream.
     */
    public RandomAccessInputStream() {
    }


    ////////////////////////
    // Overridden methods //
    ////////////////////////

    /**
     * Skips (up to) the specified number of bytes and returns the number of bytes effectively skipped.
     * The exact given number of bytes will be skipped as long as the current offset as returned by {@link #getOffset()}
     * plus the number of bytes to skip doesn't exceed the length of this stream as returned by {@link #getLength()}.
     * If it does, all the remaining bytes will be skipped so that the offset of this stream will be positionned to
     * {@link #getLength()}.
     *
     * @param n number of bytes to skip
     * @return the number of bytes that have effectively been skipped
     * @throws IOException if something went wrong
     */
    public long skip(long n) throws IOException {
        long offset = getOffset();

        if(offset+n >= getLength()) {
            seek(getLength()-1);
            return getLength() - offset - 1;
        }

        seek(n);
        return n;
    }

    /**
     * Return the number of bytes that are available for reading, that is: {@link #getLength()} - {@link #getOffset()} - 1.
     * Since <code>InputStream.available()</code> returns an int and this method overrides it, a maximum of
     * <code>Integer.MAX_VALUE</code> can be returned, even if this stream has more bytes available.
     *
     * @return the number of bytes that are available for reading.
     * @throws IOException if something went wrong
     */
    public int available() throws IOException {
        return (int)(getLength() - getOffset() - 1);
    }

    /**
     * Overrides <code>InputStream.mark()</code> to provide a working implementation of the method. The given readLimit
     * is simply ignored, the stream can be repositionned using {@link #reset()} with no limit on the number of bytes
     * read after <code>mark()</code> has been called.
     *
     * @param readLimit this parameter has no effect and is simply ignored
     */
    public synchronized void mark(int readLimit) {
        this.markOffset = readLimit;
    }

    /**
     * Overrides <code>InputStream.mark()</code> to provide a working implementation of the method.
     *
     * @throws IOException if something went wrong
     */
    public synchronized void reset() throws IOException {
        seek(this.markOffset);
    }

    /**
     * Always returns <code>true</code>: {@link #mark(int)} and {@link #reset()} methods are supported.
     */
    public boolean markSupported() {
        return true;
    }


    //////////////////////
    // Abstract methods //
    //////////////////////

    /**
     * Reads up to <code>b.length</code> bytes of data from this file into an array of bytes. This method blocks until
     * at least one byte of input is available.
     *
     * @param b the buffer into which the data is read
     * @return the total number of bytes read into the buffer, or -1 if there is no more data because the end of this
     * file has been reached.
     * @throws IOException if an I/O error occurs
     */
    public abstract int read(byte b[]) throws IOException;

    /**
     * Reads up to <code>len</code> bytes of data from this file into an array of bytes. This method blocks until at
     * least one byte of input is available.
     *
     * @param b the buffer into which the data is read
     * @param off the start offset of the data
     * @param len the maximum number of bytes read
     * @return the total number of bytes read into the buffer, or -1 if there is no more data because the end of the
     * file has been reached.
     * @throws IOException if an I/O error occurs
     */
    public abstract int read(byte b[], int off, int len) throws IOException;
}
