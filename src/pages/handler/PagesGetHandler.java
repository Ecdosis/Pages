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

import calliope.core.database.Connector;
import calliope.core.database.Connection;
import calliope.core.handler.EcdosisVersion;
import java.io.File;
import pages.constants.Service;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import pages.exception.PagesException;
import pages.Utils;
import calliope.core.handler.GetHandler;
import calliope.core.image.MimeType;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import pages.constants.Database;
import pages.constants.JSONKeys;
import java.awt.Dimension;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import pages.PagesWebApp;

/**
 * Handle a GET request for various image types, text, GeoJSON
 * @author desmond
 */
public class PagesGetHandler extends GetHandler {
    /**
     * Remove the layer spec from the vid
     * @param vid the full version path
     * @return a version path devoid of the layer-N component
     */
    String stripLayer( String vid )
    {
        int index = vid.lastIndexOf("/layer");
        if ( index != -1 )
            return vid.substring(0,index);
        else
            return vid;
    }
    /**
     * Get the facs attribute for the given range
     * @param range the range
     * @param docid the docid of the document
     * @param version1 the version id
     * @param n the page number
     * @return the path to the facsimile or null if absent
     */
    protected String getFacs( JSONObject range, String docid, String version1, 
        String n ) throws PagesException
    {
        System.out.println("range="+range.toJSONString()+"; docid="+docid+"; version1="+version1+"; n="+n);
        // look for facs attribute on range (rare)
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
        // didn't find facs attribute - manufacture it
        try
        {
            String id = docid;
            if ( version1 != null )
            {
                String vid = stripLayer(version1);
                if ( vid.length()>0 )
                {
                    if (!vid.startsWith("/") )
                        id += "/" + vid;
                    else
                        id += vid;
                }
            }
            String path = PagesWebApp.webRoot+"/corpix/"+id;
            System.out.println("Seeking image at path "+path);
            File dir = new File(path);
            File parent = dir.getParentFile();
            String fileId = "";
            if ( !dir.exists() )
            {
                dir = parent;
                int index = path.lastIndexOf("/");
                if ( index != -1 )
                    fileId = path.substring(index+1);
                else
                    fileId = path;
            }
            if ( dir.exists() )
            {
                try{
                    File[] files = dir.listFiles();
                    for ( int i=0;i<files.length;i++ )
                    {
                        FileName fn = new FileName(files[i]);
                        if ( fn.n==null )
                            System.out.println("fn.n was null");
                        else if ( fn.id==null )
                            System.out.println("fn.id was null");
                        else if ( fn.n.equals(n) && fn.id.equals(fileId) )
                        {
                            String facsPath = files[i].getAbsolutePath();
                            if ( facsPath.startsWith(PagesWebApp.webRoot) )
                            {
                                return facsPath.substring(PagesWebApp.webRoot.length());
                            }
                        }
                    }
                }
                catch ( Exception e )
                {
                    System.out.println("Security exception on "+dir.getAbsolutePath());
                }
            }
            else
            {
                System.out.println("Couldn't locate "+dir.getAbsolutePath());
                throw new Exception("Failed to locate image "+id);
            }
        }
        catch ( Exception e )
        {
            throw new PagesException(e);
        }
        return null;
    }
    /**
     * Get the page "number" from the facs attribute or file name
     * @param facs the facs url or file name
     * @return an N-attribute or the empty string if not found
     */
    String getNFromFacs( String facs )
    {
        int li = facs.lastIndexOf("/");
        if ( li != -1 )
            facs = facs.substring(li+1);
        // so now we have a simple file-name
        // 1. test if it is a newspaper/letter file name
        if ( facs.contains("-") )
        {
            String[] parts = facs.split("-");
            for ( int i=0;i<parts.length;i++ )
            {
                if ( parts[i].startsWith("P") )
                    return parts[i].substring(1);
            }
        }
        else // it's just a page in a book
        {
            li = facs.lastIndexOf(".");
            if ( li != -1 )
                facs = facs.substring(0,li);
            // remove leading 0s
            for ( int i=0;i<facs.length();i++ )
                if ( facs.charAt(i)!= '0' )
                    return facs.substring(i);
        }
        // check for the return value!
        return "";
    }
    /**
     * Get the page number
     * @param range the page range as per STILformat,usually with "n"
     * @param index the expected page number-1, might be wrong
     * @param cortex use this is no n-annotation in range
     * @return the page number
     */
    protected String getN( JSONObject range, int index, EcdosisVersion cortex )
    {
        if ( range.containsKey(JSONKeys.N) )
            return (String)range.get(JSONKeys.N);
        else if ( range.containsKey(JSONKeys.FACS) )
        {
            String facs = (String)range.get(JSONKeys.FACS);
            String n = getNFromFacs(facs);
            if ( n.length()>0 )
                return n;
        }
        else // extract from cortex
        {
            String text = cortex.getVersionString();
            int offset = ((Number)range.get("offset")).intValue();
            int len = ((Number)range.get(JSONKeys.LEN)).intValue();
            if ( len > 0 )
                return text.substring(offset,offset+len);
        }
        // sensible default
        return new Integer(index+1).toString();
    }
    Dimension getPageDimensions( String docid, String facs ) throws PagesException
    {
        Dimension d = null;
        try
        {
            Connection conn = Connector.getConnection();
            String jStr = conn.getFromDb(Database.METADATA, docid);
            if ( jStr != null )
            {
                JSONObject jObj = (JSONObject)JSONValue.parse(jStr);
                if ( jObj.containsKey("width")&& jObj.containsKey("height") )
                {
                    int w = ((Number)jObj.get("width")).intValue();
                    int h = ((Number)jObj.get("height")).intValue();
                    d = new Dimension(w,h);
                }
            }
            if ( d == null )    // no luck
            {
                File src = new File(PagesWebApp.webRoot+facs);
                System.out.println(src.getAbsolutePath());
                if ( src.exists() )
                {
                    BufferedImage bi = ImageIO.read(src);
                    int w = bi.getWidth();
                    int h = bi.getHeight();
                    d = new Dimension(w,h);
                    // let's not do this again - save data
                    JSONObject jObj = new JSONObject();
                    jObj.put("mimetype", MimeType.getContentType(facs));
                    jObj.put("width", w);
                    jObj.put("height", h);
                    conn.putToDb(Database.METADATA, docid, jObj.toJSONString());
                }
                else
                    throw new Exception("Failed to find image "+facs);
            }
        }
        catch ( Exception e )
        {
            throw new PagesException(e);
        }
        return d;
    }
    /**
     * Get a page range as an offset and length of the underlying text
     * @param docid the document containing the specified page
     * @param pageid the id of the page or null
     * @param vPath the version to look for or null
     * @return a page range object
     */
    public PageRange getPageRange( String docid, String pageid, String vPath ) 
        throws PagesException
    {
        try
        {
            EcdosisVersion pages = doGetResourceVersion( Database.CORCODE,
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
                        String id = imageId( getFacs(range,docid,vPath,pageid) );
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
                return new PageRange( offset, pageLen );
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
        if ( path.contains("-") )
        {
            String[] parts = path.split("-");
            for ( int i=0;i<parts.length;i++ )
            {
                if ( parts[i].startsWith("P") )
                {
                    return parts[i].substring(1);
                }
            }
        }
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
            else if (service.equals(Service.LETTER) )
                new PagesLetterHandler().handle(request,response,Utils.pop(urn));
            else if (service.equals(Service.DOCUMENTS) )
                new PagesDocumentsHandler().handle(request,response,Utils.pop(urn));
            else if (service.equals(Service.TEXT) )
                new PagesTextHandler().handle(request,response,Utils.pop(urn));
            else if (service.equals(Service.HTML) )
                new PagesHtmlHandler().handle(request,response,Utils.pop(urn));
            else if (service.equals(Service.ANTHOLOGY) )
                new PagesAnthologyHandler().handle(request,response,Utils.pop(urn));
            else if (service.equals(Service.DIMENSIONS) )
                new PagesGetDimensions().handle(request,response,Utils.pop(urn));
            else if (service.equals(Service.LIST) )
                new PagesListHandler().handle(request,response,Utils.pop(urn));
            else if (service.equals(Service.URI_TEMPLATE) )
                new PagesUriTemplateHandler().handle(request,response,Utils.pop(urn));
            else
                    throw new Exception("Unknown service "+service);
        } catch (Exception e) {
            throw new PagesException(e);
        }
    }
}