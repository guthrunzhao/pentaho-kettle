/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/


package org.pentaho.di.trans.steps.excelwriter;

import org.pentaho.di.core.injection.Injection;
import org.pentaho.di.core.row.value.ValueMetaFactory;

public class ExcelWriterStepField implements Cloneable {
  @Injection( name = "NAME", group = "FIELDS" )
  private String name;

  @Injection( name = "TYPE", group = "FIELDS" )
  private int type;

  @Injection( name = "FORMAT", group = "FIELDS" )
  private String format;

  @Injection( name = "STYLE_FROM_CELL", group = "FIELDS" )
  private String styleCell;

  @Injection( name = "TITLE", group = "FIELDS" )
  private String title;

  @Injection( name = "HEADERFOOTER_STYLE_FROM_CELL", group = "FIELDS" )
  private String titleStyleCell;

  @Injection( name = "CONTAINS_FORMULA", group = "FIELDS" )
  private boolean formula;

  @Injection( name = "HYPERLINK", group = "FIELDS" )
  private String hyperlinkField;

  @Injection( name = "CELL_COMMENT", group = "FIELDS" )
  private String commentField;

  @Injection( name = "CELL_COMMENT_AUTHOR", group = "FIELDS" )
  private String commentAuthorField;

  public String getCommentAuthorField() {
    return commentAuthorField;
  }

  public void setCommentAuthorField( String commentAuthorField ) {
    this.commentAuthorField = commentAuthorField;
  }

  public ExcelWriterStepField( String name, int type, String format ) {
    this.name = name;
    this.type = type;
    this.format = format;
  }

  public ExcelWriterStepField() {
  }

  public int compare( Object obj ) {
    ExcelWriterStepField field = (ExcelWriterStepField) obj;

    return name.compareTo( field.getName() );
  }

  @Override
  public boolean equals( Object obj ) {
    ExcelWriterStepField field = (ExcelWriterStepField) obj;

    return name.equals( field.getName() );
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Deprecated
  public boolean equal( Object obj ) {
    return equals( obj );
  }

  @Override
  public Object clone() {
    try {
      Object retval = super.clone();
      return retval;
    } catch ( CloneNotSupportedException e ) {
      return null;
    }
  }

  public String getName() {
    return name;
  }

  public void setName( String fieldname ) {
    this.name = fieldname;
  }

  public int getType() {
    return type;
  }

  public String getTypeDesc() {
    return ValueMetaFactory.getValueMetaName( type );
  }

  public void setType( int type ) {
    this.type = type;
  }

  public void setType( String typeDesc ) {
    this.type = ValueMetaFactory.getIdForValueMeta( typeDesc );
  }

  public String getFormat() {
    return format;
  }

  public void setFormat( String format ) {
    this.format = format;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle( String title ) {
    this.title = title;
  }

  public boolean isFormula() {
    return formula;
  }

  public void setFormula( boolean formula ) {
    this.formula = formula;
  }

  public String getHyperlinkField() {
    return hyperlinkField;
  }

  public void setHyperlinkField( String hyperlinkField ) {
    this.hyperlinkField = hyperlinkField;
  }

  public String getCommentField() {
    return commentField;
  }

  public void setCommentField( String commentField ) {
    this.commentField = commentField;
  }

  public String getTitleStyleCell() {
    return titleStyleCell;
  }

  public void setTitleStyleCell( String formatCell ) {
    this.titleStyleCell = formatCell;
  }

  public String getStyleCell() {
    return styleCell;
  }

  public void setStyleCell( String styleCell ) {
    this.styleCell = styleCell;
  }

  @Override
  public String toString() {
    return name + ":" + getTypeDesc();
  }
}
