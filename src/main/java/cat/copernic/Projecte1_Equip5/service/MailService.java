package cat.copernic.Projecte1_Equip5.service;

import cat.copernic.Projecte1_Equip5.entity.Reserva;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Enviament de correus electrònics. De moment només l'avís d'anul·lació d'una
 * reserva, que l'enunciat demana tant si anul·la el client com si anul·la
 * l'agent.
 *
 * <ul>
 * <li>L'enviament no pot fer fallar mai l'anul·lació: qualsevol
 * {@link MailException} es captura i es deixa al log (RN04).</li>
 * <li>Si l'anul·lació va dins d'una transacció, el correu s'envia DESPRÉS del
 * commit, així no arriba cap avís d'una anul·lació que després es desfaci.</li>
 * <li>Si no hi ha servidor SMTP configurat (no s'ha definit
 * {@code spring.mail.host}), l'aplicació arrenca igual i el correu només es
 * deixa al log. Vegeu application.properties per configurar-lo.</li>
 * </ul>
 */
@Service
public class MailService {

    private static final Logger LOG = LoggerFactory.getLogger(MailService.class);
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String remitent;

    /**
     * Constructor amb injecció de dependències.
     *
     * @param mailSender enviador de correu; no hi és si no hi ha SMTP configurat
     * @param remitent adreça que apareix com a remitent
     */
    public MailService(ObjectProvider<JavaMailSender> mailSender,
            @Value("${app.mail.from:no-reply@nothingan-pri.local}") String remitent) {
        this.mailSender = mailSender;
        this.remitent = remitent;
    }

    /**
     * Avisa el client que la seva reserva s'ha anul·lat. Les dades es llegeixen
     * aquí mateix (mentre la transacció encara és oberta, perquè el client i el
     * vehicle de la reserva són LAZY) i el correu s'envia quan la transacció
     * s'ha confirmat.
     *
     * @param reserva reserva ja anul·lada (amb client i vehicle carregables)
     * @param devolucio import que es retorna al client
     * @param anulladaPerAgent true si l'ha anul·lat un agent; false si el
     * client mateix
     * @param motiu motiu indicat per l'agent (null si l'anul·la el client)
     */
    public void enviarAnulacioReserva(Reserva reserva, BigDecimal devolucio,
            boolean anulladaPerAgent, String motiu) {
        String destinatari;
        String assumpte;
        String text;
        try {
            destinatari = reserva.getClient().getEmail();
            assumpte = "Reserva " + reserva.getCodiReserva() + " anul·lada";
            text = construirText(reserva, devolucio, anulladaPerAgent, motiu);
        } catch (RuntimeException e) {
            // Si no es poden llegir les dades no es pot avisar, però l'anul·lació segueix endavant
            LOG.error("No s'ha pogut preparar el correu d'anul·lació de la reserva", e);
            return;
        }

        Runnable enviament = () -> enviar(destinatari, assumpte, text);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    enviament.run();
                }
            });
        } else {
            enviament.run();
        }
    }

    private String construirText(Reserva reserva, BigDecimal devolucio,
            boolean anulladaPerAgent, String motiu) {
        String nom = reserva.getClient().getNom();
        String vehicle = reserva.getVehicle().getMarca() + " " + reserva.getVehicle().getModel()
                + " (" + reserva.getVehicle().getMatricula() + ")";
        StringBuilder sb = new StringBuilder();
        sb.append("Hola, ").append(nom).append(".\n\n");
        sb.append(anulladaPerAgent
                ? "Un agent ha anul·lat la teva reserva."
                : "Hem anul·lat la teva reserva tal com has demanat.").append("\n\n");
        sb.append("Codi de reserva: ").append(reserva.getCodiReserva()).append('\n');
        sb.append("Vehicle: ").append(vehicle).append('\n');
        sb.append("Del ").append(reserva.getDataInici().format(DATA))
                .append(" al ").append(reserva.getDataFi().format(DATA)).append('\n');
        if (anulladaPerAgent && motiu != null && !motiu.isBlank()) {
            sb.append("Motiu: ").append(motiu.trim()).append('\n');
        }
        sb.append("Import retornat: ").append(devolucio).append(" €\n\n");
        sb.append("Gràcies per confiar en nosaltres.\n");
        return sb.toString();
    }

    private void enviar(String destinatari, String assumpte, String text) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            LOG.info("Correu NO enviat (no hi ha servidor SMTP configurat). Per a {}: {}",
                    emmascarar(destinatari), assumpte);
            return;
        }
        try {
            SimpleMailMessage missatge = new SimpleMailMessage();
            missatge.setFrom(remitent);
            missatge.setTo(destinatari);
            missatge.setSubject(assumpte);
            missatge.setText(text);
            sender.send(missatge);
            LOG.info("Correu d'anul·lació enviat a {}", emmascarar(destinatari));
        } catch (MailException e) {
            LOG.error("No s'ha pogut enviar el correu d'anul·lació a {}", emmascarar(destinatari), e);
        }
    }

    /**
     * Amaga part de l'adreça perquè no quedi sencera als logs (RN04).
     */
    private static String emmascarar(String email) {
        return email == null ? "(sense correu)" : email.replaceAll("(?<=.).(?=[^@]*@)", "*");
    }
}
