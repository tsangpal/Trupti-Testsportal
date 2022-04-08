package com.realignsoft.xdc1a;
package gchii.si.xdc1a;

import java.util.Vector;

/**
 *
 *  Copyright (C) 1989, 1991 Free Software Foundation, Inc.
 *
 * The group manager uses a vector for managing a
 * collection of groups.
 *
 * @version     1a, 12/15/1998
 * @author 	Gilbert Carl Herschberger II
 */
public final class GroupManager {
  /**
   * Create a group manager.
   */
  public GroupManager() {
    vector = null;
  }

  /**
   * The manager doesn't allocate a Vector until it
   * is needed. Therefore, the getCount() method
   * returns zero when there is no Vector.
   */
  public int getCount() {
    if ( vector == null ) {
      return 0;
    }

    return vector.size();
  }

  /**
   * Returns a group.
   * @param i index of value.
   * @return copy of stored group
   * @exception AccessorException If stored group
   * cannot be returned.
   */
  public Group getGroup( int i )
      throws AccessorException {
    if ( vector == null ) {
      throw new AccessorException( "Container is empty." );
    }

    if ( i < 0 ) {
      throw new AccessorException( "Index " + i + " is too small for container." );
    }

    if ( i >= vector.size() ) {
      throw new AccessorException( "Index " + i + " is too big for container." );
    }

    Object o = vector.elementAt( i );
    if ( o == null ) {
      throw new AccessorException( "Object is null." );
    }
    
    if ( !( o instanceof Group ) ) {
      throw new AccessorException( "Object is not a xdc.Group. (Internal)" );
    }

    return (Group) o;
  }

  /**
   * Returns a byte group.
   * @param i index of value.
   * @return copy of stored group
   * @exception AccessorException If stored group
   * cannot be returned.
   */
  public ByteGroup getByteGroup( int i )
      throws AccessorException {
    Group g = getGroup( i );

    if ( !( g instanceof ByteGroup ) ) {
      throw new AccessorException( "Object is not a xdc.ByteGroup." );
    }

    return (ByteGroup) g;
  }

  /**
   * Returns a char group.
   * @param i index of value.
   * @return copy of stored group
   * @exception AccessorException If stored group
   * cannot be returned.
   */
  public CharGroup getCharGroup( int i )
      throws AccessorException {
    Group g = getGroup( i );

    if ( !( g instanceof CharGroup ) ) {
      throw new AccessorException( "Object is not a xdc.CharGroup." );
    }

    return (CharGroup) g;
  }
