/*
 * $RCSfile: TIFFDirectory.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.3 $
 * $Date: 2005/09/19 21:52:04 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.codec;
import java.io.IOException;
import java.io.EOFException;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * A class representing an Image File Directory (IFD) from a TIFF 6.0
 * stream.  The TIFF file format is described in more detail in the
 * comments for the TIFFDescriptor class.
 *
 * <p> A TIFF IFD consists of a set of TIFFField tags.  Methods are
 * provided to query the set of tags and to obtain the raw field
 * array.  In addition, convenience methods are provided for acquiring
 * the values of tags that contain a single value that fits into a
 * byte, int, long, float, or double.
 *
 * <p> Every TIFF file is made up of one or more public IFDs that are
 * joined in a linked list, rooted in the file header.  A file may
 * also contain so-called private IFDs that are referenced from
 * tag data and do not appear in the main list.
 *
 * <p><b> This class is not a committed part of the JAI API.  It may
 * be removed or changed in future releases of JAI.</b>
 *
 * @see com.lightcrafts.mediax.jai.operator.TIFFDescriptor
 * @see TIFFField
 */
public class TIFFDirectory extends Object implements Serializable {

    /** A boolean storing the endianness of the stream. */
    boolean isBigEndian;
    
    /** The number of entries in the IFD. */
    int numEntries;

    /** An array of TIFFFields. */
    TIFFField[] fields;

    /** A Hashtable indexing the fields by tag number. */
    Hashtable fieldIndex = new Hashtable();

    /** The offset of this IFD. */
    long IFDOffset = 8;

    /** The offset of the next IFD. */
    long nextIFDOffset = 0;

    /** The default constructor. */
    TIFFDirectory() {}

    private static boolean isValidEndianTag(int endian) {
        return ((endian == 0x4949) || (endian == 0x4d4d));
    }

    /**
     * Constructs a TIFFDirectory from a SeekableStream.
     * The directory parameter specifies which directory to read from
     * the linked list present in the stream; directory 0 is normally
     * read but it is possible to store multiple images in a single
     * TIFF file by maintaing multiple directories.
     *
     * @param stream a SeekableStream to read from.
     * @param directory the index of the directory to read.
     */
    public TIFFDirectory(SeekableStream stream, int directory)
        throws IOException {

        long global_save_offset = stream.getFilePointer();
        long ifd_offset;

        // Read the TIFF header
        stream.seek(0L);
        int endian = stream.readUnsignedShort();
        if (!isValidEndianTag(endian)) {
            throw new 
		IllegalArgumentException(JaiI18N.getString("TIFFDirectory1"));
        }
        isBigEndian = (endian == 0x4d4d);

        int magic = readUnsignedShort(stream);
        if (magic != 42) {
            throw new 
		IllegalArgumentException(JaiI18N.getString("TIFFDirectory2"));
        }

        // Get the initial ifd offset as an unsigned int (using a long)
        ifd_offset = readUnsignedInt(stream);
        
        for (int i = 0; i < directory; i++) {
            if (ifd_offset == 0L) {
                throw new 
		   IllegalArgumentException(JaiI18N.getString("TIFFDirectory3"));
            }
            
            stream.seek(ifd_offset);
            int entries = readUnsignedShort(stream);
            stream.skip(12*entries);

            ifd_offset = readUnsignedInt(stream);
        }

        stream.seek(ifd_offset);
        initialize(stream);
        stream.seek(global_save_offset);
    }

    /**
     * Constructs a TIFFDirectory by reading a SeekableStream.
     * The ifd_offset parameter specifies the stream offset from which
     * to begin reading; this mechanism is sometimes used to store
     * private IFDs within a TIFF file that are not part of the normal
     * sequence of IFDs.
     *
     * @param stream a SeekableStream to read from.
     * @param ifd_offset the long byte offset of the directory.
     * @param directory the index of the directory to read beyond the
     *        one at the current stream offset; zero indicates the IFD
     *        at the current offset.
     */
    public TIFFDirectory(SeekableStream stream, long ifd_offset, int directory)
        throws IOException {

        long global_save_offset = stream.getFilePointer();
        stream.seek(0L);
        int endian = stream.readUnsignedShort();
        if (!isValidEndianTag(endian)) {
            throw new 
		IllegalArgumentException(JaiI18N.getString("TIFFDirectory1"));
        }
        isBigEndian = (endian == 0x4d4d);

        // Seek to the first IFD.
        stream.seek(ifd_offset);

        // Seek to desired IFD if necessary.
        int dirNum = 0;
        while(dirNum < directory) {
            // Get the number of fields in the current IFD.
            int numEntries = readUnsignedShort(stream);

            // Skip to the next IFD offset value field.
            stream.seek(ifd_offset + 12*numEntries);

            // Read the offset to the next IFD beyond this one.
            ifd_offset = readUnsignedInt(stream);

            // Seek to the next IFD.
            stream.seek(ifd_offset);

            // Increment the directory.
            dirNum++;
        }

        initialize(stream);
        stream.seek(global_save_offset);
    }

