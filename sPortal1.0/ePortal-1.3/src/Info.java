/*
 * Geotools 2 - OpenSource mapping toolkit
 * (C) 2003, Geotools Project Managment Committee (PMC)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public
 *    License along with this library; if not, write to the Free Software
 *    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 *    This package contains documentation from OpenGIS specifications.
 *    OpenGIS consortium's work is fully acknowledged here.
 */
package org.geotools.cs;

// OpenGIS legacy dependencies
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Locale;
import java.util.Map;

import org.geotools.referencing.NamedIdentifier;
import org.geotools.resources.RemoteProxy;
import org.geotools.resources.Utilities;
import org.geotools.resources.i18n.ErrorKeys;
import org.geotools.resources.i18n.Errors;
import org.geotools.units.Unit;
import org.geotools.util.SimpleInternationalString;
import org.geotools.util.WeakHashSet;
import org.opengis.cs.CS_AngularUnit;
import org.opengis.cs.CS_Info;
import org.opengis.cs.CS_LinearUnit;
import org.opengis.cs.CS_Unit;
import org.opengis.metadata.Identifier;
import org.opengis.util.InternationalString;


/**
 * A base class for metadata applicable to coordinate system objects.
 * The metadata items "Abbreviation", "Alias", "Authority", "AuthorityCode",
 * "Name" and "Remarks" were specified in the Simple Features interfaces,
 * so they have been kept here.
 *
 * This specification does not dictate what the contents of these items
 * should be. However, the following guidelines are suggested:
 * <ul>
 *   <li>When {@link org.geotools.cs.CoordinateSystemAuthorityFactory}
 *       is used to create an object, the "Authority" and "AuthorityCode"
 *       values should be set to the authority name of the factory object,
 *       and the authority code supplied by the client, respectively. The
 *       other values may or may not be set. (If the authority is EPSG,
 *       the implementer may consider using the corresponding metadata values
 *       in the EPSG tables.)</li>
 *   <li>When {@link org.geotools.cs.CoordinateSystemFactory} creates an
 *       object, the "Name" should be set to the value supplied by the client.
 *       All of the other metadata items should be left empty.</li>
 * </ul>
 *
 * @version $Id: Info.java 14834 2005-07-20 11:55:24Z desruisseaux $
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 *
 * @see org.opengis.cs.CS_Info
 *
 * @deprecated Replaced by {@link org.geotools.referencing.AbstractIdentifiedObject} and
 *             {@link NamedIdentifier}.
 */
public class Info implements org.opengis.referencing.IdentifiedObject, Serializable {
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = -391073894118270236L;

    /**
     * An empty array of identifiers.
     */
    static final Identifier[] EMPTY_IDENTIFIERS = new Identifier[0];
    
    /**
     * Set of weak references to existing coordinate systems.
     * This set is used in order to return a pre-existing object
     * instead of creating a new one.
     */
    static final WeakHashSet pool = new WeakHashSet();
    
    /**
     * The non-localized object name.
     */
    private final Identifier name;
    
    /**
     * The non-localized object remarks.
     */
    private final InternationalString remarks;
    /**
     * Properties for all <code>get</code>methods except {@link #getName}.
     * For example, the method {@link #getAuthorityCode} returns the value
     * of property <code>"authorityCode"</code>. May be <code>null</code>
     * if there are no properties for this object.
     */
    private final Map properties;
    
    /**
     * OpenGIS object returned by {@link #cachedOpenGIS}.
     * It may be a hard or a weak reference.
     */
    private transient Object proxy;
    
    /**
     * Creates an object with the specified name. If <code>name</code>
     * implements the {@link java.util.Map} interface, then its values
     * will be copied for the following keys:
     * <ul>
     *   <li>"authority"</li>
     *   <li>"authorityCode"</li>
     *   <li>"alias"</li>
     *   <li>"abbreviation"</li>
     *   <li>"remarks"</li>
     * </ul>
     *
     * @param name This object name.
     */
    public Info(final CharSequence name) {
        ensureNonNull("name", name);                
        if (name instanceof Map) {
            // that has to be the strangest thing I have seen in a long time!
            // JG - a CharSequence that is a map?
            properties = new InfoProperties( (Map) name);
            proxy = properties.get("proxy");
        } else {
            properties = null;
        }
        this.remarks = new SimpleInternationalString("");
        this.name = new NamedIdentifier(null, name.toString());
    }
    
