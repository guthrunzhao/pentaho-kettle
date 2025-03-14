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


package org.pentaho.di.repository;

/**
 * Repository User object
 *
 * @author rmansoor
 *
 */
public interface IUser {
  /**
   * Set the login for the user
   *
   * @param login
   */
  public void setLogin( String login );

  /**
   * Get the login for a the user
   *
   * @return user login
   */
  public String getLogin();

  /**
   * Set the password for the
   *
   * @param password
   */
  public void setPassword( String password );

  /**
   * Get the password for the user
   *
   * @return user password
   */
  public String getPassword();

  /**
   * Set the user name for the user
   *
   * @param username
   */
  public void setUsername( String username );

  /**
   * Get the user name for the user
   *
   * @return user name
   */
  public String getUsername();

  /**
   * Set the description of the user
   *
   * @param description
   */
  public void setDescription( String description );

  /**
   * Get the user's description
   *
   * @return user description
   */
  public String getDescription();

  /**
   * Make the user enabled or disabled
   *
   * @param enabled
   */
  public void setEnabled( boolean enabled );

  /**
   * Check if the user is enabled or not
   *
   * @return the enabled
   */
  public boolean isEnabled();

  /**
   * Get the object id
   *
   * @return Object Id
   */
  public ObjectId getObjectId();

  /**
   * Set the object id of this user
   *
   * @param object
   *          id
   */
  public void setObjectId( ObjectId id );

  /**
   * The name of the user maps to the login id
   *
   * @return name
   */
  public String getName();

  /**
   * Set the name of the user.
   *
   * @param name
   *          The name of the user maps to the login id.
   */
  public void setName( String name );

  /**
   * Check if the user is admin or not
   *
   * @return admin
   */
  public default Boolean isAdmin() {
    return null;
  }

  /**
   * Make the user admin or not
   * @param admin
   */
  public default void setAdmin( Boolean admin ) {
    // Default implementation does nothing
  }

}
