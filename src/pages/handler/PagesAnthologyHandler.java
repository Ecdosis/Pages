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
import pages.PagesWebApp;
import pages.constants.Database;
import pages.exception.PagesException;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import calliope.core.constants.JSONKeys;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.io.File;
import java.io.FileOutputStream;
import calliope.core.database.Connection;
import calliope.core.database.Connector;
import calliope.core.exception.DbException;

/**
 *
 * @author desmond
 */
public class PagesAnthologyHandler extends PagesGetHandler 
{
    HashMap<String,String> refsDecl;
    /**
     * Make a list of the jpgs in a folder
     * @param dir the directory to list
     * @return an array of file names
     */
    String[] listJpgs( String dir )
    {
        StringBuilder sb = new StringBuilder();
        try
        {
            File d = new File( dir );
            if ( !d.exists() )
            {
                try {
                     String[] parts = d.getAbsolutePath().split("/");
                     StringBuilder sbs = new StringBuilder();
                     for ( int i=0;i<parts.length;i++ )
                     {
                         sbs.append("/");
                         sbs.append(parts[i]);
                         File test = new File(sbs.toString());
                         if ( test.exists() )
                             System.out.println(test.getAbsolutePath()+" exists");
                         else
                             System.out.println(test.getAbsolutePath()+" does no exist");
                     }
                }
                catch ( Exception e )
                {
                    System.out.println(e.getMessage());
                }
                throw new Exception(d.getAbsolutePath()+" does not exist");
            }
            else if ( !d.isDirectory() )
                throw new Exception(d.getAbsolutePath()+" is not a directory");
            else
            {
                String[] res = d.list(new JpgFilter());
                return res;
            }
        }
        catch ( Exception e )
        {
            writeToDebug(sb+e.getMessage()+" user="
                +System.getProperty("user.name"),"listJpg.log");
        }
        return new String[0];
     }
    /**
     * Derive the orientation of the image from its file name
     * @param num the page number
     * @param rectoIsOdd true if recto is understood to be odd
     * @return an "r" (recto) or "v" (verso)
     */
    String getOrientation( int num, boolean rectoIsOdd )
    {
        if ( rectoIsOdd )
        {
            if ( num%2==0 )
                return "v";
            else
                return "r";
        }
        else
        {
            if ( num%2==0 )
                return "r";
            else
                return "v";
        }
    }
    /**
     * Get the number from the file name
     * @param name the file name
     * @return an integer being the number in the file name
     */
    int fileNum( String name )
    {
        String subname = name.substring(0,name.indexOf(".jpg"));
        return Integer.parseInt(subname);
    }
    /**
     * Normalise the page specification
     * @param jobj the actual page spec
     * @return a canonical one with all field filled out
     */
    JSONObject cleanSpec( JSONObject jobj )
    {
        String src = (String)jobj.get("src");
        int num = fileNum(src);
        if ( !jobj.containsKey("o") )
        {
            jobj.put( "o", getOrientation(num,true) );
        }
        return jobj;
    }
    /**
     * Is the previous recto page odd (or verso even)?
     * @param prev the page spec of the previous page
     * @param alternating true if
     * @return true if recto is odd
     */
    boolean isRectoOdd( JSONObject prev, boolean alternating )
    {
        String src = (String)prev.get("src");
        String orientation = (String)prev.get("o");
        int num = fileNum(src);
        if ( !alternating ) // for recto or verso-only MSS
        {
            if ( orientation.equals("c")||orientation.equals("r") )
            {
                if ( num%2==1 )
                    return false;   // ensure recto
                else
                    return true;
            }
            else    // current is verso, next is also verso
            {
                if ( num%2==1 )
                    return true;    // ensure verso
                else
                    return false;
            }
        }
        else    // usual case
        {
            if ( (orientation.equals("r") 
                || orientation.equals("c")) && num%2==1 )
                return true;
            else if ( orientation.equals("v") && num%2==0 )
                return true;
            else 
                return false;
        }
    }
    /**
     * Is this page name a Roman numeral?
     * @param name the page name
     * @return true if it is
     */
    boolean isRoman( String name )
    {
        name = name.toLowerCase();
        try
        {
            int value = RomanNumeral.valueOf(name);
            return true;
        }
        catch ( Exception e )
        {
            return false;
        }
    }
    /**
     * Get the next Roman numeral in sequence
     * @param numerus the current Roman number
     * @return the next one in lowercase
     */
    static String incRoman( String numerus )
    {
        try
        {
            int number = RomanNumeral.valueOf(numerus.toUpperCase());
            return RomanNumeral.convertToRoman(number+1).toLowerCase();
        }
        catch ( Exception e )
        {
            return "";
        }
    }
    /**
     * Increment a page number
     * @param name the current page number
     * @return the next number in sequence
     */
    String incName( String name )
    {
        try
        {
            if ( isRoman(name) )
            {
                return incRoman(name);
            }
            else 
            {
                int i=0;
                for ( i=0;i<name.length();i++ )
                {
                    if ( !Character.isDigit(name.charAt(i)) )
                        break;
                }
                if ( i == name.length() )   // it's arabic
                {
                    int num = Integer.parseInt(name);
                    return Integer.toString(num+1);
                }
                else if ( i > 0 )
                {
                    String intPart = name.substring(0,i);
                    int value = Integer.parseInt(intPart)+1;
                    return Integer.toString(value) + name.substring(i);
                }
                else
                    throw new Exception("invalid page number "+name);
            }
        }
        catch ( Exception e )
        {
            System.out.println(e.getMessage());
            return "1";
        }
    }
    /**
     * Create an empty pages spec
     * @param docid the document identifier
     * @return an empty (no pages) specification
     */
    String emptySpec( String docid )
    {
        // create default json spec
        StringBuilder sb = new StringBuilder();
        sb.append("{\"docid\":\"");
        sb.append(docid);
        sb.append("\",\"alternating\":true,\"specials\":[]}");
        return sb.toString();
    }
    /**
     * Get the metadata entry for the docid
     * @param docid the docid
     * @return a JSON string
     */
    String getMetadata( String docid ) throws DbException
    {
        Connection conn = Connector.getConnection();
        String json = conn.getFromDb( Database.METADATA, docid );
        if ( json == null )
        {
            json = emptySpec( docid );
        }
        return json;
    }
    /**
     * Get the anthology directory path given its docid
     * @param docid the document identifier
     * @return the absolute directory path
     */
    String getDirPath( String docid )
    {
        return PagesWebApp.webRoot+"/corpix/"+docid;
    }
    /**
     * Create a file name from its file number
     * @param fileNum the file number
     * @return a String
     */
    String fileName( int fileNum )
    {
        StringBuilder sb = new StringBuilder();
        int len = (int)Math.floor(Math.log10(fileNum))+1;
        for ( int i=len;i<8;i++ )
            sb.append("0");
        return sb.toString()+fileNum+".jpg";
    }
    /**
     * Get the previous page when starting in the middle of a sequence
     * @param map map of file names to page specs
     * @param pageNo the current page number
     * @param alternating true if recto and verso alternate
     * @return the JSOB page spec of the immediately preceding page
     */
    JSONObject previousPage( HashMap<String,JSONObject> map, int pageNo, 
        boolean alternating )
    {
        Set<String> keys = map.keySet();
        String[] keyArray = new String[keys.size()];
        keys.toArray(keyArray);
        Arrays.sort(keyArray);
        JSONObject first = null;
        for ( int i=keyArray.length-1;i>=0;i-- )
        {
            JSONObject jObj = map.get(keyArray[i]);
            int fileNum = fileNum((String)jObj.get("src"));
            if ( fileNum < pageNo )
            {
                first = jObj;
                break;
            }
        }
        if ( first != null )
        {
            int fileNum = fileNum((String)first.get("src"));
            String orientation = (String)first.get("o");
            String n = (String)first.get("n");
            for ( int i=fileNum+1;i<pageNo;i++ )
            {
                if ( alternating )
                {
                    if ( orientation.equals("r")||orientation.equals("c") )
                        orientation = "v";
                    else
                        orientation = "r";
                }
                n = incName(n);
            }
            // OK, so orientation is set for previous file
            // create new page spec
            JSONObject prevPage = new JSONObject();
            prevPage.put("src",fileName(pageNo-1));
            prevPage.put("o",orientation);
            prevPage.put("n",n);
            return prevPage;
        }
        else
            return null;
    }
    /**
     * Convert the metadata shorthand refs to full representaiton
     * @param jObj the raw page metadata
     * @return the modified object with expanded page refs
     */
    JSONObject convertRefs(JSONObject jObj )
    {
        if ( jObj.containsKey("refs") )
        {
            String refs = (String)jObj.get("refs");
            String[] items = refs.split(",");
            jObj.remove("refs");
            JSONArray objs = new JSONArray();
            jObj.put("refs",objs);
            for ( int i=0;i<items.length;i++ )
            {
                if ( items[i].length()>1 )
                {
                    String prefix = items[i].substring(0,1);
                    if ( refsDecl != null && refsDecl.containsKey(prefix) )
                    {
                        String value = items[i].substring(1);
                        JSONObject obj = new JSONObject();
                        obj.put("title",refsDecl.get(prefix));
                        obj.put("value",value);
                        objs.add(obj);
                    }
                    else
                        System.out.println("missing refsdecl or unkown ref type "
                            +prefix);
                }
                // else we ignore it - bad specification
            }
        }
        return jObj;
    }
    /**
     * Increment the extra page-refs
     */
    JSONArray incRefs( JSONArray refs )
    {
        JSONArray newRefs = new JSONArray();
        for ( int i=0;i<refs.size();i++ )
        {
            JSONObject newObj = new JSONObject();
            JSONObject oldObj = (JSONObject)refs.get(i);
            newObj.put("title",oldObj.get("title"));
            String newValue = incName((String)oldObj.get("value"));
            newObj.put("value",newValue);
            newRefs.add( newObj );
        }
        return newRefs;
    }
    void writeToDebug( String message, String file )
    {
        try
        {
            File debug = new File("/tmp/"+file);
            if ( debug.exists() )
                debug.delete();
            FileOutputStream fos = new FileOutputStream(debug);
            fos.write(message.getBytes());
            fos.close();
        }
        catch ( Exception e )
        {
            e.printStackTrace(System.out);
        }
    }
    String lastPart( String docid )
    {
        String[] parts = docid.split("/");
        if ( parts.length>0 )
            return parts[parts.length-1];
        else
            return docid;
    }
    /**
     * Get a list of page specifications for ms-viewer
     * @param request the http request
     * @param response the response
     * @param urn the urn (empty)
     * @throws PagesException 
     */
    public void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) throws PagesException
    {
        try
        {
            String docid = request.getParameter(Params.DOCID);
            if ( docid != null )
            {
                String spec;
                String json = getMetadata(docid);
                String dirPath = getDirPath(docid);
                String[] files = listJpgs(dirPath);
                if ( files.length==0 )
                    spec = emptySpec(docid);
                else
                {
                    JSONObject jObj = (JSONObject)JSONValue.parse(json);
                    String title = (String) jObj.get(JSONKeys.TITLE);
                    if ( title == null )
                        title = lastPart(docid);
                    JSONArray refs = (JSONArray)jObj.get("refsdecl");
                    JSONArray specials = (JSONArray)jObj.get("specials");
                    boolean alternating = (jObj.containsKey("alternating"))
                        ?(Boolean)jObj.get("alternating"):true;
                    if ( refs != null )
                    {
                        refsDecl = new HashMap<String,String>();
                        for ( int i=0;i<refs.size();i++ )
                        {
                            JSONObject obj = (JSONObject)refs.get(i);
                            refsDecl.put( (String)obj.get("prefix"), 
                                (String)obj.get("desc"));
                        }
                    }
                    HashMap<String,JSONObject> map = new HashMap<String,JSONObject>();
                    for ( int i=0;i<specials.size();i++ )
                    {
                        JSONObject special = (JSONObject)specials.get(i);
                        JSONObject cleaned = cleanSpec(special);
                        map.put((String)special.get("src"),cleaned);
                    }
                    Arrays.sort(files);
                    boolean rectoIsOdd = true; // default
                    JSONObject last = null;
                    JSONObject wrapper = new JSONObject();
                    JSONArray dest = new JSONArray();
                    wrapper.put("ranges",dest);
                    wrapper.put(JSONKeys.TITLE,title);
                    for ( int i=0;i<files.length;i++ )
                    {
                        if ( i > 0 && fileNum(files[i])-fileNum(files[i-1]) > 1 )
                            last = null;
                        if ( map.containsKey(files[i]) )
                        {
                            last = convertRefs(map.get(files[i]));
                            dest.add(last);
                        }
                        else    // create a spec
                        {
                            JSONObject elem = new JSONObject();
                            int pageNo = fileNum(files[i]);
                            if ( last == null )
                                last = previousPage(map,pageNo,alternating);
                            rectoIsOdd = (last==null)?true:isRectoOdd(last,alternating);
                            String orientation = getOrientation(pageNo,rectoIsOdd);
                            elem.put("n",(last==null)?"1":incName((String)last.get("n")));
                            if ( last != null && last.containsKey("refs") )
                            {
                                if ( last.get("refs") instanceof String )
                                    last = convertRefs(last);
                                JSONArray jRefs = (JSONArray)last.get("refs");
                                if ( jRefs.size()>0 )
                                    elem.put("refs",incRefs(jRefs));
                            }
                            elem.put( "src",files[i]);
                            elem.put("o",orientation);
                            last = elem;
                            dest.add( elem );
                        }
                    }
                    spec = wrapper.toJSONString();
                }
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().println(spec);
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
