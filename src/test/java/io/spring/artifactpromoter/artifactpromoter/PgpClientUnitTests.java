/*
 * Copyright 2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.spring.artifactpromoter.artifactpromoter;

import static org.assertj.core.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;

/**
 * Unit tests for {@link PgpClient}.
 *
 * @author Mark Paluch
 */
class PgpClientUnitTests {

	@Test
	void shouldReadPgpKey() throws Exception {

		PGPSecretKey pgpSecretKey = loadKey();

		assertThat(pgpSecretKey).isNotNull();
		assertThat(pgpSecretKey.getPublicKey().getUserIDs().next()).isEqualTo("My dummy key <something@example.com>");
	}

	@Test
	void signMessage() throws Exception {

		PGPSecretKey pgpSecretKey = loadKey();

		String signature = PgpClient.createSignature(
				new ByteArrayInputStream("hello-world".getBytes(StandardCharsets.UTF_8)), pgpSecretKey,
				"something@example.com".toCharArray());

		assertThat(signature).contains("BEGIN PGP SIGNATURE").contains("Version: BCPG");
	}

	@Test
	void signAndVerifyMessage() throws Exception {

		PGPSecretKey pgpSecretKey = loadKey();

		String signature = PgpClient.createSignature(
				new ByteArrayInputStream("hello-world".getBytes(StandardCharsets.UTF_8)), pgpSecretKey,
				"something@example.com".toCharArray());

		PgpClient.verifySignature("hello-world", signature,
				new ClassPathResource("public-key-D6C063D5.asc").getInputStream());
	}

	@Test
	void verificationShouldFail() throws Exception {

		PGPSecretKey pgpSecretKey = loadKey();

		String signature = PgpClient.createSignature(
				new ByteArrayInputStream("hello-world".getBytes(StandardCharsets.UTF_8)), pgpSecretKey,
				"something@example.com".toCharArray());

		assertThatThrownBy(
				() -> PgpClient.verifySignature("tampered",
						signature, new ClassPathResource("public-key-D6C063D5.asc").getInputStream()))
								.isInstanceOf(IllegalStateException.class).hasMessageContaining("invalid");
	}

	@Test
	void wrongPassPhraseShouldFail() throws Exception {

		PGPSecretKey pgpSecretKey = loadKey();

		assertThatThrownBy(
				() -> PgpClient.createSignature(new ByteArrayInputStream("hello-world".getBytes(StandardCharsets.UTF_8)),
						pgpSecretKey, "wrong".toCharArray())).isInstanceOf(IllegalArgumentException.class);

	}

	private PGPSecretKey loadKey() throws IOException, PGPException {
		try (InputStream keyring = new ClassPathResource("private-key-D6C063D5.asc").getInputStream()) {
			return PgpClient.readSecretKey("D6C063D5", keyring);
		}
	}
}
