package com.gestion.backend.almacenamiento;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.gestion.backend.error.RecursoNoEncontradoException;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Service
@Profile("minio")
public class AlmacenamientoMinio implements AlmacenamientoArchivos {

	private final String bucket;
	private final long expiracionMinutos;
	private final S3Client cliente;
	private final S3Presigner firmador;

	public AlmacenamientoMinio(
			@Value("${gestion.almacenamiento.minio.endpoint:http://localhost:9000}") String endpoint,
			@Value("${gestion.almacenamiento.minio.bucket:reportes}") String bucket,
			@Value("${gestion.almacenamiento.minio.region:us-east-1}") String region,
			@Value("${gestion.almacenamiento.minio.access-key:minioadmin}") String accessKey,
			@Value("${gestion.almacenamiento.minio.secret-key:minioadmin}") String secretKey,
			@Value("${gestion.almacenamiento.minio.expiracion-url-minutos:15}") long expiracionMinutos) {
		this.bucket = bucket;
		this.expiracionMinutos = expiracionMinutos;

		StaticCredentialsProvider credenciales = StaticCredentialsProvider.create(
				AwsBasicCredentials.create(accessKey, secretKey));

		this.cliente = S3Client.builder()
				.endpointOverride(URI.create(endpoint))
				.region(Region.of(region))
				.credentialsProvider(credenciales)
				.forcePathStyle(true)
				.build();

		this.firmador = S3Presigner.builder()
				.endpointOverride(URI.create(endpoint))
				.region(Region.of(region))
				.credentialsProvider(credenciales)
				.serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
				.build();
	}

	@PostConstruct
	public void verificarBucket() {
		try {
			cliente.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
		} catch (SdkException e) {
			throw new IllegalStateException(
					"No se pudo acceder al bucket '" + bucket
							+ "' en MinIO. Verifica que el contenedor este arriba y el bucket exista.",
					e);
		}
	}

	@Override
	public void guardar(String clave, InputStream contenido, String tipoContenido, long tamanio) {
		try {
			PutObjectRequest solicitud = PutObjectRequest.builder()
					.bucket(bucket)
					.key(clave)
					.contentType(tipoContenido)
					.contentLength(tamanio)
					.build();
			cliente.putObject(solicitud, RequestBody.fromInputStream(contenido, tamanio));
		} catch (SdkException e) {
			throw new IllegalStateException("No se pudo guardar el archivo en MinIO", e);
		}
	}

	@Override
	public String urlDe(String clave) {
		try {
			GetObjectRequest solicitud = GetObjectRequest.builder().bucket(bucket).key(clave).build();
			PresignedGetObjectRequest firmada = firmador.presignGetObject(GetObjectPresignRequest.builder()
					.signatureDuration(Duration.ofMinutes(expiracionMinutos))
					.getObjectRequest(solicitud)
					.build());
			return firmada.url().toString();
		} catch (SdkException e) {
			throw new IllegalStateException("No se pudo firmar la URL del archivo", e);
		}
	}

	@Override
	public RecursoAlmacenado obtener(String clave) {
		try {
			ResponseInputStream<GetObjectResponse> respuesta = cliente.getObject(
					GetObjectRequest.builder().bucket(bucket).key(clave).build());
			return new RecursoAlmacenado(respuesta, respuesta.response().contentType(),
					respuesta.response().contentLength());
		} catch (NoSuchKeyException e) {
			throw new RecursoNoEncontradoException("Archivo no encontrado: " + clave);
		} catch (SdkException e) {
			throw new IllegalStateException("No se pudo leer el archivo desde MinIO", e);
		}
	}

	@PreDestroy
	public void cerrar() {
		firmador.close();
		cliente.close();
	}
}
