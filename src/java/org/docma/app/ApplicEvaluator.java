/*
 * ApplicEvaluator.java
 * 
 *  Copyright (C) 2013  Manfred Paula, http://www.docmenta.org
 *   
 *  This file is part of Docmenta. Docmenta is free software: you can 
 *  redistribute it and/or modify it under the terms of the GNU Lesser 
 *  General Public License as published by the Free Software Foundation, 
 *  either version 3 of the License, or (at your option) any later version.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Docmenta.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.docma.app;

import org.docma.coreapi.*;
import java.util.*;

/**
 *
 * @author MP
 */
public class ApplicEvaluator
{
    private List declaredApplics;
    private List applicList;
    private StringTokenizer expression;
    private String token;
    private boolean checkDeclared = false;

    /* --------------  Public methods  ---------------------- */

    public ApplicEvaluator()
    {
    }

    public void setDeclaredApplics(String[] applics)
    {
        declaredApplics = Arrays.asList(applics);
    }

    public void setApplicability(String[] applics)
    {
        applicList = Arrays.asList(applics);
    }

    public void setCheckDeclared(boolean checkDeclared)
    {
        this.checkDeclared = checkDeclared;
    }

    public boolean evaluate(String applicExpression) throws DocException
    {
        expression = new StringTokenizer(applicExpression, "(),|-", true);

        nextToken();    // move cursor to first token
        boolean res = orTerm();
        if (! getToken().equals("")) {
            throw new DocException("Unexpected characters: " + getToken());
        }
        return res;
    }

    /* --------------  Private methods  ---------------------- */

    private boolean orTerm() throws DocException
    {
        boolean res = andTerm();
        while (getToken().equals("|")) {
            nextToken();
            res |= andTerm();
        }
        return res;
    }

    private boolean andTerm() throws DocException
    {
        boolean res = applicTerm();
        while (getToken().equals(",")) {
            nextToken();
            res &= applicTerm();
        }
        return res;
    }

    private boolean applicTerm() throws DocException
    {
        checkUnexpectedEnd();
        if (getToken().equals("-")) {
            nextToken();
            return !applicTerm();
        } else
        if (getToken().equals("(")) {
            nextToken();
            boolean res = orTerm();
            if (getToken().equals(")")) {
                nextToken();
                return res;
            } else {
                throw new DocException("Missing closing bracket. Expected: ). Found: " + getToken());
            }
        } else {
            String applicVar = getToken();
            if (checkDeclared) {
                if (! declaredApplics.contains(applicVar)) {
                    throw new DocException("Applicability '" + applicVar + "' is not declared.");
                }
            }
            nextToken();
            return applicList.contains(applicVar);
        }
    }

    private boolean nextToken()
    {
        while (expression.hasMoreTokens()) {
            token = expression.nextToken().trim();
            if (token.length() > 0) return true;  // skip spaces
        }
        token = "";
        return false;
    }

    private String getToken()
    {
        return token;
    }

    private void checkUnexpectedEnd() throws DocException
    {
        if (token.equals("")) {
                throw new DocException("Unexpected end of expression.");
        }
    }

}