    /**
     * Gets the name of this object. The default implementation
     * returns the non-localized name given at construction time.
     *
     * @param locale The desired locale, or <code>null</code> for a default
     *        locale.
     *        If no string is available for the specified locale, an arbitrary
     *        locale is used.
     *
     * @see org.opengis.cs.CS_Info#getName()
     */
    public Identifier getName() {
        return name;
    }
    
    /**
     * Gets the authority name, or <code>null</code> if unspecified.
     * An Authority is an organization that maintains definitions of Authority
     * Codes.  For example the European Petroleum Survey Group (EPSG) maintains
     * a database of coordinate systems, and other spatial referencing objects,
     * where each object has a code number ID.  For example, the EPSG code for
     * a WGS84 Lat/Lon coordinate system is '4326'.
     *
     * @param locale The desired locale, or <code>null</code> for the default
     *        locale.
     *        If no string is available for the specified locale, an arbitrary
     *        locale is used.
     *
     * @see org.opengis.cs.CS_Info#getAuthority()
     */
    public String getAuthority(final Locale locale) {
        return getProperty("authority");
    }
    
    /**
     * Gets the authority-specific identification code, or <code>null</code>
     * if unspecified.  The AuthorityCode is a compact string defined by an
     * Authority to reference a particular spatial reference object.
     * For example, the European Survey Group (EPSG) authority uses 32 bit
     * integers to reference coordinate systems, so all their code strings
     * will consist of a few digits.  The EPSG code for WGS84 Lat/Lon is '4326'
     *
     * @param locale The desired locale, or <code>null</code> for the default
     *        locale.
     *        If no string is available for the specified locale, an arbitrary
     *        locale is used.
     *
     * @see org.opengis.cs.CS_Info#getAuthorityCode()
     *
     * @deprecated Replaced by {@link NamedIdentifier#getCode}.
     */
    public String getAuthorityCode(final Locale locale) {
        return getProperty("authorityCode");
    }
    
    /**
     * Gets the alias, or <code>null</code> if there is none.
     *
     * @param locale The desired locale, or <code>null</code> for the default
     *        locale.
     *        If no string is available for the specified locale, an arbitrary
     *        locale is used.
     *
     * @see org.opengis.cs.CS_Info#getAlias()
     *
     * @deprecated Replaced by {@link org.geotools.referencing.AbstractIdentifiedObject#getIdentifiers}.
     */
    public String getAlias(final Locale locale) {
        return getProperty("alias");
    }

    /** For compatibility with GeoAPI interfaces. */
    public java.util.Collection getAlias() {
        return java.util.Collections.EMPTY_SET;
    }    
    
    /**
     * Gets the abbreviation, or <code>null</code> if there is none.
     *
     * @param locale The desired locale, or <code>null</code> for the default
     *        locale.
     *        If no string is available for the specified locale, an arbitrary
     *        locale is used.
     *
     * @see org.opengis.cs.CS_Info#getAbbreviation()
     *
     * @deprecated No replacement.
     */
    public String getAbbreviation(final Locale locale) {
        return getProperty("abbreviation");
    }
    
    /**
     * Gets the provider-supplied remarks,
     * or <code>null</code> if there is none.
     *
     * @param locale The desired locale, or <code>null</code> for the default
     *        locale.
     *        If no string is available for the specified locale, an arbitrary
     *        locale is used.
     *
     * @see org.opengis.cs.CS_Info#getRemarks()
     */
    public InternationalString getRemarks() {
        return remarks;
    }
    
    /**
     * Gets the property for the specified key,
     * or <code>null</code> if there is none.
     *
     * @param key The key.
     */
    private String getProperty(final String key) {
        return (properties!=null) ? (String) properties.get(key) : null;
    }

    /**
     * Compare two objects for equality.
     *
     * @param  object1 The first object to compare (may be <code>null</code>).
     * @param  object2 The second object to compare (may be <code>null</code>).
     * @return <code>true</code> if both objects are equal.
     */
    static boolean equals(final Object object1, final Object object2) {
        return Utilities.equals(object1, object2);
    }

    /**
     * Compare two objects for equality.
     *
     * @param  object1 The first object to compare (may be <code>null</code>).
     * @param  object2 The second object to compare (may be <code>null</code>).
     * @param  compareNames <code>true</code> for performing a strict comparaison, or
     *         <code>false</code> for comparing only properties relevant to transformations.
     * @return <code>true</code> if both objects are equal.
     */
    static boolean equals(final Info object1, final Info object2, final boolean compareNames) {
        return (object1==object2) || (object1!=null && object1.equals(object2, compareNames));
    }

