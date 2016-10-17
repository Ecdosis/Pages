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

import calliope.core.handler.EcdosisVersion;
import calliope.core.database.Connector;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import pages.constants.Database;
import pages.constants.Params;
import pages.constants.Formats;
import pages.constants.JSONKeys;
import pages.exception.MissingDocumentException;
import pages.exception.PagesException;
import pages.Utils;
import calliope.AeseFormatter;
import calliope.core.exception.CalliopeException;
import calliope.core.URLEncoder;
import org.json.simple.*;
import calliope.json.JSONResponse;
import pages.exception.NativeException;
/**
 * Return a HTML rendering of the text of just one page
 * @author desmond
 */
public class PagesHtmlHandler extends PagesGetHandler {
    /**
     * Get the document body of the given urn or null
     * @param db the database where it is
     * @param docID the docID of the resource
     * @return the document body or null if not present
     */
    private static String getDocumentBody( String db, String docID ) 
        throws CalliopeException
    {
        try
        {
            String jStr = Connector.getConnection().getFromDb(db,docID);
            if ( jStr != null )
            {
                JSONObject jDoc = (JSONObject)JSONValue.parse( jStr );
                if ( jDoc != null )
                {
                    Object body = jDoc.get( JSONKeys.BODY );
                    if ( body != null )
                        return body.toString();
                }
            }
            throw new CalliopeException("document "+db+"/"+docID+" not found");
        }
        catch ( Exception e )
        {
            throw new CalliopeException( e );
        }
    }
    /**
     * Fetch a single style text
     * @param style the path to the style in the corform database
     * @return the text of the style
     */
    public static String fetchStyle( String style ) throws CalliopeException
    {
        // 1. try to get each literal style name
        String actual = getDocumentBody(Database.CORFORM,style);
        while ( actual == null )
        {
            // 2. add "default" to the end
            actual = getDocumentBody( Database.CORFORM,
                URLEncoder.append(style,Formats.DEFAULT) );
            if ( actual == null )
            {
                // 3. pop off last path component and try again
                if ( style.length()>0 )
                    style = Utils.chomp(style);
                else
                    throw new CalliopeException("no suitable format");
            }
        }
        return actual;
    }
    public void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) throws PagesException
    {
        try
        {
            String docid = request.getParameter(Params.DOCID);
            String pageid = request.getParameter(Params.PAGEID);
            String vPath = (String)request.getParameter(Params.VERSION1);
            if ( docid !=null )
            {
                EcdosisVersion version = doGetResourceVersion( Database.CORTEX, 
                    docid, vPath );
                char[] text = version.getVersion();
                // just get the basic corcode
                EcdosisVersion corcode = doGetResourceVersion( Database.CORCODE,
                    docid+"/default", vPath );
                PageRange pr = getPageRange( docid, pageid, vPath );
                // format the text using the default corcode
                String[] corCodes = new String[1];
                corCodes[0] = corcode.getVersionString();
                JSONObject defaultCC = (JSONObject)JSONValue.parse(corCodes[0]);
                String[] styles = new String[1];
                String styleName = (String)defaultCC.get(JSONKeys.STYLE);
                if ( styleName == null )
                    styleName = "TEI/default";
                styles[0] = fetchStyle( styleName );
                String[] formats = new String[1];
                formats[0] = "STIL";
                JSONResponse html = new JSONResponse(JSONResponse.HTML);
                /*String text, String[] markup, String[] css, 
                JSONResponse output*/
                int res = new AeseFormatter().format( 
                    new String(text), corCodes, styles, html );
                if ( res == 0 )
                    throw new NativeException("html formatting failed");
                else
                {
                    HTMLSelector hs = new HTMLSelector( html.getBody() );
                    response.setContentType("text/plain");
                    response.getWriter().println(hs.getPage(pr));
                }
            }
            else
                throw new Exception("Missing doicd or not found="+docid);
        }
        catch ( Exception e )
        {
            throw new MissingDocumentException(e);
        }
    }
}
