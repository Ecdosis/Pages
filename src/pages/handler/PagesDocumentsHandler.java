/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
 * Get a list of documents  with page information from the database
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
            String[] docids = conn.listCollection( Database.PAGES );
            StringBuilder sb = new StringBuilder();
            sb.append("[ ");
            for ( int i=0;i<docids.length;i++ )
            {
                sb.append("{ ");
                sb.append("\"docid\": \"");
                sb.append(docids[i]);
                sb.append("\", \"title\": \"");
                String doc = conn.getFromDb( Database.PAGES, docids[i] );
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
