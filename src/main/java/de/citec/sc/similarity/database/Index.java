package de.citec.sc.similarity.database;

import java.io.Serializable;

/**
 * This class stores index-information about a datapoint from a data-file. The
 * class implements the Serializable-interface so that the index-file can be
 * serialized to the hard-drive if necessary.
 * 
 * @author hterhors
 *
 *         Feb 18, 2016
 */
public class Index implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * File name of the data-file
	 */
	public String fileName;

	/**
	 * The position of the data-point in the data-file.
	 */
	public long bytePosition;

	/**
	 * The number of bytes of the datapoint. If each datapoint is stored in a
	 * single line this information is not required. Instead use the
	 * readLine()-method.
	 */
	public long length;

	/**
	 * Create a new empty index. Only used for Java-Serialization.
	 */
	public Index() {
		this.fileName = "";
		this.bytePosition = -1;
		this.length = -1;
	}

	/**
	 * Create a new index that stores information about a single datapoint.
	 */
	public Index(final String fileName, final long fromByte, final long length) {
		super();
		this.fileName = fileName;
		this.bytePosition = fromByte;
		this.length = length;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (this.fileName == null ? 0 : this.fileName.hashCode());
		result = prime * result + (int) (this.bytePosition ^ this.bytePosition >>> 32);
		result = prime * result + (int) (this.length ^ this.length >>> 32);
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Index)) {
			return false;
		}
		final Index other = (Index) obj;
		if (this.fileName == null) {
			if (other.fileName != null) {
				return false;
			}
		} else if (!this.fileName.equals(other.fileName)) {
			return false;
		}
		if (this.bytePosition != other.bytePosition) {
			return false;
		}
		if (this.length != other.length) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "Index [" + (this.fileName != null ? "fileName=" + this.fileName + ", " : "") + "fromByte="
				+ this.bytePosition + ", length=" + this.length + "]";
	}

}