    /**
     * Compare this object with the specified object for equality.
     *
     * If <code>compareNames</code> is <code>true</code>, then all available properties
     * are compared including {@linkplain #getName name}, {@linkplain #getAlias alias},
     * {@linkplain #getAuthorityCode authority code}, etc.
     *
     * If <code>compareNames</code> is <code>false</code>, then this method compare
     * only the properties needed for computing transformations. In other words,
     * <code>sourceCS.equals(targetCS, false)</code> returns <code>true</code> if
     * the transformation from <code>sourceCS</code> to <code>targetCS</code> is
     * the identity transform, no matter what {@link #getName} saids.
     *
     * @param  object The object to compare to <code>this</code>.
     * @param  compareNames <code>true</code> for performing a strict comparaison, or
     *         <code>false</code> for comparing only properties relevant to transformations.
     * @return <code>true</code> if both objects are equal.
     */
    public boolean equals(final Info object, final boolean compareNames) {
        if (object!=null && object.getClass().equals(getClass())) {
            if (!compareNames) {
                return true;
            }
            return equals(name,       object.name) &&
                   equals(properties, object.properties);
        }
        return false;
    }
    
    /**
     * Compares the specified object with this info for equality.
     * The default implementation invokes {@link #equals(Info, boolean)}.
     *
     * @param  object The other object (may be <code>null</code>).
     * @return <code>true</code> if both objects are equal.
     */
    public final boolean equals(final Object object) {
        return (object instanceof Info) && equals((Info)object, true);
    }
    
    /**
     * Returns a hash value for this info. {@linkplain #getName Name},
     * {@linkplain #getAlias alias}, {@linkplain #getAuthorityCode authority code}
     * and the like are not taken in account. In other words, two info objects
     * will return the same hash value if they are equal in the sense of
     * <code>{@link #equals equals}(Info, <strong>false</strong>)</code>.
     *
     * @return The hash code value. This value doesn't need to be the same
     *         in past or future versions of this class.
     */
    public int hashCode() {
        // Subclasses need to override this!!!!
        return (int)serialVersionUID ^ getClass().hashCode();
    }
    
    /**
     * Returns a <em>Well Known Text</em> (WKT) for this info.
     * "Well known text" are part of OpenGIS's specification.
     *
     * @see org.opengis.cs.CS_Info#getWKT()
     */
    public String toString() {
        final String WKT = getProperty("WKT");
        return (WKT!=null) ? WKT : toString(null);
    }
    
    /**
     * Returns a <em>Well Known Text</em> (WKT) for this info.
     *
     * @param context The contextual unit. Most subclasses will
     *        ignore this argument, except {@link PrimeMeridian}.
     */
    final String toString(final Unit context) {
        final Locale locale = null;
        final StringBuffer buffer = new StringBuffer(40);
        buffer.append("[\"");
        buffer.append(getWKTName(locale));
        buffer.append('"');
        buffer.insert(0, addString(buffer, context));
        if (properties!=null) {
            final String authority = getAuthority(locale);
            if (authority!=null) {
                buffer.append(", AUTHORITY[\"");
                buffer.append(authority);
                final String code = getAuthorityCode(locale);
                if (code!=null) {
                    buffer.append("\",\"");
                    buffer.append(code);
                }
                buffer.append("\"]");
            }
        }
        buffer.append(']');
        return buffer.toString();
    }

    /**
     * Returns the name to place into the WKT.
     * To be overriden by {@link Projection} only.
     */
    String getWKTName(final Locale locale) {
        return name.getCode();
    }
    
    /**
     * Adds more information inside the "[...]" part of {@link #toString}.
     * The default implementation adds nothing. Subclasses will override
     * this method in order to complete string representation.
     *
     * @param  buffer The buffer to add the string to.
     * @param  context The contextual unit. Most subclasses will
     *         ignore this argument, except {@link PrimeMeridian}.
     * @return The WKT code name (e.g. "GEOGCS").
     */
    String addString(final StringBuffer buffer, final Unit context) {
        return Utilities.getShortClassName(this);
    }
    
    /**
     * Adds a unit in WKT form.
     */
    final void addUnit(final StringBuffer buffer, final Unit unit) {
        if (unit!=null) {
            buffer.append("UNIT[\"");
            buffer.append(unit.getLocalizedName());
            buffer.append('"');
            Unit base=null;
            if (Unit.METRE.canConvert(unit)) {
                base = Unit.METRE;
            } else if (Unit.RADIAN.canConvert(unit)) {
                base = Unit.RADIAN;
            } else if (Unit.SECOND.canConvert(unit)) {
                base = Unit.SECOND;
            }
            if (unit!=null) {
                buffer.append(',');
                buffer.append(base.convert(1, unit));
            }
            buffer.append(']');
        }
    }
    
