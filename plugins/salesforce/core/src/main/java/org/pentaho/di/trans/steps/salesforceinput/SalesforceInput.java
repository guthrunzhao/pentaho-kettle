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


package org.pentaho.di.trans.steps.salesforceinput;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

import mondrian.util.Base64;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleValueException;
import org.pentaho.di.core.util.Utils;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.di.trans.steps.salesforce.SalesforceConnectionUtils;
import org.pentaho.di.trans.steps.salesforce.SalesforceRecordValue;
import org.pentaho.di.trans.steps.salesforce.SalesforceStep;

/**
 * Read data from Salesforce module, convert them to rows and writes these to one or more output streams.
 *
 * @author Samatar
 * @since 10-06-2007
 */
public class SalesforceInput extends SalesforceStep {
  private static Class<?> PKG = SalesforceInputMeta.class; // for i18n purposes, needed by Translator2!!

  private SalesforceInputMeta meta;
  private SalesforceInputData data;

  public SalesforceInput( StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta,
    Trans trans ) {
    super( stepMeta, stepDataInterface, copyNr, transMeta, trans );
  }

  @Override
  public boolean processRow( StepMetaInterface smi, StepDataInterface sdi ) throws KettleException {
    if ( first ) {
      first = false;

      // Create the output row meta-data
      data.outputRowMeta = new RowMeta();

      meta.getFields( data.outputRowMeta, getStepname(), null, null, this, repository, metaStore );

      // For String to <type> conversions, we allocate a conversion meta data row as well...
      //
      data.convertRowMeta = data.outputRowMeta.cloneToType( ValueMetaInterface.TYPE_STRING );

      // Let's query Salesforce
      data.connection.query( meta.isSpecifyQuery() );

      data.limitReached = true;
      data.recordcount = data.connection.getQueryResultSize();
      if ( data.recordcount > 0 ) {
        data.limitReached = false;
        data.nrRecords = data.connection.getRecordsCount();
      }
      if ( log.isDetailed() ) {
        logDetailed( BaseMessages.getString( PKG, "SalesforceInput.Log.RecordCount" ) + " : " + data.recordcount );
      }

    }

    Object[] outputRowData = null;

    try {
      // get one row ...
      outputRowData = getOneRow();

      if ( outputRowData == null ) {
        setOutputDone();
        return false;
      }

      putRow( data.outputRowMeta, outputRowData ); // copy row to output rowset(s);

      if ( checkFeedback( getLinesInput() ) ) {
        if ( log.isDetailed() ) {
          logDetailed( BaseMessages.getString( PKG, "SalesforceInput.log.LineRow", "" + getLinesInput() ) );
        }
      }

      data.rownr++;
      data.recordIndex++;

      return true;
    } catch ( KettleException e ) {
      boolean sendToErrorRow = false;
      String errorMessage = null;
      if ( getStepMeta().isDoingErrorHandling() ) {
        sendToErrorRow = true;
        errorMessage = e.toString();
      } else {
        logError( BaseMessages.getString( PKG, "SalesforceInput.log.Exception", e.getMessage() ) );
        logError( Const.getStackTracker( e ) );
        setErrors( 1 );
        stopAll();
        setOutputDone(); // signal end to receiver(s)
        return false;
      }
      if ( sendToErrorRow ) {
        // Simply add this row to the error row
        putError( getInputRowMeta(), outputRowData, 1, errorMessage, null, "SalesforceInput001" );
      }
    }
    return true;
  }