    private static final int[] sizeOfType = {
        0, //  0 = n/a
        1, //  1 = byte
        1, //  2 = ascii
        2, //  3 = short
        4, //  4 = long
        8, //  5 = rational
        1, //  6 = sbyte
        1, //  7 = undefined
        2, //  8 = sshort
        4, //  9 = slong
        8, // 10 = srational
        4, // 11 = float
        8  // 12 = double 
    };

    private void initialize(SeekableStream stream) throws IOException {
        long nextTagOffset;
        int i, j;

        IFDOffset = stream.getFilePointer();

        numEntries = readUnsignedShort(stream);
        fields = new TIFFField[numEntries];
        
        for (i = 0; i < numEntries; i++) {
            int tag = readUnsignedShort(stream);
            int type = readUnsignedShort(stream);
            int count = (int)(readUnsignedInt(stream));
            int value = 0;
	    
            // The place to return to to read the next tag
            nextTagOffset = stream.getFilePointer() + 4;

	    try {
		// If the tag data can't fit in 4 bytes, the next 4 bytes
		// contain the starting offset of the data
		if (count*sizeOfType[type] > 4) {
		    value = (int)(readUnsignedInt(stream));
		    stream.seek(value);
		}
	    } catch (ArrayIndexOutOfBoundsException ae) {

		System.err.println(tag + " " + 
				   JaiI18N.getString("TIFFDirectory4"));
		// if the data type is unknown we should skip this TIFF Field
		stream.seek(nextTagOffset);
		continue;
	    }

            fieldIndex.put(new Integer(tag), new Integer(i));
            Object obj = null;

            try {
                switch (type) {
                case TIFFField.TIFF_BYTE:
                case TIFFField.TIFF_SBYTE:
                case TIFFField.TIFF_UNDEFINED:
                case TIFFField.TIFF_ASCII:
                    byte[] bvalues = new byte[count];
                    stream.readFully(bvalues, 0, count);

                    if (type == TIFFField.TIFF_ASCII) {

                        // Can be multiple strings
                        int index = 0, prevIndex = 0;
                        Vector v = new Vector();

                        while (index < count) {
			
                            while ((index < count) && (bvalues[index++] != 0));

                            // When we encountered zero, means one string has ended
                            v.add(new String(bvalues, prevIndex, 
                                             (index - prevIndex)) );
                            prevIndex = index;
                        }

                        count = v.size();
                        String strings[] = new String[count];
                        for (int c = 0 ; c < count; c++) {
                            strings[c] = (String)v.elementAt(c);
                        }

                        obj = strings;
                    } else {
                        obj = bvalues;
                    }

                    break;

                case TIFFField.TIFF_SHORT:
                    char[] cvalues = new char[count];
                    for (j = 0; j < count; j++) {
                        cvalues[j] = (char)(readUnsignedShort(stream));
                    }
                    obj = cvalues;
                    break;
                
                case TIFFField.TIFF_LONG:
                    long[] lvalues = new long[count];
                    for (j = 0; j < count; j++) {
                        lvalues[j] = readUnsignedInt(stream);
                    }
                    obj = lvalues;
                    break;
                
                case TIFFField.TIFF_RATIONAL:
                    long[][] llvalues = new long[count][2];
                    for (j = 0; j < count; j++) {
                        llvalues[j][0] = readUnsignedInt(stream);
                        llvalues[j][1] = readUnsignedInt(stream);
                    }
                    obj = llvalues;
                    break;
                
                case TIFFField.TIFF_SSHORT:
                    short[] svalues = new short[count];
                    for (j = 0; j < count; j++) {
                        svalues[j] = readShort(stream);
                    }
                    obj = svalues;
                    break;
                
                case TIFFField.TIFF_SLONG:
                    int[] ivalues = new int[count];
                    for (j = 0; j < count; j++) {
                        ivalues[j] = readInt(stream);
                    }
                    obj = ivalues;
                    break;
                
                case TIFFField.TIFF_SRATIONAL:
                    int[][] iivalues = new int[count][2];
                    for (j = 0; j < count; j++) {
                        iivalues[j][0] = readInt(stream);
                        iivalues[j][1] = readInt(stream);
                    }
                    obj = iivalues;
                    break;

                case TIFFField.TIFF_FLOAT:
                    float[] fvalues = new float[count];
                    for (j = 0; j < count; j++) {
                        fvalues[j] = readFloat(stream);
                    }
                    obj = fvalues;
                    break;

                case TIFFField.TIFF_DOUBLE:
                    double[] dvalues = new double[count];
                    for (j = 0; j < count; j++) {
                        dvalues[j] = readDouble(stream);
                    }
                    obj = dvalues;
                    break;

                default:
                    System.err.println(JaiI18N.getString("TIFFDirectory0"));
                    break;
                }

                fields[i] = new TIFFField(tag, type, count, obj);
            } catch(EOFException eofe) {
                // The TIFF 6.0 fields have tag numbers less than or equal
                // to 532 (ReferenceBlackWhite) or equal to 33432 (Copyright).
                // If there is an error reading a baseline tag, then re-throw
                // the exception and fail; otherwise continue with the next
                // field.
                if(tag <= 532 || tag == 33432) {
                    throw eofe;
                }

                fieldIndex.remove(new Integer(tag));
                // XXX Warning message
            }

            stream.seek(nextTagOffset);
        }

        // Read the offset of the next IFD.
        nextIFDOffset = readUnsignedInt(stream);
    }

