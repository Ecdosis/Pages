/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pages.handler;

import calliope.core.Utils;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import pages.exception.MissingDocumentException;
import pages.constants.Params;
import pages.person.Table;
import pages.person.Person;

/**
 * Get the description of a letter from its docid
 * @author desmond
 */
public class PagesLetterHandler extends PagesGetHandler 
{
    public void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) 
        throws MissingDocumentException
    {
        try
        {
            String res = "";
            String docid = request.getParameter(Params.DOCID);
            if ( docid != null )
            {
                int index = docid.lastIndexOf("/");
                if ( index > 0 )
                {
                    Table table = new Table( Utils.shortDocID(docid)+"/letters" );
                    String last = docid.substring(index+1);
                    String[] parts = last.split("-");
                    String from = null;
                    String to = null;
                    String date = null;
                    for ( int i=0;i<parts.length;i++ )
                    {
                        if ( table.people.containsKey(parts[i]) )
                        {
                            Person p = table.people.get(parts[i]);
                            if ( from == null )
                                from = p.name;
                            else if ( to == null )
                                to = p.name;
                        }
                        else if ( PagesListHandler.isDay(parts[i]) )
                        {
                            date = Integer.toString(Integer.parseInt(parts[i]));
                        }
                        else if ( PagesListHandler.isMonthName(parts[i]) )
                        {
                            if ( date == null )
                                date = "";
                            else
                                date += " ";
                            date += PagesListHandler.months.get(parts[i]);
                        }
                        else if ( PagesListHandler.isYear(parts[i]) )
                        {
                            if ( date == null )
                                date = "";
                            else
                                date += " ";
                            date += parts[i];
                        }
                    }
                    if ( from != null && to != null )
                        res = from+" to "+to;
                    if ( date != null )
                        res += " "+date;
                }
            }
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().println(res);
        }
        catch ( Exception e )
        {
            throw new MissingDocumentException(e);
        }
    }
}
