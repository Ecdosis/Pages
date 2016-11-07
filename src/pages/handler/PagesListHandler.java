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
import calliope.core.Utils;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import pages.constants.Database;
import pages.constants.JSONKeys;
import pages.constants.Params;
import pages.exception.PagesException;
import pages.exception.MissingDocumentException;
import java.awt.Dimension;
import pages.PagesWebApp;
import calliope.core.DocType;
import java.io.File;
import java.util.Arrays;

/**
 * Get a list of pages for a given document and version
 * @author desmond
 */
public class PagesListHandler extends PagesGetHandler
{
    String getFacs( File f )
    {
        String absPath = f.getAbsolutePath();
        String facs = Utils.subtractPaths( absPath, PagesWebApp.webRoot );
        System.out.println("seeking "+absPath);
        if ( !facs.startsWith("/") )
            return "/"+facs;
        else
            return facs;
    }
    int compareObjs( JSONObject a, JSONObject b )
    {
        if ( a.containsKey("src")&&b.containsKey("src"))
        {
            String srcA = (String)a.get("src");
            String srcB = (String)b.get("src");
            return srcA.compareTo(srcB);
        }
        else
            return 0;
    }
    /**
     * Sort a list of JSON object with src attributes
     * @param list 
     */
    void sortImageList( JSONArray list )
    {
        int increment = list.size() / 2;
	while (increment > 0) 
        {
            for (int i = increment; i < list.size(); i++) 
            {
                int j = i;
                JSONObject temp = (JSONObject)list.get(i);
                while (j >= increment 
                    && compareObjs((JSONObject)list.get(j-increment),temp)>0) 
                {
                    list.set(j,list.get(j-increment));
                    j = j - increment;
                }
                list.set(j,temp);
            }
            if (increment == 2) 
                increment = 1;
            else 
                increment *= (5.0 / 11);
        }
    }
    /**
     * Retrieve the pages for the given docid
     * @param docid the docid
     * @param vid the version id starting with slash
     * @return a JSON description of the pages of that docid
     * @throws PagesException 
     */
    String getPagesFromCorpix( String docid, String vid ) throws PagesException
    {
        int docType = DocType.classify(docid);
        String path = PagesWebApp.webRoot+"/corpix/"+docid+vid;
        File dir = new File(path);
        while ( dir != null && !dir.exists() && !dir.isDirectory() )
            dir = dir.getParentFile();
        System.out.println("searching dir "+dir.getName());
        if ( dir != null )
        {
            JSONArray list = new JSONArray();
            File[] files = dir.listFiles();
            for ( int i=0;i<files.length;i++ )
            {
                String fname = files[i].getName();
                if ( files[i].isFile() && DocType.matchFile(docid,fname,docType) )
                {
                     String n = DocType.getPageNo(fname,docType);
                     String facs = getFacs(files[i]);
                     String pageId = docid;
                     pageId += "/"+n;
                     Dimension d = getPageDimensions(pageId,facs);
                     if ( facs != null && n !=null )
                     {
                         PageDesc ps = new PageDesc(n,facs,d);
                         list.add( ps.toJSONObject() );
                     }
                }
            }
            //System.out.println("Found "+list.size()+" for "+docid);
            sortImageList( list );
            return list.toJSONString().replaceAll("\\\\/","/");
        }
        return "[]";
    }
    /**
     * Try to get version1 by examining the corpix directory
     * @param docid the document identfier
     * @return the first version in the corpix directory for that docid
     */
    String getVersion1FromCorpix( String docid )
    {
        // try to get it from corpix
        String path = PagesWebApp.webRoot+"/corpix/"+docid;
        File dir = new File(path);
        String version1 = "";
        if ( dir.exists() )
        {
            String[] files = dir.list();
            Arrays.sort(files);
            for ( int i=0;i<files.length;i++ )
            {
                if ( files[i].startsWith(dir.getName()) )
                {
                    version1 = "/"+files[i];
                    break;
                }
            }        
        }
        else
            version1 = "";
        return version1;
    }
    /**
     * Handle the http request
     * @param request the request object
     * @param response the response to write to
     * @param urn the residual urn after truncation to get here
     * @throws MissingDocumentException 
     */
    public void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) 
        throws MissingDocumentException
    {
        try
        {
            String docid = request.getParameter(Params.DOCID);
            String vPath = (String)request.getParameter(Params.VERSION1);
            if ( vPath == null )
                vPath = getVersion1FromCorpix(docid);
            String text ="[]";
            if ( docid != null )
            {
                EcdosisVersion pages = doGetResourceVersion( Database.CORCODE,
                    docid+"/pages", vPath);
                //System.out.println("docid="+docid+" vPath="+vPath);
                EcdosisVersion cortex = doGetResourceVersion( Database.CORTEX,
                    docid, vPath);
                if ( pages==null||pages.isEmpty()||cortex.isEmpty() )
                    text = getPagesFromCorpix(docid,stripLayer(vPath));
                else
                {
                    String stil = pages.getVersionString();
                    //if ( pages.getMVD()!= null )
                    //    System.out.println(pages.getMVD().getVersionTable());
                    JSONObject bson = (JSONObject)JSONValue.parse(stil);
                    if ( bson.containsKey(JSONKeys.RANGES) )
                    {
                        JSONArray ranges = (JSONArray)bson.get(JSONKeys.RANGES);
                        JSONArray list = new JSONArray();
                        int offset = 0;
                        for ( int i=0;i<ranges.size();i++ )
                        {
                            JSONObject range = (JSONObject)ranges.get(i);
                            // add absolute offset
                            offset += ((Number)range.get(JSONKeys.RELOFF)).intValue();
                            range.put("offset", offset);
                            // get actual page number from text
                            String n = getN(range,i,cortex).trim();
                            // the actual path to the image relative to webroot
                            String facs = getFacs(range,docid,vPath,n);
                            // the docid of the page metadata
                            String pageId = docid;
                            if ( vPath != null )
                                pageId += "/"+vPath;
                            pageId += "/"+n;
                            Dimension d = getPageDimensions(pageId,facs);
                            if ( facs != null && n !=null )
                            {
                                PageDesc ps = new PageDesc(n,facs,d);
                                list.add( ps.toJSONObject() );
                            }
                        }
                        text = list.toJSONString().replaceAll("\\\\/","/");
                    }
                    else
                        text = "[]";
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
