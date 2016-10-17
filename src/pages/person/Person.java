/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pages.person;
/**
 *
 * @author desmond
 */
public class Person implements Comparable<Person>
{
    public String name,sortKey,key;
    Person(String name,String sortKey)
    {
        this.name = name;
        this.sortKey = sortKey;
    }
    public int compareTo( Person p)
    {
        return this.sortKey.compareTo(p.sortKey);
    }
}
