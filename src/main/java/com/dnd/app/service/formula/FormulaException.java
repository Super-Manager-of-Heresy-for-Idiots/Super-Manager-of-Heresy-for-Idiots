package com.dnd.app.service.formula;

/** Raised for any parse or evaluation problem in the formula DSL (unknown variable/function, type
 *  mismatch, division by zero, syntax error). Carried up as a controlled validation failure. */
public class FormulaException extends RuntimeException {
    public FormulaException(String message) {
        super(message);
    }
}
