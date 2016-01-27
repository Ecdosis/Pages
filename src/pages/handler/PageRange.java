/*
 * This file is part of Pages.
 *
 *  Pages is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  Pages is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Pages.  If not, see <http://www.gnu.org/licenses/>.
 *  (c) copyright Desmond Schmidt 2015
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
