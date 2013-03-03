/*
 * $RCSfile: DataBufferUtils.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:00 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.codecimpl.util;

import java.awt.image.DataBuffer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Utility class to enable compatibility with Java 2 1.4 real-valued
 * DataBuffer classes. Factory methods and data array accessors are
 * defined to use reflection. The core Java classes are given precedence
 * over their JAI equivalents.
 */
public final class DataBufferUtils {

    /**
     * Priority ordered array of DataBufferFloat class names.
     */
    private static final String[] FLOAT_CLASS_NAMES = {
        "java.awt.image.DataBufferFloat",
        "com.lightcrafts.mediax.jai.DataBufferFloat",
        "com.lightcrafts.media.jai.codecimpl.util.DataBufferFloat"
    };

    /**
     * Priority ordered array of DataBufferDouble class names.
     */
    private static final String[] DOUBLE_CLASS_NAMES = {
        "java.awt.image.DataBufferDouble",
        "com.lightcrafts.mediax.jai.DataBufferDouble",
        "com.lightcrafts.media.jai.codecimpl.util.DataBufferDouble"
    };

    /**
     * Classes to be used for DB float and double.
     */
    private static Class floatClass = null;
    private static Class doubleClass = null;

    /**
     * Initialize float and double DB classes.
     */
    static {
        floatClass = getDataBufferClass(DataBuffer.TYPE_FLOAT);
        doubleClass = getDataBufferClass(DataBuffer.TYPE_DOUBLE);
    }

    /**
     * Return the class for the specified data type.
     *
     * @param dataType The data type from among
     * <code>DataBuffer.TYPE_*</code>.
     */
    private static final Class getDataBufferClass(int dataType) {
        // Set the array of class names.
        String[] classNames = null;
        switch(dataType) {
        case DataBuffer.TYPE_FLOAT:
            classNames = FLOAT_CLASS_NAMES;
            break;
        case DataBuffer.TYPE_DOUBLE:
            classNames = DOUBLE_CLASS_NAMES;
            break;
        default:
            throw new IllegalArgumentException("dataType == "+dataType+"!");
        }

        // Initialize the return value.
        Class dataBufferClass = null;

        // Loop over the class names array in priority order.
        for(int i = 0; i < classNames.length; i++) {
            try {
                // Attempt to get the class.
                dataBufferClass = Class.forName(classNames[i]);

                // Break if the class was found.
                if(dataBufferClass != null) {
                    break;
                }
            } catch(ClassNotFoundException e) {
                // Ignore the exception.
            }
        }

        // Throw an exception if no class was found.
        if(dataBufferClass == null) {
            throw new RuntimeException
                (JaiI18N.getString("DataBufferUtils0")+" "+
                 (String)(dataType == DataBuffer.TYPE_FLOAT ?
                          "DataBufferFloat" : "DataBufferDouble"));
        }

        return dataBufferClass;
    }

    /**
     * Construct a <code>DataBuffer</code> of the requested type
     * using the parameters with the specified types and values.
     */
    private static final DataBuffer constructDataBuffer(int dataType,
                                                        Class[] paramTypes,
                                                        Object[] paramValues) {

        Class dbClass = null;
        switch(dataType) {
        case DataBuffer.TYPE_FLOAT:
            dbClass = floatClass;
            break;
        case DataBuffer.TYPE_DOUBLE:
            dbClass = doubleClass;
            break;
        default:
            throw new IllegalArgumentException("dataType == "+dataType+"!");
        }

        DataBuffer dataBuffer = null;
        try {
            Constructor constructor = dbClass.getConstructor(paramTypes);
            dataBuffer = (DataBuffer)constructor.newInstance(paramValues);
        } catch(Exception e) {
            throw new RuntimeException(JaiI18N.getString("DataBufferUtils1"));
        }

        return dataBuffer;
    }

    /**
     * Invoke the <code>DataBuffer</code> method of the specified name
     * using the parameters with the specified types and values.
     */
    private static final Object invokeDataBufferMethod(DataBuffer dataBuffer,
                                                       String methodName,
                                                       Class[] paramTypes,
                                                       Object[] paramValues) {
        if(dataBuffer == null) {
            throw new IllegalArgumentException("dataBuffer == null!");
        }

        Class dbClass = dataBuffer.getClass();

        Object returnValue = null;
        try {
            Method method = dbClass.getMethod(methodName, paramTypes);
            returnValue = method.invoke(dataBuffer, paramValues);
        } catch(Exception e) {
            throw new RuntimeException(JaiI18N.getString("DataBufferUtils2")+
                                       " \""+methodName+"\".");
        }

        return returnValue;
    }

    public static final DataBuffer createDataBufferFloat(float[][] dataArray,
                                                         int size) {
        return constructDataBuffer(DataBuffer.TYPE_FLOAT,
                                   new Class[] {float[][].class,
                                                int.class},
                                   new Object[] {dataArray,
                                                 new Integer(size)});
    }

