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
package pages;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Connector;
/**
 * This launches an instance of the Jetty service
 * @author desmond
 */
public class JettyServerThread extends Thread 
{
    /**
     * Run the server
     */
    public void run()
    {
        try
        {
            Server server = new Server(PagesWebApp.wsPort);
            Connector[] connectors = server.getConnectors();
            connectors[0].setHost(PagesWebApp.host);
            server.setHandler(new JettyServer());
            server.start();
            server.join();
        }
        catch ( Exception e )
        {
            e.printStackTrace( System.out );
        }
    }
}
