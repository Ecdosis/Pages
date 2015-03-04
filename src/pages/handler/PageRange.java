/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pages.handler;

/**
 * A page range is an offset and length within plain text
 * @author desmond
 */
public class PageRange {
    /** offset into document bytes */
    int offset;
    /** length of page text in bytes */
    int length;
    /**
     * Create a page range
     * @param offset offset within  the base plain text
     * @param length length of the page in textual characters
     */
    public PageRange( int offset, int length )
    {
        this.offset = offset;
        this.length = length;
    }
    /**
     * Get the end offset (one index beyond last char)
     * @return the index of the end of the range
     */
    public int end()
    {
        return this.offset+this.length;
    }
}
