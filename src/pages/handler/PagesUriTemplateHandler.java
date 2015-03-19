/*
 * This file is part of BHLPages.
 *
 *  BHLPages is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  BHLPages is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with BHLPages.  If not, see <http://www.gnu.org/licenses/>.
 *  (c) copyright Biodiversity Heritage Library 2015 
 *  http://www.biodiversitylibrary.org/
 */


package pages.handler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import pages.exception.MissingDocumentException;
import java.io.IOException;


/**
 * Get the template of an image URI in accordance with RFC6570 variables 
 * docid and pageid are recognised if required
 * {language} a one-word human readable language name e.g. "english"
 * {docid} the work identifier with no leading slash, e.g. "english/harpur/h080"
 * {pageid} page number or other minimal identifier e.g. 143a - no suffix!
 * @author desmond
 */
public class PagesUriTemplateHandler extends PagesGetHandler
{
    public void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) 
        throws MissingDocumentException
    {
        try
        {
            // URI_TYPE may be passed to get the "document" URI template
            // otherwise the usual version template will be returned
            String path = getUriTemplate( request );
            response.setContentType("text/plain");
            response.getWriter().print( path );
        }
        catch ( IOException ioe )
        {
            throw new MissingDocumentException(ioe);
        }
    }
    public static String getUriTemplate( HttpServletRequest request )
    {
        String host = request.getServerName();
        int port = request.getServerPort();
        StringBuilder sb = new StringBuilder();
        sb.append("http://");
        sb.append(host);
        if (port!=80 )
        {
            sb.append(":");
            sb.append(port);
        }
        sb.append("corpix/{docid}/{pageid}");
        return sb.toString();
    }
}