    /**
     * Makes sure an argument is non-null. This is a
     * convenience method for subclass constructors.
     *
     * @param  name   Argument name.
     * @param  object User argument.
     * @throws IllegalArgumentException if <code>object</code> is null.
     */
    protected static void ensureNonNull(final String name, final Object object)
        throws IllegalArgumentException
    {
        if (object==null) {
            throw new IllegalArgumentException(Errors.format(
                        ErrorKeys.NULL_ARGUMENT_$1, name));
        }
    }
    
    /**
     * Makes sure an array element is non-null.
     *
     * @param  name  Argument name.
     * @param  array User argument.
     * @param  index Element to check.
     * @throws IllegalArgumentException if <code>array[i]</code> is null.
     */
    static void ensureNonNull(final String name, final Object[] array, final int index)
        throws IllegalArgumentException
    {
        if (array[index]==null) {
            throw new IllegalArgumentException(Errors.format(
                        ErrorKeys.NULL_ARGUMENT_$1, name+'['+index+']'));
        }
    }
    
    /**
     * Makes sure that the specified unit is a temporal one.
     *
     * @param  unit Unit to check.
     * @throws IllegalArgumentException if <code>unit</code> is not a temporal
     *         unit.
     */
    static void ensureTimeUnit(final Unit unit) throws IllegalArgumentException {
        if (!Unit.SECOND.canConvert(unit)) {
            throw new IllegalArgumentException(Errors.format(
                        ErrorKeys.NON_TEMPORAL_UNIT_$1, unit));
        }
    }
    
    /**
     * Makes sure that the specified unit is a linear one.
     *
     * @param  unit Unit to check.
     * @throws IllegalArgumentException if <code>unit</code> is not a linear
     *         unit.
     */
    static void ensureLinearUnit(final Unit unit) throws IllegalArgumentException {
        if (!Unit.METRE.canConvert(unit)) {
            throw new IllegalArgumentException(Errors.format(
                        ErrorKeys.NON_LINEAR_UNIT_$1, unit));
        }
    }
    
    /**
     * Makes sure that the specified unit is an angular one.
     *
     * @param  unit Unit to check.
     * @throws IllegalArgumentException if <code>unit</code> is not an angular
     *         unit.
     */
    static void ensureAngularUnit(final Unit unit) throws IllegalArgumentException {
        if (!Unit.DEGREE.canConvert(unit)) {
            throw new IllegalArgumentException(Errors.format(
                        ErrorKeys.NON_ANGULAR_UNIT_$1, unit));
        }
    }

    /**
     * Serialize a single instance of this object.
     * This is an optimisation for speeding up RMI.
     */
    Object writeReplace() throws ObjectStreamException {
        return pool.canonicalize(this);
    }
    
    /**
     * Returns a reference to a unique instance of this <code>Info</code>.
     * This method is automatically invoked during deserialization.
     *
     * NOTE ABOUT ACCESS-MODIFIER:      This method can't be private,
     * because it would prevent it from being invoked from subclasses
     * in this package (e.g. {@link CoordinateSystem}).   This method
     * <em>will not</em> be invoked for classes outside this package,
     * unless we give it <code>protected</code> access.   TODO: Would
     * it be a good idea?
     */
    Object readResolve() throws ObjectStreamException {
        return pool.canonicalize(this);
    }
    
    /**
     * Returns an OpenGIS interface for this info.
     * The returned object is suitable for RMI use.
     *
     * Note: The returned type is a generic {@link Object} in order
     *       to avoid premature class loading of OpenGIS interface.
     *
     * @param  adapters The originating {@link Adapters}.
     * @return The OpenGIS interface for this info.
     * @throws RemoteException if the object can't be exported.
     */
    Object toOpenGIS(final Object adapters) throws RemoteException {
        return new Export(adapters);
    }
    
    /**
     * Returns an OpenGIS interface for this info. This method first looks in the cache.
     * If no interface was previously cached, then this method creates a new adapter and
     * caches the result.
     *
     * @param  adapters The originating {@link Adapters}.
     * @return The OpenGIS interface for this info.
     * @throws RemoteException if the object can't be exported.
     */
    final synchronized Object cachedOpenGIS(final Object adapters) throws RemoteException {
        if (proxy!=null) {
            if (proxy instanceof Reference) {
                final Object ref = ((Reference) proxy).get();
                if (ref!=null) {
                    return ref;
                }
            } else {
                return proxy;
            }
        }
        final Object opengis = toOpenGIS(adapters);
        proxy = new WeakReference(opengis);
        return opengis;
    }

