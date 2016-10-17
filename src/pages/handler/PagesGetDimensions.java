/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pages.handler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import pages.exception.PagesException;
import pages.constants.Params;
import calliope.core.database.Connection;
import calliope.core.database.Connector;
import calliope.core.constants.JSONKeys;
import pages.constants.Database;
import org.json.simple.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.util.ArrayList;
import pages.PagesWebApp;

/**
 * Get the dimensions of a page image
 * @author desmond
 */
public class PagesGetDimensions extends PagesGetHandler
{
    String docid;
    private JSONObject getImageDimensions( File f ) throws Exception
    {
        BufferedImage bimg = ImageIO.read(f);
        int width = bimg.getWidth();
        int height = bimg.getHeight();
        JSONObject jObj = new JSONObject();
        jObj.put("width",width);
        jObj.put("height",height);
        return jObj;
    }
    ArrayList<String> getFileVariants( String filename )
    {
        ArrayList<String> list = new ArrayList<String>();
        list.add( filename );
        list.add( filename+".jpg");
        list.add( filename+".png");
        String[] parts = docid.split("/");
        String last = parts[parts.length-1];
        if ( filename.contains("/") )
            filename = filename.substring(0,filename.lastIndexOf("/"));
        StringBuilder sb = new StringBuilder();
        for ( int i=0;i<8-last.length();i++ )
            sb.append("0");
        sb.append( last );
        list.add( filename+"/"+sb.toString()+".jpg" );
        list.add( filename+"/"+sb.toString()+".png" );
        return list;
    }
    public void handle(HttpServletRequest request,
        HttpServletResponse response, String urn) throws PagesException 
    {
        docid = request.getParameter(Params.DOCID);
        try
        {
            Connection conn = Connector.getConnection();
            JSONObject jObj;
            if ( docid == null )
            {
                jObj = new JSONObject();
                jObj.put("error","Missing docid");
            }
            else 
            {
                String jStr = conn.getFromDb(Database.METADATA,docid);
                if ( jStr != null )
                {
                    jObj = (JSONObject)JSONValue.parse(jStr);
                    jObj.remove(JSONKeys._ID);
                    jObj.remove(JSONKeys.DOCID);
                }
                else
                {
                    String filename = PagesWebApp.webRoot+"/corpix/"+docid;
                    ArrayList<String> variants = getFileVariants(filename);
                    jObj = null;
                    for ( int i=0;i<variants.size();i++ )
                    {
                        File f = new File(variants.get(i));
                        //System.out.println(f.getAbsolutePath());
                        if ( f.exists() )
                        {
                            jObj = getImageDimensions(f);
                            break;
                        }
                    }
                    if ( jObj != null ) // found it
                    {
                        jStr = jObj.toJSONString().replaceAll("\\\\/","/");
                        conn.putToDb(Database.METADATA, docid, jStr);
                    }
                    else    // ooops
                    {
                        jStr = "{\"error\":\"File "+docid+" Not found\"}";
                        jObj = (JSONObject)JSONValue.parse(jStr);
                    }
                }
            }
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().print( jObj.toJSONString().replaceAll("\\\\/","/") );
        }
        catch ( Exception e )
        {
            throw new PagesException(e);
        }
    }
}