  private Object[] getOneRow() throws KettleException {
    if ( data.limitReached || data.rownr >= data.recordcount ) {
      return null;
    }

    // Build an empty row based on the meta-data
    Object[] outputRowData = buildEmptyRow();

    try {

      // check for limit rows
      if ( data.limit > 0 && data.rownr >= data.limit ) {
        // User specified limit and we reached it
        // We end here
        data.limitReached = true;
        return null;
      } else {
        if ( data.rownr >= data.nrRecords || data.finishedRecord ) {
          if ( meta.getRecordsFilter() != SalesforceConnectionUtils.RECORDS_FILTER_UPDATED ) {
            // We retrieved all records available here
            // maybe we need to query more again ...
            if ( log.isDetailed() ) {
              logDetailed( BaseMessages.getString( PKG, "SalesforceInput.Log.NeedQueryMore", "" + data.rownr ) );
            }

            if ( data.connection.queryMore() ) {
              // We returned more result (query is not done yet)
              int nr = data.connection.getRecordsCount();
              data.nrRecords += nr;
              if ( log.isDetailed() ) {
                logDetailed( BaseMessages.getString( PKG, "SalesforceInput.Log.QueryMoreRetrieved", "" + nr ) );
              }

              // We need here to initialize recordIndex
              data.recordIndex = 0;

              data.finishedRecord = false;
            } else {
              // Query is done .. we finished !
              return null;
            }
          }
        }
      }

      // Return a record
      SalesforceRecordValue srvalue = data.connection.getRecord( data.recordIndex );
      data.finishedRecord = srvalue.isAllRecordsProcessed();

      if ( meta.getRecordsFilter() == SalesforceConnectionUtils.RECORDS_FILTER_DELETED ) {
        if ( srvalue.isRecordIndexChanges() ) {
          // We have moved forward...
          data.recordIndex = srvalue.getRecordIndex();
        }
        if ( data.finishedRecord && srvalue.getRecordValue() == null ) {
          // We processed all records
          return null;
        }
      }
      for ( int i = 0; i < data.nrfields; i++ ) {
        String value =
          data.connection.getRecordValue( srvalue.getRecordValue(), meta.getInputFields()[i].getField() );

        // DO Trimming!
        switch ( meta.getInputFields()[i].getTrimType() ) {
          case SalesforceInputField.TYPE_TRIM_LEFT:
            value = Const.ltrim( value );
            break;
          case SalesforceInputField.TYPE_TRIM_RIGHT:
            value = Const.rtrim( value );
            break;
          case SalesforceInputField.TYPE_TRIM_BOTH:
            value = Const.trim( value );
            break;
          default:
            break;
        }

        doConversions( outputRowData, i, value );

        // Do we need to repeat this field if it is null?
        if ( meta.getInputFields()[i].isRepeated() ) {
          if ( data.previousRow != null && Utils.isEmpty( value ) ) {
            outputRowData[i] = data.previousRow[i];
          }
        }

      } // End of loop over fields...

      int rowIndex = data.nrfields;

      // See if we need to add the url to the row...
      if ( meta.includeTargetURL() && !Utils.isEmpty( meta.getTargetURLField() ) ) {
        outputRowData[rowIndex++] = data.connection.getURL();
      }

      // See if we need to add the module to the row...
      if ( meta.includeModule() && !Utils.isEmpty( meta.getModuleField() ) ) {
        outputRowData[rowIndex++] = data.connection.getModule();
      }

      // See if we need to add the generated SQL to the row...
      if ( meta.includeSQL() && !Utils.isEmpty( meta.getSQLField() ) ) {
        outputRowData[rowIndex++] = data.connection.getSQL();
      }

      // See if we need to add the server timestamp to the row...
      if ( meta.includeTimestamp() && !Utils.isEmpty( meta.getTimestampField() ) ) {
        outputRowData[rowIndex++] = data.connection.getServerTimestamp();
      }

      // See if we need to add the row number to the row...
      if ( meta.includeRowNumber() && !Utils.isEmpty( meta.getRowNumberField() ) ) {
        outputRowData[rowIndex++] = new Long( data.rownr );
      }

      if ( meta.includeDeletionDate() && !Utils.isEmpty( meta.getDeletionDateField() ) ) {
        outputRowData[rowIndex++] = srvalue.getDeletionDate();
      }

      RowMetaInterface irow = getInputRowMeta();

      data.previousRow = irow == null ? outputRowData : irow.cloneRow( outputRowData ); // copy it to make
    } catch ( Exception e ) {
      throw new KettleException( BaseMessages
        .getString( PKG, "SalesforceInput.Exception.CanNotReadFromSalesforce" ), e );
    }

    return outputRowData;
  }

  // DO CONVERSIONS...
  void doConversions( Object[] outputRowData, int i, String value ) throws KettleValueException {
    ValueMetaInterface targetValueMeta = data.outputRowMeta.getValueMeta( i );
    ValueMetaInterface sourceValueMeta = data.convertRowMeta.getValueMeta( i );

    if ( ValueMetaInterface.TYPE_BINARY != targetValueMeta.getType() ) {
      outputRowData[i] = targetValueMeta.convertData( sourceValueMeta, value );
    } else {
      // binary type of salesforce requires specific conversion
      if ( value != null ) {
        outputRowData[ i ] = Base64.decode( value );
      } else {
        outputRowData[ i ] = null;
      }
    }
  }

  /*
   * build the SQL statement to send to Salesforce
   */
  private String BuiltSOQl() {
    String sql = "";
    SalesforceInputField[] fields = meta.getInputFields();

    switch ( meta.getRecordsFilter() ) {
      case SalesforceConnectionUtils.RECORDS_FILTER_UPDATED:
        for ( int i = 0; i < data.nrfields; i++ ) {
          SalesforceInputField field = fields[i];
          sql += environmentSubstitute( field.getField() );
          if ( i < data.nrfields - 1 ) {
            sql += ",";
          }
        }
        break;
      case SalesforceConnectionUtils.RECORDS_FILTER_DELETED:
        sql += "SELECT ";
        for ( int i = 0; i < data.nrfields; i++ ) {
          SalesforceInputField field = fields[i];
          sql += environmentSubstitute( field.getField() );
          if ( i < data.nrfields - 1 ) {
            sql += ",";
          }
        }
        sql += " FROM " + environmentSubstitute( meta.getModule() ) + " WHERE isDeleted = true";
        break;
      default:
        sql += "SELECT ";
        for ( int i = 0; i < data.nrfields; i++ ) {
          SalesforceInputField field = fields[i];
          sql += environmentSubstitute( field.getField() );
          if ( i < data.nrfields - 1 ) {
            sql += ",";
          }
        }
        sql = sql + " FROM " + environmentSubstitute( meta.getModule() );
        if ( !Utils.isEmpty( environmentSubstitute( meta.getCondition() ) ) ) {
          sql += " WHERE " + environmentSubstitute( meta.getCondition().replace( "\n\r", "" ).replace( "\n", "" ) );
        }
        break;
    }

    return sql;
  }

