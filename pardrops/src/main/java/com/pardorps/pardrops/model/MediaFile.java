package com.pardorps.pardrops.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * Contenido binario de una imagen guardada en la base de datos.
 *
 * <p>En producción el sistema de archivos de Render es efímero: si las imágenes
 * solo vivieran en disco desaparecerían en cada redespliegue. Esta entidad
 * conserva los bytes en PostgreSQL para que el catálogo siga mostrando las
 * mismas imágenes después de un despliegue o un reinicio.</p>
 *
 * <p>La ficha pública de la imagen (propietario, nombre, tipo) sigue estando en
 * {@link MediaAsset}; aquí solo se guarda el contenido y su tipo MIME.</p>
 */
@Entity
@Table(name = "media_files")
public class MediaFile {

    /** Nombre único con el que se guardó el fichero. */
    @Id
    @Column(name = "stored_name", length = 160, nullable = false)
    private String storedName;

    /** Tipo MIME real del contenido. */
    @Column(name = "mime_type", length = 100, nullable = false)
    private String mimeType;

    /**
     * Bytes del fichero. Se mapea como VARBINARY para que Hibernate use
     * {@code bytea} en PostgreSQL y un binario equivalente en H2.
     */
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "content", nullable = false, length = 5_000_000)
    private byte[] content;

    /** Fecha en la que se guardó el fichero. */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Constructor vacío requerido por JPA. */
    public MediaFile() {
    }

    /**
     * @param storedName nombre único del fichero.
     * @param mimeType   tipo MIME del contenido.
     * @param content    bytes del fichero.
     * @param createdAt  fecha de guardado.
     */
    public MediaFile(String storedName, String mimeType, byte[] content, Instant createdAt) {
        this.storedName = storedName;
        this.mimeType = mimeType;
        this.content = content;
        this.createdAt = createdAt;
    }

    public String getStoredName() {
        return storedName;
    }

    public void setStoredName(String storedName) {
        this.storedName = storedName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
