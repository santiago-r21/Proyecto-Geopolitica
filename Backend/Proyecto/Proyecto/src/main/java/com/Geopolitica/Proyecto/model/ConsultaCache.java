package com.Geopolitica.Proyecto.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * Caché durable de resultados ya calculados desde la OEC, por (tipo, clave).
 * P.ej. tipo="CATALOGO_PAISES", clave="global" -> el catálogo completo de países;
 * más adelante tipo="DEPENDENCIA", clave="CO:2023" -> el resumen de un país/año.
 * No hay TTL: los datos históricos de la OEC no cambian, así que una vez
 * cacheados no se vuelven a pedir.
 */
@Entity
@Table(name = "consulta_cache", uniqueConstraints = @UniqueConstraint(columnNames = {"tipo", "clave"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConsultaCache {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String tipo;

	@Column(nullable = false)
	private String clave;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, columnDefinition = "jsonb")
	private String resultado;

	@Column(nullable = false)
	private Instant consultadoEn;

	public ConsultaCache(String tipo, String clave, String resultado, Instant consultadoEn) {
		this.tipo = tipo;
		this.clave = clave;
		this.resultado = resultado;
		this.consultadoEn = consultadoEn;
	}
}