    /** Returns the number of directory entries. */
    public int getNumEntries() {
        return numEntries;
    }

    /**
     * Returns the value of a given tag as a <code>TIFFField</code>,
     * or <code>null</code> if the tag is not present.
     *
     * <p>Note that the value (data) of the <code>TIFFField</code>
     * will always be the actual field value regardless of the number of
     * bytes required for that value.  This is the case despite the fact
     * that the TIFF <i>IFD Entry</i> corresponding to the field may
     * actually contain the offset to the field's value rather than
     * the value itself (the latter occurring if and only if the
     * value fits into 4 bytes).  In other words, the value of the
     * field will already have been read from the TIFF stream.</p>
     */
    public TIFFField getField(int tag) {
        Integer i = (Integer)fieldIndex.get(new Integer(tag));
        if (i == null) {
            return null;
        } else {
            return fields[i.intValue()];
        }
    }

    /**
     * Returns true if a tag appears in the directory. 
     */
    public boolean isTagPresent(int tag) {
        return fieldIndex.containsKey(new Integer(tag));
    }

    /**
     * Returns an ordered array of ints indicating the tag
     * values.
     */
    public int[] getTags() {
        int[] tags = new int[fieldIndex.size()];
        Enumeration enumeration = fieldIndex.keys();
        int i = 0;

        while (enumeration.hasMoreElements()) {
            tags[i++] = ((Integer)enumeration.nextElement()).intValue();
        }

        return tags;
    }

    /**
     * Returns an array of TIFFFields containing all the fields
     * in this directory.
     */
    public TIFFField[] getFields() {
        return fields;
    }

    /**
     * Returns the value of a particular index of a given tag as a
     * byte.  The caller is responsible for ensuring that the tag is
     * present and has type TIFFField.TIFF_SBYTE, TIFF_BYTE, or
     * TIFF_UNDEFINED.
     */
    public byte getFieldAsByte(int tag, int index) {
        Integer i = (Integer)fieldIndex.get(new Integer(tag));
        byte [] b = (fields[i.intValue()]).getAsBytes();
        return b[index];
    }

    /**
     * Returns the value of index 0 of a given tag as a
     * byte.  The caller is responsible for ensuring that the tag is
     * present and has  type TIFFField.TIFF_SBYTE, TIFF_BYTE, or
     * TIFF_UNDEFINED.
     */
    public byte getFieldAsByte(int tag) {
        return getFieldAsByte(tag, 0);
    }

    /**
     * Returns the value of a particular index of a given tag as a
     * long.  The caller is responsible for ensuring that the tag is
     * present and has type TIFF_BYTE, TIFF_SBYTE, TIFF_UNDEFINED,
     * TIFF_SHORT, TIFF_SSHORT, TIFF_SLONG or TIFF_LONG.
     */
    public long getFieldAsLong(int tag, int index) {
        Integer i = (Integer)fieldIndex.get(new Integer(tag));
        return (fields[i.intValue()]).getAsLong(index);
    }

    /**
     * Returns the value of index 0 of a given tag as a
     * long.  The caller is responsible for ensuring that the tag is
     * present and has type TIFF_BYTE, TIFF_SBYTE, TIFF_UNDEFINED,
     * TIFF_SHORT, TIFF_SSHORT, TIFF_SLONG or TIFF_LONG.
     */
    public long getFieldAsLong(int tag) {
        return getFieldAsLong(tag, 0);
    }

    /**
     * Returns the value of a particular index of a given tag as a
     * float.  The caller is responsible for ensuring that the tag is
     * present and has numeric type (all but TIFF_UNDEFINED and
     * TIFF_ASCII).
     */
    public float getFieldAsFloat(int tag, int index) {
        Integer i = (Integer)fieldIndex.get(new Integer(tag));
        return fields[i.intValue()].getAsFloat(index);
    }

