/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pages.person;

import calliope.core.constants.Database;
import calliope.core.database.Connection;
import calliope.core.database.Connector;
import calliope.core.exception.DbException;
import calliope.core.constants.JSONKeys;
import pages.exception.PagesException;
import java.util.HashMap;
import org.json.simple.*;
import org.json.simple.JSONValue;

/**
 *
 * @author desmond
 */
public class Table {
    public HashMap<String,Person> people;
    public Table( String docid ) throws PagesException
    {
        people = new HashMap<String,Person>();
        try
        {
            Connection conn = Connector.getConnection();
            String bson = conn.getFromDb(Database.PEOPLE, docid);
            if ( bson != null )
            {
                JSONObject jDoc = (JSONObject)JSONValue.parse(bson);
                JSONArray jArr = (JSONArray)jDoc.get("persons");
                for ( int i=0;i<jArr.size();i++ )
                {
                    JSONObject jObj = (JSONObject)jArr.get(i);
                    
                    people.put( (String)jObj.get(JSONKeys.KEY),
                            new Person((String)jObj.get("fullName"),
                            (String)jObj.get("sortName")) );
                }
            }
            else
                throw new DbException(docid+" not found");
        }
        catch ( Exception e )
        {
            throw new PagesException(e);
        }
    }
    public boolean hasPerson(String key )
    {
        return people.containsKey(key);
    }
}
