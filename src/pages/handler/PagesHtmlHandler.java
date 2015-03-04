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
 *  (c) copyright Desmond Schmidt 2014
 */


package pages.handler;

import calliope.core.handler.AeseVersion;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import pages.constants.Database;
import pages.constants.Params;
import pages.constants.JSONKeys;
import pages.exception.MissingDocumentException;
import pages.exception.PagesException;
import calliope.AeseFormatter;
import org.json.simple.*;
import calliope.json.JSONResponse;
import pages.exception.NativeException;
/**
 * Return a HTML rendering of the text of just one page
 * @author desmond
 */
public class PagesHtmlHandler extends PagesGetHandler {
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
                AeseVersion version = doGetResourceVersion( Database.CORTEX, 
                    docid, vPath );
                byte[] text = version.getVersion();
                // just get the basic corcode
                AeseVersion corcode = doGetResourceVersion( Database.CORCODE,
                    docid+"/default", vPath );
                PageRange pr = getPageRange( docid, pageid, vPath );
                // format the text using the default corcode
                String[] corCodes = new String[1];
                corCodes[0] = corcode.getVersionString();
                JSONObject defaultCC = (JSONObject)JSONValue.parse(corCodes[0]);
                String[] styles = new String[1];
                styles[0] = (String)defaultCC.get(JSONKeys.FORMAT);
                String[] formats = new String[1];
                formats[0] = "STIL";
                JSONResponse html = new JSONResponse(JSONResponse.HTML);
                int res = new AeseFormatter().format( 
                    text, corCodes, styles, formats, html );
                if ( res == 0 )
                    throw new NativeException("html formatting failed");
                else
                {
                    HTMLSelector hs = new HTMLSelector( html.getBody() );
                    response.setContentType("text/plain;charset="
                        +version.getEncoding());
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
