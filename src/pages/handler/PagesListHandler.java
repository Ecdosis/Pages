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
                EcdosisVersion pages = doGetResourceVersion( Database.CORCODE,
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
                        String facs = imageId(getFacs(range));
                        String n = getN(range,i);
                        if ( facs != null && n !=null )
                        {
                            PageDesc ps = new PageDesc(n,facs);
                            list.add( ps.toJSONObject() );
                        }
                    }
                    text = list.toJSONString();
                }
                response.setContentType("application/json;charset=UTF-8");
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
