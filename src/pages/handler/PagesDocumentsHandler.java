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

import calliope.core.database.Connection;
import calliope.core.database.Connector;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import pages.exception.PagesException;
import pages.constants.Database;
import org.json.simple.JSONValue;
import org.json.simple.JSONObject;
import pages.constants.Params;

/**
 * Get a list of documents with titles from the database
 * @author desmond
 */
public class PagesDocumentsHandler extends PagesGetHandler 
{
    public void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) throws PagesException
    {
        try
        {
            Connection conn = Connector.getConnection();
            String[] docids = conn.listCollection( Database.CORCODE );
            StringBuilder sb = new StringBuilder();
            sb.append("[ ");
            for ( int i=0;i<docids.length;i++ )
            {
                if ( docids[i].endsWith("/pages") )
                {
                    sb.append("{ ");
                    sb.append("\"docid\": \"");
                    sb.append(docids[i]);
                    sb.append("\", \"title\": \"");
                    String doc = conn.getFromDb( Database.CORCODE, docids[i] );
                    if ( doc != null )
                    {
                        JSONObject obj = (JSONObject)JSONValue.parse(doc);
                        if ( obj.containsKey(Params.TITLE) )
                            sb.append(obj.get(Params.TITLE));
                    }
                    sb.append("\"");
                    sb.append(" }");
                    if ( i<docids.length-1 )
                        sb.append(", ");
                }
            }
            sb.append(" ]");
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().println(sb.toString());
        }
        catch ( Exception e )
        {
            throw new PagesException(e);
        }
    }
}
