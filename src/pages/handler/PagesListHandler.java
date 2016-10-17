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
import java.util.HashMap;
import pages.PagesWebApp;
import pages.person.Table;
import java.io.File;

/**
 * Get a list of pages for a given document and version
 * @author desmond
 */
public class PagesListHandler extends PagesGetHandler
{
    static HashMap<String,String> months;
    static 
    {
        months = new HashMap<String,String>();
        months.put("JAN","January");
        months.put("FEB","February");
        months.put("MAR","March");
        months.put("APR","April");
        months.put("MAY","May");
        months.put("JUN","June");
        months.put("JUL","July");
        months.put("AUG","August");
        months.put("SEP","September");
        months.put("OCT","October");
        months.put("NOV","November");
        months.put("DEC","December");
    }
    public static boolean isDay( String day )
    {
        if ( isNumber(day) )
        {
            int value = Integer.parseInt(day);
            return value > 0 && value < 32;
        }
        else
            return false;
    }
    public static boolean isMonthName( String mon )
    {
        return months.containsKey( mon );
    }
    public static boolean isNumber(String num)
    {
        for ( int i=0;i<num.length();i++ )
        {
            if ( !Character.isDigit(num.charAt(i)) )
                return false;
        }
        return true;
    }
    public static boolean isYear( String year )
    {
        if ( isNumber(year) )
        {
            int value = Integer.parseInt(year);
            if ( value > 1800 && value < 2000 )
                return true;
            else
                return false;
        }
        else
            return false;
    }
    private boolean isPage( String page )
    {
        if ( page.startsWith("P") && page.length() > 1 )
        {
            String rest = page.substring(1);
            if ( isNumber(rest) || Utils.isLcRomanNumber(rest) )
                return true;
            else    // mixed: must be Arabic number then letters
            {
                StringBuilder sb = new StringBuilder();
                for ( int i=0;i<rest.length();i++ )
                {
                    if ( Character.isDigit(rest.charAt(i)))
                        sb.append( rest.charAt(i) );
                    else
                        break;
                }
                if ( sb.length()> 0 )
                {
                    rest = rest.substring(sb.length());
                    for ( int i=0;i<rest.length();i++ )
                    {
                        if ( !Character.isLetter(rest.charAt(i)))
                            return false;
                    }
                    return true;
                }
            }
        }
        return false;
    }
    /**
     * Check that a docid refers to a letter
     * @param docid document identifier of a possible letter
     * @return true if it is else false
     * @throws PagesException 
     */
    private boolean docidIsLetter( String docid ) throws PagesException
    {
        int state = 0;
        Table table = new Table( Utils.shortDocID(docid)+"/letters" );
        int lastIndex = docid.lastIndexOf("/");
        if ( lastIndex == -1 )
            throw new PagesException("Invalid docid "+docid);
        String last = docid.substring(lastIndex+1);
        if ( last.contains("-") )
        {
            String[] parts = last.split("-");
            for ( int i=0;i<parts.length;i++ )
            {
                switch ( state )
                {
                    case 0: // look for day
                        if ( isDay(parts[i]) )
                            state = 1;
                        else if ( isMonthName(parts[i]) )
                            state = 2;
                        else if ( isYear(parts[i]) )
                            state = 3;
                        else
                            state = -1;
                        break;
                    case 1: // look for month
                        if ( isMonthName(parts[i]) )
                            state = 2;
                        else 
                            state = -1;
                        break;
                    case 2: // look for year
                        if ( isYear(parts[i]) )
                            state = 3;
                        break;
                    case 3: // look for sender
                        if ( table.hasPerson(parts[i]) )
                            state = 4;
                        else
                            state = -1;
                        break;
                    case 4: // look for recipient
                        if ( table.hasPerson(parts[i]) )
                            state = 5;
                        else
                            state = -1;
                        break;
                }
                if ( state == -1 )
                    break;
            }
        }
        return state != -1;
    }
    String buildRegex( String letter )
    {
        String[] parts = letter.split("-");
        StringBuilder sb = new StringBuilder();
        for ( int i=0;i<parts.length-2;i++ )
        {
            sb.append(parts[i]);
            sb.append("-");
        }
        sb.append("P.+-");
        sb.append(parts[parts.length-2]);
        sb.append("-");
        String last = parts[parts.length-1];
        int index = last.lastIndexOf(".");
        if ( index != -1 )
            last = last.substring(0,index);
        sb.append(last);
        sb.append(".*");
        return sb.toString();
    }
    String getPageNumber( String fName )
    {
        String[] parts = fName.split("-");
        if ( parts.length > 2 )
        {
            String rawPNum = parts[parts.length-3];
            if ( rawPNum.startsWith("P") )
                return rawPNum.substring(1);
        }
        return "";
    }
    String getFacs( File f )
    {
        String absPath = f.getAbsolutePath();
        String facs = Utils.subtractPaths( absPath, PagesWebApp.webRoot );
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
                while (j >= increment && compareObjs((JSONObject)list.get(j-increment),temp)>0) 
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
    String getPagesFromCorpix( String docid ) throws PagesException
    {
       int lastIndex = docid.lastIndexOf("/");
       if ( lastIndex != -1 )
       {
           String letter = docid.substring(lastIndex+1);
           String firstPart = docid.substring(0,lastIndex);
           String path = PagesWebApp.webRoot+"/corpix/"+firstPart;
           File dir = new File(path);
           JSONArray list = new JSONArray();
           if ( dir.exists() )
           {
               String regex = buildRegex(letter);
               File[] files = dir.listFiles();
               for ( int i=0;i<files.length;i++ )
               {
                   if ( files[i].isFile() && files[i].getName().matches(regex) )
                   {
                        String n = getPageNumber(files[i].getName());
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
               sortImageList( list );
               return list.toJSONString().replaceAll("\\\\/","/");
           }
        }
        return "[]";
    }
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
                System.out.println("docid="+docid+" vPath="+vPath);
                EcdosisVersion cortex = doGetResourceVersion( Database.CORTEX,
                    docid, vPath);
                if ( (pages.isEmpty()||cortex.isEmpty()) && docidIsLetter(docid) )
                    text = getPagesFromCorpix(docid);
                else
                {
                    String stil = pages.getVersionString();
                    System.out.println("pages for "+vPath+"="+stil);
                    if ( pages.getMVD()!= null )
                        System.out.println(pages.getMVD().getVersionTable());
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
                            // the actual page number
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
