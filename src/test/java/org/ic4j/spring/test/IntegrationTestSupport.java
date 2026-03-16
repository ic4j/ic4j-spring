package org.ic4j.spring.test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Assumptions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

final class IntegrationTestSupport {
	private static final String LOCAL_REPLICA_STATUS_URL = "http://127.0.0.1:4943/api/v2/status";
	private static final Path LOCAL_CANISTER_IDS = Paths.get(".dfx", "local", "canister_ids.json");
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private IntegrationTestSupport() {
	}

	static void assumeLocalReplicaAvailable() {
		Assumptions.assumeTrue(isStatusEndpointHealthy(), "Local IC replica is required for this integration test");
	}

	static String rootMessage(Throwable throwable) {
		Throwable current = throwable;
		while (current.getCause() != null && current.getCause() != current) {
			current = current.getCause();
		}
		return current.getMessage() == null ? current.toString() : current.getMessage();
	}

	static Optional<String> resolveLocalCanisterId(String canisterName) {
		if (!Files.exists(LOCAL_CANISTER_IDS)) {
			return Optional.empty();
		}

		try {
			Map<String, Map<String, String>> canisterIds = OBJECT_MAPPER.readValue(
				LOCAL_CANISTER_IDS.toFile(),
				new TypeReference<Map<String, Map<String, String>>>() {
				}
			);

			Map<String, String> networkIds = canisterIds.get(canisterName);
			if (networkIds == null) {
				return Optional.empty();
			}

			return Optional.ofNullable(networkIds.get("local"));
		} catch (IOException e) {
			return Optional.empty();
		}
	}

	private static boolean isStatusEndpointHealthy() {
		HttpURLConnection connection = null;
		try {
			connection = (HttpURLConnection) new URL(LOCAL_REPLICA_STATUS_URL).openConnection();
			connection.setRequestMethod("GET");
			connection.setConnectTimeout(2000);
			connection.setReadTimeout(2000);
			return connection.getResponseCode() == HttpURLConnection.HTTP_OK;
		} catch (IOException e) {
			return false;
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}
}