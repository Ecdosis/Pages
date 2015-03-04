/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pages.handler;

import calliope.core.handler.AeseVersion;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import pages.constants.Database;
import pages.constants.JSONKeys;
import pages.constants.Params;
import pages.exception.MissingDocumentException;

/**
 * Get a list of pages for a given document and version
 * @author desmond
 */
public class PagesListHandler extends PagesGetHandler
{
    public void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) 
        throws MissingDocumentException
    {
        try
        {
            String docid = request.getParameter(Params.DOCID);
            String vPath = (String)request.getParameter(Params.VERSION1);
            String text ="[]";
            if ( docid != null )
            {
                AeseVersion pages = doGetResourceVersion( Database.CORCODE,
                    docid+"/pages", vPath);
                if ( pages== null)
                    throw new Exception("Couldn't find document "+docid);
                String stil = pages.getVersionString();
                JSONObject bson = (JSONObject)JSONValue.parse(stil);
                if ( bson.containsKey(JSONKeys.RANGES) )
                {
                    JSONArray ranges = (JSONArray)bson.get(JSONKeys.RANGES);
                    JSONArray list = new JSONArray();
                    for ( int i=0;i<ranges.size();i++ )
                    {
                        JSONObject range = (JSONObject)ranges.get(i);
                        String facs = getFacs( range );
                        if ( facs != null )
                            list.add( imageId(facs) );
                    }
                    text = list.toJSONString();
                }
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().println(text);
            }
            else throw new Exception("Must specify document identifier");
        }
        catch ( Exception e )
        {
            throw new MissingDocumentException(e);
        }
    }
            
}