  /**
   * Build an empty row based on the meta-data.
   *
   * @return
   */
  private Object[] buildEmptyRow() {
    Object[] rowData = RowDataUtil.allocateRowData( data.outputRowMeta.size() );
    return rowData;
  }

  @Override
  public boolean init( StepMetaInterface smi, StepDataInterface sdi ) {
    meta = (SalesforceInputMeta) smi;
    data = (SalesforceInputData) sdi;

    if ( super.init( smi, sdi ) ) {
      // get total fields in the grid
      data.nrfields = meta.getInputFields().length;

      // Check if field list is filled
      if ( data.nrfields == 0 ) {
        log.logError( BaseMessages.getString( PKG, "SalesforceInputDialog.FieldsMissing.DialogMessage" ) );
        return false;
      }

      String soSQL = environmentSubstitute( meta.getQuery() );
      try {

        if ( meta.isSpecifyQuery() ) {
          // Check if user specified a query
          if ( Utils.isEmpty( soSQL ) ) {
            log.logError( BaseMessages.getString( PKG, "SalesforceInputDialog.QueryMissing.DialogMessage" ) );
            return false;
          }
        } else {
          // check records filter
          if ( meta.getRecordsFilter() != SalesforceConnectionUtils.RECORDS_FILTER_ALL ) {
            String realFromDateString = environmentSubstitute( meta.getReadFrom() );
            if ( Utils.isEmpty( realFromDateString ) ) {
              log.logError( BaseMessages.getString( PKG, "SalesforceInputDialog.FromDateMissing.DialogMessage" ) );
              return false;
            }
            String realToDateString = environmentSubstitute( meta.getReadTo() );
            if ( Utils.isEmpty( realToDateString ) ) {
              log.logError( BaseMessages.getString( PKG, "SalesforceInputDialog.ToDateMissing.DialogMessage" ) );
              return false;
            }
            try {
              SimpleDateFormat dateFormat = new SimpleDateFormat( SalesforceInputMeta.DATE_TIME_FORMAT );
              data.startCal = new GregorianCalendar();
              data.startCal.setTime( dateFormat.parse( realFromDateString ) );
              data.endCal = new GregorianCalendar();
              data.endCal.setTime( dateFormat.parse( realToDateString ) );
              dateFormat = null;
            } catch ( Exception e ) {
              log.logError( BaseMessages.getString( PKG, "SalesforceInput.ErrorParsingDate" ), e );
              return false;
            }
          }
        }

        data.limit = Const.toLong( environmentSubstitute( meta.getRowLimit() ), 0 );

        // Do we have to query for all records included deleted records
        data.connection.setQueryAll( meta.isQueryAll() );

        // Build query if needed
        if ( meta.isSpecifyQuery() ) {
          // Free hand SOQL Query
          data.connection.setSQL( soSQL.replace( "\n\r", " " ).replace( "\n", " " ) );
        } else {
          // Set calendars for update or deleted records
          if ( meta.getRecordsFilter() != SalesforceConnectionUtils.RECORDS_FILTER_ALL ) {
            data.connection.setCalendar( meta.getRecordsFilter(), data.startCal, data.endCal );
          }

          if ( meta.getRecordsFilter() == SalesforceConnectionUtils.RECORDS_FILTER_UPDATED ) {
            // Return fields list
            data.connection.setFieldsList( BuiltSOQl() );
          } else {
            // Build now SOQL
            data.connection.setSQL( BuiltSOQl() );
          }
        }

        // Now connect ...
        data.connection.connect();

        return true;
      } catch ( KettleException ke ) {
        logError( BaseMessages.getString( PKG, "SalesforceInput.Log.ErrorOccurredDuringStepInitialize" )
          + ke.getMessage() );
        return false;
      }
    }
    return false;
  }

  @Override
  public void dispose( StepMetaInterface smi, StepDataInterface sdi ) {
    if ( data.outputRowMeta != null ) {
      data.outputRowMeta = null;
    }
    if ( data.convertRowMeta != null ) {
      data.convertRowMeta = null;
    }
    if ( data.previousRow != null ) {
      data.previousRow = null;
    }
    if ( data.startCal != null ) {
      data.startCal = null;
    }
    if ( data.endCal != null ) {
      data.endCal = null;
    }
    super.dispose( smi, sdi );
  }
}
