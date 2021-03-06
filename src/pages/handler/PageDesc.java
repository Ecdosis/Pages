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
import org.json.simple.JSONObject;
import java.awt.Dimension;
/**
 * Description of page
 * @author desmond
 */
public class PageDesc 
{
    String n;
    String src;
    Dimension d;
    PageDesc(String n, String src, Dimension d )
    {
        this.n = n;
        this.d = d;
        this.src = src;
    }
    public JSONObject toJSONObject()
    {
        JSONObject obj = new JSONObject();
        obj.put("n",n);
        obj.put("width",d.width);
        obj.put("height",d.height);
        obj.put("src",src);
        return obj;
    }
}
