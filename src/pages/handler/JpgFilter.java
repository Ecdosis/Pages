/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pages.handler;

import java.io.FilenameFilter;
import java.io.File;
class JpgFilter implements FilenameFilter
{
    public boolean accept(File dir, String name)
    {
        boolean found = name.endsWith(".jpg");
        return found;
    }
}