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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Locale;
import java.util.function.ObjIntConsumer;
import java.util.stream.StreamSupport;

import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.bcpg.ArmoredInputStream;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPSignatureList;
import org.bouncycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentVerifierBuilderProvider;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;

import org.springframework.util.Assert;

/**
 * Client utility to use PGP signatures.
 *
 * @author Mark Paluch
 */
public class PgpClient {

	/**
	 * Create a PGP signature for {@link InputStream message}.
	 *
	 * @param message the message to sign.
	 * @param secretKey the secret key.
	 * @param passphrase passphrase to unlock the key.
	 * @return the PGP signature block.
	 * @throws IOException
	 * @throws PGPException
	 */
	@SuppressWarnings("rawtypes")
	public static String createSignature(InputStream message, PGPSecretKey secretKey, char[] passphrase)
			throws IOException, PGPException {

		ByteArrayOutputStream buffer = new ByteArrayOutputStream();

		// Unlock the private key using the password
		PGPPrivateKey pgpPrivKey;
		try {
			pgpPrivKey = secretKey.extractPrivateKey(new JcePBESecretKeyDecryptorBuilder().build(passphrase));
		} catch (PGPException e) {
			throw new IllegalArgumentException("Cannot decrypt private key. Wrong passphrase? ", e);
		}

		PGPSignatureGenerator signer = new PGPSignatureGenerator(
				new BcPGPContentSignerBuilder(secretKey.getPublicKey().getAlgorithm(), PGPUtil.SHA256));

		signer.init(PGPSignature.BINARY_DOCUMENT, pgpPrivKey);

		Iterator it = secretKey.getPublicKey().getUserIDs();
		if (it.hasNext()) {
			PGPSignatureSubpacketGenerator spGen = new PGPSignatureSubpacketGenerator();
			spGen.addSignerUserID(false, (String) it.next());
			signer.setHashedSubpackets(spGen.generate());
		}

		doWithStream(message, 4096, (bytes, bytesRead) -> signer.update(bytes, 0, bytesRead));

		ArmoredOutputStream aos = new ArmoredOutputStream(buffer);
		signer.generate().encode(aos);
		aos.close();

		return buffer.toString();
	}

	/**
	 * Verify a PGP {@code signature}.
	 *
	 * @param message
	 * @param signature
	 * @param keyring
	 * @throws IOException
	 * @throws PGPException
	 * @throws IllegalStateException if the signature is invalid
	 */
	public static void verifySignature(String message, String signature, InputStream keyring)
			throws IOException, PGPException {
		verifySignature(new ByteArrayInputStream(message.getBytes()),
				new ByteArrayInputStream(signature.getBytes(StandardCharsets.US_ASCII)), keyring);
	}

	/**
	 * Verify a PGP {@code signature}
	 *
	 * @param message
	 * @param signature
	 * @param keyring
	 * @throws IOException
	 * @throws PGPException
	 * @throws IllegalStateException if the signature is invalid
	 */
	public static void verifySignature(InputStream message, InputStream signature, InputStream keyring)
			throws IOException, PGPException {

		PGPPublicKeyRingCollection keyRings = loadPublicKeyRings(keyring);
		ArmoredInputStream armored = new ArmoredInputStream(signature);

		String[] headers = armored.getArmorHeaders();

		Assert.state(headers != null && headers.length == 1, "Wrong number of headers found!");

		JcaPGPObjectFactory pgpFact = new JcaPGPObjectFactory(armored);
		PGPSignatureList signatures = (PGPSignatureList) pgpFact.nextObject();
		PGPSignature sig = signatures.get(0);

		sig.init(new BcPGPContentVerifierBuilderProvider(), keyRings.getPublicKey(sig.getKeyID()));

		doWithStream(message, 4096, (bytes, bytesRead) -> sig.update(bytes, 0, bytesRead));

		if (!sig.verify()) {
			throw new IllegalStateException("Signature invalid");
		}
	}

	/**
	 * A simple routine that opens a key ring file and loads the first available key suitable for signature generation.
	 *
	 * @param keyToUse key fingerprint.
	 * @param keyring stream to read the secret key ring collection from.
	 * @return a secret key.
	 * @throws IOException on a problem with using the input stream.
	 * @throws PGPException if there is an issue parsing the input stream.
	 */
	@SuppressWarnings("rawtypes")
	public static PGPSecretKey readSecretKey(String keyToUse, InputStream keyring) throws IOException, PGPException {

		PGPSecretKeyRingCollection keyRings = loadSecretKeyRings(keyring);

		return StreamSupport.stream(keyRings.spliterator(), false) //
				.flatMap(it -> StreamSupport.stream(it.spliterator(), false)) //
				.filter(PGPSecretKey::isSigningKey) //
				.filter(it -> {

					String fingerprint = Hex.encodeHexString(it.getPublicKey().getFingerprint());
					return fingerprint.toLowerCase(Locale.ROOT).endsWith(keyToUse.toLowerCase(Locale.ROOT));
				}).findFirst() //
				.orElseThrow(
						() -> new IllegalArgumentException(String.format("Can't find signing key %s in key ring.", keyToUse)));
	}

	private static PGPSecretKeyRingCollection loadSecretKeyRings(InputStream keyring) throws IOException, PGPException {
		return new PGPSecretKeyRingCollection(PGPUtil.getDecoderStream(keyring), new BcKeyFingerprintCalculator());
	}

	private static PGPPublicKeyRingCollection loadPublicKeyRings(InputStream keyring) throws IOException, PGPException {
		return new PGPPublicKeyRingCollection(PGPUtil.getDecoderStream(keyring), new BcKeyFingerprintCalculator());
	}

	private static void doWithStream(InputStream inputStream, int bufferSize, ObjIntConsumer<byte[]> callback)
			throws IOException {

		byte[] buffer = new byte[bufferSize];
		int bytesRead;
		while ((bytesRead = inputStream.read(buffer)) != -1) {
			callback.accept(buffer, bytesRead);
		}
	}

}
