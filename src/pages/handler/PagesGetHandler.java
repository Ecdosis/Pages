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
import pages.constants.Service;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import pages.exception.PagesException;
import pages.Utils;
import calliope.core.handler.GetHandler;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import pages.constants.Database;
import pages.constants.JSONKeys;
import java.io.File;
import java.io.FileInputStream;

/**
 * Handle a GET request for various image types, text, GeoJSON
 * @author desmond
 */
public class PagesGetHandler extends GetHandler {
    /**
     * Get the facs attribute for the given range
     * @param range the range
     * @return its FACS (facsimile) value or null if absent
     */
    protected String getFacs( JSONObject range )
    {
        JSONArray annotations = (JSONArray)range.get(JSONKeys.ANNOTATIONS);
        if ( annotations != null )
        {
            for ( int j=0;j<annotations.size();j++ )
            {
                JSONObject jobj = (JSONObject)annotations.get(j);
                if ( jobj.containsKey(JSONKeys.FACS) )
                    return (String)jobj.get(JSONKeys.FACS);
            }
        }
        // shouldn't happen
        return null;
    }
    /**
     * Get a page range as an offset and length of the underlying text
     * @param docid the document containing the specified page
     * @param pageid the id of the page or null
     * @param vPath the version to look for or null
     * @param encoding the encoding of the data the page range points to
     * @return a page range object
     */
    public PageRange getPageRange( String docid, String pageid, String vPath, 
        String encoding ) throws PagesException
    {
        try
        {
            AeseVersion pages = doGetResourceVersion( Database.CORCODE,
                docid+"/pages", vPath);
            String stil = pages.getVersionString();
            JSONObject bson = (JSONObject)JSONValue.parse(stil);
            if ( bson.containsKey(JSONKeys.RANGES) )
            {
                JSONArray ranges = (JSONArray)bson.get(JSONKeys.RANGES);
                if ( ranges.size()==0 )
                    throw new Exception("no ranges for document");
                JSONObject range = (JSONObject)ranges.get(0);
                Number reloff = (Number)range.get(JSONKeys.RELOFF);
                int offset=0;
                int pageLen = 0;
                if ( reloff != null )
                {
                    offset = reloff.intValue();
                    Number len = (Number)range.get(JSONKeys.LEN);
                    if ( len !=null )
                        pageLen = len.intValue();
                }
                else
                    throw new Exception("Missing reloff attribute on range");
                if ( pageid != null )
                {
                    for ( int i=0;i<ranges.size();i++ )
                    {
                        range = (JSONObject)ranges.get(i);
                        String id = imageId( getFacs(range) );
                        Number len = (Number)range.get(JSONKeys.LEN);
                        if ( len == null )
                            throw new Exception("missing length of range!");
                        else
                            pageLen = len.intValue();
                        if ( pageid.equals(id) )
                            break;
                        else
                            offset += len.intValue();
                    }
                }
                return new PageRange( offset, pageLen, encoding );
            }
            else
                throw new Exception("Missing ranges in pages resource");
        }
        catch ( Exception e )
        {
            throw new PagesException(e);
        }
    }
    /**
     * Extract the image Id from the full image path
     * @parampath a relative image path and file name
     */
    protected String imageId( String path )
    {
        int slash = path.lastIndexOf("/");
        if( slash != -1 )
            path = path.substring(slash+1);
        int dot = path.lastIndexOf(".");
        if( dot != -1 )
            path = path.substring(0,dot);
        return path;
    }
    /**
     * Handle a request for one of the data types supported here
     * @param request the http request
     * @param response the http response
     * @param urn the remainder of the request urn to be processed
     * @throws PagesException 
     */
    public void handle(HttpServletRequest request,
        HttpServletResponse response, String urn) throws PagesException {
        try {
            String service = Utils.first(urn);
            if (service.equals(Service.IMAGE)) 
                new PagesImageHandler().handle(request, response, Utils.pop(urn));
            else if (service.equals(Service.DOCUMENTS) )
                new PagesDocumentsHandler().handle(request,response,Utils.pop(urn));
            else if (service.equals(Service.TEXT) )
                new PagesTextHandler().handle(request,response,Utils.pop(urn));
            else if (service.equals(Service.HTML) )
                new PagesHtmlHandler().handle(request,response,Utils.pop(urn));
            else if (service.equals(Service.LIST) )
                new PagesListHandler().handle(request,response,Utils.pop(urn));
            else
                    throw new Exception("Unknown service "+service);
        } catch (Exception e) {
            throw new PagesException(e);
        }
    }
}