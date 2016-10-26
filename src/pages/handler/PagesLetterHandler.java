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
 *  (c) copyright Desmond Schmidt 2016
 */

package pages.handler;

import calliope.core.Utils;
import calliope.core.DocType;
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
                        else if ( DocType.isDay(parts[i]) )
                        {
                            date = Integer.toString(Integer.parseInt(parts[i]));
                        }
                        else if ( DocType.isMonth(parts[i]) )
                        {
                            if ( date == null )
                                date = "";
                            else
                                date += " ";
                            date += DocType.getMonth(parts[i]);
                        }
                        else if ( DocType.isYear(parts[i]) )
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
