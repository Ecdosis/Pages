/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pages.handler;
import java.io.File;
/**
 * represent newspaper/letter filenames and also book images
 * @author desmond
 */
public class FileName {
    /** the name of the image as referred to in the editor */
    String n;
    /** id of the document it belongs to (newspapers/letters) minus page-ref */
    String id;
    public FileName( File file )
    {
        String name = file.getName();
        if ( name.contains("-") )
        {
            String[] parts = name.split("-");
            boolean pageSet = false;
            StringBuilder sb = new StringBuilder();
            for ( int i=0;i<parts.length;i++ )
            {
                if ( !pageSet && parts[i].startsWith("P")) 
                {
                    pageSet = true;
                    n = parts[i].substring(1);
                }
                else
                {
                    if ( sb.length()>0 )
                        sb.append("-");
                    if ( parts[i].contains(".") )
                        parts[i] = parts[i].substring(0,parts[i].lastIndexOf("."));
                    sb.append(parts[i]);
                }
            }
            id = sb.toString();
        }
        else if ( name.contains(".") )
        {
            name = name.substring(0,name.lastIndexOf("."));
            while ( name.startsWith("0") )
                name = name.substring(1);
            id = "";
            n = name;
        }
        else 
        {
            id = "";
            n = name;
        }
    }
}