    /** For compatibility with GeoAPI interfaces. */
    public java.util.Set/*<Identifier>*/ getIdentifiers() {
        return java.util.Collections.EMPTY_SET;
    }    

    /** For compatibility with GeoAPI interfaces. */
    public String toWKT() {
        return toString();
    }
    
    
    /////////////////////////////////////////////////////////////////////////
    ////////////////                                         ////////////////
    ////////////////             OPENGIS ADAPTER             ////////////////
    ////////////////                                         ////////////////
    /////////////////////////////////////////////////////////////////////////
    
    /**
     * Wraps a {@link Info} object for use with OpenGIS. This wrapper is a
     * good place to check for non-implemented OpenGIS methods (just check
     * for methods throwing {@link UnsupportedOperationException}). This
     * class is suitable for RMI use.
     */
    class Export extends UnicastRemoteObject implements RemoteProxy, CS_Info {
        /**
         * The originating adapter.
         */
        protected final Adapters adapters;
        
        /**
         * Constructs a remote object.
         */
        protected Export(final Object adapters) throws RemoteException {
            super(); // TODO: Fetch the port number from the adapter.
            this.adapters = (Adapters)adapters;
        }
        
        /**
         * Returns the underlying implementation.
         */
        public final Serializable getImplementation() throws RemoteException {
            return Info.this;
        }
        
        /**
         * Gets the name.
         */
        public String getName() throws RemoteException {
            return Info.this.getName().toString();
        }
        
        /**
         * Gets the authority name.
         */
        public String getAuthority() throws RemoteException {
            return Info.this.getAuthority(null);
        }
        
        /**
         * Gets the authority-specific identification code.
         */
        public String getAuthorityCode() throws RemoteException {
            return Info.this.getAuthorityCode(null);
        }
        
        /**
         * Gets the alias.
         */
        public String getAlias() throws RemoteException {
            return Info.this.getAlias(null);
        }
        
        /**
         * Gets the abbreviation.
         */
        public String getAbbreviation() throws RemoteException {
            return Info.this.getAbbreviation(null);
        }
        
        /**
         * Gets the provider-supplied remarks.
         */
        public String getRemarks() throws RemoteException {
            return Info.this.getRemarks().toString();
        }
        
        /**
         * Gets a Well-Known text representation of this object.
         */
        public String getWKT() throws RemoteException {
            return Info.this.toString();
        }
        
        /**
         * Gets an XML representation of this object.
         */
        public String getXML() throws RemoteException {
            throw new UnsupportedOperationException("XML formatting not yet implemented");
        }
        
        /**
         * Returns a string representation of this info.
         */
        public String toString() {
            return Info.this.toString();
        }
    }
    
    /**
     * OpenGIS abstract unit.
     */
    class AbstractUnit extends Export implements CS_Unit {
        /**
         * Number of meters per linear unit or
         *          radians per angular unit.
         */
        final double scale;
        
        /**
         * Constructs an abstract unit.
         */
        public AbstractUnit(final Adapters adapters, final double scale)
                throws RemoteException
        {
            super(adapters);
            this.scale = scale;
        }
        
        /**
         * Returns a Well Known Text for this unit.
         */
        public final String toString() {
            return "UNIT[\""+name+"\","+scale+']';
        }
    }
    
    /**
     * OpenGIS linear unit.
     */
    final class LinearUnit extends AbstractUnit implements CS_LinearUnit {
        /**
         * Constructs a linear unit.
         */
        public LinearUnit(final Adapters adapters, final double metersPerUnit)
                throws RemoteException
        {
            super(adapters, metersPerUnit);
        }
        
        /**
         * Returns the number of meters per linear unit.
         */
        public double getMetersPerUnit() throws RemoteException {
            return scale;
        }
    }
    
    /**
     * OpenGIS angular unit.
     */
    final class AngularUnit extends AbstractUnit implements CS_AngularUnit {
        /**
         * Constructs an angular unit.
         */
        public AngularUnit(final Adapters adapters, final double radiansPerUnit)
                throws RemoteException
        {
            super(adapters, radiansPerUnit);
        }
        
        /**
         * Returns the number of radians per angular unit.
         */
        public double getRadiansPerUnit() throws RemoteException {
            return scale;
        }
    }
}
