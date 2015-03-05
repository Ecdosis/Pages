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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import calliope.core.handler.AeseVersion;
import pages.exception.MissingDocumentException;
import pages.constants.Params;
import pages.constants.Database;

/**
 * Get the text of a page
 * @author desmond
 */
public class PagesTextHandler extends PagesGetHandler {
    /**
     * Get the plain text of a page. Assume parameters version1, pageid 
     * (image file name minus extension) and docid. Assume also that there 
     * is a corcode at docid/pages. (Should already have checked for this)
     * @param request the original http request
     * @param response the response
     * @param urn the urn used for the request
     * @throws MissingDocumentException 
     */
    public void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) 
        throws MissingDocumentException
    {
        try
        {
            String docid = request.getParameter(Params.DOCID);
            String pageid = request.getParameter(Params.PAGEID);
            String vPath = (String)request.getParameter(Params.VERSION1);
            if ( docid != null )
            {
                // pageid and vPath can be null
                AeseVersion version = doGetResourceVersion( Database.CORTEX, 
                    docid, vPath );
                PageRange pr = getPageRange( docid, pageid, vPath, 
                    version.getEncoding() );
                byte[] text = version.getVersion();
                byte[] chunk = new byte[pr.length];
                System.arraycopy(text, pr.offset, chunk, 0, pr.length);
                String str = new String( chunk, version.getEncoding() );
                response.setContentType("text/plain;charset="
                    +version.getEncoding());
                response.getWriter().println(str);
            }
            else
                throw new Exception("Missing docid or not found="+docid);
        }
        catch ( Exception e )
        {
            throw new MissingDocumentException(e);
        }
    }
}
