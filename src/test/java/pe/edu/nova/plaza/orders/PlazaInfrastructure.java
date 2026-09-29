package pe.edu.nova.plaza.orders;

import java.io.IOException;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.vault.VaultContainer;

/**
 * La base y el Vault de las pruebas, levantados una vez por JVM.
 *
 * <p>Vault guarda las credenciales de la base igual que el compose de Plaza: un secreto con
 * {@code DB_URL}, {@code DB_USERNAME} y {@code DB_PASSWORD}. El servicio las lee de ahí, así que la
 * prueba recorre el mismo camino que producción.
 */
final class PlazaInfrastructure {

    private static final String VAULT_TOKEN = "plaza-test-root";

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("orders")
            .withUsername("orders")
            .withPassword("orders-test");

    private static final VaultContainer<?> VAULT =
            new VaultContainer<>("hashicorp/vault:2.1.1").withVaultToken(VAULT_TOKEN);

    private static boolean started;

    private PlazaInfrastructure() {}

    /** Levanta los contenedores, carga el secreto y apunta el servicio a Vault. */
    static synchronized void start() {
        if (started) {
            return;
        }
        POSTGRES.start();
        VAULT.start();
        try {
            VAULT.execInContainer(
                    "vault",
                    "kv",
                    "put",
                    "secret/plaza/orders/db",
                    "DB_URL=" + POSTGRES.getJdbcUrl(),
                    "DB_USERNAME=" + POSTGRES.getUsername(),
                    "DB_PASSWORD=" + POSTGRES.getPassword());
        } catch (IOException e) {
            throw new IllegalStateException("Could not load the database secret into Vault", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while loading the database secret into Vault", e);
        }
        System.setProperty("nova.secrets.vault.address", VAULT.getHttpHostAddress());
        System.setProperty("nova.secrets.vault.token", VAULT_TOKEN);
        started = true;
    }
}
