package com.pardorps.pardrops.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Ficha de una imagen subida a la plataforma.
 *
 * <p>Guarda los metadatos del fichero (nombre original, nombre guardado, tipo MIME, tamaño y
 * propietario) para poder auditarlo y borrarlo. El binario <strong>no</strong> se guarda en la
 * base de datos: vive en el almacén de archivos y aquí solo queda la ruta pública.</p>
 *
 * <p>El propietario es quien ha subido el fichero. Solo esa persona o un administrador pueden
 * gestionarlo.</p>
 */
@Entity
@Table(name = "media_assets")
@Getter
@Setter
@NoArgsConstructor
public class MediaAsset {

    /** Identificador del recurso. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nombre original del fichero, tal y como lo eligio el usuario. */
    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    /** Nombre con el que se guarda el fichero en el almacén (aleatorio). */
    @Column(name = "stored_name", nullable = false, unique = true, length = 255)
    private String storedName;

    /** Ruta pública del recurso (por ejemplo {@code /media/abc123.png}). */
    @Column(name = "public_path", nullable = false, length = 500)
    private String publicPath;

    /** Tipo MIME detectado en el contenido del fichero. */
    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    /** Tamaño del fichero en bytes. */
    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    /** Usuario que subió el fichero. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    /** Fecha de subida. */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
