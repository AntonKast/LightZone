/*
 * @(#)CharacterIteratorWrapper.java	1.4 05/11/17
 *
 * Portions Copyright 2006 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
 *******************************************************************************
 * (C) Copyright IBM Corp. 1996-2005 - All Rights Reserved                     *
 *                                                                             *
 * The original version of this source code and documentation is copyrighted   *
 * and owned by IBM, These materials are provided under terms of a License     *
 * Agreement between IBM and Sun. This technology is protected by multiple     *
 * US and International patents. This notice and attribution to IBM may not    *
 * to removed.                                                                 *
 *******************************************************************************
 */

package jdk1_6.sun.text.normalizer;

import java.text.CharacterIterator;

/**
 * This class is a wrapper around CharacterIterator and implements the
 * UCharacterIterator protocol
 * @author ram
 */

public class CharacterIteratorWrapper extends UCharacterIterator {

    private CharacterIterator iterator;

    public CharacterIteratorWrapper(CharacterIterator iter){
        if(iter==null){
            throw new IllegalArgumentException();
        }
        iterator     = iter;
    }

    /**
     * @see UCharacterIterator#current()
     */
    public int current() {
        int c = iterator.current();
        if(c==CharacterIterator.DONE){
          return DONE;
        }
        return c;
    }

    /**
     * @see UCharacterIterator#getLength()
     */
    public int getLength() {
        return (iterator.getEndIndex() - iterator.getBeginIndex());
    }

    /**
     * @see UCharacterIterator#getIndex()
     */
    public int getIndex() {
        return iterator.getIndex();
    }

    /**
     * @see UCharacterIterator#next()
     */
    public int next() {
        int i = iterator.current();
        iterator.next();
        if(i==CharacterIterator.DONE){
          return DONE;
        }
        return i;
    }

    /**
     * @see UCharacterIterator#previous()
     */
    public int previous() {
        int i = iterator.previous();
        if(i==CharacterIterator.DONE){
            return DONE;
        }
        return i;
    }

    /**
     * @see UCharacterIterator#setIndex(int)
     */
    public void setIndex(int index) {
        iterator.setIndex(index);
    }

    //// for StringPrep
    /**
     * @see UCharacterIterator#getText(char[])
     */
    public int getText(char[] fillIn, int offset){
        int length =iterator.getEndIndex() - iterator.getBeginIndex();
        int currentIndex = iterator.getIndex();
        if(offset < 0 || offset + length > fillIn.length){
            throw new IndexOutOfBoundsException(Integer.toString(length));
        }

        for (char ch = iterator.first(); ch != CharacterIterator.DONE; ch = iterator.next()) {
            fillIn[offset++] = ch;
        }
        iterator.setIndex(currentIndex);

        return length;
    }

    /**
     * Creates a clone of this iterator.  Clones the underlying character iterator.
     * @see UCharacterIterator#clone()
     */
    public Object clone(){
        try {
            CharacterIteratorWrapper result = (CharacterIteratorWrapper) super.clone();
            result.iterator = (CharacterIterator)this.iterator.clone();
            return result;
        } catch (CloneNotSupportedException e) {
            return null; // only invoked if bad underlying character iterator
        }
    }
}