    /**
     * Returns the value of index 0 of a given tag as a float.  The
     * caller is responsible for ensuring that the tag is present and
     * has numeric type (all but TIFF_UNDEFINED and TIFF_ASCII).
     */
    public float getFieldAsFloat(int tag) {
        return getFieldAsFloat(tag, 0);
    }

    /**
     * Returns the value of a particular index of a given tag as a
     * double.  The caller is responsible for ensuring that the tag is
     * present and has numeric type (all but TIFF_UNDEFINED and
     * TIFF_ASCII).
     */
    public double getFieldAsDouble(int tag, int index) {
        Integer i = (Integer)fieldIndex.get(new Integer(tag));
        return fields[i.intValue()].getAsDouble(index);
    }

    /**
     * Returns the value of index 0 of a given tag as a double.  The
     * caller is responsible for ensuring that the tag is present and
     * has numeric type (all but TIFF_UNDEFINED and TIFF_ASCII).
     */
    public double getFieldAsDouble(int tag) {
        return getFieldAsDouble(tag, 0);
    }

    // Methods to read primitive data types from the stream

    private short readShort(SeekableStream stream)
        throws IOException {
        if (isBigEndian) {
            return stream.readShort();
        } else {
            return stream.readShortLE();
        }
    }

    private int readUnsignedShort(SeekableStream stream)
        throws IOException {
        if (isBigEndian) {
            return stream.readUnsignedShort();
        } else {
            return stream.readUnsignedShortLE();
        }
    }

    private int readInt(SeekableStream stream) 
        throws IOException {
        if (isBigEndian) {
            return stream.readInt();
        } else {
            return stream.readIntLE();
        }
    }

    private long readUnsignedInt(SeekableStream stream) 
        throws IOException {
        if (isBigEndian) {
            return stream.readUnsignedInt();
        } else {
            return stream.readUnsignedIntLE();
        }
    }

    private long readLong(SeekableStream stream)
        throws IOException {
        if (isBigEndian) {
            return stream.readLong();
        } else {
            return stream.readLongLE();
        }
    }

    private float readFloat(SeekableStream stream)
        throws IOException {
        if (isBigEndian) {
            return stream.readFloat();
        } else {
            return stream.readFloatLE();
        }
    }

    private double readDouble(SeekableStream stream)
        throws IOException {
        if (isBigEndian) {
            return stream.readDouble();
        } else {
            return stream.readDoubleLE();
        }
    }

    private static int readUnsignedShort(SeekableStream stream,
                                         boolean isBigEndian)
        throws IOException {
        if (isBigEndian) {
            return stream.readUnsignedShort();
        } else {
            return stream.readUnsignedShortLE();
        }
    }

    private static long readUnsignedInt(SeekableStream stream,
                                        boolean isBigEndian) 
        throws IOException {
        if (isBigEndian) {
            return stream.readUnsignedInt();
        } else {
            return stream.readUnsignedIntLE();
        }
    }

    // Utilities

    /**
     * Returns the number of image directories (subimages) stored in a
     * given TIFF file, represented by a <code>SeekableStream</code>.
     */
    public static int getNumDirectories(SeekableStream stream)
        throws IOException{
        long pointer = stream.getFilePointer(); // Save stream pointer

        stream.seek(0L);
        int endian = stream.readUnsignedShort();
        if (!isValidEndianTag(endian)) {
            throw new 
		IllegalArgumentException(JaiI18N.getString("TIFFDirectory1"));
        }
        boolean isBigEndian = (endian == 0x4d4d);
        int magic = readUnsignedShort(stream, isBigEndian);
        if (magic != 42) {
            throw new 
		IllegalArgumentException(JaiI18N.getString("TIFFDirectory2"));
        }
        
        stream.seek(4L);
        long offset = readUnsignedInt(stream, isBigEndian);

        int numDirectories = 0;
        while (offset != 0L) {
            ++numDirectories;

            // EOFException means IFD was probably not properly terminated.
            try {
                stream.seek(offset);
                int entries = readUnsignedShort(stream, isBigEndian);
                stream.skip(12*entries);
                offset = readUnsignedInt(stream, isBigEndian);
            } catch(EOFException eof) {
                numDirectories--;
                break;
            }
        }
      
        stream.seek(pointer); // Reset stream pointer
        return numDirectories;
    }

    /**
     * Returns a boolean indicating whether the byte order used in the
     * the TIFF file is big-endian (i.e. whether the byte order is from  
     * the most significant to the least significant)
     */
    public boolean isBigEndian() {
	return isBigEndian;
    }

    /**
     * Returns the offset of the IFD corresponding to this
     * <code>TIFFDirectory</code>.
     */
    public long getIFDOffset() {
        return IFDOffset;
    }

    /**
     * Returns the offset of the next IFD after the IFD corresponding to this
     * <code>TIFFDirectory</code>.
     */
    public long getNextIFDOffset() {
        return nextIFDOffset;
    }
}
