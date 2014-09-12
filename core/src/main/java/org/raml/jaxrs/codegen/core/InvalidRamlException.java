package org.raml.jaxrs.codegen.core;

import java.util.List;

import org.raml.parser.rule.ValidationResult;

public class InvalidRamlException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    /** 
     * Reported numbers start at zero, but text editors start with 1.
     */
    private static final int ZERO_OFFSET_CORRECTION = 1;

    public InvalidRamlException() {
    }

    public InvalidRamlException(List<ValidationResult> results) {
        super(toMessage(results));
    }

    public static String toMessage(List<ValidationResult> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("Invalid RAML definition:");
        for (ValidationResult result: results) {
            sb.append("\n\t").append(result.getLevel());
            sb.append(" line:").append(result.getLine() + ZERO_OFFSET_CORRECTION);
            
            sb.append(" columns:").append(result.getStartColumn() + ZERO_OFFSET_CORRECTION)
            .append("..").append(result.getEndColumn() + ZERO_OFFSET_CORRECTION);
            sb.append(" ").append(result.getMessage());
        }
        return sb.toString();
    }

}