    public static final DataBuffer createDataBufferFloat(float[][] dataArray,
                                                         int size,
                                                         int[] offsets) {
        return constructDataBuffer(DataBuffer.TYPE_FLOAT,
                                   new Class[] {float[][].class,
                                                int.class,
                                                int[].class},
                                   new Object[] {dataArray,
                                                 new Integer(size),
                                                 offsets});
    }

    public static final DataBuffer createDataBufferFloat(float[] dataArray,
                                                         int size) {
        return constructDataBuffer(DataBuffer.TYPE_FLOAT,
                                   new Class[] {float[].class,
                                                int.class},
                                   new Object[] {dataArray,
                                                 new Integer(size)});
    }

    public static final DataBuffer createDataBufferFloat(float[] dataArray,
                                                         int size,
                                                         int offset) {
        return constructDataBuffer(DataBuffer.TYPE_FLOAT,
                                   new Class[] {float[].class,
                                                int.class,
                                                int.class},
                                   new Object[] {dataArray,
                                                 new Integer(size),
                                                 new Integer(offset)});
    }

    public static final DataBuffer createDataBufferFloat(int size) {
        return constructDataBuffer(DataBuffer.TYPE_FLOAT,
                                   new Class[] {int.class},
                                   new Object[] {new Integer(size)});
    }

    public static final DataBuffer createDataBufferFloat(int size,
                                                         int numBanks) {
        return constructDataBuffer(DataBuffer.TYPE_FLOAT,
                                   new Class[] {int.class,
                                                int.class},
                                   new Object[] {new Integer(size),
                                                 new Integer(numBanks)});
    }

    public static final float[][] getBankDataFloat(DataBuffer dataBuffer) {
        return (float[][])invokeDataBufferMethod(dataBuffer,
                                                 "getBankData",
                                                 null,
                                                 null);
    }

    public static final float[] getDataFloat(DataBuffer dataBuffer) {
        return (float[])invokeDataBufferMethod(dataBuffer,
                                               "getData",
                                               null,
                                               null);
    }

    public static final float[] getDataFloat(DataBuffer dataBuffer,
                                             int bank) {
        return (float[])invokeDataBufferMethod(dataBuffer,
                                               "getData",
                                               new Class[] {int.class},
                                               new Object[] {new Integer(bank)});
    }

    public static final DataBuffer createDataBufferDouble(double[][] dataArray,
                                                          int size) {
        return constructDataBuffer(DataBuffer.TYPE_DOUBLE,
                                   new Class[] {double[][].class,
                                                int.class},
                                   new Object[] {dataArray,
                                                 new Integer(size)});
    }

    public static final DataBuffer createDataBufferDouble(double[][] dataArray,
                                                          int size,
                                                          int[] offsets) {
        return constructDataBuffer(DataBuffer.TYPE_DOUBLE,
                                   new Class[] {double[][].class,
                                                int.class,
                                                int[].class},
                                   new Object[] {dataArray,
                                                 new Integer(size),
                                                 offsets});
    }

    public static final DataBuffer createDataBufferDouble(double[] dataArray,
                                                          int size) {
        return constructDataBuffer(DataBuffer.TYPE_DOUBLE,
                                   new Class[] {double[].class,
                                                int.class},
                                   new Object[] {dataArray,
                                                 new Integer(size)});
    }

    public static final DataBuffer createDataBufferDouble(double[] dataArray,
                                                          int size,
                                                          int offset) {
        return constructDataBuffer(DataBuffer.TYPE_DOUBLE,
                                   new Class[] {double[].class,
                                                int.class,
                                                int.class},
                                   new Object[] {dataArray,
                                                 new Integer(size),
                                                 new Integer(offset)});
    }

    public static final DataBuffer createDataBufferDouble(int size) {
        return constructDataBuffer(DataBuffer.TYPE_DOUBLE,
                                   new Class[] {int.class},
                                   new Object[] {new Integer(size)});
    }

    public static final DataBuffer createDataBufferDouble(int size,
                                                          int numBanks) {
        return constructDataBuffer(DataBuffer.TYPE_DOUBLE,
                                   new Class[] {int.class,
                                                int.class},
                                   new Object[] {new Integer(size),
                                                 new Integer(numBanks)});
    }

    public static final double[][] getBankDataDouble(DataBuffer dataBuffer) {
        return (double[][])invokeDataBufferMethod(dataBuffer,
                                                 "getBankData",
                                                  null,
                                                  null);
    }

    public static final double[] getDataDouble(DataBuffer dataBuffer) {
        return (double[])invokeDataBufferMethod(dataBuffer,
                                               "getData",
                                                null,
                                                null);
    }

    public static final double[] getDataDouble(DataBuffer dataBuffer,
                                               int bank) {
        return (double[])invokeDataBufferMethod(dataBuffer,
                                               "getData",
                                                new Class[] {int.class},
                                                new Object[] {new Integer(bank)});
    }
}
