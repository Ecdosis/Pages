/*
 * This file is part of TILT.
 *
 *  TILT is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  TILT is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with TILT.  If not, see <http://www.gnu.org/licenses/>.
 *  (c) copyright Desmond Schmidt 2015
 */

package pages.handler;
import org.htmlparser.Parser;
import org.htmlparser.util.ParserException;
import org.htmlparser.util.NodeList;
import org.htmlparser.Node;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.nodes.TagNode;
import pages.exception.HTMLException;
import java.util.HashSet;
/**
 * Select a range of HTML and make it into a valid document
 * @author desmond
 */
public class HTMLSelector 
{
    String src;
    /** same as src but as encoded bytes*/
    byte[] bytes;
    StringBuilder result;
    int offset;
    String encoding;
    HashSet<String> emptyTags;
    boolean finished;
    /**
     * Create a HTML selector
     * @param htmlDoc the HTML document
     * @param encoding its encoding
     */
    public HTMLSelector( String htmlDoc, String encoding )
    {
        this.src = htmlDoc;
        this.encoding = encoding;
        try
        {
            this.bytes = src.getBytes(encoding);
        }
        catch ( Exception e )
        {
            this.bytes = src.getBytes();
        }
        this.emptyTags = new HashSet<String>();
        this.emptyTags.add( "br");
        this.emptyTags.add("img");
        this.emptyTags.add("hr");
    }
    /**
     * Get the position of the right angle bracket given the start-bracket
     * @param from the position of the start bracket
     * @return the position in bytes of the right angle bracket of that tag
     */
    int rightAngleBracketPos( int from )
    {
        for ( int i=from;i<src.length();i++ )
            if ( src.charAt(i)=='>')
                return i+1;
        return src.length();
    }
    /**
     * Compose a start tag
     * @param n the node to make a start tag from
     * @return the start tag including attributes as in the source
     */
    String getStartTag( Node n )
    {
        int start = n.getStartPosition();
        int rbPos = rightAngleBracketPos(start);
        return this.src.substring(start,rbPos);
    }
    /**
     * Compose an end-tag
     * @param n the node
     * @return a string representing the end-tag for this node
     */
    String getEndTag( Node n )
    {
        return "</"+((TagNode)n).getRawTagName()+">";
    }
    /**
     * Write the start tags needed to keep this segment well-formed
     * @param n the node to start from
     */
    void writeStartTags( Node n )
    {
        if ( n.getParent()!= null )
            writeStartTags( n.getParent() );
        result.append( getStartTag(n) );
    }
    /**
     * Write out the end tags to get a balanced tree
     * @param n the node containing the last bit of text, or an ancestor
     */
    void writeEndTags( Node n )
    {
        if ( n != null )
        {
            result.append(getEndTag(n));
            writeEndTags( n.getParent() );
            this.finished = true;
        }
    }
    /**
     * Get the byte length of a string in a particular encoding
     * @param content the string to measure
     * @param encoding its encoding
     * @return its length in bytes
     */
    int byteLength( String content, String encoding )
    {
        try
        {
            return content.getBytes(encoding).length;
        }
        catch ( Exception e )
        {
            return content.getBytes().length;
        }
    }
    /**
     * Get a segment of a string given a byte range
     * @param content the original string
     * @param start the byte offset in content
     * @param length the byte length of content needed
     * @param encoding the string encoding into bytes
     * @return the string segment based on its byte length
     */
    private String byteSegment( String content, int start, int length, 
        String encoding )
    {
        byte[] text;
        try
        {
            text = content.getBytes(encoding);
            byte[] tail = new byte[length];
            System.arraycopy(text,start,tail,0,length);
            return new String( tail, encoding ); 
        }
        catch ( Exception e )
        {
            //just assume default encoding
            text = content.getBytes();
            byte[] tail = new byte[length];
            System.arraycopy(text,start,tail,0,length);
            return new String( tail ); 
        }  
    }
    /**
     * Write out the last bit of text that overshoots the end
     * @param content the content of the overshooting node
     * @param pr the page-range 
     */
    private void writeOvershoot( String content, PageRange pr )
    {
        int contentLength = byteLength(content,pr.encoding);
        int overshoot = contentLength+offset-pr.end();
        int rest = contentLength-overshoot;
        String leading = byteSegment( content, 0, rest, pr.encoding );
        result.append( leading );
    }
    /**
     * Write out the first bit of text that undershoots the start
     * @param content the content of the undershooting node
     * @param pr the page range
     */
    private void writeUndershoot( String content, PageRange pr )
    {
        int contentLength = byteLength(content,pr.encoding);
        int undershoot = pr.offset-offset;
        String leading = byteSegment( content, undershoot, 
            contentLength-undershoot, pr.encoding );
        result.append( leading );
    }
    /**
     * Is the current node empty?
     * @param n the possibly empty node
     * @return true if it is br,hr etc.
     */
    private boolean isEmptyNode(TagNode n )
    {
        return emptyTags.contains(n.getRawTagName());
    }
    /**
     * Parse a tree node looking for start and end of the range
     * @param n the node to parse
     * @param pr the page-range in the underlying text
     */
    private void parseNode( Node n, PageRange pr )
    {
        if ( !this.finished )
        {
            // 1. n is a text-node, no children
            if ( n instanceof TextNode )
            {
                String content = n.getText();
                int contentLength = byteLength(content,pr.encoding);
                if ( contentLength+offset <= pr.offset )
                    offset += contentLength;
                else if ( result == null )
                {
                    result = new StringBuilder();
                    writeStartTags(n.getParent());
                }
                if ( result != null )
                {
                    if ( offset < pr.offset )
                    {
                        writeUndershoot( content, pr );
                        offset += contentLength;
                    }
                    else if ( contentLength+offset < pr.end() )
                    {
                        result.append(content);
                        offset += contentLength;
                    }
                    else if ( offset < pr.end() )
                    {
                        writeOvershoot(content,pr);
                        offset = pr.end();
                        if ( n.getParent() != null )
                            writeEndTags(n.getParent());
                    }
                }
            }
            // 2. n is an "element"
            else if ( n instanceof TagNode )
            {
                if ( offset >= pr.offset && offset < pr.end() )
                {
                    if ( result == null )
                        result = new StringBuilder();
                    if ( isEmptyNode((TagNode)n) )
                        result.append(getStartTag((TagNode)n));
                    else
                    {
                        result.append( getStartTag(n) );
                        if ( n.getFirstChild() != null )
                            parseNode( n.getFirstChild(), pr );
                        if ( !finished )
                            result.append( getEndTag(n) );
                    }
                }
                else if ( n.getFirstChild()!=null )
                    parseNode( n.getFirstChild(), pr );
            }
            // 3. process siblings of n
            if ( n.getNextSibling() != null )
                parseNode( n.getNextSibling(), pr );
        }
    }
    /**
     * Get part of the HTML's base text surrounded by valid HTML
     * @param pr the page range in the base text
     * @return a HTML document body fragment
     * @throws HTMLException
     */
    public String getPage( PageRange pr ) throws HTMLException
    {
        try
        {
            int offset = 0;
            Parser parser = new Parser( this.src, null );
            NodeList nl = parser.parse(null);
            for ( int i=0;i<nl.size();i++ )
                parseNode(nl.elementAt(i),pr);
            if ( result != null )
                return result.toString();
            else
                return "";
        }
        catch ( ParserException pe )
        {
            throw new HTMLException(pe);
        }
    }
}
