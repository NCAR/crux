/*
 * Copyright (c) 2016. University Corporation for Atmospheric Research (UCAR). All rights reserved.
 */

package edu.ucar.ral.crux;

import java.util.ArrayList;
import java.util.List;

/**
 * An Exception which is thrown when there is one or more validation failure
 */
public class ValidationException extends Exception {
  private List<ValidationError> validationErrors;

  public ValidationException( List<ValidationError> validationErrors ){
    this.validationErrors = validationErrors;
  }

  public ValidationException( ValidationError validationError ){
    this.validationErrors = new ArrayList<ValidationError>(1);
    this.validationErrors.add( validationError );
  }

  public ValidationException( String message, List<ValidationError> validationErrors ){
    super( message );
    this.validationErrors = validationErrors;
  }

  public ValidationException( String message, ValidationError validationError ){
    super( message );
    this.validationErrors = new ArrayList<ValidationError>(1);
    this.validationErrors.add( validationError );
  }

  @Override
  public String getMessage(){
    String msg = super.getMessage();
    if( msg == null ){
      msg = "";
    }
    for( ValidationError error : validationErrors ){
      msg += error + "\n";
    }
    return msg;
  }

  public List<ValidationError> getValidationErrors() {
    return validationErrors;
  }
}
