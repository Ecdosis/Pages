/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
import java.util.Vector;
/**
 * Select a range of HTML and make it into a valid document
 * @author desmond
 */
public class HTMLSelector 
{
    String src;
    StringBuilder result;
    int offset;
    HashSet<String> emptyTags;
    public HTMLSelector( String htmlDoc )
    {
        this.src = htmlDoc;
        this.emptyTags = new HashSet<String>();
        this.emptyTags.add( "br");
        this.emptyTags.add("img");
        this.emptyTags.add("hr");
    }
    int rightAngleBracketPos( int from )
    {
        for ( int i=from;i<src.length();i++ )
            if( src.charAt(i)=='>')
                return i+1;
        return src.length();
    }
    String getStartTag( Node n )
    {
        int start = n.getStartPosition();
        return src.substring(start,rightAngleBracketPos(start));
    }
    String getEndTag( Node n )
    {
        return "</"+((TagNode)n).getRawTagName()+">";
    }
    void writeStartTags( Node n )
    {
        if ( n.getParent()!= null )
        {
            writeStartTags( n.getParent() );
            result.append( getStartTag(n) );
        }
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
        }
    }
    /**
     * Write out the last bit of text that overshoots the end
     * @param content the content of the overshooting node
     * @param pr the page-range 
     */
    private void writeOvershoot( String content, PageRange pr )
    {
        int overshoot = content.length()+offset-pr.end();
        int rest = content.length()-overshoot;
        String leading = content.substring(0,rest);
        result.append( leading );
    }
    /**
     * Write out the first bit of text that undershoots the start
     * @param content the content of the undershooting node
     * @param pr the page range
     */
    private void writeUndershoot( String content, PageRange pr )
    {
        int undershoot = content.length()+offset-pr.offset;
        String leading = content.substring(undershoot);
        result.append( leading );
    }
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
        // 1. n is a text-node, no children
        if ( n instanceof TextNode )
        {
            String content = n.getText();
            if ( content.length()+offset <= pr.offset )
                offset += content.length();
            else if ( result == null )
            {
                result = new StringBuilder();
                writeStartTags(n);
            }
            if ( result != null )
            {
                if ( offset < pr.offset )
                {
                    writeUndershoot( content, pr );
                    offset +=content.length();
                }
                else if ( content.length()+offset < pr.end() )
                {
                    result.append(content);
                    offset += content.length();
                }
                else
                {
                    writeOvershoot(content,pr);
                    offset = pr.end();
                    writeEndTags(n);
                }
            }
        }
        // 2. n is an "element"
        else if ( n instanceof TagNode )
        {
            if ( offset >= pr.offset && offset < pr.end() )
            {
                if ( isEmptyNode((TagNode)n) )
                    result.append(getStartTag((TagNode)n));
                else
                {
                    result.append( getStartTag(n) );
                    if ( n.getFirstChild() != null )
                        parseNode( n.getFirstChild(), pr );
                    result.append( getEndTag(n) );
                }
            }
            else
                parseNode( n.getFirstChild(), pr );
        }
        // 3. process siblings of n
        if ( n.getNextSibling() != null )
            parseNode( n.getNextSibling(), pr );
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
            if ( nl.size()>0 )
                parseNode(nl.elementAt(0),pr);
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
