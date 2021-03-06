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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import pages.constants.Params;
import pages.exception.PagesException;

/**
 * Get the path of an image given its docid and image id
 * @author desmond
 */
public class PagesImageHandler extends PagesGetHandler {
    public void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) throws PagesException
    {
        try
        {
            String docid = request.getParameter(Params.DOCID);
            String pageid = request.getParameter(Params.PAGEID);
            if ( docid != null && pageid!= null )
            {
                String path = PagesUriTemplateHandler.getUriTemplate(request);
                path = path.replace("{docid}",docid);
                // pageid can include doc version,e.g. h080j/123 
                path = path.replace("{pageid}",pageid);
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().println(path);
            }
            else
                throw new Exception("missing docid or pageid");
        }
        catch ( Exception e )
        {
            throw new PagesException(e);
        }
    }
}
