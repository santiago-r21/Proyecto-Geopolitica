package com.Geopolitica.Proyecto.oec;

/** Error normalizado para cualquier fallo al consultar la OEC API. */
public class OecException extends RuntimeException {

	private final Integer status;

	public OecException(String message) {
		this(message, null, null);
	}

	public OecException(String message, Integer status, Throwable cause) {
		super(message, cause);
		this.status = status;
	}

	public Integer getStatus() {
		return status;
	}
}
