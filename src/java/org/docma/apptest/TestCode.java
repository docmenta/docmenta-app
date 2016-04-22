/*
 */
package org.docma.apptest;

/**
 *
 * @author MP
 */
public class TestCode {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) 
    {
        String str = "htm|html";
        String[] arr = str.split("\\|");
        for (String s : arr) {
            System.out.println("'" + s + "'");
        }
    }
    
}
