/*
 * RegExpTest.java
 */
package org.docma.apptest;

import java.util.regex.*;

/**
 *
 * @author MP
 */
public class RegExpTest 
{
    public static void main(String[] args)
    {
        String patt = "(\\S*\\s+)*indexterm(\\s+\\S*)*";
        System.out.println("Test1 (true): " + ("indexterm".matches(patt)));
        System.out.println("Test2 (false): " + ("indexterm2".matches(patt)));
        System.out.println("Test3 (false): " + ("abcindexterm2".matches(patt)));
        System.out.println("Test4 (true): " + ("indexterm x".matches(patt)));
        System.out.println("Test5 (true): " + ("indexterm xsd sds".matches(patt)));
        System.out.println("Test6 (true): " + ("1 indexterm".matches(patt)));
        System.out.println("Test7 (false): " + ("123 abc indexterm2 sds".matches(patt)));
        System.out.println("Test8 (true): " + ("123 abc indexterm sds".matches(patt)));
        System.out.println("Test9 (true): " + (" indexterm ".matches(patt)));
    }
}
