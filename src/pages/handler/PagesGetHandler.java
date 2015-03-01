/*
 * This file is part of TILT.
 *
 *  TILT is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  TILT is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with TILT.  If not, see <http://www.gnu.org/licenses/>.
 *  (c) copyright Desmond Schmidt 2014
 */
package pages.handler;

import pages.constants.Service;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import pages.exception.PagesException;
import pages.Utils;

/**
 * Handle a GET request for various image types, text, GeoJSON
 *
 * @author desmond
 */
public class PagesGetHandler extends PagesHandler {

    public void handle(HttpServletRequest request,
        HttpServletResponse response, String urn) throws PagesException {
        try {
            String service = Utils.first(urn);
            if (service.equals(Service.IMAGE)) 
                new PagesImageHandler().handle(request, response, Utils.pop(urn));
            else if (service.equals(Service.DOCUMENTS) )
                new PagesDocumentsHandler().handle(request,response,Utils.pop(urn));
            else if (service.equals(Service.TEXT) )
                new PagesTextHandler().handle(request,response,Utils.pop(urn));
            else if (service.equals(Service.HTML) )
                new PagesHtmlHandler().handle(request,response,Utils.pop(urn));
            else
                throw new Exception("Unknown service "+service);
        } catch (Exception e) {
            throw new PagesException(e);
        }
    }
}