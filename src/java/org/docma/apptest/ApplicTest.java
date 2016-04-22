/*
 * ApplicTest.java
 */

package org.docma.apptest;

import org.docma.app.*;
import org.docma.coreapi.*;

/**
 *
 * @author MP
 */
public class ApplicTest
{
    public static void main(String[] args) throws Exception
    {
        ApplicEvaluator app_eval = new ApplicEvaluator();
        String[] declaredApplics = {"PRINT", "WIN", "UNIX", "WIN_7", "WIN_XP"};
        String[] applics = {"PRINT", "WIN_7", "WIN_XP"};
        app_eval.setDeclaredApplics(declaredApplics);
        app_eval.setApplicability(applics);
        app_eval.setCheckDeclared(true);
        try {
            System.out.println("Result: " + app_eval.evaluate(" PRINT, (-UNIX),WIN_XP"));
        } catch (DocException ex) {
            System.out.println(ex.getMessage());
        }
    }
}
